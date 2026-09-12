package com.cinetrack.data.mapper

import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.StudioData
import com.cinetrack.data.api.ExternalIds
import com.cinetrack.data.api.MovieDetailResponse

import java.util.Locale

object MovieMapper {
    fun mapResponseToMovie(response: MovieDetailResponse, type: String): Movie {
        val effectiveRuntime = if (type == "tv") {
            response.episodeRunTime?.firstOrNull() ?: response.runtime
        } else {
            response.runtime
        }
        
        // Get the device's actual country code (e.g. IT, FR, DE, GB). 
        // If the country is somehow missing from the locale, fallback to a sensible default based on language, or US.
        val locale = Locale.getDefault()
        val region = locale.country.takeIf { it.isNotBlank() }
            ?: if (locale.language == "it") "IT" else "US"
        
        val regionalReleaseDate = if (type == "movie") {
            val regionalReleases = response.releaseDates?.results?.find { it.iso31661 == region }?.releaseDates
            regionalReleases?.find { it.type == 3 }?.releaseDate?.take(10)
                ?: regionalReleases?.firstOrNull()?.releaseDate?.take(10)
                ?: response.releaseDate
        } else {
            response.releaseDate
        }

        val topCast = response.credits?.cast?.distinctBy { it.id }?.map { 
            com.cinetrack.data.model.PersonData(id = it.id, name = it.name, profilePath = it.profilePath) 
        }

        val directors = response.credits?.crew?.filter { it.job == "Director" }?.distinctBy { it.id }?.map {
            com.cinetrack.data.model.PersonData(id = it.id, name = it.name, profilePath = it.profilePath)
        }
        val mainDirector = directors?.firstOrNull()

        return Movie(
            id = response.id,
            mediaType = type,
            imdbId = response.externalIds?.imdbId,
            title = response.title,
            name = response.name,
            posterPath = response.posterPath ?: response.images?.posters?.firstOrNull()?.filePath,
            backdropPath = response.backdropPath ?: response.images?.backdrops?.firstOrNull()?.filePath,
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
            streamingProviderIds = response.watchProviders?.results?.get(region)?.flatrate?.map { it.providerId },
            seasons = response.seasons,
            lastAirDate = response.lastEpisodeToAir?.airDate ?: response.lastAirDate,
            nextEpisodeAirDate = response.nextEpisodeToAir?.airDate,
            nextEpisodeString = response.nextEpisodeToAir?.let { "S${it.seasonNumber.toString().padStart(2, '0')}E${it.episodeNumber.toString().padStart(2, '0')}" },
            topCastData = topCast,
            directorData = directors,
            directorId = mainDirector?.id,
            directorName = mainDirector?.name,
            originCountry = response.originCountry ?: response.productionCountries?.mapNotNull { it.iso31661 },
            productionCompanies = if (type == "tv") {
                val networks = response.networks?.map { StudioData(it.id, it.name, it.logoPath, it.originCountry) } ?: emptyList()
                val prodCompanies = response.productionCompanies?.map { StudioData(it.id, it.name, it.logoPath, it.originCountry) } ?: emptyList()
                (networks + prodCompanies).distinctBy { it.name.trim().lowercase() }.ifEmpty { null }
            } else {
                response.productionCompanies?.map { StudioData(it.id, it.name, it.logoPath, it.originCountry) }?.distinctBy { it.name.trim().lowercase() }?.ifEmpty { null }
            }
        )
    }

    fun mapMovieToResponse(movie: Movie): MovieDetailResponse {
        val castList = movie.topCastData?.map { person ->
            com.cinetrack.data.api.CastMember(
                id = person.id,
                name = person.name,
                character = null,
                profilePath = person.profilePath
            )
        } ?: emptyList()

        val crewList = if (!movie.directorData.isNullOrEmpty()) {
            movie.directorData!!.map { person ->
                com.cinetrack.data.api.CrewMember(
                    id = person.id,
                    name = person.name,
                    job = "Director",
                    profilePath = person.profilePath
                )
            }
        } else if (movie.directorId != null && !movie.directorName.isNullOrEmpty()) {
            listOf(
                com.cinetrack.data.api.CrewMember(
                    id = movie.directorId!!,
                    name = movie.directorName!!,
                    job = "Director",
                    profilePath = movie.directorProfilePath
                )
            )
        } else {
            emptyList()
        }

        val creditsResponse = if (castList.isNotEmpty() || crewList.isNotEmpty()) {
            com.cinetrack.data.api.CreditsResponse(cast = castList, crew = crewList)
        } else null

        return MovieDetailResponse(
            id = movie.id,
            title = movie.title,
            name = movie.name,
            posterPath = movie.posterPath,
            backdropPath = movie.backdropPath,
            voteAverage = movie.voteAverage,
            voteCount = movie.voteCount,
            overview = movie.overview,
            releaseDate = movie.releaseDate,
            firstAirDate = movie.firstAirDate,
            runtime = movie.runtime,
            episodeRunTime = movie.episodeRunTime,
            genres = movie.genres,
            status = movie.status,
            tagline = movie.tagline,
            revenue = movie.revenue,
            budget = movie.budget,
            numberOfSeasons = movie.numberOfSeasons,
            numberOfEpisodes = movie.numberOfEpisodes,
            seasons = movie.seasons,
            credits = creditsResponse,
            externalIds = ExternalIds(imdbId = movie.imdbId),
            productionCompanies = movie.productionCompanies?.map {
                com.cinetrack.data.api.ProductionCompany(
                    id = it.id,
                    name = it.name,
                    logoPath = it.logoPath,
                    originCountry = it.originCountry
                )
            }
        )
    }

    val SUPPORTED_RATING_COUNTRIES = setOf("IT", "DE", "FR", "ES", "BR", "RU", "IN", "GB", "US")

    fun extractCertificationWithCountry(
        response: MovieDetailResponse, 
        type: String, 
        preferredCountry: String = "IT"
    ): Pair<String, String>? {
        val targetCountry = if (preferredCountry.uppercase() in SUPPORTED_RATING_COUNTRIES) preferredCountry.uppercase() else "US"
        return if (type == "movie") {
            // 1. Try preferred target country
            val targetCert = response.releaseDates?.results
                ?.find { it.iso31661.equals(targetCountry, ignoreCase = true) }
                ?.releaseDates?.firstOrNull { it.certification.isNotBlank() }?.certification?.trim()
            if (!targetCert.isNullOrBlank()) {
                Pair(targetCountry, targetCert)
            } else {
                // 2. Try US fallback
                val usCert = response.releaseDates?.results
                    ?.find { it.iso31661.equals("US", ignoreCase = true) }
                    ?.releaseDates?.firstOrNull { it.certification.isNotBlank() }?.certification?.trim()
                if (!usCert.isNullOrBlank()) {
                    Pair("US", usCert)
                } else {
                    // 3. Fallback to first available country (preferring supported)
                    val firstSupported = response.releaseDates?.results?.firstOrNull { r ->
                        r.iso31661.uppercase() in SUPPORTED_RATING_COUNTRIES && r.releaseDates.any { it.certification.isNotBlank() }
                    }
                    val firstItem = firstSupported ?: response.releaseDates?.results?.firstOrNull { r ->
                        r.releaseDates.any { it.certification.isNotBlank() }
                    }
                    val firstCert = firstItem?.releaseDates?.firstOrNull { it.certification.isNotBlank() }?.certification?.trim()
                    if (!firstCert.isNullOrBlank()) {
                        Pair(firstItem.iso31661, firstCert)
                    } else null
                }
            }
        } else {
            // TV Shows
            // 1. Try preferred target country
            val targetRating = response.contentRatings?.results
                ?.find { it.iso31661.equals(targetCountry, ignoreCase = true) }
                ?.rating?.trim()
            if (!targetRating.isNullOrBlank()) {
                Pair(targetCountry, targetRating)
            } else {
                // 2. Try US fallback
                val usRating = response.contentRatings?.results
                    ?.find { it.iso31661.equals("US", ignoreCase = true) }
                    ?.rating?.trim()
                if (!usRating.isNullOrBlank()) {
                    Pair("US", usRating)
                } else {
                    // 3. Fallback to first available country (preferring supported)
                    val firstSupported = response.contentRatings?.results?.firstOrNull { 
                        it.iso31661.uppercase() in SUPPORTED_RATING_COUNTRIES && it.rating.isNotBlank() 
                    }
                    val firstRating = firstSupported ?: response.contentRatings?.results?.firstOrNull { it.rating.isNotBlank() }
                    if (firstRating != null) {
                        Pair(firstRating.iso31661, firstRating.rating.trim())
                    } else null
                }
            }
        }
    }

    fun extractCertification(response: MovieDetailResponse, type: String): String? {
        return extractCertificationWithCountry(response, type)?.second
    }
}
