package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.GenreConstants
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Locale

sealed class TimeRange {
    data object AllTime : TimeRange()
    data class Year(val year: Int) : TimeRange()
}

data class StatsUiState(
    val stats: CalculatedStats? = null,
    val currentYearStats: CalculatedStats? = null,
    val timeRange: TimeRange = TimeRange.AllTime,
    val availableYears: List<Int> = emptyList(),
    val isLoading: Boolean = true
)

// Person with profile photo for cast/director charts
data class PersonStat(
    val id: Long,
    val name: String,
    val profilePath: String?,
    val count: Int
)

data class CalculatedStats(
    val totalTimeFormatted: String,
    val isEstimate: Boolean,
    val moviesWatched: Int,
    val moviesToWatch: Int,
    val totalMinutes: Int,
    val movieMinutes: Int,
    val movieTimeFormatted: String,
    val moviesEstimate: Boolean,
    val longestMovie: Movie?,
    val longestMovieMinutes: Int,
    val tvWatched: Int,
    val tvToWatch: Int,
    val totalEpisodes: Int,
    val tvMinutes: Int,
    val tvTimeFormatted: String,
    val tvEstimate: Boolean,
    val longestTV: Movie?,
    val longestTVMinutes: Int,
    val genreCounts: List<Pair<String, Int>>,
    val decadeCounts: List<Pair<String, Int>>,
    val ratingDistribution: IntArray,
    val topCast: List<PersonStat>,
    val topDirectors: List<PersonStat>,
    val topGenre: String?
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _timeRange = MutableStateFlow<TimeRange>(TimeRange.AllTime)

    val uiState: StateFlow<StatsUiState> = combine(
        repository.getLocalMoviesFlow(),
        _timeRange
    ) { movies, range ->
        val watchedMovies = movies.filter { it.watched }
        // Build the list of years from watchedAt only — never releaseDate.
        // This ensures the year picker shows years when the user actually watched things.
        val years = watchedMovies.mapNotNull { movie ->
            val date = movie.watchedAt
            if (!date.isNullOrBlank() && date.length >= 4) {
                try { date.substring(0, 4).toInt() } catch (e: Exception) { null }
            } else null
        }.distinct().sortedDescending()

        val filteredMovies = when (range) {
            is TimeRange.AllTime -> watchedMovies
            is TimeRange.Year -> watchedMovies.filter { movie ->
                // Filter by the date the user watched the movie, not its release year.
                val watchedDate = movie.watchedAt
                !watchedDate.isNullOrBlank() && watchedDate.startsWith(range.year.toString())
            }
        }
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val currentYearMovies = watchedMovies.filter { movie ->
            val watchedDate = movie.watchedAt
            !watchedDate.isNullOrBlank() && watchedDate.startsWith(currentYear.toString())
        }

        StatsUiState(
            stats = calculateStats(filteredMovies),
            currentYearStats = calculateStats(currentYearMovies),
            timeRange = range,
            availableYears = years,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
    }

    private fun formatDuration(minutes: Int): String {
        if (minutes <= 0) return "0m"
        val days = minutes / 1440
        val hours = (minutes % 1440) / 60
        val mins = minutes % 60
        return when {
            days > 0 -> "${days}g ${hours}h ${mins}m"
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }

    private fun calculateStats(movies: List<Movie>): CalculatedStats {
        val watched = movies.filter { it.watched }
        val watchedMovies = watched.filter { it.mediaType != "tv" }
        val watchedTV = watched.filter { it.mediaType == "tv" }

        // Movies
        var moviesEstimate = false
        val movieMin = watchedMovies.sumOf {
            if (it.runtime == null || it.runtime == 0) moviesEstimate = true
            it.runtime?.takeIf { r -> r > 0 } ?: 95
        }
        val longestMovie = if (watchedMovies.isNotEmpty()) watchedMovies.maxByOrNull { it.runtime ?: 0 } else null

        // TV
        var tvEstimate = false
        val tvStats = watchedTV.map { m ->
            val watchedCount = m.watchedEpisodes?.values?.sumOf { it.size } ?: 0
            val avgRunTime = m.episodeRunTime?.firstOrNull() ?: 45
            if (m.episodeRunTime.isNullOrEmpty()) tvEstimate = true
            m to (watchedCount * avgRunTime)
        }
        val tvMin = tvStats.sumOf { it.second }
        val longestTVStat = tvStats.maxByOrNull { it.second }
        val totalEpisodes = watchedTV.sumOf { it.watchedEpisodes?.values?.sumOf { it.size } ?: 0 }

        // Genres
        val genreCounts = mutableMapOf<String, Int>()
        watched.forEach { m ->
            m.genreIds?.forEach { id ->
                val genreName = GenreConstants.ALL_GENRES.find { it.id == id.toLong() }?.name ?: "Altro"
                genreCounts[genreName] = (genreCounts[genreName] ?: 0) + 1
            }
        }

        // Decades - fill gaps to make it a proper timeline
        val years = watched.mapNotNull { m ->
            val date = m.releaseDate ?: m.firstAirDate
            if (!date.isNullOrBlank() && date.length >= 4) {
                try { date.substring(0, 4).toInt() } catch (e: Exception) { null }
            } else null
        }.filter { it > 0 }

        val minYear = years.minOrNull() ?: 0
        val maxYear = years.maxOrNull() ?: 0
        
        val decadeCounts = mutableMapOf<String, Int>()
        if (minYear > 0 && maxYear > 0) {
            val startDecade = (minYear / 10) * 10
            val endDecade = (maxYear / 10) * 10
            for (d in startDecade..endDecade step 10) {
                decadeCounts["${d}s"] = 0
            }
        }

        years.forEach { year ->
            val decade = "${(year / 10) * 10}s"
            decadeCounts[decade] = (decadeCounts[decade] ?: 0) + 1
        }

        // Ratings
        val ratings = IntArray(20)
        watched.forEach { m ->
            val rating = m.personalRating ?: 0.0
            if (rating > 0) {
                val bucket = (rating * 2.0).toInt().coerceIn(1, 20) - 1
                ratings[bucket]++
            }
        }

        // Top Cast
        data class CastAccum(val name: String, val profilePath: String?, var count: Int)
        val castMap = mutableMapOf<Long, CastAccum>()
        watched.forEach { m ->
            m.topCastData?.forEach { person ->
                if (person.name.isNotBlank()) {
                    val existing = castMap[person.id]
                    if (existing == null) {
                        castMap[person.id] = CastAccum(person.name, person.profilePath, 1)
                    } else {
                        existing.count++
                    }
                }
            }
        }
        val topCast = castMap.entries
            .sortedByDescending { it.value.count }
            .take(50)
            .map { (id, accum) -> PersonStat(id, accum.name, accum.profilePath, accum.count) }

        // Top Directors
        data class DirAccum(val name: String, val profilePath: String?, var count: Int)
        val dirMap = mutableMapOf<Long, DirAccum>()
        watched.forEach { m ->
            val dirId = m.directorId ?: return@forEach
            val dirName = m.directorName ?: return@forEach
            if (dirName.isBlank()) return@forEach
            val existing = dirMap[dirId]
            if (existing == null) {
                dirMap[dirId] = DirAccum(dirName, m.directorProfilePath, 1)
            } else {
                existing.count++
            }
        }
        val topDirectors = dirMap.entries
            .sortedByDescending { it.value.count }
            .take(50)
            .map { (id, accum) -> PersonStat(id, accum.name, accum.profilePath, accum.count) }

        val topGenre = genreCounts.entries.sortedByDescending { it.value }.firstOrNull()?.key

        return CalculatedStats(
            totalTimeFormatted = formatDuration(movieMin + tvMin).uppercase(),
            isEstimate = moviesEstimate || tvEstimate,
            moviesWatched = watchedMovies.size,
            moviesToWatch = movies.count { it.mediaType != "tv" && !it.watched && (it.favorite || it.reminder) },
            totalMinutes = movieMin + tvMin,
            movieMinutes = movieMin,
            movieTimeFormatted = formatDuration(movieMin),
            moviesEstimate = moviesEstimate,
            longestMovie = longestMovie,
            longestMovieMinutes = longestMovie?.runtime ?: 0,
            tvWatched = watchedTV.size,
            tvToWatch = movies.count { it.mediaType == "tv" && !it.watched && (it.favorite || it.reminder) },
            totalEpisodes = totalEpisodes,
            tvMinutes = tvMin,
            tvTimeFormatted = formatDuration(tvMin),
            tvEstimate = tvEstimate,
            longestTV = longestTVStat?.first,
            longestTVMinutes = longestTVStat?.second ?: 0,
            genreCounts = genreCounts.entries.sortedByDescending { it.value }.map { it.key to it.value },
            decadeCounts = decadeCounts.entries.sortedBy { it.key }.map { it.key to it.value },
            ratingDistribution = ratings,
            topCast = topCast,
            topDirectors = topDirectors,
            topGenre = topGenre
        )
    }
}
