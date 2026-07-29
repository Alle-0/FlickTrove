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
            // 1. Search for the movie
            val searchResponse = tvdbApi.search(query = title, year = year, type = "movie")
            val tvdbId = searchResponse.data.firstOrNull()?.tvdb_id ?: return emptyMap()

            // 2. Fetch extended data
            val extendedResponse = tvdbApi.getMovieExtended(id = tvdbId)
            
            // 3. Map characters
            val characters = extendedResponse.data?.characters ?: emptyList()
            characters.mapNotNull { character ->
                val charName = character.name?.lowercase()?.trim() ?: return@mapNotNull null
                val image = character.image ?: return@mapNotNull null
                charName to image
            }.toMap()
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
            characters.mapNotNull { character ->
                val charName = character.name?.lowercase()?.trim() ?: return@mapNotNull null
                val image = character.image ?: return@mapNotNull null
                charName to image
            }.toMap()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }
}
