package com.cinetrack.data.repository

import com.cinetrack.data.api.TvdbApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvdbRepository @Inject constructor(
    private val tvdbApi: TvdbApi
) {

    /**
     * Fetches character images from TVDB for a movie using a search fallback since TMDB
     * does not provide tvdb_id for movies.
     * Returns a map of CharacterName to ImageUrl.
     */
    suspend fun getMovieCharacterImagesMap(title: String, year: String): Map<String, String> {
        return try {
            // 1. Search for the movie by title + year
            val searchResponse = tvdbApi.search(query = title, year = year, type = "movie")
            // Fallback: if year-filtered search returns nothing, retry without year
            val tvdbId = searchResponse.data.firstOrNull()?.tvdb_id
                ?: tvdbApi.search(query = title, type = "movie").data.firstOrNull()?.tvdb_id
                ?: return emptyMap()

            // 2. Fetch extended data
            val extendedResponse = tvdbApi.getMovieExtended(id = tvdbId)
            
            // 3. Map characters (by both character name and actor name for better matching)
            val characters = extendedResponse.data?.characters ?: emptyList()
            val map = mutableMapOf<String, String>()
            characters.forEach { character ->
                val image = character.image?.takeIf { it.isNotBlank() && it.startsWith("http") } ?: return@forEach
                val charName = character.name?.lowercase()?.trim()
                val personName = character.personName?.lowercase()?.trim()
                if (charName != null) map[charName] = image
                if (personName != null) map[personName] = image
            }
            map
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    /**
     * Fetches character images from TVDB for a series using its known tvdb_id.
     * Returns a map of CharacterName to ImageUrl.
     */
    suspend fun getSeriesCharacterImagesMap(tvdbId: String): Map<String, String> {
        return try {
            val extendedResponse = tvdbApi.getSeriesExtended(id = tvdbId)
            
            val characters = extendedResponse.data?.characters ?: emptyList()
            val map = mutableMapOf<String, String>()
            characters.forEach { character ->
                val image = character.image?.takeIf { it.isNotBlank() && it.startsWith("http") } ?: return@forEach
                val charName = character.name?.lowercase()?.trim()
                val personName = character.personName?.lowercase()?.trim()
                if (charName != null) map[charName] = image
                if (personName != null) map[personName] = image
            }
            map
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    /**
     * Fetches series seasons and episodes from TVDB and maps them to internal TMDB-like models.
     * Supports Absolute Order for Anime and Aired Order for standard series.
     */
    suspend fun getSeriesSeasons(tvdbId: String, useAbsoluteOrder: Boolean): List<com.cinetrack.data.model.Season> {
        return try {
            val allEpisodes = mutableListOf<com.cinetrack.data.api.TvdbEpisode>()
            var currentPage = 0
            var hasNextPage = true
            
            while (hasNextPage) {
                val response = if (useAbsoluteOrder) {
                    tvdbApi.getSeriesEpisodesAbsolute(tvdbId, currentPage)
                } else {
                    tvdbApi.getSeriesEpisodes(tvdbId, currentPage)
                }
                
                response.data?.episodes?.let { allEpisodes.addAll(it) }
                
                val nextLink = response.links?.next
                if (!nextLink.isNullOrEmpty()) {
                    currentPage++
                } else {
                    hasNextPage = false
                }
            }

            val extendedResponse = tvdbApi.getSeriesExtended(tvdbId, "episodes")
            val tvdbSeasons = extendedResponse.data?.seasons ?: emptyList()
            
            mapTvdbToInternal(tvdbSeasons, allEpisodes, useAbsoluteOrder)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun mapTvdbToInternal(
        tvdbSeasons: List<com.cinetrack.data.api.TvdbSeason>, 
        tvdbEpisodes: List<com.cinetrack.data.api.TvdbEpisode>,
        useAbsoluteOrder: Boolean
    ): List<com.cinetrack.data.model.Season> {
        val episodesBySeason = tvdbEpisodes.groupBy { 
            if (useAbsoluteOrder) 1 else (it.seasonNumber ?: 1) 
        }

        if (useAbsoluteOrder) {
            val allMappedEpisodes = tvdbEpisodes
                .filter { (it.absoluteNumber ?: 0) > 0 || (it.number ?: 0) > 0 }
                .sortedBy { it.absoluteNumber ?: it.number }
                .map { tvdbEp ->
                    com.cinetrack.data.model.Episode(
                        id = tvdbEp.id,
                        name = tvdbEp.name ?: "",
                        episodeNumber = tvdbEp.absoluteNumber ?: tvdbEp.number ?: 0,
                        overview = tvdbEp.overview,
                        stillPath = tvdbEp.image,
                        airDate = tvdbEp.aired,
                        voteAverage = null,
                        seasonNumber = 1
                    )
                }

            return listOf(
                com.cinetrack.data.model.Season(
                    id = 1L,
                    name = "Stagione 1",
                    seasonNumber = 1,
                    episodeCount = allMappedEpisodes.size,
                    posterPath = tvdbSeasons.firstOrNull { it.number == 1 }?.image ?: tvdbSeasons.firstOrNull()?.image,
                    airDate = null,
                    overview = null,
                    episodes = allMappedEpisodes
                )
            )
        } else {
            return tvdbSeasons
                .map { tvdbSeason ->
                    val seasonNumber = tvdbSeason.number ?: 0
                    val seasonEps = episodesBySeason[seasonNumber] ?: emptyList()
                    val mappedEpisodes = seasonEps
                        .sortedBy { it.number }
                        .map { tvdbEp ->
                            com.cinetrack.data.model.Episode(
                                id = tvdbEp.id,
                                name = tvdbEp.name ?: "",
                                episodeNumber = tvdbEp.number ?: 0,
                                overview = tvdbEp.overview,
                                stillPath = tvdbEp.image,
                                airDate = tvdbEp.aired,
                                voteAverage = null,
                                seasonNumber = seasonNumber
                            )
                        }

                    com.cinetrack.data.model.Season(
                        id = tvdbSeason.id,
                        name = tvdbSeason.name ?: "Stagione $seasonNumber",
                        seasonNumber = seasonNumber,
                        episodeCount = mappedEpisodes.size,
                        posterPath = tvdbSeason.image,
                        airDate = null,
                        overview = null,
                        episodes = mappedEpisodes
                    )
                }
                .filter { (it.episodeCount ?: 0) > 0 || it.seasonNumber == 0 }
                .sortedBy { it.seasonNumber }
        }
    }
}
