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
// AppBackground removed — using CinematicBackground directly
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
// StatsBackground removed; using CinematicBackground instead
import com.cinetrack.ui.viewmodel.RecommendationsViewModel
import com.cinetrack.ui.utils.bounceClick
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
    
    var isTinderMode by rememberSaveable { mutableStateOf(false) }
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
            if (isTinderMode) {
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
                    TinderEmptyState(
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

                                TinderMovieCard(
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
                            TinderControls(
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
            val rightControlsInset = if (!isTinderMode && uiState.preferences.showLayoutToggle) 88.dp else 44.dp

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
                if (!isTinderMode && uiState.preferences.showLayoutToggle) {
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
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), CircleShape),
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
                                isTinderMode = !isTinderMode
                            }
                            .border(
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isTinderMode) ImageVector.vectorResource(id = R.drawable.ic_grid) else ImageVector.vectorResource(id = R.drawable.ic_temi),
                            contentDescription = stringResource(R.string.recommendations_tinder_mode),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

@Composable
private fun TinderMovieCard(
    movie: Movie,
    isTop: Boolean,
    scale: Float,
    yOffset: Float,
    alpha: Float,
    swipeOffsetX: Float,
    swipeOffsetY: Float,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onActionClick: () -> Unit
) {
    val rotation = if (isTop) swipeOffsetX / 22f else 0f
    val cardHazeState = remember { HazeState() }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.translationY = yOffset.dp.toPx()
                if (isTop) {
                    this.translationX = swipeOffsetX
                    this.translationY = swipeOffsetY
                    this.rotationZ = rotation
                }
                this.alpha = alpha
                this.shadowElevation = if (isTop) 16f else 4f
                this.shape = RoundedCornerShape(48.dp)
                this.clip = true
            }
            .clip(RoundedCornerShape(48.dp))
            .then(
                if (isTop) {
                    Modifier.pointerInput(movie.id) {
                        detectDragGestures(
                            onDragEnd = onDragEnd,
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount)
                            }
                        )
                    }
                } else Modifier
            )
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                RoundedCornerShape(48.dp)
            )
            .background(Color(0xFF161618))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onActionClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = cardHazeState, style = HazeStyles.PremiumDark)
        ) {
            val posterUrl = buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, LocalImageQuality.current)
            if (posterUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(posterUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = movie.title ?: movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2E2E32),
                                    Color(0xFF151518)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_ciack),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 0f
                        )
                    )
            )
        }
        
        if (isTop) {
            val dragProgress = swipeOffsetX / 180f
            
            if (dragProgress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 40.dp, start = 32.dp)
                        .graphicsLayer {
                            rotationZ = -15f
                            this.alpha = dragProgress.coerceIn(0f, 1f)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                CircleShape
                            )
                            .border(
                                width = 2.5.dp,
                                color = Color(0xFF22C55E),
                                shape = CircleShape
                            )
                    )
                    
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.recommendations_add),
                            color = Color(0xFF22C55E),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
            
            if (dragProgress < 0f) {
                val absoluteProgress = abs(dragProgress)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 32.dp)
                        .graphicsLayer {
                            rotationZ = 15f
                            this.alpha = absoluteProgress.coerceIn(0f, 1f)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                CircleShape
                            )
                            .border(
                                width = 2.5.dp,
                                color = Color(0xFFEF4444),
                                shape = CircleShape
                            )
                    )
                    
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.recommendations_pass),
                            color = Color(0xFFEF4444),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .hazeGlass(
                        state = cardHazeState,
                        shape = RoundedCornerShape(30.dp),
                        containerColor = Color.Black.copy(alpha = 0.45f),
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .graphicsLayer { }
            ) {
                Text(
                    text = movie.title ?: movie.name ?: stringResource(R.string.recommendations_no_title),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rating = movie.voteAverage ?: 0.0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_star),
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", rating),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Text(
                        text = "•",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp
                    )
                    
                    val year = movie.releaseYear ?: movie.releaseDate?.take(4) ?: movie.firstAirDate?.take(4) ?: stringResource(R.string.recommendations_na)
                    Text(
                        text = year,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
                
                val contextLocale = LocalConfiguration.current.locales[0].language
                val genres = remember(movie.genreIds, movie.genreNamesString, contextLocale) {
                    if (!movie.genreIds.isNullOrEmpty()) {
                        movie.genreIds!!.mapNotNull { id ->
                            val defaultName = com.cinetrack.data.GenreConstants.ALL_GENRES.find { it.id == id }?.name ?: ""
                            val localized = com.cinetrack.data.GenreConstants.getLocalizedName(id, contextLocale, defaultName)
                            localized.takeIf { it.isNotEmpty() }
                        }.joinToString(", ")
                    } else {
                        movie.genreNamesString ?: movie.genres?.mapNotNull { it.name }?.joinToString(", ") ?: ""
                    }
                }
                
                if (genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = genres.uppercase(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                val overview = movie.overview
                if (!overview.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = overview,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TinderControls(
    onPass: () -> Unit,
    onInfo: () -> Unit,
    onLike: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pass button (X)
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF1C1C1E).copy(alpha = 0.75f), CircleShape)
                .border(BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f)), CircleShape)
                .bounceClick(scaleDown = 0.88f) { onPass() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                contentDescription = stringResource(R.string.recommendations_desc_pass),
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(26.dp)
            )
        }

        // Info button (i)
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF1C1C1E).copy(alpha = 0.75f), CircleShape)
                .border(BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f)), CircleShape)
                .bounceClick(scaleDown = 0.9f) { onInfo() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_documento),
                contentDescription = stringResource(R.string.recommendations_desc_info),
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(22.dp)
            )
        }

        // Like button (heart)
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF1C1C1E).copy(alpha = 0.75f), CircleShape)
                .border(BorderStroke(1.5.dp, Color(0xFF22C55E).copy(alpha = 0.5f)), CircleShape)
                .bounceClick(scaleDown = 0.88f) { onLike() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_star),
                contentDescription = stringResource(R.string.recommendations_desc_favorite),
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun TinderEmptyState(
    onRefresh: () -> Unit,
    localHazeState: dev.chrisbanes.haze.HazeState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .hazeGlass(
                        state = localHazeState,
                        shape = RoundedCornerShape(24.dp),
                        blurRadius = 16.dp,
                        containerColor = Color.Black.copy(alpha = 0.25f),
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .graphicsLayer { },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_ricarica),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = stringResource(R.string.recommendations_completed_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.recommendations_completed_desc),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                        .bounceClick(scaleDown = 0.94f) {
                            onRefresh()
                        }
                ) {
                    Text(
                        text = stringResource(R.string.recommendations_reload),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
