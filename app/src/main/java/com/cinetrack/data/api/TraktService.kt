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

    // OAuth
    @retrofit2.http.POST("oauth/token")
    suspend fun getAccessToken(
        @retrofit2.http.Body request: TraktTokenRequest
    ): TraktTokenResponse

    @retrofit2.http.POST("oauth/token")
    suspend fun refreshToken(
        @retrofit2.http.Body request: TraktRefreshTokenRequest
    ): TraktTokenResponse

    // Sync
    @retrofit2.http.GET("sync/last_activities")
    suspend fun getLastActivities(): TraktLastActivitiesResponse

    @retrofit2.http.POST("sync/history")
    suspend fun addToHistory(
        @retrofit2.http.Body request: TraktSyncRequest
    ): TraktSyncResponse

    @retrofit2.http.POST("sync/history/remove")
    suspend fun removeFromHistory(
        @retrofit2.http.Body request: TraktSyncRequest
    ): TraktSyncResponse

    @retrofit2.http.GET("sync/history")
    suspend fun getHistory(@retrofit2.http.Query("page") page: Int = 1, @retrofit2.http.Query("limit") limit: Int = 10000): retrofit2.Response<List<TraktHistoryItem>>

    @retrofit2.http.GET("sync/watchlist")
    suspend fun getWatchlist(@retrofit2.http.Query("limit") limit: Int = 10000): List<TraktWatchlistItem>
}

@Serializable
data class TraktHistoryItem(
    val id: Long = 0,
    val watched_at: String? = null,
    val action: String? = null,
    val type: String? = null, // "movie" or "show"
    val movie: TraktMovieItem? = null,
    val show: TraktShowItem? = null
)

@Serializable
data class TraktWatchlistItem(
    val rank: Int = 0,
    val id: Long = 0,
    val listed_at: String? = null,
    val type: String? = null, // "movie" or "show"
    val movie: TraktMovieItem? = null,
    val show: TraktShowItem? = null
)

@Serializable
data class TraktMovieItem(
    val title: String? = null,
    val year: Int? = null,
    val ids: TraktSyncIds? = null
)

@Serializable
data class TraktShowItem(
    val title: String? = null,
    val year: Int? = null,
    val ids: TraktSyncIds? = null
)

@Serializable
data class TraktTokenRequest(
    val code: String,
    val client_id: String,
    val client_secret: String,
    val redirect_uri: String,
    val grant_type: String = "authorization_code"
)

@Serializable
data class TraktRefreshTokenRequest(
    val refresh_token: String,
    val client_id: String,
    val client_secret: String,
    val redirect_uri: String,
    val grant_type: String = "refresh_token"
)

@Serializable
data class TraktTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String,
    val scope: String
)

@Serializable
data class TraktLastActivitiesResponse(
    val all: String,
    val movies: TraktActivityItem,
    val episodes: TraktActivityItem
)

@Serializable
data class TraktActivityItem(
    val watched_at: String? = null,
    val collected_at: String? = null,
    val rated_at: String? = null,
    val watchlisted_at: String? = null,
    val commented_at: String? = null,
    val paused_at: String? = null,
    val hidden_at: String? = null
)

@Serializable
data class TraktSyncRequest(
    val movies: List<TraktSyncMovie>? = null,
    val shows: List<TraktSyncShow>? = null,
    val episodes: List<TraktSyncEpisode>? = null
)

@Serializable
data class TraktSyncMovie(
    val ids: TraktSyncIds
)

@Serializable
data class TraktSyncShow(
    val ids: TraktSyncIds
)

@Serializable
data class TraktSyncEpisode(
    val ids: TraktSyncIds
)

@Serializable
data class TraktSyncIds(
    val tmdb: Long? = null,
    val imdb: String? = null,
    val trakt: Long? = null
)

@Serializable
data class TraktSyncResponse(
    val added: TraktSyncStats? = null,
    val existing: TraktSyncStats? = null,
    val deleted: TraktSyncStats? = null,
    val not_found: TraktSyncNotFound? = null
)

@Serializable
data class TraktSyncStats(
    val movies: Int = 0,
    val episodes: Int = 0,
    val shows: Int = 0,
    val seasons: Int = 0
)

@Serializable
data class TraktSyncNotFound(
    val movies: List<TraktSyncMovie> = emptyList(),
    val shows: List<TraktSyncShow> = emptyList(),
    val episodes: List<TraktSyncEpisode> = emptyList(),
    val seasons: List<TraktSyncShow> = emptyList() // Using Show model for ids just in case
)

@Serializable
data class TraktRatingResponse(
    val rating: Double? = null,
    val votes: Int = 0,
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
