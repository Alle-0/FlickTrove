package com.cinetrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinetrack.data.api.TraktService
import com.cinetrack.data.api.TraktSyncIds
import com.cinetrack.data.api.TraktSyncMovie
import com.cinetrack.data.api.TraktSyncRequest
import com.cinetrack.data.api.TraktSyncShow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class TraktInstantWriteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val traktService: TraktService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val action = inputData.getString(KEY_ACTION) ?: return@withContext Result.failure()
        val mediaType = inputData.getString(KEY_MEDIA_TYPE) ?: return@withContext Result.failure()
        val tmdbId = inputData.getLong(KEY_TMDB_ID, -1L)
        val imdbId = inputData.getString(KEY_IMDB_ID)

        if (tmdbId == -1L && imdbId == null) {
            return@withContext Result.failure()
        }

        try {
            val syncRequest = buildSyncRequest(mediaType, tmdbId, imdbId)

            when (action) {
                ACTION_MARK_WATCHED -> {
                    traktService.addToHistory(syncRequest)
                }
                ACTION_REMOVE_WATCHED -> {
                    traktService.removeFromHistory(syncRequest)
                }
                // Add more actions here (watchlist, rating, etc.)
                else -> return@withContext Result.failure()
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("TraktInstantWriteWorker", "Failed to write to Trakt, will retry", e)
            Result.retry() // Exponential backoff applied automatically by WorkManager
        }
    }

    private fun buildSyncRequest(mediaType: String, tmdbId: Long, imdbId: String?): TraktSyncRequest {
        val ids = TraktSyncIds(
            tmdb = if (tmdbId != -1L) tmdbId else null,
            imdb = imdbId
        )
        return if (mediaType == "movie") {
            TraktSyncRequest(movies = listOf(TraktSyncMovie(ids)))
        } else {
            TraktSyncRequest(shows = listOf(TraktSyncShow(ids)))
        }
    }

    companion object {
        const val KEY_ACTION = "action"
        const val KEY_MEDIA_TYPE = "media_type"
        const val KEY_TMDB_ID = "tmdb_id"
        const val KEY_IMDB_ID = "imdb_id"

        const val ACTION_MARK_WATCHED = "mark_watched"
        const val ACTION_REMOVE_WATCHED = "remove_watched"
    }
}
