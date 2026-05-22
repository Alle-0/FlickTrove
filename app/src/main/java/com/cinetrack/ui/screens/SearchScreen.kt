package com.cinetrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.material.icons.rounded.WifiOff
import com.cinetrack.ui.assets.CustomIcons
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.BorderStroke

import com.cinetrack.ui.components.HomeFilterModal
import com.cinetrack.ui.theme.PremiumBackground
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.MovieCardSkeleton

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.data.Movie
import com.cinetrack.data.api.PersonSearchResult
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.ui.components.CinematicBackground
import com.cinetrack.ui.components.MovieCard
import com.cinetrack.ui.components.PersonCard
import com.cinetrack.util.toComposeColor
import com.cinetrack.ui.viewmodel.SearchViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.cinetrack.ui.theme.HazeStyles
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    startX: Float? = null,
    startY: Float? = null,
    initialGenreName: String? = null,
    isFilterVisible: Boolean = false,
    isDetailVisible: Boolean = false,
    onBack: () -> Unit,
    onClosing: () -> Unit = {},
    onToggleFilter: (Boolean, Rect?) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onPersonClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isMeasured by remember { mutableStateOf(false) }
    val revealAmount = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }

    val internalHazeState = hazeState ?: remember { HazeState() }


    LaunchedEffect(isMeasured) {
        if (isMeasured) {
            revealAmount.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = CubicBezierEasing(0.7f, 0f, 0.2f, 1f)
                )
            )
        }
    }

    val triggerExit = {
        if (!isClosing) {
            isClosing = true
            keyboardController?.hide()
            focusManager.clearFocus()
            onClosing()
            scope.launch {
                revealAmount.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
                    )
                )
                onBack()
            }
        }
    }

    androidx.activity.compose.BackHandler(enabled = !isClosing && !isDetailVisible) {
        if (isFilterVisible) {
            onToggleFilter(false, null)
        } else {
            triggerExit()
        }
    }

    val focusRequester = remember { FocusRequester() }
    var hasRequestedFocus by remember { mutableStateOf(false) }
    var filterBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    
    val density = androidx.compose.ui.platform.LocalDensity.current
    var headerHeight by remember { mutableStateOf(250.dp) }
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val padding = 16.dp
    val gap = 12.dp
    val cardWidth = (screenWidth - (padding * 2) - (gap * 2)) / 3
    val personCardWidth = (screenWidth - (padding * 2) - (gap * 3)) / 4

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        if (width > 0 && !isMeasured) {
            LaunchedEffect(Unit) { isMeasured = true }
        }

        val center = Offset(x = startX ?: (width / 2f), y = startY ?: (height / 2f))

        val maxRadius = remember(center, width, height) {
            val distTopLeft = sqrt(center.x.pow(2) + center.y.pow(2))
            val distTopRight = sqrt((width - center.x).pow(2) + center.y.pow(2))
            val distBottomLeft = sqrt(center.x.pow(2) + (height - center.y).pow(2))
            val distBottomRight = sqrt((width - center.x).pow(2) + (height - center.y).pow(2))
            max(max(distTopLeft, distTopRight), max(distBottomLeft, distBottomRight)) * 1.1f
        }

        // Reveal Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val radius = revealAmount.value * maxRadius
                    clip = true
                    shape = object : Shape {
                        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                            val path = Path().apply { addOval(Rect(center = center, radius = radius)) }
                            return Outline.Generic(path)
                        }
                    }
                }
                .background(Color.Black)
        ) {
            val isTransitioning = isClosing || revealAmount.value < 1f

            LaunchedEffect(isTransitioning) {
                if (!isTransitioning && !hasRequestedFocus) {
                    hasRequestedFocus = true
                    if (initialGenreName == null) {
                        try {
                            focusRequester.requestFocus()
                        } catch (e: Exception) { }
                    } else {
                        keyboardController?.hide()
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                com.cinetrack.ui.components.shared.MovieActionsWrapper(
                    hazeState = internalHazeState,
                    folders = uiState.folders,
                    isItemInFolder = { movie, folderId ->
                        uiState.folders.find { it.id == folderId }?.itemIds?.contains("${movie.mediaType}_${movie.id}") ?: false
                    },
                    onDelete = { viewModel.deleteMovie(it) },
                    onUpdateRating = { movie, rating -> viewModel.updateRating(movie, rating) },
                    onUpdateNote = { movie, note -> viewModel.updateNote(movie, note) },
                    onToggleFolder = { movie, folder -> viewModel.toggleItemInFolder(folder, movie) }
                ) { actionsState ->
                Box(modifier = Modifier.fillMaxSize()) {
                // 1. Background Source Layer (Only captures what's behind the header/glass)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(
                            state = internalHazeState,
                            style = HazeStyles.PremiumDark
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(com.cinetrack.ui.theme.PremiumBackground)
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { focusManager.clearFocus() })
                            }
                    ) {

                    val gridState = rememberLazyGridState()
                    val shouldLoadMore = remember {
                        derivedStateOf {
                            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
                            lastVisibleItem.index >= gridState.layoutInfo.totalItemsCount - 5
                        }
                    }

                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value && !uiState.isEndReached && !uiState.isNextPageLoading) {
                            viewModel.loadNextPage()
                        }
                    }

                    if (uiState.errorMessage != null) {
                        // PREMIUM ERROR STATE
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Qualcosa è andato storto",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.errorMessage ?: "Impossibile caricare i dati. Controlla la tua connessione e riprova.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { viewModel.retry() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.onSurface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("RIPROVA")
                            }
                        }
                    } else {                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(12),
                            contentPadding = PaddingValues(
                                top = headerHeight + 16.dp,
                                bottom = paddingValues.calculateTopPadding() + 80.dp,
                                start = 12.dp,
                                end = 12.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val showEmptySearch = uiState.query.isEmpty() && (uiState.sortConfig.selectedGenres.isEmpty() || uiState.category == "person")
                            
                            if (showEmptySearch) {
                                if (uiState.category == "movie" && uiState.trendingMovies.isNotEmpty()) {
                                    item(span = { GridItemSpan(12) }) {
                                        Column {
                                            Text(
                                                text = "Tendenze del momento",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                            )
                                            TrendingHeader("FILM")
                                        }
                                    }
                                    itemsIndexed(items = uiState.trendingMovies.take(6), key = { _, item -> "trending_movie_${item.id}" }, span = { _, _ -> GridItemSpan(4) }) { index, item ->
                                        if (item is TMDBSearchResult.MovieResult) {
                                            val movie = Movie(id = item.id, mediaType = "movie", title = item.title, posterPath = item.posterPath, backdropPath = item.backdropPath, voteAverage = item.voteAverage, releaseDate = item.releaseDate, overview = item.overview, genreIds = item.genreIds)
                                            val movieStatus = uiState.favorites.find { it.id == movie.id && it.mediaType == "movie" }
                                            val folderColors = remember(movie.id, uiState.movieFolderColors) {
                                                uiState.movieFolderColors["${movie.mediaType}_${movie.id}"]?.map { 
                                                    it.toComposeColor()
                                                } ?: emptyList()
                                            }
                                            MovieCard(
                                                movie = movie,
                                                cardWidth = cardWidth,
                                                isFavorite = movieStatus?.favorite ?: false,
                                                isWatched = movieStatus?.watched ?: false,
                                                isReminder = movieStatus?.reminder ?: false,
                                                progress = movieStatus?.progress?.toFloat() ?: 0f,
                                                folderColors = folderColors,
                                                showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                staggerIndex = index,
                                                onPress = { 
                                                    keyboardController?.hide()
                                                    onMovieClick(movie) 
                                                },
                                                onAction = { viewModel.toggleFavorite(movie) },
                                                onLongPress = { m, pressOffset, cardPos ->
                                                    actionsState.onLongPress(m, pressOffset, cardPos)
                                                },
                                                onMessage = { viewModel.emitMessage(it) }
                                            )
                                        }
                                    }
                                }

                                if (uiState.category == "tv" && uiState.trendingTv.isNotEmpty()) {
                                    item(span = { GridItemSpan(12) }) {
                                        Column {
                                            Text(
                                                text = "Tendenze del momento",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                            )
                                            TrendingHeader("SERIE TV")
                                        }
                                    }
                                    itemsIndexed(items = uiState.trendingTv.take(6), key = { _, item -> "trending_tv_${item.id}" }, span = { _, _ -> GridItemSpan(4) }) { index, item ->
                                        if (item is TMDBSearchResult.TvResult) {
                                            val movie = Movie(id = item.id, mediaType = "tv", name = item.name, posterPath = item.posterPath, backdropPath = item.backdropPath, voteAverage = item.voteAverage, firstAirDate = item.firstAirDate, overview = item.overview, genreIds = item.genreIds)
                                            val movieStatus = uiState.favorites.find { it.id == movie.id && it.mediaType == "tv" }
                                            val folderColors = remember(movie.id, uiState.movieFolderColors) {
                                                uiState.movieFolderColors["${movie.mediaType}_${movie.id}"]?.map { 
                                                    it.toComposeColor()
                                                } ?: emptyList()
                                            }
                                            MovieCard(
                                                movie = movie,
                                                cardWidth = cardWidth,
                                                isFavorite = movieStatus?.favorite ?: false,
                                                isWatched = movieStatus?.watched ?: false,
                                                isReminder = movieStatus?.reminder ?: false,
                                                progress = movieStatus?.progress?.toFloat() ?: 0f,
                                                folderColors = folderColors,
                                                showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                staggerIndex = index,
                                                onPress = { 
                                                    keyboardController?.hide()
                                                    onMovieClick(movie) 
                                                },
                                                onAction = { viewModel.toggleFavorite(movie) },
                                                onLongPress = { m, pressOffset, cardPos ->
                                                    actionsState.onLongPress(m, pressOffset, cardPos)
                                                },
                                                onMessage = { viewModel.emitMessage(it) }
                                            )
                                        }
                                    }
                                }

                                if (uiState.category == "person" && uiState.trendingPeople.isNotEmpty()) {
                                    item(span = { GridItemSpan(12) }) {
                                        Column {
                                            Text(
                                                text = "Tendenze del momento",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                            )
                                            TrendingHeader("PERSONE")
                                        }
                                    }
                                    items(items = uiState.trendingPeople.take(8), key = { "trending_person_${it.id}" }, contentType = { "person_result" }, span = { GridItemSpan(3) }) { item ->
                                        if (item is TMDBSearchResult.PersonResult) {
                                            PersonCard(person = PersonSearchResult(id = item.id, name = item.name, profilePath = item.profilePath, knownForDepartment = item.knownForDepartment), width = personCardWidth, onClick = { 
                                                keyboardController?.hide()
                                                onPersonClick(item.id) 
                                            })
                                        }
                                    }
                                }
                            } else {
                                if (uiState.query.isEmpty() && uiState.sortConfig.selectedGenres.isNotEmpty()) {
                                    item(span = { GridItemSpan(12) }) { Spacer(modifier = Modifier.height(72.dp)) }
                                }
                                
                                if (uiState.results.isEmpty() && uiState.isLoading) {
                                    items(if (uiState.category == "person") 16 else 12, contentType = { "skeleton" }, span = { if (uiState.category == "person") GridItemSpan(3) else GridItemSpan(4) }) { 
                                        MovieCardSkeleton(width = if (uiState.category == "person") personCardWidth else cardWidth) 
                                    }
                                }

                                if (uiState.query.isNotEmpty() && !uiState.isLoading && uiState.results.isEmpty()) {
                                    item(span = { GridItemSpan(12) }) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(imageVector = Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(text = "Nessun risultato", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = "Nessun risultato trovato.\nProva a semplificare la ricerca", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        }
                                    }
                                }

                                itemsIndexed(
                                    items = uiState.results,
                                    key = { _, item -> when(item) {
                                        is TMDBSearchResult.MovieResult -> "${item.id}_movie"
                                        is TMDBSearchResult.TvResult -> "${item.id}_tv"
                                        is TMDBSearchResult.PersonResult -> "${item.id}_person"
                                    }},
                                    span = { _, _ -> if (uiState.category == "person") GridItemSpan(3) else GridItemSpan(4) }
                                ) { index, item ->
                                    when(item) {
                                        is TMDBSearchResult.MovieResult -> {
                                            val movie = Movie(id = item.id, mediaType = "movie", title = item.title, posterPath = item.posterPath, backdropPath = item.backdropPath, voteAverage = item.voteAverage, releaseDate = item.releaseDate, overview = item.overview, genreIds = item.genreIds)
                                            val movieStatus = uiState.favorites.find { it.id == movie.id && it.mediaType == "movie" }
                                            
                                            val folderColors = remember(movie.id, uiState.movieFolderColors) {
                                                uiState.movieFolderColors["${movie.mediaType}_${movie.id}"]?.map { 
                                                    it.toComposeColor()
                                                } ?: emptyList()
                                            }
                                            MovieCard(
                                                movie = movie,
                                                cardWidth = cardWidth,
                                                isFavorite = movieStatus?.favorite ?: false,
                                                isWatched = movieStatus?.watched ?: false,
                                                isReminder = movieStatus?.reminder ?: false,
                                                progress = movieStatus?.progress?.toFloat() ?: 0f,
                                                folderColors = folderColors,
                                                showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                staggerIndex = index,
                                                onPress = { 
                                                    keyboardController?.hide()
                                                    onMovieClick(movie) 
                                                },
                                                onAction = { viewModel.toggleFavorite(movie) },
                                                onLongPress = { m, pressOffset, cardPos ->
                                                    actionsState.onLongPress(m, pressOffset, cardPos)
                                                },
                                                onMessage = { viewModel.emitMessage(it) }
                                            )
                                        }
                                        is TMDBSearchResult.TvResult -> {
                                            val movie = Movie(id = item.id, mediaType = "tv", name = item.name, posterPath = item.posterPath, backdropPath = item.backdropPath, voteAverage = item.voteAverage, firstAirDate = item.firstAirDate, overview = item.overview, genreIds = item.genreIds)
                                            val movieStatus = uiState.favorites.find { it.id == movie.id && it.mediaType == "tv" }
                                            val folderColors = remember(movie.id, uiState.movieFolderColors) {
                                                uiState.movieFolderColors["${movie.mediaType}_${movie.id}"]?.map { 
                                                    it.toComposeColor()
                                                } ?: emptyList()
                                            }
                                            MovieCard(
                                                movie = movie,
                                                cardWidth = cardWidth,
                                                isFavorite = movieStatus?.favorite ?: false,
                                                isWatched = movieStatus?.watched ?: false,
                                                isReminder = movieStatus?.reminder ?: false,
                                                progress = movieStatus?.progress?.toFloat() ?: 0f,
                                                folderColors = folderColors,
                                                showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                staggerIndex = index,
                                                onPress = { 
                                                    keyboardController?.hide()
                                                    onMovieClick(movie) 
                                                },
                                                onAction = { viewModel.toggleFavorite(movie) },
                                                onLongPress = { m, pressOffset, cardPos ->
                                                    actionsState.onLongPress(m, pressOffset, cardPos)
                                                },
                                                onMessage = { viewModel.emitMessage(it) }
                                            )
                                        }
                                        is TMDBSearchResult.PersonResult -> {
                                            PersonCard(person = PersonSearchResult(id = item.id, name = item.name, profilePath = item.profilePath, knownForDepartment = item.knownForDepartment), width = personCardWidth, onClick = { 
                                                keyboardController?.hide()
                                                onPersonClick(item.id) 
                                            })
                                        }
                                    }
                                }

                                if (uiState.isNextPageLoading) {
                                    items(if (uiState.category == "person") 4 else 3, contentType = { "skeleton" }, span = { if (uiState.category == "person") GridItemSpan(3) else GridItemSpan(4) }) { 
                                        MovieCardSkeleton(width = if (uiState.category == "person") personCardWidth else cardWidth) 
                                    }
                                }
                            }
                        }
                    }
                } // End Background Source Layer
                }

                // 2. Header (Moved outside blurring boxes to stay sharp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.TopCenter)
                        .zIndex(10f)
                        .onGloballyPositioned { layoutCoordinates ->
                            headerHeight = with(density) { layoutCoordinates.size.height.toDp() }
                        }
                ) {
                    // Header Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.displayCutout)
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    ) {

                        // Row 1: Back, Search, Filter
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(44.dp)) {
                                Box(modifier = Modifier.fillMaxSize().hazeGlass(state = internalHazeState, shape = CircleShape, blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = true))
                                Box(modifier = Modifier.fillMaxSize().bounceClick { triggerExit() }, contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Torna indietro", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                             Box(modifier = Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.CenterStart) {
                                 Box(modifier = Modifier.fillMaxSize().hazeGlass(state = internalHazeState, shape = CircleShape, blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = true))
                                 Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp)) {
                                    Icon(imageVector = CustomIcons.PremiumSearch, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    CompositionLocalProvider(LocalTextSelectionColors provides TextSelectionColors(handleColor = primaryColor, backgroundColor = primaryColor.copy(alpha = 0.4f))) {
                                        BasicTextField(
                                            value = uiState.query,
                                            onValueChange = { viewModel.onQueryChanged(it) },
                                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = { viewModel.onQueryChanged(uiState.query) }),
                                            decorationBox = { innerTextField ->
                                                Box(contentAlignment = Alignment.CenterStart) {
                                                    if (uiState.query.isEmpty()) {
                                                        Text(text = if (initialGenreName != null) "Genere: $initialGenreName" else "Cerca film, serie, persone...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (initialGenreName != null) 0.6f else 0.3f), fontSize = 14.sp)
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )
                                    }
                                    if (uiState.query.isNotEmpty()) {
                                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Pulisci", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(20.dp).bounceClick(scaleDown = 0.8f) { viewModel.onQueryChanged("") })
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            val hasActiveFilters = uiState.sortConfig.selectedGenres.isNotEmpty() || uiState.sortConfig.selectedProviders.isNotEmpty() || uiState.sortConfig.selectedDecades.isNotEmpty()
                            Box(modifier = Modifier.size(44.dp).onGloballyPositioned { layoutCoordinates ->
                                val position = layoutCoordinates.positionInWindow()
                                filterBounds = Rect(position.x, position.y, position.x + layoutCoordinates.size.width, position.y + layoutCoordinates.size.height)
                            }) {
                                Box(modifier = Modifier.fillMaxSize()
                                     .hazeGlass(state = internalHazeState, shape = CircleShape, blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = true)
                                    .then(if (hasActiveFilters) Modifier.border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), CircleShape) else Modifier))
                                Box(modifier = Modifier.fillMaxSize().bounceClick { keyboardController?.hide(); focusManager.clearFocus(); onToggleFilter(true, filterBounds) }, contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Rounded.Tune, contentDescription = "Filtri", tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        if (uiState.recentSearches.isNotEmpty() && uiState.query.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("RECENTI", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(
                                    text = "CANCELLA TUTTO",
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.bounceClick { viewModel.clearRecentSearches() }.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.fillMaxWidth()) {
                                items(uiState.recentSearches, key = { it }, contentType = { "recent_search" }) { search ->
                                    Box(
                                        modifier = Modifier
                                            .height(32.dp)
                                            .bounceClick { viewModel.onQueryChanged(search) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().hazeGlass(state = internalHazeState, shape = CircleShape, blurRadius = HazeStyles.SmallGlassBlurRadius, useOffscreenStrategy = true))
                                        Row(
                                            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(search, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Rimuovi ricerca",
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .bounceClick(scaleDown = 0.8f) { viewModel.deleteRecentSearch(search) },
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }

                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Category Selector (Pillow)
                        val selectedIndex = when (uiState.category) {
                            "movie" -> 0
                            "tv" -> 1
                            "person" -> 2
                            else -> 0
                        }
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                            .height(40.dp)) {
                             Box(modifier = Modifier.fillMaxSize().hazeGlass(
                                 state = internalHazeState, 
                                 shape = CircleShape, 
                                 blurRadius = HazeStyles.SmallGlassBlurRadius,
                                 useOffscreenStrategy = true
                             ))
                             
                             BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                val tabWidth = maxWidth / 3
                                val tabWidthPx = with(LocalDensity.current) { tabWidth.toPx() }
                                
                                val offsetAnimatable = remember { androidx.compose.animation.core.Animatable(selectedIndex * tabWidthPx) }
                                val coroutineScope = rememberCoroutineScope()
                                
                                LaunchedEffect(selectedIndex) {
                                    if (!offsetAnimatable.isRunning) {
                                        offsetAnimatable.animateTo(
                                            targetValue = selectedIndex * tabWidthPx,
                                            animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
                                        )
                                    }
                                }
                                
                                // Liquid stretch effect
                                val velocity = offsetAnimatable.velocity
                                val stretchFactor = 1f + (kotlin.math.abs(velocity) / 4000f).coerceAtMost(0.35f)
                                val currentOffset = offsetAnimatable.value
                                val extraWidth = (tabWidth * stretchFactor) - tabWidth
                                val adjustedOffset = currentOffset - with(LocalDensity.current) { (extraWidth / 2).toPx() }
                                
                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                coroutineScope.launch {
                                                    val targetIndex = (offsetAnimatable.value / tabWidthPx).roundToInt().coerceIn(0, 2)
                                                    val categoryStr = when(targetIndex) {
                                                        0 -> "movie"
                                                        1 -> "tv"
                                                        else -> "person"
                                                    }
                                                    viewModel.onCategoryChanged(categoryStr)
                                                    
                                                    offsetAnimatable.animateTo(
                                                        targetValue = targetIndex * tabWidthPx,
                                                        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
                                                        initialVelocity = offsetAnimatable.velocity
                                                    )
                                                }
                                            },
                                            onDragCancel = {
                                                coroutineScope.launch {
                                                    offsetAnimatable.animateTo(
                                                        targetValue = selectedIndex * tabWidthPx,
                                                        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
                                                    )
                                                }
                                            },
                                            onHorizontalDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float ->
                                                change.consume()
                                                coroutineScope.launch {
                                                    val newVal = (offsetAnimatable.value + dragAmount).coerceIn(0f, tabWidthPx * 2)
                                                    offsetAnimatable.snapTo(newVal)
                                                }
                                            }
                                        )
                                    }
                                ) {
                                    Box(modifier = Modifier
                                        .offset { androidx.compose.ui.unit.IntOffset(adjustedOffset.roundToInt(), 0) }
                                        .width(tabWidth * stretchFactor)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                    )
                                    
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        CategoryTab("FILM", uiState.category == "movie") { viewModel.onCategoryChanged("movie") }
                                        CategoryTab("SERIE TV", uiState.category == "tv") { viewModel.onCategoryChanged("tv") }
                                        CategoryTab("PERSONE", uiState.category == "person") { viewModel.onCategoryChanged("person") }
                                    }
                                }
                             }
                        }

                    }
                }
                }
                }
            }
        }

        // --- Modal (Top Level to avoid clipping/zIndex issues) ---
        Box(modifier = Modifier.zIndex(5000f)) {
            val dimAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isFilterVisible) 0.65f else 0f,
                animationSpec = tween(400),
                label = "searchDimAlpha"
            )
            
            if (dimAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = dimAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onToggleFilter(false, null) }
                )
            }
            
            HomeFilterModal(
                isVisible = isFilterVisible,
                sortConfig = uiState.sortConfig,
                hazeState = internalHazeState,
                triggerBounds = filterBounds,
                category = uiState.category,
                onSortConfigChanged = { viewModel.updateSortConfig(it) },
                onDismissRequest = { onToggleFilter(false, null) }
            )
        }
    }
}




@Composable
fun RowScope.CategoryTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "tabScale")

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun TrendingHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}
