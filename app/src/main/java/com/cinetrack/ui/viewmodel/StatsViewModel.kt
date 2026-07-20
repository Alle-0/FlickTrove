package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

sealed class TimeRange {
    data object AllTime : TimeRange()
    data class Year(val year: Int) : TimeRange()
}

data class StatsUiState(
    val stats: CalculatedStats? = null,
    val currentYearStats: CalculatedStats? = null,
    val timeRange: TimeRange = TimeRange.AllTime,
    val availableYears: ImmutableList<Int> = persistentListOf(),
    val moviesInSelectedRange: ImmutableList<Movie> = persistentListOf(),
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
    val genreCounts: ImmutableList<Pair<String, Int>>,
    val decadeCounts: ImmutableList<Pair<String, Int>>,
    val ratingDistribution: ImmutableList<Int>,
    val topCast: ImmutableList<PersonStat>,
    val topDirectors: ImmutableList<PersonStat>,
    val topGenre: String?
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val preferenceRepository: com.cinetrack.data.repository.PreferenceRepository
) : ViewModel() {

    private val _timeRange = MutableStateFlow<TimeRange>(
        TimeRange.AllTime
    )

    val uiState: StateFlow<StatsUiState> = combine(
        repository.getLocalMoviesFlow(),
        _timeRange,
        preferenceRepository.userPreferencesFlow.map { it.contentLanguage }.distinctUntilChanged()
    ) { movies, range, contentLanguage ->
        val watchedMovies = movies.filter { it.watched }
        // Build the list of years from watchedAt only — never releaseDate.
        // This ensures the year picker shows years when the user actually watched things.
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

        val lang = if (contentLanguage == "system") java.util.Locale.getDefault().language else contentLanguage

        StatsUiState(
            stats = calculateStats(filteredMovies, movies, lang),
            currentYearStats = calculateStats(currentYearMovies, movies, lang),
            timeRange = range,
            availableYears = years.toImmutableList(),
            moviesInSelectedRange = filteredMovies.toImmutableList(),
            isLoading = false
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = StatsUiState()
    )

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
    }

    private fun formatDuration(minutes: Int, language: String = "it"): String {
        if (minutes <= 0) return "0m"
        val days = minutes / 1440
        val hours = (minutes % 1440) / 60
        val mins = minutes % 60
        val dayUnit = if (language.startsWith("it", ignoreCase = true)) "g" else "d"
        return when {
            days > 0 -> "${days}$dayUnit ${hours}h ${mins}m"
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }

    private fun calculateStats(movies: List<Movie>, allMovies: List<Movie>, language: String): CalculatedStats {
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
            var avgRunTime = m.episodeRunTime?.firstOrNull() ?: 45
            if (avgRunTime > 240) {
                val totalEps = m.numberOfEpisodes?.takeIf { it > 0 } ?: 1
                val calculatedAvg = avgRunTime / totalEps
                avgRunTime = if (calculatedAvg in 10..240) calculatedAvg else 45
            }
            if (m.episodeRunTime.isNullOrEmpty()) tvEstimate = true
            m to (watchedCount * avgRunTime)
        }
        val tvMin = tvStats.sumOf { it.second }
        val longestTVStat = tvStats.maxByOrNull { it.second }
        val totalEpisodes = watchedTV.sumOf { it.watchedEpisodes?.values?.sumOf { it.size } ?: 0 }

        // Genres
        val genreCounts = mutableMapOf<String, Int>()
        val otherLabel = if (language.lowercase().startsWith("it")) "Altro" else "Other"
        watched.forEach { m ->
            m.genreIds?.forEach { id ->
                val defaultName = GenreConstants.ALL_GENRES.find { it.id == id.toLong() }?.name ?: otherLabel
                val genreName = GenreConstants.getLocalizedName(id.toLong(), language, defaultName)
                genreCounts[genreName] = (genreCounts[genreName] ?: 0) + 1
            }
        }

        // Decades - fill gaps to make it a proper timeline
        val years = watched.mapNotNull { m ->
            val date = m.releaseDate ?: m.firstAirDate
            if (!date.isNullOrBlank()) {
                try {
                    java.time.LocalDate.parse(date).year
                } catch (e: Exception) {
                    try {
                        java.time.Instant.parse(date).atZone(java.time.ZoneId.systemDefault()).year
                    } catch (e2: Exception) {
                        date.take(4).toIntOrNull()
                    }
                }
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
            .toImmutableList()

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
            .toImmutableList()

        val topGenre = genreCounts.entries.sortedByDescending { it.value }.firstOrNull()?.key

        return CalculatedStats(
            totalTimeFormatted = formatDuration(movieMin + tvMin, language),
            isEstimate = moviesEstimate || tvEstimate,
            moviesWatched = watchedMovies.size,
            moviesToWatch = allMovies.count { it.mediaType != "tv" && !it.watched && (it.favorite || it.reminder) },
            totalMinutes = movieMin + tvMin,
            movieMinutes = movieMin,
            movieTimeFormatted = formatDuration(movieMin, language),
            moviesEstimate = moviesEstimate,
            longestMovie = longestMovie,
            longestMovieMinutes = longestMovie?.runtime ?: 0,
            tvWatched = watchedTV.size,
            tvToWatch = allMovies.count { it.mediaType == "tv" && !it.watched && (it.favorite || it.reminder) },
            totalEpisodes = totalEpisodes,
            tvMinutes = tvMin,
            tvTimeFormatted = formatDuration(tvMin, language),
            tvEstimate = tvEstimate,
            longestTV = longestTVStat?.first,
            longestTVMinutes = longestTVStat?.second ?: 0,
            genreCounts = genreCounts.entries.sortedByDescending { it.value }.map { it.key to it.value }.toImmutableList(),
            decadeCounts = decadeCounts.entries.sortedBy { it.key }.map { it.key to it.value }.toImmutableList(),
            ratingDistribution = ratings.toList().toImmutableList(),
            topCast = topCast,
            topDirectors = topDirectors,
            topGenre = topGenre
        )
    }
}
