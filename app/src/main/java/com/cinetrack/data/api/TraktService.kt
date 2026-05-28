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

    @GET("{type}/{id}/comments/{sort}")
    suspend fun getComments(
        @Path("type") type: String, // 'movies' or 'shows'
        @Path("id") id: String, // imdb id
        @Path("sort") sort: String = "likes",
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String = "2"
    ): List<TraktComment>
}

@Serializable
data class TraktRatingResponse(
    val rating: Double?,
    val votes: Int,
    val distribution: Map<String, Int>? = null
)

@Serializable
data class TraktComment(
    val id: Long,
    val parent_id: Long? = null,
    val created_at: String? = null,
    val comment: String? = null,
    val spoiler: Boolean = false,
    val review: Boolean = false,
    val replies: Int = 0,
    val likes: Int = 0,
    val user: TraktUser? = null
)

@Serializable
data class TraktUser(
    val username: String? = null,
    val private: Boolean = false,
    val name: String? = null,
    val vip: Boolean = false,
    val vip_ep: Boolean = false
)
