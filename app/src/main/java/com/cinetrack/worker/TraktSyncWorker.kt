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
import com.cinetrack.data.api.TMDBService
import com.cinetrack.data.api.TraktService
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.TraktAuthRepository
import com.cinetrack.data.mapper.MovieMapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@HiltWorker
class TraktSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val traktService: TraktService,
    private val tmdbService: TMDBService,
    private val traktAuthRepository: TraktAuthRepository,
    private val movieRepository: MovieRepository
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "trakt_sync_channel"

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(com.cinetrack.R.string.trakt_sync_title))
            .setContentText(applicationContext.getString(com.cinetrack.R.string.trakt_sync_prep_desc))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Se Android 14+, serve aver dichiarato il type nel manifest
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(com.cinetrack.R.string.trakt_sync_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = applicationContext.getString(com.cinetrack.R.string.trakt_sync_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun updateProgress(current: Int, total: Int) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(com.cinetrack.R.string.trakt_sync_title))
            .setContentText(applicationContext.getString(com.cinetrack.R.string.trakt_sync_progress_desc, current, total))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setProgress(total, current, false)
            .setOngoing(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        setProgress(androidx.work.workDataOf("current" to current, "total" to total))
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!traktAuthRepository.isLoggedIn.value) {
            return@withContext Result.success()
        }

        try {
            // Promuovi a Foreground Service per bypassare il limite dei 10 minuti di Android
            setForeground(getForegroundInfo())

            val lastActivities = traktService.getLastActivities()
            val localLastActivities = traktAuthRepository.getLastActivitiesTime()
            val remoteLastActivities = lastActivities.all

            if (localLastActivities == remoteLastActivities) {
                return@withContext Result.success()
            }

            var page = 1
            var serverItemCount: Int? = null
            var totalPages = 1
            val historyAccumulator = mutableListOf<com.cinetrack.data.api.TraktHistoryItem>()
            var isIntegrityValid = false

            do {
                val response = traktService.getHistory(page = page, limit = 1000)
                if (response.isSuccessful) {
                    if (page == 1) {
                        serverItemCount = response.headers()["X-Pagination-Item-Count"]?.toIntOrNull()
                        totalPages = response.headers()["X-Pagination-Page-Count"]?.toIntOrNull() ?: 1
                    }
                    val body = response.body()
                    if (body != null) {
                        historyAccumulator.addAll(body)
                    }
                } else {
                    // API Error, abort full sync logic, just keep what we have
                    break
                }
                page++
            } while (page <= totalPages)

            if (serverItemCount != null && serverItemCount == historyAccumulator.size) {
                isIntegrityValid = true
            }

            val history = historyAccumulator.distinctBy { item ->
                val tmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb
                val mediaType = if (item.type == "movie") "movie" else "tv"
                Pair(tmdbId, mediaType)
            }
            
            val total = history.size
            var current = 0
            
            val moviesToUpdate = mutableListOf<com.cinetrack.data.Movie>()

            for (item in history) {
                current++
                // Aggiorna la UI ogni 5 item
                if (current % 5 == 0 || current == total) {
                    updateProgress(current, total)
                }

                val tmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb ?: continue
                val mediaType = if (item.type == "movie") "movie" else "tv"

                val localMovie = movieRepository.getMovie(tmdbId, mediaType)
                
                if (localMovie != null) {
                    if (!localMovie.watched) {
                        val updated = localMovie.copy(watched = true, watchedAt = item.watched_at)
                        moviesToUpdate.add(updated)
                    }
                    continue
                }

                try {
                    val tmdbResponse = if (mediaType == "movie") {
                        tmdbService.getMovieBasicDetails(tmdbId)
                    } else {
                        tmdbService.getTVBasicDetails(tmdbId)
                    }
                    
                    var newMovie = MovieMapper.mapResponseToMovie(tmdbResponse, mediaType)
                    newMovie = newMovie.copy(watched = true, watchedAt = item.watched_at)
                    moviesToUpdate.add(newMovie)

                    delay(50)
                } catch (e: Exception) {
                    android.util.Log.e("TraktSyncWorker", "Impossibile scaricare dettagli per TMDB ID $tmdbId", e)
                }
            }

            // --- Two Way Diff (Rimozioni) ---
            if (isIntegrityValid) {
                val remoteWatchedIds = history.mapNotNull { 
                    val tId = it.movie?.ids?.tmdb ?: it.show?.ids?.tmdb 
                    if (tId != null) Pair(tId, if (it.type == "movie") "movie" else "tv") else null
                }.toHashSet()

                val localMovies = movieRepository.getLocalMovies()
                for (localMovie in localMovies) {
                    if (localMovie.watched) {
                        val key = Pair(localMovie.id, localMovie.mediaType)
                        if (!remoteWatchedIds.contains(key)) {
                            // Orfano trovato: l'utente lo ha cancellato da Trakt
                            val updated = localMovie.copy(watched = false)
                            moviesToUpdate.add(updated)
                        }
                    }
                }
            } else {
                android.util.Log.w("TraktSyncWorker", "Integrità Trakt fallita: attesi $serverItemCount, ricevuti ${historyAccumulator.size}. Skip rimozioni.")
            }

            // Salvataggio Massivo in SQLite
            movieRepository.saveMoviesBulk(moviesToUpdate)

            traktAuthRepository.saveLastActivitiesTime(remoteLastActivities)

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("TraktSyncWorker", "Errore durante la sync", e)
            Result.retry()
        }
    }
}
