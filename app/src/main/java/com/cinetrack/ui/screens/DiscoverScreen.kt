package com.cinetrack.ui.screens

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.data.Movie
import com.cinetrack.ui.components.CinematicBackground
import com.cinetrack.ui.components.MovieCard

import com.cinetrack.ui.viewmodel.DiscoverViewModel
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.HazeState
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.util.toComposeColor
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.*
import androidx.compose.animation.core.*

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.HomeFilterModal

object DiscoverTab : Tab {
    var requestedType: String by androidx.compose.runtime.mutableStateOf("popular_movies")

    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(id = R.string.discover_tab_title)
            return remember(title) {
                TabOptions(
                    index = 1u,
                    title = title,
                    icon = null
                )
            }
        }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        var currentContext = context
        while (currentContext is android.content.ContextWrapper && currentContext !is androidx.activity.ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? androidx.activity.ComponentActivity
        
        val viewModel = if (activity != null) {
            androidx.hilt.navigation.compose.hiltViewModel<DiscoverViewModel>(activity)
        } else {
            androidx.hilt.navigation.compose.hiltViewModel<DiscoverViewModel>()
        }
        
        LaunchedEffect(requestedType) {
            viewModel.init(requestedType)
        }

        val paddingValues = LocalAppPadding.current
        val hazeState = LocalHazeState.current
        val navigator = LocalNavigator.currentOrThrow.parent ?: LocalNavigator.currentOrThrow

        var isFilterVisible by remember { mutableStateOf(false) }
        var isActionModalVisible by remember { mutableStateOf(false) }
        
        DiscoverScreenContent(
            viewModel = viewModel,
            paddingValues = paddingValues,
            hazeState = hazeState,
            isFilterVisible = isFilterVisible,
            onToggleFilter = { visible, _ -> isFilterVisible = visible },
            onActionModalVisibilityChanged = { isActionModalVisible = it },
            onMovieClick = { movie -> 
                navigator.push(MovieDetailScreen(movie.id, movie.mediaType))
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverScreenContent(
    viewModel: DiscoverViewModel,
    paddingValues: PaddingValues,
    hazeState: HazeState? = null,
    isFilterVisible: Boolean = false,
    onToggleFilter: (Boolean, androidx.compose.ui.geometry.Rect?) -> Unit = { _, _ -> },
    onMovieClick: (Movie) -> Unit,
    onActionModalVisibilityChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyGridState = viewModel.lazyGridState
    
    // Detect scroll to end
    val endOfListReached by remember {
        derivedStateOf {
            val lastVisibleItem = lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index != null && lastVisibleItem.index >= lazyGridState.layoutInfo.totalItemsCount - 6
        }
    }

    androidx.activity.compose.BackHandler(enabled = isFilterVisible) {
        onToggleFilter(false, null)
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
    val columns = if (uiState.preferences.gridColumns in 1..4) uiState.preferences.gridColumns else 3
    val cardWidth = if (columns > 1) {
        (screenWidth - (padding * 2) - (gap * (columns - 1))) / columns
    } else {
        screenWidth - (padding * 2)
    }

    val defaultGenre = stringResource(R.string.discover_title_genre)
    val title = if (uiState.type == "genre") {
        uiState.genreName ?: defaultGenre
    } else {
        when (uiState.type) {
            "popular_movies" -> stringResource(R.string.discover_title_popular_movies)
            "now_playing_movies" -> stringResource(R.string.discover_title_now_playing_movies)
            "upcoming_movies" -> stringResource(R.string.discover_title_upcoming_movies)
            "popular_tv" -> stringResource(R.string.discover_title_popular_tv)
            "airing_today_tv" -> stringResource(R.string.discover_title_airing_today_tv)
            "on_the_air_tv" -> stringResource(R.string.discover_title_on_the_air_tv)
            "trending_movies" -> stringResource(R.string.discover_title_trending_movies)
            "trending_tv" -> stringResource(R.string.discover_title_trending_tv)
            else -> stringResource(R.string.discover_title_default)
        }
    }

    val topPadding = paddingValues.calculateTopPadding() + androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 46.dp
    val activeHazeState = hazeState ?: remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackground(modifier = Modifier.fillMaxSize())

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
                    .haze(activeHazeState, style = HazeStyles.PremiumDark)
            ) {
                LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        bottom = paddingValues.calculateBottomPadding() + 16.dp, 
                        top = topPadding + 60.dp + 12.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    state = lazyGridState,
                    modifier = Modifier.fillMaxSize()
                ) {

                    if (uiState.isLoading) {
                        items(count = 15, contentType = { "skeleton" }) {
                            if (columns == 1) {
                                com.cinetrack.ui.components.shared.MovieListCardSkeleton()
                            } else {
                                com.cinetrack.ui.components.shared.MovieCardSkeleton(width = cardWidth)
                            }
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
                                        ImageVector.vectorResource(id = R.drawable.ic_lente),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.discover_empty_category),
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
                            val folderColors = uiState.movieFolderColors["${movie.mediaType}_${movie.id}"]?.map { 
                                it.toComposeColor()
                            } ?: emptyList()
                            
                            if (columns == 1) {
                                com.cinetrack.ui.components.MovieListCard(
                                    movie = movie,
                                    modifier = Modifier.fillMaxWidth(),
                                    isFavorite = movieStatus?.favorite ?: false,
                                    isWatched = movieStatus?.watched ?: false,
                                    isReminder = movieStatus?.reminder ?: false,
                                    personalRating = movieStatus?.personalRating,
                                    progress = movieStatus?.progress?.toFloat() ?: 0f,
                                    folderColors = folderColors,
                                    showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                    showBadges = uiState.preferences.showBadges,
                                    hasAnimatedSet = viewModel.animatedMovieIds,
                                    hazeState = hazeState,
                                    staggerIndex = index,
                                    onPress = { onMovieClick(movie) },
                                    onAction = { viewModel.toggleFavorite(movie) },
                                    onLongPress = actionsState.onLongPress,
                                    onMessage = { viewModel.emitMessage(com.cinetrack.ui.utils.UiText.DynamicString(it)) }
                                )
                            } else {
                                MovieCard(
                                    movie = movie,
                                    cardWidth = cardWidth,
                                    isFavorite = movieStatus?.favorite ?: false,
                                    isWatched = movieStatus?.watched ?: false,
                                    isReminder = movieStatus?.reminder ?: false,
                                    progress = movieStatus?.progress?.toFloat() ?: 0f,
                                    personalRating = movieStatus?.personalRating,
                                    folderColors = folderColors,
                                    showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                    hasAnimatedSet = viewModel.animatedMovieIds,
                                    staggerIndex = index,
                                    onPress = { onMovieClick(movie) },
                                    onAction = { viewModel.toggleFavorite(movie) },
                                    onLongPress = actionsState.onLongPress,
                                    onMessage = { viewModel.emitMessage(com.cinetrack.ui.utils.UiText.DynamicString(it)) }
                                )
                            }
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
