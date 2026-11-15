package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegato condiviso per centralizzare le operazioni CRUD e le azioni multimediali comuni
 * (valutazione voto, nota personale, associazione a cartelle, cancellazione con snackbar Undo)
 * tra i vari ViewModel dell'applicazione, azzerando il codice duplicato.
 */
@Singleton
class MediaActionDelegate @Inject constructor(
    private val repository: MovieRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) {
    fun deleteMovie(scope: CoroutineScope, movie: Movie) {
        scope.launch {
            try {
                repository.deleteMovie(movie)
                actionFeedbackManager.emit(
                    UiText.StringResource(R.string.msg_item_removed, movie.title ?: movie.name ?: "")
                ) {
                    try {
                        repository.saveMovie(movie)
                    } catch (_: Exception) {
                        // ignore nested error
                    }
                }
            } catch (_: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_removing))
            }
        }
    }

    fun updateRating(scope: CoroutineScope, movie: Movie, rating: Double?) {
        scope.launch {
            try {
                val local = repository.getMovie(movie.id, movie.mediaType)
                val current = local ?: movie
                repository.saveMovie(current.copy(personalRating = rating, votedAt = if (rating != null) System.currentTimeMillis() else null))
            } catch (_: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating))
            }
        }
    }

    fun updateNote(scope: CoroutineScope, movie: Movie, note: String?) {
        scope.launch {
            try {
                val local = repository.getMovie(movie.id, movie.mediaType)
                val current = local ?: movie
                repository.saveMovie(current.copy(personalNote = note))
            } catch (_: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating))
            }
        }
    }

    fun toggleItemInFolder(scope: CoroutineScope, folder: FolderEntity, movie: Movie) {
        scope.launch {
            try {
                val compositeId = movie.compositeId
                val isPresent = folder.itemIds.contains(compositeId)
                if (!isPresent && folder.isShared && folder.itemIds.size >= folder.maxItems) {
                    actionFeedbackManager.emit(UiText.StringResource(R.string.folder_max_items_reached))
                    return@launch
                }
                val newItemIds = if (isPresent) {
                    folder.itemIds - compositeId
                } else {
                    folder.itemIds + compositeId
                }
                repository.saveFolder(
                    folder.copy(
                        itemIds = newItemIds,
                        updatedAt = java.time.Instant.now().toString()
                    )
                )
                val local = repository.getMovie(movie.id, movie.mediaType)
                if (local == null) {
                    repository.saveMovie(movie)
                }
                val title = movie.title ?: movie.name ?: ""
                val folderName = folder.name
                actionFeedbackManager.emit(
                    if (isPresent) UiText.StringResource(R.string.msg_item_removed_from_folder, title)
                    else UiText.StringResource(R.string.msg_item_added_to_folder, title, folderName)
                ) {
                    try {
                        repository.saveFolder(folder)
                    } catch (_: Exception) {
                        // ignore
                    }
                }
            } catch (_: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating_folder))
            }
        }
    }
}
