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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.cinetrack.ui.utils.ActionFeedbackManager
import javax.inject.Inject
import com.cinetrack.domain.GetVistiUiStateUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

data class VistiUiState(
    val movies: ImmutableList<Movie> = persistentListOf(),
    val movieCount: Int = 0,
    val tvCount: Int = 0,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val activeTab: String = "movie",
    val movieFolderColors: ImmutableMap<String, ImmutableList<String>> = persistentMapOf(),
    val folders: ImmutableList<com.cinetrack.data.local.entities.FolderEntity> = persistentListOf(),
    val sortConfig: SortConfig = SortConfig(),
    val preferences: com.cinetrack.data.model.UserPreferences = com.cinetrack.data.model.UserPreferences()
)

@HiltViewModel
class VistiViewModel @Inject constructor(
    private val getVistiUiStateUseCase: GetVistiUiStateUseCase,
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val repository: MovieRepository,
    private val preferenceRepository: PreferenceRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeTab = MutableStateFlow("movie")
    
    val movieGridState = androidx.compose.foundation.lazy.grid.LazyGridState()
    val tvGridState = androidx.compose.foundation.lazy.grid.LazyGridState()
    val animatedMovieIds = mutableSetOf<String>()
    
    fun emitMessage(message: UiText) {
        actionFeedbackManager.emit(message)
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val uiState: StateFlow<VistiUiState> = getVistiUiStateUseCase(
        repository.getLocalMoviesFlow(),
        repository.getFoldersFlow(),
        preferenceRepository.userPreferencesFlow,
        _searchQuery.debounce(300).distinctUntilChanged(),
        _activeTab
    ).stateIn(
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

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch {
            preferenceRepository.updateGridColumns(columns)
            repository.savePreferencesRemote(uiState.value.preferences.copy(gridColumns = columns))
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
}
