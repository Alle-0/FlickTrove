package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.models.SortConfig
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.cinetrack.ui.utils.ActionFeedbackManager
import javax.inject.Inject

data class VistiUiState(
    val movies: List<Movie> = emptyList(),
    val movieCount: Int = 0,
    val tvCount: Int = 0,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val activeTab: String = "movie",
    val movieFolderColors: Map<String, List<String>> = emptyMap(),
    val folders: List<com.cinetrack.data.local.entities.FolderEntity> = emptyList(),
    val sortConfig: SortConfig = SortConfig(),
    val preferences: com.cinetrack.data.models.UserPreferences = com.cinetrack.data.models.UserPreferences()
)

@HiltViewModel
class VistiViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val preferenceRepository: PreferenceRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeTab = MutableStateFlow("movie")
    
    fun emitMessage(message: String) {
        actionFeedbackManager.emit(message)
    }

    val uiState: StateFlow<VistiUiState> = combine(
        repository.getLocalMoviesFlow(),
        repository.getFoldersFlow(),
        preferenceRepository.userPreferencesFlow,
        _searchQuery,
        _activeTab
    ) { movies, folders, prefs, query, tab ->
        val watchedMovies = movies.filter { it.watched }.distinctBy { "${it.id}_${it.mediaType}" }
        val movieCount = watchedMovies.count { it.mediaType != "tv" }
        val tvCount = watchedMovies.count { it.mediaType == "tv" }
        
        val filtered = watchedMovies.filter { movie ->
            val matchesTab = if (tab == "movie") movie.mediaType != "tv" else movie.mediaType == "tv"
            val matchesSearch = query.isEmpty() || 
                (movie.title?.contains(query, ignoreCase = true) ?: false) ||
                (movie.name?.contains(query, ignoreCase = true) ?: false)
            
            val matchesGenre = prefs.vistiSort.selectedGenres.isEmpty() || 
                (movie.genreIds?.any { it in prefs.vistiSort.selectedGenres } ?: false)
            
            val matchesDecade = prefs.vistiSort.selectedDecades.isEmpty() || 
                prefs.vistiSort.selectedDecades.any { decade ->
                    val prefix = decade.substring(0, 3)
                    (movie.releaseDate?.startsWith(prefix) ?: false) ||
                    (movie.firstAirDate?.startsWith(prefix) ?: false)
                }

            val matchesProvider = prefs.vistiSort.selectedProviders.isEmpty() || 
                (movie.streamingProviderIds?.any { it in prefs.vistiSort.selectedProviders } ?: false)
            
            matchesTab && matchesSearch && matchesGenre && matchesDecade && matchesProvider
        }

        val sorted = sortMovies(filtered, prefs.vistiSort)

        val movieFolderColors = mutableMapOf<String, List<String>>()
        folders.forEach { folder ->
            val color = folder.color ?: "#FFFFFF"
            folder.itemIds.forEach { itemId ->
                val colors = movieFolderColors.getOrDefault(itemId, emptyList())
                movieFolderColors[itemId] = colors + color
            }
        }

        VistiUiState(
            movies = sorted,
            movieCount = movieCount,
            tvCount = tvCount,
            isLoading = false,
            searchQuery = query,
            activeTab = tab,
            movieFolderColors = movieFolderColors,
            folders = folders,
            sortConfig = prefs.vistiSort,
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = VistiUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTabChanged(tab: String) {
        _activeTab.value = tab
    }

    fun updateSortConfig(config: SortConfig) {
        viewModelScope.launch {
            preferenceRepository.updateVistiSort(config)
            repository.savePreferencesRemote(uiState.value.preferences.copy(vistiSort = config))
        }
    }

    fun toggleWatched(movie: Movie) {
        val title = movie.title ?: movie.name ?: ""
        viewModelScope.launch {
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            val previousState = current.copy()

            // IDEMPOTENCY CHECK: If already watched, do nothing
            if (current.watched) {
                return@launch
            }

            repository.cycleMovieStatus(current)

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

    private fun sortMovies(movies: List<Movie>, sort: SortConfig): List<Movie> {
        val isDesc = sort.sortDirection == "desc"
        return when (sort.sortType) {
            "watched_at" -> {
                if (isDesc) {
                    movies.sortedByDescending { it.watchedAt }
                } else {
                    movies.sortedBy { it.watchedAt }
                }
            }
            "release_date" -> {
                if (isDesc) {
                    movies.sortedByDescending { it.releaseDate ?: it.firstAirDate ?: "" }
                } else {
                    movies.sortedBy { it.releaseDate ?: it.firstAirDate ?: "" }
                }
            }
            "title" -> {
                if (isDesc) {
                    movies.sortedByDescending { it.title ?: it.name ?: "" }
                } else {
                    movies.sortedBy { it.title ?: it.name ?: "" }
                }
            }
            "added_at", "created_at" -> {
                if (isDesc) {
                    movies.sortedByDescending { it.clientUpdatedAt }
                } else {
                    movies.sortedBy { it.clientUpdatedAt }
                }
            }
            "vote_average" -> {
                if (isDesc) {
                    movies.sortedByDescending { it.voteAverage ?: 0.0 }
                } else {
                    movies.sortedBy { it.voteAverage ?: 0.0 }
                }
            }
            "personal_rating" -> {
                if (isDesc) {
                    movies.sortedByDescending { it.personalRating ?: 0.0 }
                } else {
                    movies.sortedBy { it.personalRating ?: 0.0 }
                }
            }
            "runtime" -> {
                if (isDesc) {
                    movies.sortedByDescending { getMovieDuration(it) }
                } else {
                    movies.sortedBy { getMovieDuration(it) }
                }
            }
            else -> movies
        }
    }

    private fun getMovieDuration(movie: Movie): Int {
        return if (movie.mediaType == "tv") {
            val avgRuntime = movie.episodeRunTime?.average()?.toInt() ?: 0
            val totalEpisodes = movie.numberOfEpisodes ?: 0
            avgRuntime * totalEpisodes
        } else {
            movie.runtime ?: 0
        }
    }
}
