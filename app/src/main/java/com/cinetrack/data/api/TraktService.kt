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
    suspend fun getHistory(
        @retrofit2.http.Query("page") page: Int = 1,
        @retrofit2.http.Query("limit") limit: Int = 1000,
        @retrofit2.http.Query("extended") extended: String = "full"
    ): retrofit2.Response<List<TraktHistoryItem>>

    @retrofit2.http.GET("sync/watchlist")
    suspend fun getWatchlist(
        @retrofit2.http.Query("page") page: Int = 1,
        @retrofit2.http.Query("limit") limit: Int = 1000
    ): retrofit2.Response<List<TraktWatchlistItem>>

    @retrofit2.http.GET("sync/ratings/movies")
    suspend fun getUserMovieRatings(
        @retrofit2.http.Query("extended") extended: String = "full"
    ): List<TraktRatedItem>

    @retrofit2.http.GET("sync/ratings/shows")
    suspend fun getUserShowRatings(
        @retrofit2.http.Query("extended") extended: String = "full"
    ): List<TraktRatedItem>

    @retrofit2.http.POST("sync/ratings")
    suspend fun addRating(
        @retrofit2.http.Body request: TraktRatingRequest
    ): TraktSyncResponse

    @retrofit2.http.POST("sync/ratings/remove")
    suspend fun removeRating(
        @retrofit2.http.Body request: TraktRatingRequest
    ): TraktSyncResponse

    @retrofit2.http.POST("sync/watchlist")
    suspend fun addToWatchlist(
        @retrofit2.http.Body request: TraktSyncRequest
    ): TraktSyncResponse

    @retrofit2.http.POST("sync/watchlist/remove")
    suspend fun removeFromWatchlist(
        @retrofit2.http.Body request: TraktSyncRequest
    ): TraktSyncResponse

    @retrofit2.http.POST("sync/history")
    suspend fun addEpisodesToHistory(
        @retrofit2.http.Body request: TraktEpisodeHistoryRequest
    ): TraktSyncResponse

    @retrofit2.http.POST("sync/history/remove")
    suspend fun removeEpisodesFromHistory(
        @retrofit2.http.Body request: TraktEpisodeHistoryRequest
    ): TraktSyncResponse

    @retrofit2.http.GET("users/me/notes/movies")
    suspend fun getUserMovieNotes(): retrofit2.Response<List<TraktNoteItem>>

    @retrofit2.http.GET("users/me/notes/shows")
    suspend fun getUserShowNotes(): retrofit2.Response<List<TraktNoteItem>>

    @retrofit2.http.GET("users/me/lists")
    suspend fun getUserLists(): retrofit2.Response<List<TraktList>>

    @retrofit2.http.GET("users/me/lists/{id}/items")
    suspend fun getListItems(
        @retrofit2.http.Path("id") listId: Long
    ): retrofit2.Response<List<TraktListItem>>

    @retrofit2.http.POST("users/me/lists")
    suspend fun createList(@retrofit2.http.Body request: TraktListRequest): retrofit2.Response<TraktList>

    @retrofit2.http.PUT("users/me/lists/{id}")
    suspend fun updateList(
        @retrofit2.http.Path("id") listId: Long, 
        @retrofit2.http.Body request: TraktListRequest
    ): retrofit2.Response<TraktList>

    @retrofit2.http.DELETE("users/me/lists/{id}")
    suspend fun deleteList(@retrofit2.http.Path("id") listId: Long): retrofit2.Response<Unit>

    @retrofit2.http.POST("users/me/lists/{id}/items")
    suspend fun addListItems(
        @retrofit2.http.Path("id") listId: Long, 
        @retrofit2.http.Body request: TraktListItemsRequest
    ): retrofit2.Response<TraktSyncResponse>

    @retrofit2.http.POST("users/me/lists/{id}/items/remove")
    suspend fun removeListItems(
        @retrofit2.http.Path("id") listId: Long, 
        @retrofit2.http.Body request: TraktListItemsRequest
    ): retrofit2.Response<TraktSyncResponse>

    @retrofit2.http.GET("sync/watched/shows")
    suspend fun getWatchedShows(
        @retrofit2.http.Query("extended") extended: String? = null
    ): retrofit2.Response<List<TraktWatchedShow>>

    @retrofit2.http.GET("sync/watched/shows")
    suspend fun getWatchedShowsRaw(
        @retrofit2.http.Query("extended") extended: String? = null
    ): retrofit2.Response<okhttp3.ResponseBody>
}

@Serializable
data class TraktHistoryItem(
    val id: Long = 0,
    val watched_at: String? = null,
    val action: String? = null,
    val type: String? = null, // "movie", "episode"
    val movie: TraktMovieItem? = null,
    val show: TraktShowItem? = null,
    val episode: TraktEpisodeItem? = null
)

@Serializable
data class TraktEpisodeItem(
    val season: Int = 0,
    val number: Int = 0,
    val title: String? = null,
    val ids: TraktSyncIds? = null
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
    val shows: TraktActivityItem = TraktActivityItem(),
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

@Serializable
data class TraktRatedItem(
    val rated_at: String? = null,
    val rating: Int = 0,           // 1–10
    val type: String? = null,      // "movie" or "show"
    val movie: TraktMovieItem? = null,
    val show: TraktShowItem? = null
)

@Serializable
data class TraktNoteItem(
    val id: Long = 0,
    val notes: String? = null,
    val privacy: String? = null,
    val spoiler: Boolean = false,
    val attached_to: TraktNoteAttachment? = null,
    val movie: TraktMovieItem? = null,
    val show: TraktShowItem? = null
)

@Serializable
data class TraktNoteAttachment(
    val type: String? = null       // "movie" or "show"
)

// ── Rating push ───────────────────────────────────────────────────────────────

@Serializable
data class TraktRatingRequest(
    val rating: Int,
    val movies: List<TraktRatingMovie>? = null,
    val shows: List<TraktRatingShow>? = null
)

@Serializable
data class TraktRatingMovie(
    val ids: TraktSyncIds,
    val rated_at: String? = null
)

@Serializable
data class TraktRatingShow(
    val ids: TraktSyncIds,
    val rated_at: String? = null
)

// ── Episode history push ──────────────────────────────────────────────────────

@Serializable
data class TraktEpisodeHistoryRequest(
    val shows: List<TraktShowWithEpisodes>
)

@Serializable
data class TraktShowWithEpisodes(
    val ids: TraktSyncIds,
    val seasons: List<TraktSeasonEpisodes>
)

@Serializable
data class TraktSeasonEpisodes(
    val number: Int,
    val episodes: List<TraktEpisodeNumber>
)

@Serializable
data class TraktEpisodeNumber(
    val number: Int
)

// --- Custom Lists/Folders ──────────────────────────────────────────────────────────────

@Serializable
data class TraktList(
    val name: String? = null,
    val description: String? = null,
    val ids: TraktSyncIds? = null
)

@Serializable
data class TraktListItem(
    val id: Long = 0,
    val rank: Int = 0,
    val type: String? = null,
    val movie: TraktMovieItem? = null,
    val show: TraktShowItem? = null
)

@Serializable
data class TraktListRequest(
    val name: String,
    val description: String? = null,
    val privacy: String = "private" // Puoi mettere "public" se vuoi che gli altri le vedano
)

@Serializable
data class TraktListItemsRequest(
    val movies: List<TraktMovieItem>? = null,
    val shows: List<TraktShowItem>? = null
)

@Serializable
data class TraktWatchedShow(
    val plays: Int = 0,
    val last_watched_at: String? = null,
    val last_updated_at: String? = null,
    val show: TraktShowItem? = null,
    val seasons: List<TraktWatchedSeason>? = null 
)

@Serializable
data class TraktWatchedSeason(
    val number: Int = 0,
    val episodes: List<TraktWatchedEpisode>? = null 
)

@Serializable
data class TraktWatchedEpisode(
    val number: Int = 0,
    val plays: Int = 0,
    val last_watched_at: String? = null
)