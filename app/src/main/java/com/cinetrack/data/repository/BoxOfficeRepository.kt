package com.cinetrack.data.repository

import com.cinetrack.data.api.TraktService
import com.cinetrack.data.local.dao.CacheDao
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.local.entities.HomeFeedCacheEntity
import com.cinetrack.data.mapper.MovieMapper
import com.cinetrack.data.model.BoxOfficeMovie
import com.cinetrack.data.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class BoxOfficeRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val cacheDao: CacheDao,
    private val traktService: TraktService,
    @Named("trakt_api_key") private val traktApiKey: String,
    private val movieRepositoryProvider: Provider<MovieRepository>
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    suspend fun getWeekendBoxOffice(forceRefresh: Boolean = false): List<BoxOfficeMovie> = withContext(Dispatchers.IO) {
        val cacheId = "box_office_weekend"
        val ttl = 24 * 60 * 60 * 1000L // 24 hours TTL

        if (!forceRefresh) {
            try {
                val cachedEntity = cacheDao.getHomeFeedEntity(cacheId)
                if (cachedEntity != null && (System.currentTimeMillis() - cachedEntity.updatedAt) < ttl) {
                    val cachedList = json.decodeFromString<List<BoxOfficeMovie>>(cachedEntity.data)
                    if (cachedList.isNotEmpty()) {
                        return@withContext cachedList
                    }
                }
            } catch (e: Exception) {
                // Ignore parse error, proceed to network
            }
        }

        try {
            val traktItems = traktService.getWeekendBoxOffice(apiKey = traktApiKey)
            if (traktItems.isEmpty()) {
                val cachedEntity = cacheDao.getHomeFeedEntity(cacheId)
                if (cachedEntity != null) {
                    return@withContext json.decodeFromString<List<BoxOfficeMovie>>(cachedEntity.data)
                }
                return@withContext emptyList()
            }

            val movieRepo = movieRepositoryProvider.get()
            val boxOfficeList = traktItems.mapIndexedNotNull { index, item ->
                val tmdbId = item.movie?.ids?.tmdb ?: return@mapIndexedNotNull null
                val rank = index + 1
                val revenue = item.revenue
                val formattedRev = BoxOfficeMovie.formatRevenue(revenue)

                // Cache-First: check favoriteDao first
                val localMovie = favoriteDao.getById(tmdbId, "movie")
                val movie = if (localMovie != null && !localMovie.posterPath.isNullOrBlank()) {
                    localMovie
                } else {
                    // Check cache or fetch details
                    try {
                        val detailsResponse = movieRepo.fetchMovieDetails(tmdbId, isTv = false)
                        MovieMapper.mapResponseToMovie(detailsResponse, "movie")
                    } catch (e: Exception) {
                        Movie(
                            id = tmdbId,
                            title = item.movie.title ?: "",
                            releaseDate = item.movie.year?.toString() ?: "",
                            mediaType = "movie"
                        )
                    }
                }

                BoxOfficeMovie(
                    movie = movie,
                    rank = rank,
                    revenue = revenue,
                    formattedRevenue = formattedRev
                )
            }

            if (boxOfficeList.isNotEmpty()) {
                try {
                    cacheDao.saveHomeFeed(
                        HomeFeedCacheEntity(
                            id = cacheId,
                            data = json.encodeToString(boxOfficeList),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    // Ignore cache write error
                }
            }

            boxOfficeList
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val cachedEntity = cacheDao.getHomeFeedEntity(cacheId)
                if (cachedEntity != null) {
                    json.decodeFromString<List<BoxOfficeMovie>>(cachedEntity.data)
                } else {
                    emptyList()
                }
            } catch (ex: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getAllTimeBoxOffice(year: Int? = null, page: Int = 1, forceRefresh: Boolean = false): List<BoxOfficeMovie> = withContext(Dispatchers.IO) {
        val cacheId = "box_office_all_time_${year ?: "all"}_p$page"
        val ttl = 24 * 60 * 60 * 1000L // 24 hours TTL

        if (!forceRefresh) {
            try {
                val cachedEntity = cacheDao.getHomeFeedEntity(cacheId)
                if (cachedEntity != null && (System.currentTimeMillis() - cachedEntity.updatedAt) < ttl) {
                    val cachedList = json.decodeFromString<List<BoxOfficeMovie>>(cachedEntity.data)
                    if (cachedList.isNotEmpty() && cachedList.any { it.formattedRevenue.isNotBlank() }) {
                        return@withContext cachedList
                    }
                }
            } catch (e: Exception) {
                // Ignore parse error, proceed to network
            }
        }

        try {
            val options = mutableMapOf<String, String>()
            options["sort_by"] = "revenue.desc"
            options["region"] = ""
            if (year != null) {
                options["primary_release_year"] = year.toString()
            }
            val movieRepo = movieRepositoryProvider.get()
            val movies = movieRepo.discoverMoviesWithParams(page = page, options = options)
            val baseRank = (page - 1) * 20

            val boxOfficeList = coroutineScope {
                movies.mapIndexed { index, movie ->
                    async {
                        val rank = baseRank + index + 1
                        val localMovie = favoriteDao.getById(movie.id, "movie")
                        val revenueFromLocal = localMovie?.revenue ?: 0L
                        val (finalMovie, finalRevenue) = if (revenueFromLocal > 0L) {
                            (localMovie ?: movie) to revenueFromLocal
                        } else {
                            try {
                                var details = movieRepo.fetchMovieDetails(movie.id, isTv = false)
                                if ((details.revenue ?: 0L) == 0L) {
                                    details = movieRepo.fetchMovieDetails(movie.id, isTv = false, forceRefresh = true)
                                }
                                val rev = details.revenue ?: 0L
                                val mapped = MovieMapper.mapResponseToMovie(details, "movie")
                                mapped to rev
                            } catch (e: Exception) {
                                movie to (movie.revenue ?: 0L)
                            }
                        }
                        val formattedRev = if (finalRevenue > 0L) {
                            BoxOfficeMovie.formatRevenue(finalRevenue)
                        } else {
                            ""
                        }
                        BoxOfficeMovie(
                            movie = finalMovie,
                            rank = rank,
                            revenue = finalRevenue,
                            formattedRevenue = formattedRev
                        )
                    }
                }.awaitAll()
            }

            if (boxOfficeList.isNotEmpty()) {
                try {
                    cacheDao.saveHomeFeed(
                        HomeFeedCacheEntity(
                            id = cacheId,
                            data = json.encodeToString(boxOfficeList),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    // Ignore cache write error
                }
            }

            boxOfficeList
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val cachedEntity = cacheDao.getHomeFeedEntity(cacheId)
                if (cachedEntity != null) {
                    json.decodeFromString<List<BoxOfficeMovie>>(cachedEntity.data)
                } else {
                    emptyList()
                }
            } catch (ex: Exception) {
                emptyList()
            }
        }
    }
}
