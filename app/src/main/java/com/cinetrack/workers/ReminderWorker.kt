package com.cinetrack.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinetrack.data.local.database.FlickTroveDatabase
import com.cinetrack.util.NotificationHelper
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
            val notifEnabled = settingsRepository.notificationsEnabled.first()
            val hasPermission = NotificationHelper.hasNotificationPermission(context)
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) // "yyyy-MM-dd"
            
            val favorites = database.favoriteDao().getAll()
            
            for (item in favorites) {
                if (!item.reminder) continue
                if (item.watched) continue
                
                if (item.mediaType == "movie") {
                    val rDate = item.releaseDate
                    if (!rDate.isNullOrEmpty() && rDate <= today) {
                        if (notifEnabled && hasPermission) {
                            NotificationHelper.showReleaseNotification(
                                context = context,
                                movieTitle = item.displayName,
                                movieId = item.id,
                                mediaType = item.mediaType,
                                posterPath = item.posterPath
                            )
                        }
                        val updated = item.copy(
                            favorite = true,
                            reminder = false,
                            migratedAt = today,
                            clientUpdatedAt = System.currentTimeMillis(),
                            syncStatus = "pending"
                        )
                        database.favoriteDao().insert(updated)
                    }
                } else if (item.mediaType == "tv") {
                    if (item.migratedAt == today) continue

                    // Check if the show itself premieres today
                    if (item.firstAirDate == today) {
                        if (notifEnabled && hasPermission) {
                            NotificationHelper.showReleaseNotification(
                                context = context,
                                movieTitle = item.displayName,
                                movieId = item.id,
                                mediaType = item.mediaType,
                                posterPath = item.posterPath
                            )
                        }
                        val updated = item.copy(
                            migratedAt = today,
                            clientUpdatedAt = System.currentTimeMillis(),
                            syncStatus = "pending"
                        )
                        database.favoriteDao().insert(updated)
                    } else if (item.nextEpisodeAirDate == today) {
                        val epString = item.nextEpisodeString ?: "nuovo episodio"
                        if (notifEnabled && hasPermission) {
                            NotificationHelper.showEpisodeReleaseNotification(
                                context = context,
                                showTitle = item.displayName,
                                showId = item.id,
                                episodeString = epString,
                                posterPath = item.posterPath
                            )
                        }
                        val updated = item.copy(
                            migratedAt = today,
                            clientUpdatedAt = System.currentTimeMillis(),
                            syncStatus = "pending"
                        )
                        database.favoriteDao().insert(updated)
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
