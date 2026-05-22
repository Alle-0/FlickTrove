package com.cinetrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.data.Movie
import com.cinetrack.ui.components.CinematicBackground
import com.cinetrack.ui.components.MovieCard

import com.cinetrack.ui.viewmodel.DiscoverViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.HazeState
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.util.toComposeColor
import androidx.compose.animation.*
import androidx.compose.animation.core.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    paddingValues: PaddingValues,
    hazeState: HazeState? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    isFilterVisible: Boolean = false,
    onToggleFilter: (Boolean, androidx.compose.ui.geometry.Rect?) -> Unit = { _, _ -> },
    onMovieClick: (Movie) -> Unit,
    onActionModalVisibilityChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    
    // Detect scroll to end
    val endOfListReached by remember {
        derivedStateOf {
            val lastVisibleItem = lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index != null && lastVisibleItem.index >= lazyGridState.layoutInfo.totalItemsCount - 6
        }
    }

    LaunchedEffect(endOfListReached, uiState.isLoading, uiState.isNextPageLoading, uiState.isEndReached) {
        if (endOfListReached && !uiState.isLoading && !uiState.isNextPageLoading && !uiState.isEndReached) {
            viewModel.loadNextPage()
        }
    }
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val padding = 16.dp
    val gap = 12.dp
    val cardWidth = (screenWidth - (padding * 2) - (gap * 2)) / 3

    val title = if (uiState.type == "genre") {
        uiState.genreName ?: "Genere"
    } else {
        when (uiState.type) {
            "popular_movies" -> "Film Popolari"
            "now_playing_movies" -> "Al Cinema"
            "upcoming_movies" -> "Prossimamente"
            "popular_tv" -> "Serie TV Popolari"
            "airing_today_tv" -> "In Onda Oggi"
            "on_the_air_tv" -> "In Arrivo"
            "trending_movies" -> "Trending Film"
            "trending_tv" -> "Trending Serie TV"
            else -> "Scopri"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        
        com.cinetrack.ui.components.shared.MovieActionsWrapper(
            hazeState = hazeState ?: HazeState(),
            folders = uiState.folders,
            isItemInFolder = { movie, folderId -> viewModel.isItemInFolder(movie, folderId) },
            onDelete = { viewModel.deleteMovie(it) },
            onUpdateRating = { movie, rating -> viewModel.updateRating(movie, rating) },
            onUpdateNote = { movie, note -> viewModel.updateNote(movie, note) },
            onToggleFolder = { movie, folder -> viewModel.toggleItemInFolder(folder, movie) },
            onActionModalVisibilityChanged = onActionModalVisibilityChanged
        ) { actionsState ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        bottom = paddingValues.calculateBottomPadding() + 16.dp, 
                        top = paddingValues.calculateTopPadding()
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    state = lazyGridState,
                    modifier = Modifier.fillMaxSize()
                ) {

                    if (uiState.isLoading) {
                        items(count = 15, contentType = { "skeleton" }) {
                            com.cinetrack.ui.components.shared.MovieCardSkeleton(width = cardWidth)
                        }
                    } else if (uiState.movies.isEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 100.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Rounded.SearchOff,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Nessun contenuto trovato in questa categoria",
                                        color = Color.White.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.movies,
                            key = { index, movie -> movie.id.toString() + movie.mediaType }
                        ) { index, movie ->
                            val movieStatus = uiState.favorites.find { it.id == movie.id && it.mediaType == movie.mediaType }
                            MovieCard(
                                movie = movie,
                                cardWidth = cardWidth,
                                isFavorite = movieStatus?.favorite ?: false,
                                isWatched = movieStatus?.watched ?: false,
                                isReminder = movieStatus?.reminder ?: false,
                                progress = movieStatus?.progress?.toFloat() ?: 0f,
                                personalRating = movieStatus?.personalRating,
                                folderColors = uiState.movieFolderColors["${movie.mediaType}_${movie.id}"]?.map { 
                                    it.toComposeColor()
                                } ?: emptyList(),
                                showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                animatedVisibilityScope = animatedVisibilityScope,
                                staggerIndex = index,
                                onPress = { onMovieClick(movie) },
                                onAction = { viewModel.toggleFavorite(movie) },
                                onLongPress = actionsState.onLongPress,
                                onMessage = { viewModel.emitMessage(it) }
                            )
                        }
                    }

                    if (uiState.isNextPageLoading) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
