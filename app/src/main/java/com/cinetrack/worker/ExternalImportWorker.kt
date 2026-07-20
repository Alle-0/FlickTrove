package com.cinetrack.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cinetrack.R
import com.cinetrack.data.repository.BackupRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.UiText
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class ExternalImportWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val NOTIFICATION_ID_PROGRESS = 1003
    private val NOTIFICATION_ID_COMPLETE = 1004
    private val CHANNEL_ID = "external_import_channel"

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createChannel()
        val title = appContext.getString(R.string.settings_sync_backup)
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(appContext.getString(R.string.settings_processing))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID_PROGRESS, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID_PROGRESS, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val title = appContext.getString(R.string.settings_sync_backup)
            val channel = NotificationChannel(
                CHANNEL_ID,
                title,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val filePath = inputData.getString("filePath") ?: return@withContext Result.failure()
        val isRestore = inputData.getBoolean("isRestore", false)
        val keepLatestWatchDate = inputData.getBoolean("keepLatestWatchDate", true)

        val file = File(filePath)
        if (!file.exists()) {
            return@withContext Result.failure()
        }

        try {
            try {
                setForeground(getForegroundInfo())
            } catch (e: Exception) {
                // Ignore if foreground service permission is not available or not required
            }

            if (isRestore) {
                file.inputStream().use { backupRepository.importDataStream(it) }
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_restore_success))
                showCompletionNotification(appContext.getString(R.string.settings_msg_restore_success), false)
            } else {
                val contentStart = file.bufferedReader().use { it.readText().take(100).trimStart() }
                val isJson = contentStart.startsWith("[") || contentStart.startsWith("{")
                val count = if (isJson) {
                    file.inputStream().use { backupRepository.migrateTraktStream(it, keepLatestWatchDate) }
                } else {
                    file.inputStream().use { backupRepository.migrateCsvStream(it, keepLatestWatchDate) }
                }
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_import_success, count))
                val msgText = appContext.getString(R.string.settings_msg_import_success, count)
                showCompletionNotification(msgText, false)
            }
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("ExternalImportWorker", "Import error", e)
            val errorRes = if (isRestore) R.string.settings_msg_restore_error else R.string.settings_msg_import_error
            actionFeedbackManager.emit(UiText.StringResource(errorRes))
            showCompletionNotification(appContext.getString(errorRes), true)
            Result.failure()
        } finally {
            try {
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                // Ignore file deletion errors
            }
        }
    }

    private fun showCompletionNotification(message: String, isError: Boolean) {
        createChannel()
        val title = appContext.getString(R.string.settings_sync_backup)
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(if (isError) android.R.drawable.stat_notify_error else android.R.drawable.stat_notify_sync)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }
}
