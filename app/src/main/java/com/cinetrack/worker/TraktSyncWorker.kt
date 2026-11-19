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
import com.cinetrack.util.getEpisodeWatchDate
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class TraktSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val traktService: TraktService,
    private val tmdbService: TMDBService,
    private val traktAuthRepository: TraktAuthRepository,
    private val movieRepository: MovieRepository,
    private val firebaseRemoteDataSource: com.cinetrack.data.remote.FirebaseRemoteDataSource
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "trakt_sync_channel"

    private val movieSyncDelegate by lazy {
        TraktMovieSyncDelegate(traktService, tmdbService, movieRepository)
    }
    private val showSyncDelegate by lazy {
        TraktShowSyncDelegate(traktService, tmdbService, movieRepository)
    }

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
        android.util.Log.e("TRAKT_DEBUG", "--- Inizio doWork ---")
        if (!traktAuthRepository.isLoggedIn.value) {
            android.util.Log.e("TRAKT_DEBUG", "Utente non loggato. Fine.")
            return@withContext Result.success()
        }

        try {
            val bulkStatsUpdates = mutableListOf<com.cinetrack.data.remote.FirebaseRemoteDataSource.TrendingStatUpdate>()
            // Promuovi a Foreground Service per bypassare il limite dei 10 minuti di Android
            setForeground(getForegroundInfo())

            val force = inputData.getBoolean("force", false)
            val isFirstSync = !traktAuthRepository.isFirstSyncCompleted()
            val lastActivities = traktService.getLastActivities()
            val localLastActivities = traktAuthRepository.getLastActivitiesTime()
            val remoteLastActivities = lastActivities.all

            if (!force && !isFirstSync && localLastActivities == remoteLastActivities) {
                return@withContext Result.success()
            }

            var page = 1
            var serverItemCount: Int? = null
            var totalPages = 1
            val historyAccumulator = mutableListOf<com.cinetrack.data.api.TraktHistoryItem>()
            var isIntegrityValid = false

            do {
                val response = traktService.getHistory(page = page, limit = 1000, extended = "full")
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
            
            // ── Fase 1a: Film visti (Movie History Sync & 2-way Diff) ────────────────
            movieSyncDelegate.syncWatchedMovies(
                history = history,
                isIntegrityValid = isIntegrityValid,
                isFirstSync = isFirstSync,
                bulkStatsUpdates = bulkStatsUpdates,
                onProgress = { current, total -> updateProgress(current, total) },
                isRecent = { isRecent(it) }
            )

            // ── Fase 1b: Episodi visti (TV Show Episodes Sync & 2-way Diff) ──────────
            showSyncDelegate.syncWatchedShows(
                historyAccumulator = historyAccumulator,
                isIntegrityValid = isIntegrityValid,
                isFirstSync = isFirstSync,
                bulkStatsUpdates = bulkStatsUpdates,
                isRecent = { isRecent(it) }
            )





            val remoteRatedAt = lastActivities.movies.rated_at
                ?: lastActivities.shows.rated_at
            val localRatedAt  = traktAuthRepository.getLastRatingsTime()
            if (isFirstSync || (remoteRatedAt != null && remoteRatedAt != localRatedAt)) {
                try {
                    val ratedMovies = traktService.getUserMovieRatings()
                    val ratedShows  = traktService.getUserShowRatings()
                    val ratingUpdates = mutableListOf<com.cinetrack.data.model.Movie>()

                    for (item in ratedMovies + ratedShows) {
                        val tmdbId    = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb ?: continue
                        val mediaType = if (item.type == "movie") "movie" else "tv"
                        val local = movieRepository.getMovieIncludingDeleted(tmdbId, mediaType)
                        if (local != null && (local.syncStatus == "pending_delete" || local.syncStatus == "deleted")) {
                            android.util.Log.d("TraktSyncWorker", "SYNC: Rating $tmdbId è ${local.syncStatus}, skip resurrezione.")
                            continue
                        }
                        
                        if (local != null) {
                            // Merge rule: remote wins; conserva locale se remote è assente
                            val newRating = item.rating.toDouble().takeIf { it > 0 } ?: local.personalRating
                            if (newRating != local.personalRating) {
                                val updated = local.copy(personalRating = newRating)
                                ratingUpdates.add(updated)
                                
                                if (isRecent(item.rated_at) && newRating != null) {
                                    val ratingDiff = newRating - (local.personalRating ?: 0.0)
                                    val countDelta = if (local.personalRating == null) 1L else 0L
                                    if (ratingDiff != 0.0 || countDelta != 0L) {
                                        bulkStatsUpdates.add(
                                            com.cinetrack.data.remote.FirebaseRemoteDataSource.TrendingStatUpdate(
                                                compositeId = updated.compositeId,
                                                ratingDelta = ratingDiff,
                                                ratingCountDelta = countDelta
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            // FIX: Se il film votato non esiste localmente, lo scarichiamo da TMDB
                            try {
                                val tmdbResponse = if (mediaType == "movie") {
                                    tmdbService.getMovieBasicDetails(tmdbId)
                                } else {
                                    tmdbService.getTVBasicDetails(tmdbId)
                                }
                                
                                var newMovie = com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(tmdbResponse, mediaType)
                                val newRating = item.rating.toDouble()
                                newMovie = newMovie.copy(personalRating = newRating)
                                ratingUpdates.add(newMovie)
                                
                                if (isRecent(item.rated_at) && newRating > 0) {
                                    bulkStatsUpdates.add(
                                        com.cinetrack.data.remote.FirebaseRemoteDataSource.TrendingStatUpdate(
                                            compositeId = newMovie.compositeId,
                                            ratingDelta = newRating,
                                            ratingCountDelta = 1L
                                        )
                                    )
                                }
                                
                                delay(50) // Evita il rate limiting
                            } catch (e: Exception) {
                                android.util.Log.e("TraktSyncWorker", "Impossibile scaricare dettagli rating per TMDB $tmdbId", e)
                            }
                        }
                    }

                    if (isFirstSync) {
                        val remoteRatedTmdbIds = (ratedMovies + ratedShows).mapNotNull {
                            it.movie?.ids?.tmdb ?: it.show?.ids?.tmdb
                        }.toHashSet()
                        val localRatingsToPush = movieRepository.getLocalMoviesIncludingDeleted().filter {
                            (it.personalRating ?: 0.0) > 0 && !remoteRatedTmdbIds.contains(it.id)
                        }
                        if (localRatingsToPush.isNotEmpty()) {
                            android.util.Log.e("TRAKT_DEBUG", "SYNC MERGE: Primo sync -> Push di ${localRatingsToPush.size} voti locali su Trakt")
                            for (item in localRatingsToPush) {
                                try {
                                    val ratingInt = (item.personalRating ?: 0.0).toInt().coerceIn(1, 10)
                                    val ids = com.cinetrack.data.api.TraktSyncIds(tmdb = item.id)
                                    val req = if (item.mediaType == "tv") {
                                        com.cinetrack.data.api.TraktRatingRequest(
                                            rating = ratingInt,
                                            shows = listOf(com.cinetrack.data.api.TraktRatingShow(ids))
                                        )
                                    } else {
                                        com.cinetrack.data.api.TraktRatingRequest(
                                            rating = ratingInt,
                                            movies = listOf(com.cinetrack.data.api.TraktRatingMovie(ids))
                                        )
                                    }
                                    traktService.addRating(req)
                                } catch (e: Exception) {
                                    android.util.Log.e("TraktSyncWorker", "Errore push voto su Trakt per ID ${item.id}", e)
                                }
                            }
                        }
                    }

                    if (ratingUpdates.isNotEmpty()) movieRepository.saveMoviesBulk(ratingUpdates)
                    remoteRatedAt?.let { traktAuthRepository.saveLastRatingsTime(it) }
                } catch (e: Exception) {
                    android.util.Log.w("TraktSyncWorker", "Ratings sync fallita, skip", e)
                }
            }

            // ── Fase 2.5: Sync Dropped (Hidden Items) ───────
            try {
                // Fetch sia film che serie nascosti
                val hiddenShowsRes = traktService.getHiddenProgressWatched(type = "show")
                val hiddenMoviesRes = traktService.getHiddenProgressWatched(type = "movie")
                
                val hiddenShows = if (hiddenShowsRes.isSuccessful) hiddenShowsRes.body() ?: emptyList() else emptyList()
                val hiddenMovies = if (hiddenMoviesRes.isSuccessful) hiddenMoviesRes.body() ?: emptyList() else emptyList()
                
                val hiddenTmdbIds = (hiddenShows.mapNotNull { it.show?.ids?.tmdb } + 
                                   hiddenMovies.mapNotNull { it.movie?.ids?.tmdb }).toSet()

                if (hiddenTmdbIds.isNotEmpty()) {
                    val allMovies = movieRepository.getLocalMoviesFlow().firstOrNull() ?: emptyList()
                    allMovies.forEach { movie ->
                        val isHiddenOnTrakt = movie.id in hiddenTmdbIds
                        if (isHiddenOnTrakt && !movie.dropped) {
                            movieRepository.saveMovie(movie.copy(dropped = true))
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("TraktSyncWorker", "Errore sync dropped (hidden)", e)
            }

            // ── Fase 3: Watchlist (paginata + two-way diff) ──────────────────
            val remoteMovieWatchlistedAt = lastActivities.movies.watchlisted_at.orEmpty()
            val remoteShowWatchlistedAt  = lastActivities.shows.watchlisted_at.orEmpty()
            val remoteWatchlistKey = if (remoteMovieWatchlistedAt.isNotEmpty() || remoteShowWatchlistedAt.isNotEmpty()) {
                "${remoteMovieWatchlistedAt}_${remoteShowWatchlistedAt}"
            } else null
            val localWatchlistKey = traktAuthRepository.getLastWatchlistTime()

            if (force || isFirstSync || (remoteWatchlistKey != null && remoteWatchlistKey != localWatchlistKey)) {
                try {
                    // Accumulare tutte le pagine prima del diff (evita falsi negative)
                    val allRemoteWatchlist = mutableListOf<com.cinetrack.data.api.TraktWatchlistItem>()
                    var wPage = 1
                    var wTotalPages = 1
                    do {
                        val wResponse = traktService.getWatchlist(page = wPage, limit = 1000)
                        if (wResponse.isSuccessful) {
                            wTotalPages = wResponse.headers()["X-Pagination-Page-Count"]?.toIntOrNull() ?: 1
                            wResponse.body()?.let { allRemoteWatchlist.addAll(it) }
                        } else break
                        wPage++
                    } while (wPage <= wTotalPages)

                    // Set di tutti gli ID remoti in watchlist risolti
                    val remoteWatchlistIds = mutableSetOf<Pair<Long, String>>()
                    val watchlistUpdates   = mutableListOf<com.cinetrack.data.model.Movie>()
                    val localMovies        = movieRepository.getLocalMoviesIncludingDeleted()
                    val toRemoveMoviesFromTraktWatchlist = mutableListOf<com.cinetrack.data.api.TraktSyncMovie>()
                    val toRemoveShowsFromTraktWatchlist  = mutableListOf<com.cinetrack.data.api.TraktSyncShow>()

                    // Guard cross-fase: film già visti localmente (o serie in pari) non devono
                    // essere resurrezionati dalla watchlist remota. Costruiamo il set dai dati locali
                    // già caricati, senza nessuna chiamata API aggiuntiva (zero costo).
                    val remoteWatchedMovieIds: Set<Pair<Long, String>> = localMovies
                        .filter { m -> m.watched || (m.mediaType == "tv" && m.isTvUpToDateOrCompleted()) }
                        .map { m -> Pair(m.id, m.mediaType) }
                        .toHashSet()

                    // favorite = true per tutto ciò che è in watchlist remota
                    for (item in allRemoteWatchlist) {
                        val mediaType = if (item.movie != null || item.type == "movie") "movie" else "tv"
                        var tmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb
                        val imdbId = item.movie?.ids?.imdb ?: item.show?.ids?.imdb

                        // Fallback: risoluzione via IMDB se ids.tmdb è assente
                        if (tmdbId == null && !imdbId.isNullOrBlank()) {
                            try {
                                val found = tmdbService.findByExternalId(imdbId, "imdb_id")
                                val resolved = if (mediaType == "movie") {
                                    found.movieResults?.firstOrNull()?.id
                                } else {
                                    found.tvResults?.firstOrNull()?.id
                                }
                                if (resolved != null && resolved > 100L) {
                                    tmdbId = resolved
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("TraktSyncWorker", "Errore lookup TMDB per IMDB ID $imdbId in watchlist", e)
                            }
                        }

                        // Fallback 2: risoluzione via TVDB se ancora assente (serie TV)
                        val tvdbId = item.show?.ids?.tvdb
                        if (tmdbId == null && tvdbId != null && tvdbId > 0L) {
                            try {
                                val found = tmdbService.findByExternalId(tvdbId.toString(), "tvdb_id")
                                val resolved = found.tvResults?.firstOrNull()?.id
                                if (resolved != null && resolved > 100L) {
                                    tmdbId = resolved
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("TraktSyncWorker", "Errore lookup TMDB per TVDB ID $tvdbId in watchlist", e)
                            }
                        }

                        if (tmdbId == null || tmdbId <= 0L) continue

                        val local = movieRepository.getMovieIncludingDeleted(tmdbId, mediaType)
                        if (local != null && (local.syncStatus == "pending_delete" || local.syncStatus == "deleted")) {
                            android.util.Log.d("TraktSyncWorker", "SYNC: Watchlist $tmdbId è ${local.syncStatus}, skip resurrezione.")
                            continue
                        }

                        // Guard: se il film/serie è già visto o completato (in locale o in Trakt history),
                        // non deve MAI essere impostato a watched = false né riapparire in Watchlist ("Da Vedere").
                        // Invece, accodiamo la rimozione dalla Watchlist di Trakt per ripulire lo stato remoto.
                        val isAlreadyWatched = (local != null && (local.watched || (local.mediaType == "tv" && local.isTvUpToDateOrCompleted()))) ||
                                remoteWatchedMovieIds.contains(Pair(tmdbId, mediaType))

                        if (isAlreadyWatched) {
                            android.util.Log.d("TraktSyncWorker", "SYNC: $mediaType $tmdbId è già visto/in pari, skip watchlist e pulisco Trakt.")
                            if (mediaType == "movie") {
                                toRemoveMoviesFromTraktWatchlist.add(
                                    com.cinetrack.data.api.TraktSyncMovie(
                                        ids = com.cinetrack.data.api.TraktSyncIds(tmdb = tmdbId, imdb = imdbId)
                                    )
                                )
                            } else {
                                toRemoveShowsFromTraktWatchlist.add(
                                    com.cinetrack.data.api.TraktSyncShow(
                                        ids = com.cinetrack.data.api.TraktSyncIds(tmdb = tmdbId, imdb = imdbId)
                                    )
                                )
                            }
                            continue
                        }

                        remoteWatchlistIds.add(Pair(tmdbId, mediaType))
                        
                        if (local != null) {
                            if (!local.favorite) {
                                watchlistUpdates.add(local.copy(favorite = true))
                            }
                        } else {
                            // Se il film/serie in watchlist non esiste nel DB locale, lo scarichiamo da TMDB
                            try {
                                val tmdbResponse = if (mediaType == "movie") {
                                    tmdbService.getMovieBasicDetails(tmdbId)
                                } else {
                                    tmdbService.getTVBasicDetails(tmdbId)
                                }
                                
                                var newMovie = com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(tmdbResponse, mediaType)
                                newMovie = newMovie.copy(favorite = true, watched = false)
                                watchlistUpdates.add(newMovie)
                                
                                delay(50)
                            } catch (e: Exception) {
                                android.util.Log.e("TraktSyncWorker", "Impossibile scaricare dettagli watchlist per TMDB ID $tmdbId", e)
                            }
                        }
                    }

                    // Rimozione atomica da Trakt degli elementi già visti che erano rimasti orfani nella watchlist remota
                    if (toRemoveMoviesFromTraktWatchlist.isNotEmpty() || toRemoveShowsFromTraktWatchlist.isNotEmpty()) {
                        try {
                            traktService.removeFromWatchlist(
                                com.cinetrack.data.api.TraktSyncRequest(
                                    movies = toRemoveMoviesFromTraktWatchlist.takeIf { it.isNotEmpty() },
                                    shows = toRemoveShowsFromTraktWatchlist.takeIf { it.isNotEmpty() }
                                )
                            )
                            android.util.Log.d("TraktSyncWorker", "SYNC: Rimossi ${toRemoveMoviesFromTraktWatchlist.size} film e ${toRemoveShowsFromTraktWatchlist.size} serie già visti dalla Watchlist Trakt.")
                        } catch (e: Exception) {
                            android.util.Log.e("TraktSyncWorker", "Errore pulizia elementi visti dalla watchlist Trakt", e)
                        }
                    }

                    // Two-way Merge/Diff per la watchlist
                    if (isFirstSync || remoteWatchlistIds.isEmpty()) {
                        // Primo sync o account vuoto: MERGE (push su Trakt di ciò che non è già presente, nessuna rimozione locale)
                        val favoritesToPush = localMovies.filter { 
                            it.favorite && !remoteWatchlistIds.contains(Pair(it.id, it.mediaType))
                        }
                        if (favoritesToPush.isNotEmpty()) {
                            android.util.Log.e("TRAKT_DEBUG", "SYNC MERGE: Primo sync -> Push di ${favoritesToPush.size} elementi in watchlist su Trakt")
                            try {
                                val moviesPush = favoritesToPush.filter { it.mediaType != "tv" }.map {
                                    com.cinetrack.data.api.TraktSyncMovie(ids = com.cinetrack.data.api.TraktSyncIds(tmdb = it.id))
                                }
                                val showsPush = favoritesToPush.filter { it.mediaType == "tv" }.map {
                                    com.cinetrack.data.api.TraktSyncShow(ids = com.cinetrack.data.api.TraktSyncIds(tmdb = it.id))
                                }
                                traktService.addToWatchlist(
                                    com.cinetrack.data.api.TraktSyncRequest(
                                        movies = moviesPush.takeIf { it.isNotEmpty() },
                                        shows = showsPush.takeIf { it.isNotEmpty() }
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("TraktSyncWorker", "Errore push watchlist verso Trakt", e)
                            }
                        }
                    } else {
                        // Two-way diff: favorite = false per ciò che non è più in watchlist
                        for (local in localMovies) {
                            if (local.favorite) {
                                val key = Pair(local.id, local.mediaType)
                                if (!remoteWatchlistIds.contains(key)) {
                                    val isShowInProgress = local.mediaType == "tv" && !local.watchedEpisodes.isNullOrEmpty()
                                    if (!isShowInProgress) {
                                        watchlistUpdates.add(local.copy(favorite = false))
                                    }
                                }
                            }
                        }
                    }

                    if (watchlistUpdates.isNotEmpty()) movieRepository.saveMoviesBulk(watchlistUpdates)
                    remoteWatchlistKey?.let { traktAuthRepository.saveLastWatchlistTime(it) }
                } catch (e: Exception) {
                    android.util.Log.w("TraktSyncWorker", "Watchlist sync fallita, skip", e)
                }
            }

            // ── Fase 4: Notes (solo utenti VIP, 403 = skip silenzioso) ───────
            try {
                val noteUpdates = mutableListOf<com.cinetrack.data.model.Movie>()

                val movieNotesResponse = traktService.getUserMovieNotes()
                val showNotesResponse  = traktService.getUserShowNotes()

                val allNotes = buildList {
                    if (movieNotesResponse.isSuccessful) movieNotesResponse.body()?.let { addAll(it) }
                    if (showNotesResponse.isSuccessful)  showNotesResponse.body()?.let  { addAll(it) }
                }

                for (item in allNotes) {
                    val tmdbId    = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb ?: continue
                    val mediaType = if ((item.attached_to?.type ?: item.movie?.let { "movie" } ?: "show") == "movie") "movie" else "tv"
                    val local     = movieRepository.getMovie(tmdbId, mediaType)
                    
                    if (local != null) {
                        // Merge rule: aggiorna nota solo se locale è vuota
                        if (local.personalNote.isNullOrBlank() && !item.notes.isNullOrBlank()) {
                            noteUpdates.add(local.copy(personalNote = item.notes.take(5000)))
                        }
                    } else {
                        // FIX: Se il film con la nota non esiste localmente, lo scarichiamo da TMDB
                        if (!item.notes.isNullOrBlank()) {
                            try {
                                val tmdbResponse = if (mediaType == "movie") {
                                    tmdbService.getMovieBasicDetails(tmdbId)
                                } else {
                                    tmdbService.getTVBasicDetails(tmdbId)
                                }
                                
                                var newMovie = com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(tmdbResponse, mediaType)
                                newMovie = newMovie.copy(personalNote = item.notes.take(5000))
                                noteUpdates.add(newMovie)
                                
                                delay(50)
                            } catch (e: Exception) {
                                android.util.Log.e("TraktSyncWorker", "Impossibile scaricare dettagli nota per TMDB $tmdbId", e)
                            }
                        }
                    }
                }

                if (noteUpdates.isNotEmpty()) movieRepository.saveMoviesBulk(noteUpdates)
            } catch (e: Exception) {
                android.util.Log.w("TraktSyncWorker", "Notes sync fallita (probabile non-VIP), skip", e)
            }

            // ── Fase 5: Trakt Custom Lists (Folders) ───────
            try {
                val listsResponse = traktService.getUserLists()
                if (listsResponse.isSuccessful) {
                    val traktLists = listsResponse.body() ?: emptyList()

                    for (tList in traktLists) {
                        val traktId = tList.ids?.trakt ?: continue
                        // Usiamo un prefisso "trakt_" per evitare che l'ID collida con cartelle create localmente
                        val folderId = "trakt_$traktId"

                        val itemsResponse = traktService.getListItems(traktId)
                        val compositeIds = mutableListOf<String>()
                        val missingMovies = mutableListOf<com.cinetrack.data.model.Movie>()

                        if (itemsResponse.isSuccessful) {
                            itemsResponse.body()?.forEach { item ->
                                val itemTmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb
                                val itemType = if (item.type == "movie") "movie" else "tv"
                                
                                if (itemTmdbId != null) {
                                    // Formato composito usato da FolderEntity ("movie_12345" o "tv_67890")
                                    compositeIds.add("${itemType}_${itemTmdbId}")

                                    // FIX "Watchlist": Se il film/serie non esiste in locale, scarichiamolo da TMDB!
                                    if (movieRepository.getMovie(itemTmdbId, itemType) == null) {
                                        try {
                                            val tmdbResponse = if (itemType == "movie") {
                                                tmdbService.getMovieBasicDetails(itemTmdbId)
                                            } else {
                                                tmdbService.getTVBasicDetails(itemTmdbId)
                                            }
                                            val newItem = com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(tmdbResponse, itemType)
                                            missingMovies.add(newItem)
                                            delay(50) // Salviamo le API da rate-limiting
                                        } catch (e: Exception) {
                                            android.util.Log.e("TraktSyncWorker", "Impossibile scaricare item $itemTmdbId per la lista", e)
                                        }
                                    }
                                }
                            }
                        }

                        // Salviamo preventivamente tutti i titoli scaricati da zero nel database
                        if (missingMovies.isNotEmpty()) {
                            movieRepository.saveMoviesBulk(missingMovies)
                        }

                        // Definiamo una palette di colori vivaci (Material UI)
                        val palette = listOf(
                            "#FF5252", "#E040FB", "#7C4DFF", "#448AFF", 
                            "#00BCD4", "#4CAF50", "#FFC107", "#FF9800", 
                            "#FF5722", "#00E676", "#D50000", "#F50057"
                        )

                        // Creiamo o aggiorniamo la Folder
                        // Usiamo .firstOrNull() per leggere il Flow e preservare colori/icone se l'utente li aveva personalizzati
                        val localFolder = movieRepository.getFolderFlow(folderId).firstOrNull()                        
                        val newFolder = com.cinetrack.data.local.entities.FolderEntity(
                            id = folderId,
                            name = tList.name ?: "Lista Trakt",
                            icon = localFolder?.icon ?: "folder_special",
                            // FIX: Estrae a sorte un colore dalla palette se è una nuova cartella
                            color = localFolder?.color ?: palette.random(),
                            description = tList.description ?: "",
                            itemIds = compositeIds,
                            createdAt = localFolder?.createdAt ?: System.currentTimeMillis().toString(),
                            updatedAt = System.currentTimeMillis().toString(),
                            syncStatus = "synced",
                            clientUpdatedAt = System.currentTimeMillis()
                        )
                        
                        movieRepository.saveFolder(newFolder)
                    }

                    // --- PULIZIA CARTELLE ZOMBIE ---
                    // Raccogliamo tutti gli ID delle liste che esistono ATTUALMENTE su Trakt
                    val remoteTraktFolderIds = traktLists.mapNotNull { it.ids?.trakt }.map { "trakt_$it" }
                    
                    // Prendiamo tutte le cartelle dal database locale
                    val allLocalFolders = movieRepository.getFoldersFlow().firstOrNull() ?: emptyList()                    
                    // Se una cartella locale inizia con "trakt_" ma non esiste più sul server, la disintegrimo
                    for (localFolder in allLocalFolders) {
                        if (localFolder.id.startsWith("trakt_") && !remoteTraktFolderIds.contains(localFolder.id)) {
                            movieRepository.deleteFolder(localFolder.id)
                            android.util.Log.d("TraktSyncWorker", "Cartella Zombie eliminata: ${localFolder.name}")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("TraktSyncWorker", "Sync Custom Lists (Folders) fallita, skip", e)
            }

            // 🚀 Fase 6: Hidden from Progress (Dropped) 🚀
            try {
                val hiddenResponse = traktService.getHiddenProgressWatched(type = "show", limit = 1000)
                if (hiddenResponse.isSuccessful) {
                    val hiddenItems = hiddenResponse.body() ?: emptyList()
                    val remoteHiddenIds = hiddenItems.mapNotNull { it.show?.ids?.tmdb }

                    val droppedUpdates = mutableListOf<com.cinetrack.data.model.Movie>()
                    val localMovies = movieRepository.getLocalMoviesIncludingDeleted()

                    if (isFirstSync || remoteHiddenIds.isEmpty()) {
                        // Push locale dropped verso Trakt
                        val droppedToPush = localMovies.filter { it.mediaType == "tv" && it.dropped && !remoteHiddenIds.contains(it.id) }
                        if (droppedToPush.isNotEmpty()) {
                            android.util.Log.e("TRAKT_DEBUG", "SYNC MERGE: Push di ${droppedToPush.size} dropped shows su Trakt")
                            try {
                                val showsPush = droppedToPush.map {
                                    com.cinetrack.data.api.TraktSyncShow(ids = com.cinetrack.data.api.TraktSyncIds(tmdb = it.id))
                                }
                                traktService.addHiddenProgressWatched(
                                    com.cinetrack.data.api.TraktSyncRequest(shows = showsPush)
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("TraktSyncWorker", "Errore push dropped", e)
                            }
                        }
                    } else {
                        // Two-way diff
                        for (hiddenId in remoteHiddenIds) {
                            val local = localMovies.find { it.id == hiddenId && it.mediaType == "tv" }
                            if (local != null && !local.dropped) {
                                droppedUpdates.add(local.copy(dropped = true))
                            }
                        }

                        val toRemoveFromTrakt = mutableListOf<com.cinetrack.data.api.TraktSyncShow>()
                        val toPushToTrakt = mutableListOf<com.cinetrack.data.api.TraktSyncShow>()
                        
                        for (local in localMovies.filter { it.mediaType == "tv" }) {
                            val isTraktHidden = remoteHiddenIds.contains(local.id)
                            if (local.dropped && !isTraktHidden) {
                                toPushToTrakt.add(com.cinetrack.data.api.TraktSyncShow(ids = com.cinetrack.data.api.TraktSyncIds(tmdb = local.id)))
                            } else if (!local.dropped && isTraktHidden) {
                                toRemoveFromTrakt.add(com.cinetrack.data.api.TraktSyncShow(ids = com.cinetrack.data.api.TraktSyncIds(tmdb = local.id)))
                            }
                        }
                        
                        if (toPushToTrakt.isNotEmpty()) {
                            traktService.addHiddenProgressWatched(com.cinetrack.data.api.TraktSyncRequest(shows = toPushToTrakt))
                        }
                        if (toRemoveFromTrakt.isNotEmpty()) {
                            traktService.removeHiddenProgressWatched(com.cinetrack.data.api.TraktSyncRequest(shows = toRemoveFromTrakt))
                        }
                    }

                    if (droppedUpdates.isNotEmpty()) movieRepository.saveMoviesBulk(droppedUpdates)
                }
            } catch (e: Exception) {
                android.util.Log.w("TraktSyncWorker", "Sync Dropped (Hidden) fallita, skip", e)
            }

            traktAuthRepository.saveLastActivitiesTime(remoteLastActivities)

            if (isFirstSync) {
                traktAuthRepository.markFirstSyncCompleted()
                android.util.Log.e("TRAKT_DEBUG", "SYNC MERGE: Primo sync completato e contrassegnato con successo.")
            }
            
            if (bulkStatsUpdates.isNotEmpty()) {
                firebaseRemoteDataSource.updateTrendingStatsBulk(bulkStatsUpdates)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("TraktSyncWorker", "Errore durante la sync", e)
            Result.retry()
        }
    }

    private fun isRecent(dateString: String?): Boolean {
        if (dateString.isNullOrBlank()) return false
        val millis = com.cinetrack.util.DateTimeUtils.parseIsoDateToMillis(dateString)
        if (millis == 0L) return false
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        return millis >= thirtyDaysAgo
    }

    private fun isRecentTime(timestamp: Long?): Boolean {
        if (timestamp == null || timestamp == 0L) return false
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        return timestamp >= thirtyDaysAgo
    }
}
