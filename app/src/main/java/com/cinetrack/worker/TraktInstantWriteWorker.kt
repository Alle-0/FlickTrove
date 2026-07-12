package com.cinetrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinetrack.data.api.TraktEpisodeHistoryRequest
import com.cinetrack.data.api.TraktEpisodeNumber
import com.cinetrack.data.api.TraktRatingMovie
import com.cinetrack.data.api.TraktRatingRequest
import com.cinetrack.data.api.TraktRatingShow
import com.cinetrack.data.api.TraktSeasonEpisodes
import com.cinetrack.data.api.TraktService
import com.cinetrack.data.api.TraktShowWithEpisodes
import com.cinetrack.data.api.TraktSyncIds
import com.cinetrack.data.api.TraktSyncMovie
import com.cinetrack.data.api.TraktSyncRequest
import com.cinetrack.data.api.TraktSyncShow
import com.cinetrack.data.repository.TraktAuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class TraktInstantWriteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val traktService: TraktService,
    private val traktAuthRepository: TraktAuthRepository,
    private val movieRepository: com.cinetrack.data.repository.MovieRepository // <-- AGGIUNTO
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        // Richiesto da Android 12+ per i lavori expedited
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            "trakt_sync_channel"
        )
            .setContentTitle("Trakt")
            .setContentText("Sincronizzazione in corso...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
        return androidx.work.ForegroundInfo(1002, notification)
    }

   override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Salta silenziosamente se l'utente non è loggato su Trakt
        if (!traktAuthRepository.isLoggedIn.value) {
            return@withContext Result.success()
        }

        val action = inputData.getString(KEY_ACTION) ?: return@withContext Result.failure()

        try {
            // ── 1. GESTIONE LISTE CUSTOM (Non richiede mediaType o tmdbId) ──
            val listId = inputData.getLong("LIST_ID", -1L)
            when (action) {
                "ACTION_CREATE_LIST" -> {
                    val name = inputData.getString("LIST_NAME") ?: ""
                    val desc = inputData.getString("LIST_DESC") ?: ""
                    val localId = inputData.getString("LOCAL_FOLDER_ID") ?: ""

                    val request = com.cinetrack.data.api.TraktListRequest(name = name, description = desc)
                    val response = traktService.createList(request)

                    if (response.isSuccessful && localId.isNotEmpty()) {
                        val traktId = response.body()?.ids?.trakt
                        if (traktId != null) {
                            val newId = "trakt_$traktId"
                            val localFolder = movieRepository.getFolderFlow(localId).firstOrNull()

                            if (localFolder != null) {
                                // 1. Ne creiamo una copia esatta con l'ID ufficiale di Trakt
                                val newFolder = localFolder.copy(id = newId)
                                
                                // 2. Salvandola, il MovieRepository si accorgerà da solo che ci sono 
                                // dei film all'interno e invierà automaticamente l'ordine ad ACTION_ADD_LIST_ITEMS!
                                movieRepository.saveFolder(newFolder)

                                // 3. Disintegriamo la vecchia cartella locale provvisoria
                                movieRepository.deleteFolder(localId)
                            }
                        }
                    }
                    return@withContext Result.success()
                }
                "ACTION_UPDATE_LIST" -> {
                    if (listId != -1L) {
                        val name = inputData.getString("LIST_NAME") ?: ""
                        val desc = inputData.getString("LIST_DESC") ?: ""
                        val request = com.cinetrack.data.api.TraktListRequest(name = name, description = desc)
                        traktService.updateList(listId, request)
                        return@withContext Result.success()
                    }
                }
                "ACTION_DELETE_LIST" -> {
                    if (listId != -1L) {
                        traktService.deleteList(listId)
                        return@withContext Result.success()
                    }
                }
                "ACTION_ADD_LIST_ITEMS" -> {
                    if (listId != -1L) {
                        val added = inputData.getStringArray("ITEMS_ADDED") ?: emptyArray()
                        val request = buildListItemsRequest(added)
                        traktService.addListItems(listId, request)
                        return@withContext Result.success()
                    }
                }
                "ACTION_REMOVE_LIST_ITEMS" -> {
                    if (listId != -1L) {
                        val removed = inputData.getStringArray("ITEMS_REMOVED") ?: emptyArray()
                        val request = buildListItemsRequest(removed)
                        traktService.removeListItems(listId, request)
                        return@withContext Result.success()
                    }
                }
            }

            // ── 2. GESTIONE FILM/SERIE (Richiede mediaType e tmdbId) ──
            val mediaType = inputData.getString(KEY_MEDIA_TYPE) ?: return@withContext Result.failure()
            val tmdbId    = inputData.getLong(KEY_TMDB_ID, -1L)
            val imdbId    = inputData.getString(KEY_IMDB_ID)

            if (tmdbId == -1L && imdbId == null) return@withContext Result.failure()

            val ids = TraktSyncIds(
                tmdb = if (tmdbId != -1L) tmdbId else null,
                imdb = imdbId
            )

            when (action) {
                // ── History (watched) ─────────────────────────────────────────
                ACTION_MARK_WATCHED -> {
                    traktService.addToHistory(buildSyncRequest(mediaType, ids))
                }
                ACTION_REMOVE_WATCHED -> {
                    traktService.removeFromHistory(buildSyncRequest(mediaType, ids))
                }

                // ── Ratings ───────────────────────────────────────────────────
                ACTION_ADD_RATING -> {
                    val rating = inputData.getInt(KEY_RATING, 0)
                    if (rating !in 1..10) return@withContext Result.failure()
                    val request = if (mediaType == "movie") {
                        TraktRatingRequest(
                            rating  = rating,
                            movies  = listOf(TraktRatingMovie(ids = ids))
                        )
                    } else {
                        TraktRatingRequest(
                            rating = rating,
                            shows  = listOf(TraktRatingShow(ids = ids))
                        )
                    }
                    traktService.addRating(request)
                }
                ACTION_REMOVE_RATING -> {
                    val request = if (mediaType == "movie") {
                        TraktRatingRequest(rating = 1, movies = listOf(TraktRatingMovie(ids = ids)))
                    } else {
                        TraktRatingRequest(rating = 1, shows  = listOf(TraktRatingShow(ids = ids)))
                    }
                    traktService.removeRating(request)
                }

                // ── Watchlist ─────────────────────────────────────────────────
                ACTION_ADD_WATCHLIST -> {
                    traktService.addToWatchlist(buildSyncRequest(mediaType, ids))
                }
                ACTION_REMOVE_WATCHLIST -> {
                    traktService.removeFromWatchlist(buildSyncRequest(mediaType, ids))
                }

                // ── Episodes ──────────────────────────────────────────────────
                ACTION_MARK_EPISODES_WATCHED -> {
                    val encodedEps = inputData.getString(KEY_SEASON_EPISODES)
                        ?: return@withContext Result.failure()
                    val request = buildEpisodeHistoryRequest(ids, encodedEps)
                    traktService.addEpisodesToHistory(request)
                }
                ACTION_REMOVE_EPISODES_WATCHED -> {
                    val encodedEps = inputData.getString(KEY_SEASON_EPISODES)
                        ?: return@withContext Result.failure()
                    val request = buildEpisodeHistoryRequest(ids, encodedEps)
                    traktService.removeEpisodesFromHistory(request)
                }

                else -> return@withContext Result.failure()
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("TraktInstantWriteWorker", "Failed to write to Trakt, will retry", e)
            Result.retry()
        }
    }

    private fun buildSyncRequest(mediaType: String, ids: TraktSyncIds): TraktSyncRequest =
        if (mediaType == "movie") {
            TraktSyncRequest(movies = listOf(TraktSyncMovie(ids)))
        } else {
            TraktSyncRequest(shows = listOf(TraktSyncShow(ids)))
        }

    /**
     * Parses the encoded episodes string into a [TraktEpisodeHistoryRequest].
     *
     * Format: "season:ep1,ep2;season2:ep1"  e.g. "1:1,2,3;2:5,6"
     */
    private fun buildEpisodeHistoryRequest(
        ids: TraktSyncIds,
        encodedEps: String
    ): TraktEpisodeHistoryRequest {
        val seasons = encodedEps.split(";").mapNotNull { part ->
            val colonIdx = part.indexOf(':')
            if (colonIdx == -1) return@mapNotNull null
            val seasonNum = part.substring(0, colonIdx).toIntOrNull() ?: return@mapNotNull null
            val epNums = part.substring(colonIdx + 1)
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .map { TraktEpisodeNumber(it) }
            if (epNums.isEmpty()) return@mapNotNull null
            TraktSeasonEpisodes(number = seasonNum, episodes = epNums)
        }
        return TraktEpisodeHistoryRequest(
            shows = listOf(TraktShowWithEpisodes(ids = ids, seasons = seasons))
        )
    }

    private fun buildListItemsRequest(items: Array<String>): com.cinetrack.data.api.TraktListItemsRequest {
        val movies = mutableListOf<com.cinetrack.data.api.TraktMovieItem>()
        val shows = mutableListOf<com.cinetrack.data.api.TraktShowItem>()
        
        for (item in items) {
            val parts = item.split("_")
            if (parts.size == 2) {
                val type = parts[0]
                val tmdbId = parts[1].toLongOrNull()
                if (tmdbId != null) {
                    val ids = com.cinetrack.data.api.TraktSyncIds(tmdb = tmdbId)
                    if (type == "movie") {
                        movies.add(com.cinetrack.data.api.TraktMovieItem(ids = ids))
                    } else if (type == "tv") {
                        shows.add(com.cinetrack.data.api.TraktShowItem(ids = ids))
                    }
                }
            }
        }
        return com.cinetrack.data.api.TraktListItemsRequest(
            movies = movies.takeIf { it.isNotEmpty() },
            shows = shows.takeIf { it.isNotEmpty() }
        )
    }

    companion object {
        const val KEY_ACTION         = "action"
        const val KEY_MEDIA_TYPE     = "media_type"
        const val KEY_TMDB_ID        = "tmdb_id"
        const val KEY_IMDB_ID        = "imdb_id"
        const val KEY_RATING         = "rating"
        const val KEY_SEASON_EPISODES = "season_episodes"

        const val ACTION_MARK_WATCHED           = "mark_watched"
        const val ACTION_REMOVE_WATCHED         = "remove_watched"
        const val ACTION_ADD_RATING             = "add_rating"
        const val ACTION_REMOVE_RATING          = "remove_rating"
        const val ACTION_ADD_WATCHLIST          = "add_watchlist"
        const val ACTION_REMOVE_WATCHLIST       = "remove_watchlist"
        const val ACTION_MARK_EPISODES_WATCHED  = "mark_episodes_watched"
        const val ACTION_REMOVE_EPISODES_WATCHED = "remove_episodes_watched"

        /**
         * Encodes a [Map<String, List<Int>>] (stagione -> episodi) nel formato
         * atteso da [KEY_SEASON_EPISODES]: "1:1,2,3;2:5,6"
         */
        fun encodeEpisodes(watchedEpisodes: Map<String, List<Int>>): String =
            watchedEpisodes.entries
                .filter { it.value.isNotEmpty() }
                .joinToString(";") { (season, eps) ->
                    "$season:${eps.joinToString(",")}"
                }
    }
}
