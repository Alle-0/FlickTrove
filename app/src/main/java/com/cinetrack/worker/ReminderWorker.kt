package com.cinetrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinetrack.data.local.database.FlickTroveDatabase
import com.cinetrack.util.NotificationHelper
import com.cinetrack.util.NotificationTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.cinetrack.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: FlickTroveDatabase,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val notifEnabled = settingsRepository.notificationsReleases.first()
            val hasPermission = NotificationHelper.hasNotificationPermission(context)
            val now = LocalDate.now()
            val today = now.format(DateTimeFormatter.ISO_LOCAL_DATE) // "yyyy-MM-dd"
            val sevenDaysAgo = now.minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val threeDaysAgo = now.minusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE)

            NotificationTracker.cleanupStaleEntries(context)
            
            val favorites = database.favoriteDao().getAll()
            
            for (item in favorites) {
                if (!item.reminder) continue
                if (item.watched) continue
                
                if (item.mediaType == "movie") {
                    val rDate = item.releaseDate
                    if (!rDate.isNullOrEmpty() && rDate <= today) {
                        // Strict AND condition: notify only if within the last 7 days and not yet notified
                        val shouldNotify = rDate >= sevenDaysAgo && !NotificationTracker.wasReleaseNotified(context, "movie", item.id, rDate)

                        if (shouldNotify && !item.remindersMuted && notifEnabled && hasPermission) {
                            NotificationHelper.showReleaseNotificationSync(
                                context = context,
                                movieTitle = item.displayName,
                                movieId = item.id,
                                mediaType = item.mediaType,
                                posterPath = item.posterPath
                            )
                            NotificationTracker.markReleaseNotified(context, "movie", item.id, rDate)
                        }

                        val updated = item.copy(
                            favorite = true,
                            reminder = false,
                            migratedAt = if (rDate >= sevenDaysAgo) today else item.migratedAt,
                            clientUpdatedAt = System.currentTimeMillis(),
                            syncStatus = "pending"
                        )
                        database.favoriteDao().insert(updated)
                    }
                } else if (item.mediaType == "tv") {
                    // Check if the show itself premiered recently (Doze resilience)
                    val premiereDate = item.firstAirDate
                    val isRecentPremiere = !premiereDate.isNullOrEmpty() && premiereDate <= today && premiereDate >= threeDaysAgo

                    if (isRecentPremiere && !NotificationTracker.wasReleaseNotified(context, "tv", item.id, premiereDate)) {
                        if (!item.remindersMuted && notifEnabled && hasPermission) {
                            NotificationHelper.showReleaseNotificationSync(
                                context = context,
                                movieTitle = item.displayName,
                                movieId = item.id,
                                mediaType = item.mediaType,
                                posterPath = item.posterPath
                            )
                        }
                        NotificationTracker.markReleaseNotified(context, "tv", item.id, premiereDate)
                        val updated = item.copy(
                            migratedAt = today,
                            clientUpdatedAt = System.currentTimeMillis(),
                            syncStatus = "pending"
                        )
                        database.favoriteDao().insert(updated)
                    } else {
                        val epAirDate = item.nextEpisodeAirDate
                        val isRecentEpisode = !epAirDate.isNullOrEmpty() && epAirDate <= today && epAirDate >= threeDaysAgo
                        val epString = item.nextEpisodeString ?: "nuovo episodio"

                        if (isRecentEpisode && !NotificationTracker.wasEpisodeNotified(context, item.id, epString)) {
                            if (!item.remindersMuted && notifEnabled && hasPermission) {
                                NotificationHelper.showEpisodeReleaseNotificationSync(
                                    context = context,
                                    showTitle = item.displayName,
                                    showId = item.id,
                                    episodeString = epString,
                                    posterPath = item.posterPath
                                )
                            }
                            NotificationTracker.markEpisodeNotified(context, item.id, epString, today)
                            val updated = item.copy(
                                migratedAt = today,
                                clientUpdatedAt = System.currentTimeMillis(),
                                syncStatus = "pending"
                            )
                            database.favoriteDao().insert(updated)
                        }
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            Result.retry()
        }
    }
}
