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
import kotlinx.coroutines.flow.firstOrNull

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
        android.util.Log.e("TRAKT_DEBUG", "--- Inizio doWork ---")
        if (!traktAuthRepository.isLoggedIn.value) {
            android.util.Log.e("TRAKT_DEBUG", "Utente non loggato. Fine.")
            return@withContext Result.success()
        }

        try {
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
            
            val total = history.size
            var current = 0
            
            val moviesToUpdate = mutableListOf<com.cinetrack.data.model.Movie>()

            for (item in history) {
                current++
                // Aggiorna la UI ogni 5 item
                if (current % 5 == 0 || current == total) {
                    updateProgress(current, total)
                }

                val tmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb ?: continue
                val mediaType = if (item.type == "movie") "movie" else "tv"

                // ---> FIX: Saltiamo le Serie TV in questa fase per non forzare il 100%!
                // Le analizzeremo nella Fase 1b contando matematicamente gli episodi.
                if (mediaType == "tv") continue

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

            // --- Two Way Merge/Diff (Rimozioni o Caricamento Iniziale) ---
            if (isIntegrityValid) {
                val remoteWatchedIds = history.mapNotNull { 
                    val tId = it.movie?.ids?.tmdb ?: it.show?.ids?.tmdb 
                    if (tId != null) Pair(tId, if (it.type == "movie") "movie" else "tv") else null
                }.toHashSet()

                val localMovies = movieRepository.getLocalMovies()
                val remoteMovieIds = remoteWatchedIds.filter { it.second != "tv" }

                if (isFirstSync || remoteMovieIds.isEmpty()) {
                    // Primo collegamento o account vuoto: MERGE (push su Trakt di ciò che manca remota, nessuna cancellazione locale)
                    val localMoviesToPush = localMovies.filter { 
                        it.watched && it.mediaType != "tv" && !remoteWatchedIds.contains(Pair(it.id, "movie"))
                    }
                    if (localMoviesToPush.isNotEmpty()) {
                        android.util.Log.e("TRAKT_DEBUG", "SYNC MERGE: Primo sync/Account vuoto -> Push di ${localMoviesToPush.size} film visti su Trakt")
                        try {
                            val syncMovies = localMoviesToPush.map {
                                com.cinetrack.data.api.TraktSyncMovie(ids = com.cinetrack.data.api.TraktSyncIds(tmdb = it.id))
                            }
                            traktService.addToHistory(com.cinetrack.data.api.TraktSyncRequest(movies = syncMovies))
                        } catch (e: Exception) {
                            android.util.Log.e("TraktSyncWorker", "Errore push film visti verso Trakt", e)
                        }
                    }
                } else {
                    for (localMovie in localMovies) {
                        if (localMovie.watched && localMovie.mediaType != "tv") {
                            val key = Pair(localMovie.id, localMovie.mediaType)
                            if (!remoteWatchedIds.contains(key)) {
                                // Orfano trovato: l'utente lo ha cancellato da Trakt
                                val updated = localMovie.copy(watched = false)
                                moviesToUpdate.add(updated)
                            }
                        }
                    }
                }
            } else {
                android.util.Log.w("TraktSyncWorker", "Integrità Trakt fallita: attesi $serverItemCount, ricevuti ${historyAccumulator.size}. Skip rimozioni.")
            }

            // Salvataggio Massivo in SQLite
            movieRepository.saveMoviesBulk(moviesToUpdate)

// ── Fase 1b: Episodi visti (Sincronizzazione Assoluta) ────────────────
            try {
                android.util.Log.e("TRAKT_DEBUG", "Inizio Fase 1b: Chiamata getWatchedShowsRaw...")

                // Mappa di fallback dagli episodi già recuperati nella cronologia (historyAccumulator)
                // Nel caso in cui Trakt API getWatchedShows non restituisca il campo seasons
                val historyEpisodesByShowTmdb = mutableMapOf<Long, MutableMap<String, MutableSet<Int>>>()
                val historyShowTitles = mutableMapOf<Long, String>()
                val historyShowImdbs = mutableMapOf<String, MutableMap<String, MutableSet<Int>>>()

                for (item in historyAccumulator) {
                    if (item.type == "episode" && item.episode != null && item.show != null) {
                        val seasonNum = item.episode.season
                        val epNum = item.episode.number
                        if (seasonNum > 0 && epNum > 0) {
                            val tmdbId = item.show.ids?.tmdb
                            val imdbId = item.show.ids?.imdb
                            if (tmdbId != null) {
                                val seasonsMap = historyEpisodesByShowTmdb.getOrPut(tmdbId) { mutableMapOf() }
                                val epsSet = seasonsMap.getOrPut(seasonNum.toString()) { mutableSetOf() }
                                epsSet.add(epNum)
                                item.show.title?.let { historyShowTitles[tmdbId] = it }
                            } else if (!imdbId.isNullOrBlank()) {
                                val seasonsMap = historyShowImdbs.getOrPut(imdbId) { mutableMapOf() }
                                val epsSet = seasonsMap.getOrPut(seasonNum.toString()) { mutableSetOf() }
                                epsSet.add(epNum)
                            }
                        }
                    }
                }

                val watchedShows = mutableListOf<com.cinetrack.data.api.TraktWatchedShow>()
                val jsonParser = kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                }

                val rawResponse = traktService.getWatchedShowsRaw()
                if (rawResponse.isSuccessful) {
                    val rawJson = rawResponse.body()?.string() ?: "[]"
                    android.util.Log.e("TRAKT_DEBUG", "SYNC RAW JSON Sample: ${rawJson.take(600)}")
                    val pageShows = jsonParser.decodeFromString<List<com.cinetrack.data.api.TraktWatchedShow>>(rawJson)
                    watchedShows.addAll(pageShows)
                } else {
                    android.util.Log.e("TraktSyncWorker", "SYNC: Errore API getWatchedShowsRaw (${rawResponse.code()})")
                }

                android.util.Log.e("TRAKT_DEBUG", "SYNC: Ricevuti ${watchedShows.size} show da Trakt.")

                val episodeUpdates = mutableListOf<com.cinetrack.data.model.Movie>()
                val processedShowTmdbIds = mutableSetOf<Long>()

                suspend fun processShowEpisodes(
                    showTmdbId: Long,
                    showTitle: String,
                    seasonsMap: Map<String, List<Int>>
                ) {
                    val totalWatched = seasonsMap.values.sumOf { it.size }
                    val local = movieRepository.getMovie(showTmdbId, "tv")

                    if (local == null) {
                        android.util.Log.d("TraktSyncWorker", "SYNC: Show '$showTitle' non in locale, scarico...")
                        try {
                            val tmdbResponse = tmdbService.getTVBasicDetails(showTmdbId)
                            var newShow = com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(tmdbResponse, "tv")

                            val totalEps = newShow.effectiveTotalEpisodes
                            val isCompleted = totalEps > 0 && totalWatched >= totalEps
                            val progressVal = if (totalEps > 0) (totalWatched.toDouble() / totalEps.toDouble()).coerceIn(0.0, 1.0) else 0.0

                            newShow = newShow.copy(
                                watched = isCompleted,
                                favorite = if (!isCompleted && totalWatched > 0) true else newShow.favorite,
                                watchedEpisodes = seasonsMap,
                                progress = progressVal,
                                syncStatus = "synced",
                                clientUpdatedAt = System.currentTimeMillis()
                            )
                            android.util.Log.e("TRAKT_DEBUG", "SYNC: Nuovo show scaricato '$showTitle' (Visti: $totalWatched/$totalEps, favorite: ${newShow.favorite}, watched: ${newShow.watched}, progress: $progressVal)")
                            episodeUpdates.add(newShow)
                        } catch (e: Exception) {
                            android.util.Log.e("TraktSyncWorker", "Errore download TMDB per $showTitle", e)
                        }
                    } else {
                        var totalEps = local.effectiveTotalEpisodes
                        var updatedNumberOfEpisodes = local.numberOfEpisodes
                        if (totalEps == 0) {
                            try {
                                val tmdbResponse = tmdbService.getTVBasicDetails(showTmdbId)
                                val mapped = com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(tmdbResponse, "tv")
                                totalEps = mapped.effectiveTotalEpisodes
                                updatedNumberOfEpisodes = if ((mapped.numberOfEpisodes ?: 0) > 0) mapped.numberOfEpisodes else local.numberOfEpisodes
                            } catch (e: Exception) {
                                android.util.Log.w("TraktSyncWorker", "Impossibile aggiornare totalEps per $showTitle", e)
                            }
                        }

                        val isCompleted = totalEps > 0 && totalWatched >= totalEps
                        val shouldBeFavorite = if (!isCompleted && totalWatched > 0) true else if (isCompleted) false else local.favorite
                        val progressVal = if (totalEps > 0) (totalWatched.toDouble() / totalEps.toDouble()).coerceIn(0.0, 1.0) else 0.0

                        if (seasonsMap != local.watchedEpisodes || local.watched != isCompleted || local.favorite != shouldBeFavorite || updatedNumberOfEpisodes != local.numberOfEpisodes || local.progress != progressVal) {
                            android.util.Log.e("TRAKT_DEBUG", "SYNC: Aggiorno stato per '$showTitle' (Visti: $totalWatched/$totalEps, favorite: $shouldBeFavorite, watched: $isCompleted, progress: $progressVal)")
                            episodeUpdates.add(
                                local.copy(
                                    watched = isCompleted,
                                    favorite = shouldBeFavorite,
                                    numberOfEpisodes = updatedNumberOfEpisodes,
                                    watchedEpisodes = seasonsMap,
                                    progress = progressVal,
                                    syncStatus = "synced",
                                    clientUpdatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }

                for (remoteShow in watchedShows) {
                    var showTmdbId = remoteShow.show?.ids?.tmdb
                    val imdbId = remoteShow.show?.ids?.imdb
                    val showTitle = remoteShow.show?.title ?: "Unknown"

                    if (showTmdbId == null && !imdbId.isNullOrBlank()) {
                        try {
                            val found = tmdbService.findByExternalId(imdbId, "imdb_id")
                            showTmdbId = found.tvResults?.firstOrNull()?.id
                        } catch (e: Exception) {
                            android.util.Log.e("TRAKT_DEBUG", "Errore lookup TMDB per IMDB ID $imdbId", e)
                        }
                    }
                    if (showTmdbId == null) continue

                    processedShowTmdbIds.add(showTmdbId)

                    // Unione degli episodi da remoteShow.seasons + historyAccumulator
                    val mergedEps = mutableMapOf<String, MutableSet<Int>>()
                    for (season in remoteShow.seasons ?: emptyList()) {
                        if (season.number == 0) continue
                        val eps = season.episodes?.map { it.number } ?: emptyList()
                        if (eps.isNotEmpty()) {
                            mergedEps.getOrPut(season.number.toString()) { mutableSetOf() }.addAll(eps)
                        }
                    }

                    historyEpisodesByShowTmdb[showTmdbId]?.forEach { (seasonStr, epsSet) ->
                        mergedEps.getOrPut(seasonStr) { mutableSetOf() }.addAll(epsSet)
                    }
                    if (!imdbId.isNullOrBlank()) {
                        historyShowImdbs[imdbId]?.forEach { (seasonStr, epsSet) ->
                            mergedEps.getOrPut(seasonStr) { mutableSetOf() }.addAll(epsSet)
                        }
                    }

                    val finalEps = mergedEps.mapValues { (_, eps) -> eps.sorted() }
                    val totalMergedCount = finalEps.values.sumOf { it.size }

                    if (showTitle.contains("Silo", ignoreCase = true) || showTitle.contains("Dragon", ignoreCase = true) || showTitle.contains("Mirror", ignoreCase = true)) {
                        android.util.Log.e("TRAKT_DEBUG", "SYNC SHOW DEBUG '$showTitle': tmdbId=$showTmdbId, seasons=${remoteShow.seasons?.size}, mergedEps=$totalMergedCount")
                    }

                    processShowEpisodes(showTmdbId, showTitle, finalEps)
                }

                // Processa anche eventuali serie TV presenti in historyAccumulator non restituite da getWatchedShows
                for ((showTmdbId, seasonsMap) in historyEpisodesByShowTmdb) {
                    if (processedShowTmdbIds.contains(showTmdbId)) continue
                    processedShowTmdbIds.add(showTmdbId)
                    val showTitle = historyShowTitles[showTmdbId] ?: "Unknown"
                    val finalEps = seasonsMap.mapValues { (_, eps) -> eps.sorted() }
                    processShowEpisodes(showTmdbId, showTitle, finalEps)
                }

                // Logica di Merge o Rimozione (Diff) protetta
                if (isIntegrityValid) {
                    val localTvShows = movieRepository.getLocalMovies().filter { it.mediaType == "tv" }
                    if (isFirstSync || processedShowTmdbIds.isEmpty()) {
                        // Primo collegamento o account vuoto: MERGE caricando gli episodi visti locali su Trakt, nessuna rimozione locale
                        val showsToPush = mutableListOf<com.cinetrack.data.api.TraktShowWithEpisodes>()
                        for (local in localTvShows) {
                            if (processedShowTmdbIds.contains(local.id)) continue
                            val seasonsList = local.watchedEpisodes?.mapNotNull { (seasonStr, epList) ->
                                val seasonNum = seasonStr.toIntOrNull() ?: return@mapNotNull null
                                if (seasonNum == 0 || epList.isEmpty()) null
                                else com.cinetrack.data.api.TraktSeasonEpisodes(
                                    number = seasonNum,
                                    episodes = epList.map { com.cinetrack.data.api.TraktEpisodeNumber(it) }
                                )
                            } ?: emptyList()
                            if (seasonsList.isNotEmpty()) {
                                showsToPush.add(
                                    com.cinetrack.data.api.TraktShowWithEpisodes(
                                        ids = com.cinetrack.data.api.TraktSyncIds(tmdb = local.id),
                                        seasons = seasonsList
                                    )
                                )
                            }
                        }
                        if (showsToPush.isNotEmpty()) {
                            android.util.Log.e("TRAKT_DEBUG", "SYNC MERGE: Primo sync/Account vuoto -> Push di ${showsToPush.size} serie TV viste su Trakt")
                            try {
                                traktService.addEpisodesToHistory(com.cinetrack.data.api.TraktEpisodeHistoryRequest(shows = showsToPush))
                            } catch (e: Exception) {
                                android.util.Log.e("TraktSyncWorker", "Errore push episodi visti verso Trakt", e)
                            }
                        }
                    } else {
                        for (local in localTvShows) {
                            if ((!local.watchedEpisodes.isNullOrEmpty() || local.watched) && !processedShowTmdbIds.contains(local.id)) {
                                episodeUpdates.add(
                                    local.copy(
                                        watched = false,
                                        watchedEpisodes = emptyMap(),
                                        syncStatus = "synced",
                                        clientUpdatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }

                if (episodeUpdates.isNotEmpty()) {
                    movieRepository.saveMoviesBulk(episodeUpdates)
                    android.util.Log.e("TRAKT_DEBUG", "SYNC: Salvati ${episodeUpdates.size} show aggiornati.")
                }
            } catch (e: Exception) {
                android.util.Log.e("TraktSyncWorker", "Errore critico Fase 1b", e)
            }





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
                        val local     = movieRepository.getMovie(tmdbId, mediaType)
                        
                        if (local != null) {
                            // Merge rule: remote wins; conserva locale se remote è assente
                            val newRating = item.rating.toDouble().takeIf { it > 0 } ?: local.personalRating
                            if (newRating != local.personalRating) {
                                ratingUpdates.add(local.copy(personalRating = newRating))
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
                                newMovie = newMovie.copy(personalRating = item.rating.toDouble())
                                ratingUpdates.add(newMovie)
                                
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
                        val localRatingsToPush = movieRepository.getLocalMovies().filter {
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

            // ── Fase 3: Watchlist (paginata + two-way diff) ──────────────────
            val remoteWatchlistedAt = lastActivities.movies.watchlisted_at
                ?: lastActivities.shows.watchlisted_at
            val localWatchlistedAt  = traktAuthRepository.getLastWatchlistTime()
            if (isFirstSync || (remoteWatchlistedAt != null && remoteWatchlistedAt != localWatchlistedAt)) {
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

                    // Set di tutti gli ID remoti in watchlist
                    val remoteWatchlistIds = allRemoteWatchlist.mapNotNull { item ->
                        val tmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb
                        val mt     = if (item.type == "movie") "movie" else "tv"
                        if (tmdbId != null) Pair(tmdbId, mt) else null
                    }.toHashSet()

                    val watchlistUpdates = mutableListOf<com.cinetrack.data.model.Movie>()
                    val localMovies      = movieRepository.getLocalMovies()

                    // favorite = true per tutto ciò che è in watchlist remota
                    for (item in allRemoteWatchlist) {
                        val tmdbId    = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb ?: continue
                        val mediaType = if (item.type == "movie") "movie" else "tv"
                        val local     = movieRepository.getMovie(tmdbId, mediaType)
                        
                        if (local != null) {
                            if (!local.favorite) {
                                watchlistUpdates.add(local.copy(favorite = true))
                            }
                        } else {
                            // FIX: Se il film in watchlist non esiste nel DB locale, lo scarichiamo da TMDB!
                            try {
                                val tmdbResponse = if (mediaType == "movie") {
                                    tmdbService.getMovieBasicDetails(tmdbId)
                                } else {
                                    tmdbService.getTVBasicDetails(tmdbId)
                                }
                                
                                var newMovie = com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(tmdbResponse, mediaType)
                                newMovie = newMovie.copy(favorite = true)
                                watchlistUpdates.add(newMovie)
                                
                                delay(50)
                            } catch (e: Exception) {
                                android.util.Log.e("TraktSyncWorker", "Impossibile scaricare dettagli watchlist per TMDB ID $tmdbId", e)
                            }
                        }
                    }

                    // Two-way Merge/Diff per la watchlist
                    if (isFirstSync || remoteWatchlistIds.isEmpty()) {
                        // Primo sync o account vuoto: MERGE (push su Trakt di ciò che non è già presente, nessuna rimozione locale)
                        val favoritesToPush = localMovies.filter { 
                            it.favorite && !remoteWatchlistIds.contains(Pair(it.id, it.mediaType ?: "movie"))
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
                                val key = Pair(local.id, local.mediaType ?: "movie")
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
                    remoteWatchlistedAt?.let { traktAuthRepository.saveLastWatchlistTime(it) }
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
                            noteUpdates.add(local.copy(personalNote = item.notes))
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
                                newMovie = newMovie.copy(personalNote = item.notes)
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

            traktAuthRepository.saveLastActivitiesTime(remoteLastActivities)

            if (isFirstSync) {
                traktAuthRepository.markFirstSyncCompleted()
                android.util.Log.e("TRAKT_DEBUG", "SYNC MERGE: Primo sync completato e contrassegnato con successo.")
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("TraktSyncWorker", "Errore durante la sync", e)
            Result.retry()
        }
    }
}
