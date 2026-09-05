package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    val includeRewatches: Boolean = false,
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
    val countryCounts: ImmutableList<Pair<String, Int>>,
    val topGenre: String?
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val preferenceRepository: com.cinetrack.data.repository.PreferenceRepository
) : ViewModel() {

    val scrollState = androidx.compose.foundation.ScrollState(0)

    private val _timeRange = MutableStateFlow<TimeRange>(
        TimeRange.AllTime
    )

    private val _includeRewatches = MutableStateFlow(false)

    fun toggleIncludeRewatches(include: Boolean) {
        _includeRewatches.value = include
    }

    val uiState: StateFlow<StatsUiState> = combine(
        repository.getLocalMoviesFlow(),
        repository.getAllWatchHistoryFlow(),
        _timeRange,
        _includeRewatches,
        preferenceRepository.userPreferencesFlow.map { it.contentLanguage }.distinctUntilChanged()
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val movies = args[0] as List<Movie>
        @Suppress("UNCHECKED_CAST")
        val watchHistory = args[1] as List<com.cinetrack.data.local.entities.WatchHistoryEntity>
        val range = args[2] as TimeRange
        val includeRewatches = args[3] as Boolean
        val contentLanguage = args[4] as String

        val watchedMovies = movies.filter { movie -> 
            movie.watched || 
            (movie.mediaType == "tv" && (movie.dropped || !movie.watchedEpisodes.isNullOrEmpty())) ||
            watchHistory.any { it.movieId == movie.id }
        }
        
        // Build the list of years from watchedAt (movies) and watch_history.
        val years = if (includeRewatches) {
            val historyYears = watchHistory.mapNotNull {
                try {
                    java.time.Instant.parse(it.watchedAt).atZone(java.time.ZoneId.systemDefault()).year
                } catch (e: Exception) { null }
            }
            val tvYears = watchedMovies.filter { it.mediaType == "tv" }.mapNotNull { movie ->
                movie.watchedAt?.let {
                    try { java.time.Instant.parse(it).atZone(java.time.ZoneId.systemDefault()).year } catch (e: Exception) { null }
                }
            }
            (historyYears + tvYears).distinct().sortedDescending()
        } else {
            watchedMovies.mapNotNull { movie ->
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
        }

        val filteredMovies = when (range) {
            is TimeRange.AllTime -> watchedMovies
            is TimeRange.Year -> watchedMovies.filter { movie ->
                if (includeRewatches) {
                    val watchedDate = movie.watchedAt
                    // Check if it was watched in this year according to history, OR fallback to main watchedAt date
                    watchHistory.any { it.movieId == movie.id && it.watchedAt.startsWith(range.year.toString()) } ||
                    (!watchedDate.isNullOrBlank() && watchedDate.startsWith(range.year.toString()))
                } else {
                    val watchedDate = movie.watchedAt
                    !watchedDate.isNullOrBlank() && watchedDate.startsWith(range.year.toString())
                }
            }
        }
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val currentYearMovies = watchedMovies.filter { movie ->
            if (includeRewatches) {
                val watchedDate = movie.watchedAt
                watchHistory.any { it.movieId == movie.id && it.watchedAt.startsWith(currentYear.toString()) } ||
                (!watchedDate.isNullOrBlank() && watchedDate.startsWith(currentYear.toString()))
            } else {
                val watchedDate = movie.watchedAt
                !watchedDate.isNullOrBlank() && watchedDate.startsWith(currentYear.toString())
            }
        }

        val lang = if (contentLanguage == "system") java.util.Locale.getDefault().language else contentLanguage

        val filteredHistory = when (range) {
            is TimeRange.AllTime -> watchHistory
            is TimeRange.Year -> watchHistory.filter { it.watchedAt.startsWith(range.year.toString()) }
        }
        val currentYearHistory = watchHistory.filter { it.watchedAt.startsWith(currentYear.toString()) }

        StatsUiState(
            stats = calculateStats(filteredMovies, movies, lang, if (includeRewatches) filteredHistory else null, watchHistory),
            currentYearStats = calculateStats(currentYearMovies, movies, lang, if (includeRewatches) currentYearHistory else null, watchHistory),
            timeRange = range,
            includeRewatches = includeRewatches,
            availableYears = years.toImmutableList(),
            moviesInSelectedRange = filteredMovies.toImmutableList(),
            isLoading = false
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
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

    private suspend fun calculateStats(filteredWatched: List<Movie>, allMovies: List<Movie>, language: String, filteredHistory: List<com.cinetrack.data.local.entities.WatchHistoryEntity>? = null, allHistory: List<com.cinetrack.data.local.entities.WatchHistoryEntity> = emptyList()): CalculatedStats {
        val watched = filteredWatched.filter { it.watched || (it.mediaType == "tv" && (it.dropped || !it.watchedEpisodes.isNullOrEmpty())) }
        
        // If history is provided, we expand the movies based on how many times they were watched
        val watchedMoviesUnique = watched.filter { it.mediaType != "tv" }
        val watchedMovies = if (filteredHistory != null) {
            watchedMoviesUnique.flatMap { movie ->
                val historyCount = filteredHistory.count { it.movieId == movie.id }
                val totalCount = maxOf(1, historyCount)
                List(totalCount) { movie }
            }
        } else {
            watchedMoviesUnique
        }
        
        val watchedTVUnique = watched.filter { it.mediaType == "tv" }
        val watchedTV = if (filteredHistory != null) {
            watchedTVUnique.flatMap { m ->
                val historyCount = filteredHistory.count { it.movieId == m.id }
                val totalHistoryCount = allHistory.count { it.movieId == m.id }
                
                val fullWatchCopy = m.copy(watched = true, watchedEpisodes = emptyMap(), dropped = false)
                val result = mutableListOf<Movie>()
                
                for (i in 0 until historyCount) {
                    result.add(fullWatchCopy)
                }
                
                if (m.watched) {
                    if (totalHistoryCount == 0) {
                        result.add(m)
                    }
                } else {
                    val currentEps = m.watchedEpisodes?.values?.sumOf { it.size } ?: 0
                    if (currentEps > 0 || totalHistoryCount == 0) {
                        result.add(m)
                    }
                }
                
                result
            }
        } else {
            watchedTVUnique.map { m ->
                if (allHistory.any { it.movieId == m.id }) {
                    m.copy(watched = true, watchedEpisodes = emptyMap(), dropped = false)
                } else {
                    m
                }
            }
        }

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
            val watchedCount = m.watchedEpisodes?.values?.sumOf { it.size }?.takeIf { it > 0 }
                ?: if (m.watched) (m.numberOfEpisodes?.takeIf { it > 0 } ?: m.seasons?.filter { (it.seasonNumber ?: 0) > 0 }?.sumOf { it.episodeCount ?: 0 }?.takeIf { it > 0 } ?: 0) else 0
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
        val totalEpisodes = watchedTV.sumOf { m ->
            m.watchedEpisodes?.values?.sumOf { it.size }?.takeIf { it > 0 }
                ?: if (m.watched) (m.numberOfEpisodes?.takeIf { it > 0 } ?: m.seasons?.filter { (it.seasonNumber ?: 0) > 0 }?.sumOf { it.episodeCount ?: 0 }?.takeIf { it > 0 } ?: 0) else 0
        }

        // We combine the expanded movies and unique TV shows for genres, decades, cast, etc.
        val combinedWatched = watchedMovies + watchedTV

        // Genres
        val genreCounts = mutableMapOf<String, Int>()
        val otherLabel = if (language.lowercase().startsWith("it")) "Altro" else "Other"
        combinedWatched.forEach { m ->
            m.genreIds?.forEach { id ->
                val defaultName = GenreConstants.ALL_GENRES.find { it.id == id.toLong() }?.name ?: otherLabel
                val genreName = GenreConstants.getLocalizedName(id.toLong(), language, defaultName)
                genreCounts[genreName] = (genreCounts[genreName] ?: 0) + 1
            }
        }

        // Decades - fill gaps to make it a proper timeline
        val years = combinedWatched.mapNotNull { m ->
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
        watched.forEach { m -> // Keep ratings for unique movies to not skew the graph with rewatches of same rating
            val rating = m.personalRating ?: 0.0
            if (rating > 0) {
                val bucket = (rating * 2.0).toInt().coerceIn(1, 20) - 1
                ratings[bucket]++
            }
        }

        // Top Cast
        data class CastAccum(val name: String, var profilePath: String?, var count: Int)
        val castMap = mutableMapOf<Long, CastAccum>()
        watched.forEach { m ->
            m.topCastData?.forEach { person ->
                if (person.name.isNotBlank()) {
                    val existing = castMap[person.id]
                    if (existing == null) {
                        castMap[person.id] = CastAccum(person.name, person.profilePath, 1)
                    } else {
                        existing.count++
                        if (existing.profilePath.isNullOrBlank() && !person.profilePath.isNullOrBlank()) {
                            existing.profilePath = person.profilePath
                        }
                    }
                }
            }
        }
        val topCast = coroutineScope {
            castMap.entries
                .sortedByDescending { it.value.count }
                .take(50)
                .map { (id, accum) ->
                    async {
                        val cachedProfile = movieRepository.getCachedPersonProfilePath(id)
                        PersonStat(
                            id = id,
                            name = accum.name,
                            count = accum.count,
                            profilePath = cachedProfile ?: accum.profilePath
                        )
                    }
                }
                .awaitAll()
                .toImmutableList()
        }
        // Top Directors
        data class DirAccum(val name: String, var profilePath: String?, var count: Int)
        val directorMap = mutableMapOf<Long, DirAccum>()
        combinedWatched.forEach { m ->
            val dirId = m.directorId ?: return@forEach
            val dirName = m.directorName ?: return@forEach
            if (dirName.isBlank()) return@forEach
            val existing = directorMap[dirId]
            if (existing == null) {
                directorMap[dirId] = DirAccum(dirName, m.directorProfilePath, 1)
            } else {
                existing.count++
                if (existing.profilePath.isNullOrBlank() && !m.directorProfilePath.isNullOrBlank()) {
                    existing.profilePath = m.directorProfilePath
                }
            }
        }
        val topDirectors = kotlinx.coroutines.coroutineScope {
            directorMap.values.sortedByDescending { it.count }.take(10).map { accum ->
                kotlinx.coroutines.async {
                    val id = directorMap.entries.find { entry -> entry.value == accum }?.key ?: 0
                    val cachedPath = repository.getCachedPersonProfilePath(id)
                    PersonStat(
                        id = id,
                        name = accum.name,
                        profilePath = cachedPath ?: accum.profilePath,
                        count = accum.count
                    )
                }
            }.awaitAll().toImmutableList()
        }
        
        // Countries
        val countryCounts = mutableMapOf<String, Int>()
        combinedWatched.forEach { m ->
            m.originCountry?.forEach { country ->
                countryCounts[country] = (countryCounts[country] ?: 0) + 1
            }
        }
        val sortedCountryCounts = countryCounts.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
            .toImmutableList()

        val topGenre = genreCounts.entries.sortedByDescending { it.value }.firstOrNull()?.key

        return CalculatedStats(
            totalTimeFormatted = formatDuration(movieMin + tvMin, language),
            isEstimate = moviesEstimate || tvEstimate,
            moviesWatched = watchedMovies.size,
            moviesToWatch = allMovies.count { it.mediaType != "tv" && !it.watched && !it.dropped && (it.favorite || it.reminder) },
            totalMinutes = movieMin + tvMin,
            movieMinutes = movieMin,
            movieTimeFormatted = formatDuration(movieMin, language),
            moviesEstimate = moviesEstimate,
            longestMovie = longestMovie,
            longestMovieMinutes = longestMovie?.runtime ?: 0,
            tvWatched = watchedTV.size,
            tvToWatch = allMovies.count { it.mediaType == "tv" && !it.watched && !it.dropped && (it.favorite || it.reminder) },
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
            countryCounts = sortedCountryCounts,
            topGenre = topGenre
        )
    }
}
