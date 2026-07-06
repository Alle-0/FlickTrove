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

            val tvShows: List<Movie> = repository.getShowsForUpdate(150)

            tvShows.forEach { show: Movie ->
                try {
                    // Delay to prevent TMDB API rate-limiting (429)
                    kotlinx.coroutines.delay(500)
                    
                    val latest = repository.fetchMovieDetails(show.id, true)
                    val latestEps = latest.numberOfEpisodes ?: 0
                    val currentEps = show.numberOfEpisodes ?: 0

                    val newNextEpAirDate = latest.nextEpisodeToAir?.airDate
                    val newNextEpString = latest.nextEpisodeToAir?.let { "S${it.seasonNumber.toString().padStart(2, '0')}E${it.episodeNumber.toString().padStart(2, '0')}" }

                    val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    val isNextEpInFuture = newNextEpAirDate != null && newNextEpAirDate > today
                    val newLastAirDate = latest.lastEpisodeToAir?.airDate ?: latest.lastAirDate
                    val oldLastAirDate = show.lastAirDate

                    if (latestEps > currentEps || show.nextEpisodeAirDate != newNextEpAirDate || newLastAirDate != oldLastAirDate) {
                        // Case 1: TMDB database expanded with new episodes that are already released
                        val addedAndReleasedEps = if (currentEps > 0 && latestEps > currentEps && !isNextEpInFuture) {
                            val airedEps = calculateAiredEpisodes(latest)
                            if (airedEps > currentEps) airedEps - currentEps else 0
                        } else {
                            0
                        }

                        // Case 2: An episode that was already planned in TMDB (e.g. announced last year) has FINALLY AIRED!
                        val newlyAiredPlannedEps = if (newLastAirDate != null && oldLastAirDate != null && newLastAirDate > oldLastAirDate && newLastAirDate <= today) {
                            if (addedAndReleasedEps > 0) 0 else 1
                        } else {
                            0
                        }

                        val releasedNewEps = addedAndReleasedEps + newlyAiredPlannedEps

                        val updated = show.copy(
                            numberOfEpisodes = latestEps,
                            newEpisodesFound = (show.newEpisodesFound ?: 0) + releasedNewEps,
                            firstAirDate = latest.firstAirDate,
                            lastAirDate = newLastAirDate ?: show.lastAirDate,
                            status = latest.status,
                            nextEpisodeAirDate = newNextEpAirDate,
                            nextEpisodeString = newNextEpString
                        )
                        repository.saveMovie(updated)

                        // Notify only if there are genuinely released new episodes and the user wants it
                        if (releasedNewEps > 0 && notifEnabled && hasPermission) {
                            NotificationHelper.showNewEpisodesNotification(
                                context = applicationContext,
                                showTitle = show.title ?: show.name ?: "Serie TV",
                                showId = show.id,
                                newEpisodesCount = releasedNewEps
                            )
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    // Skip individual show failures, continue with the rest
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.retry()
        }
    }

    private fun calculateAiredEpisodes(response: com.cinetrack.data.api.MovieDetailResponse): Int {
        val totalEps = response.numberOfEpisodes ?: 0
        val lastEp = response.lastEpisodeToAir
        val seasons = response.seasons

        if (lastEp != null && !seasons.isNullOrEmpty()) {
            val lastSeasonNum = lastEp.seasonNumber
            val lastEpNum = lastEp.episodeNumber

            val priorSeasonsEps = seasons
                .filter { it.seasonNumber > 0 && it.seasonNumber < lastSeasonNum }
                .sumOf { it.episodeCount ?: 0 }

            val calculatedAired = priorSeasonsEps + lastEpNum
            return minOf(calculatedAired, totalEps)
        }

        return totalEps
    }
}
