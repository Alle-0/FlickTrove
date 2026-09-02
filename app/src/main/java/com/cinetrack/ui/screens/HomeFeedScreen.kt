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
import com.cinetrack.ui.components.common.CategoryTabSelector
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
                navigator.push(MovieDetailScreen(movie.id, finalType))
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
            .filter { it.reminder && !it.watched && it.mediaType == targetType && it.isReleased }
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }
    }
    
    // Virtual infinite pager state
    val initialPage = if (heroList.isNotEmpty()) {
        5000 - (5000 % heroList.size)
    } else 0
    val pagerState = rememberPagerState(initialPage = initialPage) { 10000 }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        CinematicBackground(modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier            
    .fillMaxSize()
                .haze(activeHazeState, style = HazeStyles.PremiumDark)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    bottom = paddingValues.calculateBottomPadding() + 80.dp, // Spazio extra richiesto
                    top = 165.dp
                ),
                verticalArrangement = Arrangement.spacedBy(48.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.isLoading) {
                    item {
                        // Hero Skeleton
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(500.dp)
                                .clip(RoundedCornerShape(24.dp))
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
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A1A2E))
                                    .shimmerEffect()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                userScrollEnabled = false
                            ) {
                                items(4) {
                                    MovieCardSkeleton(width = 120.dp)
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
                
                // 1.5 DALLA TUA WATCHLIST
                if (watchlistList.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = stringResource(R.string.home_section_watchlist))
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(watchlistList.size) { index ->
                                val movie = watchlistList[index]
                                Box(modifier = Modifier.width(120.dp)) {
                                    MovieCard(
                                        movie = movie,
                                        cardWidth = 120.dp,
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
                            tabNavigator.current = VistiTab 
                        })
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(watchlistList.take(20).size) { index ->
                                val movie = watchlistList[index]
                                Box(modifier = Modifier.width(120.dp)) {
                                    MovieCard(
                                        movie = movie,
                                        cardWidth = 120.dp,
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
                            if (watchlistList.size > 20) {
                                item {
                                    ShowMoreCard(onClick = { 
                                        tabNavigator.current = VistiTab 
                                    })
                                }
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
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(popularList.size) { index ->
                                val movie = popularList[index]
                                Box(modifier = Modifier.width(120.dp)) {
                                    MovieCard(
                                        movie = movie,
                                        cardWidth = 120.dp,
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
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(nowPlayingList.size) { index ->
                                val movie = nowPlayingList[index]
                                Box(modifier = Modifier.width(120.dp)) {
                                    MovieCard(
                                        movie = movie,
                                        cardWidth = 120.dp,
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
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(upcomingList.size) { index ->
                                val movie = upcomingList[index]
                                Box(modifier = Modifier.width(120.dp)) {
                                    MovieCard(
                                        movie = movie,
                                        cardWidth = 120.dp,
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
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(recommendedList.drop(1).size) { index ->
                                    val movie = recommendedList.drop(1)[index]
                                    Box(modifier = Modifier.width(120.dp)) {
                                        MovieCard(
                                            movie = movie,
                                            cardWidth = 120.dp,
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

                // 5. MAGAZINE NEWS
                if (uiState.magazineNews.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = stringResource(R.string.home_section_magazine), onClick = { tabNavigator.current = NewsTab })
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
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

                // 6. TOP 10 FLICKTROVE
                if (top10List.isNotEmpty()) {
                    item {
                        HomeSectionTitle(stringResource(R.string.home_section_top_10))
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
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

@Composable
fun HomeSectionTitle(title: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.let {
                if (onClick != null) it.bounceClick { onClick() } else it
            }
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (onClick != null) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_right),
                    contentDescription = stringResource(R.string.home_section_see_all),
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ShowMoreCard(onClick: () -> Unit, height: androidx.compose.ui.unit.Dp = 180.dp) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(height)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_right),
                contentDescription = stringResource(R.string.overview_show_more),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.overview_show_more),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BackdropMovieCard(movie: Movie, onPress: (Movie) -> Unit) {
    val backdropUrl = buildTmdbImageUrl(movie.backdropPath, ImageType.BACKDROP, LocalImageQuality.current)
    Box(
        modifier = Modifier
            .width(280.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.DarkGray)
            .bounceClick { onPress(movie) }
    ) {
        AsyncImage(
            model = backdropUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 100f
                    )
                )
        )
        Text(
            text = movie.title ?: "",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )
    }
}

@Composable
fun NewsArticleCard(article: NewsItem, context: android.content.Context) {
    // Estrai la fonte dall'URL (es. "screenrant.com" -> "ScreenRant")
    val source = remember(article.link) {
        runCatching {
            android.net.Uri.parse(article.link).host
                ?.removePrefix("www.")
                ?.split(".")
                ?.firstOrNull()
                ?.replaceFirstChar { it.uppercase() }
                ?: ""
        }.getOrDefault("")
    }

    Box(
        modifier = Modifier
            .width(220.dp)
            .height(140.dp)
            .bounceClick {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(article.link))
                context.startActivity(intent)
            }
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    ) {
        // Immagine di sfondo
        if (article.imageUrl != null) {
            AsyncImage(
                model = article.imageUrl,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
            )
        } else {
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.fillMaxSize().background(Color.DarkGray)
            )
        }

        // Gradient in basso
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                    )
                )
        )

        // Testo in basso
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
        ) {
            if (source.isNotBlank()) {
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            Text(
                text = article.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                ),
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Badge "External link" in alto a destra
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Link esterno",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_external_link),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}


@Composable
fun Top10MovieCard(
    movie: Movie, 
    rank: Int, 
    hazeState: HazeState, 
    staggerIndex: Int = 0,
    isFavorite: Boolean = movie.favorite,
    isWatched: Boolean = movie.watched,
    folderColors: List<androidx.compose.ui.graphics.Color> = emptyList(),
    onPress: (Movie) -> Unit, 
    onLongPress: (Movie, androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit = { _, _, _ -> },
    onAction: (Movie) -> Unit = {},
    onMessage: (String) -> Unit = {}
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .width(156.dp)
            .height(210.dp)
    ) {
        val bgColor = MaterialTheme.colorScheme.background
        
        // Large outlined rank number — stroke only, accent color, bleeds off left/bottom edge
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(width = 100.dp, height = 110.dp)
                .offset(x = (-8).dp, y = 22.dp)
        ) {
            val outlinePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 148.sp.toPx()
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT_BOLD,
                    android.graphics.Typeface.BOLD
                )
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 7f // Double thickness because fill covers inner half
                strokeJoin = android.graphics.Paint.Join.ROUND
                strokeCap = android.graphics.Paint.Cap.ROUND
                color = accentColor.copy(alpha = 0.78f).toArgb()
            }
            val fillPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 148.sp.toPx()
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT_BOLD,
                    android.graphics.Typeface.BOLD
                )
                style = android.graphics.Paint.Style.FILL
                color = bgColor.toArgb()
            }
            drawContext.canvas.nativeCanvas.drawText(
                rank.toString(),
                0f,
                size.height * 0.85f,
                outlinePaint
            )
            drawContext.canvas.nativeCanvas.drawText(
                rank.toString(),
                0f,
                size.height * 0.85f,
                fillPaint
            )
        }

        // Poster card shifted right to reveal number on the left
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(start = 30.dp)
                .width(126.dp)
        ) {
            MovieCard(
                movie = movie,
                cardWidth = 126.dp,
                isFavorite = isFavorite,
                isWatched = isWatched,
                folderColors = folderColors,
                hazeState = hazeState,
                staggerIndex = staggerIndex,
                onPress = onPress,
                onLongPress = onLongPress,
                onAction = onAction,
                onMessage = onMessage
            )
        }
    }
}

