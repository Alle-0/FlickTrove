package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.Movie
import com.cinetrack.data.api.Person
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.domain.CycleMovieStatusUseCase
import com.cinetrack.ui.utils.ActionFeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle
import com.cinetrack.ui.navigation.PersonRoute
import com.cinetrack.ui.utils.ErrorMapper
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.data.model.UserPreferences

data class PersonDetailUiState(
    val personId: Long = 0L,
    val profilePath: String? = null,
    val person: Person? = null,
    val isLoading: Boolean = true,
    val showFullBio: Boolean = false,
    val activeTab: String = "cast_movie",
    val favorites: ImmutableList<Movie> = persistentListOf(),
    val folders: ImmutableList<com.cinetrack.data.local.entities.FolderEntity> = persistentListOf(),
    val movieFolderColors: ImmutableMap<String, ImmutableList<String>> = persistentMapOf(),
    val preferences: UserPreferences = UserPreferences(),
    val error: String? = null
)

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val repository: MovieRepository,
    private val preferenceRepository: PreferenceRepository,
    private val actionFeedbackManager: ActionFeedbackManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _personId = MutableStateFlow<Long>(0L)
    private var _profilePath: String? = null
    private val _showFullBio = MutableStateFlow(false)
    private val _activeTab = MutableStateFlow("cast_movie")
    private val _isLoading = MutableStateFlow(true)
    private val _person = MutableStateFlow<Person?>(null)
    private val _error = MutableStateFlow<String?>(null)
    
    val scrollState = androidx.compose.foundation.ScrollState(0)
    val animatedMovieIds = mutableSetOf<String>()

    fun initPerson(id: Long, profilePath: String?) {
        if (_personId.value == 0L && id != 0L) {
            _personId.value = id
            _profilePath = profilePath
            fetchPerson(id)
        }
    }

    fun emitMessage(message: UiText) {
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
        val folderColorsMap = mutableMapOf<String, ImmutableList<String>>()
        folders.forEach { folder ->
            val color = folder.color ?: "#FFFFFF"
            folder.itemIds.forEach { itemId ->
                val list = folderColorsMap[itemId] ?: persistentListOf()
                folderColorsMap[itemId] = (list + color).toImmutableList()
            }
        }

        PersonDetailUiState(
            personId = _personId.value,
            profilePath = _profilePath,
            person = person,
            isLoading = isLoading,
            showFullBio = showFullBio,
            activeTab = activeTab,
            favorites = favorites.toImmutableList(),
            folders = folders.toImmutableList(),
            movieFolderColors = folderColorsMap.toImmutableMap(),
            preferences = preferences,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PersonDetailUiState()
    )

    init {
        // Initialization moved to initPerson()
    }

    private fun fetchPerson(id: Long) {
        _personId.value = id
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _isLoading.value = true
            try {
                repository.getPersonDetailsFlow(id).collect { person ->
                    if (_person.value == null) {
                        val timeTaken = System.currentTimeMillis() - startTime
                        if (timeTaken < 400L) {
                            kotlinx.coroutines.delay(400L - timeTaken)
                        }
                    }
                    _person.value = person
                    _isLoading.value = false
                    _error.value = null

                    if (_activeTab.value == "cast_movie") {
                        val cast = person.combinedCredits?.cast.orEmpty()
                        val crew = person.combinedCredits?.crew.orEmpty()

                        val hasCastMovie = cast.any { it.mediaType == "movie" }
                        val hasCastTv = cast.any { it.mediaType == "tv" }

                        val mainDept = person.knownForDepartment?.takeIf { it != "Acting" }
                            ?: if (crew.any { it.department == "Directing" || it.job == "Director" }) "Directing"
                            else crew.groupingBy { it.department }.eachCount().maxByOrNull { it.value }?.key

                        val isMainDeptItem = { m: Movie ->
                            mainDept != null && (m.department == mainDept || (mainDept == "Directing" && m.job == "Director"))
                        }

                        val hasDeptMovie = crew.any { it.mediaType == "movie" && isMainDeptItem(it) }
                        val hasDeptTv = crew.any { it.mediaType == "tv" && isMainDeptItem(it) }
                        val hasCrewMovie = crew.any { it.mediaType == "movie" && !isMainDeptItem(it) }
                        val hasCrewTv = crew.any { it.mediaType == "tv" && !isMainDeptItem(it) }

                        val isPrimarilyCrew = person.knownForDepartment != null &&
                                person.knownForDepartment != "Acting" &&
                                (hasDeptMovie || hasDeptTv || hasCrewMovie || hasCrewTv)

                        if (isPrimarilyCrew || (!hasCastMovie && !hasCastTv)) {
                            _activeTab.value = when {
                                hasDeptMovie -> "dept_movie"
                                hasDeptTv -> "dept_tv"
                                hasCrewMovie -> "crew_movie"
                                hasCrewTv -> "crew_tv"
                                hasCastMovie -> "cast_movie"
                                hasCastTv -> "cast_tv"
                                else -> _activeTab.value
                            }
                        } else if (!hasCastMovie && hasCastTv) {
                            _activeTab.value = "cast_tv"
                        }
                    }
                }
            } catch (e: Exception) {
                if (_person.value == null) {
                    try {
                        val localMovies = repository.getLocalMoviesFlow().first()
                        var personName: String? = null
                        val personMovies = mutableListOf<Movie>()
                        val personCrewMovies = mutableListOf<Movie>()

                        for (m in localMovies) {
                            val asCast = m.topCastData?.find { it.id == id }
                            if (asCast != null) {
                                if (personName == null) personName = asCast.name
                                personMovies.add(m)
                            }
                            val asDirector = m.directorData?.find { it.id == id }
                            if (asDirector != null || m.directorId == id) {
                                if (personName == null) personName = asDirector?.name ?: m.directorName
                                if (!personCrewMovies.contains(m)) personCrewMovies.add(m)
                            }
                        }

                        if (personName != null || _profilePath != null) {
                            val fallbackName = personName ?: context.getString(R.string.person_offline_default_name, id)
                            val offlinePerson = Person(
                                id = id,
                                name = fallbackName,
                                profilePath = _profilePath,
                                knownForDepartment = if (personCrewMovies.size > personMovies.size) "Directing" else "Acting",
                                biography = context.getString(R.string.person_offline_bio),
                                combinedCredits = com.cinetrack.data.api.CombinedCredits(
                                    cast = personMovies,
                                    crew = personCrewMovies
                                )
                            )
                            _person.value = offlinePerson
                            _error.value = null
                            if (_activeTab.value == "cast_movie" && personMovies.isEmpty() && personCrewMovies.isNotEmpty()) {
                                _activeTab.value = if (offlinePerson.knownForDepartment == "Directing") "dept_movie" else "crew_movie"
                            }
                        } else {
                            _error.value = ErrorMapper.map(e.message)
                        }
                    } catch (fallbackEx: Exception) {
                        _error.value = ErrorMapper.map(e.message)
                    }
                }
            } finally {
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
                
                cycleMovieStatusUseCase(current)
                
                // 3. Re-fetch updated state to determine the correct label
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
            } catch (e: Exception) {
                // Ignore
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
