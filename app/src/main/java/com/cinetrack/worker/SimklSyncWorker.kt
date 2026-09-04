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
import androidx.work.workDataOf
import com.cinetrack.R
import com.cinetrack.data.api.SimklHistoryItem
import com.cinetrack.data.api.SimklIds
import com.cinetrack.data.api.SimklMediaItem
import com.cinetrack.data.api.SimklService
import com.cinetrack.data.api.SimklSyncHistoryRequest
import com.cinetrack.data.api.SimklSyncWatchlistRequest
import com.cinetrack.data.api.SimklAddToListRequest
import com.cinetrack.data.api.TMDBService
import com.cinetrack.data.mapper.MovieMapper
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.SimklAuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class SimklSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val simklService: SimklService,
    private val tmdbService: TMDBService,
    private val authRepository: SimklAuthRepository,
    private val movieRepository: MovieRepository
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val NOTIFICATION_ID = 2001
    private val CHANNEL_ID = "simkl_sync_channel"

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.simkl_connect))
            .setContentText("Preparing SIMKL Sync...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SIMKL Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background synchronization for SIMKL"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun updateProgress(desc: String, current: Int = 0, total: Int = 0) {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("SIMKL Sync")
            .setContentText(desc)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)

        if (total > 0) {
            builder.setProgress(total, current, false)
            setProgress(workDataOf("current" to current, "total" to total, "status" to desc))
        } else {
            builder.setProgress(0, 0, true)
            setProgress(workDataOf("status" to desc))
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (authRepository.getAccessToken().isNullOrEmpty()) {
            return@withContext Result.success()
        }

        try {
            setForeground(getForegroundInfo())
            
            val force = inputData.getBoolean("force", false)
            val isFirstSync = !authRepository.isFirstSyncCompleted()
            
            updateProgress("App ➔ SIMKL: Pushing local changes...")
            pushPendingChanges(force || isFirstSync)
            
            val activities = simklService.getActivities()
            val remoteLastSync = activities.all ?: ""
            val localLastSync = authRepository.getLastSyncTime() ?: ""

            if (!force && !isFirstSync && remoteLastSync == localLastSync) {
                // Nothing changed on the server
                return@withContext Result.success()
            }

            if (isFirstSync) {
                updateProgress("SIMKL ➔ App: Downloading library...")
                val movies = simklService.getSyncMovies()
                processPhaseItems(movies, isMovie = true)
                
                val shows = simklService.getSyncShows()
                processPhaseItems(shows, isMovie = false)

                val anime = simklService.getSyncAnime()
                processPhaseItems(anime, isMovie = false)

                authRepository.setFirstSyncCompleted(true)
                authRepository.saveLastSyncTime(remoteLastSync)
            } else {
                updateProgress("SIMKL ➔ App: Downloading updates...")
                // We use the localLastSync to fetch only what changed since our last successful sync
                val delta = simklService.getSyncAllItems(localLastSync)
                processPhaseItems(delta.movies ?: emptyList(), isMovie = true)
                processPhaseItems(delta.shows ?: emptyList(), isMovie = false)
                processPhaseItems(delta.anime ?: emptyList(), isMovie = false)
                
                authRepository.saveLastSyncTime(remoteLastSync)
            }

            updateProgress("Sync complete!", 100, 100)
            return@withContext Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.retry()
        }
    }

    private suspend fun pushPendingChanges(forceFullSync: Boolean) {
        val allLocal = movieRepository.getLocalMovies()
        
        // Se forceFullSync è true, eseguiamo un MERGE: inviamo a SIMKL tutto il nostro
        // DB locale (visti, watchlist, dropped). Questo copre il caso del primo login.
        val pendingItems = if (forceFullSync) {
            allLocal.filter { it.watched || it.favorite || it.dropped }
        } else {
            allLocal.filter { it.syncStatus == "pending_sync" }
        }
        
        if (pendingItems.isEmpty()) return

        val pendingMovies = pendingItems.filter { it.mediaType == "movie" }
        val pendingTv = pendingItems.filter { it.mediaType == "tv" }

        suspend fun pushItems(items: List<Movie>, isMovie: Boolean) {
            val watchedToPush = items.filter { it.watched }
            val watchlistToPush = items.filter { it.favorite && !it.watched }
            val droppedToPush = items.filter { it.dropped }

            // IMPORTANTE: Come per Trakt, NON inviamo mai l'history completo delle Serie TV
            // in modo massivo perché segnerebbe la serie come 100% vista su SIMKL. 
            // Gli episodi vengono tracciati uno per uno tramite SimklInstantWriteWorker.
            if (isMovie && watchedToPush.isNotEmpty()) {
                val historyItems = watchedToPush.map { 
                    SimklHistoryItem(ids = SimklIds(tmdb = it.id.toString())) 
                }
                historyItems.chunked(50).forEach { batch ->
                    try {
                        val request = SimklSyncHistoryRequest(movies = batch)
                        simklService.addToHistory(request)
                        delay(1000)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }

            if (watchlistToPush.isNotEmpty()) {
                val watchlistItems = watchlistToPush.map { 
                    SimklMediaItem(ids = SimklIds(tmdb = it.id.toString())) 
                }
                watchlistItems.chunked(50).forEach { batch ->
                    try {
                        val request = if (isMovie) SimklSyncWatchlistRequest(movies = batch) else SimklSyncWatchlistRequest(shows = batch)
                        simklService.addToWatchlist(request)
                        delay(1000)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }

            if (droppedToPush.isNotEmpty()) {
                val droppedItems = droppedToPush.map { 
                    SimklMediaItem(ids = SimklIds(tmdb = it.id.toString())) 
                }
                droppedItems.chunked(50).forEach { batch ->
                    try {
                        val request = if (isMovie) SimklAddToListRequest(to="dropped", movies = batch) else SimklAddToListRequest(to="dropped", shows = batch)
                        simklService.addToList(request)
                        delay(1000)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }

        pushItems(pendingMovies, isMovie = true)
        pushItems(pendingTv, isMovie = false)

        val syncedUpdates = pendingItems.map { 
            it.copy(syncStatus = "synced", clientUpdatedAt = System.currentTimeMillis()) 
        }
        movieRepository.saveMoviesBulk(syncedUpdates)
    }

    private suspend fun processPhaseItems(items: List<com.cinetrack.data.api.SimklSyncItemResponse>, isMovie: Boolean) {
        val total = items.size
        if (total == 0) return

        val missingTmdbIds = mutableSetOf<Long>()
        val updatesToSave = mutableListOf<Movie>()

        // 1. Check what we have locally
        for (item in items) {
            val mediaItem = if (isMovie) item.movie else (item.show ?: item.anime)
            val tmdbStr = mediaItem?.ids?.tmdb
            if (tmdbStr.isNullOrEmpty()) continue
            val tmdbId = tmdbStr.toLongOrNull() ?: continue
            
            val mediaType = if (isMovie) "movie" else "tv"
            val local = movieRepository.getMovieIncludingDeleted(tmdbId, mediaType)

            // Anti-resurrection guard: never re-insert or re-update items the user explicitly deleted
            if (local != null && local.syncStatus == "pending_delete") {
                android.util.Log.d("SimklSyncWorker", "SYNC: Item $tmdbId è pending_delete, skip resurrezione.")
                continue
            }

            if (local != null) {
                // Update local to match SIMKL state
                val isWatched = item.status == "completed"
                val isWatchlist = item.status == "plantowatch"
                val isDropped = item.status == "dropped"
                updatesToSave.add(
                    local.copy(
                        watched = isWatched || local.watched,
                        favorite = isWatchlist || local.favorite,
                        dropped = isDropped || local.dropped,
                        syncStatus = "synced"
                    )
                )
            } else {
                missingTmdbIds.add(tmdbId)
            }
        }

        // 2. Fetch missing from TMDB concurrently in chunks of 10
        if (missingTmdbIds.isNotEmpty()) {
            val chunks = missingTmdbIds.chunked(10)
            var fetchedCount = 0
            
            for (chunk in chunks) {
                supervisorScope {
                    val deferreds = chunk.map { tmdbId ->
                        async(Dispatchers.IO) {
                            try {
                                val mediaType = if (isMovie) "movie" else "tv"
                                val tmdbRes = if (isMovie) {
                                    tmdbService.getMovieBasicDetails(tmdbId)
                                } else {
                                    tmdbService.getTVBasicDetails(tmdbId)
                                }
                                
                                val itemData = items.find { 
                                    val matchItem = if (isMovie) it.movie else (it.show ?: it.anime)
                                    matchItem?.ids?.tmdb == tmdbId.toString() 
                                }
                                val isWatched = itemData?.status == "completed"
                                val isWatchlist = itemData?.status == "plantowatch"
                                val isDropped = itemData?.status == "dropped"
                                
                                val newMovie = MovieMapper.mapResponseToMovie(tmdbRes, mediaType).copy(
                                    watched = isWatched,
                                    favorite = isWatchlist,
                                    dropped = isDropped,
                                    syncStatus = "synced"
                                )
                                newMovie
                            } catch (e: Exception) {
                                android.util.Log.e("SimklSyncWorker", "Failed to fetch TMDB details for ID $tmdbId. Skipping.", e)
                                null
                            }
                        }
                    }
                    val results = deferreds.awaitAll().filterNotNull()
                    updatesToSave.addAll(results)
                    
                    fetchedCount += chunk.size
                    updateProgress("Downloading TMDB details...", fetchedCount, missingTmdbIds.size)
                    delay(50) // Tiny delay between batches to be safe with TMDB
                }
            }
        }

        // 3. Save all to DB
        if (updatesToSave.isNotEmpty()) {
            movieRepository.saveMoviesBulk(updatesToSave)
        }
    }
}
