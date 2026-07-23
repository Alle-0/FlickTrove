package com.cinetrack.data.repository.importers

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.importers.ImporterUtils.parseAndNormalizeWatchedDate
import com.cinetrack.data.repository.importers.ImporterUtils.parseCsvLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

class UniversalCsvImporter @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend fun migrateCsvStream(
        inputStream: InputStream,
        keepLatestWatchDate: Boolean = true,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
        fileName: String? = null,
        onBatchReady: suspend (List<Pair<Movie, String?>>, Boolean, suspend (Int, Int) -> Unit) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        inputStream.bufferedReader().useLines { linesSequence ->
            val lines = linesSequence.filter { it.isNotBlank() }.iterator()
            if (!lines.hasNext()) return@withContext 0
            val headerLine = lines.next()
            val headers = parseCsvLine(headerLine)
            val lowerHeaders = headers.map { it.trim().lowercase() }

            // Common Headers
            val imdbIdx = lowerHeaders.indexOfFirst { it in listOf("const", "imdb id", "imdb_id", "imdbid", "imdb", "id", "title_id", "imdb_uri", "tconst") }
            val titleIdx = lowerHeaders.indexOfFirst { it in listOf("title", "name", "movie title", "show name", "film", "series", "show_name", "movie_name", "item_name", "movie", "show", "original title", "show title", "episode title", "original_title", "show_title", "tv_show_name", "series_name") }
            val letterboxdUriIdx = lowerHeaders.indexOfFirst { it == "letterboxd uri" }
            val yearIdx = lowerHeaders.indexOfFirst { it in listOf("year", "release year", "release_year", "date_released", "date released", "release date") }
                .takeIf { it != -1 }
                ?: lowerHeaders.indexOfFirst { (it == "date" || it == "created date") && !lowerHeaders.contains("watched date") && !lowerHeaders.contains("watched_date") && !lowerHeaders.contains("watched_at") }
            val watchedDateIdx = lowerHeaders.indexOfFirst { it in listOf("watched date", "watched_date", "watched_at", "date watched", "date_watched", "log date", "diary date", "date rated", "date_rated") }
                .takeIf { it != -1 }
                ?: lowerHeaders.indexOfFirst { (it == "date" || it == "created date" || it == "created_at") && !lowerHeaders.contains("year") && !lowerHeaders.contains("release year") && !lowerHeaders.contains("release_year") }
                .takeIf { it != -1 }
                ?: lowerHeaders.indexOfFirst { it == "date" }
            val typeIdx = lowerHeaders.indexOfFirst { it in listOf("title type", "type", "media_type", "item_type", "contenttype") }
            val ratingIdx = lowerHeaders.indexOfFirst { it in listOf("rating", "your rating", "my rating", "score", "user rating", "rating10", "rating5", "user_rating", "personal_rating") }
            val noteIdx = lowerHeaders.indexOfFirst { it in listOf("review", "comment", "note", "personal_note", "your review", "my review", "notes", "text", "message") }
            val watchedIdx = lowerHeaders.indexOfFirst { it in listOf("watched", "seen", "status", "watched status", "watched_status") }
            val favIdx = lowerHeaders.indexOfFirst { it in listOf("favorite", "is_favorite", "fav", "starred", "liked") }
            val seasonIdx = lowerHeaders.indexOfFirst { it in listOf("season", "season number", "season_number", "s", "season_num") }
            val episodeIdx = lowerHeaders.indexOfFirst { it in listOf("episode", "episode number", "episode_number", "e", "episode_num", "ep") }
            val folderIdx = lowerHeaders.indexOfFirst { it in listOf("folder", "list", "list name", "list_name", "tag", "playlist", "collection") }
            
            // Yamtrack-specific Headers
            val yamtrackMediaIdIdx = lowerHeaders.indexOfFirst { it == "media_id" }
            val yamtrackSourceIdx  = lowerHeaders.indexOfFirst { it == "source" }

            // Universal TMDB ID - prioritize Yamtrack's `media_id` if present
            val tmdbIdx = if (yamtrackMediaIdIdx != -1) yamtrackMediaIdIdx 
                          else lowerHeaders.indexOfFirst { it in listOf("tmdb id", "tmdb_id", "tmdbid", "tmdb", "movie_id", "show_id", "id_movie", "movieid", "tv_show_id", "s_id", "id_show") }
            
            val isYamtrack = yamtrackMediaIdIdx != -1
            
            val hasKnownHeaders = imdbIdx != -1 || tmdbIdx != -1 || titleIdx != -1
            if (!hasKnownHeaders && headers.none { it.startsWith("tt") }) {
                return@withContext 0
            }

            val dataLines = lines.asSequence()
            val chunks = dataLines.chunked(20)
            for (chunk in chunks) {
                val deferreds = chunk.mapNotNull { line ->
                    val columns = parseCsvLine(line)
                    if (columns.isEmpty()) return@mapNotNull null
                    
                    if (isYamtrack) {
                        val source = if (yamtrackSourceIdx != -1 && columns.size > yamtrackSourceIdx) columns[yamtrackSourceIdx].trim().lowercase() else ""
                        if (source.isNotBlank() && source != "tmdb") return@mapNotNull null
                    }
                    
                    val imdbVal = if (imdbIdx != -1 && columns.size > imdbIdx) columns[imdbIdx] 
                                  else columns.firstOrNull { it.trim().startsWith("tt") }
                    val tmdbVal = if (tmdbIdx != -1 && columns.size > tmdbIdx) columns[tmdbIdx]?.toLongOrNull() else null
                    val titleVal = if (titleIdx != -1 && columns.size > titleIdx) columns[titleIdx] else null
                    val yearVal = if (yearIdx != -1 && columns.size > yearIdx) columns[yearIdx] else null
                    val rawWatchedDateVal = if (watchedDateIdx != -1 && columns.size > watchedDateIdx) columns[watchedDateIdx] else null
                    val parsedWatchedDate = parseAndNormalizeWatchedDate(rawWatchedDateVal)
                    val ratingVal = if (ratingIdx != -1 && columns.size > ratingIdx) columns[ratingIdx]?.toDoubleOrNull() else null
                    val noteVal = if (noteIdx != -1 && columns.size > noteIdx) columns[noteIdx]?.take(5000) else null
                    
                    var typeVal = if (typeIdx != -1 && columns.size > typeIdx) columns[typeIdx] else "movie"
                    if (isYamtrack) {
                        typeVal = when (typeVal.lowercase()) {
                            "movie" -> "movie"
                            "tv", "season", "episode" -> "tv"
                            else -> "movie"
                        }
                    }

                    val isWatchlistFile = fileName != null && (fileName.contains("watchlist") || fileName.contains("to_watch") || fileName.contains("planned") || fileName.contains("for_later"))
                    var droppedVal = false
                    var watchedVal = if (watchedIdx != -1 && columns.size > watchedIdx) {
                        val w = columns[watchedIdx]?.trim()?.lowercase()
                        when (w) {
                            "1", "true", "yes", "watched", "completed", "watching", "seen", "up_to_date" -> true
                            "0", "false", "no", "planned", "for_later", "watchlist", "to_watch", "to watch", "da vedere" -> false
                            "dropped", "paused" -> { droppedVal = true; false }
                            else -> !isWatchlistFile
                        }
                    } else !isWatchlistFile

                    if (isYamtrack && watchedIdx != -1 && columns.size > watchedIdx) {
                        watchedVal = when (columns[watchedIdx]) {
                            "Completed" -> true
                            "Planned", "Watching" -> false
                            "Dropped", "Paused" -> { droppedVal = true; false }
                            else -> false
                        }
                    }
                    
                    val seasonVal = if (seasonIdx != -1 && columns.size > seasonIdx) columns[seasonIdx]?.toIntOrNull() else null
                    val episodeVal = if (episodeIdx != -1 && columns.size > episodeIdx) columns[episodeIdx]?.toIntOrNull() else null
                    val epsMap = if (seasonVal != null && seasonVal > 0 && episodeVal != null && episodeVal > 0) {
                        mapOf(seasonVal.toString() to listOf(episodeVal))
                    } else null
                    val rawFolder = if (folderIdx != -1 && columns.size > folderIdx) columns[folderIdx]?.takeIf { it.isNotBlank() } else null
                    val folderVal = rawFolder ?: if (isWatchlistFile) "Watchlist" else null
                    
                    var favVal = if (favIdx != -1 && columns.size > favIdx) {
                        val f = columns[favIdx]?.trim()?.lowercase()
                        f == "1" || f == "true" || f == "yes" || f == "fav" || f == "starred"
                    } else false

                    if (!watchedVal && (isWatchlistFile || folderVal?.equals("watchlist", ignoreCase = true) == true || folderVal?.equals("to watch", ignoreCase = true) == true || folderVal?.equals("planned", ignoreCase = true) == true || folderVal?.equals("for_later", ignoreCase = true) == true || (watchedIdx != -1 && columns.size > watchedIdx && columns[watchedIdx]?.trim()?.lowercase() in listOf("planned", "for_later", "watchlist", "to_watch", "to watch", "da vedere")))) {
                        favVal = true
                    }
                    
                    async {
                        try {
                            if (!imdbVal.isNullOrBlank() && imdbVal.trim().startsWith("tt")) {
                                val tmdbMovie = movieRepository.findByImdbId(imdbVal.trim())
                                tmdbMovie?.let {
                                    val mediaType = if (epsMap != null || typeVal.contains("tv", ignoreCase = true) || typeVal.contains("series", ignoreCase = true) || it.mediaType == "tv") "tv" else "movie"
                                    val m = Movie(
                                        id = it.id,
                                        mediaType = mediaType,
                                        title = it.title,
                                        name = it.name,
                                        watched = watchedVal,
                                        favorite = favVal || droppedVal,
                                        dropped = droppedVal,
                                        personalRating = ratingVal,
                                        personalNote = noteVal,
                                        watchedEpisodes = epsMap,
                                        watchedAt = if (watchedVal) (parsedWatchedDate ?: java.time.Instant.now().toString()) else null,
                                        syncStatus = "pending",
                                        clientUpdatedAt = System.currentTimeMillis()
                                    )
                                    Pair(m, folderVal)
                                }
                            } else if (tmdbVal != null && tmdbVal > 0) {
                                val mediaType = if (epsMap != null || typeVal.contains("tv", ignoreCase = true) || typeVal.contains("series", ignoreCase = true) || typeVal.contains("show", ignoreCase = true)) "tv" else "movie"
                                val m = Movie(
                                    id = tmdbVal,
                                    mediaType = mediaType,
                                    title = titleVal ?: "Unknown ($tmdbVal)",
                                    name = if (mediaType == "tv") titleVal else null,
                                    watched = watchedVal,
                                    favorite = favVal || droppedVal,
                                    dropped = droppedVal,
                                    personalRating = ratingVal,
                                    personalNote = noteVal,
                                    watchedEpisodes = epsMap,
                                    watchedAt = if (watchedVal) (parsedWatchedDate ?: java.time.Instant.now().toString()) else null,
                                    syncStatus = "pending",
                                    clientUpdatedAt = System.currentTimeMillis()
                                )
                                Pair(m, folderVal)
                            } else if (!titleVal.isNullOrBlank()) {
                                val isTvMedia = epsMap != null || typeVal.contains("tv", ignoreCase = true) || typeVal.contains("series", ignoreCase = true) || typeVal.contains("show", ignoreCase = true) || typeVal.contains("episode", ignoreCase = true) || lowerHeaders.any { it == "tv_show_name" || it == "series_name" || it == "tv_show_id" || it == "s_id" || it == "id_show" }
                                val tmdbMovie = movieRepository.searchMediaWithYear(titleVal.trim(), yearVal?.trim(), isTv = isTvMedia)
                                tmdbMovie?.let {
                                    val mediaType = if (epsMap != null || it.mediaType == "tv") "tv" else (it.mediaType ?: "movie")
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
                                        watched = watchedVal,
                                        favorite = favVal,
                                        personalRating = ratingVal,
                                        personalNote = noteVal,
                                        watchedEpisodes = epsMap,
                                        watchedAt = if (watchedVal) (parsedWatchedDate ?: java.time.Instant.now().toString()) else null,
                                        syncStatus = "pending",
                                        clientUpdatedAt = System.currentTimeMillis()
                                    )
                                    Pair(m, folderVal)
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
                kotlinx.coroutines.delay(350)
            }
        }
        count
    }
}
