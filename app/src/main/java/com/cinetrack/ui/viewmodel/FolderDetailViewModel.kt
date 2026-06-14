package com.cinetrack.ui.viewmodel

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
import androidx.compose.foundation.lazy.grid.LazyGridState
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
        val allFolders: ImmutableList<FolderEntity> = persistentListOf()
    ) : FolderDetailUiState
    data class Error(val message: String) : FolderDetailUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val actionFeedbackManager: ActionFeedbackManager,
    private val preferenceRepository: PreferenceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _folderId = MutableStateFlow<String?>(null)
    
    val lazyGridState = LazyGridState()
    val animatedMovieIds = mutableSetOf<String>()

    fun initFolder(id: String) {
        if (_folderId.value != id) {
            _folderId.value = id
        }
    }

    fun emitMessage(message: String) {
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

    val uiState: StateFlow<FolderDetailUiState> = _folderId.filterNotNull().flatMapLatest { id ->
        repository.getFolderFlow(id).flatMapLatest { folder ->
            if (folder == null) {
            flowOf(FolderDetailUiState.Error("Cartella non trovata"))
        } else {
            combine(
                repository.getMoviesByCompositeIds(folder.itemIds),
                allFoldersFlow
            ) { movies, allFolders ->
                FolderDetailUiState.Success(folder, movies.toImmutableList(), allFolders.toImmutableList())
            }
        }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FolderDetailUiState.Loading
    )

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
                actionFeedbackManager.emit("\"$title\" rimosso dalla cartella") {
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
                if (isPresent) "\"$title\" rimosso da $folderName" 
                else "\"$title\" aggiunto a $folderName"
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
                actionFeedbackManager.emit("Cartella \"${folder.name}\" eliminata") {
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
