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
import com.cinetrack.ui.components.detail.ALL_VIBES

data class VibeStat(val vibe: String, val emoji: String, val count: Int)
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
    val colorHex: Long
)

data class FlowUiState(
    val movies: List<Movie> = emptyList(),
    val count: Int = 0,
    val topVibes: List<VibeStat> = emptyList(),
    val topMvps: List<MvpStat> = emptyList(),
    val flowPersona: FlowPersona? = null,
    val timeRange: TimeRange = TimeRange.AllTime,
    val availableYears: ImmutableList<Int> = persistentListOf()
)

@HiltViewModel
class FlowViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _timeRange = MutableStateFlow<TimeRange>(TimeRange.AllTime)

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
    }

    val uiState: StateFlow<FlowUiState> = combine(
        movieRepository.getFlowMoviesFlow(),
        _timeRange
    ) { movies, range ->
        withContext(Dispatchers.Default) {
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

            val filteredMovies = when (range) {
                is TimeRange.AllTime -> movies
                is TimeRange.Year -> movies.filter { movie ->
                    val watchedDate = movie.watchedAt
                    !watchedDate.isNullOrBlank() && watchedDate.startsWith(range.year.toString())
                }
            }

            val sortedMovies = filteredMovies.sortedByDescending { it.clientUpdatedAt }
            
            // Removed generic total runtime
            
            // Top Vibes
            val vibeCounts = mutableMapOf<String, Int>()
            sortedMovies.forEach { movie ->
                    movie.emotionalVibes?.split(",")?.forEach { vibe ->
                        val trimmed = vibe.trim()
                        if (trimmed.isNotEmpty()) {
                            vibeCounts[trimmed] = vibeCounts.getOrDefault(trimmed, 0) + 1
                        }
                    }
                }
                val topVibes = vibeCounts.entries.map { entry ->
                    val code = entry.key
                    val emoji = ALL_VIBES.find { it.code == code }?.emoji ?: "❓"
                    VibeStat(code, emoji, entry.value) 
                }.sortedByDescending { it.count }
                    
                // Top MVPs
                val mvpCounts = mutableMapOf<Long, MvpStat>()
                sortedMovies.forEach { movie ->
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
                                characterImageUrl = movie.customBackdropPath ?: movie.backdropPath,
                                profilePath = movie.favoriteActorProfilePath,
                                count = 1
                            )
                        }
                    }
                }
                val topMvps = mvpCounts.values.sortedByDescending { it.count }
                
                // Calculate Flow Persona based on Top Vibe
                val persona = topVibes.firstOrNull()?.vibe?.let { topVibe ->
                    when(topVibe) {
                        "MASTERPIECE" -> FlowPersona(R.string.persona_title_connoisseur, R.string.persona_desc_connoisseur, "🍷", 0xFFFFD700)
                        "MIND_BLOWING" -> FlowPersona(R.string.persona_title_philosopher, R.string.persona_desc_philosopher, "🌌", 0xFF9C27B0)
                        "IN_TEARS" -> FlowPersona(R.string.persona_title_empath, R.string.persona_desc_empath, "💧", 0xFF2196F3)
                        "HYPED" -> FlowPersona(R.string.persona_title_adrenaline_junkie, R.string.persona_desc_adrenaline_junkie, "⚡", 0xFFFF5722)
                        "COZY" -> FlowPersona(R.string.persona_title_comfort_seeker, R.string.persona_desc_comfort_seeker, "🍵", 0xFFFF9800)
                        "FEELS_GOOD" -> FlowPersona(R.string.persona_title_optimist, R.string.persona_desc_optimist, "☀️", 0xFFFFEB3B)
                        "FUNNY" -> FlowPersona(R.string.persona_title_jokester, R.string.persona_desc_jokester, "🎭", 0xFFE91E63)
                        "WEIRD" -> FlowPersona(R.string.persona_title_explorer, R.string.persona_desc_explorer, "🛸", 0xFF00BCD4)
                        "SCARY" -> FlowPersona(R.string.persona_title_thrill_seeker, R.string.persona_desc_thrill_seeker, "🔪", 0xFFF44336)
                        "MEH", "DISAPPOINTED", "BORING" -> FlowPersona(R.string.persona_title_critic, R.string.persona_desc_critic, "🧐", 0xFF9E9E9E)
                        else -> FlowPersona(R.string.persona_title_wanderer, R.string.persona_desc_wanderer, "🌿", 0xFF4CAF50)
                    }
                }
                
                FlowUiState(
                    movies = sortedMovies,
                    count = sortedMovies.size,
                    topVibes = topVibes,
                    topMvps = topMvps,
                    flowPersona = persona,
                    timeRange = range,
                    availableYears = years.toImmutableList()
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FlowUiState()
        )
}
