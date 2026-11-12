package com.cinetrack.domain

import com.cinetrack.data.local.entities.WatchHistoryEntity
import com.cinetrack.data.model.Movie
import com.cinetrack.util.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

/**
 * Single Source of Truth for filtering and ordering the "Continua a guardare" TV row.
 *
 * Orders shows descending by the most recent real watch timestamp (`WatchHistoryEntity.watchedAt`
 * or `Movie.watchedAt`), never by technical sync timestamps (`clientUpdatedAt`) as primary key.
 */
class SelectContinueWatchingTvUseCase @Inject constructor() {

    /**
     * Builds a map from TV show TMDB ID to its latest recorded watch timestamp in epoch millis.
     */
    fun buildLatestWatchMillisMap(histories: List<WatchHistoryEntity>): Map<Long, Long> {
        return buildMap {
            for (history in histories) {
                if (history.mediaType != "tv" || history.syncStatus == "deleted") continue
                val millis = DateTimeUtils.parseIsoDateToMillis(history.watchedAt)
                if (millis > 0L) {
                    val prev = get(history.movieId) ?: 0L
                    if (millis > prev) {
                        put(history.movieId, millis)
                    }
                }
            }
        }
    }

    /**
     * Resolves the most recent watch timestamp (epoch millis) for a show using both
     * the WatchHistory table and the Movie's `watchedAt` field.
     */
    fun resolveLatestWatchMillis(
        movie: Movie,
        latestWatchMillisByTvId: Map<Long, Long>
    ): Long {
        val historyMillis = if (movie.mediaType == "tv") latestWatchMillisByTvId[movie.id] ?: 0L else 0L
        val movieMillis = DateTimeUtils.parseIsoDateToMillis(movie.watchedAt)
        return maxOf(historyMillis, movieMillis)
    }

    operator fun invoke(
        localMovies: List<Movie>,
        latestWatchMillisByTvId: Map<Long, Long>,
        limit: Int = 10
    ): ImmutableList<Movie> {
        return localMovies
            .asSequence()
            .filter { movie ->
                if (movie.mediaType != "tv") return@filter false
                if (movie.syncStatus == "pending_delete" || movie.syncStatus == "deleted") return@filter false
                if (movie.watched || movie.dropped) return@filter false
                val watchedCount = movie.watchedEpisodes
                    ?.filterKeys { it != "0" }
                    ?.values
                    ?.sumOf { it.size }
                    ?: 0
                if (watchedCount <= 0) return@filter false
                val next = movie.calculateNextEpisode()
                next != null && !next.isUpToDateWithAirDate && next.remainingTotal > 0
            }
            .sortedWith(
                compareByDescending<Movie> { movie ->
                    resolveLatestWatchMillis(movie, latestWatchMillisByTvId)
                }
                    .thenByDescending { it.clientUpdatedAt }
                    .thenBy { it.title ?: it.name ?: "" }
                    .thenBy { it.id }
            )
            .take(limit)
            .toImmutableList()
    }
}
