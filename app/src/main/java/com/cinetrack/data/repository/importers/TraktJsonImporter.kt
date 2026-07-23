package com.cinetrack.data.repository.importers

import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.TraktExportItem
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.importers.ImporterUtils.parseAndNormalizeWatchedDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import javax.inject.Inject

class TraktJsonImporter @Inject constructor(
    private val movieRepository: MovieRepository,
    private val json: Json
) {
    suspend fun migrateTraktStream(
        inputStream: InputStream,
        keepLatestWatchDate: Boolean = true,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
        fileName: String? = null,
        onBatchReady: suspend (List<Pair<Movie, String?>>, Boolean, suspend (Int, Int) -> Unit) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val content = inputStream.bufferedReader().use { it.readText() }
        if (content.isBlank()) return@withContext 0
        var count = 0
        
        // 1. Try standard Trakt Export format
        try {
            val items = json.decodeFromString<List<TraktExportItem>>(content)
            val moviesToInsert = items.mapNotNull { item ->
                val tmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb ?: return@mapNotNull null
                val mediaType = if (item.movie != null) "movie" else "tv"
                
                val isEpisode = item.type == "episode" || item.episode != null
                var isShowWatched = item.watched == true
                if (!isEpisode && item.watchedAt != null) {
                    isShowWatched = true
                }
                
                val finalWatchedEps = if (isEpisode && item.episode?.season != null && item.episode?.number != null) {
                    mapOf(item.episode.season.toString() to listOf(item.episode.number))
                } else null

                val isDropped = item.dropped == true
                    || item.status?.lowercase()?.let { it == "dropped" || it == "paused" } == true
                Movie(
                    id = tmdbId,
                    mediaType = mediaType,
                    title = item.movie?.title ?: item.show?.title ?: "Unknown ($tmdbId)",
                    name = if (mediaType == "tv") (item.show?.title ?: "Unknown ($tmdbId)") else null,
                    watched = isShowWatched,
                    favorite = isDropped,  // keep dropped items visible in-list
                    dropped = isDropped,
                    personalRating = item.rating?.toDouble(),
                    personalNote = item.notes?.take(5000),
                    watchedEpisodes = finalWatchedEps,
                    watchedAt = parseAndNormalizeWatchedDate(item.watchedAt),
                    syncStatus = "pending",
                    clientUpdatedAt = System.currentTimeMillis()
                )
            }
            if (moviesToInsert.isNotEmpty()) {
                onBatchReady(moviesToInsert.map { Pair(it, null) }, keepLatestWatchDate, onProgress)
                return@withContext moviesToInsert.size
            }
        } catch (e: Exception) {
            // Fallthrough to Universal Smart JSON Array parser
        }

        // 2. Universal Smart JSON Parser (for Cinemaniac, TVTime, Serializd, MyDramaList, custom exports, etc.)
        try {
            val element = json.parseToJsonElement(content)
            
            val ratingsMap = mutableMapOf<Long, Double>()
            if (element is JsonObject && element["ratings"] is JsonArray) {
                (element["ratings"] as JsonArray).forEach { rObj ->
                    val o = rObj as? JsonObject
                    val id = o?.get("id")?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                    val r = o?.get("r")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                        ?: o?.get("rating")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                    if (id != null && r != null) {
                        ratingsMap[id] = r
                    }
                }
            }

            val array: List<JsonObject> = when (element) {
                is JsonArray -> element.mapNotNull { it as? JsonObject }
                is JsonObject -> {
                    val allObjects = mutableListOf<JsonObject>()
                    element.entries.forEach { entry ->
                        if (entry.key != "ratings" && entry.key != "settings" && entry.key != "categories") {
                            val valArray = entry.value as? JsonArray
                            if (valArray != null) {
                                val isCustomList = entry.key !in listOf("movies", "shows", "items", "media", "series", "list", "data", "results", "episodes", "seen_episodes", "followed_shows", "watched_episodes", "watched_movies", "watched", "watchlist", "diary", "history", "shows_list", "movies_list", "ratings", "reviews")
                                valArray.forEach { el ->
                                    val obj = el as? JsonObject
                                    if (obj != null) {
                                        val objWithFolder = if (isCustomList && !obj.containsKey("folder") && !obj.containsKey("list_name")) {
                                            JsonObject(obj + ("folder" to JsonPrimitive(entry.key)))
                                        } else obj
                                        allObjects.add(objWithFolder)
                                    }
                                }
                            }
                        }
                    }
                    if (allObjects.isNotEmpty()) allObjects else listOf(element)
                }
                else -> emptyList()
            }

            val chunks = array.chunked(20)
            for (chunk in chunks) {
                val deferreds = chunk.mapNotNull { obj ->
                    val itemObj = obj["movie"]?.jsonObject
                        ?: obj["show"]?.jsonObject
                        ?: obj["item"]?.jsonObject
                        ?: obj["series"]?.jsonObject
                        ?: obj
                    val idsObj = itemObj["ids"]?.jsonObject ?: obj["ids"]?.jsonObject ?: itemObj

                    val tmdbId = idsObj["tmdb_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: idsObj["tmdbId"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: idsObj["id_movie"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: idsObj["movie_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: idsObj["show_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: idsObj["tmdb"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: idsObj["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                    val imdbId = idsObj["imdb_id"]?.jsonPrimitive?.contentOrNull
                        ?: idsObj["imdbId"]?.jsonPrimitive?.contentOrNull
                        ?: idsObj["imdb"]?.jsonPrimitive?.contentOrNull
                        ?: idsObj["const"]?.jsonPrimitive?.contentOrNull
                    val title = itemObj["title"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["name"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["show_name"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["movie_name"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["show_title"]?.jsonPrimitive?.contentOrNull
                    val year = itemObj["year"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["release_year"]?.jsonPrimitive?.contentOrNull
                    val typeStr = itemObj["type"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["media_type"]?.jsonPrimitive?.contentOrNull
                        ?: obj["type"]?.jsonPrimitive?.contentOrNull
                        ?: obj["media_type"]?.jsonPrimitive?.contentOrNull
                        ?: "movie"

                    val userRating = if (tmdbId != null && ratingsMap.containsKey(tmdbId)) {
                        ratingsMap[tmdbId]
                    } else {
                        obj["personal_rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: obj["user_rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: obj["my_rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: itemObj["personal_rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: itemObj["user_rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: itemObj["my_rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: if (ratingsMap.isEmpty()) (obj["rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: itemObj["rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()) else null
                    }
                    val userNote = (obj["personal_note"]?.jsonPrimitive?.contentOrNull
                        ?: obj["note"]?.jsonPrimitive?.contentOrNull
                        ?: obj["comment"]?.jsonPrimitive?.contentOrNull
                        ?: obj["review"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["personal_note"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["note"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["comment"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["review"]?.jsonPrimitive?.contentOrNull)?.take(5000)

                    val hasWatchedField = listOf("watched", "seen", "status", "watch_status").any { key ->
                        obj.containsKey(key) || itemObj.containsKey(key)
                    }
                    val isWatchlistFile = fileName != null && (fileName.contains("watchlist") || fileName.contains("to_watch") || fileName.contains("planned") || fileName.contains("for_later"))
                    val explicitSeen = obj["watched"]?.jsonPrimitive?.booleanOrNull == true
                        || obj["seen"]?.jsonPrimitive?.contentOrNull == "1"
                        || obj["seen"]?.jsonPrimitive?.intOrNull == 1
                        || obj["seen"]?.jsonPrimitive?.booleanOrNull == true
                        || obj["status"]?.jsonPrimitive?.contentOrNull?.lowercase()
                            ?.let { it == "watched" || it == "completed" || it == "seen" } == true
                        || obj["watch_status"]?.jsonPrimitive?.contentOrNull?.lowercase()
                            ?.let { it == "watched" || it == "completed" } == true
                        || itemObj["watched"]?.jsonPrimitive?.booleanOrNull == true
                        || itemObj["seen"]?.jsonPrimitive?.contentOrNull == "1"
                        || itemObj["seen"]?.jsonPrimitive?.intOrNull == 1
                        || itemObj["seen"]?.jsonPrimitive?.booleanOrNull == true
                        || itemObj["status"]?.jsonPrimitive?.contentOrNull?.lowercase()
                            ?.let { it == "watched" || it == "completed" || it == "seen" } == true
                    val isSeen = if (hasWatchedField) explicitSeen else if (isWatchlistFile) false else true
                    val isDropped = obj["dropped"]?.jsonPrimitive?.booleanOrNull == true
                        || itemObj["dropped"]?.jsonPrimitive?.booleanOrNull == true
                        || obj["status"]?.jsonPrimitive?.contentOrNull?.lowercase()
                            ?.let { it == "dropped" || it == "paused" } == true
                        || obj["watch_status"]?.jsonPrimitive?.contentOrNull?.lowercase()
                            ?.let { it == "dropped" || it == "paused" } == true
                        || itemObj["status"]?.jsonPrimitive?.contentOrNull?.lowercase()
                            ?.let { it == "dropped" || it == "paused" } == true
                    var isFav = obj["favorite"]?.jsonPrimitive?.booleanOrNull == true
                        || obj["fav"]?.jsonPrimitive?.booleanOrNull == true
                        || obj["starred"]?.jsonPrimitive?.booleanOrNull == true
                        || itemObj["favorite"]?.jsonPrimitive?.booleanOrNull == true
                        || itemObj["fav"]?.jsonPrimitive?.booleanOrNull == true
                        || itemObj["starred"]?.jsonPrimitive?.booleanOrNull == true
                        || isDropped  // dropped items stay visible in the list

                    val rawFolder = obj["folder"]?.jsonPrimitive?.contentOrNull
                        ?: obj["list_name"]?.jsonPrimitive?.contentOrNull
                        ?: obj["list"]?.jsonPrimitive?.contentOrNull
                        ?: obj["tag"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["folder"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["list_name"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["list"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["tag"]?.jsonPrimitive?.contentOrNull
                    val folderName = rawFolder ?: if (isWatchlistFile) "Watchlist" else null

                    if (!isSeen && (isWatchlistFile || folderName?.equals("watchlist", ignoreCase = true) == true || folderName?.equals("to watch", ignoreCase = true) == true || folderName?.equals("planned", ignoreCase = true) == true || folderName?.equals("for_later", ignoreCase = true) == true)) {
                        isFav = true
                    }

                    val watchedEpsMap = mutableMapOf<String, MutableSet<Int>>()
                    val directEpsObj = obj["watched_episodes"]?.jsonObject ?: obj["watchedEpisodes"]?.jsonObject ?: itemObj["watched_episodes"]?.jsonObject ?: itemObj["watchedEpisodes"]?.jsonObject
                    directEpsObj?.entries?.forEach { (sNum, epsVal) ->
                        val epNums = epsVal.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull?.toIntOrNull() ?: (it as? JsonObject)?.get("number")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: (it as? JsonObject)?.get("episode")?.jsonPrimitive?.contentOrNull?.toIntOrNull() }
                        if (!epNums.isNullOrEmpty()) watchedEpsMap.getOrPut(sNum) { mutableSetOf() }.addAll(epNums)
                    }
                    val seasonsArr = obj["seasons"]?.jsonArray ?: itemObj["seasons"]?.jsonArray
                    seasonsArr?.forEach { sEl ->
                        val sObj = sEl as? JsonObject
                        val sNum = sObj?.get("number")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: sObj?.get("season")?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                        if (sNum != null && sNum > 0) {
                            val epsArr = sObj?.get("episodes")?.jsonArray
                            val epNums = epsArr?.mapNotNull { epEl ->
                                val epObj = epEl as? JsonObject
                                val isEpWatched = epObj?.get("watched")?.jsonPrimitive?.booleanOrNull ?: epObj?.get("seen")?.jsonPrimitive?.booleanOrNull ?: true
                                if (isEpWatched) {
                                    epObj?.get("number")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: epObj?.get("episode")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: (epEl as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
                                } else null
                            }
                            if (!epNums.isNullOrEmpty()) watchedEpsMap.getOrPut(sNum.toString()) { mutableSetOf() }.addAll(epNums)
                        }
                    }
                    val epSeasonNum = obj["season_number"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: obj["season"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: itemObj["season_number"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: itemObj["season"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    val epNum = obj["episode_number"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: obj["episode"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: itemObj["episode_number"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: itemObj["episode"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    if (epSeasonNum != null && epSeasonNum > 0 && epNum != null && epNum > 0) {
                        watchedEpsMap.getOrPut(epSeasonNum.toString()) { mutableSetOf() }.add(epNum)
                    }
                    val finalWatchedEps = if (watchedEpsMap.isNotEmpty()) watchedEpsMap.mapValues { it.value.sorted() } else null

                    val rawWatchedDate = obj["watched_at"]?.jsonPrimitive?.contentOrNull
                        ?: obj["watchedAt"]?.jsonPrimitive?.contentOrNull
                        ?: obj["watched_date"]?.jsonPrimitive?.contentOrNull
                        ?: obj["date"]?.jsonPrimitive?.contentOrNull
                        ?: obj["log_date"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["watched_at"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["watchedAt"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["watched_date"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["date"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["log_date"]?.jsonPrimitive?.contentOrNull
                    val parsedWatchedDate = parseAndNormalizeWatchedDate(rawWatchedDate)

                    async {
                        try {
                            if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt")) {
                                val tmdbMovie = movieRepository.findByImdbId(imdbId)
                                tmdbMovie?.let {
                                    val mediaType = if (finalWatchedEps != null || typeStr.contains("tv", ignoreCase = true) || it.mediaType == "tv") "tv" else "movie"
                                    val m = Movie(
                                        id = it.id,
                                        mediaType = mediaType,
                                        title = it.title,
                                        name = it.name,
                                        posterPath = it.posterPath,
                                        backdropPath = it.backdropPath,
                                        voteAverage = it.voteAverage,
                                        overview = it.overview,
                                        releaseDate = it.releaseDate,
                                        firstAirDate = it.firstAirDate,
                                        genreIds = it.genreIds,
                                        watched = isSeen,
                                        favorite = isFav,
                                        dropped = isDropped,
                                        personalRating = userRating,
                                        personalNote = userNote,
                                        watchedEpisodes = finalWatchedEps,
                                        watchedAt = if (isSeen) (parsedWatchedDate ?: java.time.Instant.now().toString()) else null,
                                        syncStatus = "pending",
                                        clientUpdatedAt = System.currentTimeMillis()
                                    )
                                    Pair(m, folderName)
                                }
                            } else if (tmdbId != null && tmdbId > 0) {
                                val mediaType = if (finalWatchedEps != null || typeStr.contains("tv", ignoreCase = true) || typeStr.contains("show", ignoreCase = true)) "tv" else "movie"
                                val m = Movie(
                                    id = tmdbId,
                                    mediaType = mediaType,
                                    title = title ?: "Unknown ($tmdbId)",
                                    name = if (mediaType == "tv") title else null,
                                    watched = isSeen,
                                    favorite = isFav,
                                    dropped = isDropped,
                                    personalRating = userRating,
                                    personalNote = userNote,
                                    watchedEpisodes = finalWatchedEps,
                                    watchedAt = if (isSeen) (parsedWatchedDate ?: java.time.Instant.now().toString()) else null,
                                    syncStatus = "pending",
                                    clientUpdatedAt = System.currentTimeMillis()
                                )
                                Pair(m, folderName)
                            } else if (!title.isNullOrBlank()) {
                                val isTvMedia = finalWatchedEps != null || typeStr.contains("tv", ignoreCase = true) || typeStr.contains("show", ignoreCase = true) || typeStr.contains("series", ignoreCase = true)
                                val tmdbMovie = movieRepository.searchMediaWithYear(title, year, isTv = isTvMedia)
                                tmdbMovie?.let {
                                    val mediaType = if (finalWatchedEps != null || it.mediaType == "tv") "tv" else (it.mediaType ?: "movie")
                                    val m = Movie(
                                        id = it.id,
                                        mediaType = mediaType,
                                        title = it.title,
                                        name = it.name,
                                        posterPath = it.posterPath,
                                        backdropPath = it.backdropPath,
                                        voteAverage = it.voteAverage,
                                        overview = it.overview,
                                        releaseDate = it.releaseDate,
                                        firstAirDate = it.firstAirDate,
                                        genreIds = it.genreIds,
                                        watched = isSeen,
                                        favorite = isFav,
                                        dropped = isDropped,
                                        personalRating = userRating,
                                        personalNote = userNote,
                                        watchedEpisodes = finalWatchedEps,
                                        watchedAt = if (isSeen) (parsedWatchedDate ?: java.time.Instant.now().toString()) else null,
                                        syncStatus = "pending",
                                        clientUpdatedAt = System.currentTimeMillis()
                                    )
                                    Pair(m, folderName)
                                }
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                val results = deferreds.awaitAll().filterNotNull()
                if (results.isNotEmpty()) {
                    onBatchReady(results, keepLatestWatchDate, onProgress)
                    count += results.size
                }
                kotlinx.coroutines.delay(300)
            }
        } catch (e: Exception) {
            android.util.Log.e("TraktJsonImporter", "Failed Universal JSON migration", e)
        }
        
        count
    }
}
