package com.cinetrack

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cinetrack.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltAndroidApp
class FlickTroveApplication : Application(), Configuration.Provider, coil.ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): coil.ImageLoader {
        return coil.ImageLoader.Builder(this)
            .components {
                add(coil.decode.SvgDecoder.Factory())
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val workManager = androidx.work.WorkManager.getInstance(this@FlickTroveApplication)
            
            val networkConstraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val localConstraints = androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            
            val tvUpdateRequest = androidx.work.PeriodicWorkRequestBuilder<com.cinetrack.worker.TVUpdateWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(networkConstraints)
                .build()
            
            val movieUpdateRequest = androidx.work.PeriodicWorkRequestBuilder<com.cinetrack.worker.MovieUpdateWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(networkConstraints)
                .build()
            
            val releaseReminderRequest = androidx.work.PeriodicWorkRequestBuilder<com.cinetrack.worker.ReminderWorker>(
                12, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(localConstraints)
                .build()
            
            // --- Trakt Sync Worker (24h) ---
            val traktSyncConstraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.UNMETERED)
                .build()

            val traktSyncRequest = androidx.work.PeriodicWorkRequestBuilder<com.cinetrack.worker.TraktSyncWorker>(
                6, java.util.concurrent.TimeUnit.HOURS
            )
            .setConstraints(traktSyncConstraints)
            .build()
            
            // --- SIMKL Sync Worker (24h) ---
            val simklSyncConstraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val simklSyncRequest = androidx.work.PeriodicWorkRequestBuilder<com.cinetrack.worker.SimklSyncWorker>(
                6, java.util.concurrent.TimeUnit.HOURS
            )
            .setConstraints(simklSyncConstraints)
            .build()
            
            workManager.cancelUniqueWork("ReminderMigration")
            
            workManager.enqueueUniquePeriodicWork(
                "TVUpdate",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                tvUpdateRequest
            )

            workManager.enqueueUniquePeriodicWork(
                "MovieUpdate",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                movieUpdateRequest
            )

            workManager.enqueueUniquePeriodicWork(
                "release_reminder_work",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                releaseReminderRequest
            )

            workManager.enqueueUniquePeriodicWork(
                "trakt_sync_work",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                traktSyncRequest
            )

            workManager.enqueueUniquePeriodicWork(
                "SimklSync",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                simklSyncRequest
            )

            // Refresh the home-screen widget daily so upcoming release dates
            // stay accurate even if TMDB updates them (e.g. a film is delayed).
            // UPDATE (instead of KEEP) ensures the schedule is always refreshed
            // even after app updates or reinstalls.
            val widgetRefreshRequest = androidx.work.PeriodicWorkRequestBuilder<com.cinetrack.worker.WidgetRefreshWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(localConstraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "WidgetDailyRefresh",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                widgetRefreshRequest
            )

            // Also trigger an immediate one-shot update so the widget is always
            // fresh right after app startup (avoids stale content between updates).
            val immediateWidgetRefresh = androidx.work.OneTimeWorkRequestBuilder<com.cinetrack.worker.WidgetRefreshWorker>()
                .setConstraints(localConstraints)
                .build()
            workManager.enqueue(immediateWidgetRefresh)
            
            // Trigger an immediate sync for SIMKL on startup to keep data fresh
            val immediateSimklSync = androidx.work.OneTimeWorkRequestBuilder<com.cinetrack.worker.SimklSyncWorker>()
                .setConstraints(simklSyncConstraints)
                .build()
            workManager.enqueueUniqueWork(
                "SimklSyncStartup",
                androidx.work.ExistingWorkPolicy.KEEP,
                immediateSimklSync
            )

            // Trigger an immediate sync for Trakt on startup to keep data fresh
            val immediateTraktSync = androidx.work.OneTimeWorkRequestBuilder<com.cinetrack.worker.TraktSyncWorker>()
                .setConstraints(traktSyncConstraints)
                .build()
            workManager.enqueueUniqueWork(
                "TraktSyncStartup",
                androidx.work.ExistingWorkPolicy.KEEP,
                immediateTraktSync
            )
        }
    }
}
