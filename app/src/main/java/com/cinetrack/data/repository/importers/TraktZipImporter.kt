package com.cinetrack.data.repository.importers

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.importers.ImporterUtils.parseAndNormalizeWatchedDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * Dedicated importer for the official Trakt.tv ZIP export.
 */
class TraktZipImporter @Inject constructor(
    private val movieRepository: MovieRepository
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun isTraktExport(entryNames: Set<String>): Boolean {
        val hasWatched = entryNames.any { it.startsWith("watched-movies") || it.startsWith("watched-shows") || it.startsWith("watched_movies") || it.startsWith("watched_shows") }
        val hasWatchlist = entryNames.any { it.startsWith("lists-watchlist") || it.startsWith("watchlist") }
        return hasWatched && hasWatchlist
    }

    suspend fun import(
        zipEntries: Map<String, ByteArray>,
        onBatchReady: suspend (List<Pair<Movie, String?>>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        val ratingsMap = buildRatingsMap(zipEntries)
        val (episodeMap, lastWatchedByShow) = buildEpisodeMap(zipEntries)
        val hiddenShowsSet = buildHiddenShowsSet(zipEntries)

        count += parseWatchedMovies(zipEntries, ratingsMap, onBatchReady)
        count += parseWatchedShows(zipEntries, ratingsMap, episodeMap, lastWatchedByShow, hiddenShowsSet, onBatchReady)
        count += parseWatchlist(zipEntries, ratingsMap, onBatchReady)

        count
    }

    private fun buildRatingsMap(zipEntries: Map<String, ByteArray>): Map<Long, Double> {
        val map = mutableMapOf<Long, Double>()
        val ratingEntries = zipEntries.filterKeys { it.startsWith("ratings-movies") || it.startsWith("ratings-shows") || it.startsWith("ratings_movies") || it.startsWith("ratings_shows") }
        ratingEntries.values.forEach { bytes ->
            runCatching {
                (json.parseToJsonElement(bytes.decodeToString()) as? JsonArray)?.forEach { el ->
                    val obj = el as? JsonObject ?: return@forEach
                    val mediaObj = obj["movie"]?.jsonObject ?: obj["show"]?.jsonObject ?: return@forEach
                    val tmdbId = mediaObj["ids"]?.jsonObject?.get("tmdb")?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
                    val rating = obj["rating"]?.jsonPrimitive?.doubleOrNull ?: return@forEach
                    map[tmdbId] = rating
                }
            }
        }
        return map
    }

    private fun buildEpisodeMap(
        zipEntries: Map<String, ByteArray>
    ): Pair<Map<Long, Map<String, List<Int>>>, Map<Long, String>> {
        val episodeMap = mutableMapOf<Long, MutableMap<String, MutableSet<Int>>>()
        val lastWatched = mutableMapOf<Long, String>()

        val historyEntries = zipEntries.filterKeys { it.startsWith("watched-history") || it.startsWith("watched_history") }.values
        if (historyEntries.isEmpty()) return Pair(emptyMap(), lastWatched)

        historyEntries.forEach { bytes ->
            runCatching {
                (json.parseToJsonElement(bytes.decodeToString()) as? JsonArray)?.forEach { el ->
                    val obj = el as? JsonObject ?: return@forEach
                    if (obj["type"]?.jsonPrimitive?.contentOrNull != "episode") return@forEach

                    val showTmdb = obj["show"]?.jsonObject
                        ?.get("ids")?.jsonObject
                        ?.get("tmdb")?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: return@forEach

                    val epObj = obj["episode"]?.jsonObject ?: return@forEach
                    val season = epObj["season"]?.jsonPrimitive?.intOrNull ?: return@forEach
                    val number = epObj["number"]?.jsonPrimitive?.intOrNull ?: return@forEach

                    if (season > 0 && number > 0) {
                        episodeMap
                            .getOrPut(showTmdb) { mutableMapOf() }
                            .getOrPut(season.toString()) { mutableSetOf() }
                            .add(number)
                    }

                    val watchedAt = obj["watched_at"]?.jsonPrimitive?.contentOrNull
                    if (watchedAt != null) {
                        val current = lastWatched[showTmdb]
                        if (current == null || watchedAt > current) lastWatched[showTmdb] = watchedAt
                    }
                }
            }
        }

        val sorted = episodeMap.mapValues { (_, seasons) ->
            seasons.mapValues { (_, eps) -> eps.sorted() }
        }
        return Pair(sorted, lastWatched)
    }

    private suspend fun buildHiddenShowsSet(zipEntries: Map<String, ByteArray>): Set<Long> {
        val hiddenSet = mutableSetOf<Long>()
        val hiddenEntries = zipEntries.filterKeys { it.startsWith("hidden-shows") || it.startsWith("hidden_shows") || it.startsWith("hidden-progress-watched") || it.startsWith("hidden_progress_watched") }.values
        hiddenEntries.forEach { bytes ->
            runCatching {
                (json.parseToJsonElement(bytes.decodeToString()) as? JsonArray)?.forEach { el ->
                    val obj = el as? JsonObject ?: return@forEach
                    val showObj = obj["show"]?.jsonObject ?: return@forEach
                    val idsObj = showObj["ids"]?.jsonObject ?: return@forEach
                    val tmdbId = resolveTmdbId(idsObj, showObj, isTv = true) ?: return@forEach
                    hiddenSet.add(tmdbId)
                }
            }
        }
        return hiddenSet
    }

    private suspend fun resolveTmdbId(idsObj: JsonObject, mediaObj: JsonObject, isTv: Boolean): Long? {
        var tmdbId = idsObj["tmdb"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
        if (tmdbId == null) {
            val imdbId = idsObj["imdb"]?.jsonPrimitive?.contentOrNull
            if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt")) {
                tmdbId = movieRepository.findByImdbId(imdbId)?.id
            }
        }
        if (tmdbId == null && isTv) {
            val tvdbId = idsObj["tvdb"]?.jsonPrimitive?.contentOrNull
            if (!tvdbId.isNullOrBlank()) {
                tmdbId = movieRepository.findByTvdbId(tvdbId)?.id
            }
        }
        if (tmdbId == null) {
            val title = mediaObj["title"]?.jsonPrimitive?.contentOrNull
            val year = mediaObj["year"]?.jsonPrimitive?.contentOrNull
            if (!title.isNullOrBlank()) {
                tmdbId = movieRepository.searchMediaWithYear(title, year, isTv = isTv)?.id
            }
        }
        return tmdbId
    }

    private suspend fun parseWatchedMovies(
        zipEntries: Map<String, ByteArray>,
        ratingsMap: Map<Long, Double>,
        onBatchReady: suspend (List<Pair<Movie, String?>>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        val entries = zipEntries.filterKeys { it.startsWith("watched-movies") || it.startsWith("watched_movies") }.values

        for (bytes in entries) {
            try {
                val elements = (json.parseToJsonElement(bytes.decodeToString()) as? JsonArray)?.toList() ?: emptyList()
                val chunks = elements.chunked(20)
                for (chunk in chunks) {
                    val deferreds = chunk.mapNotNull { el ->
                        async {
                            val obj = el as? JsonObject ?: return@async null
                            val movieObj = obj["movie"]?.jsonObject ?: return@async null
                            val idsObj = movieObj["ids"]?.jsonObject ?: return@async null
                            
                            val tmdbId = resolveTmdbId(idsObj, movieObj, isTv = false) ?: return@async null
                            val title = movieObj["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown ($tmdbId)"
                            val year = movieObj["year"]?.jsonPrimitive?.contentOrNull
                            val watchedAt = obj["last_watched_at"]?.jsonPrimitive?.contentOrNull

                            val movie = Movie(
                                id = tmdbId,
                                mediaType = "movie",
                                title = title,
                                watched = true,
                                favorite = false,
                                personalRating = ratingsMap[tmdbId],
                                watchedAt = parseAndNormalizeWatchedDate(watchedAt),
                                releaseYear = year,
                                syncStatus = "pending",
                                clientUpdatedAt = System.currentTimeMillis()
                            )
                            Pair(movie, null)
                        }
                    }
                    val results = deferreds.awaitAll().filterNotNull()
                    if (results.isNotEmpty()) {
                        onBatchReady(results)
                        count += results.size
                    }
                    delay(300)
                }
            } catch (e: Exception) {}
        }
        count
    }

    private suspend fun parseWatchedShows(
        zipEntries: Map<String, ByteArray>,
        ratingsMap: Map<Long, Double>,
        episodeMap: Map<Long, Map<String, List<Int>>>,
        lastWatchedByShow: Map<Long, String>,
        hiddenShowsSet: Set<Long>,
        onBatchReady: suspend (List<Pair<Movie, String?>>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        val entries = zipEntries.filterKeys { it.startsWith("watched-shows") || it.startsWith("watched_shows") }.values

        for (bytes in entries) {
            try {
                val elements = (json.parseToJsonElement(bytes.decodeToString()) as? JsonArray)?.toList() ?: emptyList()
                val chunks = elements.chunked(20)
                for (chunk in chunks) {
                    val deferreds = chunk.mapNotNull { el ->
                        async {
                            val obj = el as? JsonObject ?: return@async null
                            val showObj = obj["show"]?.jsonObject ?: return@async null
                            val idsObj = showObj["ids"]?.jsonObject ?: return@async null

                            val tmdbId = resolveTmdbId(idsObj, showObj, isTv = true) ?: return@async null
                            val title = showObj["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown ($tmdbId)"
                            val year = showObj["year"]?.jsonPrimitive?.contentOrNull

                            val eps = episodeMap[tmdbId]
                            val lastWatched = lastWatchedByShow[tmdbId]
                                ?: obj["last_watched_at"]?.jsonPrimitive?.contentOrNull

                            val isDropped = tmdbId in hiddenShowsSet

                            val movie = Movie(
                                id = tmdbId,
                                mediaType = "tv",
                                title = title,
                                name = title,
                                watched = !isDropped,
                                dropped = isDropped,
                                favorite = isDropped,
                                watchedEpisodes = eps,
                                personalRating = ratingsMap[tmdbId],
                                watchedAt = parseAndNormalizeWatchedDate(lastWatched),
                                releaseYear = year,
                                syncStatus = "pending",
                                clientUpdatedAt = System.currentTimeMillis()
                            )
                            Pair(movie, null)
                        }
                    }
                    val results = deferreds.awaitAll().filterNotNull()
                    if (results.isNotEmpty()) {
                        onBatchReady(results)
                        count += results.size
                    }
                    delay(300)
                }
            } catch (e: Exception) {}
        }
        count
    }

    private suspend fun parseWatchlist(
        zipEntries: Map<String, ByteArray>,
        ratingsMap: Map<Long, Double>,
        onBatchReady: suspend (List<Pair<Movie, String?>>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        val entries = zipEntries.filterKeys { it.startsWith("lists-watchlist") || it.startsWith("watchlist") }.values

        for (bytes in entries) {
            try {
                val elements = (json.parseToJsonElement(bytes.decodeToString()) as? JsonArray)?.toList() ?: emptyList()
                val chunks = elements.chunked(20)
                for (chunk in chunks) {
                    val deferreds = chunk.mapNotNull { el ->
                        async {
                            val obj = el as? JsonObject ?: return@async null
                            val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: "movie"
                            val mediaObj = (if (type == "show") obj["show"]?.jsonObject else obj["movie"]?.jsonObject) ?: return@async null
                            val idsObj = mediaObj["ids"]?.jsonObject ?: return@async null
                            val isTv = type == "show"

                            val tmdbId = resolveTmdbId(idsObj, mediaObj, isTv) ?: return@async null
                            val title = mediaObj["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown ($tmdbId)"
                            val year = mediaObj["year"]?.jsonPrimitive?.contentOrNull
                            val mediaType = if (isTv) "tv" else "movie"

                            val movie = Movie(
                                id = tmdbId,
                                mediaType = mediaType,
                                title = title,
                                name = if (isTv) title else null,
                                watched = false,
                                favorite = true,
                                personalRating = ratingsMap[tmdbId],
                                releaseYear = year,
                                syncStatus = "pending",
                                clientUpdatedAt = System.currentTimeMillis()
                            )
                            Pair(movie, null)
                        }
                    }
                    val results = deferreds.awaitAll().filterNotNull()
                    if (results.isNotEmpty()) {
                        onBatchReady(results)
                        count += results.size
                    }
                    delay(300)
                }
            } catch (e: Exception) {}
        }
        count
    }
}
