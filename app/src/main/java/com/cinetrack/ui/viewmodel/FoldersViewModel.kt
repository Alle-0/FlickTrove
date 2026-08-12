package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) : ViewModel() {

    private val _sortOption = kotlinx.coroutines.flow.MutableStateFlow(com.cinetrack.ui.screens.FolderSortOption.DATE)
    val sortOption: StateFlow<com.cinetrack.ui.screens.FolderSortOption> = _sortOption

    private val _sortOrder = kotlinx.coroutines.flow.MutableStateFlow(com.cinetrack.ui.screens.CommentSortOrder.DESC)
    val sortOrder: StateFlow<com.cinetrack.ui.screens.CommentSortOrder> = _sortOrder

    val folders: StateFlow<ImmutableList<FolderEntity>> = kotlinx.coroutines.flow.combine(repository.getFoldersFlow(), _sortOption, _sortOrder) { list, option, order ->
        val sorted = when (option) {
            com.cinetrack.ui.screens.FolderSortOption.DATE -> list.sortedBy { it.createdAt }
            com.cinetrack.ui.screens.FolderSortOption.NAME -> list.sortedBy { it.name.lowercase() }
            com.cinetrack.ui.screens.FolderSortOption.ITEMS -> list.sortedBy { it.itemIds.size }
        }
        if (order == com.cinetrack.ui.screens.CommentSortOrder.DESC) sorted.reversed().toImmutableList() else sorted.toImmutableList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = persistentListOf()
    )

    fun updateSort(option: com.cinetrack.ui.screens.FolderSortOption, order: com.cinetrack.ui.screens.CommentSortOrder) {
        _sortOption.value = option
        _sortOrder.value = order
    }

    val allMovies = repository.getLocalMoviesFlow()
        .map { it.toImmutableList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = persistentListOf()
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
            actionFeedbackManager.emit(UiText.StringResource(R.string.msg_folder_created, name))
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            val folder = folders.value.find { it.id == folderId }
            if (folder != null) {
                repository.deleteFolder(folderId)
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_folder_deleted, folder.name)) {
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
