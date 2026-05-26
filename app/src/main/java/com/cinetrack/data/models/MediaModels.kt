package com.cinetrack.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Episode(
    val id: Long = 0L,
    val name: String = "",
    @SerialName("episode_number") val episodeNumber: Int = 0,
    val overview: String? = null,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("season_number") val seasonNumber: Int = 0
)

@Serializable
data class Season(
    val id: Long = 0L,
    val name: String = "",
    @SerialName("season_number") val seasonNumber: Int = 0,
    @SerialName("episode_count") val episodeCount: Int? = 0,
    @SerialName("poster_path") val posterPath: String? = null,
    val overview: String? = null,
    val episodes: List<Episode>? = null
)

@Serializable
data class PersonData(
    val id: Long = 0L,
    val name: String = "",
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
data class Folder(
    var id: String = "",
    var name: String = "",
    var icon: String? = null,
    var color: String? = null,
    var description: String? = null,
    @SerialName("item_ids") var itemIds: List<String> = emptyList(),
    @SerialName("created_at") var createdAt: String? = null,
    @SerialName("updated_at") var updatedAt: String? = null
)

@Serializable
data class SortConfig(
    val sortType: String = "created_at", // 'created_at', 'vote_average', 'release_date', 'title', 'personal_rating'
    val sortDirection: String = "desc", // 'asc', 'desc'
    val selectedGenres: List<Long> = emptyList(),
    val selectedProviders: List<Long> = emptyList(),
    val selectedDecades: List<String> = emptyList(),
    val selectedKeywords: List<Long> = emptyList()
)

@Serializable
data class DiscoveryFilters(
    val selectedGenres: List<Long> = emptyList(),
    val selectedProviders: List<Long> = emptyList(),
    val selectedDecades: List<String> = emptyList(),
    val sortBy: String = "popularity.desc"
)

@Serializable
data class UserPreferences(
    val homeSort: SortConfig = SortConfig(),
    val vistiSort: SortConfig = SortConfig(sortType = "watched_at"),
    val discoveryFilters: DiscoveryFilters = DiscoveryFilters(),
    val gridColumns: Int = 3,
    val showLayoutToggle: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val showFolderBookmarks: Boolean = true,
    val showBadges: Boolean = true,
    val disabledBadges: Set<String> = emptySet(),
    val vibrationEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L
)

