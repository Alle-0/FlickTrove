package com.cinetrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.local.dao.RawTimelineRecord
import com.cinetrack.data.repository.BadgeRepository
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.WatchHistoryRepository
import com.cinetrack.domain.UpdateEpisodesUseCase
import com.cinetrack.ui.history.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val watchHistoryRepository: WatchHistoryRepository,
    private val badgeRepository: BadgeRepository,
    private val movieRepository: MovieRepository,
    private val updateEpisodesUseCase: UpdateEpisodesUseCase
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ViewMode.TIMELINE)
    private val _filterConfig = MutableStateFlow(HistoryFilterConfig())
    private val _selectedCalendarMonth = MutableStateFlow(YearMonth.now())
    private val _selectedCalendarDate = MutableStateFlow<LocalDate?>(null)
    private val _isFilterModalOpen = MutableStateFlow(false)

    private val rawTimelineFlow = watchHistoryRepository.getGlobalTimelineFlow()

    private data class CalendarFilterTuple(
        val filterConfig: HistoryFilterConfig,
        val month: YearMonth,
        val selectedDate: LocalDate?,
        val isModalOpen: Boolean
    )

    private val calendarFilterFlow = combine(
        _filterConfig,
        _selectedCalendarMonth,
        _selectedCalendarDate,
        _isFilterModalOpen
    ) { filterConfig, month, selectedDate, isModalOpen ->
        CalendarFilterTuple(filterConfig, month, selectedDate, isModalOpen)
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        rawTimelineFlow,
        _viewMode,
        calendarFilterFlow
    ) { rawRecords, viewMode, cfTuple ->
        val filterConfig = cfTuple.filterConfig
        val calendarMonth = cfTuple.month
        val selectedDate = cfTuple.selectedDate
        val isFilterModalOpen = cfTuple.isModalOpen

        // Anti-Tenet Rewatch Calculation (Chronological traversal from oldest to newest)
        val rewatchCounters = mutableMapOf<String, Int>()
        val rewatchMap = HashMap<Long, Int>(rawRecords.size)

        for (i in rawRecords.indices.reversed()) {
            val rec = rawRecords[i]
            if (rec.isRewatch) {
                val compositeKey = "${rec.mediaType}_${rec.movieId}"
                val nextCount = (rewatchCounters[compositeKey] ?: 0) + 1
                rewatchCounters[compositeKey] = nextCount
                rewatchMap[rec.historyId] = nextCount
            }
        }

        val entries = rawRecords.flatMap { rec ->
            if (rec.mediaType == "tv") {
                val epMap: Map<String, List<Int>> = if (rec.isRewatch && !rec.historicalEpisodes.isNullOrBlank()) {
                    try {
                        Json.decodeFromString(rec.historicalEpisodes)
                    } catch (e: Exception) {
                        rec.currentEpisodes ?: emptyMap()
                    }
                } else {
                    rec.currentEpisodes ?: emptyMap()
                }

                val epDatesMap: Map<String, String> = rec.episodeDates?.let {
                    try {
                        Json.decodeFromString<Map<String, String>>(it)
                    } catch (e: Exception) {
                        emptyMap()
                    }
                } ?: emptyMap()

                val validEpMap = epMap.filter { it.key != "0" && it.value.isNotEmpty() }
                if (validEpMap.isNotEmpty()) {
                    data class EpItem(val season: String, val episode: Int, val iso: String, val date: LocalDate, val time: LocalTime?)

                    val items = validEpMap.flatMap { (season, eps) ->
                        eps.map { ep ->
                            val iso = epDatesMap["${season}_${ep}"] ?: rec.watchedAt
                            val (d, t) = parseWatchedAt(iso)
                            EpItem(season, ep, iso, d, t)
                        }
                    }

                    val groupedByDay = items.groupBy { it.date }

                    groupedByDay.map { (day, dayItems) ->
                        val latestItem = dayItems.maxByOrNull { parseToEpochMillis(it.iso) } ?: dayItems.first()
                        val hasExplicitTime = latestItem.time != null && !(latestItem.time.hour == 0 && latestItem.time.minute == 0 && latestItem.time.second == 0)

                        val displayDate = try {
                            day.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
                        } catch (e: Exception) {
                            latestItem.iso.take(10)
                        }

                        val displayTime = if (hasExplicitTime && latestItem.time != null) {
                            try {
                                latestItem.time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
                            } catch (e: Exception) {
                                null
                            }
                        } else null

                        val daySeasonMap = dayItems.groupBy({ it.season }, { it.episode })
                        val episodeInfo = formatTvEpisodesForMap(daySeasonMap)

                        val rawReleaseDate = rec.firstAirDate ?: rec.releaseDate
                        val releaseYear = rawReleaseDate?.take(4)?.takeIf { it.length == 4 && it.all { ch -> ch.isDigit() } }

                        WatchTimelineEntry(
                            historyId = rec.historyId,
                            movieId = rec.movieId,
                            mediaType = rec.mediaType,
                            title = rec.title,
                            posterPath = rec.posterPath,
                            watchedAt = latestItem.iso,
                            parsedDate = day,
                            formattedDisplayDate = displayDate,
                            formattedDisplayTime = displayTime,
                            isRewatch = rec.isRewatch,
                            rewatchNumber = rewatchMap[rec.historyId] ?: 0,
                            personalRating = rec.personalRating,
                            personalNote = rec.personalNote?.takeIf { it.isNotBlank() },
                            runtimeMinutes = rec.runtimeMinutes,
                            episodeInfo = episodeInfo,
                            isTvShow = true,
                            releaseYear = releaseYear,
                            sessionEpisodes = daySeasonMap,
                            totalSessionsInRewatch = groupedByDay.size
                        )
                    }
                } else {
                    listOf(buildSingleEntry(rec, rewatchMap))
                }
            } else {
                listOf(buildSingleEntry(rec, rewatchMap))
            }
        }

        // Apply filters: Media, Viewing, Notes
        val filteredByCriteria = entries.filter { entry ->
            val matchesMedia = when (filterConfig.mediaTypeFilter) {
                HistoryMediaTypeFilter.ALL -> true
                HistoryMediaTypeFilter.MOVIES -> !entry.isTvShow
                HistoryMediaTypeFilter.TV -> entry.isTvShow
            }
            val matchesViewing = when (filterConfig.viewingTypeFilter) {
                HistoryViewingTypeFilter.ALL -> true
                HistoryViewingTypeFilter.FIRST_WATCH_ONLY -> !entry.isRewatch
                HistoryViewingTypeFilter.REWATCH_ONLY -> entry.isRewatch
            }
            val matchesNotes = if (filterConfig.withNotesOnly) !entry.personalNote.isNullOrBlank() else true

            matchesMedia && matchesViewing && matchesNotes
        }

        // Apply sort (chronological) using epoch millis to avoid lexicographical string discrepancies
        val sortedEntries = when (filterConfig.sortOption) {
            HistorySortOption.DATE_DESC -> filteredByCriteria.sortedByDescending { parseToEpochMillis(it.watchedAt) }
            HistorySortOption.DATE_ASC -> filteredByCriteria.sortedBy { parseToEpochMillis(it.watchedAt) }
        }

        // Precompute calendar entries grouped by date O(1)
        val calendarMap = filteredByCriteria.groupBy { it.parsedDate }

        val totalMovies = rawRecords.count { it.mediaType != "tv" }
        val totalTv = rawRecords.count { it.mediaType == "tv" }
        val totalRewatch = rawRecords.count { it.isRewatch }
        val totalNotes = rawRecords.count { !it.personalNote.isNullOrBlank() }
        val totalMinutes = entries.sumOf { it.runtimeMinutes.toLong() }

        HistoryUiState(
            allEntries = entries,
            filteredEntries = sortedEntries,
            calendarEntriesByDate = calendarMap,
            selectedViewMode = viewMode,
            activeFilter = HistoryFilter.ALL,
            filterConfig = filterConfig,
            selectedCalendarMonth = calendarMonth,
            selectedCalendarDate = selectedDate,
            isFilterModalOpen = isFilterModalOpen,
            totalMoviesCount = totalMovies,
            totalTvCount = totalTv,
            totalRewatchCount = totalRewatch,
            totalNotesCount = totalNotes,
            totalRuntimeMinutes = totalMinutes,
            isLoading = false
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun setFilterConfig(config: HistoryFilterConfig) {
        _filterConfig.value = config
    }

    fun resetFilterConfig() {
        _filterConfig.value = HistoryFilterConfig()
    }

    fun openFilterModal() {
        _isFilterModalOpen.value = true
    }

    fun dismissFilterModal() {
        _isFilterModalOpen.value = false
    }

    fun setSelectedCalendarDate(date: LocalDate?) {
        _selectedCalendarDate.value = date
    }

    fun setFilter(filter: HistoryFilter) {
        _filterConfig.value = when (filter) {
            HistoryFilter.ALL -> HistoryFilterConfig()
            HistoryFilter.MOVIES -> HistoryFilterConfig(mediaTypeFilter = HistoryMediaTypeFilter.MOVIES)
            HistoryFilter.TV_SHOWS -> HistoryFilterConfig(mediaTypeFilter = HistoryMediaTypeFilter.TV)
            HistoryFilter.REWATCHES -> HistoryFilterConfig(viewingTypeFilter = HistoryViewingTypeFilter.REWATCH_ONLY)
            HistoryFilter.WITH_NOTES -> HistoryFilterConfig(withNotesOnly = true)
        }
    }

    fun setCalendarMonth(month: YearMonth) {
        _selectedCalendarMonth.value = month
    }

    fun nextMonth() {
        _selectedCalendarMonth.value = _selectedCalendarMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _selectedCalendarMonth.value = _selectedCalendarMonth.value.minusMonths(1)
    }

    fun resetToCurrentMonth() {
        _selectedCalendarMonth.value = YearMonth.now()
    }

    fun deleteSessionEpisodes(entry: WatchTimelineEntry) {
        val sessionEps = entry.sessionEpisodes
        if (sessionEps == null || entry.totalSessionsInRewatch <= 1) {
            deleteWatchSession(entry.historyId)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val allHistory = watchHistoryRepository.getAllWatchHistory()
            val historyEntity = allHistory.find { it.id == entry.historyId } ?: return@launch

            // 1. Parse and remove from episodeDates
            val epDatesMap = historyEntity.episodeDates?.let {
                try {
                    Json.decodeFromString<Map<String, String>>(it).toMutableMap()
                } catch (e: Exception) {
                    mutableMapOf()
                }
            } ?: mutableMapOf()

            sessionEps.forEach { (season, eps) ->
                eps.forEach { ep ->
                    epDatesMap.remove("${season}_${ep}")
                    epDatesMap.remove("S${season}E${ep}")
                }
            }

            // 2. Handle episodes removal
            var shouldDeleteEntireEntity = false
            var updatedHistEpisodesJson: String? = historyEntity.historicalEpisodes

            if (historyEntity.historicalEpisodes != null) {
                // Archived rewatch snapshot
                val histMap = try {
                    Json.decodeFromString<Map<String, List<Int>>>(historyEntity.historicalEpisodes).toMutableMap()
                } catch (e: Exception) {
                    mutableMapOf()
                }
                sessionEps.forEach { (s, eps) ->
                    val cur = histMap[s] ?: emptyList()
                    val rem = cur - eps.toSet()
                    if (rem.isEmpty()) histMap.remove(s) else histMap[s] = rem
                }
                if (histMap.isEmpty() || histMap.values.all { it.isEmpty() }) {
                    shouldDeleteEntireEntity = true
                } else {
                    updatedHistEpisodesJson = Json.encodeToString(histMap.toMap())
                }
            } else {
                // Active/current watch: update local Movie entity
                val local = movieRepository.getMovie(entry.movieId, entry.mediaType)
                if (local != null) {
                    val curEps = (local.watchedEpisodes ?: emptyMap()).toMutableMap()
                    sessionEps.forEach { (s, eps) ->
                        val cur = curEps[s] ?: emptyList()
                        val rem = cur - eps.toSet()
                        if (rem.isEmpty()) curEps.remove(s) else curEps[s] = rem
                    }
                    val totalEps = local.effectiveTotalEpisodes
                    val watchedCount = curEps.values.sumOf { it.size }
                    if (watchedCount == 0) {
                        shouldDeleteEntireEntity = true
                    }
                    val isCompleted = totalEps > 0 && watchedCount >= totalEps
                    val progressVal = if (totalEps > 0) (watchedCount.toDouble() / totalEps.toDouble()).coerceIn(0.0, 1.0) else 0.0

                    val updatedMovie = local.copy(
                        watchedEpisodes = curEps,
                        watched = isCompleted,
                        progress = progressVal,
                        clientUpdatedAt = System.currentTimeMillis()
                    )
                    movieRepository.saveMovie(updatedMovie)
                }
            }

            if (shouldDeleteEntireEntity || epDatesMap.isEmpty()) {
                deleteWatchSession(entry.historyId)
            } else {
                val newWatchedAt = epDatesMap.values.maxOrNull() ?: historyEntity.watchedAt
                val updatedHistory = historyEntity.copy(
                    watchedAt = newWatchedAt,
                    episodeDates = if (epDatesMap.isNotEmpty()) Json.encodeToString(epDatesMap.toMap()) else null,
                    historicalEpisodes = updatedHistEpisodesJson,
                    syncStatus = if (historyEntity.syncStatus == "synced") "pending_update" else historyEntity.syncStatus
                )
                watchHistoryRepository.updateWatchHistory(updatedHistory)
                watchHistoryRepository.pushPendingWatchHistory()
                badgeRepository.evaluateBadgesAsync()
            }
        }
    }

    fun deleteWatchSession(historyId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val allHistory = watchHistoryRepository.getAllWatchHistory()
            val entry = allHistory.find { it.id == historyId }
            if (entry != null) {
                watchHistoryRepository.deleteWatchHistory(entry)
                watchHistoryRepository.pushPendingWatchHistory()

                // Reconcile Movie entity to preserve Single Source of Truth
                val remainingHistory = watchHistoryRepository.getWatchHistoryForMovie(entry.movieId, entry.mediaType)
                val local = movieRepository.getMovie(entry.movieId, entry.mediaType)
                if (local != null) {
                    if (remainingHistory.isEmpty()) {
                        movieRepository.saveMovie(
                            local.copy(
                                watched = false,
                                watchedAt = null,
                                clientUpdatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        val latest = remainingHistory.maxByOrNull { it.watchedAt }
                        if (latest != null && local.watchedAt != latest.watchedAt) {
                            var updatedLocal = local.copy(
                                watchedAt = latest.watchedAt,
                                clientUpdatedAt = System.currentTimeMillis()
                            )
                            if (entry.mediaType == "tv") {
                                if (latest.historicalEpisodes != null) {
                                    try {
                                        val parsedMap: Map<String, List<Int>> = Json.decodeFromString(latest.historicalEpisodes)
                                        val intMap = parsedMap.mapKeys { it.key.toIntOrNull() ?: 0 }
                                        updatedLocal = updateEpisodesUseCase.batchUpdate(updatedLocal, intMap)
                                    } catch (e: Exception) {
                                        updatedLocal = updateEpisodesUseCase.markAllWatched(updatedLocal)
                                    }
                                } else {
                                    updatedLocal = updateEpisodesUseCase.markAllWatched(updatedLocal)
                                }
                            } else {
                                updatedLocal = updatedLocal.copy(watched = true)
                            }
                            movieRepository.saveMovie(updatedLocal)
                        }
                    }
                }

                badgeRepository.evaluateBadgesAsync()
            }
        }
    }

    fun updateWatchDate(historyId: Long, newWatchedAtIso: String) {
        viewModelScope.launch(Dispatchers.IO) {
            watchHistoryRepository.updateWatchDate(historyId, newWatchedAtIso)
            watchHistoryRepository.pushPendingWatchHistory()

            val allHistory = watchHistoryRepository.getAllWatchHistory()
            val entry = allHistory.find { it.id == historyId }
            if (entry != null) {
                val remainingHistory = watchHistoryRepository.getWatchHistoryForMovie(entry.movieId, entry.mediaType)
                val local = movieRepository.getMovie(entry.movieId, entry.mediaType)
                if (local != null) {
                    val latest = remainingHistory.maxByOrNull { it.watchedAt }
                    if (latest != null && local.watchedAt != latest.watchedAt) {
                        movieRepository.saveMovie(
                            local.copy(
                                watchedAt = latest.watchedAt,
                                clientUpdatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            badgeRepository.evaluateBadgesAsync()
        }
    }

    companion object {
        fun buildSingleEntry(
            rec: RawTimelineRecord,
            rewatchMap: Map<Long, Int>
        ): WatchTimelineEntry {
            val (parsedDate, parsedTime) = parseWatchedAt(rec.watchedAt)
            val hasExplicitTime = parsedTime != null && !(parsedTime.hour == 0 && parsedTime.minute == 0 && parsedTime.second == 0)

            val displayDate = try {
                parsedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
            } catch (e: Exception) {
                rec.watchedAt.take(10)
            }

            val displayTime = if (hasExplicitTime) {
                try {
                    parsedTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
                } catch (e: Exception) {
                    null
                }
            } else null

            val episodeInfo = if (rec.mediaType == "tv") {
                formatTvEpisodes(rec.historicalEpisodes, rec.currentEpisodes)
            } else null

            val rawReleaseDate = if (rec.mediaType == "tv") {
                rec.firstAirDate ?: rec.releaseDate
            } else {
                rec.releaseDate ?: rec.firstAirDate
            }
            val releaseYear = rawReleaseDate?.take(4)?.takeIf { it.length == 4 && it.all { ch -> ch.isDigit() } }

            return WatchTimelineEntry(
                historyId = rec.historyId,
                movieId = rec.movieId,
                mediaType = rec.mediaType,
                title = rec.title,
                posterPath = rec.posterPath,
                watchedAt = rec.watchedAt,
                parsedDate = parsedDate,
                formattedDisplayDate = displayDate,
                formattedDisplayTime = displayTime,
                isRewatch = rec.isRewatch,
                rewatchNumber = rewatchMap[rec.historyId] ?: 0,
                personalRating = rec.personalRating,
                personalNote = rec.personalNote?.takeIf { it.isNotBlank() },
                runtimeMinutes = rec.runtimeMinutes,
                episodeInfo = episodeInfo,
                isTvShow = rec.mediaType == "tv",
                releaseYear = releaseYear
            )
        }

        fun parseWatchedAt(raw: String): Pair<LocalDate, LocalTime?> {
            return try {
                if (raw.contains("T")) {
                    try {
                        val instant = Instant.parse(raw)
                        val zdt = instant.atZone(ZoneId.systemDefault())
                        zdt.toLocalDate() to zdt.toLocalTime()
                    } catch (e: Exception) {
                        val ldt = LocalDateTime.parse(raw.take(19))
                        ldt.toLocalDate() to ldt.toLocalTime()
                    }
                } else if (raw.length >= 10) {
                    LocalDate.parse(raw.take(10)) to null
                } else {
                    LocalDate.now() to null
                }
            } catch (e: Exception) {
                LocalDate.now() to null
            }
        }

        fun parseToEpochMillis(raw: String?): Long {
            if (raw.isNullOrBlank()) return 0L
            return try {
                if (raw.contains("T")) {
                    try {
                        Instant.parse(raw).toEpochMilli()
                    } catch (e: Exception) {
                        LocalDateTime.parse(raw.take(19)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                } else if (raw.length >= 10) {
                    LocalDate.parse(raw.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else {
                    0L
                }
            } catch (e: Exception) {
                0L
            }
        }

        private fun formatTvEpisodes(
            @Suppress("UNUSED_PARAMETER") historicalEpisodes: String?,
            currentEpisodes: Map<String, List<Int>>?
        ): String? {
            return formatTvEpisodesForMap(currentEpisodes)
        }

        private fun formatTvEpisodesForMap(
            episodesMap: Map<String, List<Int>>?
        ): String? {
            if (!episodesMap.isNullOrEmpty()) {
                val validMap = episodesMap.filter { it.key != "0" && it.value.isNotEmpty() }
                val totalEps = validMap.values.sumOf { it.size }
                if (totalEps > 0) {
                    val seasonKeys = validMap.keys.sortedBy { it.toIntOrNull() ?: 0 }
                    return when {
                        seasonKeys.size == 1 -> "S${seasonKeys.first()} • $totalEps ep"
                        seasonKeys.size > 1 -> "${seasonKeys.joinToString(", ") { "S$it" }} • $totalEps ep"
                        else -> "$totalEps ep"
                    }
                }
            }
            return null
        }
    }
}
