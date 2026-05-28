package com.cinetrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.data.Movie
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.viewmodel.FolderDetailViewModel
import com.cinetrack.ui.viewmodel.FolderDetailUiState
import com.cinetrack.ui.components.MovieCard
import com.cinetrack.ui.components.shared.DeleteFolderDialog
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.util.toComposeColor

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    viewModel: FolderDetailViewModel,
    paddingValues: PaddingValues,
    hazeState: HazeState? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    showDeleteConfirm: Boolean = false,
    onShowDeleteConfirmChange: (Boolean) -> Unit = {},
    showEditDialog: Boolean = false,
    onShowEditDialogChange: (Boolean) -> Unit = {},
    folderEditMode: com.cinetrack.ui.components.shared.FolderEditMode = com.cinetrack.ui.components.shared.FolderEditMode.NAME,
    onFolderUpdated: (String, String) -> Unit = { _, _ -> },
    onMovieClick: (Movie) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folderColors by viewModel.movieFolderColors.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = uiState) {
                is FolderDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is FolderDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = Color.White.copy(alpha = 0.5f))
                    }
                }
                is FolderDetailUiState.Success -> {
                    val localHazeState = remember { HazeState() }
                    
                    MovieActionsWrapper(
                        hazeState = localHazeState,
                        folders = state.allFolders,
                        isItemInFolder = { movie, folderId ->
                            state.allFolders.find { it.id == folderId }?.itemIds?.contains("${movie.mediaType}_${movie.id}") ?: false
                        },
                        onDelete = { movie -> viewModel.removeMovieFromFolder(movie) },
                        onUpdateRating = { movie, rating -> viewModel.updateRating(movie, rating) },
                        onUpdateNote = { movie, note -> viewModel.updateNote(movie, note) },
                        onToggleFolder = { movie, folder -> viewModel.toggleItemInFolder(folder, movie) }
                    ) { actionsState ->
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(
                                    state = localHazeState,
                                    style = HazeStyles.PremiumDark
                                )
                        ) {
                            val columns = if (preferences.gridColumns in 1..3) preferences.gridColumns else 3
                            val gap = 8.dp
                            val padding = 16.dp
                            val cardWidth = if (columns > 1) {
                                (maxWidth - (padding * 2) - (gap * (columns - 1))) / columns
                            } else {
                                maxWidth - (padding * 2)
                            }
                            
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = paddingValues.calculateTopPadding() + 16.dp,
                                    bottom = paddingValues.calculateBottomPadding() + 32.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (state.movies.isEmpty()) {
                                    item(span = { GridItemSpan(3) }) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(300.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Movie,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.15f),
                                                    modifier = Modifier.size(64.dp)
                                                )
                                                Spacer(Modifier.height(16.dp))
                                                Text(
                                                    text = "Questa cartella è vuota",
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    itemsIndexed(state.movies, key = { index, movie -> movie.id }) { index, movie ->
                                        val currentFolderColors = folderColors[movie.compositeId]?.map { 
                                            it.toComposeColor()
                                        } ?: emptyList()
                                        
                                        if (columns == 1) {
                                            com.cinetrack.ui.components.MovieListCard(
                                                movie = movie,
                                                modifier = Modifier.fillMaxWidth(),
                                                isFavorite = movie.favorite,
                                                isWatched = movie.watched,
                                                isReminder = movie.reminder,
                                                personalRating = movie.personalRating,
                                                progress = movie.progress?.toFloat() ?: 0f,
                                                folderColors = currentFolderColors,
                                                showFolderBookmarks = preferences.showFolderBookmarks,
                                                showBadges = preferences.showBadges,
                                                hazeState = localHazeState,
                                                staggerIndex = index,
                                                onPress = { onMovieClick(movie) },
                                                onLongPress = { m, pressOffset, cardPos ->
                                                    actionsState.onLongPress(m, pressOffset, cardPos)
                                                },
                                                onMessage = { viewModel.emitMessage(it) }
                                            )
                                        } else {
                                            MovieCard(
                                                movie = movie,
                                                cardWidth = cardWidth,
                                                isFavorite = movie.favorite,
                                                isWatched = movie.watched,
                                                isReminder = movie.reminder,
                                                personalRating = movie.personalRating,
                                                onPress = { onMovieClick(movie) },
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                staggerIndex = index,
                                                onLongPress = { m, pressOffset, cardPos ->
                                                    actionsState.onLongPress(m, pressOffset, cardPos)
                                                },
                                                onMessage = { viewModel.emitMessage(it) },
                                                progress = movie.progress?.toFloat() ?: 0f,
                                                folderColors = currentFolderColors,
                                                showFolderBookmarks = preferences.showFolderBookmarks
                                            )
                                        }
                                    }
                                }
                            }

                            if (showDeleteConfirm) {
                                DeleteFolderDialog(
                                    onConfirm = {
                                        viewModel.deleteFolder()
                                        onShowDeleteConfirmChange(false)
                                        onBack()
                                    },
                                    onDismiss = { onShowDeleteConfirmChange(false) },
                                    folderName = state.folder.name,
                                    hazeState = localHazeState
                                )
                            }
                            
                            if (showEditDialog) {
                                com.cinetrack.ui.components.shared.FolderEditDialog(
                                    initialName = state.folder.name,
                                    initialColor = state.folder.color ?: "#FFFFFF",
                                    editMode = folderEditMode,
                                    onDismiss = { onShowEditDialogChange(false) },
                                    onSave = { newName, newColor ->
                                        onShowEditDialogChange(false)
                                        viewModel.updateFolderDetails(newName, newColor)
                                        onFolderUpdated(newName, newColor)
                                    },
                                    hazeState = localHazeState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
