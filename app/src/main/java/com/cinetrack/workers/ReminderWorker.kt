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

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: FlickTroveDatabase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) // "yyyy-MM-dd"
            
            val favorites = database.favoriteDao().getAll()
            
            for (item in favorites) {
                if (!item.reminder) continue
                if (item.watched) continue
                
                if (item.mediaType == "movie") {
                    if (item.releaseDate == today) {
                        NotificationHelper.showReleaseNotification(
                            context = context,
                            movieTitle = item.displayName,
                            movieId = item.id,
                            mediaType = item.mediaType
                        )
                    }
                } else if (item.mediaType == "tv") {
                    // Check if the show itself premieres today
                    if (item.firstAirDate == today) {
                        NotificationHelper.showReleaseNotification(
                            context = context,
                            movieTitle = item.displayName,
                            movieId = item.id,
                            mediaType = item.mediaType
                        )
                    } else if (item.nextEpisodeAirDate == today) {
                        val epString = item.nextEpisodeString ?: "nuovo episodio"
                        NotificationHelper.showEpisodeReleaseNotification(
                            context = context,
                            showTitle = item.displayName,
                            showId = item.id,
                            episodeString = epString
                        )
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
