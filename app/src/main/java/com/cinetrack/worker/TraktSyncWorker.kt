package com.cinetrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinetrack.data.api.TraktService
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.TraktAuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class TraktSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val traktService: TraktService,
    private val traktAuthRepository: TraktAuthRepository,
    private val movieRepository: MovieRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!traktAuthRepository.isLoggedIn.value) {
            // Non loggato, nulla da fare
            return@withContext Result.success()
        }

        try {
            // 1. Controlla le ultime attività
            val lastActivities = traktService.getLastActivities()
            val localLastActivities = traktAuthRepository.getLastActivitiesTime()
            val remoteLastActivities = lastActivities.all // Timestamp ISO più recente

            if (localLastActivities == remoteLastActivities) {
                // Nessuna novità su Trakt
                return@withContext Result.success()
            }

            // 2. Novità rilevate! Scarica la History (Visti) e aggiorna il DB locale.
            // Attenzione: in un'app di produzione con migliaia di film, si userebbe l'impaginazione
            // e una riconciliazione più sofisticata. Qui applichiamo un approccio base.
            val history = traktService.getHistory()

            for (item in history) {
                val tmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb ?: continue
                val mediaType = if (item.type == "movie") "movie" else "tv"

                // Controlla se abbiamo il film/serie in locale
                val localMovie = movieRepository.getMovie(tmdbId, mediaType)
                if (localMovie != null && !localMovie.watched) {
                    val updated = localMovie.copy(watched = true, watchedAt = item.watched_at)
                    // Usiamo insert direttamente nel DB (favoriteDao) o un apposito metodo nel Repository
                    // per evitare di scatenare un altro InstantWriteWorker a catena.
                    // Dato che la UI non ha ancora un metodo `syncSaveMovie` (che non accoda chiamate di rete),
                    // usiamo un approccio ottimistico chiamando l'API pubblica di MovieRepository.
                    // N.B: l'InstantWriteWorker controllerà (oldMovie.watched != movie.watched), 
                    // ma passando syncToTrakt = false evitiamo il loop di rimbalzo verso il server!
                    movieRepository.saveMovie(updated, syncToTrakt = false)
                }
            }

            // 3. Salva il nuovo timestamp
            traktAuthRepository.saveLastActivitiesTime(remoteLastActivities)

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("TraktSyncWorker", "Errore durante la sync 24h", e)
            Result.retry()
        }
    }
}
