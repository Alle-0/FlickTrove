package com.cinetrack.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Authentication Models
@Serializable
data class SimklTokenRequest(
    val code: String,
    val client_id: String,
    val client_secret: String,
    val redirect_uri: String,
    val grant_type: String = "authorization_code"
)

@Serializable
data class SimklTokenResponse(
    val access_token: String,
    val token_type: String? = null,
    val scope: String? = null
)

// Sync Models
@Serializable
data class SimklSyncResponse(
    val added: SimklSyncResult? = null,
    val updated: SimklSyncResult? = null,
    val deleted: SimklSyncResult? = null
)

@Serializable
data class SimklSyncResult(
    val movies: Int = 0,
    val shows: Int = 0,
    val anime: Int = 0
)

// Media Item Models
@Serializable
data class SimklMediaItem(
    val title: String? = null,
    val year: Int? = null,
    val ids: SimklIds
)

@Serializable
data class SimklIds(
    val simkl: Long? = null,
    val simkl_id: Long? = null,
    val imdb: String? = null,
    val tmdb: String? = null,
    val tvdb: String? = null
)

@Serializable
data class SimklSyncHistoryRequest(
    val movies: List<SimklHistoryItem>? = null,
    val shows: List<SimklHistoryItem>? = null,
    val anime: List<SimklHistoryItem>? = null
)

@Serializable
data class SimklHistoryItem(
    val ids: SimklIds,
    val watched_at: String? = null,
    val episodes: List<SimklEpisode>? = null,
    val seasons: List<SimklSeason>? = null
)

@Serializable
data class SimklSeason(
    val season: Int? = null,
    val number: Int? = null,
    val episodes: List<SimklSeasonEpisode>
)

@Serializable
data class SimklSeasonEpisode(
    val episode: Int? = null,
    val number: Int? = null
)

@Serializable
data class SimklEpisode(
    val season: Int,
    val episode: Int
)

@Serializable
data class SimklSyncWatchlistRequest(
    val movies: List<SimklMediaItem>? = null,
    val shows: List<SimklMediaItem>? = null,
    val anime: List<SimklMediaItem>? = null
)

@Serializable
data class SimklAddToListRequest(
    val to: String, // e.g. "dropped", "watching", "plantowatch"
    val movies: List<SimklMediaItem>? = null,
    val shows: List<SimklMediaItem>? = null,
    val anime: List<SimklMediaItem>? = null
)

@Serializable
data class SimklActivitiesResponse(
    val all: String? = null,
    val tv_shows: SimklActivityDates? = null,
    val anime: SimklActivityDates? = null,
    val movies: SimklActivityDates? = null
)

@Serializable
data class SimklActivityDates(
    val all: String? = null,
    val rated_at: String? = null,
    val watchlisted_at: String? = null,
    val watched_at: String? = null
)

@Serializable
data class SimklSyncItemResponse(
    val last_watched_at: String? = null,
    val status: String? = null,
    val user_rating: Int? = null,
    val movie: SimklMediaItem? = null,
    val show: SimklMediaItem? = null,
    val anime: SimklMediaItem? = null
)

@Serializable
data class SimklAllItemsResponse(
    val movies: List<SimklSyncItemResponse>? = null,
    val shows: List<SimklSyncItemResponse>? = null,
    val anime: List<SimklSyncItemResponse>? = null
)

// AUTH V2 Models
@Serializable
data class SimklTokenRequestV2(
    val grant_type: String, // "authorization_code" or "refresh_token"
    val client_id: String,
    val code: String? = null,
    val redirect_uri: String? = null,
    val code_verifier: String? = null,
    val refresh_token: String? = null
)

@Serializable
data class SimklTokenResponseV2(
    val access_token: String,
    val token_type: String? = null,
    val expires_in: Long? = null,
    val refresh_token: String? = null,
    val scope: String? = null
)

// User Settings
@Serializable
data class SimklUserSettingsResponse(
    val user: SimklUser? = null,
    val account: SimklAccount? = null
)

@Serializable
data class SimklUser(
    val name: String? = null,
    val avatar: String? = null
)

@Serializable
data class SimklAccount(
    val id: Long? = null,
    val timezone: String? = null,
    val type: String? = null // "free", "pro", "vip"
)

// Custom Lists Models (Resilient to free/premium_only responses)
@Serializable
data class SimklUserListsResponse(
    val error: String? = null,
    val message: String? = null,
    val lists: List<SimklCustomListSummary>? = null
)

@Serializable
data class SimklCustomListSummary(
    val id: Long,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    val media_type: String? = null,
    val privacy: String? = null
)

@Serializable
data class SimklListDetailResponse(
    val error: String? = null,
    val message: String? = null,
    val id: Long? = null,
    val name: String? = null,
    val description: String? = null,
    val media_type: String? = null,
    val items: List<SimklCustomListItem>? = null
)

@Serializable
data class SimklCustomListItem(
    val title: String? = null,
    val year: Int? = null,
    val type: String? = null, // "movie", "show", "anime"
    val ids: SimklItemIds? = null,
    val poster: String? = null,
    val position: Int? = null
)

@Serializable
data class SimklItemIds(
    val simkl: Long? = null,
    val simkl_id: Long? = null,
    val imdb: String? = null,
    @SerialName("tmdb")
    val tmdbElement: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("tvdb")
    val tvdbElement: kotlinx.serialization.json.JsonElement? = null,
    val slug: String? = null
) {
    val tmdb: Long?
        get() = when (val elem = tmdbElement) {
            is kotlinx.serialization.json.JsonPrimitive -> elem.content.toLongOrNull()
            else -> null
        }

    val tvdb: Long?
        get() = when (val elem = tvdbElement) {
            is kotlinx.serialization.json.JsonPrimitive -> elem.content.toLongOrNull()
            else -> null
        }
}

