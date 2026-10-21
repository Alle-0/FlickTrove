package com.cinetrack.ui.history.model

import java.time.LocalDate
import java.time.YearMonth

enum class HistoryFilter {
    ALL,
    MOVIES,
    TV_SHOWS,
    REWATCHES,
    WITH_NOTES
}

enum class HistorySortOption {
    DATE_DESC,
    DATE_ASC,
    TITLE_ASC,
    RATING_DESC
}

enum class HistoryMediaTypeFilter {
    ALL,
    MOVIES,
    TV
}

enum class HistoryViewingTypeFilter {
    ALL,
    FIRST_WATCH_ONLY,
    REWATCH_ONLY
}

data class HistoryFilterConfig(
    val sortOption: HistorySortOption = HistorySortOption.DATE_DESC,
    val mediaTypeFilter: HistoryMediaTypeFilter = HistoryMediaTypeFilter.ALL,
    val viewingTypeFilter: HistoryViewingTypeFilter = HistoryViewingTypeFilter.ALL,
    val withNotesOnly: Boolean = false
) {
    val isFiltered: Boolean
        get() = sortOption != HistorySortOption.DATE_DESC ||
                mediaTypeFilter != HistoryMediaTypeFilter.ALL ||
                viewingTypeFilter != HistoryViewingTypeFilter.ALL ||
                withNotesOnly

    val activeFilterCount: Int
        get() {
            var c = 0
            if (sortOption != HistorySortOption.DATE_DESC) c++
            if (mediaTypeFilter != HistoryMediaTypeFilter.ALL) c++
            if (viewingTypeFilter != HistoryViewingTypeFilter.ALL) c++
            if (withNotesOnly) c++
            return c
        }
}

enum class ViewMode {
    TIMELINE,
    CALENDAR
}

data class WatchTimelineEntry(
    val historyId: Long,
    val movieId: Long,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val watchedAt: String,
    val parsedDate: LocalDate,
    val formattedDisplayDate: String,
    val formattedDisplayTime: String?, // null if 00:00:00 or no explicit time
    val isRewatch: Boolean,
    val rewatchNumber: Int,
    val personalRating: Double?,
    val personalNote: String?,
    val runtimeMinutes: Int,
    val episodeInfo: String?,
    val isTvShow: Boolean
)

data class HistoryUiState(
    val allEntries: List<WatchTimelineEntry> = emptyList(),
    val filteredEntries: List<WatchTimelineEntry> = emptyList(),
    // O(1) Precomputed Calendar Map
    val calendarEntriesByDate: Map<LocalDate, List<WatchTimelineEntry>> = emptyMap(),
    val selectedViewMode: ViewMode = ViewMode.TIMELINE,
    val activeFilter: HistoryFilter = HistoryFilter.ALL,
    val filterConfig: HistoryFilterConfig = HistoryFilterConfig(),
    val selectedCalendarMonth: YearMonth = YearMonth.now(),
    val selectedCalendarDate: LocalDate? = null,
    val isFilterModalOpen: Boolean = false,
    val totalMoviesCount: Int = 0,
    val totalTvCount: Int = 0,
    val totalRewatchCount: Int = 0,
    val totalNotesCount: Int = 0,
    val totalRuntimeMinutes: Long = 0L,
    val isLoading: Boolean = true
)

