package com.cinetrack.ui.components.shared

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.cinetrack.data.model.Movie
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

    var popupCardSize by mutableStateOf(Size(350f, 525f))
        private set

    var explodingMovie by mutableStateOf<Movie?>(null)
        private set

    var explodingCardPosition by mutableStateOf(Offset.Zero)
        private set

    var explodingCardSize by mutableStateOf(Size(350f, 525f))
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
    
    fun updatePopupCardSize(size: Size) {
        if (size.width > 0 && size.height > 0) {
            popupCardSize = size
        }
    }

    fun openActionsPopup(
        movie: Movie, 
        pressOffset: Offset, 
        cardPosition: Offset, 
        cardSize: Size = popupCardSize
    ) {
        activeMovie = movie
        popupPressOffset = pressOffset
        popupCardPosition = cardPosition
        if (cardSize.width > 0 && cardSize.height > 0) {
            popupCardSize = cardSize
        }
        showActionsPopup = true
    }

    fun startCardExplosion(movie: Movie) {
        val currentPosition = popupCardPosition
        val currentSize = popupCardSize
        closeAll()
        explodingMovie = movie
        explodingCardPosition = currentPosition
        explodingCardSize = currentSize
    }

    fun finishCardExplosion(movie: Movie) {
        if (explodingMovie?.id == movie.id) {
            explodingMovie = null
        }
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
