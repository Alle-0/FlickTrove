package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.api.Person
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle
import com.cinetrack.ui.navigation.PersonRoute
import androidx.navigation.toRoute
import com.cinetrack.ui.utils.ErrorMapper

import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.data.models.UserPreferences

data class PersonDetailUiState(
    val person: Person? = null,
    val isLoading: Boolean = true,
    val showFullBio: Boolean = false,
    val activeTab: String = "cast_movie",
    val favorites: List<Movie> = emptyList(),
    val folders: List<com.cinetrack.data.local.entities.FolderEntity> = emptyList(),
    val movieFolderColors: Map<String, List<String>> = emptyMap(),
    val preferences: UserPreferences = UserPreferences(),
    val error: String? = null
)

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val preferenceRepository: PreferenceRepository,
    private val actionFeedbackManager: ActionFeedbackManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PersonRoute>()
    private val _personId = MutableStateFlow<Long>(route.id)
    private val _showFullBio = MutableStateFlow(false)
    private val _activeTab = MutableStateFlow("cast_movie")
    private val _isLoading = MutableStateFlow(true)
    private val _person = MutableStateFlow<Person?>(null)
    private val _error = MutableStateFlow<String?>(null)

    fun emitMessage(message: String) {
        actionFeedbackManager.emit(message)
    }

    val uiState: StateFlow<PersonDetailUiState> = combine(
        combine(
            _person,
            _isLoading,
            _showFullBio,
            _activeTab,
            _error
        ) { person, isLoading, showBio, tab, error ->
            Triple(person, Triple(isLoading, showBio, tab), error)
        },
        combine(
            repository.getLocalMoviesFlow(),
            repository.getFoldersFlow(),
            preferenceRepository.userPreferencesFlow
        ) { favorites, folders, prefs ->
            Triple(favorites, folders, prefs)
        }
    ) { groupA, groupB ->
        val person = groupA.first
        val (isLoading, showFullBio, activeTab) = groupA.second
        val error = groupA.third
        
        val favorites = groupB.first
        val folders = groupB.second
        val preferences = groupB.third

        // Calculate folder colors for each movie
        val folderColorsMap = mutableMapOf<String, MutableList<String>>()
        folders.forEach { folder ->
            val color = folder.color ?: "#FFFFFF"
            folder.itemIds.forEach { itemId ->
                folderColorsMap.getOrPut(itemId) { mutableListOf() }.add(color)
            }
        }

        PersonDetailUiState(
            person = person,
            isLoading = isLoading,
            showFullBio = showFullBio,
            activeTab = activeTab,
            favorites = favorites,
            folders = folders,
            movieFolderColors = folderColorsMap,
            preferences = preferences,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PersonDetailUiState()
    )

    init {
        fetchPerson(route.id)
    }

    private fun fetchPerson(id: Long) {
        _personId.value = id
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _isLoading.value = true
            try {
                val person = repository.getPersonDetails(id)
                _person.value = person
                _error.value = null

                if (_activeTab.value == "cast_movie") {
                    val directorCredits = person.combinedCredits?.crew
                        ?.filter { it.job == "Director" }
                        .orEmpty()

                    val shouldPreferDirectorTab = person.knownForDepartment == "Directing" && directorCredits.isNotEmpty()

                    if (shouldPreferDirectorTab) {
                        _activeTab.value = when {
                            directorCredits.any { it.mediaType == "movie" } -> "crew_movie"
                            directorCredits.any { it.mediaType == "tv" } -> "crew_tv"
                            else -> _activeTab.value
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = ErrorMapper.map(e.message)
            } finally {
                val timeTaken = System.currentTimeMillis() - startTime
                if (timeTaken < 600L) {
                    kotlinx.coroutines.delay(600L - timeTaken)
                }
                _isLoading.value = false
            }
        }
    }

    fun retry() {
        fetchPerson(_personId.value)
    }

    fun loadPerson(id: Long) {
        if (_personId.value == id && _person.value != null) return
        fetchPerson(id)
    }

    fun toggleBio() {
        _showFullBio.value = !_showFullBio.value
    }

    fun onTabChanged(tab: String) {
        _activeTab.value = tab
    }

    fun toggleFavorite(movie: Movie) {
        val title = movie.displayName
        viewModelScope.launch {
            try {
                val local = repository.getMovie(movie.id, movie.mediaType)
                val current = local ?: movie
                
                // Save previous state for undo
                val previousState = current.copy()
                
                // IDEMPOTENCY CHECK: If already watched, do nothing
                if (current.watched) {
                    return@launch
                }
                
                repository.cycleMovieStatus(current)
                
                // 3. Re-fetch updated state to determine the correct label
                val updated = repository.getMovie(movie.id, movie.mediaType)
                
                val actionLabel = when {
                    updated == null -> "rimosso"
                    updated.watched -> "segnato come visto"
                    updated.favorite -> "aggiunto a Da Vedere"
                    updated.reminder -> "promemoria impostato"
                    !updated.favorite && !updated.watched && !updated.reminder -> "rimosso"
                    else -> "aggiornato"
                }
                
                actionFeedbackManager.emit("\"$title\" $actionLabel") {
                    repository.saveMovie(previousState)
                }
            } catch (e: Exception) {
                // Ignore
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
}
