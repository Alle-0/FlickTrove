package com.cinetrack.ui.screens

import androidx.compose.ui.res.stringResource

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.data.Movie
import com.cinetrack.ui.components.*
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.ui.components.shared.layoutToggleIcon
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.components.shared.nextGridColumns
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.viewmodel.RecommendationsViewModel
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.components.recommendations.FlickMovieCard
import com.cinetrack.ui.components.recommendations.FlickControls
import com.cinetrack.ui.components.recommendations.FlickEmptyState
import com.cinetrack.ui.components.recommendations.lerp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlin.math.abs

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.cinetrack.ui.LocalAppPadding

object RecommendationsTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(R.string.recommendations_tab_title)
            return remember(title) {
                TabOptions(
                    index = 5u,
                    title = title,
                    icon = null
                )
            }
        }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        var currentContext = context
        while (currentContext is android.content.ContextWrapper && currentContext !is androidx.activity.ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? androidx.activity.ComponentActivity
        
        val viewModel = if (activity != null) {
            androidx.hilt.navigation.compose.hiltViewModel<RecommendationsViewModel>(activity)
        } else {
            androidx.hilt.navigation.compose.hiltViewModel<RecommendationsViewModel>()
        }
        val paddingValues = LocalAppPadding.current
        val navigator = LocalNavigator.currentOrThrow.parent ?: LocalNavigator.currentOrThrow
        val hazeState = com.cinetrack.ui.LocalHazeState.current

        var isActionModalVisible by remember { mutableStateOf(false) }

        RecommendationsScreenContent(
            viewModel = viewModel,
            paddingValues = paddingValues,
            hazeState = hazeState,
            onActionModalVisibilityChanged = { isActionModalVisible = it },
            onMovieClick = { movie ->
                navigator.push(MovieDetailScreen(movie.id, movie.mediaType))
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RecommendationsScreenContent(
    viewModel: RecommendationsViewModel,
    paddingValues: PaddingValues,
    hazeState: HazeState? = null,
    onMovieClick: (Movie) -> Unit,
    onActionModalVisibilityChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyGridState = viewModel.lazyGridState
    
    val localHazeState = hazeState ?: remember { dev.chrisbanes.haze.HazeState() }
    
    var isFlickMode by rememberSaveable { mutableStateOf(false) }
    var topCardIndex by rememberSaveable(uiState.mediaType) { mutableStateOf(0) }
    
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

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading && !uiState.isNextPageLoading) {
            topCardIndex = 0
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

    val topPadding = paddingValues.calculateTopPadding() + androidx.compose.foundation.layout.WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 46.dp
    val stickyHeaderHeight = 60.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Haze source: covers CinematicBackground and content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = localHazeState, style = HazeStyles.PremiumDark)
        ) {
            CinematicBackground(modifier = Modifier.fillMaxSize())

        MovieActionsWrapper(
            hazeState = localHazeState,
            folders = uiState.folders,
            isItemInFolder = { movie, folderId -> viewModel.isItemInFolder(movie, folderId) },
            onDelete = { viewModel.deleteMovie(it) },
            onUpdateRating = { movie, rating -> viewModel.updateRating(movie, rating) },
            onUpdateNote = { movie, note -> viewModel.updateNote(movie, note) },
            onToggleFolder = { movie, folder -> viewModel.toggleItemInFolder(folder, movie) },
            onActionModalVisibilityChanged = onActionModalVisibilityChanged
        ) { actionsState ->
            if (isFlickMode) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = topPadding + stickyHeaderHeight + 12.dp,
                                bottom = paddingValues.calculateBottomPadding() + 80.dp,
                                start = 24.dp,
                                end = 24.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.85f)
                                .graphicsLayer {
                                    shape = RoundedCornerShape(38.dp)
                                    clip = true
                                }
                                .border(
                                    BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    RoundedCornerShape(38.dp)
                                )
                        ) {
                            com.cinetrack.ui.components.shared.MovieCardSkeleton(
                                width = screenWidth - 48.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else if (uiState.recommendedMovies.isEmpty() || topCardIndex >= uiState.recommendedMovies.size) {
                    FlickEmptyState(
                        onRefresh = {
                            topCardIndex = 0
                            viewModel.onRefresh()
                        },
                        localHazeState = localHazeState
                    )
                } else {
                    val movies = uiState.recommendedMovies
                    val scope = rememberCoroutineScope()
                    val haptic = LocalHapticFeedback.current

                    val swipeOffsetX = remember { Animatable(0f) }
                    val swipeOffsetY = remember { Animatable(0f) }

                    LaunchedEffect(topCardIndex) {
                        swipeOffsetX.snapTo(0f)
                        swipeOffsetY.snapTo(0f)
                    }

                    LaunchedEffect(topCardIndex, movies.size, uiState.isLoading, uiState.isNextPageLoading, uiState.isEndReached) {
                        if (topCardIndex >= movies.size - 4 && !uiState.isLoading && !uiState.isNextPageLoading && !uiState.isEndReached) {
                            viewModel.loadNextPage()
                        }
                    }

                    val swipeRight = { movie: Movie ->
                        scope.launch {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            swipeOffsetX.animateTo(1000f, tween(250, easing = FastOutSlowInEasing))
                            viewModel.toggleFavorite(movie) {
                                topCardIndex = maxOf(0, topCardIndex - 1)
                            }
                            topCardIndex++
                        }
                    }

                    val swipeLeft = {
                        scope.launch {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            swipeOffsetX.animateTo(-1000f, tween(250, easing = FastOutSlowInEasing))
                            topCardIndex++
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = topPadding + stickyHeaderHeight + 8.dp,
                                bottom = paddingValues.calculateBottomPadding() + 8.dp,
                                start = 20.dp,
                                end = 20.dp
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val cardsToShow = (topCardIndex until minOf(movies.size, topCardIndex + 3)).toList()
                            cardsToShow.reversed().forEach { index ->
                                val movie = movies[index]
                                val isTopCard = index == topCardIndex

                                val dragProgress = (abs(swipeOffsetX.value) / 300f).coerceIn(0f, 1f)

                                val scale: Float
                                val yOffset: Float
                                val alpha: Float

                                if (isTopCard) {
                                    scale = 1f
                                    yOffset = 0f
                                    alpha = 1f
                                } else {
                                    val cardPositionInStack = index - topCardIndex
                                    if (cardPositionInStack == 1) {
                                        scale = lerp(0.94f, 1f, dragProgress)
                                        yOffset = lerp(16f, 0f, dragProgress)
                                        alpha = lerp(0.85f, 1f, dragProgress)
                                    } else {
                                        scale = lerp(0.88f, 0.94f, dragProgress)
                                        yOffset = lerp(32f, 16f, dragProgress)
                                        alpha = lerp(0.6f, 0.85f, dragProgress)
                                    }
                                }

                                FlickMovieCard(
                                    movie = movie,
                                    isTop = isTopCard,
                                    scale = scale,
                                    yOffset = yOffset,
                                    alpha = alpha,
                                    swipeOffsetX = if (isTopCard) swipeOffsetX.value else 0f,
                                    swipeOffsetY = if (isTopCard) swipeOffsetY.value else 0f,
                                    onDrag = { dragAmount ->
                                        if (isTopCard) {
                                            scope.launch {
                                                swipeOffsetX.snapTo(swipeOffsetX.value + dragAmount.x)
                                                swipeOffsetY.snapTo(swipeOffsetY.value + dragAmount.y)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (isTopCard) {
                                            val threshold = 180f
                                            val velocityX = swipeOffsetX.value
                                            if (velocityX > threshold) {
                                                swipeRight(movie)
                                            } else if (velocityX < -threshold) {
                                                swipeLeft()
                                            } else {
                                                scope.launch {
                                                    swipeOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                                }
                                                scope.launch {
                                                    swipeOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                                }
                                            }
                                        }
                                    },
                                    onActionClick = { 
                                        onMovieClick(movie)
                                    }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .zIndex(1f)
                        ) {
                            FlickControls(
                                onPass = {
                                    if (topCardIndex < movies.size) {
                                        swipeLeft()
                                    }
                                },
                                onInfo = {
                                    if (topCardIndex < movies.size) {
                                        onMovieClick(movies[topCardIndex])
                                    }
                                },
                                onLike = {
                                    if (topCardIndex < movies.size) {
                                        val movie = movies[topCardIndex]
                                        swipeRight(movie)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(
                        top = topPadding + stickyHeaderHeight + 12.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    state = lazyGridState,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    if (uiState.isLoading) {
                        items(count = 15, contentType = { "skeleton" }) {
                            com.cinetrack.ui.components.shared.MovieCardSkeleton(width = cardWidth)
                        }
                    } else if (uiState.recommendedMovies.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.ic_star),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    stringResource(R.string.recommendations_empty_1),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 24.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.recommendations_empty_2),
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.recommendedMovies,
                            key = { _, movie -> movie.id.toString() + movie.mediaType }
                        ) { index, movie ->
                            val movieStatus = uiState.favorites.find { it.id == movie.id && it.mediaType == movie.mediaType }
                            if (columns == 1) {
                                com.cinetrack.ui.components.MovieListCard(
                                    movie = movie.copy(personalRating = movieStatus?.personalRating),
                                    modifier = Modifier.fillMaxWidth(),
                                    isFavorite = movieStatus?.favorite ?: false,
                                    isWatched = movieStatus?.watched ?: false,
                                    isReminder = movieStatus?.reminder ?: false,
                                    progress = movieStatus?.progress?.toFloat() ?: 0f,
                                    folderColors = viewModel.getMovieFolderColors(movie),
                                    showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                    hasAnimatedSet = viewModel.animatedMovieIds,
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
                                    folderColors = viewModel.getMovieFolderColors(movie),
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

                        if (uiState.isNextPageLoading) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
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
        } // end inner haze-source Box

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(1f)
                .padding(top = topPadding + 0.dp, start = 24.dp, end = 24.dp)
                .height(stickyHeaderHeight),
            contentAlignment = Alignment.Center
        ) {
            val rightControlsInset = if (!isFlickMode && uiState.preferences.showLayoutToggle) 88.dp else 44.dp

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

                val options = listOf(stringResource(R.string.folder_detail_tab_movies), stringResource(R.string.folder_detail_tab_tv))
                val selectedIndex = if (uiState.mediaType == "movie") 0 else 1

                CategoryTabSelector(
                    options = options,
                    selectedIndex = selectedIndex,
                    onOptionClick = { index ->
                        viewModel.onMediaTypeChanged(if (index == 0) "movie" else "tv")
                    },
                    modifier = Modifier
                )
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isFlickMode && uiState.preferences.showLayoutToggle) {
                    Box(modifier = Modifier.size(36.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeGlass(state = localHazeState, shape = CircleShape, blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = false)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .bounceClick(scaleDown = 0.92f) {
                                    val nextColumns = nextGridColumns(uiState.preferences.gridColumns)
                                    viewModel.updatePreferences(uiState.preferences.copy(gridColumns = nextColumns))
                                }
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = layoutToggleIcon(uiState.preferences.gridColumns),
                                contentDescription = stringResource(R.string.folder_detail_change_columns),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.size(36.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeGlass(state = localHazeState, shape = CircleShape, blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = false)
                    )
    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { }
                            .bounceClick(scaleDown = 0.92f) {
                                isFlickMode = !isFlickMode
                            }
                            .border(
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFlickMode) ImageVector.vectorResource(id = R.drawable.ic_grid) else ImageVector.vectorResource(id = R.drawable.ic_temi),
                            contentDescription = stringResource(R.string.recommendations_flick_mode),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

