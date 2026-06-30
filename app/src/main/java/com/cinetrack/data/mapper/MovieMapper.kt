package com.cinetrack.data.mapper

import com.cinetrack.data.Movie
import com.cinetrack.data.api.MovieDetailResponse

import java.util.Locale

object MovieMapper {
    fun mapResponseToMovie(response: MovieDetailResponse, type: String): Movie {
        val effectiveRuntime = if (type == "tv") {
            response.episodeRunTime?.firstOrNull() ?: response.runtime
        } else {
            response.runtime
        }
        
        val region = if (Locale.getDefault().language == "it") "IT" else "US"
        
        val regionalReleaseDate = if (type == "movie") {
            val regionalReleases = response.releaseDates?.results?.find { it.iso31661 == region }?.releaseDates
            regionalReleases?.find { it.type == 3 }?.releaseDate?.take(10)
                ?: regionalReleases?.firstOrNull()?.releaseDate?.take(10)
                ?: response.releaseDate
        } else {
            response.releaseDate
        }

        val topCast = response.credits?.cast?.take(15)?.distinctBy { it.id }?.map { 
            com.cinetrack.data.models.PersonData(id = it.id, name = it.name, profilePath = it.profilePath) 
        }

        val directors = response.credits?.crew?.filter { it.job == "Director" }?.distinctBy { it.id }?.map {
            com.cinetrack.data.models.PersonData(id = it.id, name = it.name, profilePath = it.profilePath)
        }
        val mainDirector = directors?.firstOrNull()

        return Movie(
            id = response.id,
            mediaType = type,
            imdbId = response.externalIds?.imdbId,
            title = response.title,
            name = response.name,
            posterPath = response.posterPath,
            backdropPath = response.backdropPath,
            voteAverage = response.voteAverage,
            voteCount = response.voteCount,
            overview = response.overview,
            releaseDate = regionalReleaseDate,
            firstAirDate = response.firstAirDate,
            runtime = effectiveRuntime,
            episodeRunTime = response.episodeRunTime,
            revenue = response.revenue,
            budget = response.budget,
            tagline = response.tagline,
            genres = response.genres,
            genreIds = response.genres?.map { it.id },
            numberOfSeasons = response.numberOfSeasons,
            numberOfEpisodes = response.numberOfEpisodes,
            streamingProviderIds = response.watchProviders?.results?.get("IT")?.flatrate?.map { it.providerId },
            seasons = response.seasons,
            nextEpisodeAirDate = response.nextEpisodeToAir?.airDate,
            nextEpisodeString = response.nextEpisodeToAir?.let { "S${it.seasonNumber.toString().padStart(2, '0')}E${it.episodeNumber.toString().padStart(2, '0')}" },
            topCastData = topCast,
            directorData = directors,
            directorId = mainDirector?.id,
            directorName = mainDirector?.name,
            directorProfilePath = mainDirector?.profilePath
        )
    }

    fun extractCertification(response: MovieDetailResponse, type: String): String? {
        return if (type == "movie") {
            val usReleases = response.releaseDates?.results?.find { it.iso31661 == "US" }
            usReleases?.releaseDates?.firstOrNull { it.certification.isNotEmpty() }?.certification
                ?: response.releaseDates?.results?.find { it.iso31661 == "IT" }?.releaseDates?.firstOrNull { it.certification.isNotEmpty() }?.certification
        } else {
            response.contentRatings?.results?.find { it.iso31661 == "US" }?.rating
                ?: response.contentRatings?.results?.find { it.iso31661 == "IT" }?.rating
        }
    }
}
