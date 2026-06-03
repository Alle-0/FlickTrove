package com.cinetrack

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cinetrack.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FlickTrove_KotlinApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)

        val workManager = androidx.work.WorkManager.getInstance(this)
        
        val networkConstraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val localConstraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        
        val migrationRequest = androidx.work.PeriodicWorkRequestBuilder<com.cinetrack.worker.ReminderMigrationWorker>(
            12, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(localConstraints)
            .build()
        
        val tvUpdateRequest = androidx.work.PeriodicWorkRequestBuilder<com.cinetrack.worker.TVUpdateWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(networkConstraints)
            .build()
        
        val releaseReminderRequest = androidx.work.PeriodicWorkRequestBuilder<com.cinetrack.workers.ReminderWorker>(
            12, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(localConstraints)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "ReminderMigration",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            migrationRequest
        )
        
        workManager.enqueueUniquePeriodicWork(
            "TVUpdate",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            tvUpdateRequest
        )

        workManager.enqueueUniquePeriodicWork(
            "ReleaseReminder",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            releaseReminderRequest
        )
    }
}
