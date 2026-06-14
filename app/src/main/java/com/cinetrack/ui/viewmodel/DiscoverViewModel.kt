package com.cinetrack.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
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
import com.cinetrack.data.models.SortConfig
import com.cinetrack.data.repository.PreferenceRepository
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
    val type: String = "popular",
    val genreName: String? = null,
    val currentPage: Int = 1,
    val isEndReached: Boolean = false,
    val favorites: ImmutableList<Movie> = persistentListOf(),
    val sortConfig: SortConfig = SortConfig(),
    val movieFolderColors: ImmutableMap<String, ImmutableList<String>> = persistentMapOf(),
    val folders: ImmutableList<com.cinetrack.data.local.entities.FolderEntity> = persistentListOf(),
    val preferences: com.cinetrack.data.models.UserPreferences = com.cinetrack.data.models.UserPreferences()
)


@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val repository: MovieRepository,
    private val preferenceRepository: PreferenceRepository,
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
    private val _currentPage = MutableStateFlow(1)
    private val _isEndReached = MutableStateFlow(false)
    private val _sortConfig = MutableStateFlow(SortConfig())
    
    val lazyGridState = LazyGridState()
    val animatedMovieIds = mutableSetOf<String>()
    
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
        getDiscoverUiStateUseCase(
            apiMovies = groupA.first,
            isLoading = groupA.second.first,
            isNextPageLoading = groupA.second.second,
            currentPage = groupA.second.third,
            isEndReached = groupA.third,
            localMovies = groupB.first,
            sortConfig = groupB.second,
            folders = groupB.third.first,
            prefs = groupB.third.second,
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
            selectedGenres = if (newGenreId != null) listOf(newGenreId) else emptyList()
        )
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
                val currentGenreId = genreId
                val fetchedResults = if (currentGenreId != null) {
                    // Fetch by genre if provided in route
                    repository.getMoviesByGenre(currentGenreId, page = pageToFetch).map { it.copy(mediaType = "movie") }
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
            cycleMovieStatusUseCase(current)

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
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            repository.saveMovie(current.copy(personalRating = rating))
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
        }
    }

    fun updateSortConfig(config: SortConfig) {
        _sortConfig.value = config
    }

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch {
            preferenceRepository.updateGridColumns(columns)
            repository.savePreferencesRemote(uiState.value.preferences.copy(gridColumns = columns))
        }
    }
}
