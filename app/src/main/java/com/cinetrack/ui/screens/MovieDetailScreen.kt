@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
package com.cinetrack.ui.screens

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import com.cinetrack.ui.utils.premiumScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.lerp
import com.cinetrack.ui.components.shared.FolderPickerModalContent
import com.cinetrack.ui.utils.bounceClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.data.Movie
import com.cinetrack.ui.components.*
import com.cinetrack.ui.components.detail.*
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.ui.theme.PrimaryTeal
import com.cinetrack.ui.viewmodel.MovieDetailViewModel
import com.cinetrack.ui.viewmodel.DetailUiState
import com.cinetrack.ui.viewmodel.DetailEvent
import com.cinetrack.ui.viewmodel.WatchState
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.Coil
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.theme.PremiumBackground
import com.cinetrack.ui.theme.HazeStyles
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.core.screen.Screen
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.util.toComposeColor
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class MovieDetailScreen(
    val movieId: Long,
    val mediaType: String
) : Screen {
    override val key: ScreenKey = uniqueScreenKey
    @Composable
    override fun Content() {
        val viewModel = getViewModel<MovieDetailViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        val detailStackDepth = androidx.compose.runtime.remember(navigator.items) {
            navigator.items.count { it is MovieDetailScreen || it is com.cinetrack.ui.screens.PersonDetailScreen }
        }

        LaunchedEffect(movieId, mediaType) {
            kotlinx.coroutines.delay(250)
            viewModel.initMovie(movieId, mediaType)
        }

        val searchOverlay = com.cinetrack.ui.LocalSearchOverlay.current

        MovieDetailScreenContent(
            viewModel = viewModel,
            paddingValues = PaddingValues(0.dp),
            onBackClick = { navigator.pop() },
            onMovieClick = { movie ->
                navigator.push(MovieDetailScreen(movie.id, movie.mediaType))
            },
            detailStackDepth = detailStackDepth,
            onPersonClick = { personId, profilePath ->
                navigator.push(PersonDetailScreen(personId, profilePath))
            },
            onGenreClick = { genreId, genreName, offset ->
                searchOverlay?.invoke(offset, genreId, genreName, null, null)
            },
            onKeywordClick = { keywordId, keywordName, offset ->
                searchOverlay?.invoke(offset, null, null, keywordId, keywordName)
            },
            onHomeClick = {
                navigator.popUntilRoot()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MovieDetailScreenContent(
    viewModel: MovieDetailViewModel,
    paddingValues: PaddingValues,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    hazeState: HazeState? = null,
    onBackClick: () -> Unit = {},
    onMovieClick: (Movie) -> Unit = {},
    onPersonClick: (Long, String?) -> Unit = { _, _ -> },
    onGenreClick: (Long, String?, Offset) -> Unit = { _, _, _ -> },
    onKeywordClick: (Long, String?, Offset) -> Unit = { _, _, _ -> },
    detailStackDepth: Int = 1,
    onHomeClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val folders = (uiState as? DetailUiState.Success)?.folders ?: emptyList()
    val context = androidx.compose.ui.platform.LocalContext.current
    val extractedColor by viewModel.extractedColor.collectAsStateWithLifecycle()
    var isHeaderVisible by remember { mutableStateOf(true) }
    var showEpisodesSheet by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showRatingInfoDialog by remember { mutableStateOf(false) }
    val showTranslationPrompt by viewModel.showTranslationPrompt.collectAsStateWithLifecycle()
    val translationStates by viewModel.translationStates.collectAsStateWithLifecycle()
    val movieActions = com.cinetrack.ui.components.shared.LocalMovieActions.current
    val useMovieLogo by viewModel.useMovieLogo.collectAsStateWithLifecycle()

    var hasCompletedFirstEnter by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasCompletedFirstEnter = true
    }
    val effectiveSharedTransitionScope = if (hasCompletedFirstEnter) null else sharedTransitionScope

    androidx.activity.compose.BackHandler(enabled = true) {
        if (movieActions.isAnyModalOpen) {
            movieActions.closeAll()
        } else if (showRatingInfoDialog) {
            showRatingInfoDialog = false
        } else if (showTranslationPrompt != null) {
            viewModel.dismissTranslationPrompt()
        } else if (showFolderPicker) {
            showFolderPicker = false
        } else if (showEpisodesSheet) {
            showEpisodesSheet = false
        } else {
            onBackClick()
        }
    }
    
    val currentImageQuality = LocalImageQuality.current
    // Estrazione colore dinamica se accentColor è nullo nel database
    LaunchedEffect(uiState, currentImageQuality) {
        val currentState = uiState
        if (currentState is DetailUiState.Success) {
            val movie = currentState.movieEntry
            val imageUrl = buildTmdbImageUrl(movie.posterPath ?: movie.backdropPath, ImageType.POSTER, currentImageQuality)
            if (imageUrl != null) {
                viewModel.fetchAccentColor(imageUrl, movie)
            }
        }
    }

    val density = LocalDensity.current
    val scrollThreshold = with(density) { 320.dp.toPx() } // Increased for a more deliberate transition
    val scrollProgress by remember {
        derivedStateOf { (scrollState.value / scrollThreshold).coerceIn(0f, 1f) }
    }
    
    // Hybrid symbiote bar logic:
    // 1. Stretches while scrolling (motion-driven).
    // 2. Returns to separate circles if stopped "halfway".
    // 3. STAYS merged if it has reached the full pill state (where the title is).
    // 4. Splits during navigation transitions (like predictive back).
    val isScrolling = scrollState.isScrollInProgress
    val isTransitioning = animatedVisibilityScope?.transition?.let { it.currentState != it.targetState } ?: false
    val isMerged = scrollProgress >= 1f && !isTransitioning
    
    val targetSymbioteProgress = if (isScrolling || isMerged) scrollProgress else 0f
    
    val symbioteProgress by animateFloatAsState(
        targetValue = targetSymbioteProgress,
        animationSpec = if (isScrolling || isMerged) {
            // Rapid follow during scroll or when locked in merged state
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            // Organic return to circles when stopped halfway or during transition
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "symbioteProgress"
    )

    val localHazeState = remember { HazeState() }
    val backdropHazeState = remember { HazeState() }
    
    val globalAccentColor = when (val state = uiState) {
        is DetailUiState.Success -> state.movieEntry.accentColor.toComposeColor(extractedColor ?: Color(0xFF1A1A1A))
        else -> extractedColor ?: Color(0xFF1A1A1A)
    }

    // Removed showRatingDialog and showNoteDialog as they are now handled inline

    val targetBackgroundColor = when (val state = uiState) {
        is DetailUiState.Success -> {
            val baseColor = state.movieEntry.accentColor.toComposeColor(extractedColor ?: Color(0xFF1A1A1A))
            
            lerp(baseColor, Color.Black, 0.65f)
        }
        else -> Color(0xFF0A0A0A)
    }
    val animatedBgColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 800),
        label = "backgroundColor"
    )

    // Press states for top bar buttons to scale only icons
    var isBackPressed by remember { mutableStateOf(false) }
    val backIconScale by animateFloatAsState(
        targetValue = if (isBackPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = if (isBackPressed) 10000f else Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "BackIconScale"
    )

    var isFolderButtonPressed by remember { mutableStateOf(false) }
    val folderIconScale by animateFloatAsState(
        targetValue = if (isFolderButtonPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = if (isFolderButtonPressed) 10000f else Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "FolderIconScale"
    )

    var isShareButtonPressed by remember { mutableStateOf(false) }
    val shareIconScale by animateFloatAsState(
        targetValue = if (isShareButtonPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = if (isShareButtonPressed) 10000f else Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "ShareIconScale"
    )

    var cachedSuccess by remember { mutableStateOf<DetailUiState.Success?>(null) }
    if (uiState is DetailUiState.Success) {
        cachedSuccess = uiState as DetailUiState.Success
    }

    MovieActionsWrapper(
        hazeState = localHazeState,
        folders = (uiState as? DetailUiState.Success)?.folders ?: emptyList(),
        isItemInFolder = { movie, folderId -> (uiState as? DetailUiState.Success)?.folders?.find { it.id == folderId }?.itemIds?.contains("${movie.mediaType}_${movie.id}") == true },
        onUpdateRating = { movie, rating -> viewModel.onEvent(DetailEvent.RateMovie(movie, rating)) },
        onUpdateNote = { movie, note -> viewModel.onEvent(DetailEvent.UpdateMovieNote(movie, note)) },
        onToggleFolder = { movie, folder -> viewModel.onEvent(DetailEvent.ToggleMovieFolderMembership(movie, folder)) },
        onDelete = { movie -> viewModel.onEvent(DetailEvent.DeleteMovieItem(movie)) }
    ) { actionsState ->
        Box(modifier = Modifier.fillMaxSize().background(PremiumBackground).background(animatedBgColor)) {
        
        AnimatedContent(
            targetState = when (uiState) {
                is DetailUiState.Loading -> 0
                is DetailUiState.Success -> 1
                is DetailUiState.Error -> 2
            },
            transitionSpec = {
                fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) togetherWith 
                fadeOut(animationSpec = tween(400, easing = FastOutSlowInEasing))
            },
            label = "DetailUiStateTransition"
        ) { targetIndex ->
            when (targetIndex) {
                0 -> {
                    DetailSkeleton(
                        hazeState = localHazeState,
                        paddingValues = paddingValues
                    )
                }
                2 -> {
                    val errorState = uiState as? DetailUiState.Error
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_cloud),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.White.copy(alpha = 0.3f)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = stringResource(R.string.detail_error_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = errorState?.message ?: stringResource(R.string.detail_error_unknown),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { viewModel.onEvent(DetailEvent.Refresh) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .padding(horizontal = 32.dp)
                                .bounceClick { viewModel.onEvent(DetailEvent.Refresh) }
                        ) {
                            Text(stringResource(R.string.detail_retry), fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        }

                        TextButton(
                            onClick = onBackClick,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(stringResource(R.string.detail_go_back), color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
                1 -> {
                    val state = cachedSuccess
                    if (state != null) {
                        val activeMovie = state.movieEntry
                        val accentColor = activeMovie.accentColor.toComposeColor(extractedColor ?: Color(0xFF1A1A1A))
                        
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .haze(localHazeState, style = HazeStyles.PremiumDark)
                                    .verticalScroll(scrollState)
                            ) {
                                DetailBackdrop(
                                    backdropPath = activeMovie.backdropPath,
                                    posterPath = activeMovie.posterPath,
                                    accentColor = accentColor,
                                    backgroundColor = animatedBgColor,
                                    modifier = Modifier
                                        .haze(backdropHazeState, style = HazeStyles.PremiumDark)
                                )

                                Column(
                                    modifier = Modifier
                                        .offset(y = (-140).dp)
                                        .padding(bottom = paddingValues.calculateBottomPadding() + 60.dp)
                                ) {
                                    DetailHeader(
                                        movie = activeMovie,
                                        ratings = state.externalRatings,
                                        accentColor = accentColor,
                                        matchPercentage = state.matchPercentage,
                                        logoPath = if (useMovieLogo) {
                                            val currentLang = java.util.Locale.getDefault().language
                                            val logos = state.details?.images?.logos
                                            val bestLogo = logos?.firstOrNull { it.iso6391 == currentLang } 
                                                ?: logos?.firstOrNull { it.iso6391 == "en" } 
                                                ?: logos?.firstOrNull()
                                            bestLogo?.filePath
                                        } else null,
                                        hazeState = backdropHazeState,
                                        sharedTransitionScope = effectiveSharedTransitionScope,
                                        onRatingClick = { showRatingInfoDialog = true }
                                    )

                                    DetailMetaRows(
                                        genres = state.movieEntry.genres ?: emptyList(),
                                        keywords = state.details.keywords?.getList()?.take(10) ?: emptyList(),
                                        streaming = state.streamingProviders,
                                        buyAndRent = state.buyRentProviders,
                                        accentColor = accentColor,
                                        onGenreClick = { genre, offset -> onGenreClick(genre.id, genre.name, offset) },
                                        onKeywordClick = { id, name, offset -> onKeywordClick(id, name, offset) },
                                        onProviderClick = { provider ->
                                            val title = java.net.URLEncoder.encode(state.movieEntry.displayName, "UTF-8")
                                            val name = provider.providerName.lowercase().trim()
                                            val deepLink = when {
                                                name.contains("netflix") -> "https://www.netflix.com/search?q=$title"
                                                name.contains("prime video") || name.contains("amazon prime") || name.contains("amazon video") -> "https://app.primevideo.com/search?searchTerm=$title"
                                                name.contains("disney") -> "https://www.disneyplus.com/search?q=$title"
                                                name.contains("apple") -> "https://tv.apple.com/search?term=$title"
                                                name.contains("crunchyroll") -> "https://www.crunchyroll.com/search?q=$title"
                                                name.contains("now") || name.contains("sky") -> "https://www.nowtv.it/ricerca?q=$title"
                                                name.contains("paramount") -> "https://www.paramountplus.com/search?q=$title"
                                                name.contains("youtube") -> "https://www.youtube.com/results?search_query=$title"
                                                name.contains("google play") -> "https://play.google.com/store/search?q=$title&c=movies"
                                                name.contains("rakuten") -> "https://rakuten.tv/it/search?q=$title"
                                                name.contains("timvision") -> "https://www.timvision.it/search?q=$title"
                                                name.contains("infinity") || name.contains("mediaset") -> "https://mediasetinfinity.mediaset.it/ricerca/$title"
                                                name.contains("raiplay") -> "https://www.raiplay.it/ricerca.html?q=$title"
                                                name.contains("discovery") -> "https://www.discoveryplus.com/it/search?query=$title"
                                                name.contains("max") || name.contains("hbo") -> "https://play.max.com/search?q=$title"
                                                name.contains("hulu") -> "https://www.hulu.com/search?q=$title"
                                                name.contains("peacock") -> "https://www.peacocktv.com/watch/search?q=$title"
                                                name.contains("chili") -> "https://it.chili.com/search?q=$title"
                                                name.contains("mgm") -> "https://www.mgmplus.com/search?q=$title"
                                                else -> state.watchProviderLink
                                            }
                                            
                                            deepLink?.let { link ->
                                                try {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    viewModel.emitMessage(com.cinetrack.ui.utils.UiText.StringResource(R.string.detail_cannot_open_link))
                                                }
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(40.dp))

                                    DetailInfo(
                                        overview = activeMovie.overview ?: stringResource(R.string.detail_no_overview),
                                        accentColor = accentColor
                                    )

                                    Spacer(modifier = Modifier.height(40.dp))

                                    DetailPersonalZone(
                                        movie = activeMovie,
                                        accentColor = accentColor,
                                        hazeState = localHazeState,
                                        onRate = { viewModel.onEvent(DetailEvent.Rate(it)) },
                                        onNoteUpdate = { viewModel.onEvent(DetailEvent.UpdateNote(it)) }
                                    )

                                    Spacer(modifier = Modifier.height(40.dp))

                                    DetailCast(
                                        directors = state.directors,
                                        cast = state.cast,
                                        accentColor = accentColor,
                                        sharedTransitionScope = effectiveSharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onPersonClick = onPersonClick
                                    )

                                    Spacer(modifier = Modifier.height(40.dp))

                                    DetailTechnicalInfo(
                                        details = state.details,
                                        accentColor = accentColor
                                    )

                                    Spacer(modifier = Modifier.height(40.dp))

                                    if (state.trailers.isNotEmpty()) {
                                        DetailTrailers(
                                            trailers = state.trailers,
                                            accentColor = accentColor
                                        )
                                        Spacer(modifier = Modifier.height(48.dp))
                                    }
                                    
                                    if (state.traktComments.isNotEmpty()) {
                                        DetailComments(
                                            comments = state.traktComments,
                                            accentColor = accentColor,
                                            translationStates = translationStates,
                                            hazeState = localHazeState,
                                            onTranslateClick = { commentId, text ->
                                                viewModel.requestTranslation(commentId, text)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(48.dp))
                                    }

                                    DetailRecommendations(
                                        collection = state.details.belongsToCollection,
                                        collectionMovies = state.collectionMovies,
                                        recommendedMovies = state.recommendations,
                                        currentId = activeMovie.id,
                                        accentColor = accentColor,
                                        onMovieClick = onMovieClick,
                                        onLongPress = actionsState.onLongPress,
                                        onAction = { movie -> viewModel.onEvent(DetailEvent.CycleStatus(movie)) },
                                        onMessage = { viewModel.emitMessage(com.cinetrack.ui.utils.UiText.DynamicString(it)) },
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            if (!showEpisodesSheet) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = paddingValues.calculateBottomPadding() + 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DetailActions(
                                        movie = activeMovie,
                                        watchState = state.watchState,
                                        progress = state.watchedProgress,
                                        accentColor = accentColor,
                                        hazeState = localHazeState,
                                        onStateChange = { viewModel.onEvent(DetailEvent.SetWatchState(it)) },
                                        onRemove = { viewModel.onEvent(DetailEvent.DeleteMovie) },
                                        onEpisodesClick = { showEpisodesSheet = true }
                                    )
                                }
                            }

                            if (showEpisodesSheet) {
                                EpisodesBottomSheet(
                                    movie = activeMovie,
                                    viewModel = viewModel,
                                    hazeState = localHazeState,
                                    onDismiss = { showEpisodesSheet = false }
                                )
                            }
                        }
                    }
                }
            }
        }


        // Morphing Folder Picker & Dismiss Layer
        val successState = uiState as? DetailUiState.Success
        
        val movieInFolders = successState?.folders?.filter { folder ->
            val itemId = "${viewModel.mediaType}_${viewModel.movieId}"
            folder.itemIds.contains(itemId)
        } ?: emptyList()
        
        val folderColors = movieInFolders.map { 
            it.color.toComposeColor()
        }

        // Coordinated Transition for a premium feel
        val transition = updateTransition(targetState = showFolderPicker, label = "FolderMorph")
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val pillStartWidth = screenWidth - 40.dp
        val pillEndWidth = pillStartWidth // Keep buttons at the edges
            
        // Use the shared scrollProgress for consistency
        val currentScrollProgress = scrollProgress
        val collapsedPillWidth = pillStartWidth

        val modalWidth by transition.animateDp(
            transitionSpec = { 
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow) 
            },
            label = "width"
        ) { collapsedPillWidth }

        val modalHeight by transition.animateDp(
            transitionSpec = { 
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow) 
            },
            label = "height"
        ) { if (it) 500.dp else 44.dp }

        val modalCorner by transition.animateDp(label = "corner") { if (it) 28.dp else 22.dp }
        
        val modalExpansionProgress by transition.animateFloat(
            transitionSpec = { spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow) },
            label = "modalExpansion"
        ) { if (it) 1f else 0f }

        val contentAlpha by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(durationMillis = 200, delayMillis = 150)
                } else {
                    tween(durationMillis = 150)
                }
            },
            label = "contentAlpha"
        ) { if (it) 1f else 0f }

        val scrimAlpha by transition.animateFloat(label = "scrim") { if (it) 1f else 0f }

        // Dismiss Layer (Scrim)
        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
                    .background(Color.Black.copy(alpha = scrimAlpha * 0.6f))
                    .pointerInput(Unit) {
                        detectTapGestures { showFolderPicker = false }
                    }
            )
        }

        val currentEffectiveProgress = (symbioteProgress.coerceAtLeast(modalExpansionProgress)).coerceIn(0f, 1f)

        val symbioteShape: Shape = remember(currentEffectiveProgress, density, modalCorner) {
            GenericShape { size, _ ->
                val circleSize = with(density) { 44.dp.toPx() }
                val progress = currentEffectiveProgress
                val pillWidth = size.width
                val pillHeight = size.height
                val radius = with(density) { modalCorner.toPx() }
                
                if (progress <= 0.01f && pillHeight <= with(density) { 45.dp.toPx() }) return@GenericShape

                val stretchWidth = circleSize + (pillWidth / 2f - circleSize) * progress
                val p4 = progress * progress * progress * progress
                val innerRadius = radius * (1f - p4)
                
                val pathLeft = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(
                        left = 0f, top = 0f, right = stretchWidth + 2f, bottom = pillHeight,
                        topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                        topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius),
                        bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius),
                        bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
                    ))
                }
                
                val pathRight = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(
                        left = pillWidth - stretchWidth - 2f, top = 0f, right = pillWidth, bottom = pillHeight,
                        topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius),
                        topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                        bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                        bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius)
                    ))
                }
                
                addPath(androidx.compose.ui.graphics.Path.combine(
                    androidx.compose.ui.graphics.PathOperation.Union,
                    pathLeft,
                    pathRight
                ))
            }
        }

        // Morphing Container (Pill Bar -> Modal)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(11f)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = 8.dp), // Positioned higher as requested
            contentAlignment = Alignment.TopCenter
        ) {
                Box(
                    modifier = Modifier
                        .size(width = modalWidth, height = modalHeight),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (currentEffectiveProgress > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    clip = true
                                    shape = symbioteShape
                                }
                                .hazeGlass(
                                    state = localHazeState,
                                    shape = symbioteShape,
                                    useOffscreenStrategy = true
                                )
                        )
                    }
                Column(modifier = Modifier.fillMaxSize()) {
                    // Pill Header Row (always 44.dp high)
                    val iconBrush = when {
                        folderColors.isEmpty() -> SolidColor(Color.White)
                        folderColors.size == 1 -> SolidColor(folderColors.first())
                        else -> Brush.linearGradient(folderColors)
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        // Left side actions (Back + Home)
                        Row(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isBackPressed = true
                                            try { awaitRelease() } finally { isBackPressed = false }
                                        },
                                        onTap = {
                                            if (showFolderPicker) showFolderPicker = false 
                                            else onBackClick() 
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentEffectiveProgress <= 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .hazeGlass(
                                            state = localHazeState,
                                            shape = CircleShape,
                                            blurRadius = HazeStyles.SmallGlassBlurRadius,
                                            useOffscreenStrategy = true
                                        )
                                )
                            }
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_left),
                                contentDescription = stringResource(R.string.detail_content_desc_back),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = backIconScale
                                        scaleY = backIconScale
                                    }
                            )
                        }
                        // Home FAB — visible from the 3rd consecutive detail screen onwards
                        val homeButtonVisible = detailStackDepth >= 3
                        val homeButtonAlpha by animateFloatAsState(
                            targetValue = if (homeButtonVisible) 1f else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "HomeFabAlpha"
                        )
                        val homeButtonScale by animateFloatAsState(
                            targetValue = if (homeButtonVisible) 1f else 0.6f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "HomeFabScale"
                        )
                        if (homeButtonAlpha > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer {
                                        alpha = homeButtonAlpha
                                        scaleX = homeButtonScale
                                        scaleY = homeButtonScale
                                    }
                                    .then(
                                        if (currentEffectiveProgress <= 0.01f) {
                                            Modifier.hazeGlass(
                                                state = localHazeState,
                                                shape = CircleShape,
                                                blurRadius = HazeStyles.SmallGlassBlurRadius,
                                                useOffscreenStrategy = true
                                            )
                                        } else Modifier
                                    )
                                    .bounceClick { onHomeClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_home),
                                    contentDescription = stringResource(R.string.detail_content_desc_home),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        } // end Row

                        // Title Area: Swaps between Movie Title (on scroll) and Modal Title (on expansion)
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxHeight()
                                .padding(horizontal = 90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Movie Title: Visible when scrolled OR during modal expansion
                            val movieTitleAlpha = if (showFolderPicker) {
                                (1f - modalExpansionProgress).coerceIn(0f, 1f)
                            } else {
                                // Only start showing the title when the scroll is 70% complete
                                ((symbioteProgress - 0.7f) / 0.3f).coerceIn(0f, 1f)
                            }

                            if (movieTitleAlpha > 0.01f || (showFolderPicker && modalExpansionProgress < 1f)) {
                                Text(
                                    text = successState?.movieEntry?.displayName ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .alpha(movieTitleAlpha)
                                        .graphicsLayer {
                                            scaleX = 0.9f + (0.1f * movieTitleAlpha)
                                            scaleY = 0.9f + (0.1f * movieTitleAlpha)
                                            translationY = 10f * (1f - movieTitleAlpha)
                                        }
                                )
                            }

                            // Modal Title: Visible when expanding/expanded
                            if (modalExpansionProgress > 0.01f) {
                                Text(
                                    text = stringResource(R.string.detail_manage_folders),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .alpha(modalExpansionProgress)
                                        .graphicsLayer {
                                            translationY = 10f * (1f - modalExpansionProgress)
                                        }
                                )
                            }
                        }


                        // Right side actions (Share + Folder)
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Share Button
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .alpha(if (successState != null) 1f else 0f)
                                    .then(
                                        if (successState != null) {
                                            Modifier.pointerInput(Unit) {
                                                detectTapGestures(
                                                    onPress = {
                                                        isShareButtonPressed = true
                                                        try { awaitRelease() } finally { isShareButtonPressed = false }
                                                    },
                                                    onTap = { 
                                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                            val imageUrl = buildTmdbImageUrl(successState.movieEntry.posterPath ?: successState.movieEntry.backdropPath, ImageType.POSTER, currentImageQuality)
                                                            val fileUri = if (imageUrl != null) {
                                                                val request = coil.request.ImageRequest.Builder(context)
                                                                    .data(imageUrl)
                                                                    .build()
                                                                val result = context.imageLoader.execute(request)
                                                                if (result is coil.request.SuccessResult) {
                                                                    val bitmap = (result.drawable as android.graphics.drawable.BitmapDrawable).bitmap
                                                                    val imagesDir = java.io.File(context.cacheDir, "images")
                                                                    imagesDir.mkdirs()
                                                                    val file = java.io.File(imagesDir, "share_poster.jpg")
                                                                    val fos = java.io.FileOutputStream(file)
                                                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos)
                                                                    fos.close()
                                                                    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                                } else null
                                                            } else null

                                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                val sendIntent: android.content.Intent = android.content.Intent().apply {
                                                                    action = android.content.Intent.ACTION_SEND
                                                                    val link = "https://flicktrove.com/detail/${successState.movieEntry.mediaType}/${successState.movieEntry.id}"
                                                                    putExtra(android.content.Intent.EXTRA_TEXT, context.getString(R.string.detail_share_text, successState.movieEntry.displayName, link))
                                                                    if (fileUri != null) {
                                                                        putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                                                        type = "image/jpeg"
                                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                    } else {
                                                                        type = "text/plain"
                                                                    }
                                                                }
                                                                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                                                context.startActivity(shareIntent)
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentEffectiveProgress <= 0.01f && successState != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .hazeGlass(
                                                state = localHazeState,
                                                shape = CircleShape,
                                                blurRadius = HazeStyles.SmallGlassBlurRadius,
                                                useOffscreenStrategy = true
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_share),
                                    contentDescription = stringResource(R.string.detail_content_desc_share),
                                    tint = Color.White,
                                    modifier = Modifier
                                        .offset(x = -0.5.dp)
                                        .size(15.dp)
                                        .graphicsLayer {
                                            scaleX = shareIconScale
                                            scaleY = shareIconScale
                                        }
                                )
                            }

                            // Folder Button
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .alpha(if (successState != null) 1f else 0f)
                                .then(
                                    if (successState != null) {
                                        Modifier.pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isFolderButtonPressed = true
                                                    try { awaitRelease() } finally { isFolderButtonPressed = false }
                                                },
                                                onTap = { showFolderPicker = !showFolderPicker }
                                            )
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentEffectiveProgress <= 0.01f && successState != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .hazeGlass(
                                            state = localHazeState,
                                            shape = CircleShape,
                                            blurRadius = HazeStyles.SmallGlassBlurRadius,
                                            useOffscreenStrategy = true
                                        )
                                )
                            }
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_cartella_piena),
                                contentDescription = stringResource(R.string.detail_content_desc_folder),
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(alpha = 0.99f)
                                    .graphicsLayer {
                                        scaleX = folderIconScale
                                        scaleY = folderIconScale
                                    }
                                    .drawWithCache {
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(iconBrush, blendMode = BlendMode.SrcAtop)
                                        }
                                    },
                                tint = Color.White
                            )
                        }
                        }
                    }

                    // Modal Content Layer
                    if (contentAlpha > 0.01f) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().alpha(contentAlpha)) {
                            if (successState != null) {
                                FolderPickerModalContent(
                                    folders = successState.folders,
                                    isItemInFolder = { folderId: String -> 
                                        val itemId = "${viewModel.mediaType}_${viewModel.movieId}"
                                        successState.folders.find { it.id == folderId }?.itemIds?.contains(itemId) ?: false 
                                    },
                                    onToggleItem = { folder -> viewModel.onEvent(DetailEvent.ToggleFolderMembership(folder)) },
                                    onCreateFolder = { name: String, color: String -> viewModel.onEvent(DetailEvent.CreateFolder(name, color)) },
                                    onClose = { showFolderPicker = false },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Loading state for picker
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            }
                        }
                    }
                }
        }
        }
        }
    }

    // Ratings Info Dialog
    AnimatedVisibility(
        visible = showRatingInfoDialog,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val blurAlpha by transition.animateFloat(
            transitionSpec = { tween(200) },
            label = "blurAlpha"
        ) { if (it == androidx.compose.animation.EnterExitState.Visible) 0.85f else 0f }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * (blurAlpha / 0.85f)))
                .clickable { showRatingInfoDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.85f)
                    .hazeGlass(
                        state = localHazeState,
                        alpha = blurAlpha,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable(enabled = false) {}
            ) {
                val dialogScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .heightIn(max = 500.dp)
                ) {
                    // Title (Fixed)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    ) {
                        Icon(
                            ImageVector.vectorResource(id = R.drawable.ic_documento),
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.detail_rating_legend_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .premiumScrollbar(dialogScrollState)
                            .padding(end = 12.dp)
                            .verticalFadingEdges(dialogScrollState, 16.dp, 16.dp)
                            .verticalScroll(dialogScrollState)
                    ) {

                    // Film (MPAA)
                    Text(
                        stringResource(R.string.detail_rating_movies_usa),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RatingLegendItem("G", Color(0xFF4CAF50), stringResource(R.string.detail_rating_g))
                        RatingLegendItem("PG", Color(0xFF8BC34A), stringResource(R.string.detail_rating_pg))
                        RatingLegendItem("PG-13", Color(0xFFFF9800), stringResource(R.string.detail_rating_pg13))
                        RatingLegendItem("R", Color(0xFFF44336), stringResource(R.string.detail_rating_r))
                        RatingLegendItem("NC-17", Color(0xFFD32F2F), stringResource(R.string.detail_rating_nc17))
                        RatingLegendItem("NR", Color(0xFF9E9E9E), stringResource(R.string.detail_rating_nr))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Serie TV (TV Parental Guidelines)
                    Text(
                        stringResource(R.string.detail_rating_tv_usa),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RatingLegendItem("TV-Y", Color(0xFF4CAF50), stringResource(R.string.detail_rating_tvy))
                        RatingLegendItem("TV-Y7", Color(0xFF8BC34A), stringResource(R.string.detail_rating_tvy7))
                        RatingLegendItem("TV-G", Color(0xFF66BB6A), stringResource(R.string.detail_rating_tvg))
                        RatingLegendItem("TV-PG", Color(0xFFFF9800), stringResource(R.string.detail_rating_tvpg))
                        RatingLegendItem("TV-14", Color(0xFFF44336), stringResource(R.string.detail_rating_tv14))
                        RatingLegendItem("TV-MA", Color(0xFFD32F2F), stringResource(R.string.detail_rating_tvma))
                    }
                                     }

                    // Close Button (Fixed)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showRatingInfoDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Text(stringResource(R.string.settings_close), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    // Translation Prompt Dialog
    var rememberedPromptData by remember { mutableStateOf<Pair<Long, String>?>(null) }
    LaunchedEffect(showTranslationPrompt) {
        if (showTranslationPrompt != null) {
            rememberedPromptData = showTranslationPrompt
        }
    }

    com.cinetrack.ui.components.shared.FlickTroveModal(
        isVisible = showTranslationPrompt != null,
        onDismissRequest = { viewModel.dismissTranslationPrompt() },
        hazeState = localHazeState
    ) {
        val promptData = showTranslationPrompt ?: rememberedPromptData
        if (promptData != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                    contentDescription = stringResource(R.string.settings_close),
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier
                        .size(18.dp)
                        .bounceClick(scaleDown = 0.9f) { viewModel.dismissTranslationPrompt() }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_traduzione),
                contentDescription = null,
                tint = globalAccentColor,
                modifier = Modifier.size(48.dp).padding(bottom = 16.dp)
            )
            Text(
                stringResource(R.string.detail_translate_comment),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.detail_translate_download_prompt),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.translateComment(promptData.first, promptData.second, requireWifi = false)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = globalAccentColor.copy(alpha = 0.2f),
                        contentColor = globalAccentColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.detail_translate_yes_cellular), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.translateComment(promptData.first, promptData.second, requireWifi = true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        contentColor = Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.detail_translate_no_wifi))
                }

                TextButton(
                    onClick = { viewModel.dismissTranslationPrompt() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.detail_translate_not_now), color = Color.White.copy(alpha = 0.65f))
                }
            }
        }
    }
}


@Composable
private fun RatingLegendItem(cert: String, color: Color, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(55.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cert,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
    }
}
