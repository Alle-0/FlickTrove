package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.models.SortConfig
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.domain.CycleMovieStatusUseCase
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

data class HomeUiState(
    val movies: ImmutableList<Movie> = persistentListOf(),
    val releasedMovies: ImmutableList<Movie> = persistentListOf(),
    val unreleasedMovies: ImmutableList<Movie> = persistentListOf(),
    val movieCount: Int = 0,
    val tvCount: Int = 0,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val activeTab: String = "movie",
    val sortConfig: SortConfig = SortConfig(),
    val notificationCount: Int = 0,
    val movieFolderColors: ImmutableMap<String, ImmutableList<String>> = persistentMapOf(),
    val folders: ImmutableList<com.cinetrack.data.local.entities.FolderEntity> = persistentListOf(),
    val preferences: com.cinetrack.data.models.UserPreferences = com.cinetrack.data.models.UserPreferences()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val getHomeUiStateUseCase: com.cinetrack.domain.GetHomeUiStateUseCase,
    private val repository: MovieRepository,
    private val preferenceRepository: PreferenceRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeTab = MutableStateFlow("movie")
    
    fun emitMessage(message: String) {
        actionFeedbackManager.emit(message)
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val uiState: StateFlow<HomeUiState> = getHomeUiStateUseCase(
        moviesFlow = repository.getLocalMoviesFlow(),
        foldersFlow = repository.getFoldersFlow(),
        preferencesFlow = preferenceRepository.userPreferencesFlow,
        searchQueryFlow = _searchQuery.debounce(300).distinctUntilChanged(),
        activeTabFlow = _activeTab
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = HomeUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTabChanged(tab: String) {
        _activeTab.value = tab
    }

    fun updateSortConfig(config: SortConfig) {
        viewModelScope.launch {
            try {
                preferenceRepository.updateHomeSort(config)
                repository.savePreferencesRemote(uiState.value.preferences.copy(homeSort = config))
            } catch (e: Exception) {
                actionFeedbackManager.emit("Errore durante il salvataggio")
            }
        }
    }

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch {
            try {
                val updated = uiState.value.preferences.copy(gridColumns = columns)
                preferenceRepository.updateGridColumns(columns)
                repository.savePreferencesRemote(updated)
            } catch (e: Exception) {
                actionFeedbackManager.emit("Errore durante il salvataggio")
            }
        }
    }

    fun toggleWatched(movie: Movie) {
        val title = movie.title ?: movie.name ?: ""
        viewModelScope.launch {
            try {
                val local = repository.getMovie(movie.id, movie.mediaType)
                val current = local ?: movie
                val previousState = current.copy()

                // IDEMPOTENCY CHECK: If already watched, do nothing
                if (current.watched) {
                    return@launch
                }

                cycleMovieStatusUseCase(current)
                
                val updated = repository.getMovie(movie.id, movie.mediaType)
                val actionLabel = when {
                    updated == null -> "rimosso"
                    updated.watched -> "segnato come visto"
                    updated.favorite -> "aggiunto a Da Vedere"
                    updated.reminder -> "promemoria impostato"
                    else -> "aggiornato"
                }
                
                actionFeedbackManager.emit("\"$title\" $actionLabel") {
                    try {
                        repository.saveMovie(previousState)
                    } catch (e: Exception) {
                        // ignore nested error
                    }
                }
            } catch (e: Exception) {
                actionFeedbackManager.emit("Errore durante l'aggiornamento")
            }
        }
    }

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch {
            try {
                repository.deleteMovie(movie)
                actionFeedbackManager.emit("\"${movie.title ?: movie.name}\" rimosso") {
                    try {
                        repository.saveMovie(movie)
                    } catch (e: Exception) {
                        // ignore nested error
                    }
                }
            } catch (e: Exception) {
                actionFeedbackManager.emit("Errore durante la rimozione")
            }
        }
    }

    fun updateRating(movie: Movie, rating: Double) {
        viewModelScope.launch {
            try {
                val local = repository.getMovie(movie.id, movie.mediaType)
                val current = local ?: movie
                repository.saveMovie(current.copy(personalRating = rating))
            } catch (e: Exception) {
                actionFeedbackManager.emit("Errore durante l'aggiornamento")
            }
        }
    }

    fun updateNote(movie: Movie, note: String) {
        viewModelScope.launch {
            try {
                val local = repository.getMovie(movie.id, movie.mediaType)
                val current = local ?: movie
                repository.saveMovie(current.copy(personalNote = note))
            } catch (e: Exception) {
                actionFeedbackManager.emit("Errore durante l'aggiornamento")
            }
        }
    }

    fun toggleItemInFolder(folder: com.cinetrack.data.local.entities.FolderEntity, movie: Movie) {
        viewModelScope.launch {
            try {
                val compositeId = "${movie.mediaType}_${movie.id}"
                val newItemIds = if (folder.itemIds.contains(compositeId)) {
                    folder.itemIds - compositeId
                } else {
                    folder.itemIds + compositeId
                }
                repository.saveFolder(folder.copy(itemIds = newItemIds, updatedAt = java.time.Instant.now().toString()))
            } catch (e: Exception) {
                actionFeedbackManager.emit("Errore durante l'aggiornamento cartella")
            }
        }
    }

    private fun sortMovies(movies: List<Movie>, sort: SortConfig): List<Movie> {
        val isDesc = sort.sortDirection == "desc"
        return when (sort.sortType) {
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
