package com.cinetrack.data.repository.importers

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.importers.ImporterUtils.parseAndNormalizeWatchedDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * Dedicated importer for the official Trakt.tv ZIP export.
 *
 * File layout expected inside the ZIP:
 *   watched-movies.json   – movies the user has watched
 *   watched-shows.json    – shows the user has interacted with (no episode list)
 *   watched-history.json  – full play-by-play history (used to rebuild episode maps)
 *   lists-watchlist.json  – the user's watchlist (movies + shows to see)
 *   ratings-movies.json   – personal movie ratings
 *   ratings-shows.json    – personal show ratings
 */
class TraktZipImporter @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns true when the given zip entry names look like a Trakt export.
     */
    fun isTraktExport(entryNames: Set<String>): Boolean =
        ("watched-movies.json" in entryNames || "watched-shows.json" in entryNames) &&
        "lists-watchlist.json" in entryNames

    /**
     * Parses all relevant Trakt ZIP entries and emits batches of [Movie] objects
     * to [onBatchReady]. Returns the total number of items emitted.
     */
    suspend fun import(
        zipEntries: Map<String, ByteArray>,
        onBatchReady: suspend (List<Pair<Movie, String?>>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        var count = 0

        // ── 1. Ratings ───────────────────────────────────────────────────────
        val ratingsMap = buildRatingsMap(zipEntries)

        // ── 2. Episode map from history ──────────────────────────────────────
        //  episodeMap: tmdbShowId -> { "seasonNum" -> Set<episodeNum> }
        //  lastWatched: tmdbShowId -> ISO date of last watch
        val (episodeMap, lastWatchedByShow) = buildEpisodeMap(zipEntries)

        // ── 3. Watched movies ─────────────────────────────────────────────────
        val watchedMovies = parseWatchedMovies(zipEntries, ratingsMap)
        if (watchedMovies.isNotEmpty()) {
            onBatchReady(watchedMovies)
            count += watchedMovies.size
        }

        // ── 4. Watched shows (merge with episode map) ─────────────────────────
        val watchedShows = parseWatchedShows(zipEntries, ratingsMap, episodeMap, lastWatchedByShow)
        if (watchedShows.isNotEmpty()) {
            onBatchReady(watchedShows)
            count += watchedShows.size
        }

        // ── 5. Watchlist (movies + shows to watch) ────────────────────────────
        val watchlistItems = parseWatchlist(zipEntries, ratingsMap)
        if (watchlistItems.isNotEmpty()) {
            onBatchReady(watchlistItems)
            count += watchlistItems.size
        }

        count
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun buildRatingsMap(zipEntries: Map<String, ByteArray>): Map<Long, Double> {
        val map = mutableMapOf<Long, Double>()
        listOf("ratings-movies.json", "ratings-shows.json").forEach { name ->
            val bytes = zipEntries[name] ?: return@forEach
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

        val bytes = zipEntries["watched-history.json"] ?: return Pair(emptyMap(), lastWatched)

        runCatching {
            (json.parseToJsonElement(bytes.decodeToString()) as? JsonArray)?.forEach { el ->
                val obj = el as? JsonObject ?: return@forEach
                // Only episode entries carry show+episode data
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

                // Track the most-recent watched_at date for this show
                val watchedAt = obj["watched_at"]?.jsonPrimitive?.contentOrNull
                if (watchedAt != null) {
                    val current = lastWatched[showTmdb]
                    if (current == null || watchedAt > current) lastWatched[showTmdb] = watchedAt
                }
            }
        }

        // Convert Set<Int> -> sorted List<Int>
        val sorted = episodeMap.mapValues { (_, seasons) ->
            seasons.mapValues { (_, eps) -> eps.sorted() }
        }
        return Pair(sorted, lastWatched)
    }

    private fun parseWatchedMovies(
        zipEntries: Map<String, ByteArray>,
        ratingsMap: Map<Long, Double>
    ): List<Pair<Movie, String?>> {
        val result = mutableListOf<Pair<Movie, String?>>()
        val bytes = zipEntries["watched-movies.json"] ?: return result

        runCatching {
            (json.parseToJsonElement(bytes.decodeToString()) as? JsonArray)?.forEach { el ->
                val obj = el as? JsonObject ?: return@forEach
                val movieObj = obj["movie"]?.jsonObject ?: return@forEach
                val idsObj = movieObj["ids"]?.jsonObject ?: return@forEach
                val tmdbId = idsObj["tmdb"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
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
                result.add(Pair(movie, null))
            }
        }
        return result
    }

    private fun parseWatchedShows(
        zipEntries: Map<String, ByteArray>,
        ratingsMap: Map<Long, Double>,
        episodeMap: Map<Long, Map<String, List<Int>>>,
        lastWatchedByShow: Map<Long, String>
    ): List<Pair<Movie, String?>> {
        val result = mutableListOf<Pair<Movie, String?>>()
        val bytes = zipEntries["watched-shows.json"] ?: return result

        runCatching {
            (json.parseToJsonElement(bytes.decodeToString()) as? JsonArray)?.forEach { el ->
                val obj = el as? JsonObject ?: return@forEach
                val showObj = obj["show"]?.jsonObject ?: return@forEach
                val idsObj = showObj["ids"]?.jsonObject ?: return@forEach
                val tmdbId = idsObj["tmdb"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
                val title = showObj["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown ($tmdbId)"
                val year = showObj["year"]?.jsonPrimitive?.contentOrNull

                val eps = episodeMap[tmdbId]
                val lastWatched = lastWatchedByShow[tmdbId]
                    ?: obj["last_watched_at"]?.jsonPrimitive?.contentOrNull

                val movie = Movie(
                    id = tmdbId,
                    mediaType = "tv",
                    title = title,
                    name = title,
                    // If we have episode data, mark as in-progress (favorite=true, watched=false).
                    // The UpdateEpisodesUseCase will upgrade to watched=true if all eps are done.
                    watched = false,
                    favorite = true,
                    watchedEpisodes = eps,
                    personalRating = ratingsMap[tmdbId],
                    watchedAt = parseAndNormalizeWatchedDate(lastWatched),
                    releaseYear = year,
                    syncStatus = "pending",
                    clientUpdatedAt = System.currentTimeMillis()
                )
                result.add(Pair(movie, null))
            }
        }
        return result
    }

    private fun parseWatchlist(
        zipEntries: Map<String, ByteArray>,
        ratingsMap: Map<Long, Double>
    ): List<Pair<Movie, String?>> {
        val result = mutableListOf<Pair<Movie, String?>>()
        val bytes = zipEntries["lists-watchlist.json"] ?: return result

        runCatching {
            (json.parseToJsonElement(bytes.decodeToString()) as? JsonArray)?.forEach { el ->
                val obj = el as? JsonObject ?: return@forEach
                val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: "movie"
                val mediaObj = if (type == "show") obj["show"]?.jsonObject else obj["movie"]?.jsonObject
                val idsObj = mediaObj?.get("ids")?.jsonObject ?: return@forEach
                val tmdbId = idsObj["tmdb"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
                val title = mediaObj?.get("title")?.jsonPrimitive?.contentOrNull ?: "Unknown ($tmdbId)"
                val year = mediaObj?.get("year")?.jsonPrimitive?.contentOrNull
                val mediaType = if (type == "show") "tv" else "movie"

                val movie = Movie(
                    id = tmdbId,
                    mediaType = mediaType,
                    title = title,
                    name = if (mediaType == "tv") title else null,
                    watched = false,
                    favorite = true,  // "to watch" = in watchlist = favorite
                    personalRating = ratingsMap[tmdbId],
                    releaseYear = year,
                    syncStatus = "pending",
                    clientUpdatedAt = System.currentTimeMillis()
                )
                result.add(Pair(movie, null))
            }
        }
        return result
    }
}
