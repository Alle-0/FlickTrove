package com.cinetrack.ui.screens

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.rounded.Warning
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
import com.cinetrack.ui.components.home.HeroSpotlightCarousel
import com.cinetrack.ui.components.home.TrovePickCard
import com.cinetrack.ui.components.home.HomeSectionTitle
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.home.HeroSpotlightCarousel
import com.cinetrack.ui.components.home.TrovePickCard
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.components.shared.MovieCardSkeleton
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.utils.bounceClick
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
    onMovieClick: (Movie) -> Unit = {}
) {
    val tabNavigator = LocalTabNavigator.current    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    val stableOnLongPress: (Movie, androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit = remember(movieActions) {
        { m, offset, pos -> movieActions.openActionsPopup(m, offset, pos) }
    }

    val stableOnAction: (Movie) -> Unit = remember(viewModel) { { m -> viewModel.toggleWatched(m) } }
    val stableOnMessage: (String) -> Unit = remember(viewModel) {
        { msg -> viewModel.emitMessage(com.cinetrack.ui.utils.UiText.DynamicString(msg)) }
    }

    val getMovieFolderColors = remember(uiState.movieFolderColors) {
        { movie: Movie ->
            val compositeId = "${if(movie.mediaType.isNotEmpty()) movie.mediaType else uiState.activeTab}_${movie.id}"
            uiState.movieFolderColors[compositeId]?.map { it.toComposeColor() } ?: emptyList()
        }
    }

    val isMovieFavorite = remember(uiState.allLocalMovies) {
        { movie: Movie ->
            movie.favorite || uiState.allLocalMovies.find { it.id == movie.id && it.mediaType == movie.mediaType }?.favorite == true
        }
    }

    val isMovieWatched = remember(uiState.allLocalMovies) {
        { movie: Movie ->
            movie.watched || uiState.allLocalMovies.find { it.id == movie.id && it.mediaType == movie.mediaType }?.watched == true
        }
    }

    val isMovieReminder = remember(uiState.allLocalMovies) {
        { movie: Movie ->
            movie.reminder || uiState.allLocalMovies.find { it.id == movie.id && it.mediaType == movie.mediaType }?.reminder == true
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
        Box(
            modifier = Modifier            
    .fillMaxSize()
                .haze(activeHazeState, style = HazeStyles.PremiumDark)
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
                if (uiState.hasFeedError) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_error),
                                    contentDescription = stringResource(R.string.content_description_error),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = stringResource(R.string.error_feed_title),
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.error_feed_subtitle),
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = { viewModel.retryFeed() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.error_state_retry), color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (uiState.isFeedLoading) {
                    item {
                        // Hero Skeleton
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                                .height(500.dp)
                                .clip(RoundedCornerShape(36.dp))
                                .background(Color(0xFF1A1A2E))
                                .shimmerEffect()
                        )
                    }
                    
                    item {
                        // Standard Row Skeleton
                        Column {
                            Box(
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp)
                                    .width(160.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A1A2E))
                                    .shimmerEffect()
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                userScrollEnabled = false
                            ) {
                                items(4) {
                                    MovieCardSkeleton(width = 110.dp)
                                }
                            }
                        }
                    }

                    item {
                        // Trove Pick Card Skeleton
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(vertical = 24.dp)
                                .height(260.dp)
                                .clip(RoundedCornerShape(36.dp))
                                .background(Color(0xFF1A1A2E))
                                .shimmerEffect()
                        )
                    }

                    items(2) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp)
                                    .width(140.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A1A2E))
                                    .shimmerEffect()
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                userScrollEnabled = false
                            ) {
                                items(4) {
                                    MovieCardSkeleton(width = 110.dp)
                                }
                            }
                        }
                    }
                }

                // HERO CAROUSEL
                if (heroList.isNotEmpty()) {
                    item {
                        HeroSpotlightCarousel(
                            movies = heroList,
                            pagerState = pagerState,
                            onMovieClick = onMovieClick
                        )
                    }
                }

                // CONTINUA A GUARDARE (TV)
                if (isTv && uiState.continueWatchingTv.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = stringResource(R.string.home_section_continue_watching), onClick = null)
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.continueWatchingTv.size) { index ->
                                val movie = uiState.continueWatchingTv[index]
                                Box(modifier = Modifier.width(110.dp)) {
                                    MovieCard(
                                        movie = movie,
                                        cardWidth = 110.dp,
                                        isFavorite = isMovieFavorite(movie),
                                        isWatched = isMovieWatched(movie),
                                        isReminder = isMovieReminder(movie),
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
                
                // 1. DALLA TUA WATCHLIST
                if (watchlistList.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = stringResource(R.string.home_section_watchlist), onClick = { 
                            tabNavigator.current = HomeTab 
                        })
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(watchlistList.take(10).size) { index ->
                                val movie = watchlistList[index]
                                Box(modifier = Modifier.width(110.dp)) {
                                    MovieCard(
                                        movie = movie,
                                        cardWidth = 110.dp,
                                        isFavorite = isMovieFavorite(movie),
                                        isWatched = isMovieWatched(movie),
                                        isReminder = isMovieReminder(movie),
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
                            if (watchlistList.size > 10) {
                                item {
                                    ShowMoreCard(onClick = { 
                                        tabNavigator.current = HomeTab 
                                    })
                                }
                            }
                        }
                    }
                }
                
                // 4. THE TROVE'S PICK + CONSIGLIATI (se disponibili)
                if (recommendedList.isNotEmpty()) {
                    val trovePick = recommendedList.first()
                    item {
                        TrovePickCard(
                            movie = trovePick,
                            onMovieClick = onMovieClick
                        )
                    }
                    // Se ci sono altri consigliati, mostra una riga con il resto
                    if (recommendedList.size > 1) {
                        item {
                            HomeSectionTitle(
                                title = stringResource(R.string.home_section_recommended),
                                onClick = { tabNavigator.current = RecommendationsTab }
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(recommendedList.drop(1).size) { index ->
                                    val movie = recommendedList.drop(1)[index]
                                    Box(modifier = Modifier.width(110.dp)) {
                                        MovieCard(
                                            movie = movie,
                                            cardWidth = 110.dp,
                                            isFavorite = isMovieFavorite(movie),
                                            isWatched = isMovieWatched(movie),
                                            isReminder = isMovieReminder(movie),
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
                                item {
                                    ShowMoreCard(onClick = { tabNavigator.current = RecommendationsTab })
                                }
                            }
                        }
                    }
                }

                // PERCHÉ HAI GUARDATO [TITOLO]
                val becauseYouWatchedData = if (isTv) uiState.becauseYouWatchedTv else uiState.becauseYouWatchedMovie
                if (becauseYouWatchedData != null && becauseYouWatchedData.second.isNotEmpty()) {
                    val seedMovie = becauseYouWatchedData.first
                    val recs = becauseYouWatchedData.second
                    item {
                        HomeSectionTitle(title = stringResource(R.string.home_section_because_you_watched, seedMovie.displayName ?: seedMovie.name ?: ""), onClick = null)
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recs.size) { index ->
                                val movie = recs[index]
                                Box(modifier = Modifier.width(110.dp)) {
                                    MovieCard(
                                        movie = movie,
                                        cardWidth = 110.dp,
                                        isFavorite = isMovieFavorite(movie),
                                        isWatched = isMovieWatched(movie),
                                        isReminder = isMovieReminder(movie),
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
                
                // 6. TOP 10 FLICKTROVE
                if (top10List.isNotEmpty()) {
                    item {
                        HomeSectionTitle(stringResource(R.string.home_section_top_10))
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(top10List.take(10).size) { index ->
                                val movie = top10List[index]
                                Top10MovieCard(
                                    movie = movie,
                                    rank = index + 1,
                                    isFavorite = isMovieFavorite(movie),
                                    isWatched = isMovieWatched(movie),
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

                // 2. POPOLARI (2:3 Posters)
                if (popularList.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = stringResource(R.string.home_section_popular), onClick = { 
                            DiscoverTab.requestedType = if (isTv) "popular_tv" else "popular_movies"
                            tabNavigator.current = DiscoverTab 
                        })
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(popularList.size) { index ->
                                val movie = popularList[index]
                                Box(modifier = Modifier.width(110.dp)) {
                                    MovieCard(
                                        movie = movie,
                                        cardWidth = 110.dp,
                                        isFavorite = isMovieFavorite(movie),
                                        isWatched = isMovieWatched(movie),
                                        isReminder = isMovieReminder(movie),
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
                            item {
                                ShowMoreCard(onClick = { 
                                    DiscoverTab.requestedType = if (isTv) "popular_tv" else "popular_movies"
                                    tabNavigator.current = DiscoverTab 
                                })
                            }
                        }
                    }
                }

                // 2. NOW IN THEATERS / NOW STREAMING (2:3 Posters)
                if (nowPlayingList.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = stringResource(if (isTv) R.string.home_section_now_streaming else R.string.home_section_now_in_theaters), onClick = { 
                            DiscoverTab.requestedType = if (isTv) "on_the_air_tv" else "now_playing_movies"
                            tabNavigator.current = DiscoverTab 
                        })
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(nowPlayingList.size) { index ->
                                val movie = nowPlayingList[index]
                                Box(modifier = Modifier.width(110.dp)) {
                                    MovieCard(
                                        movie = movie,
                                        cardWidth = 110.dp,
                                        isFavorite = isMovieFavorite(movie),
                                        isWatched = isMovieWatched(movie),
                                        isReminder = isMovieReminder(movie),
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
                            item {
                                ShowMoreCard(onClick = { 
                                    DiscoverTab.requestedType = if (isTv) "on_the_air_tv" else "now_playing_movies"
                                    tabNavigator.current = DiscoverTab 
                                })
                            }
                        }
                    }
                }

                // 3. IN USCITA (2:3 Posters)
                if (upcomingList.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = stringResource(R.string.home_section_upcoming), onClick = { 
                            DiscoverTab.requestedType = if (isTv) "upcoming_tv" else "upcoming_movies"
                            tabNavigator.current = DiscoverTab 
                        })
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(upcomingList.size) { index ->
                                val movie = upcomingList[index]
                                Box(modifier = Modifier.width(110.dp)) {
                                    MovieCard(
                                        movie = movie,
                                        cardWidth = 110.dp,
                                        isFavorite = isMovieFavorite(movie),
                                        isWatched = isMovieWatched(movie),
                                        isReminder = isMovieReminder(movie),
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
                            item {
                                ShowMoreCard(onClick = { 
                                    DiscoverTab.requestedType = if (isTv) "upcoming_tv" else "upcoming_movies"
                                    tabNavigator.current = DiscoverTab 
                                })
                            }
                        }
                    }
                }

                // 5. MAGAZINE NEWS
                if (uiState.magazineNews.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = stringResource(R.string.home_section_magazine), onClick = { tabNavigator.current = NewsTab })
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.magazineNews.size) { index ->
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

        // Perfectly Centered Floating Sticky Header
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
                // Filter bounds setup
                val rightControlsInset = 44.dp

                // Category Tab Selector Island
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(end = rightControlsInset),
                    contentAlignment = Alignment.Center
                ) {
                    Spacer(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeGlass(state = activeHazeState, shape = CircleShape, blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = false)
                    )
                    
                    val options = listOf("Movies", "TV Series")
                    val selectedIndex = if (uiState.activeTab == "movie") 0 else 1
                    
                    CategoryTabSelector(
                        options = options,
                        selectedIndex = selectedIndex,
                        onOptionClick = { index ->
                            viewModel.onTabChanged(if (index == 0) "movie" else "tv")
                        }
                    )
                }

                // Circular Surprise Me Button (Right Aligned)
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeGlass(
                                    state = activeHazeState, 
                                    shape = CircleShape, 
                                    blurRadius = HazeStyles.SmallGlassBlurRadius, 
                                    useOffscreenStrategy = false,
                                    borderWidth = 1.dp,
                                    borderColor = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop)
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .bounceClick(scaleDown = 0.92f) { 
                                    surpriseMeRequest?.invoke()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_bacchetta),
                                contentDescription = "Surprise Me",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

