package com.cinetrack.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import retrofit2.http.GET
import retrofit2.http.Query

interface OmdbService {
    @GET("/")
    suspend fun getRatings(
        @Query("i") imdbId: String,
        @Query("apikey") apiKey: String
    ): OmdbResponse
}

@Serializable
data class OmdbResponse(
    @SerialName("Ratings") val ratings: List<OmdbRating>? = null,
    val imdbRating: String? = null,
    val imdbVotes: String? = null,
    val Metascore: String? = null,
    val Awards: String? = null
)

@Serializable
data class OmdbRating(
    @SerialName("Source") val source: String,
    @SerialName("Value") val value: String
)
