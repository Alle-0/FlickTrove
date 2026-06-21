package com.cinetrack.data.models

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@Stable
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
@Stable
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
@Stable
data class PersonData(
    val id: Long = 0L,
    val name: String = "",
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
@Stable
data class Folder(
    var id: String = "",
    var name: String = "",
    var icon: String? = null,
    var color: String? = null,
    var description: String? = null,
    @SerialName("item_ids") var itemIds: List<String> = emptyList(),
    @SerialName("created_at") var createdAt: String? = null,
    @SerialName("updated_at") var updatedAt: String? = null,
    @SerialName("client_updated_at") var clientUpdatedAt: Long = 0L
)

@Serializable
@Stable
data class SortConfig(
    val sortType: String = "created_at", // 'created_at', 'vote_average', 'release_date', 'title', 'personal_rating'
    val sortDirection: String = "desc", // 'asc', 'desc'
    val selectedGenres: List<Long> = emptyList(),
    val selectedProviders: List<Long> = emptyList(),
    val selectedDecades: List<String> = emptyList(),
    val selectedKeywords: List<Long> = emptyList()
)

@Serializable
@Stable
data class DiscoveryFilters(
    val selectedGenres: List<Long> = emptyList(),
    val selectedProviders: List<Long> = emptyList(),
    val selectedDecades: List<String> = emptyList(),
    val sortBy: String = "popularity.desc"
)

@Serializable
@Stable
data class UserPreferences(
    val homeSort: SortConfig = SortConfig(),
    val vistiSort: SortConfig = SortConfig(sortType = "watched_at"),
    val discoveryFilters: DiscoveryFilters = DiscoveryFilters(),
    val gridColumns: Int = 3,
    val showLayoutToggle: Boolean = false,
    val isSearchSuggestionsExpanded: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val showFolderBookmarks: Boolean = true,
    val showBadges: Boolean = true,
    val disabledBadges: Set<String> = emptySet(),
    val vibrationEnabled: Boolean = true,
    val accentColor: String = "Teal",
    val appTheme: String = "System", // System, Light, Dark, Amoled
    val contentLanguage: String = "it-IT",
    val advancedVisualEffectsEnabled: Boolean = true,
    val dynamicAppIconEnabled: Boolean = false,
    val showSplitReleasesHome: Boolean = true,
    val showAppEntryAnimation: Boolean = true,
    val lastSyncTimestamp: Long = 0L
)

