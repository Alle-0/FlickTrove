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
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.data.model.Movie
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
import com.cinetrack.ui.components.dialog.EpisodesBottomSheet

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
    var showCoverSelectionSheet by remember { mutableStateOf(false) }
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
        } else if (showCoverSelectionSheet) {
            showCoverSelectionSheet = false
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
            val targetPath = movie.customBackdropPath ?: movie.backdropPath ?: movie.posterPath
            val imageType = if (movie.customBackdropPath != null || movie.backdropPath != null) ImageType.BACKDROP else ImageType.POSTER
            val imageUrl = buildTmdbImageUrl(targetPath, imageType, currentImageQuality)
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
    val rootHazeState = remember { HazeState() }
    val backdropHazeState = remember { HazeState() }
    
    val themePrimaryColor = MaterialTheme.colorScheme.primary
    val fallbackAccentColor = remember(themePrimaryColor) {
        themePrimaryColor
    }

    val rawAccentColor = when (val state = uiState) {
        is DetailUiState.Success -> state.movieEntry.accentColor.toComposeColor(extractedColor ?: fallbackAccentColor)
        else -> extractedColor ?: fallbackAccentColor
    }
    val globalAccentColor = remember(rawAccentColor) {
        ColorUtils.ensureVividAccent(rawAccentColor)
    }

    val baseDarkColor = remember { Color(0xFF161620) } // Balanced sleek dark slate
    val targetBackgroundColor = when (uiState) {
        is DetailUiState.Success -> lerp(globalAccentColor, baseDarkColor, 0.68f)
        else -> baseDarkColor
    }
    val animatedBgColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 800),
        label = "backgroundColor"
    )

    // Press states and icon scales are now handled inside DetailMorphingTopBar

    var cachedSuccess by remember { mutableStateOf<DetailUiState.Success?>(null) }
    if (uiState is DetailUiState.Success) {
        cachedSuccess = uiState as DetailUiState.Success
    }
    var trailersMenuData by remember { mutableStateOf<com.cinetrack.ui.components.detail.TrailersMenuModalData?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        MovieActionsWrapper(
            hazeState = rootHazeState,
            folders = (uiState as? DetailUiState.Success)?.folders ?: emptyList(),
            isItemInFolder = { movie, folderId -> (uiState as? DetailUiState.Success)?.folders?.find { it.id == folderId }?.itemIds?.contains("${movie.mediaType}_${movie.id}") == true },
            onUpdateRating = { movie, rating -> viewModel.onEvent(DetailEvent.RateMovie(movie, rating)) },
            onUpdateNote = { movie, note -> viewModel.onEvent(DetailEvent.UpdateMovieNote(movie, note)) },
            onToggleFolder = { movie, folder -> viewModel.onEvent(DetailEvent.ToggleMovieFolderMembership(movie, folder)) },
            onDelete = { movie -> viewModel.onEvent(DetailEvent.DeleteMovieItem(movie)) }
        ) { actionsState ->
            Box(modifier = Modifier.fillMaxSize().background(PremiumBackground).background(animatedBgColor).haze(rootHazeState, style = HazeStyles.PremiumDark)) {
        
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
                        hazeState = null,
                        paddingValues = paddingValues
                    )
                }
                2 -> {
                    val errorState = uiState as? DetailUiState.Error
                    DetailErrorState(
                        errorMessage = errorState?.message,
                        onRetry = { viewModel.onEvent(DetailEvent.Refresh) },
                        onBackClick = onBackClick
                    )
                }
                1 -> {
                    val state = cachedSuccess
                    if (state != null) {
                        val activeMovie = state.movieEntry
                        val rawTargetAccentColor = activeMovie.accentColor.toComposeColor(extractedColor ?: fallbackAccentColor)
                        val targetAccentColor = remember(rawTargetAccentColor) {
                            ColorUtils.ensureVividAccent(rawTargetAccentColor)
                        }
                        val accentColor by animateColorAsState(
                            targetValue = targetAccentColor,
                            animationSpec = tween(800),
                            label = "AccentColorAnimation"
                        )

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .haze(localHazeState, style = HazeStyles.PremiumDark)
                                    .verticalScroll(scrollState, enabled = trailersMenuData == null)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    DetailBackdrop(
                                        backdropPath = activeMovie.customBackdropPath ?: activeMovie.backdropPath,
                                        posterPath = activeMovie.posterPath,
                                        accentColor = accentColor,
                                        backgroundColor = animatedBgColor,
                                        modifier = Modifier
                                            .haze(backdropHazeState, style = HazeStyles.PremiumDark)
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        showCoverSelectionSheet = true
                                                    }
                                                )
                                            }
                                    )
                                }

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
                                        onRatingClick = { showRatingInfoDialog = true },
                                        hasAlternativeCovers = activeMovie.customBackdropPath != null || (state.details?.images?.backdrops?.size ?: 0) > 1,
                                        onCoverSelectClick = { showCoverSelectionSheet = true }
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

                                    Spacer(modifier = Modifier.height(24.dp))

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

                                    if (state.videos.isNotEmpty() || state.trailers.isNotEmpty()) {
                                        DetailTrailers(
                                            videos = state.videos,
                                            trailers = state.trailers,
                                            accentColor = accentColor,
                                            hazeState = localHazeState,
                                            isMenuOpen = trailersMenuData != null,
                                            onMenuOpenChange = { if (!it) trailersMenuData = null },
                                            onOpenCategoryMenu = { data ->
                                                trailersMenuData = data
                                            }
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


        // Morphing Top Bar (pill → folder picker modal)
        val successState = uiState as? DetailUiState.Success
        DetailMorphingTopBar(
            successState = successState,
            viewModel = viewModel,
            localHazeState = localHazeState,
            symbioteProgress = symbioteProgress,
            detailStackDepth = detailStackDepth,
            currentImageQuality = currentImageQuality,
            showFolderPicker = showFolderPicker,
            onFolderPickerChange = { showFolderPicker = it },
            onBackClick = onBackClick,
            onHomeClick = onHomeClick
        )
        } // end Box (fillMaxSize background)
    } // end MovieActionsWrapper


    // Ratings Info Dialog
    DetailRatingInfoDialog(
        visible = showRatingInfoDialog,
        onDismiss = { showRatingInfoDialog = false },
        hazeState = rootHazeState
    )
    // Translation Prompt Dialog
    DetailTranslationPromptModal(
        showTranslationPrompt = showTranslationPrompt,
        onDismiss = { viewModel.dismissTranslationPrompt() },
        onTranslate = { commentId, text, requireWifi ->
            viewModel.translateComment(commentId, text, requireWifi)
        },
        hazeState = rootHazeState,
        accentColor = globalAccentColor
    )

    DetailCoverSelectionModal(
        isVisible = showCoverSelectionSheet && cachedSuccess != null,
        onDismiss = { showCoverSelectionSheet = false },
        movieEntry = cachedSuccess?.movieEntry,
        backdrops = cachedSuccess?.details?.images?.backdrops ?: emptyList(),
        currentImageQuality = currentImageQuality,
        accentColor = globalAccentColor,
        hazeState = rootHazeState,
        onSelectCover = { filePath ->
            viewModel.onEvent(DetailEvent.UpdateCustomCover(filePath))
        }
    )

    com.cinetrack.ui.components.detail.DetailTrailersMenuModal(
        data = trailersMenuData,
        accentColor = globalAccentColor,
        hazeState = rootHazeState,
        onDismiss = { trailersMenuData = null }
    )
    } // end root Box
}
