package com.cinetrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.cinetrack.ui.components.glass.hazeGlass
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.geometry.Rect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.data.Movie
import com.cinetrack.ui.components.MovieCard
import com.cinetrack.ui.components.CategoryButton
import com.cinetrack.ui.components.CategoryTabSelector
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.components.CinematicBackground

import com.cinetrack.util.toComposeColor
import com.cinetrack.ui.viewmodel.VistiViewModel
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import com.cinetrack.ui.theme.HazeStyles

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VistiScreen(
    viewModel: VistiViewModel,
    paddingValues: PaddingValues,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    isFilterVisible: Boolean = false,
    onToggleFilter: (Boolean, Rect?) -> Unit = { _, _ -> },
    onMovieClick: (Movie) -> Unit = {},
    onActionModalVisibilityChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val padding = 16.dp
    val gap = 12.dp
    val cardWidth = (screenWidth - (padding * 2) - (gap * 2)) / 3
    
    val listState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    
    val stickyHeaderHeight = 60.dp
    val topPadding = paddingValues.calculateTopPadding()
    val localHazeState = remember { HazeState() }

    LaunchedEffect(uiState.sortConfig) {
        if (uiState.movies.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    var filterButtonBounds by remember { mutableStateOf<Rect?>(null) }

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
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    state = listState,
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
                        com.cinetrack.ui.components.shared.MovieCardSkeleton(width = cardWidth)
                    }
                }
            } else if (uiState.movies.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = if (uiState.searchQuery.isEmpty()) "Non hai ancora visto nulla" else "Nessun risultato",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    state = listState,
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
                    itemsIndexed(
                        items = uiState.movies,
                        key = { index, movie: Movie -> movie.id.toString() + movie.mediaType }
                    ) { index, movie ->
                        val folderColors = remember(movie.id, uiState.movieFolderColors) {
                            uiState.movieFolderColors["${movie.mediaType}_${movie.id}"]?.map { 
                                it.toComposeColor()
                            } ?: emptyList()
                        }
                        MovieCard(
                            movie = movie,
                            cardWidth = cardWidth,
                            isFavorite = movie.favorite,
                            isWatched = movie.watched,
                            isReminder = movie.reminder,
                            progress = (movie.progress ?: 0.0).toFloat(),
                            folderColors = folderColors,
                            showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                            animatedVisibilityScope = animatedVisibilityScope,
                            staggerIndex = index,
                            onPress = { onMovieClick(movie) },
                            onAction = { viewModel.toggleWatched(movie) },
                            onLongPress = { m, pressOffset, cardPos ->
                                actionsState.onLongPress(m, pressOffset, cardPos)
                            },
                            onMessage = { viewModel.emitMessage(it) }
                        )
                    }
                }
            }
        }

        // Perfectly Centered Floating Sticky Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding + 0.dp, start = 16.dp, end = 32.dp)
                .height(stickyHeaderHeight),
            contentAlignment = Alignment.Center
        ) {
            // Category Tab Selector Island (Centered with more left offset for balance)
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .offset(x = (-28).dp),
                contentAlignment = Alignment.Center
            ) {
                // Glassmorphic Layer (Separate from content to keep text sharp)
                Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
                    Spacer(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeGlass(state = localHazeState, shape = RoundedCornerShape(50), blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = false)
                    )
                    
                    // Content Layer (Always sharp)
                    val options = listOf("FILM", "SERIE TV")
                    val selectedIndex = if (uiState.activeTab == "movie") 0 else {
                        if (uiState.activeTab == "tv") 1 else 0
                    }
                    
                    CategoryTabSelector(
                        options = options,
                        counts = listOf(uiState.movieCount, uiState.tvCount),
                        selectedIndex = selectedIndex,
                        onOptionClick = { index ->
                            viewModel.onTabChanged(if (index == 0) "movie" else "tv")
                            scope.launch {
                                listState.scrollToItem(0)
                            }
                        }
                    )
                }
            }


            val hasActiveFilters = uiState.sortConfig.selectedGenres.isNotEmpty() || 
                                   uiState.sortConfig.selectedProviders.isNotEmpty() || 
                                   uiState.sortConfig.selectedDecades.isNotEmpty()

            // Circular Filter Button (Right Aligned)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.CenterEnd)
                    .onGloballyPositioned { coords: LayoutCoordinates ->
                        filterButtonBounds = coords.boundsInRoot()
                    }
            ) {
                // Background Layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeGlass(state = localHazeState, shape = RoundedCornerShape(50), blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = false)
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
                                if (hasActiveFilters) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
                            ),
                            RoundedCornerShape(50)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Filtri",
                        tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    
                    if (hasActiveFilters) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                        )
                    }
                }
            }
        }
        }
    }
}
