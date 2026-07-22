package com.cinetrack.ui.components.shared

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.cinetrack.data.model.Movie
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.R
import dev.chrisbanes.haze.HazeState

/**
 * State holder for the MovieActionsWrapper to pass callbacks to the content.
 */
data class MovieActionsState(
    val onLongPress: (Movie, Offset, Offset) -> Unit,
    val show: (Movie) -> Unit,
    val onQuickVote: (Movie) -> Unit,
    val onQuickNote: (Movie) -> Unit,
    val onFolders: (Movie) -> Unit,
    val onDelete: (Movie) -> Unit,
    val onShare: (Movie) -> Unit
)

/**
 * A wrapper component that proxies action requests to the global MovieActionsManager.
 * This resolves text blur issues by rendering modals at the root level.
 */
@Composable
fun MovieActionsWrapper(
    hazeState: HazeState, // Kept for compatibility, but dialogs use global hazeState now
    folders: List<FolderEntity> = emptyList(),
    isItemInFolder: (Movie, String) -> Boolean = { _, _ -> false },
    onDelete: (Movie) -> Unit = {},
    onUpdateRating: (Movie, Double) -> Unit = { _, _ -> },
    onUpdateNote: (Movie, String) -> Unit = { _, _ -> },
    onToggleFolder: (Movie, FolderEntity) -> Unit = { _, _ -> },
    onActionModalVisibilityChanged: (Boolean) -> Unit = {},
    content: @Composable (MovieActionsState) -> Unit
) {
    val manager = LocalMovieActions.current
    val context = LocalContext.current
    
    // Sync local callbacks with global manager whenever they change
    SideEffect {
        manager.setupCallbacks(
            folders = folders,
            isItemInFolder = isItemInFolder,
            onDelete = onDelete,
            onUpdateRating = onUpdateRating,
            onUpdateNote = onUpdateNote,
            onToggleFolder = onToggleFolder
        )
    }

    val actionsState = remember(onDelete) {
        MovieActionsState(
            onLongPress = { movie, pressOffset, cardPos ->
                manager.openActionsPopup(movie, pressOffset, cardPos)
            },
            show = { movie ->
                // Default position for general "More" button: top right-ish or just centered
                manager.openActionsPopup(movie, Offset(0f, 0f), Offset(100f, 200f)) 
            },
            onQuickVote = { manager.openRating(it) },
            onQuickNote = { manager.openNotes(it) },
            onFolders = { manager.openFolders(it) },
            onDelete = { onDelete(it) },
            onShare = { m ->
                val shareTitle = m.title ?: m.name ?: ""
                val shareType = if (m.mediaType == "tv") "tv" else "movie"
                val url = "https://alle-0.github.io/FlickTrove/open.html?type=$shareType&id=${m.id}"
                val sendIntent: android.content.Intent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    val body = context.getString(R.string.action_share_body, url)
                    putExtra(android.content.Intent.EXTRA_TEXT, "🎬 $shareTitle\n$body")
                    type = "text/plain"
                }
                val shareIntent = android.content.Intent.createChooser(sendIntent, context.getString(R.string.action_share_title))
                context.startActivity(shareIntent)
            }
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        content(actionsState)
    }
}
