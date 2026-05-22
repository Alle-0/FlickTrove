package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.cinetrack.ui.utils.ActionFeedbackManager
import javax.inject.Inject
import java.util.UUID
import java.time.Instant

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) : ViewModel() {

    val folders: StateFlow<List<FolderEntity>> = repository.getFoldersFlow()
        .map { it.sortedByDescending { folder -> folder.createdAt } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createFolder(name: String, icon: String, color: String, description: String = "") {
        viewModelScope.launch {
            val newFolder = FolderEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                icon = icon,
                color = color,
                description = description,
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )
            repository.saveFolder(newFolder)
            actionFeedbackManager.emit("Cartella \"$name\" creata")
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            val folder = folders.value.find { it.id == folderId }
            if (folder != null) {
                repository.deleteFolder(folderId)
                actionFeedbackManager.emit("Cartella \"${folder.name}\" eliminata") {
                    repository.saveFolder(folder)
                }
            }
        }
    }

    fun updateFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.saveFolder(folder.copy(updatedAt = Instant.now().toString()))
        }
    }
}
