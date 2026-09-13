package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.model.BoxOfficeMovie
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.domain.CycleMovieStatusUseCase
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.UiText
import com.cinetrack.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BoxOfficeCategory {
    WEEKEND,
    ALL_TIME
}

data class BoxOfficeUiState(
    val activeTab: BoxOfficeCategory = BoxOfficeCategory.WEEKEND,
    val weekendList: ImmutableList<BoxOfficeMovie> = persistentListOf(),
    val allTimeList: ImmutableList<BoxOfficeMovie> = persistentListOf(),
    val selectedYear: Int? = null,
    val isLoadingWeekend: Boolean = true,
    val isLoadingAllTime: Boolean = false,
    val isRefreshing: Boolean = false,
    val isError: Boolean = false,
    val folders: ImmutableList<FolderEntity> = persistentListOf(),
    val movieFolderColors: ImmutableMap<String, ImmutableList<String>> = persistentMapOf(),
    val allLocalMovies: ImmutableMap<String, Movie> = persistentMapOf()
)

@HiltViewModel
class BoxOfficeViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val actionFeedbackManager: ActionFeedbackManager
) : ViewModel() {

    private val _activeTab = MutableStateFlow(BoxOfficeCategory.WEEKEND)
    private val _weekendList = MutableStateFlow<List<BoxOfficeMovie>>(emptyList())
    private val _allTimeList = MutableStateFlow<List<BoxOfficeMovie>>(emptyList())
    private val _selectedYear = MutableStateFlow<Int?>(null)
    private val _isLoadingWeekend = MutableStateFlow(true)
    private val _isLoadingAllTime = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _isError = MutableStateFlow(false)

    init {
        loadWeekendBoxOffice(forceRefresh = false)
    }

    val uiState: StateFlow<BoxOfficeUiState> = combine(
        _activeTab,
        _weekendList,
        _allTimeList,
        _selectedYear,
        _isLoadingWeekend,
        _isLoadingAllTime,
        _isRefreshing,
        _isError,
        repository.getFoldersFlow(),
        repository.getLocalMoviesFlow()
    ) { params ->
        val activeTab = params[0] as BoxOfficeCategory
        @Suppress("UNCHECKED_CAST")
        val weekendList = params[1] as List<BoxOfficeMovie>
        @Suppress("UNCHECKED_CAST")
        val allTimeList = params[2] as List<BoxOfficeMovie>
        val selectedYear = params[3] as? Int
        val isLoadingWeekend = params[4] as Boolean
        val isLoadingAllTime = params[5] as Boolean
        val isRefreshing = params[6] as Boolean
        val isError = params[7] as Boolean
        @Suppress("UNCHECKED_CAST")
        val folders = params[8] as List<FolderEntity>
        @Suppress("UNCHECKED_CAST")
        val localMovies = params[9] as List<Movie>

        val localMap = localMovies.associateBy { "${it.mediaType}_${it.id}" }
        val folderColorMap = mutableMapOf<String, MutableList<String>>()
        folders.forEach { folder ->
            val color = folder.color
            if (!color.isNullOrBlank()) {
                folder.itemIds.forEach { itemId ->
                    folderColorMap.getOrPut(itemId) { mutableListOf() }.add(color)
                }
            }
        }

        BoxOfficeUiState(
            activeTab = activeTab,
            weekendList = weekendList.toImmutableList(),
            allTimeList = allTimeList.toImmutableList(),
            selectedYear = selectedYear,
            isLoadingWeekend = isLoadingWeekend,
            isLoadingAllTime = isLoadingAllTime,
            isRefreshing = isRefreshing,
            isError = isError,
            folders = folders.toImmutableList(),
            movieFolderColors = folderColorMap.mapValues { it.value.toImmutableList() }.toImmutableMap(),
            allLocalMovies = localMap.toImmutableMap()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BoxOfficeUiState()
    )

    fun switchTab(tab: BoxOfficeCategory) {
        _activeTab.value = tab
        if (tab == BoxOfficeCategory.ALL_TIME && _allTimeList.value.isEmpty() && !_isLoadingAllTime.value) {
            loadAllTimeBoxOffice(year = _selectedYear.value)
        }
    }

    fun selectYear(year: Int?) {
        if (_selectedYear.value == year) return
        _selectedYear.value = year
        loadAllTimeBoxOffice(year = year)
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (_activeTab.value) {
                BoxOfficeCategory.WEEKEND -> loadWeekendBoxOffice(forceRefresh = true)
                BoxOfficeCategory.ALL_TIME -> loadAllTimeBoxOffice(year = _selectedYear.value, forceRefresh = true)
            }
            _isRefreshing.value = false
        }
    }

    private fun loadWeekendBoxOffice(forceRefresh: Boolean) {
        viewModelScope.launch {
            _isLoadingWeekend.value = true
            _isError.value = false
            try {
                val list = repository.getWeekendBoxOffice(forceRefresh = forceRefresh)
                _weekendList.value = list
                if (list.isEmpty()) {
                    _isError.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isError.value = true
            } finally {
                _isLoadingWeekend.value = false
            }
        }
    }

    private fun loadAllTimeBoxOffice(year: Int?, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingAllTime.value = true
            _isError.value = false
            try {
                val list = repository.getAllTimeBoxOffice(year = year, page = 1, forceRefresh = forceRefresh)
                _allTimeList.value = list
            } catch (e: Exception) {
                e.printStackTrace()
                _isError.value = true
            } finally {
                _isLoadingAllTime.value = false
            }
        }
    }

    // Movie action delegates
    fun toggleWatched(movie: Movie) {
        viewModelScope.launch {
            cycleMovieStatusUseCase(movie)
        }
    }

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch {
            try {
                repository.deleteMovie(movie)
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
                repository.saveMovie(current.copy(personalRating = rating, votedAt = System.currentTimeMillis()))
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

    fun toggleItemInFolder(folder: FolderEntity, movie: Movie) {
        viewModelScope.launch {
            try {
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
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating_folder))
            }
        }
    }

    fun emitMessage(uiText: UiText) {
        actionFeedbackManager.emit(uiText)
    }
}
