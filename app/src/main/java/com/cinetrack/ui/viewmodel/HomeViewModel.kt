package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.SortConfig
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.domain.CycleMovieStatusUseCase
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.LazyGridState
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
    val preferences: com.cinetrack.data.model.UserPreferences = com.cinetrack.data.model.UserPreferences()
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
    
    val movieGridState = LazyGridState()
    val tvGridState = LazyGridState()
    val animatedMovieIds = mutableSetOf<String>()
    
    fun emitMessage(message: UiText) {
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
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_saving))
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
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_saving))
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
                val actionMsgRes = when {
                    updated == null -> R.string.msg_action_removed
                    updated.watched -> R.string.msg_action_watched
                    updated.favorite -> R.string.msg_action_favorite
                    updated.reminder -> R.string.msg_action_reminder
                    else -> R.string.msg_action_updated
                }
                
                actionFeedbackManager.emit(UiText.StringResource(actionMsgRes, title)) {
                    try {
                        repository.saveMovie(previousState)
                    } catch (e: Exception) {
                        // ignore nested error
                    }
                }
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating))
            }
        }
    }

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch {
            try {
                repository.deleteMovie(movie)
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_item_removed, movie.title ?: movie.name ?: "")) {
                    try {
                        repository.saveMovie(movie)
                    } catch (e: Exception) {
                        // ignore nested error
                    }
                }
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_removing))
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
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating))
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
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating))
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
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating_folder))
            }
        }
    }

    private fun sortMovies(movies: List<Movie>, sort: SortConfig): List<Movie> {
        val isDesc = sort.sortDirection == "desc"
        return when (sort.sortType) {
            "release_date" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { it.releaseDate ?: it.firstAirDate ?: "" }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { it.releaseDate ?: it.firstAirDate ?: "" }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                }
            }
            "title" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { it.title ?: it.name ?: "" }.thenBy { it.id })
                }
            }
            "added_at", "created_at" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { it.clientUpdatedAt }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { it.clientUpdatedAt }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                }
            }
            "vote_average" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { it.voteAverage ?: 0.0 }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { it.voteAverage ?: 0.0 }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                }
            }
            "personal_rating" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { it.personalRating ?: 0.0 }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { it.personalRating ?: 0.0 }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                }
            }
            "runtime" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { getMovieDuration(it) }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { getMovieDuration(it) }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
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
