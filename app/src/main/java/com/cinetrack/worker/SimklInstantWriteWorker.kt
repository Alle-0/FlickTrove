package com.cinetrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinetrack.data.api.SimklService
import com.cinetrack.data.api.SimklSyncHistoryRequest
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.SimklAuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SimklInstantWriteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val simklService: SimklService,
    private val simklAuthRepository: SimklAuthRepository,
    private val movieRepository: MovieRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            "trakt_sync_channel"
        )
            .setContentTitle("SIMKL")
            .setContentText(applicationContext.getString(com.cinetrack.R.string.auth_syncing))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
        return androidx.work.ForegroundInfo(1003, notification)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (simklAuthRepository.getAccessToken() == null) {
            return@withContext Result.success()
        }

        val action = inputData.getString(TraktInstantWriteWorker.KEY_ACTION) ?: return@withContext Result.failure()
        
        try {
            val mediaType = inputData.getString(TraktInstantWriteWorker.KEY_MEDIA_TYPE) ?: return@withContext Result.failure()
            val tmdbId = inputData.getLong(TraktInstantWriteWorker.KEY_TMDB_ID, -1L)
            
            if (tmdbId == -1L) return@withContext Result.failure()

            val ids = com.cinetrack.data.api.SimklIds(tmdb = tmdbId.toString())

            when (action) {
                // ── History (watched) ─────────────────────────────────────────
                TraktInstantWriteWorker.ACTION_MARK_WATCHED -> {
                    simklService.addToHistory(buildHistoryRequest(mediaType, ids))
                }
                TraktInstantWriteWorker.ACTION_REMOVE_WATCHED -> {
                    simklService.removeFromHistory(buildHistoryRequest(mediaType, ids))
                    // Hard-delete the pending_delete row once SIMKL has confirmed the removal
                    movieRepository.hardDeleteMovie(tmdbId, mediaType)
                    android.util.Log.d("SimklInstantWriteWorker", "Hard-deleted pending_delete row for $tmdbId ($mediaType)")
                }

                // ── Watchlist ─────────────────────────────────────────────────
                TraktInstantWriteWorker.ACTION_ADD_WATCHLIST -> {
                    simklService.addToWatchlist(buildWatchlistRequest(mediaType, ids))
                }
                TraktInstantWriteWorker.ACTION_REMOVE_WATCHLIST -> {
                    simklService.removeFromWatchlist(buildWatchlistRequest(mediaType, ids))
                }

                // ── Dropped ───────────────────────────────────────────────────
                TraktInstantWriteWorker.ACTION_MARK_DROPPED -> {
                    simklService.addToList(buildAddToListRequest(mediaType, ids, "dropped"))
                }
                TraktInstantWriteWorker.ACTION_REMOVE_DROPPED -> {
                    // There is no /remove-from-list endpoint in SIMKL doc. If you remove it from dropped, it probably returns to "not in list".
                    // You might need to use removeFromWatchlist? Or just ignore for now if SIMKL doesn't have an explicit remove from list API.
                    simklService.removeFromWatchlist(buildWatchlistRequest(mediaType, ids))
                }

                // ── Episodes ──────────────────────────────────────────────────
                TraktInstantWriteWorker.ACTION_MARK_EPISODES_WATCHED -> {
                    val encodedEps = inputData.getString(TraktInstantWriteWorker.KEY_SEASON_EPISODES)
                        ?: return@withContext Result.failure()
                    simklService.addToHistory(buildEpisodeHistoryRequest(mediaType, ids, encodedEps))
                }
                TraktInstantWriteWorker.ACTION_REMOVE_EPISODES_WATCHED -> {
                    val encodedEps = inputData.getString(TraktInstantWriteWorker.KEY_SEASON_EPISODES)
                        ?: return@withContext Result.failure()
                    simklService.removeFromHistory(buildEpisodeHistoryRequest(mediaType, ids, encodedEps))
                }
                else -> return@withContext Result.success() // Ignoriamo logiche come rating/custom lists che SIMKL potrebbe non supportare allo stesso modo
            }

            // SIMKL strict rate limit: delay slightly just in case WorkManager executes too fast
            kotlinx.coroutines.delay(1000)

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("SimklInstantWriteWorker", "Failed to write to SIMKL, will retry", e)
            Result.retry()
        }
    }

    private fun buildHistoryRequest(mediaType: String, ids: com.cinetrack.data.api.SimklIds): SimklSyncHistoryRequest {
        val item = com.cinetrack.data.api.SimklHistoryItem(ids = ids)
        return if (mediaType == "movie") {
            SimklSyncHistoryRequest(movies = listOf(item))
        } else if (mediaType == "anime") {
            SimklSyncHistoryRequest(anime = listOf(item))
        } else {
            SimklSyncHistoryRequest(shows = listOf(item))
        }
    }

    private fun buildWatchlistRequest(mediaType: String, ids: com.cinetrack.data.api.SimklIds): com.cinetrack.data.api.SimklSyncWatchlistRequest {
        val item = com.cinetrack.data.api.SimklMediaItem(ids = ids)
        return if (mediaType == "movie") {
            com.cinetrack.data.api.SimklSyncWatchlistRequest(movies = listOf(item))
        } else if (mediaType == "anime") {
            com.cinetrack.data.api.SimklSyncWatchlistRequest(anime = listOf(item))
        } else {
            com.cinetrack.data.api.SimklSyncWatchlistRequest(shows = listOf(item))
        }
    }

    private fun buildAddToListRequest(mediaType: String, ids: com.cinetrack.data.api.SimklIds, toList: String): com.cinetrack.data.api.SimklAddToListRequest {
        val item = com.cinetrack.data.api.SimklMediaItem(ids = ids)
        return if (mediaType == "movie") {
            com.cinetrack.data.api.SimklAddToListRequest(to = toList, movies = listOf(item))
        } else if (mediaType == "anime") {
            com.cinetrack.data.api.SimklAddToListRequest(to = toList, anime = listOf(item))
        } else {
            com.cinetrack.data.api.SimklAddToListRequest(to = toList, shows = listOf(item))
        }
    }

    private fun buildEpisodeHistoryRequest(
        mediaType: String,
        ids: com.cinetrack.data.api.SimklIds,
        encodedEps: String
    ): SimklSyncHistoryRequest {
        val seasonsMap = mutableMapOf<Int, MutableList<com.cinetrack.data.api.SimklSeasonEpisode>>()
        
        // Formato stringa: "1:1,2,3;2:5,6"
        val seasons = encodedEps.split(";").mapNotNull { part ->
            val colonIdx = part.indexOf(':')
            if (colonIdx == -1) return@mapNotNull null
            val seasonNum = part.substring(0, colonIdx).toIntOrNull() ?: return@mapNotNull null
            val epNums = part.substring(colonIdx + 1).split(",").mapNotNull { it.trim().toIntOrNull() }
            
            val eps = seasonsMap.getOrPut(seasonNum) { mutableListOf() }
            epNums.forEach { epNum ->
                eps.add(com.cinetrack.data.api.SimklSeasonEpisode(episode = epNum, number = epNum))
            }
        }
        val seasonsList = seasonsMap.map { com.cinetrack.data.api.SimklSeason(season = it.key, number = it.key, episodes = it.value) }
        
        val showWithEps = com.cinetrack.data.api.SimklHistoryItem(ids = ids, seasons = seasonsList.takeIf { it.isNotEmpty() })
        
        return if (mediaType == "anime") {
            SimklSyncHistoryRequest(anime = listOf(showWithEps))
        } else {
            SimklSyncHistoryRequest(shows = listOf(showWithEps))
        }
    }
}
