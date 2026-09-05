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
    private val movieRepository: MovieRepository,
    private val firebaseRemoteDataSource: com.cinetrack.data.remote.FirebaseRemoteDataSource
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
            val isFullDownload = isFirstSync || force
            
            val bulkStatsUpdates = mutableListOf<com.cinetrack.data.remote.FirebaseRemoteDataSource.TrendingStatUpdate>()
            
            updateProgress("App ➔ SIMKL: Pushing local changes...")
            pushPendingChanges() // ONLY push pending_sync (offline edits)
            
            val activities = simklService.getActivities()
            val remoteLastSync = activities.all ?: ""
            val localLastSync = authRepository.getLastSyncTime() ?: ""

            if (!isFullDownload && remoteLastSync == localLastSync) {
                // Nothing changed on the server
                return@withContext Result.success()
            }

            if (isFullDownload) {
                updateProgress("SIMKL ➔ App: Downloading library...")
                val movies = simklService.getSyncMovies()
                val shows = simklService.getSyncShows()
                val anime = simklService.getSyncAnime()

                processPhaseItems(movies, isMovie = true, bulkStatsUpdates)
                processPhaseItems(shows, isMovie = false, bulkStatsUpdates)
                processPhaseItems(anime, isMovie = false, bulkStatsUpdates)

                val remoteTmdbIds = (movies + shows + anime).mapNotNull { 
                    it.movie?.ids?.tmdb ?: it.show?.ids?.tmdb ?: it.anime?.ids?.tmdb 
                }.mapNotNull { it.toLongOrNull() }.toSet()

                val allLocal = movieRepository.getLocalMovies()

                if (isFirstSync) {
                    // MERGE BIDIREZIONALE: Push su SIMKL solo di ciò che è locale ma manca sul server
                    val localMissingOnSimkl = allLocal.filter { 
                        (it.watched || it.favorite || it.dropped || it.watchedEpisodes?.isNotEmpty() == true) && 
                        !remoteTmdbIds.contains(it.id) 
                    }
                    if (localMissingOnSimkl.isNotEmpty()) {
                        android.util.Log.d("SimklSyncWorker", "SYNC MERGE: Primo sync -> Push di ${localMissingOnSimkl.size} elementi mancanti su SIMKL")
                        pushItemsToSimkl(localMissingOnSimkl.filter { it.mediaType == "movie" }, isMovie = true)
                        pushItemsToSimkl(localMissingOnSimkl.filter { it.mediaType == "tv" }, isMovie = false)
                    }
                } else {
                    // FORCE SYNC MANUALE: Cleanup orfani (Sync Distruttivo)
                    val orphans = allLocal.filter { 
                        (it.watched || it.favorite || it.dropped) && !remoteTmdbIds.contains(it.id) 
                    }
                    if (orphans.isNotEmpty()) {
                        android.util.Log.d("SimklSyncWorker", "SYNC: Removing ${orphans.size} orphaned items deleted from SIMKL")
                        val cleared = orphans.map { 
                            it.copy(watched = false, favorite = false, dropped = false, syncStatus = "synced") 
                        }
                        movieRepository.saveMoviesBulk(cleared)
                    }
                }

                if (isFirstSync) authRepository.setFirstSyncCompleted(true)
                authRepository.saveLastSyncTime(remoteLastSync)
            } else {
                updateProgress("SIMKL ➔ App: Downloading updates...")
                // We use the localLastSync to fetch only what changed since our last successful sync
                val delta = simklService.getSyncAllItems(localLastSync)
                processPhaseItems(delta.movies ?: emptyList(), isMovie = true, bulkStatsUpdates)
                processPhaseItems(delta.shows ?: emptyList(), isMovie = false, bulkStatsUpdates)
                processPhaseItems(delta.anime ?: emptyList(), isMovie = false, bulkStatsUpdates)
                
                authRepository.saveLastSyncTime(remoteLastSync)
            }

            if (bulkStatsUpdates.isNotEmpty()) {
                firebaseRemoteDataSource.updateTrendingStatsBulk(bulkStatsUpdates)
            }

            updateProgress("Sync complete!", 100, 100)
            return@withContext Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.retry()
        }
    }

    private suspend fun pushPendingChanges() {
        val allLocal = movieRepository.getLocalMovies()
        
        val pendingItems = allLocal.filter { it.syncStatus == "pending_sync" }
        if (pendingItems.isEmpty()) return

        val pendingMovies = pendingItems.filter { it.mediaType == "movie" }
        val pendingTv = pendingItems.filter { it.mediaType == "tv" }

        pushItemsToSimkl(pendingMovies, isMovie = true)
        pushItemsToSimkl(pendingTv, isMovie = false)

        val syncedUpdates = pendingItems.map { 
            it.copy(syncStatus = "synced", clientUpdatedAt = System.currentTimeMillis()) 
        }
        movieRepository.saveMoviesBulk(syncedUpdates)
    }

    private suspend fun pushItemsToSimkl(items: List<Movie>, isMovie: Boolean) {
        if (items.isEmpty()) return
        
        val watchedToPush = items.filter { it.watched }
        val watchlistToPush = items.filter { it.favorite && !it.watched }
        val droppedToPush = items.filter { it.dropped }

        val historyItemsToPush = if (isMovie) {
            watchedToPush.map { SimklHistoryItem(ids = SimklIds(tmdb = it.id.toString())) }
        } else {
            val tvHistory = mutableListOf<SimklHistoryItem>()
            items.forEach { show ->
                val seasonsList = mutableListOf<com.cinetrack.data.api.SimklSeason>()
                show.watchedEpisodes?.forEach { (seasonStr, epNums) ->
                    val seasonNum = seasonStr.toIntOrNull() ?: return@forEach
                    val episodesList = epNums.map { com.cinetrack.data.api.SimklSeasonEpisode(episode = it, number = it) }
                    seasonsList.add(com.cinetrack.data.api.SimklSeason(season = seasonNum, number = seasonNum, episodes = episodesList))
                }
                if (seasonsList.isNotEmpty()) {
                    tvHistory.add(SimklHistoryItem(
                        ids = SimklIds(tmdb = show.id.toString()),
                        seasons = seasonsList
                    ))
                } else if (show.watched) {
                    tvHistory.add(SimklHistoryItem(ids = SimklIds(tmdb = show.id.toString())))
                }
            }
            tvHistory
        }

        if (historyItemsToPush.isNotEmpty()) {
            historyItemsToPush.chunked(50).forEach { batch ->
                try {
                    val request = if (isMovie) SimklSyncHistoryRequest(movies = batch) else SimklSyncHistoryRequest(shows = batch)
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

    private suspend fun processPhaseItems(
        items: List<com.cinetrack.data.api.SimklSyncItemResponse>,
        isMovie: Boolean,
        bulkStatsUpdates: MutableList<com.cinetrack.data.remote.FirebaseRemoteDataSource.TrendingStatUpdate>
    ) {
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
            
            val isWatched = item.status == "completed" || item.status == "watching" || item.status == "watched" || !item.last_watched_at.isNullOrBlank()
            val isWatchlist = item.status == "plantowatch" || item.status == "plan_to_watch"
            val isDropped = item.status == "dropped"
            val newRating = item.user_rating?.toDouble()

            if (local != null) {
                // Update local to match SIMKL state
                val oldWatched = local.watched
                val oldRating = local.personalRating
                
                val updated = local.copy(
                    watched = isWatched,
                    favorite = isWatchlist,
                    dropped = isDropped,
                    personalRating = newRating ?: local.personalRating,
                    syncStatus = "synced"
                )
                updatesToSave.add(updated)
                
                if (isRecent(item.last_watched_at)) {
                    var viewsDelta = 0L
                    if (isWatched && !oldWatched) viewsDelta = 1L
                    
                    val ratingDiff = if (newRating != null) newRating - (oldRating ?: 0.0) else 0.0
                    val countDelta = if (newRating != null && oldRating == null) 1L else 0L
                    
                    if (viewsDelta != 0L || ratingDiff != 0.0 || countDelta != 0L) {
                        bulkStatsUpdates.add(
                            com.cinetrack.data.remote.FirebaseRemoteDataSource.TrendingStatUpdate(
                                compositeId = updated.compositeId,
                                viewsDelta = viewsDelta,
                                ratingDelta = ratingDiff,
                                ratingCountDelta = countDelta
                            )
                        )
                    }
                }
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
                                val isWatched = itemData?.status == "completed" || itemData?.status == "watching" || itemData?.status == "watched" || !itemData?.last_watched_at.isNullOrBlank()
                                val isWatchlist = itemData?.status == "plantowatch" || itemData?.status == "plan_to_watch"
                                val isDropped = itemData?.status == "dropped"
                                val newRating = itemData?.user_rating?.toDouble()
                                
                                val newMovie = MovieMapper.mapResponseToMovie(tmdbRes, mediaType).copy(
                                    watched = isWatched,
                                    favorite = isWatchlist,
                                    dropped = isDropped,
                                    personalRating = newRating,
                                    syncStatus = "synced"
                                )
                                
                                if (isRecent(itemData?.last_watched_at)) {
                                    var viewsDelta = 0L
                                    if (isWatched) viewsDelta = 1L
                                    
                                    val ratingDiff = newRating ?: 0.0
                                    val countDelta = if (newRating != null) 1L else 0L
                                    
                                    if (viewsDelta != 0L || ratingDiff != 0.0 || countDelta != 0L) {
                                        bulkStatsUpdates.add(
                                            com.cinetrack.data.remote.FirebaseRemoteDataSource.TrendingStatUpdate(
                                                compositeId = newMovie.compositeId,
                                                viewsDelta = viewsDelta,
                                                ratingDelta = ratingDiff,
                                                ratingCountDelta = countDelta
                                            )
                                        )
                                    }
                                }
                                
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

    private fun isRecent(dateString: String?): Boolean {
        if (dateString.isNullOrBlank()) return false
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            val date = format.parse(dateString)
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            date != null && date.time >= thirtyDaysAgo
        } catch (e: Exception) {
            false
        }
    }

    private fun isRecentTime(timestamp: Long?): Boolean {
        if (timestamp == null || timestamp == 0L) return false
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        return timestamp >= thirtyDaysAgo
    }
}
