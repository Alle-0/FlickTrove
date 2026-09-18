package com.cinetrack.data.repository.importers

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.importers.ImporterUtils.parseAndNormalizeWatchedDate
import com.cinetrack.data.repository.importers.ImporterUtils.parseCsvLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BingersImporter @Inject constructor() {

    private class BingersItemData(
        val tmdbId: Long,
        val mediaType: String,
        var title: String = "",
        var year: String? = null,
        var imdbId: String? = null,
        var isWatched: Boolean = false,
        var isFavorite: Boolean = false,
        var isDropped: Boolean = false,
        var personalRating: Double? = null,
        var personalNote: String? = null,
        var watchedAt: String? = null,
        val watchedEpisodes: MutableMap<String, MutableSet<Int>> = mutableMapOf(),
        val extractedWatchDates: MutableSet<String> = mutableSetOf(),
        val folders: MutableSet<String> = mutableSetOf()
    )

    fun isBingersZip(zipEntries: Map<String, ByteArray>): Boolean {
        val keys = zipEntries.keys.map { it.lowercase() }
        val hasLibrary = keys.any { it.endsWith("library.csv") }
        val hasSupplemental = keys.any { it.endsWith("watches.csv") } ||
                keys.any { it.endsWith("ratings.csv") } ||
                keys.any { it.endsWith("lists.csv") }
        return hasLibrary && hasSupplemental
    }

    suspend fun migrateBingersZip(
        zipEntries: Map<String, ByteArray>,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
        onBatchReady: suspend (List<Pair<Movie, String?>>, suspend (Int, Int) -> Unit) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val itemsMap = mutableMapOf<String, BingersItemData>()

        fun getItem(mediaType: String, tmdbId: Long): BingersItemData {
            val key = "${mediaType}_$tmdbId"
            return itemsMap.getOrPut(key) {
                BingersItemData(tmdbId = tmdbId, mediaType = mediaType)
            }
        }

        // 1. Parse library.csv
        val libraryEntry = zipEntries.entries.firstOrNull { it.key.lowercase().endsWith("library.csv") }?.value
        if (libraryEntry != null) {
            parseLibraryCsv(libraryEntry.decodeToString(), ::getItem)
        }

        // 2. Parse watches.csv (Episodes & history)
        val watchesEntry = zipEntries.entries.firstOrNull { it.key.lowercase().endsWith("watches.csv") }?.value
        if (watchesEntry != null) {
            parseWatchesCsv(watchesEntry.decodeToString(), itemsMap, ::getItem)
        }

        // 3. Parse ratings.csv
        val ratingsEntry = zipEntries.entries.firstOrNull { it.key.lowercase().endsWith("ratings.csv") }?.value
        if (ratingsEntry != null) {
            parseRatingsCsv(ratingsEntry.decodeToString(), itemsMap)
        }

        // 4. Parse lists.csv (Folders)
        val listsEntry = zipEntries.entries.firstOrNull { it.key.lowercase().endsWith("lists.csv") }?.value
        if (listsEntry != null) {
            parseListsCsv(listsEntry.decodeToString(), itemsMap, ::getItem)
        }

        emitImportedBatches(itemsMap, onProgress, onBatchReady)
    }

    private fun parseLibraryCsv(csvContent: String, getItem: (String, Long) -> BingersItemData) {
        val lines = csvContent.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return
        val headers = parseCsvLine(lines.next()).map { it.trim().lowercase() }

        val typeIdx = headers.indexOfFirst { it in listOf("type", "media_type", "mediatype") }
        val tmdbIdx = headers.indexOfFirst { it in listOf("tmdb_id", "tmdbid", "tmdb", "id") }
        val imdbIdx = headers.indexOfFirst { it in listOf("imdb_id", "imdbid", "imdb") }
        val titleIdx = headers.indexOfFirst { it in listOf("title", "name") }
        val yearIdx = headers.indexOfFirst { it in listOf("year", "release_year") }
        val statusIdx = headers.indexOfFirst { it in listOf("status", "watch_status") }
        val ratingIdx = headers.indexOfFirst { it in listOf("rating", "user_rating", "score") }
        val watchedDateIdx = headers.indexOfFirst { it in listOf("watched_at", "date_watched", "watched_date") }

        while (lines.hasNext()) {
            val cols = parseCsvLine(lines.next())
            val tmdbId = if (tmdbIdx != -1 && cols.size > tmdbIdx) cols[tmdbIdx].toLongOrNull() else null
            if (tmdbId == null || tmdbId <= 0L) continue

            val rawType = if (typeIdx != -1 && cols.size > typeIdx) cols[typeIdx].trim().lowercase() else "movie"
            val mediaType = if (rawType in listOf("tv", "show", "series", "anime")) "tv" else "movie"
            val title = if (titleIdx != -1 && cols.size > titleIdx) cols[titleIdx].trim() else "Unknown ($tmdbId)"
            val year = if (yearIdx != -1 && cols.size > yearIdx) cols[yearIdx].trim() else null
            val imdbId = if (imdbIdx != -1 && cols.size > imdbIdx) cols[imdbIdx].trim().takeIf { it.isNotBlank() } else null
            val status = if (statusIdx != -1 && cols.size > statusIdx) cols[statusIdx].trim().lowercase() else "watched"
            val rating = if (ratingIdx != -1 && cols.size > ratingIdx) cols[ratingIdx].trim().toDoubleOrNull() else null
            val watchedDate = if (watchedDateIdx != -1 && cols.size > watchedDateIdx) parseAndNormalizeWatchedDate(cols[watchedDateIdx]) else null

            val item = getItem(mediaType, tmdbId)
            item.title = title
            item.year = year
            item.imdbId = imdbId
            if (rating != null) item.personalRating = rating
            if (watchedDate != null) item.watchedAt = watchedDate

            when (status) {
                "watched", "completed", "seen" -> {
                    item.isWatched = true
                    item.isFavorite = false
                }
                "watching" -> {
                    item.isWatched = false
                    item.isFavorite = true
                }
                "watchlist", "planned", "plan_to_watch", "towatch" -> {
                    item.isWatched = false
                    item.isFavorite = true
                }
                "dropped", "paused" -> {
                    item.isWatched = false
                    item.isDropped = true
                    item.isFavorite = true
                }
                else -> item.isWatched = true
            }
        }
    }

    private fun parseWatchesCsv(
        csvContent: String,
        itemsMap: MutableMap<String, BingersItemData>,
        getItem: (String, Long) -> BingersItemData
    ) {
        val lines = csvContent.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return
        val headers = parseCsvLine(lines.next()).map { it.trim().lowercase() }

        val tmdbIdx = headers.indexOfFirst { it in listOf("tmdb_id", "tmdbid", "tmdb", "id") }
        val seasonIdx = headers.indexOfFirst { it in listOf("season", "season_number", "s") }
        val epIdx = headers.indexOfFirst { it in listOf("episode", "episode_number", "e") }
        val dateIdx = headers.indexOfFirst { it in listOf("date_watched", "watched_at", "date", "watched_date") }
        val typeIdx = headers.indexOfFirst { it in listOf("type", "media_type") }

        while (lines.hasNext()) {
            val cols = parseCsvLine(lines.next())
            val tmdbId = if (tmdbIdx != -1 && cols.size > tmdbIdx) cols[tmdbIdx].toLongOrNull() else null
            if (tmdbId == null || tmdbId <= 0L) continue

            val season = if (seasonIdx != -1 && cols.size > seasonIdx) cols[seasonIdx].toIntOrNull() else null
            val episode = if (epIdx != -1 && cols.size > epIdx) cols[epIdx].toIntOrNull() else null
            val dateStr = if (dateIdx != -1 && cols.size > dateIdx) parseAndNormalizeWatchedDate(cols[dateIdx]) else null
            val rawType = if (typeIdx != -1 && cols.size > typeIdx) cols[typeIdx].trim().lowercase() else ""
            val isTv = season != null || rawType in listOf("tv", "show", "series", "episode")

            val mediaType = if (isTv) "tv" else "movie"
            val item = itemsMap["${mediaType}_$tmdbId"] ?: getItem(mediaType, tmdbId)

            if (season != null && episode != null && season > 0 && episode > 0) {
                item.watchedEpisodes.getOrPut(season.toString()) { mutableSetOf() }.add(episode)
            }
            if (dateStr != null) {
                item.extractedWatchDates.add(dateStr)
                if (item.watchedAt == null || dateStr > item.watchedAt!!) {
                    item.watchedAt = dateStr
                }
            }
        }
    }

    private fun parseRatingsCsv(
        csvContent: String,
        itemsMap: MutableMap<String, BingersItemData>
    ) {
        val lines = csvContent.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return
        val headers = parseCsvLine(lines.next()).map { it.trim().lowercase() }

        val tmdbIdx = headers.indexOfFirst { it in listOf("tmdb_id", "tmdbid", "tmdb", "id") }
        val ratingIdx = headers.indexOfFirst { it in listOf("rating", "score", "user_rating") }

        while (lines.hasNext()) {
            val cols = parseCsvLine(lines.next())
            val tmdbId = if (tmdbIdx != -1 && cols.size > tmdbIdx) cols[tmdbIdx].toLongOrNull() else null
            val rating = if (ratingIdx != -1 && cols.size > ratingIdx) cols[ratingIdx].toDoubleOrNull() else null
            if (tmdbId != null && rating != null && rating > 0.0) {
                val item = itemsMap["movie_$tmdbId"] ?: itemsMap["tv_$tmdbId"]
                if (item != null && item.personalRating == null) {
                    item.personalRating = rating
                }
            }
        }
    }

    private fun parseListsCsv(
        csvContent: String,
        itemsMap: MutableMap<String, BingersItemData>,
        getItem: (String, Long) -> BingersItemData
    ) {
        val lines = csvContent.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return
        val headers = parseCsvLine(lines.next()).map { it.trim().lowercase() }

        val listIdx = headers.indexOfFirst { it in listOf("list_name", "list", "name", "folder") }
        val tmdbIdx = headers.indexOfFirst { it in listOf("tmdb_id", "tmdbid", "tmdb", "id") }
        val typeIdx = headers.indexOfFirst { it in listOf("type", "media_type") }
        val titleIdx = headers.indexOfFirst { it in listOf("title", "name") }

        while (lines.hasNext()) {
            val cols = parseCsvLine(lines.next())
            val listName = if (listIdx != -1 && cols.size > listIdx) cols[listIdx].trim() else ""
            val tmdbId = if (tmdbIdx != -1 && cols.size > tmdbIdx) cols[tmdbIdx].toLongOrNull() else null
            if (listName.isBlank() || tmdbId == null || tmdbId <= 0L) continue

            val rawType = if (typeIdx != -1 && cols.size > typeIdx) cols[typeIdx].trim().lowercase() else ""
            val mediaType = if (rawType in listOf("tv", "show", "series")) "tv" else "movie"
            val title = if (titleIdx != -1 && cols.size > titleIdx) cols[titleIdx].trim() else ""

            val item = itemsMap["${mediaType}_$tmdbId"] ?: itemsMap["movie_$tmdbId"] ?: itemsMap["tv_$tmdbId"] ?: getItem(mediaType, tmdbId)
            if (item.title.isBlank() && title.isNotBlank()) item.title = title
            item.folders.add(listName)
        }
    }

    private suspend fun emitImportedBatches(
        itemsMap: Map<String, BingersItemData>,
        onProgress: suspend (Int, Int) -> Unit,
        onBatchReady: suspend (List<Pair<Movie, String?>>, suspend (Int, Int) -> Unit) -> Unit
    ): Int {
        val moviesWithFolders = mutableListOf<Pair<Movie, String?>>()

        for (item in itemsMap.values) {
            val hasEpisodes = item.watchedEpisodes.isNotEmpty()
            val finalWatched = when {
                item.isDropped -> false
                hasEpisodes -> true
                else -> item.isWatched
            }

            val movie = Movie(
                id = item.tmdbId,
                mediaType = item.mediaType,
                title = if (item.mediaType == "movie") item.title else null,
                name = if (item.mediaType == "tv") item.title else null,
                watched = finalWatched,
                favorite = item.isFavorite || item.isDropped,
                dropped = item.isDropped,
                personalRating = item.personalRating,
                personalNote = item.personalNote,
                watchedEpisodes = if (hasEpisodes) item.watchedEpisodes.mapValues { it.value.sorted() } else null,
                watchedAt = item.watchedAt,
                releaseYear = item.year,
                imdbId = item.imdbId,
                syncStatus = "pending",
                clientUpdatedAt = System.currentTimeMillis()
            )
            movie.extractedWatchDates.addAll(item.extractedWatchDates)

            if (item.folders.isEmpty()) {
                moviesWithFolders.add(Pair(movie, null))
            } else {
                item.folders.forEach { folder ->
                    moviesWithFolders.add(Pair(movie, folder))
                }
            }
        }

        if (moviesWithFolders.isEmpty()) return 0

        val chunks = moviesWithFolders.chunked(100)
        var processed = 0
        for (chunk in chunks) {
            onBatchReady(chunk) { cur, _ ->
                onProgress(processed + cur, moviesWithFolders.size)
            }
            processed += chunk.size
            onProgress(processed, moviesWithFolders.size)
        }

        return itemsMap.size
    }
}
