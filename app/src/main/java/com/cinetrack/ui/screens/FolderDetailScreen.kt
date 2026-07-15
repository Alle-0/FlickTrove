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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
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
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.viewmodel.FolderDetailViewModel
import com.cinetrack.ui.viewmodel.FolderDetailUiState
import com.cinetrack.ui.components.card.MovieCard
import com.cinetrack.ui.components.common.CinematicBackground
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
import com.cinetrack.ui.components.card.MovieListCard

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

class BoundsHolder { var rect: androidx.compose.ui.geometry.Rect? = null }

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
    val filterButtonBounds = remember { BoundsHolder() }
    val activeFilterConfig = com.cinetrack.ui.LocalActiveFilterConfig.current
    
    val lazyGridState = rememberLazyGridState()

    val successStateForScroll = uiState as? FolderDetailUiState.Success
    val currentActiveTab = successStateForScroll?.activeTab ?: "all"
    val currentSortConfig = successStateForScroll?.sortConfig ?: com.cinetrack.data.model.SortConfig()

    LaunchedEffect(currentActiveTab, currentSortConfig, successStateForScroll != null) {
        if (successStateForScroll != null) {
            activeFilterConfig.value = com.cinetrack.ui.FilterModalConfig(
                triggerBounds = null,
                isVisti = false,
                category = currentActiveTab,
                sortConfig = currentSortConfig,
                onSortConfigChanged = { viewModel.updateSortConfig(it) }
            )
        } else {
            activeFilterConfig.value = null
        }
    }

    DisposableEffect(Unit) {
        onDispose { activeFilterConfig.value = null }
    }

    LaunchedEffect(successStateForScroll?.sortConfig, successStateForScroll?.activeTab) {
        if (successStateForScroll != null && successStateForScroll.movies.isNotEmpty()) {
            lazyGridState.scrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = uiState) {
                is FolderDetailUiState.Loading -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = if (preferences.gridColumns in 1..4) preferences.gridColumns else 3
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
                                top = paddingValues.calculateTopPadding() + androidx.compose.foundation.layout.WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 46.dp + 60.dp + 16.dp,
                                bottom = paddingValues.calculateBottomPadding() + 32.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            userScrollEnabled = false
                        ) {
                            items(12, contentType = { "skeleton" }) {
                                if (columns == 1) {
                                    com.cinetrack.ui.components.shared.MovieListCardSkeleton()
                                } else {
                                    com.cinetrack.ui.components.shared.MovieCardSkeleton(width = cardWidth)
                                }
                            }
                        }
                    }
                }
                is FolderDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = stringResource(R.string.folder_detail_error),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = state.message, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
                is FolderDetailUiState.Success -> {
                    val externalHazeState = hazeState ?: remember { HazeState() }
                    val internalHazeState = remember { HazeState() }
                    
                    Box(modifier = Modifier.fillMaxSize().haze(state = externalHazeState, style = HazeStyles.PremiumDark)) {
                        MovieActionsWrapper(
                        hazeState = externalHazeState,
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
                            modifier = Modifier.fillMaxSize().haze(state = internalHazeState, style = HazeStyles.PremiumDark)
                        ) {
                            CompositionLocalProvider(com.cinetrack.ui.LocalHazeState provides internalHazeState) {
                                CinematicBackground(modifier = Modifier.fillMaxSize())
                            val columns = if (preferences.gridColumns in 1..4) preferences.gridColumns else 3
                            val gap = 8.dp
                            val padding = 16.dp
                            val cardWidth = if (columns > 1) {
                                (maxWidth - (padding * 2) - (gap * (columns - 1))) / columns
                            } else {
                                maxWidth - (padding * 2)
                            }
                            
                            val currentOnMovieClick by rememberUpdatedState(onMovieClick)
                            val currentOnLongPress by rememberUpdatedState { m: Movie, p: androidx.compose.ui.geometry.Offset, c: androidx.compose.ui.geometry.Offset -> actionsState.onLongPress(m, p, c) }
                            val currentOnMessage by rememberUpdatedState(viewModel::emitMessage)

                            val handlePress: (Movie) -> Unit = remember { { m -> currentOnMovieClick(m) } }
                            val handleLongPress: (Movie, androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit = remember { { m, p, c -> currentOnLongPress(m, p, c) } }
                            val handleMessage: (String) -> Unit = remember { { msg -> currentOnMessage(com.cinetrack.ui.utils.UiText.DynamicString(msg)) } }

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
                                                    text = stringResource(R.string.folder_detail_empty),
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    itemsIndexed(state.movies, key = { index, movie -> movie.id }) { index, movie ->
                                        val currentFolderColors = remember(folderColors[movie.compositeId]) {
                                            folderColors[movie.compositeId]?.map { 
                                                it.toComposeColor()
                                            } ?: emptyList()
                                        }
                                        
                                        if (columns == 1) {
                                            com.cinetrack.ui.components.card.MovieListCard(
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
                                                showAdvancedBadges = true,
                                                hazeState = internalHazeState,
                                                hasAnimatedSet = viewModel.animatedMovieIds,
                                                staggerIndex = index,
                                                onPress = handlePress,
                                                onLongPress = handleLongPress,
                                                onMessage = handleMessage
                                            )
                                        } else {
                                            MovieCard(
                                                movie = movie,
                                                cardWidth = cardWidth,
                                                isFavorite = movie.favorite,
                                                isWatched = movie.watched,
                                                isReminder = movie.reminder,
                                                personalRating = movie.personalRating,
                                                onPress = handlePress,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                staggerIndex = index,
                                                onLongPress = handleLongPress,
                                                onMessage = handleMessage,
                                                progress = movie.progress?.toFloat() ?: 0f,
                                                folderColors = currentFolderColors,
                                                showFolderBookmarks = preferences.showFolderBookmarks,
                                                showBadges = preferences.showBadges,
                                                showAdvancedBadges = true,
                                                hasAnimatedSet = viewModel.animatedMovieIds,
                                                hazeState = internalHazeState
                                            )
                                        }
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
                                    hazeState = externalHazeState
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
                                    hazeState = externalHazeState
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
                                            .hazeGlass(state = internalHazeState, shape = androidx.compose.foundation.shape.RoundedCornerShape(50), blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = false)
                                    )
                                    
                                    val options = listOf(stringResource(R.string.folder_detail_tab_all), stringResource(R.string.folder_detail_tab_movies), stringResource(R.string.folder_detail_tab_tv))
                                    val counts = listOf(state.movieCount + state.tvCount, state.movieCount, state.tvCount)
                                    val selectedIndex = when(state.activeTab) { "movie" -> 1; "tv" -> 2; else -> 0 }
                                    
                                    com.cinetrack.ui.components.common.CategoryTabSelector(
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
                                                    state = internalHazeState, 
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
                                                contentDescription = stringResource(R.string.folder_detail_change_columns),
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
                                                filterButtonBounds.rect = coords.boundsInRoot()
                                            }                              ) {
                                    // Background Layer
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .hazeGlass(
                                                state = internalHazeState, 
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
                                                onToggleFilter(true, filterButtonBounds.rect)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_filtri),
                                            contentDescription = stringResource(R.string.folder_detail_filters),
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
}
