package com.cinetrack.ui.components.shared

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.cinetrack.data.Movie
import com.cinetrack.data.local.entities.FolderEntity

/**
 * Global manager for movie-related actions (Rating, Notes, Folders, Deletion).
 * Hoisted to MainActivity to avoid blur artifacts from nested Haze capture.
 */
@Stable
class MovieActionsManager {
    var activeMovie by mutableStateOf<Movie?>(null)
        private set
    
    var showRatingDialog by mutableStateOf(false)
        private set
    
    var showNotesDialog by mutableStateOf(false)
        private set
    
    var showFolderDialog by mutableStateOf(false)
        private set
        
    var showActionsPopup by mutableStateOf(false)
        private set

    val isAnyModalOpen: Boolean
        get() = showRatingDialog || showNotesDialog || showFolderDialog || showActionsPopup

    var popupPressOffset by mutableStateOf(Offset.Zero)
        private set
        
    var popupCardPosition by mutableStateOf(Offset.Zero)
        private set

    var onDeleteCallback by mutableStateOf<((Movie) -> Unit)?>(null)
        private set
        
    var onUpdateRatingCallback by mutableStateOf<((Movie, Double) -> Unit)?>(null)
        private set
        
    var onUpdateNoteCallback by mutableStateOf<((Movie, String) -> Unit)?>(null)
        private set
        
    var onToggleFolderCallback by mutableStateOf<((Movie, FolderEntity) -> Unit)?>(null)
        private set
        
    var isItemInFolderCallback by mutableStateOf<((Movie, String) -> Boolean)>({ _, _ -> false })
        private set
        
    var foldersList by mutableStateOf<List<FolderEntity>>(emptyList())
        private set

    fun openRating(movie: Movie) {
        activeMovie = movie
        showRatingDialog = true
        showActionsPopup = false
    }

    fun openNotes(movie: Movie) {
        activeMovie = movie
        showNotesDialog = true
        showActionsPopup = false
    }

    fun openFolders(movie: Movie) {
        activeMovie = movie
        showFolderDialog = true
        showActionsPopup = false
    }
    
    fun openActionsPopup(movie: Movie, pressOffset: Offset, cardPosition: Offset) {
        activeMovie = movie
        popupPressOffset = pressOffset
        popupCardPosition = cardPosition
        showActionsPopup = true
    }
    
    fun setupCallbacks(
        folders: List<FolderEntity> = emptyList(),
        isItemInFolder: (Movie, String) -> Boolean = { _, _ -> false },
        onDelete: (Movie) -> Unit = {},
        onUpdateRating: (Movie, Double) -> Unit = { _, _ -> },
        onUpdateNote: (Movie, String) -> Unit = { _, _ -> },
        onToggleFolder: (Movie, FolderEntity) -> Unit = { _, _ -> }
    ) {
        foldersList = folders
        isItemInFolderCallback = isItemInFolder
        onDeleteCallback = onDelete
        onUpdateRatingCallback = onUpdateRating
        onUpdateNoteCallback = onUpdateNote
        onToggleFolderCallback = onToggleFolder
    }

    fun closeAll() {
        showRatingDialog = false
        showNotesDialog = false
        showFolderDialog = false
        showActionsPopup = false
    }
    
    fun clearActiveMovie() {
        activeMovie = null
    }
}

val LocalMovieActions = staticCompositionLocalOf<MovieActionsManager> {
    error("No MovieActionsManager provided")
}
