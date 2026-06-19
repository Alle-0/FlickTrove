package com.cinetrack.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinetrack.data.Movie
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class MovieUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RepositoryEntryPoint {
        fun movieRepository(): MovieRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            RepositoryEntryPoint::class.java
        )
        val repository = entryPoint.movieRepository()

        return try {
            val movies: List<Movie> = repository.getUpcomingMoviesForUpdate(150)

            movies.forEach { movie: Movie ->
                try {
                    // Delay to prevent TMDB API rate-limiting (429)
                    kotlinx.coroutines.delay(500)
                    
                    val latest = repository.fetchMovieDetails(movie.id, false)

                    // Check if releaseDate, runtime or status changed
                    var needsUpdate = false
                    var updatedMovie = movie

                    if (latest.releaseDate != movie.releaseDate) {
                        updatedMovie = updatedMovie.copy(releaseDate = latest.releaseDate)
                        needsUpdate = true
                    }
                    if (latest.status != movie.status) {
                        updatedMovie = updatedMovie.copy(status = latest.status)
                        needsUpdate = true
                    }
                    if (latest.runtime != movie.runtime) {
                        updatedMovie = updatedMovie.copy(runtime = latest.runtime)
                        needsUpdate = true
                    }

                    if (needsUpdate) {
                        repository.saveMovie(updatedMovie)
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    // Skip individual movie failures, continue with the rest
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.retry()
        }
    }
}
