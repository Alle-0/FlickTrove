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
            val missingDetailsMovies = allMovies.filter { 
                it.runtime == null || it.runtime == 0 || it.topCastData.isNullOrEmpty()
            }

            if (missingDetailsMovies.isEmpty()) {
                return Result.success()
            }

            val total = missingDetailsMovies.size
            Log.d("LibraryDetailsSync", "Found $total movies missing details")

            missingDetailsMovies.forEachIndexed { index, movie ->
                try {
                    setProgress(androidx.work.workDataOf("current" to index, "total" to total))
                    val isTv = movie.mediaType == "tv"
                    val response = repository.fetchMovieDetails(movie.id, isTv)
                    val freshMovie = MovieMapper.mapResponseToMovie(response, if (isTv) "tv" else "movie")
                    
                    favoriteDao.updateMetadata(
                        id = movie.id,
                        mediaType = movie.mediaType,
                        runtime = freshMovie.runtime,
                        episodeRunTime = freshMovie.episodeRunTime,
                        genres = freshMovie.genres,
                        topCastData = freshMovie.topCastData,
                        directorData = freshMovie.directorData,
                        directorId = freshMovie.directorId,
                        directorName = freshMovie.directorName,
                        directorProfilePath = freshMovie.directorProfilePath,
                        seasons = freshMovie.seasons,
                        numberOfSeasons = freshMovie.numberOfSeasons,
                        numberOfEpisodes = freshMovie.numberOfEpisodes
                    )
                    
                    val localMovie = favoriteDao.getById(movie.id, movie.mediaType)
                    if (localMovie != null) {
                        try {
                            firebase.setMovie(localMovie)
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                        }
                    }
                    
                    // Delay to prevent TMDB API rate-limiting (429)
                    delay(500)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("LibraryDetailsSync", "Failed to fetch missing details for ${movie.id}", e)
                }
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
