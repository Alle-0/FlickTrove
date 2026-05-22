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

sealed interface FolderDetailUiState {
    object Loading : FolderDetailUiState
    data class Success(
        val folder: FolderEntity,
        val movies: List<Movie>,
        val allFolders: List<FolderEntity> = emptyList()
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

    private val folderId: String = checkNotNull(savedStateHandle["folderId"])

    fun emitMessage(message: String) {
        actionFeedbackManager.emit(message)
    }

    private val allFoldersFlow = repository.getFoldersFlow()

    val movieFolderColors: StateFlow<Map<String, List<String>>> = allFoldersFlow.map { folders ->
        val map = mutableMapOf<String, MutableList<String>>()
        folders.forEach { folder ->
            folder.itemIds.forEach { id ->
                map.getOrPut(id) { mutableListOf() }.add(folder.color ?: "#FFFFFF")
            }
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val preferences = preferenceRepository.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    val uiState: StateFlow<FolderDetailUiState> = repository.getFolderFlow(folderId).flatMapLatest { folder ->
        if (folder == null) {
            flowOf(FolderDetailUiState.Error("Cartella non trovata"))
        } else {
            combine(
                repository.getMoviesByCompositeIds(folder.itemIds),
                allFoldersFlow
            ) { movies, allFolders ->
                FolderDetailUiState.Success(folder, movies, allFolders)
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
            repository.saveMovie(movie.copy(personalRating = rating))
        }
    }

    fun updateNote(movie: Movie, note: String) {
        viewModelScope.launch {
            repository.saveMovie(movie.copy(personalNote = note))
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
                repository.deleteFolder(folderId)
                actionFeedbackManager.emit("Cartella \"${folder.name}\" eliminata") {
                    repository.saveFolder(folder)
                }
            }
        }
    }
}
