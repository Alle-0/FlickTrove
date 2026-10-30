package com.cinetrack.ui.screens

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.saveable.rememberSaveable
import com.cinetrack.ui.components.common.LocalSymbiontPullState
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.Alignment
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.BoxOfficeMovie
import com.cinetrack.ui.viewmodel.BoxOfficeCategory
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.NewsItem
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalFilterRequest
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.card.MovieCard
import com.cinetrack.ui.components.card.ShowMoreCard
import com.cinetrack.ui.components.card.BackdropMovieCard
import com.cinetrack.ui.components.card.NewsArticleCard
import com.cinetrack.ui.components.card.Top10MovieCard
import com.cinetrack.ui.components.common.CategoryTabSelector
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.home.*
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.components.shared.MovieCardSkeleton
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.utils.bounceClick
import kotlinx.coroutines.launch
import com.cinetrack.ui.viewmodel.HomeViewModel
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.cinetrack.util.toComposeColor

object HomeFeedTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(id = R.string.home_tab_title)
            return remember(title) {
                TabOptions(
                    index = 0u,
                    title = title,
                    icon = null
                )
            }
        }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        var currentContext = context
        while (currentContext is android.content.ContextWrapper && currentContext !is ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? ComponentActivity
        
        val viewModel = if (activity != null) {
            androidx.hilt.navigation.compose.hiltViewModel<HomeViewModel>(activity)
        } else {
            androidx.hilt.navigation.compose.hiltViewModel<HomeViewModel>()
        }
        val paddingValues = LocalAppPadding.current
        val activeHazeState = LocalHazeState.current ?: remember { HazeState() }
        val surpriseMeRequest = com.cinetrack.ui.LocalSurpriseMeRequest.current
        val filterRequest = LocalFilterRequest.current
        val navigator = LocalNavigator.currentOrThrow.parent ?: LocalNavigator.currentOrThrow
        val tabNavigator = LocalTabNavigator.current

        HomeFeedScreenContent(
            viewModel = viewModel,
            paddingValues = paddingValues,
            hazeState = activeHazeState,
            isFilterVisible = false,
            onToggleFilter = { visible, bounds -> if (visible) filterRequest?.invoke(bounds) },
            onMovieClick = { movie -> 
                val activeTab = viewModel.uiState.value.activeTab
                val defaultType = if (activeTab == "tv") "tv" else "movie"
                val finalType = if (movie.mediaType.isNotEmpty()) movie.mediaType else defaultType
                navigator.push(
                    MovieDetailScreen(
                        movieId = movie.id,
                        mediaType = finalType,
                        preloadedTitle = movie.title ?: movie.name,
                        preloadedPosterPath = movie.posterPath,
                        preloadedBackdropPath = movie.backdropPath,
                        preloadedLogoPath = movie.logoPath,
                        preloadedAccentColor = movie.accentColor
                    )
                )
            },
            onBoxOfficeClick = {
                BoxOfficeTab.requestedCategory = BoxOfficeCategory.WEEKEND
                tabNavigator.current = BoxOfficeTab
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeFeedScreenContent(
    viewModel: HomeViewModel,
    paddingValues: PaddingValues,
    hazeState: HazeState? = null,
    isFilterVisible: Boolean = false,
    onToggleFilter: (Boolean, Rect?) -> Unit = { _, _ -> },
    onMovieClick: (Movie) -> Unit = {},
    onBoxOfficeClick: () -> Unit = {}
) {
    val tabNavigator = LocalTabNavigator.current    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val troveRevealedToday by viewModel.troveRevealedToday.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val configuration = LocalConfiguration.current
    
    var filterButtonBounds = remember { arrayOf<Rect?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    androidx.activity.compose.BackHandler(enabled = isFilterVisible) {
        onToggleFilter(false, null)
    }

    val stickyHeaderHeight = 60.dp
    val topPadding = paddingValues.calculateTopPadding() + androidx.compose.foundation.layout.WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 46.dp
    val activeHazeState = hazeState ?: remember { HazeState() }
    val surpriseMeRequest = com.cinetrack.ui.LocalSurpriseMeRequest.current

    val isTv = uiState.activeTab == "tv"
    
    // Pick the right lists based on activeTab
    val popularList = if (isTv) uiState.popularTv else uiState.popularMovies
    val upcomingList = if (isTv) uiState.upcomingTv else uiState.upcomingMovies
    val recommendedList = if (isTv) uiState.recommendedTv else uiState.recommendedMovies
    val nowPlayingList = if (isTv) uiState.nowStreamingTv else uiState.nowPlayingMovies
    val top10List = if (isTv) uiState.top10Tv else uiState.top10Movies
    val movieActions = com.cinetrack.ui.components.shared.LocalMovieActions.current
    
    SideEffect {
        movieActions.setupCallbacks(
            folders = uiState.folders,
            isItemInFolder = { movie, folderId ->
                uiState.folders.find { it.id == folderId }?.itemIds?.contains("${if(movie.mediaType.isNotEmpty()) movie.mediaType else uiState.activeTab}_${movie.id}") ?: false
            },
            onDelete = { viewModel.deleteMovie(it) },
            onUpdateRating = { movie, rating -> viewModel.updateRating(movie, rating) },
            onUpdateNote = { movie, note -> viewModel.updateNote(movie, note) },
            onToggleFolder = { movie, folder -> viewModel.toggleItemInFolder(folder, movie) }
        )
    }

    val stableOnLongPress: (Movie, androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit = remember(movieActions, uiState.folders, viewModel) {
        { m, offset, pos ->
            movieActions.setupCallbacks(
                folders = uiState.folders,
                isItemInFolder = { movie, folderId ->
                    uiState.folders.find { it.id == folderId }?.itemIds?.contains("${if(movie.mediaType.isNotEmpty()) movie.mediaType else uiState.activeTab}_${movie.id}") ?: false
                },
                onDelete = { viewModel.deleteMovie(it) },
                onUpdateRating = { movie, rating -> viewModel.updateRating(movie, rating) },
                onUpdateNote = { movie, note -> viewModel.updateNote(movie, note) },
                onToggleFolder = { movie, folder -> viewModel.toggleItemInFolder(folder, movie) }
            )
            movieActions.openActionsPopup(m, offset, pos)
        }
    }

    val stableOnAction: (Movie) -> Unit = remember(viewModel) { { m -> viewModel.toggleWatched(m) } }
    val stableOnMessage: (String) -> Unit = remember(viewModel) {
        { msg -> viewModel.emitMessage(com.cinetrack.ui.utils.UiText.DynamicString(msg)) }
    }

    val localMoviesMap = remember(uiState.allLocalMovies) {
        uiState.allLocalMovies.associateBy { "${it.mediaType}_${it.id}" }
    }

    val precomputedFolderColors = remember(uiState.movieFolderColors) {
        uiState.movieFolderColors.mapValues { entry ->
            entry.value.map { it.toComposeColor() }
        }
    }

    val getMovieFolderColors = remember(precomputedFolderColors, uiState.activeTab) {
        { movie: Movie ->
            val compositeId = "${if (movie.mediaType.isNotEmpty()) movie.mediaType else uiState.activeTab}_${movie.id}"
            precomputedFolderColors[compositeId] ?: emptyList()
        }
    }

    val resolveLocalMovie = remember(localMoviesMap) {
        { remoteMovie: Movie ->
            val local = localMoviesMap["${remoteMovie.mediaType}_${remoteMovie.id}"]
            if (local != null) {
                local.copy(
                    genreIds = if (!remoteMovie.genreIds.isNullOrEmpty()) remoteMovie.genreIds else local.genreIds
                ).apply {
                    this.logoPath = remoteMovie.logoPath ?: local.logoPath
                    this.matchScore = remoteMovie.matchScore ?: local.matchScore
                }
            } else {
                remoteMovie
            }
        }
    }

    val getMovieProgress = remember(localMoviesMap) {
        { movie: Movie ->
            val local = localMoviesMap["${movie.mediaType}_${movie.id}"]
            val effective = local ?: movie
            (effective.progress ?: 0.0).toFloat().let { p ->
                if (p > 0f) p
                else if (effective.mediaType == "tv" && !effective.watchedEpisodes.isNullOrEmpty() && (effective.numberOfEpisodes ?: 0) > 0) {
                    val totalWatched = effective.watchedEpisodes!!.filter { it.key != "0" }.values.sumOf { it.size }
                    (totalWatched.toFloat() / effective.numberOfEpisodes!!.toFloat()).coerceIn(0f, 1f)
                } else 0f
            }
        }
    }

    val isMovieFavorite = remember(localMoviesMap) {
        { movie: Movie ->
            movie.favorite || localMoviesMap["${movie.mediaType}_${movie.id}"]?.favorite == true
        }
    }

    val isMovieWatched = remember(localMoviesMap) {
        { movie: Movie ->
            movie.watched || localMoviesMap["${movie.mediaType}_${movie.id}"]?.watched == true
        }
    }

    val isMovieReminder = remember(localMoviesMap) {
        { movie: Movie ->
            movie.reminder || localMoviesMap["${movie.mediaType}_${movie.id}"]?.reminder == true
        }
    }

    val heroList = remember(isTv, uiState.trendingTv, uiState.trendingMovies) {
        val trendingList = if (isTv) uiState.trendingTv else uiState.trendingMovies
        trendingList.take(10)
    }

    val watchlistList = remember(isTv, uiState.allLocalMovies) {
        val targetType = if (isTv) "tv" else "movie"
        uiState.allLocalMovies
            .filter { it.favorite && !it.watched && it.mediaType == targetType && it.isReleased }
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }
    }
    
    // Virtual infinite pager state
    val initialPage = if (heroList.isNotEmpty()) {
        5000 - (5000 % heroList.size)
    } else 0
    val pagerState = rememberPagerState(initialPage = viewModel.feedPagerIndex ?: initialPage) { 10000 }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.feedPagerIndex = pagerState.currentPage
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        CinematicBackground(modifier = Modifier.fillMaxSize())
        val symbiontPullState = LocalSymbiontPullState.current
        val currentPullFraction = pullToRefreshState.distanceFraction
        SideEffect {
            symbiontPullState.progress = currentPullFraction
            symbiontPullState.isRefreshing = isRefreshing
        }
        DisposableEffect(Unit) {
            onDispose {
                symbiontPullState.progress = 0f
                symbiontPullState.isRefreshing = false
            }
        }

        Box(
            modifier = Modifier            
                .fillMaxSize()
                .haze(activeHazeState, style = HazeStyles.PremiumDark)
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.pullToRefresh() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize(),
                indicator = {}
            ) {
                LazyColumn(
                state = viewModel.feedListState,
                contentPadding = PaddingValues(
                    bottom = paddingValues.calculateBottomPadding() + 80.dp, // Spazio extra richiesto
                    top = 165.dp
                ),
                verticalArrangement = Arrangement.spacedBy(48.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.hasFeedError && heroList.isEmpty() && popularList.isEmpty()) {
                    item {
                        HomeFeedErrorCard(onRetry = { viewModel.retryFeed() })
                    }
                } else if (uiState.isFeedLoading && heroList.isEmpty() && popularList.isEmpty()) {
                    homeFeedSkeletons()
                }

                // HERO CAROUSEL
                if (heroList.isNotEmpty()) {
                    item(key = "section_hero_spotlight") {
                        HeroSpotlightCarousel(
                            movies = heroList,
                            pagerState = pagerState,
                            onMovieClick = onMovieClick,
                            onResolveLogo = { movie -> viewModel.resolveMovieLogo(movie) }
                        )
                    }
                }

                // DYNAMICALLY REORDERABLE & CUSTOMIZABLE HOME SECTIONS
                uiState.preferences.homeSectionOrder.forEach { sectionKey ->
                    when (sectionKey) {
                        com.cinetrack.data.model.HomeFeedSectionConstants.BOX_OFFICE -> {
                            // BOX OFFICE WEEKEND WINNER (Movies only)
                            if (uiState.preferences.showHomeBoxOffice && !isTv && uiState.boxOfficeWinner != null) {
                                val winner = uiState.boxOfficeWinner!!
                                item(key = "section_box_office_winner") {
                                    AnimatedHomeSection {
                                        Column {
                                            HomeSectionTitle(
                                                title = stringResource(R.string.box_office_weekend_badge),
                                                onClick = onBoxOfficeClick
                                            )
                                            HomeBoxOfficeWinnerCard(
                                                item = winner,
                                                onMovieClick = { onMovieClick(winner.movie) },
                                                onResolveLogo = { movie -> viewModel.resolveMovieLogo(movie) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        com.cinetrack.data.model.HomeFeedSectionConstants.CONTINUE_WATCHING -> {
                            // CONTINUA A GUARDARE (TV)
                            if (uiState.preferences.showHomeContinueWatching && isTv && uiState.continueWatchingTv.isNotEmpty()) {
                                item(key = "section_continue_watching") {
                                    AnimatedHomeSection {
                                        Column {
                                            HomeSectionTitle(title = stringResource(R.string.home_section_continue_watching), onClick = null)
                                            
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                items(
                                                    items = uiState.continueWatchingTv,
                                                    key = { movie -> movie.id }
                                                ) { rawMovie ->
                                                    val movie = resolveLocalMovie(rawMovie)
                                                    val isUpdating = viewModel.updatingShowIds[movie.id] == true
                                                    Box(modifier = Modifier
                                                        .width(110.dp)
                                                        .animateItem(
                                                            fadeInSpec = tween(220),
                                                            fadeOutSpec = tween(280),
                                                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                                                        )
                                                    ) {
                                                        com.cinetrack.ui.components.card.ContinueWatchingSeriesCard(
                                                            movie = movie,
                                                            cardWidth = 110.dp,
                                                            isUpdating = isUpdating,
                                                            isFavorite = isMovieFavorite(movie),
                                                            isWatched = movie.watched,
                                                            isReminder = movie.reminder,
                                                            progress = getMovieProgress(movie),
                                                            personalRating = movie.personalRating,
                                                            folderColors = getMovieFolderColors(movie),
                                                            hazeState = activeHazeState,
                                                            staggerIndex = -1,
                                                            onPress = onMovieClick,
                                                            onLongPress = stableOnLongPress,
                                                            onAction = stableOnAction,
                                                            onQuickMarkWatched = { m -> viewModel.markNextEpisodeWatched(m) },
                                                            onMessage = stableOnMessage
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        com.cinetrack.data.model.HomeFeedSectionConstants.WATCHLIST -> {
                            // 1. DALLA TUA WATCHLIST
                            if (uiState.preferences.showHomeWatchlist && watchlistList.isNotEmpty()) {
                                item(key = "section_watchlist_${if (isTv) "tv" else "movie"}") {
                                    HomePosterRow(
                                        title = stringResource(R.string.home_section_watchlist),
                                        movies = watchlistList,
                                        onTitleClick = { tabNavigator.current = HomeTab },
                                        onShowMoreClick = if (watchlistList.size > 10) { { tabNavigator.current = HomeTab } } else null,
                                        maxDisplayCount = 10,
                                        resolveLocalMovie = resolveLocalMovie,
                                        isMovieFavorite = isMovieFavorite,
                                        isMovieWatched = isMovieWatched,
                                        isMovieReminder = isMovieReminder,
                                        getMovieProgress = getMovieProgress,
                                        getMovieFolderColors = getMovieFolderColors,
                                        activeHazeState = activeHazeState,
                                        onMovieClick = onMovieClick,
                                        onLongPress = stableOnLongPress,
                                        onAction = stableOnAction,
                                        onMessage = stableOnMessage,
                                        keyPrefix = "wl_${if (isTv) "tv" else "movie"}"
                                    )
                                }
                            }
                        }

                        com.cinetrack.data.model.HomeFeedSectionConstants.TROVE_PICK -> {
                            // 4. THE TROVE'S PICK + CONSIGLIATI (se disponibili)
                            if (recommendedList.isNotEmpty()) {
                                val trovePick = recommendedList.first()
                                item(key = "section_trove_pick_${if (isTv) "tv" else "movie"}") {
                                    AnimatedHomeSection {
                                        TrovePickCard(
                                            movie           = trovePick,
                                            isRevealedToday = troveRevealedToday,
                                            onReveal        = { id -> viewModel.markTroveRevealed(id) },
                                            onMovieClick    = onMovieClick,
                                            onResetReveal   = { viewModel.resetTroveRevealed() },
                                            onResolveLogo   = { movie -> viewModel.resolveMovieLogo(movie) }
                                        )
                                    }
                                }
                                // Se ci sono altri consigliati, mostra una riga con il resto
                                if (recommendedList.size > 1) {
                                    item(key = "section_trove_pick_rest_${if (isTv) "tv" else "movie"}") {
                                        val restRecommended = remember(recommendedList) { recommendedList.drop(1) }
                                        HomePosterRow(
                                            title = stringResource(R.string.home_section_recommended),
                                            movies = restRecommended,
                                            onTitleClick = {
                                                RecommendationsTab.requestedMediaType = if (isTv) "tv" else "movie"
                                                tabNavigator.current = RecommendationsTab
                                            },
                                            onShowMoreClick = {
                                                RecommendationsTab.requestedMediaType = if (isTv) "tv" else "movie"
                                                tabNavigator.current = RecommendationsTab
                                            },
                                            resolveLocalMovie = resolveLocalMovie,
                                            isMovieFavorite = isMovieFavorite,
                                            isMovieWatched = isMovieWatched,
                                            isMovieReminder = isMovieReminder,
                                            getMovieProgress = getMovieProgress,
                                            getMovieFolderColors = getMovieFolderColors,
                                            activeHazeState = activeHazeState,
                                            onMovieClick = onMovieClick,
                                            onLongPress = stableOnLongPress,
                                            onAction = stableOnAction,
                                            onMessage = stableOnMessage,
                                            keyPrefix = "rec_${if (isTv) "tv" else "movie"}"
                                        )
                                    }
                                }
                            }
                        }

                        com.cinetrack.data.model.HomeFeedSectionConstants.BECAUSE_YOU_WATCHED -> {
                            // PERCHÉ HAI GUARDATO [TITOLO]
                            val becauseYouWatchedData = if (isTv) uiState.becauseYouWatchedTv else uiState.becauseYouWatchedMovie
                            if (uiState.preferences.showHomeBecauseYouWatched && becauseYouWatchedData != null && becauseYouWatchedData.second.isNotEmpty()) {
                                val seedMovie = becauseYouWatchedData.first
                                val recs = becauseYouWatchedData.second
                                item(key = "section_because_you_watched_${if (isTv) "tv" else "movie"}") {
                                    HomePosterRow(
                                        title = stringResource(R.string.home_section_because_you_watched, seedMovie.displayName),
                                        movies = recs,
                                        resolveLocalMovie = resolveLocalMovie,
                                        isMovieFavorite = isMovieFavorite,
                                        isMovieWatched = isMovieWatched,
                                        isMovieReminder = isMovieReminder,
                                        getMovieProgress = getMovieProgress,
                                        getMovieFolderColors = getMovieFolderColors,
                                        activeHazeState = activeHazeState,
                                        onMovieClick = onMovieClick,
                                        onLongPress = stableOnLongPress,
                                        onAction = stableOnAction,
                                        onMessage = stableOnMessage,
                                        keyPrefix = "byw_${if (isTv) "tv" else "movie"}"
                                    )
                                }
                            }
                        }

                        com.cinetrack.data.model.HomeFeedSectionConstants.TOP_10 -> {
                            // 6. TOP 10 FLICKTROVE
                            if (top10List.isNotEmpty()) {
                                item(key = "section_top_10_${if (isTv) "tv" else "movie"}") {
                                    AnimatedHomeSection {
                                        Column {
                                            HomeSectionTitle(stringResource(R.string.home_section_top_10))
                                            
                                            val top10Limited = remember(top10List) { top10List.take(10) }
                                            val top10RowState = rememberSaveable(if (isTv) "tv" else "movie", saver = LazyListState.Saver) {
                                                LazyListState()
                                            }
                                            LazyRow(
                                                state = top10RowState,
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                items(
                                                    count = top10Limited.size,
                                                    key = { index -> "top_${top10Limited[index].mediaType}_${top10Limited[index].id}" }
                                                ) { index ->
                                                    val rawMovie = top10Limited[index]
                                                    val movie = resolveLocalMovie(rawMovie)
                                                    Top10MovieCard(
                                                        movie = movie,
                                                        rank = index + 1,
                                                        isFavorite = isMovieFavorite(movie),
                                                        isWatched = isMovieWatched(movie),
                                                        isReminder = isMovieReminder(movie),
                                                        progress = getMovieProgress(movie),
                                                        personalRating = movie.personalRating,
                                                        folderColors = getMovieFolderColors(movie),
                                                        hazeState = activeHazeState,
                                                        staggerIndex = index,
                                                        onPress = onMovieClick,
                                                        onLongPress = stableOnLongPress,
                                                        onAction = stableOnAction,
                                                        onMessage = stableOnMessage
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        com.cinetrack.data.model.HomeFeedSectionConstants.POPULAR -> {
                            // 2. POPOLARI (2:3 Posters)
                            if (popularList.isNotEmpty()) {
                                item(key = "section_popular_${if (isTv) "tv" else "movie"}") {
                                    HomePosterRow(
                                        title = stringResource(R.string.home_section_popular),
                                        movies = popularList,
                                        onTitleClick = { 
                                            DiscoverTab.requestedType = if (isTv) "popular_tv" else "popular_movies"
                                            tabNavigator.current = DiscoverTab 
                                        },
                                        onShowMoreClick = { 
                                            DiscoverTab.requestedType = if (isTv) "popular_tv" else "popular_movies"
                                            tabNavigator.current = DiscoverTab 
                                        },
                                        resolveLocalMovie = resolveLocalMovie,
                                        isMovieFavorite = isMovieFavorite,
                                        isMovieWatched = isMovieWatched,
                                        isMovieReminder = isMovieReminder,
                                        getMovieProgress = getMovieProgress,
                                        getMovieFolderColors = getMovieFolderColors,
                                        activeHazeState = activeHazeState,
                                        onMovieClick = onMovieClick,
                                        onLongPress = stableOnLongPress,
                                        onAction = stableOnAction,
                                        onMessage = stableOnMessage,
                                        keyPrefix = "pop_${if (isTv) "tv" else "movie"}"
                                    )
                                }
                            }
                        }

                        com.cinetrack.data.model.HomeFeedSectionConstants.NOW_PLAYING -> {
                            // 2. NOW IN THEATERS / NOW STREAMING (2:3 Posters)
                            if (nowPlayingList.isNotEmpty()) {
                                item(key = "section_now_playing_${if (isTv) "tv" else "movie"}") {
                                    HomePosterRow(
                                        title = stringResource(if (isTv) R.string.home_section_now_streaming else R.string.home_section_now_in_theaters),
                                        movies = nowPlayingList,
                                        onTitleClick = { 
                                            DiscoverTab.requestedType = if (isTv) "on_the_air_tv" else "now_playing_movies"
                                            tabNavigator.current = DiscoverTab 
                                        },
                                        onShowMoreClick = { 
                                            DiscoverTab.requestedType = if (isTv) "on_the_air_tv" else "now_playing_movies"
                                            tabNavigator.current = DiscoverTab 
                                        },
                                        resolveLocalMovie = resolveLocalMovie,
                                        isMovieFavorite = isMovieFavorite,
                                        isMovieWatched = isMovieWatched,
                                        isMovieReminder = isMovieReminder,
                                        getMovieProgress = getMovieProgress,
                                        getMovieFolderColors = getMovieFolderColors,
                                        activeHazeState = activeHazeState,
                                        onMovieClick = onMovieClick,
                                        onLongPress = stableOnLongPress,
                                        onAction = stableOnAction,
                                        onMessage = stableOnMessage,
                                        keyPrefix = "now_${if (isTv) "tv" else "movie"}"
                                    )
                                }
                            }
                        }

                        com.cinetrack.data.model.HomeFeedSectionConstants.UPCOMING -> {
                            // 3. IN USCITA (2:3 Posters)
                            if (upcomingList.isNotEmpty()) {
                                item(key = "section_upcoming_${if (isTv) "tv" else "movie"}") {
                                    HomePosterRow(
                                        title = stringResource(R.string.home_section_upcoming),
                                        movies = upcomingList,
                                        onTitleClick = { 
                                            DiscoverTab.requestedType = if (isTv) "upcoming_tv" else "upcoming_movies"
                                            tabNavigator.current = DiscoverTab 
                                        },
                                        onShowMoreClick = { 
                                            DiscoverTab.requestedType = if (isTv) "upcoming_tv" else "upcoming_movies"
                                            tabNavigator.current = DiscoverTab 
                                        },
                                        resolveLocalMovie = resolveLocalMovie,
                                        isMovieFavorite = isMovieFavorite,
                                        isMovieWatched = isMovieWatched,
                                        isMovieReminder = isMovieReminder,
                                        getMovieProgress = getMovieProgress,
                                        getMovieFolderColors = getMovieFolderColors,
                                        activeHazeState = activeHazeState,
                                        onMovieClick = onMovieClick,
                                        onLongPress = stableOnLongPress,
                                        onAction = stableOnAction,
                                        onMessage = stableOnMessage,
                                        keyPrefix = "upc_${if (isTv) "tv" else "movie"}"
                                    )
                                }
                            }
                        }

                        com.cinetrack.data.model.HomeFeedSectionConstants.NEWS -> {
                            // 5. MAGAZINE NEWS
                            if (uiState.magazineNews.isNotEmpty()) {
                                item(key = "section_news") {
                                    AnimatedHomeSection {
                                        Column {
                                            HomeSectionTitle(title = stringResource(R.string.home_section_magazine), onClick = { tabNavigator.current = NewsTab })
                                            
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                items(
                                                    count = uiState.magazineNews.size,
                                                    key = { index -> "news_${uiState.magazineNews[index].link.hashCode()}" }
                                                ) { index ->
                                                    val article = uiState.magazineNews[index]
                                                    NewsArticleCard(article = article, context = context)
                                                }
                                                item {
                                                    ShowMoreCard(onClick = { tabNavigator.current = NewsTab }, height = 140.dp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // CARD DI SUPPORTO — in fondo al feed, riappare ogni 30 giorni dopo il dismiss
                item(key = "section_support_card") {
                    AnimatedHomeSection {
                        HomeSupportCard()
                    }
                }

            }
        }
    }

        // Perfectly Centered Floating Sticky Header
        HomeFloatingHeader(
            activeTab = uiState.activeTab,
            topPadding = topPadding,
            stickyHeaderHeight = stickyHeaderHeight,
            activeHazeState = activeHazeState,
            onTabChanged = { tab -> viewModel.onTabChanged(tab) },
            onSurpriseMeClick = { rect -> surpriseMeRequest?.invoke(rect) }
        )
    }
}



