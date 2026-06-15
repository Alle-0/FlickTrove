package com.cinetrack.ui.screens

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import com.cinetrack.ui.components.shared.layoutToggleIcon
import com.cinetrack.ui.components.shared.nextGridColumns
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.components.glass.hazeGlass
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.data.Movie
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.viewmodel.FolderDetailViewModel
import com.cinetrack.ui.viewmodel.FolderDetailUiState
import com.cinetrack.ui.components.MovieCard
import com.cinetrack.ui.components.CinematicBackground
import com.cinetrack.ui.components.shared.DeleteFolderDialog
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.util.toComposeColor

import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import androidx.compose.runtime.remember

data class FolderDetailTab(
    val folderId: String,
    val folderName: String,
    val folderColor: String? = null
) : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 99u,
                title = folderName,
                icon = null
            )
        }

    @Composable
    override fun Content() {
        val viewModel = getViewModel<FolderDetailViewModel>()
        val navigator = LocalNavigator.currentOrThrow.parent ?: LocalNavigator.currentOrThrow
        val hazeState = com.cinetrack.ui.LocalHazeState.current
        val filterRequest = com.cinetrack.ui.LocalFilterRequest.current

        LaunchedEffect(folderId) {
            viewModel.initFolder(folderId)
        }

        var showDeleteConfirm by remember { mutableStateOf(false) }
        var showEditDialog by remember { mutableStateOf(false) }
        var folderEditMode by remember { mutableStateOf(com.cinetrack.ui.components.shared.FolderEditMode.NAME) }

        @OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
        FolderDetailScreenContent(
            viewModel = viewModel,
            paddingValues = com.cinetrack.ui.LocalAppPadding.current,
            hazeState = hazeState,
            showDeleteConfirm = showDeleteConfirm,
            onShowDeleteConfirmChange = { showDeleteConfirm = it },
            showEditDialog = showEditDialog,
            onShowEditDialogChange = { showEditDialog = it },
            folderEditMode = folderEditMode,
            onFolderUpdated = { _, _ -> },
            onMovieClick = { movie ->
                navigator.push(MovieDetailScreen(movie.id, movie.mediaType))
            },
            onToggleFilter = { visible, bounds -> if (visible) filterRequest?.invoke(bounds) },
            onBack = { 
                // We'll handle this back navigation by resetting the tab in MainScreen or via BackHandler here
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreenContent(
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
    onToggleFilter: (Boolean, androidx.compose.ui.geometry.Rect?) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folderColors by viewModel.movieFolderColors.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    var filterButtonBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val activeFilterConfig = com.cinetrack.ui.LocalActiveFilterConfig.current

    LaunchedEffect(uiState) {
        val successState = uiState as? FolderDetailUiState.Success
        if (successState != null) {
            activeFilterConfig.value = com.cinetrack.ui.FilterModalConfig(
                triggerBounds = null,
                isVisti = false,
                category = successState.activeTab,
                sortConfig = successState.sortConfig,
                onSortConfigChanged = { viewModel.updateSortConfig(it) }
            )
        } else {
            activeFilterConfig.value = null
        }
    }

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
                    val activeHazeState = hazeState ?: remember { HazeState() }
                    val lazyGridState = viewModel.lazyGridState
                    
                    MovieActionsWrapper(
                        hazeState = activeHazeState,
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
                                    state = activeHazeState,
                                    style = HazeStyles.PremiumDark
                                )
                        ) {
                            CinematicBackground(modifier = Modifier.fillMaxSize())
                            val columns = if (preferences.gridColumns in 1..4) preferences.gridColumns else 3
                            val gap = 8.dp
                            val padding = 16.dp
                            val cardWidth = if (columns > 1) {
                                (maxWidth - (padding * 2) - (gap * (columns - 1))) / columns
                            } else {
                                maxWidth - (padding * 2)
                            }
                            
                            LazyVerticalGrid(
                                state = lazyGridState,
                                columns = GridCells.Fixed(columns),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = paddingValues.calculateTopPadding() + androidx.compose.foundation.layout.WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 46.dp + 60.dp + 16.dp,
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
                                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_ciack),
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
                                                hazeState = activeHazeState,
                                                hasAnimatedSet = viewModel.animatedMovieIds,
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
                                                showFolderBookmarks = preferences.showFolderBookmarks,
                                                hasAnimatedSet = viewModel.animatedMovieIds
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
                                    hazeState = activeHazeState
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
                                    hazeState = activeHazeState
                                )
                            }
                        }
                    }

                    // Perfectly Centered Floating Sticky Header
                    val topPadding = paddingValues.calculateTopPadding() + androidx.compose.foundation.layout.WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 46.dp
                    val stickyHeaderHeight = 60.dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = topPadding)
                            .align(Alignment.TopCenter)
                            .zIndex(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(stickyHeaderHeight)
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val rightControlsInset = if (preferences.showLayoutToggle) 88.dp else 44.dp
                            
                            Box(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .padding(end = rightControlsInset),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
                                    Spacer(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .hazeGlass(state = activeHazeState, shape = androidx.compose.foundation.shape.RoundedCornerShape(50), blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = false)
                                    )
                                    
                                    val options = listOf("TUTTI", "FILM", "SERIE TV")
                                    val counts = listOf(state.movieCount + state.tvCount, state.movieCount, state.tvCount)
                                    val selectedIndex = when(state.activeTab) { "movie" -> 1; "tv" -> 2; else -> 0 }
                                    
                                    com.cinetrack.ui.components.CategoryTabSelector(
                                        options = options,
                                        counts = counts,
                                        selectedIndex = selectedIndex,
                                        onOptionClick = { index ->
                                            viewModel.onTabChanged(when(index) { 1 -> "movie"; 2 -> "tv"; else -> "all" })
                                        }
                                    )
                                }
                            }
                            
                            val hasActiveFilters = false // Pending folder filtering implementation
                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Layout Toggle Button
                                if (preferences.showLayoutToggle) {
                                    Box(modifier = Modifier.size(36.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .hazeGlass(
                                                    state = activeHazeState, 
                                                    shape = CircleShape, 
                                                    blurRadius = HazeStyles.SmallGlassBlurRadius, 
                                                    useOffscreenStrategy = false
                                                )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .bounceClick(scaleDown = 0.92f) {
                                                    val columns = if (preferences.gridColumns in 1..4) preferences.gridColumns else 3
                                                    viewModel.updateGridColumns(nextGridColumns(columns))
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val columns = if (preferences.gridColumns in 1..4) preferences.gridColumns else 3
                                            Icon(
                                                imageVector = layoutToggleIcon(columns),
                                                contentDescription = "Cambia Colonne",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Circular Filter Button
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .onGloballyPositioned { coords: LayoutCoordinates ->
                                            filterButtonBounds = coords.boundsInRoot()
                                        }
                                ) {
                                    // Background Layer
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .hazeGlass(
                                                state = activeHazeState, 
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50), 
                                                blurRadius = HazeStyles.SmallGlassBlurRadius, 
                                                useOffscreenStrategy = false,
                                                borderWidth = if (hasActiveFilters) 1.5.dp else 1.dp,
                                                borderColor = if (hasActiveFilters) MaterialTheme.colorScheme.primary else HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop)
                                            )
                                    )
                                    // Icon Layer
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .bounceClick(scaleDown = 0.92f) {
                                                onToggleFilter(true, filterButtonBounds)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_filtri),
                                            contentDescription = "Filtri",
                                            tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    if (hasActiveFilters) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 2.dp, y = (-2).dp)
                                                .size(10.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                .border(2.dp, Color(0xFF1E1E1E), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
