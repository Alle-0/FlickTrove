package com.cinetrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinetrack.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic WorkManager worker that refreshes the home-screen widget once a day.
 * This ensures the "next upcoming release" shown in the widget stays up-to-date
 * even when release dates change on TMDB (e.g. a film is delayed/moved forward).
 *
 * Scheduled at app startup with [ExistingPeriodicWorkPolicy.KEEP] so re-installs
 * or reboots do not reset the schedule.
 */
@HiltWorker
class WidgetRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            WidgetUpdater.update(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
