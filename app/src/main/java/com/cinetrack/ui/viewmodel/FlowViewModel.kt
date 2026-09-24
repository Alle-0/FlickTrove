package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.cinetrack.ui.components.detail.ALL_VIBES
import com.cinetrack.ui.components.detail.normalizeVibeCode
import com.cinetrack.ui.components.detail.findVibe

data class VibeStat(val vibe: String, val emoji: String, val count: Int, val iconRes: Int? = null, val colorHex: Long? = null)
data class MvpStat(
    val actorId: Long,
    val actorName: String,
    val characterImageUrl: String?,
    val profilePath: String?,
    val count: Int
)

data class FlowPersona(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val emoji: String,
    @androidx.annotation.DrawableRes val iconRes: Int,
    val colorHex: Long
)

enum class FlowSortOption {
    RECENT_CHECKIN,
    WATCH_DATE,
    RATING
}

enum class FlowSortOrder {
    DESC,
    ASC
}

enum class FlowMediaTypeFilter {
    ALL,
    MOVIE,
    TV
}

data class FlowFilterConfig(
    val sortOption: FlowSortOption = FlowSortOption.RECENT_CHECKIN,
    val sortOrder: FlowSortOrder = FlowSortOrder.DESC,
    val selectedVibes: Set<String> = emptySet(),
    val mediaType: FlowMediaTypeFilter = FlowMediaTypeFilter.ALL
) {
    val hasActiveFilters: Boolean
        get() = sortOption != FlowSortOption.RECENT_CHECKIN ||
                sortOrder != FlowSortOrder.DESC ||
                selectedVibes.isNotEmpty() ||
                mediaType != FlowMediaTypeFilter.ALL
}

data class FlowUiState(
    val movies: List<Movie> = emptyList(),
    val count: Int = 0,
    val totalUnfilteredCount: Int = 0,
    val topVibes: List<VibeStat> = emptyList(),
    val topMvps: List<MvpStat> = emptyList(),
    val flowPersona: FlowPersona? = null,
    val timeRange: TimeRange = TimeRange.AllTime,
    val availableYears: ImmutableList<Int> = persistentListOf(),
    val filterConfig: FlowFilterConfig = FlowFilterConfig()
)

@HiltViewModel
class FlowViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _timeRange = MutableStateFlow<TimeRange>(TimeRange.AllTime)
    private val _filterConfig = MutableStateFlow(FlowFilterConfig())

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
    }

    fun updateFilterConfig(config: FlowFilterConfig) {
        _filterConfig.value = config
    }

    fun resetFilters() {
        _filterConfig.value = FlowFilterConfig()
    }

    val uiState: StateFlow<FlowUiState> = combine(
        movieRepository.getFlowMoviesFlow(),
        _timeRange,
        _filterConfig
    ) { movies, range, filterConfig ->
        withContext(Dispatchers.Default) {
            // Auto self-healing for legacy or imported vibes (e.g., Movie Paradise unmapped strings)
            val unnormalizedMovies = movies.filter { movie ->
                val rawVibes = movie.emotionalVibes
                if (!rawVibes.isNullOrBlank()) {
                    rawVibes.split(",").any {
                        val trimmed = it.trim()
                        trimmed.isNotEmpty() && normalizeVibeCode(trimmed) != trimmed
                    }
                } else false
            }
            if (unnormalizedMovies.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    unnormalizedMovies.forEach { movie ->
                        val fixedVibes = movie.emotionalVibes?.split(",")
                            ?.map { normalizeVibeCode(it.trim()) }
                            ?.filter { it.isNotBlank() }
                            ?.distinct()
                            ?.joinToString(",")
                        if (!fixedVibes.isNullOrBlank() && fixedVibes != movie.emotionalVibes) {
                            movie.emotionalVibes = fixedVibes
                            movieRepository.saveMovie(movie, syncToTrakt = false)
                        }
                    }
                }
            }

            val watchedMovies = movies.filter { it.watched || (it.mediaType == "tv" && it.dropped) }
            val years = watchedMovies.mapNotNull { movie ->
                val date = movie.watchedAt
                if (!date.isNullOrBlank()) {
                    try {
                        java.time.Instant.parse(date).atZone(java.time.ZoneId.systemDefault()).year
                    } catch (e: Exception) {
                        try {
                            java.time.LocalDate.parse(date).year
                        } catch (e2: Exception) {
                            date.take(4).toIntOrNull()
                        }
                    }
                } else null
            }.distinct().sortedDescending()

            val timeFilteredMovies = when (range) {
                is TimeRange.AllTime -> movies
                is TimeRange.Year -> movies.filter { movie ->
                    val watchedDate = movie.watchedAt
                    !watchedDate.isNullOrBlank() && watchedDate.startsWith(range.year.toString())
                }
            }

            // Top Vibes calcolati sull'intero pool del periodo (per alimentare il selettore filtri con i conteggi corretti)
            val vibeCounts = mutableMapOf<String, Int>()
            timeFilteredMovies.forEach { movie ->
                movie.emotionalVibes?.split(",")?.forEach { vibe ->
                    val trimmed = vibe.trim()
                    if (trimmed.isNotEmpty()) {
                        val normalized = normalizeVibeCode(trimmed)
                        if (normalized.isNotBlank()) {
                            vibeCounts[normalized] = vibeCounts.getOrDefault(normalized, 0) + 1
                        }
                    }
                }
            }
            val topVibes = vibeCounts.entries.map { entry ->
                val code = entry.key
                val emotionalVibe = findVibe(code)
                val emoji = emotionalVibe?.emoji ?: "❓"
                VibeStat(code, emoji, entry.value, emotionalVibe?.iconRes, emotionalVibe?.colorHex) 
            }.sortedByDescending { it.count }

            // 1. Filtro Tipo Media
            val mediaFilteredMovies = when (filterConfig.mediaType) {
                FlowMediaTypeFilter.ALL -> timeFilteredMovies
                FlowMediaTypeFilter.MOVIE -> timeFilteredMovies.filter { (it.mediaType.ifEmpty { "movie" }) == "movie" }
                FlowMediaTypeFilter.TV -> timeFilteredMovies.filter { it.mediaType == "tv" }
            }

            // 2. Filtro Vibe Emozionale
            val vibeFilteredMovies = if (filterConfig.selectedVibes.isNotEmpty()) {
                mediaFilteredMovies.filter { movie ->
                    val movieVibes = movie.emotionalVibes?.split(",")?.map { normalizeVibeCode(it.trim()) }?.toSet().orEmpty()
                    movieVibes.any { it in filterConfig.selectedVibes }
                }
            } else {
                mediaFilteredMovies
            }

            // 3. Ordinamento
            val sortedMovies = when (filterConfig.sortOption) {
                FlowSortOption.RECENT_CHECKIN -> {
                    if (filterConfig.sortOrder == FlowSortOrder.DESC) {
                        vibeFilteredMovies.sortedByDescending { it.clientUpdatedAt ?: 0L }
                    } else {
                        vibeFilteredMovies.sortedBy { it.clientUpdatedAt ?: 0L }
                    }
                }
                FlowSortOption.WATCH_DATE -> {
                    if (filterConfig.sortOrder == FlowSortOrder.DESC) {
                        vibeFilteredMovies.sortedByDescending { it.watchedAt.orEmpty() }
                    } else {
                        vibeFilteredMovies.sortedBy { it.watchedAt.orEmpty() }
                    }
                }
                FlowSortOption.RATING -> {
                    if (filterConfig.sortOrder == FlowSortOrder.DESC) {
                        vibeFilteredMovies.sortedByDescending { it.personalRating ?: 0.0 }
                    } else {
                        vibeFilteredMovies.sortedBy { it.personalRating ?: 0.0 }
                    }
                }
            }
                    
            // Top MVPs
            val mvpCounts = mutableMapOf<Long, MvpStat>()
            timeFilteredMovies.forEach { movie ->
                val id = movie.favoriteActorId
                val name = movie.favoriteActorName
                if (id != null && name != null) {
                    val current = mvpCounts[id]
                    if (current != null) {
                        mvpCounts[id] = current.copy(count = current.count + 1)
                    } else {
                        mvpCounts[id] = MvpStat(
                            actorId = id,
                            actorName = name,
                            characterImageUrl = null, // unused — profilePath carries the actor image
                            profilePath = movie.favoriteActorTmdbPath ?: movie.favoriteActorProfilePath,
                            count = 1
                        )
                    }
                }
            }
            val topMvps = mvpCounts.values.sortedByDescending { it.count }
            
            // Calculate Flow Persona based on Top Vibe
            val persona = topVibes.firstOrNull()?.vibe?.let { topVibe ->
                when(topVibe) {
                    "MASTERPIECE" -> FlowPersona(R.string.persona_title_connoisseur, R.string.persona_desc_connoisseur, "🍷", R.drawable.ic_vibe_masterpiece, 0xFFFFD700)
                    "MIND_BLOWING" -> FlowPersona(R.string.persona_title_philosopher, R.string.persona_desc_philosopher, "🌌", R.drawable.ic_vibe_mind_blowing, 0xFF9C27B0)
                    "IN_TEARS" -> FlowPersona(R.string.persona_title_empath, R.string.persona_desc_empath, "💧", R.drawable.ic_vibe_in_tears, 0xFF2196F3)
                    "HYPED" -> FlowPersona(R.string.persona_title_adrenaline_junkie, R.string.persona_desc_adrenaline_junkie, "⚡", R.drawable.ic_vibe_hyped, 0xFFFF5722)
                    "COZY" -> FlowPersona(R.string.persona_title_comfort_seeker, R.string.persona_desc_comfort_seeker, "🍵", R.drawable.ic_vibe_cozy, 0xFFFF9800)
                    "FEELS_GOOD" -> FlowPersona(R.string.persona_title_optimist, R.string.persona_desc_optimist, "☀️", R.drawable.ic_vibe_feels_good, 0xFFFFEB3B)
                    "FUNNY" -> FlowPersona(R.string.persona_title_jokester, R.string.persona_desc_jokester, "🎭", R.drawable.ic_vibe_funny, 0xFFE91E63)
                    "WEIRD" -> FlowPersona(R.string.persona_title_explorer, R.string.persona_desc_explorer, "🛸", R.drawable.ic_vibe_weird, 0xFF00BCD4)
                    "SCARY" -> FlowPersona(R.string.persona_title_thrill_seeker, R.string.persona_desc_thrill_seeker, "🔪", R.drawable.ic_vibe_scary, 0xFFF44336)
                    "MEH" -> FlowPersona(R.string.persona_title_critic, R.string.persona_desc_critic, "🧐", R.drawable.ic_vibe_meh, 0xFF9E9E9E)
                    "DISAPPOINTED" -> FlowPersona(R.string.persona_title_critic, R.string.persona_desc_critic, "😤", R.drawable.ic_vibe_disappointed, 0xFFE64A19)
                    "BORING" -> FlowPersona(R.string.persona_title_critic, R.string.persona_desc_critic, "😴", R.drawable.ic_vibe_boring, 0xFF7986CB)
                    else -> FlowPersona(R.string.persona_title_wanderer, R.string.persona_desc_wanderer, "🌿", R.drawable.ic_world, 0xFF4CAF50)
                }
            }
            
            FlowUiState(
                movies = sortedMovies,
                count = sortedMovies.size,
                totalUnfilteredCount = timeFilteredMovies.size,
                topVibes = topVibes,
                topMvps = topMvps,
                flowPersona = persona,
                timeRange = range,
                availableYears = years.toImmutableList(),
                filterConfig = filterConfig
            )
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FlowUiState()
    )
}
