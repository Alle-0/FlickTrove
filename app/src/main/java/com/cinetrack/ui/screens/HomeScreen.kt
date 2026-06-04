package com.cinetrack.ui.screens

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.util.toComposeColor
import com.cinetrack.data.Movie
import com.cinetrack.ui.components.*
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.layoutToggleIcon
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.components.shared.nextGridColumns
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.HomeViewModel
import dev.chrisbanes.haze.*
import kotlinx.coroutines.launch
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    paddingValues: PaddingValues,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    isFilterVisible: Boolean = false,
    onToggleFilter: (Boolean, androidx.compose.ui.geometry.Rect?) -> Unit = { _, _ -> },
    onActionModalVisibilityChanged: (Boolean) -> Unit = {},
    onMovieClick: (Movie) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val columns = if (uiState.preferences.gridColumns in 1..3) uiState.preferences.gridColumns else 3
    val padding = 16.dp
    val gap = 12.dp
    val cardWidth = if (columns > 1) {
        (screenWidth - (padding * 2) - (gap * (columns - 1))) / columns
    } else {
        screenWidth - (padding * 2)
    }
    
    var filterButtonBounds by remember { mutableStateOf<Rect?>(null) }
    val scope = rememberCoroutineScope()
    
    val movieGridState = rememberLazyGridState()
    val tvGridState = rememberLazyGridState()
    
    val currentGridState = if (uiState.activeTab == "movie") movieGridState else tvGridState
    
    LaunchedEffect(uiState.sortConfig) {
        if (uiState.movies.isNotEmpty()) {
            movieGridState.scrollToItem(0)
            tvGridState.scrollToItem(0)
        }
    }
    
    val context = LocalContext.current

    androidx.activity.compose.BackHandler(enabled = isFilterVisible) {
        onToggleFilter(false, null)
    }

    val stickyHeaderHeight = 60.dp
    val topPadding = paddingValues.calculateTopPadding()
    val localHazeState = remember { HazeState() }

    MovieActionsWrapper(
        hazeState = hazeState ?: HazeState(),
        folders = uiState.folders,
        isItemInFolder = { movie, folderId ->
            uiState.folders.find { it.id == folderId }?.itemIds?.contains("${movie.mediaType}_${movie.id}") ?: false
        },
        onDelete = { viewModel.deleteMovie(it) },
        onUpdateRating = { movie, rating -> viewModel.updateRating(movie, rating) },
        onUpdateNote = { movie, note -> viewModel.updateNote(movie, note) },
        onToggleFolder = { movie, folder -> viewModel.toggleItemInFolder(folder, movie) },
        onActionModalVisibilityChanged = onActionModalVisibilityChanged
    ) { actionsState ->
        Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(localHazeState, style = HazeStyles.PremiumDark)
            ) {
            if (uiState.isLoading) {
                LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        bottom = paddingValues.calculateBottomPadding() + 16.dp, 
                        top = topPadding + stickyHeaderHeight + 12.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false
                ) {
                    items(count = 15, contentType = { "skeleton" }) {
                        if (columns == 1) {
                            com.cinetrack.ui.components.shared.MovieListCardSkeleton()
                        } else {
                            com.cinetrack.ui.components.shared.MovieCardSkeleton(width = cardWidth)
                        }
                    }
                }
            } else if (uiState.movies.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = if (uiState.searchQuery.isEmpty()) "La tua lista è vuota" else "Nessun risultato",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                val stableOnPress: (Movie) -> Unit = remember(onMovieClick) { { m -> onMovieClick(m) } }
                val stableOnAction: (Movie) -> Unit = remember(viewModel) { { m -> viewModel.toggleWatched(m) } }
                val stableOnLongPress: (Movie, androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit = remember(actionsState) {
                    { m, offset, pos -> actionsState.onLongPress(m, offset, pos) }
                }
                val stableOnMessage: (String) -> Unit = remember(viewModel) {
                    { msg -> viewModel.emitMessage(msg) }
                }

                LazyVerticalGrid(
                    state = currentGridState,
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        bottom = paddingValues.calculateBottomPadding() + 16.dp, 
                        top = topPadding + stickyHeaderHeight + 12.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val sections = listOf(
                        "" to uiState.releasedMovies,
                        "Non ancora usciti" to uiState.unreleasedMovies
                    ).filter { it.second.isNotEmpty() }

                    sections.forEachIndexed { sectionIndex, (title, items) ->
                        if (title.isNotEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = title.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(
                                        top = if (sectionIndex > 0) 24.dp else 4.dp, 
                                        bottom = 8.dp
                                    )
                                )
                            }
                        }

                        itemsIndexed(
                            items = items,
                            key = { index, movie: Movie -> movie.id.toString() + movie.mediaType },
                            contentType = { _, _ -> "movie_card" }
                        ) { index, movie ->
                            val posterUrl = buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, LocalImageQuality.current)
                            val folderColors = remember(movie.id, uiState.movieFolderColors) {
                                uiState.movieFolderColors["${movie.mediaType}_${movie.id}"]?.map { 
                                    it.toComposeColor()
                                } ?: emptyList()
                            }

                            if (columns == 1) {
                                com.cinetrack.ui.components.MovieListCard(
                                    movie = movie,
                                    isFavorite = movie.favorite,
                                    isWatched = movie.watched,
                                    isReminder = movie.reminder,
                                    progress = (movie.progress ?: 0.0).toFloat(),
                                    folderColors = folderColors,
                                    showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                    showBadges = uiState.preferences.showBadges,
                                    hazeState = hazeState,
                                    staggerIndex = index,
                                    onPress = stableOnPress,
                                    onAction = stableOnAction,
                                    onLongPress = stableOnLongPress,
                                    onMessage = stableOnMessage
                                )
                            } else {
                                MovieCard(
                                    movie = movie,
                                    cardWidth = cardWidth,
                                    isFavorite = movie.favorite,
                                    isWatched = movie.watched,
                                    isReminder = movie.reminder,
                                    progress = (movie.progress ?: 0.0).toFloat(),
                                    folderColors = folderColors,
                                    showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                    showBadges = uiState.preferences.showBadges,
                                    hazeState = hazeState,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    staggerIndex = index,
                                    onPress = stableOnPress,
                                    onAction = stableOnAction,
                                    onLongPress = stableOnLongPress,
                                    onMessage = stableOnMessage
                                )
                            }
                        }
                    }
                }
            }
        }

        // Perfectly Centered Floating Sticky Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(1f)
                .padding(top = topPadding + 0.dp, start = 24.dp, end = 24.dp)
                .height(stickyHeaderHeight),
            contentAlignment = Alignment.Center
        ) {
                val rightControlsInset = if (uiState.preferences.showLayoutToggle) 88.dp else 44.dp

                // Category Tab Selector Island (Centered with more left offset for balance)
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(end = rightControlsInset),
                    contentAlignment = Alignment.Center
                ) {
                    Spacer(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeGlass(state = localHazeState, shape = CircleShape, blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = false)
                    )
                    
                    val options = listOf("FILM", "SERIE TV")
                    val selectedIndex = if (uiState.activeTab == "movie") 0 else 1
                    
                    CategoryTabSelector(
                        options = options,
                        counts = listOf(uiState.movieCount, uiState.tvCount),
                        selectedIndex = selectedIndex,
                        onOptionClick = { index ->
                            viewModel.onTabChanged(if (index == 0) "movie" else "tv")
                            scope.launch {
                                if (index == 0) movieGridState.scrollToItem(0) else tvGridState.scrollToItem(0)
                            }
                        }
                    )
                }

                val hasActiveFilters = uiState.sortConfig.selectedGenres.isNotEmpty() || 
                                       uiState.sortConfig.selectedProviders.isNotEmpty() || 
                                       uiState.sortConfig.selectedDecades.isNotEmpty()

                // Circular Layout Toggle & Filter Buttons (Right Aligned)
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Layout Toggle Button
                    if (uiState.preferences.showLayoutToggle) {
                        Box(
                            modifier = Modifier.size(36.dp)
                        ) {
                            // Background Layer
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .hazeGlass(state = localHazeState, shape = CircleShape, blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = false)
                            )

                            // Interactive Content Layer
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .bounceClick(scaleDown = 0.92f) { 
                                        viewModel.updateGridColumns(nextGridColumns(columns))
                                    }
                                    .border(
                                        BorderStroke(1.dp, HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop)),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = layoutToggleIcon(columns),
                                    contentDescription = "Cambia Layout",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Filter Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .onGloballyPositioned { coords: androidx.compose.ui.layout.LayoutCoordinates ->
                                filterButtonBounds = coords.boundsInRoot()
                            }
                    ) {
                        // Background Layer
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeGlass(state = localHazeState, shape = CircleShape, blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = false)
                        )

                        // Interactive Content Layer
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .bounceClick(scaleDown = 0.92f) { 
                                    onToggleFilter(true, filterButtonBounds) 
                                }
                                .border(
                                    BorderStroke(
                                        if (hasActiveFilters) 1.5.dp else 1.dp, 
                                        if (hasActiveFilters) MaterialTheme.colorScheme.primary else HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_filtri),
                                contentDescription = "Filtri",
                                tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
