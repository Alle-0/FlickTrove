package com.cinetrack.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinetrack.data.repository.MovieRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val movieRepository: MovieRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Perform global sync (Firebase -> Room)
            movieRepository.syncWithFirebase()
            
            // Note: Individual local changes are already synced "fire-and-forget" 
            // in the repository. This worker ensures the full state is periodically reconciled.
            
            Result.success()
        } catch (e: Exception) {
            // Retry if it's a transient failure, otherwise fail
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "FlickTroveSyncWorker"
    }
}
