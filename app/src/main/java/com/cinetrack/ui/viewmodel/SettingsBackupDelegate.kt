package com.cinetrack.ui.viewmodel

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.BackupRepository
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.UiText
import com.cinetrack.worker.ExternalImportWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegate responsible for local backup export/restore, external Cinemaniac migrations,
 * rewatch count/cleanup, and local/total database wipes.
 */
@Singleton
class SettingsBackupDelegate @Inject constructor(
    private val backupRepository: BackupRepository,
    private val movieRepository: MovieRepository,
    private val actionFeedbackManager: ActionFeedbackManager,
    @ApplicationContext private val context: Context
) {

    suspend fun getBackupData(): String? {
        return try {
            backupRepository.exportData()
        } catch (e: Exception) {
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_export_error))
            null
        }
    }

    fun restoreData(json: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "restore_payload_${System.currentTimeMillis()}.tmp")
                tempFile.writeText(json)
                restoreFile(tempFile.absolutePath, scope)
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_restore_error))
            }
        }
    }

    fun restoreFile(filePath: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val workRequest = OneTimeWorkRequestBuilder<ExternalImportWorker>()
                    .setInputData(
                        workDataOf(
                            "filePath" to filePath,
                            "isRestore" to true,
                            "keepLatestWatchDate" to true
                        )
                    )
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "ExternalImportWork",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_import_started))
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_restore_error))
            }
        }
    }

    fun wipeLocalData(scope: CoroutineScope) {
        scope.launch {
            try {
                movieRepository.wipeLocalData()
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_action_success))
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.DynamicString(e.message ?: "Failed to wipe local data"))
            }
        }
    }

    fun wipeTotalData(scope: CoroutineScope) {
        scope.launch {
            try {
                movieRepository.wipeTotalData()
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_action_success))
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.DynamicString(e.message ?: "Failed to wipe total data"))
            }
        }
    }

    fun countDuplicateRewatches(scope: CoroutineScope, onResult: (Int) -> Unit) {
        scope.launch {
            try {
                val count = movieRepository.countDuplicateRewatches()
                onResult(count)
            } catch (e: Exception) {
                onResult(0)
            }
        }
    }

    fun cleanupRewatches(scope: CoroutineScope, onComplete: (Int) -> Unit) {
        scope.launch {
            try {
                val deletedCount = movieRepository.cleanupAllRewatches()
                if (deletedCount > 0) {
                    actionFeedbackManager.emit(UiText.StringResource(R.string.settings_cleanup_rewatches_success, deletedCount))
                } else {
                    actionFeedbackManager.emit(UiText.StringResource(R.string.settings_cleanup_rewatches_none))
                }
                onComplete(deletedCount)
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.DynamicString(e.message ?: "Failed to clean up rewatches"))
                onComplete(0)
            }
        }
    }

    fun migrateExternalData(fileContent: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "migrate_payload_${System.currentTimeMillis()}.tmp")
                tempFile.writeText(fileContent)
                migrateExternalFile(tempFile.absolutePath, scope)
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_import_error))
            }
        }
    }

    fun migrateExternalFile(filePath: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val workRequest = OneTimeWorkRequestBuilder<ExternalImportWorker>()
                    .setInputData(
                        workDataOf(
                            "filePath" to filePath,
                            "isRestore" to false
                        )
                    )
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "ExternalImportWork",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_import_started))
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_import_error))
            }
        }
    }

    fun deleteUnmatchedItem(movie: Movie, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                movieRepository.deleteMovie(movie)
            } catch (_: Exception) {
                // Ignore or show error
            }
        }
    }
}
