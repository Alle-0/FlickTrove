package com.cinetrack.data.repository.importers

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.importers.ImporterUtils.parseAndNormalizeWatchedDate
import com.cinetrack.data.repository.importers.ImporterUtils.parseCsvLine
import com.cinetrack.data.repository.importers.ImporterUtils.parseCsvRecords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TvTimeGdprImporter @Inject constructor(
    private val movieRepository: MovieRepository
) {
    private data class TvTimeItemData(
        val id: String?,
        val title: String,
        val mediaType: String,
        var isWatched: Boolean,
        var isFavorite: Boolean = false,
        var isForLater: Boolean = false,
        var watchedAt: String? = null,
        var personalRating: Double? = null,
        var personalNote: String? = null,
        val folders: MutableSet<String> = mutableSetOf(),
        val watchedEpisodes: MutableMap<String, MutableSet<Int>> = mutableMapOf()
    )

    suspend fun migrateTvTimeGdprZip(
        zipEntries: Map<String, ByteArray>,
        keepLatestWatchDate: Boolean,
        onProgress: suspend (Int, Int) -> Unit,
        onBatchReady: suspend (List<Pair<Movie, String?>>, Boolean, suspend (Int, Int) -> Unit) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val itemsMap = mutableMapOf<String, TvTimeItemData>()

        fun addItem(
            id: String?,
            title: String,
            mediaType: String,
            isWatched: Boolean,
            watchedAt: String? = null,
            isFavorite: Boolean = false,
            isForLater: Boolean = false
        ): TvTimeItemData {
            val cleanTitle = title.trim()
            if (cleanTitle.isBlank()) return TvTimeItemData(null, "", mediaType, isWatched)
            val key = if (!id.isNullOrBlank()) "${mediaType}_${id.trim()}" else "${mediaType}_${cleanTitle.lowercase()}"
            val existing = itemsMap.values.find { (it.id != null && it.id == id) || (it.title.equals(cleanTitle, ignoreCase = true) && it.mediaType == mediaType) }
            if (existing != null) {
                if (isWatched && !isForLater) {
                    if (mediaType != "tv" || !existing.isForLater) {
                        existing.isWatched = true
                        existing.isForLater = false
                    }
                } else if (isForLater && (!existing.isWatched || mediaType == "tv")) {
                    existing.isForLater = true
                    existing.isWatched = false
                }
                if (isFavorite || (isForLater && (!existing.isWatched || mediaType == "tv"))) existing.isFavorite = true
                if (watchedAt != null && existing.watchedAt == null) existing.watchedAt = watchedAt
                return existing
            }
            val newItem = TvTimeItemData(
                id = id?.trim(),
                title = cleanTitle,
                mediaType = mediaType,
                isWatched = if (isForLater) false else isWatched,
                isFavorite = isFavorite || (isForLater && !isWatched),
                isForLater = if (isWatched) false else isForLater,
                watchedAt = watchedAt
            )
            itemsMap[key] = newItem
            return newItem
        }

        // 1. Parse followed_tv_show.csv
        zipEntries["followed_tv_show.csv"]?.let { bytes ->
            bytes.decodeToString().lineSequence().filter { it.isNotBlank() && !it.startsWith("user_id,") }.forEach { line ->
                val cols = parseCsvLine(line)
                if (cols.size >= 7) {
                    val title = cols[5]
                    val showId = cols[6]
                    val updatedAt = cols.getOrNull(7)?.let { parseAndNormalizeWatchedDate(it) }
                    addItem(id = showId, title = title, mediaType = "tv", isWatched = true, watchedAt = updatedAt)
                }
            }
        }

        // 2. Parse user_tv_show_data.csv
        zipEntries["user_tv_show_data.csv"]?.let { bytes ->
            bytes.decodeToString().lineSequence().filter { it.isNotBlank() && !it.startsWith("tv_show_id,") }.forEach { line ->
                val cols = parseCsvLine(line)
                if (cols.size >= 5) {
                    val showId = cols[0]
                    val isFav = cols[2].trim() == "1" || cols[2].trim().equals("true", ignoreCase = true)
                    val epsSeen = cols[3].trim().toIntOrNull() ?: 0
                    val title = cols[4]
                    addItem(id = showId, title = title, mediaType = "tv", isWatched = epsSeen > 0, isFavorite = isFav)
                }
            }
        }

        // 3. Parse user_show_special_status.csv (To Watch / Status)
        zipEntries["user_show_special_status.csv"]?.let { bytes ->
            bytes.decodeToString().lineSequence().filter { it.isNotBlank() && !it.startsWith("created_at,") }.forEach { line ->
                val cols = parseCsvLine(line)
                if (cols.size >= 6) {
                    val title = cols[2]
                    val showId = cols[4]
                    val status = cols[5].trim().lowercase()
                    val isForLater = status.isNotBlank() && status !in listOf("archived", "dropped", "watched", "completed", "seen", "ignored", "stopped", "up_to_date")
                    addItem(id = showId, title = title, mediaType = "tv", isWatched = !isForLater, isForLater = isForLater)
                }
            }
        }

        // 3b. Parse user_movie_special_status.csv & user_movie_data.csv if present
        zipEntries["user_movie_special_status.csv"]?.let { bytes ->
            val records = parseCsvRecords(bytes.decodeToString())
            if (records.isNotEmpty()) {
                val headers = records.first().map { it.trim().lowercase() }
                val titleIdx = headers.indexOfFirst { it in listOf("title", "movie_name", "name") }.takeIf { it != -1 } ?: 2
                val idIdx = headers.indexOfFirst { it in listOf("movie_id", "id") }.takeIf { it != -1 } ?: 4
                val statusIdx = headers.indexOfFirst { it in listOf("status", "special_status") }
                for (i in 1 until records.size) {
                    val cols = records[i]
                    val title = cols.getOrNull(titleIdx)?.trim() ?: ""
                    if (title.isNotBlank()) {
                        val movieId = cols.getOrNull(idIdx)?.trim()
                        val status = if (statusIdx != -1) cols.getOrNull(statusIdx)?.trim()?.lowercase() ?: "" else cols.lastOrNull()?.trim()?.lowercase() ?: ""
                        val isForLater = status in listOf("for_later", "watchlist", "planned", "to_watch", "da vedere", "not_started", "to_see", "unwatched")
                        val isWatched = status in listOf("archived", "dropped", "watched", "completed", "seen", "ignored", "stopped", "up_to_date")
                        if (isForLater || isWatched) {
                            addItem(id = movieId, title = title, mediaType = "movie", isWatched = isWatched, isForLater = isForLater)
                        }
                    }
                }
            }
        }
        zipEntries["user_movie_data.csv"]?.let { bytes ->
            val records = parseCsvRecords(bytes.decodeToString())
            if (records.isNotEmpty()) {
                val headers = records.first().map { it.trim().lowercase() }
                val idIdx = headers.indexOfFirst { it in listOf("movie_id", "id") }.takeIf { it != -1 } ?: 0
                val favIdx = headers.indexOfFirst { it in listOf("favorite", "is_favorite", "fav") }.takeIf { it != -1 } ?: 2
                val titleIdx = headers.indexOfFirst { it in listOf("title", "movie_name", "name") }.takeIf { it != -1 } ?: 4
                val watchCountIdx = headers.indexOfFirst { it in listOf("watch_count", "watched_count", "views", "watched") }
                val isForLaterIdx = headers.indexOfFirst { it in listOf("is_for_later", "for_later", "watchlist") }
                val statusIdx = headers.indexOfFirst { it in listOf("status", "state", "special_status") }
                for (i in 1 until records.size) {
                    val cols = records[i]
                    val title = cols.getOrNull(titleIdx)?.trim() ?: ""
                    if (title.isNotBlank()) {
                        val movieId = cols.getOrNull(idIdx)?.trim()
                        val isFav = cols.getOrNull(favIdx)?.trim() == "1" || cols.getOrNull(favIdx)?.trim()?.equals("true", ignoreCase = true) == true
                        val watchCount = if (watchCountIdx != -1) cols.getOrNull(watchCountIdx)?.trim()?.toIntOrNull() else null
                        val isForLaterStr = if (isForLaterIdx != -1) cols.getOrNull(isForLaterIdx)?.trim()?.lowercase() ?: "" else ""
                        val statusVal = if (statusIdx != -1) cols.getOrNull(statusIdx)?.trim()?.lowercase() ?: "" else ""
                        val isForLater = isForLaterStr in listOf("true", "1") || statusVal in listOf("for_later", "watchlist", "planned", "to_watch", "da vedere", "not_started", "to_see") || (watchCount != null && watchCount == 0)
                        val isWatched = (watchCount != null && watchCount > 0) || statusVal in listOf("watched", "completed", "seen", "archived") || (!isForLater && isForLaterStr == "false") || (!isForLater && watchCount == null && isForLaterStr.isBlank() && statusVal.isBlank())
                        addItem(id = movieId, title = title, mediaType = "movie", isWatched = isWatched, isFavorite = isFav || isForLater, isForLater = isForLater)
                    }
                }
            }
        }

        // 4. Parse tracking-prod-records-v2.csv (Series Tracking)
        zipEntries["tracking-prod-records-v2.csv"]?.let { bytes ->
            val lines = bytes.decodeToString().lineSequence().filter { it.isNotBlank() }.iterator()
            if (lines.hasNext()) {
                val headerLine = lines.next()
                val headers = parseCsvLine(headerLine)
                val lowerHeaders = headers.map { it.trim().lowercase() }
                val seasonIdx = lowerHeaders.indexOfFirst { it in listOf("season_number", "season", "s") }
                val episodeIdx = lowerHeaders.indexOfFirst { it in listOf("episode_number", "episode", "e") }
                val titleIdx = lowerHeaders.indexOfFirst { it in listOf("show_name", "series_name", "title") }.takeIf { it != -1 } ?: 11
                val showIdIdx = lowerHeaders.indexOfFirst { it in listOf("tv_show_id", "show_id", "id") }.takeIf { it != -1 } ?: 15
                val isForLaterIdx = lowerHeaders.indexOfFirst { it in listOf("is_for_later", "for_later") }.takeIf { it != -1 } ?: 9
                val updatedAtIdx = lowerHeaders.indexOfFirst { it in listOf("updated_at", "date") }.takeIf { it != -1 } ?: 0

                while (lines.hasNext()) {
                    val line = lines.next()
                    val cols = parseCsvLine(line)
                    if (cols.size > kotlin.math.max(titleIdx, showIdIdx)) {
                        val updatedAt = cols.getOrNull(updatedAtIdx)?.let { parseAndNormalizeWatchedDate(it) }
                        val isForLater = cols.getOrNull(isForLaterIdx)?.trim()?.let { it.equals("true", ignoreCase = true) || it == "1" } ?: false
                        val title = cols[titleIdx]
                        val showId = cols[showIdIdx]
                        val item = addItem(id = showId, title = title, mediaType = "tv", isWatched = !isForLater, watchedAt = updatedAt, isForLater = isForLater)
                        if (!isForLater && seasonIdx != -1 && episodeIdx != -1) {
                            val sNum = cols.getOrNull(seasonIdx)?.trim()?.toIntOrNull()
                            val eNum = cols.getOrNull(episodeIdx)?.trim()?.toIntOrNull()
                            if (sNum != null && sNum > 0 && eNum != null && eNum > 0) {
                                item.watchedEpisodes.getOrPut(sNum.toString()) { mutableSetOf() }.add(eNum)
                            }
                        }
                    }
                }
            }
        }

        // 5. Parse tracking-prod-records.csv (Movies Tracking)
        zipEntries["tracking-prod-records.csv"]?.let { bytes ->
            val records = parseCsvRecords(bytes.decodeToString())
            if (records.isNotEmpty()) {
                val headers = records.first().map { it.trim().lowercase() }
                val titleIdx = headers.indexOfFirst { it in listOf("show_name", "series_name", "movie_name", "title") }.takeIf { it != -1 } ?: 5
                val watchCountIdx = headers.indexOfFirst { it in listOf("watch_count", "watched_count", "views") }
                val isForLaterIdx = headers.indexOfFirst { it in listOf("is_for_later", "for_later", "watchlist") }
                val statusIdx = headers.indexOfFirst { it in listOf("status", "state") }
                val typeIdx = headers.indexOfFirst { it in listOf("type", "record_type", "action") }
                val watchDateRangeIdx = headers.indexOfFirst { it in listOf("watch_date_range_key", "watch_date") }
                val dateIdx = headers.indexOfFirst { it in listOf("updated_at", "created_at", "date") }.takeIf { it != -1 } ?: 10
                for (i in 1 until records.size) {
                    val cols = records[i]
                    val title = cols.getOrNull(titleIdx)?.trim() ?: ""
                    if (title.isNotBlank()) {
                        val watchCount = if (watchCountIdx != -1) cols.getOrNull(watchCountIdx)?.trim()?.toIntOrNull() else null
                        val isForLaterStr = if (isForLaterIdx != -1) cols.getOrNull(isForLaterIdx)?.trim()?.lowercase() ?: "" else ""
                        val statusVal = if (statusIdx != -1) cols.getOrNull(statusIdx)?.trim()?.lowercase() ?: "" else ""
                        val typeVal = if (typeIdx != -1) cols.getOrNull(typeIdx)?.trim()?.lowercase() ?: "" else ""
                        val hasWatchDateRange = if (watchDateRangeIdx != -1) !cols.getOrNull(watchDateRangeIdx)?.trim().isNullOrEmpty() else false
                        val isForLater = isForLaterStr in listOf("true", "1") || statusVal in listOf("for_later", "watchlist", "planned", "to_watch", "da vedere") || (watchCount != null && watchCount == 0) || typeVal == "towatch"
                        val isWatched = (watchCount != null && watchCount > 0) || statusVal in listOf("watched", "completed", "seen") || typeVal == "watch" || hasWatchDateRange || (!isForLater && isForLaterStr == "false") || (!isForLater && watchCountIdx == -1 && isForLaterIdx == -1 && typeIdx == -1 && watchDateRangeIdx == -1)
                        val updatedAt = cols.getOrNull(dateIdx)?.let { parseAndNormalizeWatchedDate(it) }
                        addItem(id = null, title = title, mediaType = "movie", isWatched = isWatched, watchedAt = updatedAt, isForLater = isForLater)
                    }
                }
            }
        }

        // 5b. Scan only dedicated watchlist or to-watch CSV files in the zip
        zipEntries.forEach { (fileName, bytes) ->
            if (fileName.endsWith(".csv", ignoreCase = true)) {
                val lowerName = fileName.lowercase()
                val isDedicatedWatchlistFile = (lowerName.contains("watchlist") || lowerName.contains("to_watch") || lowerName.contains("for_later") || lowerName.contains("planned")) &&
                        !lowerName.contains("special_status") && !lowerName.contains("tracking") && !lowerName.contains("data") && !lowerName.contains("seen_episodes")
                if (isDedicatedWatchlistFile) {
                    val records = try { parseCsvRecords(bytes.decodeToString()) } catch (e: Exception) { emptyList() }
                    if (records.isNotEmpty() && records.size > 1) {
                        val headers = records.first().map { it.trim().lowercase() }
                        val titleIdx = headers.indexOfFirst { it in listOf("title", "movie_name", "series_name", "show_name", "name") }
                        val idIdx = headers.indexOfFirst { it in listOf("movie_id", "tv_show_id", "show_id", "id") }
                        val typeIdx = headers.indexOfFirst { it in listOf("type", "media_type", "parent_type") }

                        if (titleIdx != -1 || idIdx != -1) {
                            for (i in 1 until records.size) {
                                val cols = records[i]
                                val title = if (titleIdx != -1) cols.getOrNull(titleIdx)?.trim() ?: "" else ""
                                val idVal = if (idIdx != -1) cols.getOrNull(idIdx)?.trim() else null
                                if (title.isNotBlank() || !idVal.isNullOrBlank()) {
                                    val typeStr = if (typeIdx != -1) cols.getOrNull(typeIdx)?.trim()?.lowercase() ?: "" else if (lowerName.contains("show") || lowerName.contains("tv")) "tv" else "movie"
                                    val mediaType = if (typeStr == "tv" || typeStr == "show" || typeStr == "series") "tv" else "movie"
                                    val item = addItem(id = idVal, title = title.ifBlank { idVal ?: "" }, mediaType = mediaType, isWatched = false, isFavorite = true, isForLater = true)
                                    item.folders.add("Watchlist")
                                }
                            }
                        }
                    }
                }
            }
        }

        zipEntries["seen_episodes.csv"]?.let { bytes ->
            val lines = bytes.decodeToString().lineSequence().filter { it.isNotBlank() }.iterator()
            if (lines.hasNext()) {
                val headerLine = lines.next()
                val headers = parseCsvLine(headerLine)
                val lowerHeaders = headers.map { it.trim().lowercase() }
                val seasonIdx = lowerHeaders.indexOfFirst { it in listOf("season_number", "season", "s") }
                val episodeIdx = lowerHeaders.indexOfFirst { it in listOf("episode_number", "episode", "e") }
                val showNameIdx = lowerHeaders.indexOfFirst { it in listOf("show_name", "series_name", "title") }.takeIf { it != -1 } ?: 5
                val showIdIdx = lowerHeaders.indexOfFirst { it in listOf("tv_show_id", "show_id", "id") }.takeIf { it != -1 } ?: 13

                while (lines.hasNext()) {
                    val line = lines.next()
                    val cols = parseCsvLine(line)
                    if (cols.size > kotlin.math.max(showNameIdx, showIdIdx) && seasonIdx != -1 && episodeIdx != -1) {
                        val sNum = cols.getOrNull(seasonIdx)?.trim()?.toIntOrNull()
                        val eNum = cols.getOrNull(episodeIdx)?.trim()?.toIntOrNull()
                        val showName = cols[showNameIdx]
                        val showId = cols[showIdIdx]
                        if (sNum != null && sNum > 0 && eNum != null && eNum > 0 && showName.isNotBlank()) {
                            val match = itemsMap.values.find { (showId.isNotBlank() && it.id == showId) || (it.title.equals(showName, ignoreCase = true) && it.mediaType == "tv") }
                                ?: addItem(id = showId, title = showName, mediaType = "tv", isWatched = true)
                            match.watchedEpisodes.getOrPut(sNum.toString()) { mutableSetOf() }.add(eNum)
                        }
                    }
                }
            }
        }

        // 6. Parse comments-prod-comments.csv (Notes/Reviews)
        zipEntries["comments-prod-comments.csv"]?.let { bytes ->
            val records = parseCsvRecords(bytes.decodeToString())
            if (records.isNotEmpty()) {
                val headers = records.first().map { it.trim().lowercase() }
                val noteIdx = headers.indexOfFirst { it in listOf("note", "comment", "text", "body", "content") }.takeIf { it != -1 } ?: 6
                val movieNameIdx = headers.indexOfFirst { it in listOf("movie_name", "movie", "film_name") }.takeIf { it != -1 } ?: 22
                val seriesNameIdx = headers.indexOfFirst { it in listOf("series_name", "show_name", "show") }.takeIf { it != -1 } ?: 23
                val parentTypeIdx = headers.indexOfFirst { it in listOf("parent_type", "type", "media_type") }.takeIf { it != -1 } ?: 5
                val seasonIdx = headers.indexOfFirst { it in listOf("season_number", "season") }.takeIf { it != -1 } ?: 24
                val episodeIdx = headers.indexOfFirst { it in listOf("episode_number", "episode") }.takeIf { it != -1 } ?: 21

                for (i in 1 until records.size) {
                    val cols = records[i]
                    val maxReq = kotlin.math.max(noteIdx, kotlin.math.max(movieNameIdx, seriesNameIdx))
                    if (cols.size > maxReq) {
                        val noteText = cols.getOrNull(noteIdx)?.trim() ?: ""
                        val movieName = cols.getOrNull(movieNameIdx)?.trim() ?: ""
                        val seriesName = cols.getOrNull(seriesNameIdx)?.trim() ?: ""
                        val title = if (movieName.isNotBlank()) movieName else seriesName
                        if (title.isNotBlank() && noteText.isNotBlank()) {
                            val parentType = cols.getOrNull(parentTypeIdx)?.trim()?.lowercase() ?: ""
                            val isTv = parentType == "series" || parentType == "show" || parentType == "tv" || seriesName.isNotBlank()
                            val mediaType = if (isTv) "tv" else "movie"
                            val match = itemsMap.values.find { it.title.equals(title, ignoreCase = true) && (if (isTv) it.mediaType == "tv" else true) }
                                ?: addItem(id = null, title = title, mediaType = mediaType, isWatched = true)
                            
                            val sNum = cols.getOrNull(seasonIdx)?.trim()?.toIntOrNull()
                            val eNum = cols.getOrNull(episodeIdx)?.trim()?.toIntOrNull()
                            val formattedNote = if (sNum != null && sNum > 0 && eNum != null && eNum > 0) {
                                "[S${sNum.toString().padStart(2, '0')}E${eNum.toString().padStart(2, '0')}]: $noteText"
                            } else {
                                noteText
                            }
                            
                            match.personalNote = if (match.personalNote.isNullOrBlank()) formattedNote else "${match.personalNote}\n\n$formattedNote"
                        }
                    }
                }
            }
        }

        // 7. Parse ratings-live-votes.csv & ratings-3-prod-episode_votes.csv (Ratings)
        fun extractTvTimeScore(voteKey: String): Double? {
            val suffix = voteKey.substringAfterLast("-").trim().toIntOrNull() ?: return null
            return when (suffix) {
                29 -> 10.0 // Mindblown / Awesome
                3 -> 8.0   // Good / Happy
                28 -> 7.0  // Emotional / Sad
                2 -> 6.0   // Neutral / Okay
                1 -> 4.0   // Bad
                else -> if (suffix in 1..10) suffix.toDouble() else null
            }
        }

        zipEntries["ratings-live-votes.csv"]?.let { bytes ->
            bytes.decodeToString().lineSequence().filter { it.isNotBlank() && !it.startsWith("uuid,") }.forEach { line ->
                val cols = parseCsvLine(line)
                if (cols.size >= 4) {
                    val voteKey = cols[2]
                    val movieName = cols[3].trim()
                    val score = extractTvTimeScore(voteKey)
                    if (movieName.isNotBlank() && score != null) {
                        val match = itemsMap.values.find { it.title.equals(movieName, ignoreCase = true) }
                            ?: addItem(id = null, title = movieName, mediaType = "movie", isWatched = true)
                        if (match.personalRating == null || score > (match.personalRating ?: 0.0)) {
                            match.personalRating = score
                        }
                    }
                }
            }
        }

        zipEntries["ratings-3-prod-episode_votes.csv"]?.let { bytes ->
            bytes.decodeToString().lineSequence().filter { it.isNotBlank() && !it.startsWith("user_id,") }.forEach { line ->
                val cols = parseCsvLine(line)
                if (cols.size >= 4) {
                    val voteKey = cols[1]
                    val seriesName = cols[3].trim()
                    val score = extractTvTimeScore(voteKey)
                    if (seriesName.isNotBlank() && score != null) {
                        val match = itemsMap.values.find { it.title.equals(seriesName, ignoreCase = true) && it.mediaType == "tv" }
                            ?: addItem(id = null, title = seriesName, mediaType = "tv", isWatched = true)
                        if (match.personalRating == null || score > (match.personalRating ?: 0.0)) {
                            match.personalRating = score
                        }
                    }
                }
            }
        }

        // 8. Parse lists-prod-lists.csv (Custom Folders / Lists)
        zipEntries["lists-prod-lists.csv"]?.let { bytes ->
            bytes.decodeToString().lineSequence().filter { it.isNotBlank() && !it.startsWith("name,") }.forEach { line ->
                val cols = parseCsvLine(line)
                if (cols.size >= 3) {
                    val folderName = cols[0].trim()
                    val objectsStr = cols[2]
                    val isWatchlistFolder = folderName.equals("watchlist", ignoreCase = true) ||
                            folderName.equals("to watch", ignoreCase = true) ||
                            folderName.equals("for_later", ignoreCase = true) ||
                            folderName.equals("da vedere", ignoreCase = true) ||
                            folderName.equals("planned", ignoreCase = true) ||
                            folderName.equals("to_watch", ignoreCase = true) ||
                            folderName.equals("da guardare", ignoreCase = true)

                    if (folderName.isNotBlank() && objectsStr.isNotBlank()) {
                        val blocks = Regex("\\{[^}]+\\}").findAll(objectsStr).toList()
                        if (blocks.isNotEmpty()) {
                            for (b in blocks) {
                                val blockStr = b.value
                                val idStr = Regex("\"?id\"?\\s*[:=]\\s*\"?(\\d+)\"?").find(blockStr)?.groupValues?.get(1)
                                val title = Regex("\"?(?:name|title)\"?\\s*[:=]\\s*\"?([^\"',\\}]+)\"?").find(blockStr)?.groupValues?.get(1)?.trim()?.removeSuffix("\"")?.removeSuffix("'")?.trim()
                                val typeStr = Regex("\"?type\"?\\s*[:=]\\s*\"?([a-zA-Z]+)\"?").find(blockStr)?.groupValues?.get(1)?.lowercase()
                                val mediaType = if (typeStr == "show" || typeStr == "series" || typeStr == "tv") "tv" else "movie"

                                val match = itemsMap.values.find { 
                                    (idStr != null && it.id == idStr) || (!title.isNullOrBlank() && it.title.equals(title, ignoreCase = true) && it.mediaType == mediaType) 
                                }
                                if (match != null) {
                                    match.folders.add(folderName)
                                    if (isWatchlistFolder) {
                                        match.isWatched = false
                                        match.isForLater = true
                                        match.isFavorite = true
                                    }
                                } else if (!title.isNullOrBlank() || idStr != null) {
                                    val newItem = addItem(
                                        id = idStr,
                                        title = title ?: (idStr ?: ""),
                                        mediaType = mediaType,
                                        isWatched = !isWatchlistFolder,
                                        isForLater = isWatchlistFolder
                                    )
                                    newItem.folders.add(folderName)
                                }
                            }
                        } else {
                            val idMatches = Regex("\"?id\"?\\s*[:=]\\s*\"?(\\d+)\"?").findAll(objectsStr)
                            for (m in idMatches) {
                                val idStr = m.groupValues[1]
                                val match = itemsMap.values.find { it.id == idStr }
                                if (match != null) {
                                    match.folders.add(folderName)
                                    if (isWatchlistFolder) {
                                        match.isWatched = false
                                        match.isForLater = true
                                        match.isFavorite = true
                                    }
                                } else if (isWatchlistFolder) {
                                    val newItem = addItem(
                                        id = idStr,
                                        title = idStr,
                                        mediaType = "movie",
                                        isWatched = false,
                                        isForLater = true
                                    )
                                    newItem.folders.add(folderName)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 9. Resolve all items against TMDB and save
        val allItems = itemsMap.values.filter { it.title.isNotBlank() }
        var savedCount = 0
        val totalToProcess = allItems.size
        
        val chunks = allItems.chunked(15)
        for (chunk in chunks) {
            val deferreds = chunk.map { item ->
                async {
                    try {
                        val isTv = item.mediaType == "tv"
                        val tmdbResult = try { movieRepository.searchMediaWithYear(item.title, null, isTv = isTv) } catch (e: Exception) { null }
                        val tmdb = tmdbResult ?: Movie(
                            id = item.id?.toLongOrNull()?.takeIf { it > 0 } ?: (kotlin.math.abs(item.title.hashCode()).toLong().coerceAtLeast(1000000L)),
                            mediaType = item.mediaType,
                            title = item.title,
                            name = if (isTv) item.title else null,
                            watched = item.isWatched,
                            favorite = item.isFavorite || item.isForLater,
                            syncStatus = "pending",
                            clientUpdatedAt = System.currentTimeMillis()
                        )
                        tmdb.let { tmdb ->
                            val finalMediaType = if (isTv) "tv" else (tmdb.mediaType ?: "movie")
                            val seasonsList = tmdb.seasons
                            val finalEps: Map<String, List<Int>>? = if (item.watchedEpisodes.isNotEmpty()) {
                                item.watchedEpisodes.mapValues { it.value.sorted() }
                            } else if (item.isWatched && seasonsList != null) {
                                val allWatched = mutableMapOf<String, List<Int>>()
                                val todayIso = try { java.time.LocalDate.now().toString() } catch (e: Exception) { "2026-01-01" }
                                seasonsList.filter { (it.seasonNumber ?: 0) > 0 }.forEach { season ->
                                    val count = tmdb.getReleasedEpisodeCountForSeason(season, todayIso, null, null)
                                    if (count > 0) {
                                        allWatched[(season.seasonNumber ?: 0).toString()] = (1..count).toList()
                                    }
                                }
                                allWatched.takeIf { it.isNotEmpty() }
                            } else null

                            val totalReleasedEps = tmdb.effectiveTotalEpisodes
                            val watchedEpsCount = finalEps?.filterKeys { it != "0" }?.values?.sumOf { it.size } ?: 0
                            val (finalWatched, finalProgress) = if (finalMediaType == "tv") {
                                if (item.isForLater && watchedEpsCount == 0) {
                                    Pair(false, 0.0)
                                } else if (finalEps != null && finalEps.isNotEmpty() && totalReleasedEps > 0) {
                                    val isCompleted = watchedEpsCount >= totalReleasedEps
                                    if (isCompleted && !item.isForLater) {
                                        Pair(true, 1.0)
                                    } else {
                                        val p = (watchedEpsCount.toDouble() / totalReleasedEps).coerceIn(0.001, 0.999)
                                        Pair(false, p)
                                    }
                                } else if (item.isWatched && !item.isForLater && watchedEpsCount > 0) {
                                    Pair(true, 1.0)
                                } else {
                                    Pair(false, 0.0)
                                }
                            } else {
                                if (item.isForLater) Pair(false, 0.0) else Pair(item.isWatched, if (item.isWatched) 1.0 else 0.0)
                            }

                            val isWatchlistFolder = item.folders.any { fname ->
                                fname.equals("watchlist", ignoreCase = true) ||
                                fname.equals("to watch", ignoreCase = true) ||
                                fname.equals("for_later", ignoreCase = true) ||
                                fname.equals("da vedere", ignoreCase = true) ||
                                fname.equals("planned", ignoreCase = true) ||
                                fname.equals("to_watch", ignoreCase = true)
                            }
                            val isItemWatchlist = item.isForLater || isWatchlistFolder || (!finalWatched && watchedEpsCount == 0 && !item.isWatched)
                            val finalFav = item.isFavorite || (if (!finalWatched && isItemWatchlist) true else false)

                            val movieObj = Movie(
                                id = tmdb.id,
                                mediaType = finalMediaType,
                                title = tmdb.title,
                                name = tmdb.name,
                                posterPath = tmdb.posterPath,
                                backdropPath = tmdb.backdropPath,
                                voteAverage = tmdb.voteAverage,
                                overview = tmdb.overview,
                                releaseDate = tmdb.releaseDate,
                                firstAirDate = tmdb.firstAirDate,
                                genreIds = tmdb.genreIds,
                                watched = finalWatched,
                                favorite = finalFav,
                                personalRating = item.personalRating,
                                personalNote = item.personalNote,
                                watchedEpisodes = finalEps,
                                progress = finalProgress,
                                seasons = tmdb.seasons,
                                numberOfSeasons = tmdb.numberOfSeasons,
                                numberOfEpisodes = tmdb.numberOfEpisodes,
                                watchedAt = if (finalWatched || watchedEpsCount > 0) (item.watchedAt ?: java.time.Instant.now().toString()) else null,
                                syncStatus = "pending",
                                clientUpdatedAt = System.currentTimeMillis()
                            )
                            val foldersList = item.folders.toList()
                            if (foldersList.isNotEmpty()) {
                                foldersList.map { fname -> Pair(movieObj, fname) }
                            } else {
                                listOf(Pair(movieObj, null))
                            }
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            
            val batchPairs = deferreds.awaitAll().filterNotNull().flatten()
            if (batchPairs.isNotEmpty()) {
                onBatchReady(batchPairs, keepLatestWatchDate, { _, _ -> })
                savedCount += batchPairs.distinctBy { Pair(it.first.id, it.first.mediaType) }.size
            }
            onProgress(savedCount, totalToProcess)
            kotlinx.coroutines.delay(300)
        }
        
        savedCount
    }
}
