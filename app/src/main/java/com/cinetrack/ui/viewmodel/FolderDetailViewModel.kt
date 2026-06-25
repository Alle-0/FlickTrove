package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.data.models.UserPreferences
import javax.inject.Inject
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.collections.immutable.ImmutableList
import com.cinetrack.ui.utils.GlobalErrorHandler
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

sealed interface FolderDetailUiState {
    object Loading : FolderDetailUiState
    data class Success(
        val folder: FolderEntity,
        val movies: ImmutableList<Movie>,
        val allFolders: ImmutableList<FolderEntity> = persistentListOf(),
        val activeTab: String = "all",
        val movieCount: Int = 0,
        val tvCount: Int = 0,
        val sortConfig: com.cinetrack.data.models.SortConfig = com.cinetrack.data.models.SortConfig()
    ) : FolderDetailUiState
    data class Error(val message: String) : FolderDetailUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val actionFeedbackManager: ActionFeedbackManager,
    private val preferenceRepository: PreferenceRepository,
    private val globalErrorHandler: GlobalErrorHandler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _folderId = MutableStateFlow<String?>(null)
    
    val animatedMovieIds = mutableSetOf<String>()

    fun initFolder(id: String) {
        if (_folderId.value != id) {
            _folderId.value = id
        }
    }

    fun emitMessage(message: UiText) {
        actionFeedbackManager.emit(message)
    }

    private val allFoldersFlow = repository.getFoldersFlow()

    val movieFolderColors: StateFlow<ImmutableMap<String, ImmutableList<String>>> = allFoldersFlow.map { folders ->
        val map = mutableMapOf<String, MutableList<String>>()
        folders.forEach { folder ->
            folder.itemIds.forEach { id ->
                map.getOrPut(id) { mutableListOf() }.add(folder.color ?: "#FFFFFF")
            }
        }
        map.mapValues { it.value.toImmutableList() }.toImmutableMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentMapOf())

    val preferences = preferenceRepository.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch {
            preferenceRepository.updateGridColumns(columns)
        }
    }

    private val _sortConfig = MutableStateFlow(com.cinetrack.data.models.SortConfig())
    fun updateSortConfig(config: com.cinetrack.data.models.SortConfig) {
        _sortConfig.value = config
    }

    private val _activeTab = MutableStateFlow("all")
    fun onTabChanged(tab: String) { _activeTab.value = tab }

    val uiState: StateFlow<FolderDetailUiState> = combine(
        _folderId.filterNotNull().flatMapLatest { id ->
            repository.getFolderFlow(id).flatMapLatest { folder ->
                if (folder == null) {
                    globalErrorHandler.emitError(UiText.StringResource(R.string.msg_folder_not_found)) { initFolder(id) }
                    kotlinx.coroutines.flow.flowOf(FolderDetailUiState.Error("Cartella non trovata") as FolderDetailUiState)
                } else {
                    combine(
                        repository.getMoviesByCompositeIds(folder.itemIds),
                        allFoldersFlow
                    ) { movies, allFolders ->
                        FolderDetailUiState.Success(folder, movies.toImmutableList(), allFolders.toImmutableList()) as FolderDetailUiState
                    }
                }
            }
        },
        _activeTab,
        _sortConfig
    ) { baseState, activeTab, sortConfig ->
        if (baseState is FolderDetailUiState.Success) {
            val movieCount = baseState.movies.count { it.mediaType == "movie" }
            val tvCount = baseState.movies.count { it.mediaType == "tv" }
            var filtered = when (activeTab) {
                "movie" -> baseState.movies.filter { it.mediaType == "movie" }
                "tv" -> baseState.movies.filter { it.mediaType == "tv" }
                else -> baseState.movies
            }
            filtered = applySortConfig(filtered, sortConfig)
            baseState.copy(
                movies = filtered.toImmutableList(),
                activeTab = activeTab,
                movieCount = movieCount,
                tvCount = tvCount,
                sortConfig = sortConfig
            )
        } else {
            baseState
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FolderDetailUiState.Loading
    )

    private fun applySortConfig(movies: List<Movie>, sort: com.cinetrack.data.models.SortConfig): List<Movie> {
        val isDesc = sort.sortDirection == "desc"
        var filtered = movies
        
        if (sort.selectedGenres.isNotEmpty()) {
            filtered = filtered.filter { movie -> movie.genreIds?.any { it in sort.selectedGenres } == true }
        }
        
        if (sort.selectedDecades.isNotEmpty()) {
            filtered = filtered.filter { movie ->
                sort.selectedDecades.any { decade ->
                    val prefix = decade.take(3)
                    (movie.releaseDate?.startsWith(prefix) == true) || (movie.firstAirDate?.startsWith(prefix) == true)
                }
            }
        }
        
        if (sort.selectedProviders.isNotEmpty()) {
            filtered = filtered.filter { movie ->
                movie.streamingProviderIds?.any { it in sort.selectedProviders } == true
            }
        }

        return when (sort.sortType) {
            "watched_at" -> if (isDesc) filtered.sortedByDescending { it.watchedAt } else filtered.sortedBy { it.watchedAt }
            "release_date" -> if (isDesc) filtered.sortedByDescending { it.releaseDate ?: it.firstAirDate ?: "" } else filtered.sortedBy { it.releaseDate ?: it.firstAirDate ?: "" }
            "title" -> if (isDesc) filtered.sortedByDescending { it.title ?: it.name ?: "" } else filtered.sortedBy { it.title ?: it.name ?: "" }
            "added_at", "created_at" -> if (isDesc) filtered.sortedByDescending { it.clientUpdatedAt } else filtered.sortedBy { it.clientUpdatedAt }
            "vote_average" -> if (isDesc) filtered.sortedByDescending { it.voteAverage ?: 0.0 } else filtered.sortedBy { it.voteAverage ?: 0.0 }
            "personal_rating" -> if (isDesc) filtered.sortedByDescending { it.personalRating ?: 0.0 } else filtered.sortedBy { it.personalRating ?: 0.0 }
            "runtime" -> if (isDesc) filtered.sortedByDescending { getMovieDuration(it) } else filtered.sortedBy { getMovieDuration(it) }
            else -> filtered
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

    fun removeMovieFromFolder(movie: Movie) {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is FolderDetailUiState.Success) {
                val folder = currentState.folder
                val itemId = "${movie.mediaType}_${movie.id}"
                val updatedIds = folder.itemIds.filter { it != itemId }
                
                repository.saveFolder(folder.copy(
                    itemIds = updatedIds,
                    updatedAt = Instant.now().toString()
                ))

                val title = movie.title ?: movie.name ?: ""
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_item_removed_from_folder, title)) {
                    repository.saveFolder(folder)
                }
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

    fun toggleItemInFolder(targetFolder: FolderEntity, movie: Movie) {
        viewModelScope.launch {
            val itemId = "${movie.mediaType}_${movie.id}"
            val isPresent = targetFolder.itemIds.contains(itemId)
            val updatedIds = if (isPresent) {
                targetFolder.itemIds.filter { it != itemId }
            } else {
                targetFolder.itemIds + itemId
            }
            
            repository.saveFolder(targetFolder.copy(
                itemIds = updatedIds,
                updatedAt = Instant.now().toString()
            ))
            
            val title = movie.title ?: movie.name ?: ""
            val folderName = targetFolder.name
            actionFeedbackManager.emit(
                if (isPresent) UiText.StringResource(R.string.msg_item_removed_from_folder, title)
                else UiText.StringResource(R.string.msg_item_added_to_folder, title, folderName)
            ) {
                repository.saveFolder(targetFolder)
            }
        }
    }

    fun deleteFolder() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is FolderDetailUiState.Success) {
                val folder = currentState.folder
                _folderId.value?.let { id ->
                    repository.deleteFolder(id)
                }
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_folder_deleted, folder.name)) {
                    repository.saveFolder(folder)
                }
            }
        }
    }

    fun updateFolderDetails(newName: String, newColor: String) {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is FolderDetailUiState.Success) {
                val folder = currentState.folder
                repository.saveFolder(folder.copy(
                    name = newName,
                    color = newColor,
                    updatedAt = Instant.now().toString()
                ))
            }
        }
    }
}
