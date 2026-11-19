package com.cinetrack.worker

import android.util.Log
import com.cinetrack.data.api.TMDBService
import com.cinetrack.data.api.TraktHistoryItem
import com.cinetrack.data.api.TraktService
import com.cinetrack.data.api.TraktSyncIds
import com.cinetrack.data.api.TraktSyncMovie
import com.cinetrack.data.api.TraktSyncRequest
import com.cinetrack.data.mapper.MovieMapper
import com.cinetrack.data.model.Movie
import com.cinetrack.data.remote.FirebaseRemoteDataSource
import com.cinetrack.data.repository.MovieRepository
import kotlinx.coroutines.delay

/**
 * Delegate managing Movie watched history synchronization and two-way diffing between Trakt and local SQLite.
 */
class TraktMovieSyncDelegate(
    private val traktService: TraktService,
    private val tmdbService: TMDBService,
    private val movieRepository: MovieRepository
) {

    suspend fun syncWatchedMovies(
        history: List<TraktHistoryItem>,
        isIntegrityValid: Boolean,
        isFirstSync: Boolean,
        bulkStatsUpdates: MutableList<FirebaseRemoteDataSource.TrendingStatUpdate>,
        onProgress: suspend (current: Int, total: Int) -> Unit,
        isRecent: (String?) -> Boolean
    ) {
        val total = history.size
        var current = 0

        val remoteWatchedMovieIds = mutableSetOf<Pair<Long, String>>()
        val moviesToUpdate = mutableListOf<Movie>()

        for (item in history) {
            current++
            if (current % 5 == 0 || current == total) {
                onProgress(current, total)
            }

            var tmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb
            val imdbId = item.movie?.ids?.imdb ?: item.show?.ids?.imdb
            val mediaType = if (item.type == "movie") "movie" else "tv"

            // Skip TV Shows in Movie phase
            if (mediaType == "tv") continue

            // Fallback: resolution via IMDB if ids.tmdb is missing
            if (tmdbId == null && !imdbId.isNullOrBlank()) {
                try {
                    val found = tmdbService.findByExternalId(imdbId, "imdb_id")
                    val resolved = found.movieResults?.firstOrNull()?.id
                    if (resolved != null && resolved > 100L) {
                        tmdbId = resolved
                    }
                } catch (e: Exception) {
                    Log.e("TraktSyncWorker", "Errore lookup TMDB per IMDB ID $imdbId in history", e)
                }
            }

            if (tmdbId == null || tmdbId <= 0L) continue
            remoteWatchedMovieIds.add(Pair(tmdbId, "movie"))

            val localMovie = movieRepository.getMovieIncludingDeleted(tmdbId, mediaType)
            if (localMovie != null && (localMovie.syncStatus == "pending_delete" || localMovie.syncStatus == "deleted")) {
                Log.d("TraktSyncWorker", "SYNC: Film $tmdbId è ${localMovie.syncStatus}, skip resurrezione.")
                continue
            }
            if (localMovie != null) {
                var currentMovie = localMovie
                if (currentMovie.posterPath.isNullOrEmpty()) {
                    try {
                        val tmdbResponse = tmdbService.getMovieBasicDetails(tmdbId)
                        val fresh = MovieMapper.mapResponseToMovie(tmdbResponse, mediaType)
                        currentMovie = currentMovie.copy(
                            posterPath = fresh.posterPath ?: currentMovie.posterPath,
                            backdropPath = fresh.backdropPath ?: currentMovie.backdropPath
                        )
                        delay(50)
                    } catch (_: Exception) {
                        // Non-fatal
                    }
                }
                if (!currentMovie.watched || currentMovie != localMovie) {
                    val updated = currentMovie.copy(watched = true, watchedAt = item.watched_at, favorite = false)
                    moviesToUpdate.add(updated)
                    if (isRecent(item.watched_at)) {
                        bulkStatsUpdates.add(
                            FirebaseRemoteDataSource.TrendingStatUpdate(
                                compositeId = updated.compositeId,
                                viewsDelta = 1L
                            )
                        )
                    }
                }
                continue
            }

            try {
                val tmdbResponse = tmdbService.getMovieBasicDetails(tmdbId)
                var newMovie = MovieMapper.mapResponseToMovie(tmdbResponse, mediaType)
                newMovie = newMovie.copy(watched = true, watchedAt = item.watched_at, favorite = false)
                moviesToUpdate.add(newMovie)

                if (isRecent(item.watched_at)) {
                    bulkStatsUpdates.add(
                        FirebaseRemoteDataSource.TrendingStatUpdate(
                            compositeId = newMovie.compositeId,
                            viewsDelta = 1L
                        )
                    )
                }

                delay(50)
            } catch (e: Exception) {
                Log.e("TraktSyncWorker", "Impossibile scaricare dettagli per TMDB ID $tmdbId", e)
            }
        }

        // --- Two Way Merge/Diff (Rimozioni o Caricamento Iniziale) ---
        if (isIntegrityValid) {
            val remoteWatchedIds = remoteWatchedMovieIds
            val localMovies = movieRepository.getLocalMoviesIncludingDeleted()
            val remoteMovieIds = remoteWatchedIds.filter { it.second != "tv" }

            if (isFirstSync || remoteMovieIds.isEmpty()) {
                // Primo collegamento o account vuoto: MERGE (push su Trakt di ciò che manca remota, nessuna cancellazione locale)
                val localMoviesToPush = localMovies.filter {
                    it.watched && it.mediaType != "tv" && !remoteWatchedIds.contains(Pair(it.id, "movie"))
                }
                if (localMoviesToPush.isNotEmpty()) {
                    Log.e("TRAKT_DEBUG", "SYNC MERGE: Primo sync/Account vuoto -> Push di ${localMoviesToPush.size} film visti su Trakt")
                    try {
                        val syncMovies = localMoviesToPush.map {
                            TraktSyncMovie(
                                ids = TraktSyncIds(tmdb = it.id),
                                watched_at = it.watchedAt
                            )
                        }
                        traktService.addToHistory(TraktSyncRequest(movies = syncMovies))
                    } catch (e: Exception) {
                        Log.e("TraktSyncWorker", "Errore push film visti verso Trakt", e)
                    }
                }
            } else {
                val now = System.currentTimeMillis()
                for (localMovie in localMovies) {
                    if (localMovie.watched && localMovie.mediaType != "tv") {
                        val key = Pair(localMovie.id, localMovie.mediaType)
                        if (!remoteWatchedIds.contains(key)) {
                            // Anti-overwrite guard: se il film è stato modificato di recente in locale (ultimi 15 min),
                            // non resettarlo a watched = false perché l'InstantWriteWorker potrebbe essere ancora in volo
                            val isRecentlyModified = (now - localMovie.clientUpdatedAt) < 15 * 60 * 1000L
                            if (isRecentlyModified) {
                                Log.d("TraktSyncWorker", "SYNC: Film ${localMovie.id} visto di recente in locale, preservo e pusho su Trakt.")
                                try {
                                    val syncMovies = listOf(
                                        TraktSyncMovie(
                                            ids = TraktSyncIds(tmdb = localMovie.id, imdb = localMovie.imdbId),
                                            watched_at = localMovie.watchedAt
                                        )
                                    )
                                    traktService.addToHistory(TraktSyncRequest(movies = syncMovies))
                                } catch (e: Exception) {
                                    Log.e("TraktSyncWorker", "Errore push film visto di recente su Trakt", e)
                                }
                            } else {
                                // Orfano trovato: l'utente lo ha cancellato da Trakt
                                val updated = localMovie.copy(watched = false)
                                moviesToUpdate.add(updated)
                            }
                        }
                    }
                }
            }
        } else {
            Log.w("TraktSyncWorker", "Integrità Trakt fallita. Skip rimozioni film.")
        }

        // Salvataggio Massivo in SQLite
        movieRepository.saveMoviesBulk(moviesToUpdate)
    }
}
