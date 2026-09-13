package com.cinetrack.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.domain.CycleMovieStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.domain.GetDiscoverUiStateUseCase
import javax.inject.Inject

import com.cinetrack.util.toComposeColor
import com.cinetrack.data.model.SortConfig
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.data.repository.SettingsRepository
import androidx.compose.foundation.lazy.grid.LazyGridState

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

data class DiscoverUiState(
    val movies: ImmutableList<Movie> = persistentListOf(),
    val isLoading: Boolean = true,
    val isNextPageLoading: Boolean = false,
    val isError: Boolean = false,
    val type: String = "popular",
    val genreName: String? = null,
    val currentPage: Int = 1,
    val isEndReached: Boolean = false,
    val favorites: ImmutableList<Movie> = persistentListOf(),
    val sortConfig: SortConfig = SortConfig(),
    val movieFolderColors: ImmutableMap<String, ImmutableList<String>> = persistentMapOf(),
    val folders: ImmutableList<com.cinetrack.data.local.entities.FolderEntity> = persistentListOf(),
    val preferences: com.cinetrack.data.model.UserPreferences = com.cinetrack.data.model.UserPreferences()
)


@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val repository: MovieRepository,
    private val preferenceRepository: PreferenceRepository,
    private val settingsRepository: SettingsRepository,
    private val actionFeedbackManager: ActionFeedbackManager,
    private val getDiscoverUiStateUseCase: GetDiscoverUiStateUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var type: String = "popular"
    private var genreId: Long? = null
    private var genreName: String? = null
    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _isNextPageLoading = MutableStateFlow(false)
    private val _isError = MutableStateFlow(false)
    private val _currentPage = MutableStateFlow(1)
    private val _isEndReached = MutableStateFlow(false)
    private val _sortConfig = MutableStateFlow(SortConfig())
    private var currentFetchJob: kotlinx.coroutines.Job? = null

    // State for upcoming movies fallback: Italian cinemas first, then global releases
    private var isUpcomingFallbackGlobal: Boolean = false
    private var regionalUpcomingMaxPages: Int = Int.MAX_VALUE
    private var globalUpcomingPage: Int = 1
    
    val lazyGridState = LazyGridState()
    val animatedMovieIds = mutableSetOf<String>()
    
    fun emitMessage(message: UiText) {
        actionFeedbackManager.emit(message)
    }

    val uiState: StateFlow<DiscoverUiState> = combine(
        combine(
            _movies,
            _isLoading,
            _isNextPageLoading
        ) { movies, loading, nextLoading ->
            Triple(movies, loading, nextLoading)
        },
        combine(
            _isError,
            _currentPage,
            _isEndReached
        ) { error, page, endReached ->
            Triple(error, page, endReached)
        },
        combine(
            repository.getLocalMoviesFlow(),
            _sortConfig,
            repository.getFoldersFlow(),
            preferenceRepository.userPreferencesFlow
        ) { local, sort, folders, prefs ->
            Triple(local, sort, Pair(folders, prefs))
        }
    ) { groupA, groupB, groupC ->
        getDiscoverUiStateUseCase(
            apiMovies = groupA.first,
            isLoading = groupA.second,
            isNextPageLoading = groupA.third,
            isError = groupB.first,
            currentPage = groupB.second,
            isEndReached = groupB.third,
            localMovies = groupC.first,
            sortConfig = groupC.second,
            folders = groupC.third.first,
            prefs = groupC.third.second,
            type = type,
            genreName = genreName
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = DiscoverUiState(type = type, genreName = genreName)
    )

    fun init(newType: String = "popular", newGenreId: Long? = null, newGenreName: String? = null) {
        if (this.type == newType && this.genreId == newGenreId) return
        this.type = newType
        this.genreId = newGenreId
        this.genreName = newGenreName
        _sortConfig.value = _sortConfig.value.copy(
            selectedGenres = if (newGenreId != null) listOf(newGenreId) else emptyList(),
            selectedDecades = emptyList()
        )
        fetchMovies()
    }

    private fun fetchMovies(isNextPage: Boolean = false) {
        if (_isEndReached.value && isNextPage) return
        
        currentFetchJob?.cancel()
        currentFetchJob = viewModelScope.launch {
            if (isNextPage) {
                _isNextPageLoading.value = true
            } else {
                _isLoading.value = true
                _currentPage.value = 1
                _isEndReached.value = false
                _isError.value = false
                isUpcomingFallbackGlobal = false
                regionalUpcomingMaxPages = Int.MAX_VALUE
                globalUpcomingPage = 1
            }

            try {
                val pageToFetch = if (isNextPage) _currentPage.value + 1 else 1
                val config = _sortConfig.value
                val hasCustomFilters = config.selectedGenres.isNotEmpty() || config.selectedProviders.isNotEmpty() || config.selectedDecades.isNotEmpty() || config.sortType != "created_at"

                val fetchedResults = if (hasCustomFilters || genreId != null) {
                    val options = mutableMapOf<String, String>()
                    val isTvType = type.contains("tv")
                    val today = java.time.LocalDate.now().toString()
                    val oneMonthAgo = java.time.LocalDate.now().minusMonths(1).toString()

                    // 1. Base Type Params
                    when (type) {
                        "now_playing_movies", "now_playing" -> {
                            options["with_release_type"] = "2|3"
                            options["release_date.gte"] = oneMonthAgo
                            options["release_date.lte"] = today
                        }
                        "upcoming_movies", "upcoming" -> {
                            options["primary_release_date.gte"] = today
                            options["region"] = ""
                        }
                        "airing_today_tv" -> {
                            options["air_date.gte"] = today
                            options["air_date.lte"] = today
                        }
                        "on_the_air_tv", "streaming_tv", "upcoming_tv" -> {
                            options["first_air_date.gte"] = today
                        }
                    }

                    // 2. Sort By Mapping
                    val tmdbSortBy = when (config.sortType) {
                        "release_date", "added_at", "watched_at" -> if (isTvType) "first_air_date" else "primary_release_date"
                        "vote_average", "personal_rating" -> "vote_average"
                        "title" -> "original_title"
                        "runtime" -> "revenue" // closest proxy if sorting by length isn't ideal
                        else -> "popularity"
                    }
                    options["sort_by"] = "$tmdbSortBy.${config.sortDirection}"

                    // 3. Genres
                    val combinedGenres = (config.selectedGenres + listOfNotNull(genreId)).distinct()
                    if (combinedGenres.isNotEmpty()) {
                        options["with_genres"] = combinedGenres.joinToString(",")
                    }

                    // 4. Providers
                    if (config.selectedProviders.isNotEmpty()) {
                        options["with_watch_providers"] = config.selectedProviders.joinToString("|")
                    }

                    // 5. Decades / Release months
                    if (config.selectedDecades.isNotEmpty()) {
                        if (type.contains("upcoming")) {
                            val monthKeys = config.selectedDecades.filter { it.matches(Regex("\\d{4}-\\d{2}")) }
                            val hasFuture = config.selectedDecades.contains("future")
                            val currentYearMonth = java.time.YearMonth.now()
                            val dateKey = if (isTvType) "first_air_date" else "primary_release_date"

                            if (monthKeys.isNotEmpty()) {
                                val minMonth = monthKeys.minOrNull()!!
                                val maxMonth = monthKeys.maxOrNull()!!
                                val minYearMonth = java.time.YearMonth.parse(minMonth)
                                val maxYearMonth = java.time.YearMonth.parse(maxMonth)

                                val startDate = if (minYearMonth <= currentYearMonth) today else minYearMonth.atDay(1).toString()
                                options["$dateKey.gte"] = startDate

                                if (!hasFuture) {
                                    val endDate = maxYearMonth.atEndOfMonth().toString()
                                    options["$dateKey.lte"] = endDate
                                }
                            } else if (hasFuture) {
                                val beyondYearMonth = currentYearMonth.plusMonths(12)
                                options["$dateKey.gte"] = beyondYearMonth.atDay(1).toString()
                            }
                        } else {
                            val minDecade = config.selectedDecades.mapNotNull { it.replace("s", "").toIntOrNull() }.minOrNull()
                            val maxDecade = config.selectedDecades.mapNotNull { it.replace("s", "").toIntOrNull() }.maxOrNull()
                            if (minDecade != null && maxDecade != null) {
                                val startDate = "$minDecade-01-01"
                                val endDate = "${maxDecade + 9}-12-31"
                                if (isTvType) {
                                    options["first_air_date.gte"] = startDate
                                    options["first_air_date.lte"] = endDate
                                } else {
                                    options["primary_release_date.gte"] = startDate
                                    options["primary_release_date.lte"] = endDate
                                }
                            }
                        }
                    }

                    if (isTvType) {
                        repository.discoverTVWithParams(page = pageToFetch, options = options).map { it.copy(mediaType = "tv") }
                    } else {
                        repository.discoverMoviesWithParams(page = pageToFetch, options = options).map { it.copy(mediaType = "movie") }
                    }
                } else {
                    // Fallback to specific endpoints when no custom filters
                    when (type) {
                        "popular_movies", "popular" -> repository.getPopularMovies(page = pageToFetch).map { it.copy(mediaType = "movie") }
                        "now_playing_movies", "now_playing" -> repository.getNowPlayingMovies(page = pageToFetch).map { it.copy(mediaType = "movie") }
                        "upcoming_movies", "upcoming" -> fetchUpcomingMovies(pageToFetch = pageToFetch)
                        "popular_tv" -> repository.getPopularTV(page = pageToFetch).map { it.copy(mediaType = "tv") }
                        "airing_today_tv" -> repository.getAiringTodayTV(page = pageToFetch).map { it.copy(mediaType = "tv") }
                        "on_the_air_tv", "streaming_tv" -> repository.getOnTheAirTV(page = pageToFetch).map { it.copy(mediaType = "tv") }
                        "trending_all" -> repository.getTrendingAll(page = pageToFetch) 
                        "trending_movies", "trending" -> repository.getTrendingMovies(page = pageToFetch).map { it.copy(mediaType = "movie") }
                        "trending_tv" -> repository.getTrendingTV(page = pageToFetch).map { it.copy(mediaType = "tv") }
                        else -> repository.getPopularMovies(page = pageToFetch).map { it.copy(mediaType = "movie") }
                    }
                }
                
                val today = java.time.LocalDate.now().toString()
                val processedResults = if (type.contains("upcoming")) {
                    val monthKeys = config.selectedDecades.filter { it.matches(Regex("\\d{4}-\\d{2}")) }.toSet()
                    val hasFuture = config.selectedDecades.contains("future")
                    val beyondDate = java.time.YearMonth.now().plusMonths(12).atDay(1).toString()

                    fetchedResults.filter { movie ->
                        val relDate = movie.releaseDate ?: movie.firstAirDate
                        val isNotReleased = !movie.isReleased && (relDate == null || relDate >= today)
                        if (!isNotReleased) return@filter false

                        if (config.selectedDecades.isEmpty()) {
                            true
                        } else {
                            val movieMonth = relDate?.take(7)
                            val matchesMonth = movieMonth != null && movieMonth in monthKeys
                            val matchesFuture = hasFuture && relDate != null && relDate >= beyondDate
                            if (monthKeys.isNotEmpty() && hasFuture) {
                                matchesMonth || matchesFuture
                            } else if (monthKeys.isNotEmpty()) {
                                matchesMonth
                            } else if (hasFuture) {
                                matchesFuture
                            } else {
                                true
                            }
                        }
                    }
                } else {
                    fetchedResults
                }

                val hideSaved = settingsRepository.hideSavedFromDiscovery.first()
                val localMovies = repository.getLocalMoviesFlow().first()
                val localCompositeIds = localMovies.map { "${it.mediaType}_${it.id}" }.toSet()

                val finalResults = if (hideSaved) {
                    processedResults.filter { movie ->
                        val compositeId = "${movie.mediaType}_${movie.id}"
                        !localCompositeIds.contains(compositeId)
                    }
                } else {
                    processedResults
                }

                if (finalResults.isEmpty() && fetchedResults.isNotEmpty()) {
                    // All items filtered out, auto-load next page to avoid empty screen
                    _currentPage.value = pageToFetch
                    fetchMovies(isNextPage = true)
                    return@launch
                }

                if (finalResults.isEmpty()) {
                    _isEndReached.value = true
                } else {
                    if (isNextPage) {
                        _movies.value = (_movies.value + finalResults).distinctBy { it.id }
                        _currentPage.value = pageToFetch
                    } else {
                        _movies.value = finalResults
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellation
            } catch (e: Exception) {
                e.printStackTrace()
                _isEndReached.value = true // Prevent infinite loops on error
                if (!isNextPage) {
                    _isError.value = true
                }
            } finally {
                _isLoading.value = false
                _isNextPageLoading.value = false
            }
        }
    }

    private suspend fun fetchUpcomingMovies(pageToFetch: Int): List<Movie> {
        val existingIds = _movies.value.map { it.id }.toSet()
        val today = java.time.LocalDate.now().toString()

        // Phase 1: Regional upcoming movies (e.g. Italian cinema releases)
        if (!isUpcomingFallbackGlobal) {
            val response = repository.getRegionalUpcomingMoviesResponse(page = pageToFetch)
            val regionalResults = response.results
                .map { it.copy(mediaType = "movie") }
                .filter { movie ->
                    val date = movie.releaseDate
                    !movie.isReleased && (date == null || date >= today)
                }
            val totalPages = response.totalPages ?: 1
            regionalUpcomingMaxPages = totalPages

            if (pageToFetch >= totalPages) {
                // Reached the end of regional releases; switch strategy for future pages
                isUpcomingFallbackGlobal = true
                globalUpcomingPage = 1
            }

            if (regionalResults.isNotEmpty()) {
                return regionalResults
            } else {
                // If regional results are empty, immediately switch to global fallback
                isUpcomingFallbackGlobal = true
                globalUpcomingPage = 1
            }
        }

        // Phase 2: Global upcoming movies fallback (translated in Italian / user's language)
        var results = emptyList<Movie>()
        var attempts = 0
        while (results.isEmpty() && attempts < 5) {
            attempts++
            val globalResponse = repository.getGlobalUpcomingMoviesResponse(page = globalUpcomingPage)
            val globalTotalPages = globalResponse.totalPages ?: 1
            globalUpcomingPage++

            val uniqueGlobal = globalResponse.results
                .map { it.copy(mediaType = "movie") }
                .filter { movie ->
                    val date = movie.releaseDate
                    movie.id !in existingIds && !movie.isReleased && (date == null || date >= today)
                }

            if (uniqueGlobal.isNotEmpty()) {
                results = uniqueGlobal
                break
            }

            if (globalUpcomingPage > globalTotalPages || globalResponse.results.isEmpty()) {
                _isEndReached.value = true
                break
            }
        }

        return results
    }

    fun loadNextPage() {
        if (!_isNextPageLoading.value && !_isLoading.value && !_isEndReached.value) {
            fetchMovies(isNextPage = true)
        }
    }

    fun retry() {
        fetchMovies(isNextPage = false)
    }

    fun toggleFavorite(movie: Movie) {
        val title = movie.title ?: movie.name ?: ""
        viewModelScope.launch {
            // 1. Fetch current database state to have an accurate baseline
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            val previousState = current.copy()

            // 2. IDEMPOTENCY CHECK: If already watched, do nothing (as requested)
            if (current.watched) {
                return@launch
            }

            // 3. Perform the cycle
            cycleMovieStatusUseCase(current)

            // 4. Re-fetch current state to determine what happened for feedback
            val updated = repository.getMovie(movie.id, movie.mediaType)
            val actionMsgRes = when {
                updated == null -> R.string.msg_action_removed
                updated.watched -> R.string.msg_action_watched
                updated.favorite -> R.string.msg_action_favorite
                updated.reminder -> R.string.msg_action_reminder
                else -> R.string.msg_action_updated
            }
            
            actionFeedbackManager.emit(UiText.StringResource(actionMsgRes, title)) {
                repository.saveMovie(previousState)
            }
        }
    }

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch {
            repository.deleteMovie(movie)
            actionFeedbackManager.emit(UiText.StringResource(R.string.msg_item_removed, movie.title ?: movie.name ?: "")) {
                repository.saveMovie(movie)
            }
        }
    }

    fun updateRating(movie: Movie, rating: Double) {
        viewModelScope.launch {
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            repository.saveMovie(current.copy(personalRating = rating, votedAt = System.currentTimeMillis()))
        }
    }

    fun updateNote(movie: Movie, note: String) {
        viewModelScope.launch {
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            repository.saveMovie(current.copy(personalNote = note))
        }
    }

    fun getMovieFolderColors(movie: Movie): List<androidx.compose.ui.graphics.Color> {
        val compositeId = "${movie.mediaType}_${movie.id}"
        val colorHexes = uiState.value.movieFolderColors[compositeId] ?: emptyList()
        return colorHexes.map { it.toComposeColor() }
    }

    fun isItemInFolder(movie: Movie, folderId: String): Boolean {
        val folder = uiState.value.folders.find { it.id == folderId }
        val compositeId = "${movie.mediaType}_${movie.id}"
        return folder?.itemIds?.contains(compositeId) == true
    }

    fun toggleItemInFolder(folder: com.cinetrack.data.local.entities.FolderEntity, movie: Movie) {
        viewModelScope.launch {
            val compositeId = "${movie.mediaType}_${movie.id}"
            val newItemIds = if (folder.itemIds.contains(compositeId)) {
                folder.itemIds - compositeId
            } else {
                folder.itemIds + compositeId
            }
            repository.saveFolder(folder.copy(itemIds = newItemIds, updatedAt = java.time.Instant.now().toString()))
            
            val local = repository.getMovie(movie.id, movie.mediaType)
            if (local == null) {
                repository.saveMovie(movie)
            }
        }
    }

    fun updateSortConfig(config: SortConfig) {
        _sortConfig.value = config
        _movies.value = emptyList()
        _currentPage.value = 1
        _isEndReached.value = false
        fetchMovies(isNextPage = false)
    }

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch {
            preferenceRepository.updateGridColumns(columns)
            repository.savePreferencesRemote(uiState.value.preferences.copy(gridColumns = columns))
        }
    }
}
