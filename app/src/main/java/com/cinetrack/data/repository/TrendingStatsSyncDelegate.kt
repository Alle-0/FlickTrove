package com.cinetrack.data.repository

import com.cinetrack.data.api.MovieDetailResponse
import com.cinetrack.data.model.Movie
import com.cinetrack.data.remote.FirebaseRemoteDataSource
import com.cinetrack.ui.components.detail.normalizeVibeCode
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegate managing Firebase global trending stats delta calculation (views, rating, vibes, MVP actor)
 * and in-memory TTL caching for Top 10 FlickTrove movies and TV shows.
 */
@Singleton
class TrendingStatsSyncDelegate @Inject constructor(
    private val firebaseRemoteDataSource: FirebaseRemoteDataSource
) {
    private val top10Mutex = Mutex()
    private var cachedTop10Both: Pair<List<Movie>, List<Movie>>? = null
    private var lastTop10FetchTime: Long = 0L
    private val TOP10_TTL_MS = 12 * 60 * 60 * 1000L // 12 ore

    fun parseVibeCodes(emotionalVibes: String?): Set<String> {
        return emotionalVibes
            ?.split(",")
            ?.mapNotNull { raw ->
                val normalized = normalizeVibeCode(raw)
                normalized.takeIf { it.isNotBlank() }
            }
            ?.toSet()
            ?: emptySet()
    }

    suspend fun syncGlobalStatsDeltas(
        oldMovie: Movie?,
        movie: Movie,
        onInvalidateCache: (String) -> Unit
    ) {
        val oldVibes = parseVibeCodes(oldMovie?.emotionalVibes)
        val newVibes = parseVibeCodes(movie.emotionalVibes)

        val addedVibes = newVibes - oldVibes
        val removedVibes = oldVibes - newVibes

        val newMvp = movie.favoriteActorId
        val oldMvp = oldMovie?.favoriteActorId

        val oldRating = oldMovie?.personalRating
        val newRating = movie.personalRating

        val newStatus = if (movie.watched) "watched" else "unwatched"
        val oldStatus = if (oldMovie?.watched == true) "watched" else "unwatched"

        var calculatedViewsDelta = 0L
        if (movie.mediaType == "tv") {
            val oldEps = oldMovie?.watchedEpisodes?.values?.sumOf { it.size } ?: 0
            val newEps = movie.watchedEpisodes?.values?.sumOf { it.size } ?: 0
            calculatedViewsDelta = (newEps - oldEps).toLong()
        } else {
            if (newStatus == "watched" && oldStatus != "watched") calculatedViewsDelta = 1L
            if (oldStatus == "watched" && newStatus != "watched") calculatedViewsDelta = -1L
        }

        if (addedVibes.isNotEmpty() || removedVibes.isNotEmpty() || newMvp != oldMvp || newRating != oldRating || calculatedViewsDelta != 0L) {
            firebaseRemoteDataSource.updateGlobalMovieStats(
                compositeId = movie.compositeId,
                addedVibes = addedVibes.toList(),
                removedVibes = removedVibes.toList(),
                newMvp = newMvp,
                oldMvp = oldMvp,
                newRating = newRating,
                oldRating = oldRating,
                newStatus = newStatus,
                oldStatus = oldStatus,
                viewsDelta = calculatedViewsDelta
            )
            onInvalidateCache(movie.compositeId)
        }
    }

    suspend fun getTop10FlickTroveBoth(
        fetchMovieDetail: suspend (id: Long, isTv: Boolean) -> MovieDetailResponse
    ): Pair<List<Movie>, List<Movie>> {
        val now = System.currentTimeMillis()
        cachedTop10Both?.let {
            if (now - lastTop10FetchTime < TOP10_TTL_MS) return it
        }

        return top10Mutex.withLock {
            val cached = cachedTop10Both
            if (cached != null && (System.currentTimeMillis() - lastTop10FetchTime) < TOP10_TTL_MS) {
                return@withLock cached
            }

            val (movieIds, tvIds) = firebaseRemoteDataSource.fetchTop10MonthlyBoth()
            val result = coroutineScope {
                val moviesDeferred = async {
                    movieIds.mapNotNull { compositeId ->
                        val idStr = compositeId.removePrefix("movie_")
                        val id = idStr.toLongOrNull() ?: return@mapNotNull null
                        try {
                            val detail = fetchMovieDetail(id, false)
                            Movie(
                                id = detail.id,
                                title = detail.title ?: "",
                                posterPath = detail.posterPath,
                                backdropPath = detail.backdropPath,
                                releaseDate = detail.releaseDate ?: "",
                                overview = detail.overview ?: "",
                                voteAverage = detail.voteAverage ?: 0.0,
                                voteCount = detail.voteCount ?: 0,
                                genreIds = detail.genres?.map { it.id } ?: emptyList(),
                                mediaType = "movie",
                                runtime = detail.runtime
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                val tvDeferred = async {
                    tvIds.mapNotNull { compositeId ->
                        val idStr = compositeId.removePrefix("tv_")
                        val id = idStr.toLongOrNull() ?: return@mapNotNull null
                        try {
                            val detail = fetchMovieDetail(id, true)
                            Movie(
                                id = detail.id,
                                title = detail.name ?: detail.title ?: "",
                                posterPath = detail.posterPath,
                                backdropPath = detail.backdropPath,
                                releaseDate = detail.firstAirDate ?: detail.releaseDate ?: "",
                                overview = detail.overview ?: "",
                                voteAverage = detail.voteAverage ?: 0.0,
                                voteCount = detail.voteCount ?: 0,
                                genreIds = detail.genres?.map { it.id } ?: emptyList(),
                                mediaType = "tv",
                                runtime = detail.runtime
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                Pair(moviesDeferred.await(), tvDeferred.await())
            }
            cachedTop10Both = result
            lastTop10FetchTime = System.currentTimeMillis()
            result
        }
    }

    suspend fun getTop10FlickTrove(
        isTv: Boolean,
        fetchMovieDetail: suspend (id: Long, isTv: Boolean) -> MovieDetailResponse
    ): List<Movie> {
        val (movies, tv) = getTop10FlickTroveBoth(fetchMovieDetail)
        return if (isTv) tv else movies
    }
}
