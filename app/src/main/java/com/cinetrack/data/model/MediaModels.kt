package com.cinetrack.data.model

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
    @SerialName("air_date") val airDate: String? = null,
    val overview: String? = null,
    val episodes: List<Episode>? = null
)

@Serializable
@Stable
data class NextEpisodeInfo(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val remainingInSeason: Int,
    val remainingTotal: Int,
    val progress: Float,
    val isLastEpisodeOfSeries: Boolean = false,
    val isUpToDateWithAirDate: Boolean = false
) {
    val episodeCode: String
        get() = "S${seasonNumber.toString().padStart(2, '0')} E${episodeNumber.toString().padStart(2, '0')}"
}

@Serializable
@Stable
data class PersonData(
    val id: Long = 0L,
    val name: String = "",
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
@Stable
data class StudioData(
    val id: Long = 0L,
    val name: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("origin_country") val originCountry: String? = null
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
    val selectedKeywords: List<Long> = emptyList(),
    val selectedStatuses: List<String> = emptyList() // "dropped", "not_started", "watching", "up_to_date"
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
    val foldersSort: SortConfig = SortConfig(sortType = "date", sortDirection = "desc"),
    val discoveryFilters: DiscoveryFilters = DiscoveryFilters(),
    val gridColumns: Int = 3,
    val showLayoutToggle: Boolean = false,
    val isSearchSuggestionsExpanded: Boolean = true,
    val notificationsReleases: Boolean = true,
    val notificationsSocial: Boolean = true,
    val showFolderBookmarks: Boolean = true,
    val showBadges: Boolean = true,
    val disabledBadges: Set<String> = emptySet(),
    val vibrationEnabled: Boolean = true,
    val accentColor: String = "Teal",
    val appTheme: String = "System", // System, Light, Dark, Amoled
    val contentLanguage: String = "system",
    val advancedVisualEffectsEnabled: Boolean = true,
    val dynamicAppIconEnabled: Boolean = false,
    val showSplitReleasesHome: Boolean = true,
    val showSplitDroppedHome: Boolean = false,
    val showAppEntryAnimation: Boolean = true,
    val useMovieLogo: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val defaultStartTab: String = "feed",
    val tvdbJwtToken: String = "",
    val showMyFolders: Boolean = true,
    val showYourFlow: Boolean = true,
    val titleTextSizeMultiplier: Float = 1.0f,
    val imageQuality: String = "MEDIUM",
    val showGeneralStats: Boolean = true,
    val dashboardCardOrder: List<String> = listOf("stats", "folders", "flow"),
    val showHomeContinueWatching: Boolean = true,
    val showHomeWatchlist: Boolean = true,
    val showHomeBecauseYouWatched: Boolean = true,
    val homeSectionOrder: List<String> = HomeFeedSectionConstants.DEFAULT_ORDER
)

object HomeFeedSectionConstants {
    const val CONTINUE_WATCHING = "continue_watching"
    const val WATCHLIST = "watchlist"
    const val TROVE_PICK = "trove_pick"
    const val BECAUSE_YOU_WATCHED = "because_you_watched"
    const val TOP_10 = "top_10"
    const val POPULAR = "popular"
    const val NOW_PLAYING = "now_playing"
    const val UPCOMING = "upcoming"
    const val NEWS = "news"

    val DEFAULT_ORDER = listOf(
        CONTINUE_WATCHING,
        WATCHLIST,
        TROVE_PICK,
        BECAUSE_YOU_WATCHED,
        TOP_10,
        POPULAR,
        NOW_PLAYING,
        UPCOMING,
        NEWS
    )

    fun sanitizeOrder(savedOrder: List<String>): List<String> {
        val validSaved = savedOrder.filter { it in DEFAULT_ORDER }.distinct()
        val missing = DEFAULT_ORDER.filter { it !in validSaved }
        return validSaved + missing
    }
}


