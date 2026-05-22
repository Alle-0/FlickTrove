package com.cinetrack.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.cinetrack.ui.utils.ActionFeedbackManager
import javax.inject.Inject
import com.cinetrack.ui.navigation.DiscoverRoute
import com.cinetrack.util.toComposeColor
import androidx.navigation.toRoute
import com.cinetrack.data.models.SortConfig
import com.cinetrack.data.repository.PreferenceRepository

data class DiscoverUiState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = true,
    val isNextPageLoading: Boolean = false,
    val type: String = "popular",
    val genreName: String? = null,
    val currentPage: Int = 1,
    val isEndReached: Boolean = false,
    val favorites: List<Movie> = emptyList(),
    val sortConfig: SortConfig = SortConfig(),
    val movieFolderColors: Map<String, List<String>> = emptyMap(),
    val folders: List<com.cinetrack.data.local.entities.FolderEntity> = emptyList(),
    val preferences: com.cinetrack.data.models.UserPreferences = com.cinetrack.data.models.UserPreferences()
)


@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val preferenceRepository: PreferenceRepository,
    private val actionFeedbackManager: ActionFeedbackManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<DiscoverRoute>()
    private val type: String = route.type
    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _isNextPageLoading = MutableStateFlow(false)
    private val _currentPage = MutableStateFlow(1)
    private val _isEndReached = MutableStateFlow(false)
    private val _sortConfig = MutableStateFlow(SortConfig())
    
    fun emitMessage(message: String) {
        actionFeedbackManager.emit(message)
    }

    val uiState: StateFlow<DiscoverUiState> = combine(
        combine(
            _movies,
            _isLoading,
            _isNextPageLoading,
            _currentPage,
            _isEndReached
        ) { movies, loading, nextLoading, page, endReached ->
            Triple(movies, Triple(loading, nextLoading, page), endReached)
        },
        combine(
            repository.getLocalMoviesFlow(),
            _sortConfig,
            repository.getFoldersFlow(),
            preferenceRepository.userPreferencesFlow
        ) { local, sort, folders, prefs ->
            Triple(local, sort, Pair(folders, prefs))
        }
    ) { groupA, groupB ->
        val apiMovies = groupA.first
        val (isLoading, isNextPageLoading, currentPage) = groupA.second
        val isEndReached = groupA.third
        
        val localMovies = groupB.first
        val sortConfig = groupB.second
        val (folders, prefs) = groupB.third

        // Sync api movies with local state
        val syncedMovies = apiMovies.map { apiMovie ->
            localMovies.find { it.id == apiMovie.id && it.mediaType == apiMovie.mediaType } ?: apiMovie
        }
        
        // Apply local filtering
        val filteredMovies = syncedMovies.filter { movie ->
            val matchesGenres = if (sortConfig.selectedGenres.isEmpty()) true 
                else movie.genreIds?.any { genreId -> sortConfig.selectedGenres.contains(genreId.toLong()) } ?: false
            
            val matchesProviders = if (sortConfig.selectedProviders.isEmpty()) true
                else movie.streamingProviderIds?.any { providerId -> sortConfig.selectedProviders.contains(providerId) } ?: false
                
            val matchesDecade = if (sortConfig.selectedDecades.isEmpty()) true
                else {
                    val year = movie.releaseDate?.split("-")?.firstOrNull()?.toIntOrNull()
                    if (year != null) {
                        sortConfig.selectedDecades.any { decade ->
                            try {
                                val decadeStart = decade.toInt()
                                year in decadeStart..(decadeStart + 9)
                            } catch (e: Exception) {
                                false
                            }
                        }
                    } else false
                }
                
            val isUpcomingSection = type.contains("upcoming") || type.contains("airing_today") || type.contains("on_the_air")
            val meetsReleaseCriteria = isUpcomingSection || movie.isReleased
                
            matchesGenres && matchesProviders && matchesDecade && meetsReleaseCriteria
        }
        
        // Apply local sorting
        val sortedMovies = when (sortConfig.sortType) {
            "release_date" -> {
                if (sortConfig.sortDirection == "desc") filteredMovies.sortedByDescending { it.releaseDate ?: "" }
                else filteredMovies.sortedBy { it.releaseDate ?: "9999-99-99" }
            }
            "vote_average" -> {
                if (sortConfig.sortDirection == "desc") filteredMovies.sortedByDescending { it.voteAverage ?: 0.0 }
                else filteredMovies.sortedBy { it.voteAverage ?: 10.0 }
            }
            "title" -> {
                if (sortConfig.sortDirection == "desc") filteredMovies.sortedByDescending { it.title ?: it.name ?: "" }
                else filteredMovies.sortedBy { it.title ?: it.name ?: "" }
            }
            else -> filteredMovies
        }

        val movieFolderColors = mutableMapOf<String, List<String>>()
        folders.forEach { folder ->
            val color = folder.color ?: "#FFFFFF"
            folder.itemIds.forEach { itemId ->
                val colors = movieFolderColors.getOrDefault(itemId, emptyList())
                movieFolderColors[itemId] = colors + color
            }
        }

        DiscoverUiState(
            movies = sortedMovies,
            isLoading = isLoading,
            isNextPageLoading = isNextPageLoading,
            type = type,
            genreName = route.genreName,
            currentPage = currentPage,
            isEndReached = isEndReached,
            favorites = localMovies,
            sortConfig = sortConfig,
            movieFolderColors = movieFolderColors,
            folders = folders,
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = DiscoverUiState(type = type, genreName = route.genreName)
    )

    init {
        // If a genreId was passed via navigation, set it in the filter state
        route.genreId?.let { gid ->
            _sortConfig.value = _sortConfig.value.copy(
                selectedGenres = listOf(gid)
            )
        }
        fetchMovies()
    }

    private fun fetchMovies(isNextPage: Boolean = false) {
        if (_isEndReached.value && isNextPage) return
        
        viewModelScope.launch {
            if (isNextPage) {
                _isNextPageLoading.value = true
            } else {
                _isLoading.value = true
                _currentPage.value = 1
                _isEndReached.value = false
            }

            try {
                val pageToFetch = if (isNextPage) _currentPage.value + 1 else 1
                val fetchedResults = if (route.genreId != null) {
                    // Fetch by genre if provided in route
                    repository.getMoviesByGenre(route.genreId, page = pageToFetch).map { it.copy(mediaType = "movie") }
                } else {
                    when (type) {
                        "popular_movies", "popular" -> repository.getPopularMovies(page = pageToFetch).map { it.copy(mediaType = "movie") }
                        "now_playing_movies", "now_playing" -> repository.getNowPlayingMovies(page = pageToFetch).map { it.copy(mediaType = "movie") }
                        "upcoming_movies", "upcoming" -> repository.getUpcomingMovies(page = pageToFetch).map { it.copy(mediaType = "movie") }
                        "popular_tv" -> repository.getPopularTV(page = pageToFetch).map { it.copy(mediaType = "tv") }
                        "airing_today_tv" -> repository.getAiringTodayTV(page = pageToFetch).map { it.copy(mediaType = "tv") }
                        "on_the_air_tv", "streaming_tv" -> repository.getOnTheAirTV(page = pageToFetch).map { it.copy(mediaType = "tv") }
                        "trending_all" -> repository.getTrendingAll(page = pageToFetch) // Uses its own mapping logic
                        "trending_movies", "trending" -> repository.getTrendingMovies(page = pageToFetch).map { it.copy(mediaType = "movie") }
                        "trending_tv" -> repository.getTrendingTV(page = pageToFetch).map { it.copy(mediaType = "tv") }
                        else -> repository.getPopularMovies(page = pageToFetch).map { it.copy(mediaType = "movie") }
                    }
                }
                
                if (fetchedResults.isEmpty()) {
                    _isEndReached.value = true
                } else {
                    if (isNextPage) {
                        _movies.value = (_movies.value + fetchedResults).distinctBy { it.id }
                        _currentPage.value = pageToFetch
                    } else {
                        _movies.value = fetchedResults
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isEndReached.value = true // Prevent infinite loops on error
            } finally {
                _isLoading.value = false
                _isNextPageLoading.value = false
            }
        }
    }

    fun loadNextPage() {
        if (!_isNextPageLoading.value && !_isLoading.value && !_isEndReached.value) {
            fetchMovies(isNextPage = true)
        }
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
            repository.cycleMovieStatus(current)

            // 4. Re-fetch current state to determine what happened for feedback
            val updated = repository.getMovie(movie.id, movie.mediaType)
            val actionLabel = when {
                updated == null -> "rimosso"
                updated.watched -> "segnato come visto"
                updated.favorite -> "aggiunto a Da Vedere"
                updated.reminder -> "promemoria impostato"
                else -> "aggiornato"
            }

            actionFeedbackManager.emit("\"$title\" $actionLabel") {
                repository.saveMovie(previousState)
            }
        }
    }

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch {
            repository.deleteMovie(movie)
            actionFeedbackManager.emit("\"${movie.title ?: movie.name}\" rimosso") {
                repository.saveMovie(movie)
            }
        }
    }

    fun updateRating(movie: Movie, rating: Double) {
        viewModelScope.launch {
            repository.saveMovie(movie.copy(personalRating = rating))
        }
    }

    fun updateNote(movie: Movie, note: String) {
        viewModelScope.launch {
            repository.saveMovie(movie.copy(personalNote = note))
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
        }
    }

    fun updateSortConfig(config: SortConfig) {
        _sortConfig.value = config
    }
}
