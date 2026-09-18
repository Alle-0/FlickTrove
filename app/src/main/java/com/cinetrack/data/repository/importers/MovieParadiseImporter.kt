package com.cinetrack.data.repository.importers

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.importers.ImporterUtils.parseAndNormalizeWatchedDate
import com.cinetrack.data.repository.importers.ImporterUtils.parseCsvLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import javax.inject.Inject

class MovieParadiseImporter @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    private class MovieParadiseItemData(
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
        val folders: MutableSet<String> = mutableSetOf(),
        var emotionalVibes: String? = null,
        var favoriteActorCharacter: String? = null,
        var favoriteActorName: String? = null
    )

    fun isMovieParadiseZip(zipEntries: Map<String, ByteArray>): Boolean {
        val keys = zipEntries.keys
        val dataEntry = zipEntries.entries.firstOrNull { it.key.equals("data.json", ignoreCase = true) }
        if (dataEntry != null) {
            val preview = dataEntry.value.take(1000).toByteArray().decodeToString()
            if (preview.contains("exportVersion") && (preview.contains("library") || preview.contains("tvStatusOverrides") || preview.contains("movieReactions") || preview.contains("profile"))) {
                return true
            }
        }
        val hasHistory = keys.any { it.equals("watch_history.csv", ignoreCase = true) }
        val hasRatings = keys.any { it.equals("ratings.csv", ignoreCase = true) }
        val hasListItems = keys.any { it.equals("list_items.csv", ignoreCase = true) }
        return hasHistory && hasRatings && hasListItems
    }

    fun isMovieParadiseJson(content: String): Boolean {
        val preview = content.take(1000)
        return preview.contains("exportVersion") && (preview.contains("library") || preview.contains("tvStatusOverrides") || preview.contains("movieReactions") || preview.contains("profile"))
    }

    suspend fun migrateMovieParadiseZip(
        zipEntries: Map<String, ByteArray>,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
        onBatchReady: suspend (List<Pair<Movie, String?>>, suspend (Int, Int) -> Unit) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val dataJsonBytes = zipEntries.entries.firstOrNull { it.key.equals("data.json", ignoreCase = true) }?.value
        val itemsMap = mutableMapOf<String, MovieParadiseItemData>()

        fun getItem(mediaType: String, tmdbId: Long): MovieParadiseItemData {
            val key = "${mediaType}_$tmdbId"
            return itemsMap.getOrPut(key) {
                MovieParadiseItemData(tmdbId = tmdbId, mediaType = mediaType)
            }
        }

        if (dataJsonBytes != null) {
            parseDataJson(dataJsonBytes.decodeToString(), itemsMap, ::getItem)
        }

        // Parse optional supplemental CSV files from zip
        parseSupplementalCsvs(zipEntries, itemsMap, ::getItem)

        emitImportedBatches(itemsMap, onProgress, onBatchReady)
    }

    suspend fun migrateJsonStream(
        inputStream: InputStream,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
        onBatchReady: suspend (List<Pair<Movie, String?>>, suspend (Int, Int) -> Unit) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val content = inputStream.bufferedReader().use { it.readText() }
        val itemsMap = mutableMapOf<String, MovieParadiseItemData>()

        fun getItem(mediaType: String, tmdbId: Long): MovieParadiseItemData {
            val key = "${mediaType}_$tmdbId"
            return itemsMap.getOrPut(key) {
                MovieParadiseItemData(tmdbId = tmdbId, mediaType = mediaType)
            }
        }

        parseDataJson(content, itemsMap, ::getItem)
        emitImportedBatches(itemsMap, onProgress, onBatchReady)
    }

    private fun parseDataJson(
        jsonContent: String,
        itemsMap: MutableMap<String, MovieParadiseItemData>,
        getItem: (String, Long) -> MovieParadiseItemData
    ) {
        val root = try {
            json.parseToJsonElement(jsonContent).jsonObject
        } catch (_: Exception) {
            return
        }

        // 1. Library: Movies & TV Shows
        val library = root["library"]?.jsonArray
        library?.forEach { el ->
            val obj = el.jsonObject
            val type = obj["type"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: "movie"
            val mediaType = if (type == "tv" || type == "show" || type == "series") "tv" else "movie"
            val tmdbId = obj["tmdbId"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: obj["tmdb_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: return@forEach
            val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown ($tmdbId)"
            val year = obj["year"]?.jsonPrimitive?.contentOrNull
            val imdbId = obj["imdbId"]?.jsonPrimitive?.contentOrNull ?: obj["imdb_id"]?.jsonPrimitive?.contentOrNull
            val status = obj["status"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: "watched"
            val rating = obj["rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            val watchedAt = obj["watchedAt"]?.jsonPrimitive?.contentOrNull ?: obj["watched_at"]?.jsonPrimitive?.contentOrNull

            val item = getItem(mediaType, tmdbId)
            item.title = title
            item.year = year
            item.imdbId = imdbId
            if (rating != null) item.personalRating = rating
            if (watchedAt != null) item.watchedAt = watchedAt

            when (status) {
                "watched", "completed" -> {
                    item.isWatched = true
                    item.isFavorite = false
                }
                "watching" -> {
                    item.isWatched = false
                    item.isFavorite = true
                }
                "towatch", "planned", "watchlist", "plan_to_watch" -> {
                    item.isWatched = false
                    item.isFavorite = true
                }
                "dropped", "paused" -> {
                    item.isWatched = false
                    item.isDropped = true
                    item.isFavorite = true
                }
                else -> {
                    item.isWatched = true
                }
            }
        }

        // 2. TV Status Overrides (dropped, paused, etc.)
        val overrides = root["tvStatusOverrides"]?.jsonArray
        overrides?.forEach { el ->
            val obj = el.jsonObject
            val tmdbId = obj["tmdbId"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
            val override = obj["override"]?.jsonPrimitive?.contentOrNull?.lowercase()
            if (override == "dropped" || override == "paused") {
                val item = getItem("tv", tmdbId)
                item.isDropped = true
                item.isFavorite = true
                item.isWatched = false
            }
        }

        // 3. Episodes (for TV Shows)
        val episodes = root["episodes"]?.jsonArray
        episodes?.forEach { el ->
            val obj = el.jsonObject
            val showTmdbId = obj["showTmdbId"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
            val season = obj["season"]?.jsonPrimitive?.intOrNull ?: return@forEach
            val episode = obj["episode"]?.jsonPrimitive?.intOrNull ?: return@forEach
            val isEpWatched = obj["watched"]?.jsonPrimitive?.booleanOrNull ?: true
            val epWatchedAt = obj["watchedAt"]?.jsonPrimitive?.contentOrNull
            val showTitle = obj["showTitle"]?.jsonPrimitive?.contentOrNull

            if (isEpWatched && season > 0 && episode > 0) {
                val item = getItem("tv", showTmdbId)
                if (item.title.isBlank() && !showTitle.isNullOrBlank()) {
                    item.title = showTitle
                }
                item.watchedEpisodes.getOrPut(season.toString()) { mutableSetOf() }.add(episode)
                if (!epWatchedAt.isNullOrBlank()) {
                    item.extractedWatchDates.add(epWatchedAt)
                    if (item.watchedAt == null || epWatchedAt > item.watchedAt!!) {
                        item.watchedAt = epWatchedAt
                    }
                }
            }
        }

        // 4. Watch Events (Rewatches)
        val watchEvents = root["watchEvents"]?.jsonArray
        watchEvents?.forEach { el ->
            val obj = el.jsonObject
            val tmdbId = obj["tmdbId"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
            val type = obj["type"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: "tv"
            val mediaType = if (type == "movie") "movie" else "tv"
            val watchedAt = obj["watchedAt"]?.jsonPrimitive?.contentOrNull
            if (!watchedAt.isNullOrBlank()) {
                val item = getItem(mediaType, tmdbId)
                item.extractedWatchDates.add(watchedAt)
            }
        }

        // 5. Movie & Episode Reactions (Emotional Vibes & Favorite Character / Person)
        val movieReactions = root["movieReactions"]?.jsonArray
        movieReactions?.forEach { el ->
            val obj = el.jsonObject
            val tmdbId = obj["tmdbId"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
            val emotions = obj["emotions"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.uppercase() }
            val favChar = obj["favoriteCharacter"]?.jsonPrimitive?.contentOrNull
            val favPerson = obj["favoritePerson"]?.jsonPrimitive?.contentOrNull

            val item = itemsMap["movie_$tmdbId"] ?: itemsMap["tv_$tmdbId"] ?: getItem("movie", tmdbId)
            if (!emotions.isNullOrEmpty()) {
                item.emotionalVibes = emotions.joinToString(", ")
            }
            if (!favChar.isNullOrBlank()) {
                item.favoriteActorCharacter = favChar
            }
            if (!favPerson.isNullOrBlank()) {
                item.favoriteActorName = favPerson
            }
        }

        val episodeReactions = root["episodeReactions"]?.jsonArray
        episodeReactions?.forEach { el ->
            val obj = el.jsonObject
            val showTmdbId = obj["showTmdbId"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
            val favChar = obj["favoriteCharacter"]?.jsonPrimitive?.contentOrNull
            val favPerson = obj["favoritePerson"]?.jsonPrimitive?.contentOrNull
            val emotions = obj["emotions"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.uppercase() }

            val item = getItem("tv", showTmdbId)
            if (item.favoriteActorCharacter.isNullOrBlank() && !favChar.isNullOrBlank()) {
                item.favoriteActorCharacter = favChar
            }
            if (item.favoriteActorName.isNullOrBlank() && !favPerson.isNullOrBlank()) {
                item.favoriteActorName = favPerson
            }
            if (item.emotionalVibes.isNullOrBlank() && !emotions.isNullOrEmpty()) {
                item.emotionalVibes = emotions.joinToString(", ")
            }
        }

        // 6. Comments (Personal notes)
        val comments = root["comments"]?.jsonArray
        comments?.forEach { el ->
            val obj = el.jsonObject
            val tmdbId = obj["tmdbId"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
            val type = obj["type"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: "movie"
            val mediaType = if (type == "tv") "tv" else "movie"
            val body = obj["body"]?.jsonPrimitive?.contentOrNull
            if (!body.isNullOrBlank()) {
                val item = getItem(mediaType, tmdbId)
                if (item.personalNote.isNullOrBlank()) {
                    item.personalNote = body.take(5000)
                }
            }
        }

        // 7. Custom Lists (Folders)
        val lists = root["lists"]?.jsonArray
        lists?.forEach { el ->
            val listObj = el.jsonObject
            val listTitle = listObj["title"]?.jsonPrimitive?.contentOrNull?.trim() ?: return@forEach
            val listItems = listObj["items"]?.jsonArray ?: return@forEach
            listItems.forEach { itemEl ->
                val itemObj = itemEl.jsonObject
                val tmdbId = itemObj["tmdbId"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
                val type = itemObj["type"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: "movie"
                val mediaType = if (type == "tv") "tv" else "movie"
                val itemTitle = itemObj["title"]?.jsonPrimitive?.contentOrNull
                val item = getItem(mediaType, tmdbId)
                if (item.title.isBlank() && !itemTitle.isNullOrBlank()) {
                    item.title = itemTitle
                }
                item.folders.add(listTitle)
            }
        }
    }

    private fun parseSupplementalCsvs(
        zipEntries: Map<String, ByteArray>,
        itemsMap: MutableMap<String, MovieParadiseItemData>,
        getItem: (String, Long) -> MovieParadiseItemData
    ) {
        // favorites.csv
        zipEntries.entries.firstOrNull { it.key.equals("favorites.csv", ignoreCase = true) }?.value?.let { bytes ->
            bytes.decodeToString().lineSequence().drop(1).forEach { line ->
                if (line.isNotBlank()) {
                    val cols = parseCsvLine(line)
                    if (cols.size > 4) {
                        val type = cols.getOrNull(1)?.trim()?.lowercase() ?: "movie"
                        val mediaType = if (type == "tv") "tv" else "movie"
                        val tmdbId = cols.getOrNull(4)?.trim()?.toLongOrNull()
                        if (tmdbId != null) {
                            val item = getItem(mediaType, tmdbId)
                            item.isFavorite = true
                        }
                    }
                }
            }
        }

        // ratings.csv
        zipEntries.entries.firstOrNull { it.key.equals("ratings.csv", ignoreCase = true) }?.value?.let { bytes ->
            bytes.decodeToString().lineSequence().drop(1).forEach { line ->
                if (line.isNotBlank()) {
                    val cols = parseCsvLine(line)
                    if (cols.size > 8 && cols[0].trim().equals("title", ignoreCase = true)) {
                        val type = cols.getOrNull(1)?.trim()?.lowercase() ?: "movie"
                        val mediaType = if (type == "tv") "tv" else "movie"
                        val tmdbId = cols.getOrNull(4)?.trim()?.toLongOrNull()
                        val rating = cols.getOrNull(8)?.trim()?.toDoubleOrNull()
                        if (tmdbId != null && rating != null) {
                            val item = getItem(mediaType, tmdbId)
                            if (item.personalRating == null) {
                                item.personalRating = rating
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun emitImportedBatches(
        itemsMap: Map<String, MovieParadiseItemData>,
        onProgress: suspend (Int, Int) -> Unit,
        onBatchReady: suspend (List<Pair<Movie, String?>>, suspend (Int, Int) -> Unit) -> Unit
    ): Int {
        val moviesWithFolders = mutableListOf<Pair<Movie, String?>>()

        for (item in itemsMap.values) {
            val hasWatchedEpisodes = item.watchedEpisodes.isNotEmpty()
            val finalWatched = when {
                item.isDropped -> false
                hasWatchedEpisodes -> true
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
                watchedEpisodes = if (hasWatchedEpisodes) item.watchedEpisodes.mapValues { it.value.sorted() } else null,
                watchedAt = parseAndNormalizeWatchedDate(item.watchedAt),
                releaseYear = item.year,
                imdbId = item.imdbId,
                emotionalVibes = item.emotionalVibes,
                favoriteActorCharacter = item.favoriteActorCharacter,
                favoriteActorName = item.favoriteActorName,
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
            onBatchReady(chunk) { cur, tot ->
                onProgress(processed + cur, moviesWithFolders.size)
            }
            processed += chunk.size
            onProgress(processed, moviesWithFolders.size)
        }

        return itemsMap.size
    }
}
