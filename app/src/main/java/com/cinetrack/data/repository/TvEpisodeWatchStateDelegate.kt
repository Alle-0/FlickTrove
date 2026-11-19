package com.cinetrack.data.repository

import com.cinetrack.data.local.entities.WatchHistoryEntity
import com.cinetrack.data.model.Movie
import com.cinetrack.util.DateTimeUtils
import com.cinetrack.util.getEpisodeWatchDate
import com.cinetrack.util.removeEpisodeWatchDate
import com.cinetrack.util.setEpisodeWatchDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegate managing TV episode watch states, date mapping serialization/deserialization,
 * legacy backfilling, and watch history session updates.
 */
@Singleton
class TvEpisodeWatchStateDelegate @Inject constructor(
    private val watchHistoryRepository: WatchHistoryRepository
) {
    suspend fun recordEpisodeWatchState(
        movie: Movie,
        changedEpisodes: List<Pair<Int, Int>>? = null,
        isAdding: Boolean = true,
        customDate: Instant? = null,
        isProgressAdvancing: Boolean = true,
        onSaveMovie: suspend (Movie) -> Unit
    ) {
        if (movie.mediaType != "tv") return
        val watchedCount = movie.watchedEpisodes?.filter { it.key != "0" }?.values?.sumOf { it.size } ?: 0
        if (watchedCount > 0 || movie.watched) {
            val currentHistory = watchHistoryRepository.getWatchHistoryForMovie(movie.id, movie.mediaType)
            val activeSession = if (currentHistory.any { it.isRewatch }) {
                currentHistory.filter { it.isRewatch }.maxByOrNull { DateTimeUtils.parseIsoDateToMillis(it.watchedAt) }
            } else {
                currentHistory.find { !it.isRewatch }
            }
            val nowStr = customDate?.toString() ?: Instant.now().toString()

            val currentDatesMap = try {
                activeSession?.episodeDates?.let { 
                    Json.decodeFromString<Map<String, String>>(it).toMutableMap() 
                } ?: mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }

            val newlyAddedSet = if (isAdding && !changedEpisodes.isNullOrEmpty()) {
                changedEpisodes.toSet()
            } else emptySet()

            val watchedEpsMap = movie.watchedEpisodes ?: emptyMap()

            if (activeSession != null) {
                val isBackfillingLegacy = currentDatesMap.isEmpty()
                for ((seasonStr, epList) in watchedEpsMap) {
                    val sNum = seasonStr.toIntOrNull() ?: continue
                    for (epNum in epList) {
                        if ((sNum to epNum) !in newlyAddedSet && currentDatesMap.getEpisodeWatchDate(sNum, epNum) == null) {
                            val fallbackDate = if (isBackfillingLegacy || !isProgressAdvancing) {
                                activeSession.watchedAt
                            } else {
                                nowStr
                            }
                            currentDatesMap.setEpisodeWatchDate(sNum, epNum, fallbackDate)
                        }
                    }
                }
            } else {
                for ((seasonStr, epList) in watchedEpsMap) {
                    val sNum = seasonStr.toIntOrNull() ?: continue
                    for (epNum in epList) {
                        if (currentDatesMap.getEpisodeWatchDate(sNum, epNum) == null) {
                            currentDatesMap.setEpisodeWatchDate(sNum, epNum, nowStr)
                        }
                    }
                }
            }

            if (!changedEpisodes.isNullOrEmpty()) {
                for ((seasonNum, epNum) in changedEpisodes) {
                    if (isAdding) {
                        currentDatesMap.setEpisodeWatchDate(seasonNum, epNum, nowStr)
                    } else {
                        currentDatesMap.removeEpisodeWatchDate(seasonNum, epNum)
                    }
                }
            }

            // Rimuovi eventuali episodi in currentDatesMap che non sono più in watchedEpisodes
            val currentEpisodeKeys = watchedEpsMap.flatMap { (sStr, epList) ->
                val sNum = sStr.toIntOrNull() ?: return@flatMap emptyList<Pair<Int, Int>>()
                epList.map { sNum to it }
            }.toSet()

            val keysToRemove = mutableListOf<Pair<Int, Int>>()
            for (key in currentDatesMap.keys) {
                val parts = if (key.contains("_")) {
                    key.split("_")
                } else if (key.startsWith("S") && key.contains("E")) {
                    key.removePrefix("S").split("E")
                } else null

                if (parts != null && parts.size == 2) {
                    val s = parts[0].toIntOrNull()
                    val e = parts[1].toIntOrNull()
                    if (s != null && e != null && (s to e) !in currentEpisodeKeys) {
                        keysToRemove.add(s to e)
                    }
                }
            }
            for ((s, e) in keysToRemove) {
                currentDatesMap.removeEpisodeWatchDate(s, e)
            }

            val newEpisodeDatesJson = if (currentDatesMap.isNotEmpty()) {
                try { Json.encodeToString(currentDatesMap) } catch (e: Exception) { null }
            } else null

            if (activeSession == null) {
                watchHistoryRepository.insertWatchHistory(
                    WatchHistoryEntity(
                        movieId = movie.id,
                        mediaType = movie.mediaType,
                        watchedAt = nowStr,
                        isRewatch = false,
                        episodeDates = newEpisodeDatesJson
                    )
                )
                if (movie.watchedAt != nowStr) {
                    onSaveMovie(movie.copy(watchedAt = nowStr, clientUpdatedAt = System.currentTimeMillis()))
                }
            } else {
                val allResolvedDates = currentDatesMap.values.toList()
                val targetIso = if (allResolvedDates.isNotEmpty()) {
                    allResolvedDates.maxByOrNull { DateTimeUtils.parseIsoDateToMillis(it) } ?: nowStr
                } else if (isProgressAdvancing) {
                    nowStr
                } else {
                    activeSession.watchedAt
                }

                watchHistoryRepository.updateWatchHistory(
                    activeSession.copy(
                        watchedAt = targetIso,
                        episodeDates = newEpisodeDatesJson,
                        syncStatus = "pending"
                    )
                )
                val latestHistory = watchHistoryRepository.getWatchHistoryForMovie(movie.id, movie.mediaType)
                    .maxByOrNull { DateTimeUtils.parseIsoDateToMillis(it.watchedAt) }
                if (latestHistory != null && movie.watchedAt != latestHistory.watchedAt) {
                    onSaveMovie(movie.copy(watchedAt = latestHistory.watchedAt, clientUpdatedAt = System.currentTimeMillis()))
                }
            }
        }
    }
}
