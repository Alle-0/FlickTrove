package com.cinetrack.worker

import android.util.Log
import com.cinetrack.data.api.TMDBService
import com.cinetrack.data.api.TraktEpisodeHistoryRequest
import com.cinetrack.data.api.TraktEpisodeNumber
import com.cinetrack.data.api.TraktHistoryItem
import com.cinetrack.data.api.TraktSeasonEpisodes
import com.cinetrack.data.api.TraktService
import com.cinetrack.data.api.TraktShowWithEpisodes
import com.cinetrack.data.api.TraktSyncIds
import com.cinetrack.data.api.TraktWatchedShow
import com.cinetrack.data.local.entities.WatchHistoryEntity
import com.cinetrack.data.mapper.MovieMapper
import com.cinetrack.data.model.Movie
import com.cinetrack.data.remote.FirebaseRemoteDataSource
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.util.DateTimeUtils
import com.cinetrack.util.getEpisodeWatchDate
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

/**
 * Delegate managing TV Shows watched episodes synchronization, episode counting,
 * per-episode dates resolution, and two-way diffing between Trakt and local SQLite.
 */
class TraktShowSyncDelegate(
    private val traktService: TraktService,
    private val tmdbService: TMDBService,
    private val movieRepository: MovieRepository
) {

    suspend fun syncWatchedShows(
        historyAccumulator: List<TraktHistoryItem>,
        isIntegrityValid: Boolean,
        isFirstSync: Boolean,
        bulkStatsUpdates: MutableList<FirebaseRemoteDataSource.TrendingStatUpdate>,
        isRecent: (String?) -> Boolean
    ) {
        try {
            Log.e("TRAKT_DEBUG", "Inizio Fase 1b: Chiamata getWatchedShowsRaw...")

            // Mappa di fallback dagli episodi già recuperati nella cronologia (historyAccumulator)
            val historyEpisodesByShowTmdb = mutableMapOf<Long, MutableMap<String, MutableSet<Int>>>()
            val historyShowTitles = mutableMapOf<Long, String>()
            val historyShowImdbs = mutableMapOf<String, MutableMap<String, MutableSet<Int>>>()
            val historyEpisodeDatesByShowTmdb = mutableMapOf<Long, MutableMap<String, String>>()
            val historyEpisodeDatesByShowImdb = mutableMapOf<String, MutableMap<String, String>>()

            for (item in historyAccumulator) {
                if (item.type == "episode" && item.episode != null && item.show != null) {
                    val seasonNum = item.episode.season
                    val epNum = item.episode.number
                    if (seasonNum > 0 && epNum > 0) {
                        val tmdbId = item.show.ids?.tmdb
                        val imdbId = item.show.ids?.imdb
                        val epKey = "S${seasonNum}E${epNum}"
                        val watchedAt = item.watched_at
                        if (tmdbId != null) {
                            val seasonsMap = historyEpisodesByShowTmdb.getOrPut(tmdbId) { mutableMapOf() }
                            val epsSet = seasonsMap.getOrPut(seasonNum.toString()) { mutableSetOf() }
                            epsSet.add(epNum)
                            item.show.title?.let { historyShowTitles[tmdbId] = it }
                            if (!watchedAt.isNullOrBlank()) {
                                historyEpisodeDatesByShowTmdb.getOrPut(tmdbId) { mutableMapOf() }[epKey] = watchedAt
                            }
                        } else if (!imdbId.isNullOrBlank()) {
                            val seasonsMap = historyShowImdbs.getOrPut(imdbId) { mutableMapOf() }
                            val epsSet = seasonsMap.getOrPut(seasonNum.toString()) { mutableSetOf() }
                            epsSet.add(epNum)
                            if (!watchedAt.isNullOrBlank()) {
                                historyEpisodeDatesByShowImdb.getOrPut(imdbId) { mutableMapOf() }[epKey] = watchedAt
                            }
                        }
                    }
                }
            }

            val watchedShows = mutableListOf<TraktWatchedShow>()
            val jsonParser = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }

            val rawResponse = traktService.getWatchedShowsRaw()
            if (rawResponse.isSuccessful) {
                val rawJson = rawResponse.body()?.string() ?: "[]"
                Log.e("TRAKT_DEBUG", "SYNC RAW JSON Sample: ${rawJson.take(600)}")
                val pageShows = jsonParser.decodeFromString<List<TraktWatchedShow>>(rawJson)
                watchedShows.addAll(pageShows)
            } else {
                Log.e("TraktSyncWorker", "SYNC: Errore API getWatchedShowsRaw (${rawResponse.code()})")
            }

            Log.e("TRAKT_DEBUG", "SYNC: Ricevuti ${watchedShows.size} show da Trakt.")

            val episodeUpdates = mutableListOf<Movie>()
            val processedShowTmdbIds = mutableSetOf<Long>()

            suspend fun processShowEpisodes(
                showTmdbId: Long,
                showTitle: String,
                seasonsMap: Map<String, List<Int>>,
                lastWatchedAt: String? = null,
                showEpisodeDates: Map<String, String> = emptyMap()
            ) {
                val totalWatched = seasonsMap.values.sumOf { it.size }
                val local = movieRepository.getMovieIncludingDeleted(showTmdbId, "tv")

                // Anti-resurrection guard: if the user explicitly deleted this show, never re-insert it
                if (local != null && (local.syncStatus == "pending_delete" || local.syncStatus == "deleted")) {
                    Log.d("TraktSyncWorker", "SYNC: Show '$showTitle' è ${local.syncStatus}, skip resurrezione.")
                    return
                }

                // --- Sincronizzazione episodeDates in Room WatchHistoryEntity ---
                val existingHistory = movieRepository.getWatchHistoryForMovie(showTmdbId, "tv")
                val existingEpDates = mutableMapOf<String, String>()
                existingHistory.forEach { h ->
                    h.episodeDates?.let { json ->
                        try {
                            val parsed: Map<String, String> = Json.decodeFromString(json)
                            existingEpDates.putAll(parsed)
                        } catch (_: Exception) {}
                    }
                }
                val mergedEpDates = (existingEpDates + showEpisodeDates).toMutableMap()
                val epDatesJson = if (mergedEpDates.isNotEmpty()) {
                    try {
                        Json.encodeToString(
                            kotlinx.serialization.serializer(),
                            mergedEpDates
                        )
                    } catch (_: Exception) { null }
                } else null
                val bestWatchedDate = lastWatchedAt ?: showEpisodeDates.values.maxOrNull()
                val bestWatchedMillis = DateTimeUtils.parseIsoDateToMillis(bestWatchedDate)

                if (existingHistory.isNotEmpty()) {
                    val latestEntity = existingHistory.maxByOrNull { it.watchedAt } ?: existingHistory.first()
                    val latestEntityMillis = DateTimeUtils.parseIsoDateToMillis(latestEntity.watchedAt)
                    val shouldUpdateDate = bestWatchedDate != null && bestWatchedMillis > latestEntityMillis
                    val shouldUpdateEpDates = epDatesJson != null && epDatesJson != latestEntity.episodeDates
                    if (shouldUpdateDate || shouldUpdateEpDates) {
                        movieRepository.updateWatchHistory(
                            latestEntity.copy(
                                watchedAt = bestWatchedDate?.takeIf { shouldUpdateDate } ?: latestEntity.watchedAt,
                                episodeDates = if (shouldUpdateEpDates) epDatesJson else latestEntity.episodeDates,
                                syncStatus = "pending"
                            )
                        )
                    }
                } else if (totalWatched > 0) {
                    val date = bestWatchedDate ?: local?.watchedAt ?: DateTimeUtils.nowIsoUtc()
                    movieRepository.insertWatchHistory(
                        WatchHistoryEntity(
                            movieId = showTmdbId,
                            mediaType = "tv",
                            watchedAt = date,
                            isRewatch = false,
                            episodeDates = epDatesJson
                        )
                    )
                }

                if (local == null) {
                    Log.d("TraktSyncWorker", "SYNC: Show '$showTitle' non in locale, scarico...")
                    try {
                        val tmdbResponse = tmdbService.getTVBasicDetails(showTmdbId)
                        var newShow = MovieMapper.mapResponseToMovie(tmdbResponse, "tv")

                        val totalEps = newShow.effectiveTotalEpisodes
                        val isCompleted = totalEps > 0 && totalWatched >= totalEps
                        val progressVal = if (totalEps > 0) (totalWatched.toDouble() / totalEps.toDouble()).coerceIn(0.0, 1.0) else 0.0

                        newShow = newShow.copy(
                            watched = isCompleted,
                            favorite = if (!isCompleted && totalWatched > 0) true else newShow.favorite,
                            watchedEpisodes = seasonsMap,
                            progress = progressVal,
                            watchedAt = bestWatchedDate ?: newShow.watchedAt,
                            syncStatus = "synced",
                            clientUpdatedAt = System.currentTimeMillis()
                        )
                        Log.e("TRAKT_DEBUG", "SYNC: Nuovo show scaricato '$showTitle' (Visti: $totalWatched/$totalEps, favorite: ${newShow.favorite}, watched: ${newShow.watched}, progress: $progressVal)")
                        episodeUpdates.add(newShow)
                        delay(50)
                    } catch (e: Exception) {
                        Log.e("TraktSyncWorker", "Errore scaricamento dettagli show TMDB $showTmdbId", e)
                    }
                } else {
                    val totalEps = local.effectiveTotalEpisodes
                    val isCompleted = totalEps > 0 && totalWatched >= totalEps
                    val shouldBeFavorite = if (isCompleted) false else if (totalWatched > 0) true else local.favorite
                    val progressVal = if (totalEps > 0) (totalWatched.toDouble() / totalEps.toDouble()).coerceIn(0.0, 1.0) else 0.0

                    var updatedPosterPath = local.posterPath
                    var updatedBackdropPath = local.backdropPath
                    var updatedNumberOfEpisodes = local.numberOfEpisodes

                    if (local.posterPath.isNullOrEmpty() || (local.numberOfEpisodes ?: 0) == 0) {
                        try {
                            val tmdbResponse = tmdbService.getTVBasicDetails(showTmdbId)
                            val fresh = MovieMapper.mapResponseToMovie(tmdbResponse, "tv")
                            updatedPosterPath = fresh.posterPath ?: local.posterPath
                            updatedBackdropPath = fresh.backdropPath ?: local.backdropPath
                            val freshEpisodes = fresh.numberOfEpisodes
                            if (freshEpisodes != null && freshEpisodes > 0) {
                                updatedNumberOfEpisodes = freshEpisodes
                            }
                            delay(50)
                        } catch (_: Exception) {}
                    }

                    val resolvedWatchedAt = bestWatchedDate ?: local.watchedAt
                    if (seasonsMap != local.watchedEpisodes || local.watched != isCompleted || local.favorite != shouldBeFavorite || updatedNumberOfEpisodes != local.numberOfEpisodes || local.progress != progressVal || updatedPosterPath != local.posterPath || resolvedWatchedAt != local.watchedAt) {
                        Log.e("TRAKT_DEBUG", "SYNC: Aggiorno stato per '$showTitle' (Visti: $totalWatched/$totalEps, favorite: $shouldBeFavorite, watched: $isCompleted, progress: $progressVal)")
                        episodeUpdates.add(
                            local.copy(
                                watched = isCompleted,
                                favorite = shouldBeFavorite,
                                numberOfEpisodes = updatedNumberOfEpisodes,
                                watchedEpisodes = seasonsMap,
                                progress = progressVal,
                                posterPath = updatedPosterPath,
                                backdropPath = updatedBackdropPath,
                                watchedAt = resolvedWatchedAt,
                                syncStatus = "synced",
                                clientUpdatedAt = System.currentTimeMillis()
                            )
                        )

                        if (isRecent(lastWatchedAt)) {
                            val localWatched = local.watchedEpisodes?.values?.sumOf { it.size } ?: 0
                            val delta = totalWatched - localWatched
                            if (delta > 0) {
                                bulkStatsUpdates.add(
                                    FirebaseRemoteDataSource.TrendingStatUpdate(
                                        compositeId = local.compositeId,
                                        viewsDelta = delta.toLong()
                                    )
                                )
                            }
                        }
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
                        val resolved = found.tvResults?.firstOrNull()?.id
                        if (resolved != null && resolved > 100L) {
                            showTmdbId = resolved
                        } else if (resolved != null) {
                            Log.w("TraktSyncWorker", "SYNC: ID TMDB sospetto ($resolved) per '$showTitle' via IMDB $imdbId — skip")
                        }
                    } catch (e: Exception) {
                        Log.e("TRAKT_DEBUG", "Errore lookup TMDB per IMDB ID $imdbId", e)
                    }
                }

                val tvdbId = remoteShow.show?.ids?.tvdb
                if (showTmdbId == null && tvdbId != null && tvdbId > 0L) {
                    try {
                        val found = tmdbService.findByExternalId(tvdbId.toString(), "tvdb_id")
                        val resolved = found.tvResults?.firstOrNull()?.id
                        if (resolved != null && resolved > 100L) {
                            showTmdbId = resolved
                        }
                    } catch (e: Exception) {
                        Log.e("TRAKT_DEBUG", "Errore lookup TMDB per TVDB ID $tvdbId", e)
                    }
                }

                if (showTmdbId == null || showTmdbId <= 0L) continue

                processedShowTmdbIds.add(showTmdbId)

                val mergedEps = mutableMapOf<String, MutableSet<Int>>()
                val mergedEpDates = mutableMapOf<String, String>()
                for (season in remoteShow.seasons ?: emptyList()) {
                    if (season.number == 0) continue
                    val sNum = season.number
                    val eps = season.episodes ?: emptyList()
                    if (eps.isNotEmpty()) {
                        mergedEps.getOrPut(sNum.toString()) { mutableSetOf() }.addAll(eps.map { it.number })
                        eps.forEach { ep ->
                            if (!ep.last_watched_at.isNullOrBlank()) {
                                mergedEpDates["S${sNum}E${ep.number}"] = ep.last_watched_at
                            }
                        }
                    }
                }

                historyEpisodesByShowTmdb[showTmdbId]?.forEach { (seasonStr, epsSet) ->
                    mergedEps.getOrPut(seasonStr) { mutableSetOf() }.addAll(epsSet)
                }
                historyEpisodeDatesByShowTmdb[showTmdbId]?.forEach { (epKey, dateStr) ->
                    mergedEpDates.putIfAbsent(epKey, dateStr)
                }
                if (!imdbId.isNullOrBlank()) {
                    historyShowImdbs[imdbId]?.forEach { (seasonStr, epsSet) ->
                        mergedEps.getOrPut(seasonStr) { mutableSetOf() }.addAll(epsSet)
                    }
                    historyEpisodeDatesByShowImdb[imdbId]?.forEach { (epKey, dateStr) ->
                        mergedEpDates.putIfAbsent(epKey, dateStr)
                    }
                }

                val finalEps = mergedEps.mapValues { (_, eps) -> eps.sorted() }
                val totalMergedCount = finalEps.values.sumOf { it.size }

                if (showTitle.contains("Silo", ignoreCase = true) || showTitle.contains("Dragon", ignoreCase = true) || showTitle.contains("Mirror", ignoreCase = true)) {
                    Log.e("TRAKT_DEBUG", "SYNC SHOW DEBUG '$showTitle': tmdbId=$showTmdbId, seasons=${remoteShow.seasons?.size}, mergedEps=$totalMergedCount")
                }

                processShowEpisodes(showTmdbId, showTitle, finalEps, remoteShow.last_watched_at, mergedEpDates)
            }

            // Processa anche eventuali serie TV presenti in historyAccumulator non restituite da getWatchedShows
            for ((showTmdbId, seasonsMap) in historyEpisodesByShowTmdb) {
                if (processedShowTmdbIds.contains(showTmdbId)) continue
                processedShowTmdbIds.add(showTmdbId)
                val showTitle = historyShowTitles[showTmdbId] ?: "Unknown"
                val finalEps = seasonsMap.mapValues { (_, eps) -> eps.sorted() }
                val epDates = historyEpisodeDatesByShowTmdb[showTmdbId] ?: emptyMap()
                processShowEpisodes(showTmdbId, showTitle, finalEps, null, epDates)
            }

            // Logica di Merge o Rimozione (Diff) protetta
            if (isIntegrityValid) {
                val localTvShows = movieRepository.getLocalMoviesIncludingDeleted().filter { it.mediaType == "tv" }
                if (isFirstSync || processedShowTmdbIds.isEmpty()) {
                    val showsToPush = mutableListOf<TraktShowWithEpisodes>()
                    for (local in localTvShows) {
                        if (processedShowTmdbIds.contains(local.id)) continue
                        val historyList = movieRepository.getWatchHistoryForMovie(local.id, "tv")
                        val localEpDates = mutableMapOf<String, String>()
                        historyList.forEach { h ->
                            h.episodeDates?.let { json ->
                                try {
                                    val parsed: Map<String, String> = Json.decodeFromString(json)
                                    localEpDates.putAll(parsed)
                                } catch (_: Exception) {}
                            }
                        }

                        val seasonsList = local.watchedEpisodes?.mapNotNull { (seasonStr, epList) ->
                            val seasonNum = seasonStr.toIntOrNull() ?: return@mapNotNull null
                            if (seasonNum == 0 || epList.isEmpty()) null
                            else TraktSeasonEpisodes(
                                number = seasonNum,
                                episodes = epList.map { epNum ->
                                    val epWatchedAt = localEpDates.getEpisodeWatchDate(seasonNum, epNum) ?: local.watchedAt
                                    TraktEpisodeNumber(epNum, watched_at = epWatchedAt)
                                }
                            )
                        } ?: emptyList()
                        if (seasonsList.isNotEmpty()) {
                            showsToPush.add(
                                TraktShowWithEpisodes(
                                    ids = TraktSyncIds(tmdb = local.id),
                                    seasons = seasonsList
                                )
                            )
                        }
                    }
                    if (showsToPush.isNotEmpty()) {
                        Log.e("TRAKT_DEBUG", "SYNC MERGE: Primo sync/Account vuoto -> Push di ${showsToPush.size} serie TV viste su Trakt")
                        try {
                            traktService.addEpisodesToHistory(TraktEpisodeHistoryRequest(shows = showsToPush))
                        } catch (e: Exception) {
                            Log.e("TraktSyncWorker", "Errore push episodi visti verso Trakt", e)
                        }
                    }
                } else {
                    for (local in localTvShows) {
                        if (local.syncStatus == "pending_delete") continue
                        if (local.dropped) continue
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

            movieRepository.saveMoviesBulk(episodeUpdates)
        } catch (e: Exception) {
            Log.e("TraktSyncWorker", "Errore durante la Fase 1b (Watched Shows)", e)
        }
    }
}
