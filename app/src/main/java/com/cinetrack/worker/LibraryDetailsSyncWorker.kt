package com.cinetrack.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.mapper.MovieMapper
import com.cinetrack.data.remote.FirebaseRemoteDataSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class LibraryDetailsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncEntryPoint {
        fun favoriteDao(): FavoriteDao
        fun movieRepository(): MovieRepository
        fun firebaseRemoteDataSource(): FirebaseRemoteDataSource
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncEntryPoint::class.java
        )
        val favoriteDao = entryPoint.favoriteDao()
        val repository = entryPoint.movieRepository()
        val firebase = entryPoint.firebaseRemoteDataSource()

        return try {
            val allMovies = favoriteDao.getAll()
            val missingDetailsMovies = allMovies.filter { movie ->
                val eps = movie.watchedEpisodes
                movie.runtime == null || movie.runtime == 0 || movie.topCastData.isNullOrEmpty() || (movie.mediaType == "tv" && movie.watched && (eps.isNullOrEmpty() || eps.values.sumOf { s -> s.size } == 0))
            }

            if (missingDetailsMovies.isEmpty()) {
                return Result.success()
            }

            val total = missingDetailsMovies.size
            Log.d("LibraryDetailsSync", "Found $total movies missing details")

            val updateEpisodesUseCase = com.cinetrack.domain.UpdateEpisodesUseCase()
            val updatedForFirebase = mutableListOf<com.cinetrack.data.model.Movie>()

            missingDetailsMovies.forEachIndexed { index, movie ->
                try {
                    setProgress(androidx.work.workDataOf("current" to index, "total" to total))
                    val isTv = movie.mediaType == "tv"
                    val response = repository.fetchMovieDetails(movie.id, isTv)
                    val freshMovie = MovieMapper.mapResponseToMovie(response, if (isTv) "tv" else "movie")
                    
                    var currentLocal = favoriteDao.getById(movie.id, movie.mediaType)
                    if (currentLocal != null) {
                        currentLocal = currentLocal.copy(
                            runtime = freshMovie.runtime ?: currentLocal.runtime,
                            episodeRunTime = freshMovie.episodeRunTime ?: currentLocal.episodeRunTime,
                            genres = freshMovie.genres ?: currentLocal.genres,
                            topCastData = freshMovie.topCastData ?: currentLocal.topCastData,
                            directorData = freshMovie.directorData ?: currentLocal.directorData,
                            directorId = freshMovie.directorId ?: currentLocal.directorId,
                            directorName = freshMovie.directorName ?: currentLocal.directorName,
                            directorProfilePath = freshMovie.directorProfilePath ?: currentLocal.directorProfilePath,
                            seasons = freshMovie.seasons ?: currentLocal.seasons,
                            numberOfSeasons = freshMovie.numberOfSeasons ?: currentLocal.numberOfSeasons,
                            numberOfEpisodes = freshMovie.numberOfEpisodes ?: currentLocal.numberOfEpisodes,
                            posterPath = freshMovie.posterPath ?: currentLocal.posterPath,
                            backdropPath = freshMovie.backdropPath ?: currentLocal.backdropPath,
                            overview = freshMovie.overview ?: currentLocal.overview,
                            firstAirDate = freshMovie.firstAirDate ?: currentLocal.firstAirDate,
                            releaseDate = freshMovie.releaseDate ?: currentLocal.releaseDate,
                            releaseYear = freshMovie.releaseYear ?: currentLocal.releaseYear,
                            status = freshMovie.status ?: currentLocal.status,
                            voteAverage = freshMovie.voteAverage ?: currentLocal.voteAverage,
                            voteCount = freshMovie.voteCount ?: currentLocal.voteCount
                        )
                        favoriteDao.insert(currentLocal)
                    }
                    val currentEps = currentLocal?.watchedEpisodes
                    if (isTv && currentLocal != null) {
                        val currentEps = currentLocal.watchedEpisodes
                        if (currentLocal.watched && (currentEps.isNullOrEmpty() || currentEps.values.sumOf { it.size } == 0)) {
                            val allWatched = updateEpisodesUseCase.markAllWatched(freshMovie).watchedEpisodes
                            if (!allWatched.isNullOrEmpty()) {
                                currentLocal = currentLocal.copy(watchedEpisodes = allWatched, progress = 1.0)
                                favoriteDao.insert(currentLocal)
                            }
                        } else if (!currentLocal.watched && !currentEps.isNullOrEmpty()) {
                            val totalWatched = currentEps.filter { it.key != "0" }.values.sumOf { it.size }
                            val totalEpisodes = freshMovie.effectiveTotalEpisodes
                            val progressVal = if (totalEpisodes > 0) (totalWatched.toDouble() / totalEpisodes).coerceIn(0.0, 1.0) else 0.0
                            
                            var updatedLocal = currentLocal.copy(progress = progressVal)
                            
                            if (totalEpisodes > 0 && totalWatched >= totalEpisodes) {
                                updatedLocal = updatedLocal.copy(
                                    watched = true,
                                    favorite = false,
                                    reminder = false,
                                    newEpisodesFound = 0,
                                    watchedAt = currentLocal.watchedAt ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
                                )
                            }
                            
                            if (updatedLocal != currentLocal) {
                                currentLocal = updatedLocal
                                favoriteDao.insert(currentLocal)
                            }
                        }
                    }

                    if (currentLocal != null) {
                        updatedForFirebase.add(currentLocal)
                        if (updatedForFirebase.size >= 25) {
                            try {
                                firebase.setMoviesBulk(updatedForFirebase.toList())
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                            updatedForFirebase.clear()
                        }
                    }
                    
                    // Delay to prevent TMDB API rate-limiting (429)
                    delay(500)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("LibraryDetailsSync", "Failed to fetch missing details for ${movie.id}", e)
                }
            }
            
            if (updatedForFirebase.isNotEmpty()) {
                try {
                    firebase.setMoviesBulk(updatedForFirebase)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
                updatedForFirebase.clear()
            }

            setProgress(androidx.work.workDataOf("current" to total, "total" to total))
            Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("LibraryDetailsSync", "Worker failed", e)
            Result.retry()
        }
    }
}
