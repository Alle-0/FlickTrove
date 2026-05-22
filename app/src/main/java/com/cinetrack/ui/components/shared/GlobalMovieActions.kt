package com.cinetrack.ui.components.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import com.cinetrack.ui.components.MovieActionsPopup
import com.cinetrack.ui.components.shared.QuickRatingModal
import com.cinetrack.ui.components.shared.QuickNoteModal
import com.cinetrack.ui.components.shared.QuickFolderModal
import dev.chrisbanes.haze.HazeState

/**
 * Renders movie action modals globally.
 * Should be placed in MainActivity outside of nested haze capture zones.
 */
@Composable
fun GlobalMovieActions(
    manager: MovieActionsManager,
    hazeState: HazeState, // This should be the root haze state if we want background blur
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val movie = manager.activeMovie ?: return

    Box(modifier = modifier) {
        // 1. Actions Popup
        if (manager.showActionsPopup) {
        MovieActionsPopup(
            movie = movie,
            showMenu = true,
            onDismiss = { manager.closeAll() },
            pressOffset = manager.popupPressOffset,
            cardPosition = manager.popupCardPosition,
            hazeState = hazeState,
            onQuickVote = { manager.openRating(it) },
            onQuickNote = { manager.openNotes(it) },
            onFolders = { manager.openFolders(it) },
            onShare = { m ->
                val shareTitle = m.title ?: m.name ?: ""
                val shareType = if (m.mediaType == "tv") "tv" else "movie"
                val url = "https://flicktrove.com/media/$shareType/${m.id}"
                val sendIntent: android.content.Intent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "🎬 $shareTitle\nGuarda su FlickTrove: $url")
                    type = "text/plain"
                }
                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
                manager.closeAll()
            },
            onDelete = { manager.onDeleteCallback?.invoke(it) }
        )
    }

    // 2. Quick Rating Modal
    if (manager.showRatingDialog) {
        QuickRatingModal(
            initialRating = movie.personalRating ?: 0.0,
            movieTitle = movie.title ?: movie.name ?: "",
            accentColor = MaterialTheme.colorScheme.primary,
            hazeState = hazeState,
            onSave = { rating ->
                manager.onUpdateRatingCallback?.invoke(movie, rating)
                manager.closeAll()
            },
            onDismiss = { manager.closeAll() }
        )
    }

    // 3. Quick Note Modal
    if (manager.showNotesDialog) {
        QuickNoteModal(
            initialNote = movie.personalNote ?: "",
            movieTitle = movie.title ?: movie.name ?: "",
            accentColor = MaterialTheme.colorScheme.primary,
            hazeState = hazeState,
            onSave = { note ->
                manager.onUpdateNoteCallback?.invoke(movie, note)
                manager.closeAll()
            },
            onDismiss = { manager.closeAll() }
        )
    }

    // 4. Quick Folder Modal
    if (manager.showFolderDialog) {
        QuickFolderModal(
            movieTitle = movie.title ?: movie.name ?: "",
            folders = manager.foldersList,
            hazeState = hazeState,
            isItemInFolder = { folderId -> manager.isItemInFolderCallback(movie, folderId) },
            onToggleItem = { folder -> manager.onToggleFolderCallback?.invoke(movie, folder) },
            onClose = { manager.closeAll() }
        )
    }
    }
}
