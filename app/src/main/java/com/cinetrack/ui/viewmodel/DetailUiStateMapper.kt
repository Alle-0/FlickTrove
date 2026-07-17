package com.cinetrack.ui.viewmodel

import com.cinetrack.data.model.Movie
import com.cinetrack.data.api.CrewMember
import com.cinetrack.data.api.MovieDetailResponse
import com.cinetrack.data.api.TraktComment
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.mapper.MovieMapper
import com.cinetrack.data.model.Season
import com.cinetrack.domain.CalculateMatchScoreUseCase
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import javax.inject.Inject

class DetailUiStateMapper @Inject constructor(
    private val calculateMatchScoreUseCase: CalculateMatchScoreUseCase
) {
    fun mapToState(
        movieId: Long,
        mediaType: String,
        metadata: MovieDetailResponse?,
        externalRatings: ExternalRatings,
        loadingSeason: Boolean,
        seasonDetails: Map<Int, Season>,
        collectionMovies: List<Movie>,
        errorMsg: String?,
        traktComments: List<TraktComment>,
        localMovies: List<Movie>,
        folders: List<FolderEntity>
    ): DetailUiState {
        if (errorMsg != null && metadata == null) return DetailUiState.Error(errorMsg)
        if (metadata == null) return DetailUiState.Loading

        val movie = localMovies.find { it.id == movieId && it.mediaType == mediaType }
        val freshMovie = MovieMapper.mapResponseToMovie(metadata, mediaType)
        
        val effectiveMovie = movie?.copy(
            genres = freshMovie.genres,
            runtime = freshMovie.runtime,
            episodeRunTime = freshMovie.episodeRunTime,
            tagline = freshMovie.tagline,
            overview = freshMovie.overview,
            title = freshMovie.title,
            name = freshMovie.name,
            posterPath = freshMovie.posterPath,
            backdropPath = freshMovie.backdropPath,
            voteAverage = freshMovie.voteAverage,
            voteCount = freshMovie.voteCount,
            numberOfSeasons = freshMovie.numberOfSeasons,
            numberOfEpisodes = freshMovie.numberOfEpisodes,
            revenue = freshMovie.revenue,
            budget = freshMovie.budget,
            seasons = freshMovie.seasons,
            releaseDate = freshMovie.releaseDate,
            firstAirDate = freshMovie.firstAirDate,
            lastAirDate = freshMovie.lastAirDate,
            nextEpisodeAirDate = freshMovie.nextEpisodeAirDate,
            nextEpisodeString = freshMovie.nextEpisodeString,
            releaseYear = freshMovie.releaseYear,
            status = freshMovie.status,
            imdbId = freshMovie.imdbId,
            topCastData = freshMovie.topCastData ?: movie.topCastData,
            directorData = freshMovie.directorData ?: movie.directorData,
            directorId = freshMovie.directorId ?: movie.directorId,
            directorName = freshMovie.directorName ?: movie.directorName,
            directorProfilePath = freshMovie.directorProfilePath ?: movie.directorProfilePath,
            accentColor = freshMovie.accentColor ?: movie.accentColor
        ) ?: freshMovie

        val totalEpisodes = effectiveMovie.effectiveTotalEpisodes
        val watchedEpisodesCount = effectiveMovie.watchedEpisodes?.filterKeys { it != "0" }?.values?.sumOf { it.size } ?: 0
        val progress = if (mediaType == "tv" && totalEpisodes > 0) (watchedEpisodesCount.toFloat() / totalEpisodes).coerceIn(0f, 1f) else 0f
    
        val localMoviesMap = localMovies.associateBy { "${it.mediaType}_${it.id}" }

        fun Movie.hydrate(): Movie {
            val local = localMoviesMap["${this.mediaType}_${this.id}"]
            return if (local != null) {
                this.copy(
                    favorite = local.favorite,
                    watched = local.watched,
                    reminder = local.reminder,
                    progress = local.progress,
                    watchedEpisodes = local.watchedEpisodes,
                    watchedAt = local.watchedAt,
                    personalRating = local.personalRating,
                    personalNote = local.personalNote
                )
            } else this
        }

        val finalMovie = effectiveMovie
        val matchScore = calculateMatchScoreUseCase(finalMovie, localMovies)

        val currentLang = java.util.Locale.getDefault().language.lowercase()
        val watchRegion = if (currentLang == "it") "IT" else {
            val userCountry = java.util.Locale.getDefault().country
            if (userCountry.isNotBlank() && metadata.watchProviders?.results?.containsKey(userCountry) == true) userCountry else "US"
        }

        return DetailUiState.Success(
            movieEntry = finalMovie,
            details = metadata,
            isFavorite = finalMovie.favorite,
            isWatched = finalMovie.watched,
            watchState = when {
                finalMovie.watched -> WatchState.WATCHED
                finalMovie.favorite || finalMovie.reminder -> WatchState.BOOKMARKED
                else -> WatchState.NONE
            },
            watchedProgress = progress,
            matchPercentage = matchScore,
            directors = (metadata.credits?.crew?.filter { c: CrewMember -> c.job == "Director" } ?: emptyList()).toImmutableList(),
            cast = (metadata.credits?.cast?.take(15)?.distinctBy { it.id } ?: emptyList()).toImmutableList(),
            streamingProviders = (metadata.watchProviders?.results?.get(watchRegion)?.flatrate?.distinctBy { it.providerId } ?: emptyList()).toImmutableList(),
            buyRentProviders = ((metadata.watchProviders?.results?.get(watchRegion)?.buy ?: emptyList()) + 
                               (metadata.watchProviders?.results?.get(watchRegion)?.rent ?: emptyList())).distinctBy { it.providerId }.toImmutableList(),
            trailers = (metadata.videos?.results?.let { videos -> 
                videos.filter { v -> v.site == "YouTube" && v.type == "Trailer" }.map { v -> v.key }.distinct()
            } ?: emptyList()).toImmutableList(),
            videos = (metadata.videos?.results?.filter { v -> v.site == "YouTube" || v.site == "Vimeo" } ?: emptyList()).toImmutableList(),
            recommendations = (metadata.recommendations?.results?.take(10)?.map { rec ->
                localMovies.find { it.id == rec.id && it.mediaType == mediaType } ?: MovieMapper.mapResponseToMovie(
                    MovieDetailResponse(
                        id = rec.id, title = rec.title, name = rec.name,
                        posterPath = rec.posterPath, backdropPath = rec.backdropPath,
                        voteAverage = rec.voteAverage, releaseDate = rec.releaseDate, firstAirDate = rec.firstAirDate,
                        overview = rec.overview
                    ),
                    mediaType
                )
            } ?: emptyList()).map { it.hydrate() }.toImmutableList(),
            collectionMovies = collectionMovies.map { it.hydrate() }.toImmutableList(),
            externalRatings = externalRatings,
            loadingSeason = loadingSeason,
            seasonDetails = seasonDetails.toImmutableMap(),
            folders = folders.toImmutableList(),
            watchProviderLink = metadata.watchProviders?.results?.get(watchRegion)?.link,
            traktComments = traktComments.toImmutableList()
        )
    }
}
