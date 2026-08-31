package com.cinetrack.ui.screens

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.HomeViewModel
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

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
                navigator.push(MovieDetailScreen(movie.id, movie.mediaType))
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

    val navigator = LocalNavigator.currentOrThrow.parent ?: LocalNavigator.currentOrThrow
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        CinematicBackground(modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier            
    .fillMaxSize()
                .haze(activeHazeState, style = HazeStyles.PremiumDark)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    bottom = paddingValues.calculateBottomPadding() + 24.dp,
                    top = topPadding + stickyHeaderHeight + 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. POPOLARI (2:3 Posters)
                if (popularList.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = "Popolari", onClick = { navigator.push(DiscoverTab) })
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
                                        isFavorite = movie.favorite,
                                        isWatched = movie.watched,
                                        hazeState = activeHazeState,
                                        onPress = onMovieClick
                                    )
                                }
                            }
                            item {
                                ShowMoreCard(onClick = { navigator.push(DiscoverTab) })
                            }
                        }
                    }
                }

                // 2. NOW IN THEATERS / NOW STREAMING (2:3 Posters)
                if (nowPlayingList.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = if (isTv) "Adesso in streaming" else "Adesso al cinema", onClick = { navigator.push(DiscoverTab) })
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
                                        isFavorite = movie.favorite,
                                        isWatched = movie.watched,
                                        hazeState = activeHazeState,
                                        onPress = onMovieClick
                                    )
                                }
                            }
                            item {
                                ShowMoreCard(onClick = { navigator.push(DiscoverTab) })
                            }
                        }
                    }
                }

                // 3. IN USCITA (2:3 Posters)
                if (upcomingList.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = "In uscita", onClick = { navigator.push(DiscoverTab) })
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
                                        isFavorite = movie.favorite,
                                        isWatched = movie.watched,
                                        hazeState = activeHazeState,
                                        onPress = onMovieClick
                                    )
                                }
                            }
                            item {
                                ShowMoreCard(onClick = { navigator.push(DiscoverTab) })
                            }
                        }
                    }
                }

                // 4. CONSIGLIATI PER TE (2:3 Posters)
                if (recommendedList.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = "Consigliati per te", onClick = { navigator.push(RecommendationsTab) })
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recommendedList.size) { index ->
                                val movie = recommendedList[index]
                                Box(modifier = Modifier.width(120.dp)) {
                                    MovieCard(
                                        movie = movie,
                                        cardWidth = 120.dp,
                                        isFavorite = movie.favorite,
                                        isWatched = movie.watched,
                                        hazeState = activeHazeState,
                                        onPress = onMovieClick
                                    )
                                }
                            }
                            item {
                                ShowMoreCard(onClick = { navigator.push(RecommendationsTab) })
                            }
                        }
                    }
                }

                // 5. MAGAZINE NEWS
                if (uiState.magazineNews.isNotEmpty()) {
                    item {
                        HomeSectionTitle(title = "Magazine News", onClick = { navigator.push(NewsTab) })
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.magazineNews.size) { index ->
                                val article = uiState.magazineNews[index]
                                NewsArticleCard(article = article, context = context)
                            }
                            item {
                                ShowMoreCard(onClick = { navigator.push(NewsTab) })
                            }
                        }
                    }
                }

                // 6. TOP 10 FLICKTROVE
                if (top10List.isNotEmpty()) {
                    item {
                        HomeSectionTitle("Top 10 FlickTrove")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(top10List.take(10).size) { index ->
                                val movie = top10List[index]
                                Top10MovieCard(
                                    movie = movie,
                                    rank = index + 1,
                                    hazeState = activeHazeState,
                                    onPress = onMovieClick
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
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .let { 
                if (onClick != null) {
                    it.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    ) 
                } else it 
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        if (onClick != null) {
            Icon(
                imageVector = androidx.compose.material.icons.filled.ArrowForward,
                contentDescription = "Vedi tutto",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ShowMoreCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(180.dp) // height of MovieCard (typically poster ratio 120 width -> 180 height)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = androidx.compose.material.icons.filled.ArrowForward,
                contentDescription = "Mostra altro",
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mostra altro",
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
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.DarkGray)
            .bounceClick {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(article.link))
                context.startActivity(intent)
            }
    ) {
        article.imageUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.5f)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = article.pubDate.take(16), // simple format
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun Top10MovieCard(movie: Movie, rank: Int, hazeState: HazeState, onPress: (Movie) -> Unit) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
    ) {
        // Number behind/left of poster
        Text(
            text = rank.toString(),
            fontSize = 120.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-20).dp, y = 20.dp)
        )
        
        // Poster
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(start = 24.dp)
                .width(116.dp)
        ) {
            MovieCard(
                movie = movie,
                cardWidth = 116.dp,
                isFavorite = movie.favorite,
                isWatched = movie.watched,
                hazeState = hazeState,
                onPress = onPress
            )
        }
    }
}
