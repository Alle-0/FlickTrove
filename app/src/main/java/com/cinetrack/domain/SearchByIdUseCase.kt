package com.cinetrack.domain

import com.cinetrack.data.api.MovieDetailResponse
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.api.TMDBService
import com.cinetrack.data.model.Movie
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Use case dedicated to resolving media by explicit ID:
 * - IMDb IDs (e.g., "tt0111161")
 * - TMDB IDs with prefix (e.g., "#530915", "id:530915", "tmdb:530915")
 *
 * Keeps ViewModels clean and avoids monolithic "god files".
 */
class SearchByIdUseCase @Inject constructor(
    private val tmdbService: TMDBService
) {
    companion object {
        private val IMDB_REGEX = Regex("""^(?:tt\d{7,10})$""", RegexOption.IGNORE_CASE)
        private val TMDB_ID_REGEX = Regex("""^(?:#|id:|id\s+|tmdb:)\s*(\d+)$""", RegexOption.IGNORE_CASE)
        private val TVDB_ID_REGEX = Regex("""^(?:tvdb[:\s-]\s*)(\d+)$""", RegexOption.IGNORE_CASE)
    }

    fun isIdQuery(query: String): Boolean {
        val trimmed = query.trim()
        return IMDB_REGEX.matches(trimmed) || TMDB_ID_REGEX.matches(trimmed) || TVDB_ID_REGEX.matches(trimmed)
    }

    suspend operator fun invoke(
        query: String,
        category: String,
        isOnline: Boolean,
        localMovies: List<Movie>
    ): List<TMDBSearchResult>? {
        val trimmed = query.trim()
        val imdbMatch = IMDB_REGEX.matchEntire(trimmed)
        val tmdbMatch = TMDB_ID_REGEX.matchEntire(trimmed)
        val tvdbMatch = TVDB_ID_REGEX.matchEntire(trimmed)

        if (imdbMatch == null && tmdbMatch == null && tvdbMatch == null) return null

        if (tvdbMatch != null) {
            if (!isOnline) return emptyList()
            val tvdbId = tvdbMatch.groupValues[1]
            return try {
                val findResp = tmdbService.findByExternalId(tvdbId, "tvdb_id")
                val tvShows = findResp.tvResults ?: emptyList()
                val movies = findResp.movieResults ?: emptyList()
                when (category) {
                    "movie" -> movies
                    "tv" -> tvShows
                    else -> tvShows + movies
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emptyList()
            }
        }

        if (imdbMatch != null) {
            if (!isOnline) return emptyList()
            return try {
                val cleanImdbId = trimmed.lowercase()
                val findResp = tmdbService.findByExternalId(cleanImdbId, "imdb_id")
                val movies = findResp.movieResults ?: emptyList()
                val tvShows = findResp.tvResults ?: emptyList()
                when (category) {
                    "movie" -> movies
                    "tv" -> tvShows
                    else -> movies + tvShows
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emptyList()
            }
        }

        if (tmdbMatch != null) {
            val tmdbId = tmdbMatch.groupValues[1].toLongOrNull() ?: return emptyList()
            return resolveByTmdbId(tmdbId, category, isOnline, localMovies)
        }

        return emptyList()
    }

    suspend fun resolveByTmdbId(
        tmdbId: Long,
        category: String,
        isOnline: Boolean,
        localMovies: List<Movie>
    ): List<TMDBSearchResult> {
        if (!isOnline) {
            val local = localMovies.find { it.id == tmdbId }
            return if (local != null) {
                when (category) {
                    "movie" -> if (local.mediaType != "tv") listOf(local.toMovieResult()) else emptyList()
                    "tv" -> if (local.mediaType == "tv") listOf(local.toTvResult()) else emptyList()
                    else -> listOf(if (local.mediaType == "tv") local.toTvResult() else local.toMovieResult())
                }
            } else emptyList()
        }

        return try {
            val directResult: TMDBSearchResult? = when (category) {
                "movie" -> {
                    try {
                        tmdbService.getMovieDetails(tmdbId).toMovieResult()
                    } catch (_: Exception) {
                        null
                    }
                }
                "tv" -> {
                    try {
                        tmdbService.getTVDetails(tmdbId).toTvResult()
                    } catch (_: Exception) {
                        null
                    }
                }
                "person" -> {
                    try {
                        val person = tmdbService.getPersonDetails(tmdbId)
                        TMDBSearchResult.PersonResult(
                            id = person.id,
                            name = person.name ?: "",
                            profilePath = person.profilePath,
                            knownForDepartment = person.knownForDepartment
                        )
                    } catch (_: Exception) { null }
                }
                "collection" -> {
                    try {
                        val col = tmdbService.getCollectionDetails(tmdbId)
                        TMDBSearchResult.CollectionResult(
                            id = col.id,
                            name = col.name,
                            posterPath = col.posterPath,
                            backdropPath = col.backdropPath,
                            overview = col.overview,
                            partsPosterPaths = col.parts.mapNotNull { it.posterPath }.take(4),
                            partsCount = col.parts.size,
                            partsIds = col.parts.map { it.id }
                        )
                    } catch (_: Exception) { null }
                }
                else -> {
                    try {
                        tmdbService.getMovieDetails(tmdbId).toMovieResult()
                    } catch (_: Exception) {
                        try { tmdbService.getTVDetails(tmdbId).toTvResult() } catch (_: Exception) { null }
                    }
                }
            }
            directResult?.let { listOf(it) } ?: emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    private fun MovieDetailResponse.toMovieResult() = TMDBSearchResult.MovieResult(
        id = id,
        title = title ?: originalTitle,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        genreIds = genres?.map { it.id } ?: emptyList(),
        overview = overview
    )

    private fun MovieDetailResponse.toTvResult() = TMDBSearchResult.TvResult(
        id = id,
        name = name ?: originalName,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        firstAirDate = firstAirDate,
        genreIds = genres?.map { it.id } ?: emptyList(),
        overview = overview
    )

    private fun Movie.toMovieResult() = TMDBSearchResult.MovieResult(
        id = id,
        title = title,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        genreIds = genreIds?.map { it.toLong() } ?: emptyList(),
        overview = overview
    )

    private fun Movie.toTvResult() = TMDBSearchResult.TvResult(
        id = id,
        name = name,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        firstAirDate = firstAirDate,
        genreIds = genreIds?.map { it.toLong() } ?: emptyList(),
        overview = overview
    )
}
