package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.models.DiscoveryFilters
import com.cinetrack.data.models.SortConfig
import com.cinetrack.data.models.UserPreferences
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.domain.UpdateEpisodesUseCase
import com.cinetrack.ui.utils.ActionFeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<Movie> = emptyList(),
    val watchlistMovies: List<Movie> = emptyList(),
    val watchlistTV: List<Movie> = emptyList(),
    val seenMovies: List<Movie> = emptyList(),
    val seenTV: List<Movie> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val syncingIds: Set<Long> = emptySet(),
    val updatesCount: Int = 0,
    val userCountry: String = Locale.getDefault().country
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val preferenceRepository: PreferenceRepository,
    private val updateEpisodesUseCase: UpdateEpisodesUseCase,
    private val actionFeedbackManager: ActionFeedbackManager
) : ViewModel() {

    private val _syncingIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<FavoritesUiState> = combine(
        movieRepository.getLocalMoviesFlow(),
        preferenceRepository.userPreferencesFlow,
        _syncingIds
    ) { movies, prefs, syncingIds ->
        val watchlist = movies.filter { it.favorite && !it.watched }
        val seen = movies.filter { it.watched }

        FavoritesUiState(
            favorites = movies,
            watchlistMovies = watchlist.filter { it.mediaType == "movie" },
            watchlistTV = watchlist.filter { it.mediaType == "tv" },
            seenMovies = seen.filter { it.mediaType == "movie" },
            seenTV = seen.filter { it.mediaType == "tv" },
            preferences = prefs,
            isLoading = false,
            syncingIds = syncingIds,
            updatesCount = watchlist.count { (it.newEpisodesFound ?: 0) > 0 }
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Protect battery - Stop activity when inactive
        initialValue = FavoritesUiState()
    )

    init {
        viewModelScope.launch {
            // "Boot Sequence": Room is already observed, now trigger remote sync in background.
            // This is fire-and-forget; the Flow observation will pick up changes.
            movieRepository.syncWithFirebase()
        }
    }

    // --- Media Library Actions ---

    fun toggleFavorite(movie: Movie) {
        if (_syncingIds.value.contains(movie.id)) return
        
        val title = movie.title ?: movie.name ?: ""
        val previousState = movie.copy()
        viewModelScope.launch {
            _syncingIds.update { it + movie.id }
            try {
                // 1. Fetch current database state to have an accurate baseline
                val local = movieRepository.getMovie(movie.id, movie.mediaType)
                val current = local ?: movie
                val previousState = current.copy()

                // 2. IDEMPOTENCY CHECK: If already watched, do nothing
                if (current.watched) {
                    return@launch
                }

                // 3. Perform the cycle
                movieRepository.cycleMovieStatus(current)
                
                val updated = movieRepository.getMovie(movie.id, movie.mediaType)
                val actionLabel = when {
                    updated == null -> "rimosso"
                    updated.watched -> "segnato come visto"
                    updated.favorite -> "aggiunto a Da Vedere"
                    updated.reminder -> "promemoria impostato"
                    else -> "aggiornato"
                }
                
                actionFeedbackManager.emit("\"$title\" $actionLabel") {
                    movieRepository.saveMovie(previousState)
                }
            } finally {
                _syncingIds.update { it - movie.id }
            }
        }
    }

    fun toggleWatched(movie: Movie) {
        if (_syncingIds.value.contains(movie.id)) return
        
        val title = movie.title ?: movie.name ?: ""
        viewModelScope.launch {
            _syncingIds.update { it + movie.id }
            try {
                // 1. Fetch current database state to have an accurate baseline
                val local = movieRepository.getMovie(movie.id, movie.mediaType)
                val current = local ?: movie
                val previousState = current.copy()

                // 2. IDEMPOTENCY CHECK: If already watched, do nothing
                if (current.watched) {
                    return@launch
                }

                // 3. Perform the cycle
                movieRepository.cycleMovieStatus(current)
                
                val updated = movieRepository.getMovie(movie.id, movie.mediaType)
                val actionLabel = when {
                    updated == null -> "rimosso"
                    updated.watched -> "segnato come visto"
                    updated.favorite -> "aggiunto a Da Vedere"
                    updated.reminder -> "promemoria impostato"
                    else -> "aggiornato"
                }
                
                actionFeedbackManager.emit("\"$title\" $actionLabel") {
                    movieRepository.saveMovie(previousState)
                }
            } finally {
                _syncingIds.update { it - movie.id }
            }
        }
    }

    fun updateRating(movie: Movie, rating: Double?) {
        viewModelScope.launch {
            val updated = movie.copy(personalRating = rating)
            movieRepository.saveMovie(updated)
        }
    }

    fun updateNote(movie: Movie, note: String?) {
        viewModelScope.launch {
            val updated = movie.copy(personalNote = note)
            movieRepository.saveMovie(updated)
        }
    }

    fun updateEpisodes(movie: Movie, seasonNum: Int, episodeNums: List<Int>) {
        viewModelScope.launch {
            val updated = updateEpisodesUseCase(movie, seasonNum, episodeNums)
            movieRepository.saveMovie(updated)
        }
    }

    fun clearUpdate(movieId: Long) {
        viewModelScope.launch {
            val movie = uiState.value.favorites.find { it.id == movieId }
            movie?.let {
                val updated = it.copy(newEpisodesFound = 0)
                movieRepository.saveMovie(updated)
            }
        }
    }

    // --- Preference Setters (Persistent + Remote Sync) ---

    fun setHomeSort(config: SortConfig) {
        viewModelScope.launch {
            preferenceRepository.updateHomeSort(config)
            movieRepository.savePreferencesRemote(uiState.value.preferences.copy(homeSort = config))
        }
    }

    fun setVistiSort(config: SortConfig) {
        viewModelScope.launch {
            preferenceRepository.updateVistiSort(config)
            movieRepository.savePreferencesRemote(uiState.value.preferences.copy(vistiSort = config))
        }
    }

    fun setDiscoveryFilters(filters: DiscoveryFilters) {
        viewModelScope.launch {
            preferenceRepository.updateDiscoveryFilters(filters)
            movieRepository.savePreferencesRemote(uiState.value.preferences.copy(discoveryFilters = filters))
        }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            preferenceRepository.updateGridColumns(columns)
            movieRepository.savePreferencesRemote(uiState.value.preferences.copy(gridColumns = columns))
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateNotificationsEnabled(enabled)
            movieRepository.savePreferencesRemote(uiState.value.preferences.copy(notificationsEnabled = enabled))
        }
    }

    fun setShowFolderBookmarks(show: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowFolderBookmarks(show)
            movieRepository.savePreferencesRemote(uiState.value.preferences.copy(showFolderBookmarks = show))
        }
    }
}
