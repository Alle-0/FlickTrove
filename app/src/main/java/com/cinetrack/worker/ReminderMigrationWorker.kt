package com.cinetrack.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinetrack.data.Movie
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.SettingsRepository
import com.cinetrack.util.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class ReminderMigrationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RepositoryEntryPoint {
        fun movieRepository(): MovieRepository
        fun settingsRepository(): SettingsRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            RepositoryEntryPoint::class.java
        )
        val repository = entryPoint.movieRepository()
        val settingsRepository = entryPoint.settingsRepository()
        val today = LocalDate.now().toString()

        return try {
            val localMovies = repository.getLocalMovies()

            val toMigrate = localMovies.filter { movie: Movie ->
                val date = movie.releaseDate ?: movie.firstAirDate ?: ""
                movie.reminder && date.isNotEmpty() && date <= today
            }

            // Read the preference once – honour the user's choice
            val notifEnabled = settingsRepository.notificationsEnabled.first()
            // Also check the runtime permission (device-level guard)
            val hasPermission = NotificationHelper.hasNotificationPermission(applicationContext)

            toMigrate.forEach { movie: Movie ->
                // Move movie: reminder → favorite (only if not already watched)
                val updated = movie.copy(
                    favorite = !movie.watched,
                    reminder = false,
                    migratedAt = today
                )
                repository.saveMovie(updated)

                // Show notification only when both the user preference
                // and the system permission are satisfied
                if (notifEnabled && hasPermission) {
                    NotificationHelper.showReleaseNotification(
                        context = applicationContext,
                        movieTitle = movie.title ?: movie.name ?: "Contenuto rilasciato",
                        movieId = movie.id,
                        mediaType = movie.mediaType,
                        posterPath = movie.posterPath
                    )
                }
                
                // Rate-limit network requests to Firebase and OkHttp pool
                kotlinx.coroutines.delay(200)
            }

            Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.retry()
        }
    }
}
