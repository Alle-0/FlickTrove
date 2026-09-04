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
    val episodes: List<SimklEpisode>? = null
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
