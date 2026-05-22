package com.cinetrack.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface TraktService {
    @GET("{type}/{id}/ratings")
    suspend fun getRatings(
        @Path("type") type: String, // 'movies' or 'shows'
        @Path("id") id: String, // tmdb id
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String = "2"
    ): TraktRatingResponse
}

@Serializable
data class TraktRatingResponse(
    val rating: Double?,
    val votes: Int,
    val distribution: Map<String, Int>? = null
)
