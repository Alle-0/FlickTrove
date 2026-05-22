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

class TVUpdateWorker(
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

        return try {
            val notifEnabled = settingsRepository.notificationsEnabled.first()
            val hasPermission = NotificationHelper.hasNotificationPermission(applicationContext)

            val tvShows: List<Movie> = repository.getLocalMovies()
                .filter { it.mediaType == "tv" }

            tvShows.forEach { show: Movie ->
                try {
                    val latest = repository.fetchMovieDetails(show.id, true)
                    val latestEps = latest.numberOfEpisodes ?: 0
                    val currentEps = show.numberOfEpisodes ?: 0

                    val newNextEpAirDate = latest.nextEpisodeToAir?.airDate
                    val newNextEpString = latest.nextEpisodeToAir?.let { "S${it.seasonNumber.toString().padStart(2, '0')}E${it.episodeNumber.toString().padStart(2, '0')}" }

                    if (latestEps > currentEps || show.nextEpisodeAirDate != newNextEpAirDate) {
                        val newEps = if (currentEps > 0 && latestEps > currentEps) latestEps - currentEps else 0

                        val updated = show.copy(
                            numberOfEpisodes = latestEps,
                            newEpisodesFound = (show.newEpisodesFound ?: 0) + newEps,
                            firstAirDate = latest.firstAirDate,
                            status = latest.status,
                            nextEpisodeAirDate = newNextEpAirDate,
                            nextEpisodeString = newNextEpString
                        )
                        repository.saveMovie(updated)

                        // Notify only if there are genuinely new episodes and the user wants it
                        if (newEps > 0 && notifEnabled && hasPermission) {
                            NotificationHelper.showNewEpisodesNotification(
                                context = applicationContext,
                                showTitle = show.title ?: show.name ?: "Serie TV",
                                showId = show.id,
                                newEpisodesCount = newEps
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Skip individual show failures, continue with the rest
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
