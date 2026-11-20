package com.cinetrack.ui.components.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.screens.CollectionDetailScreen
import com.cinetrack.ui.screens.CommsUniCommentsScreen
import com.cinetrack.ui.screens.MovieDetailScreen
import com.cinetrack.ui.screens.PersonDetailScreen
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.theme.PremiumBackground
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.viewmodel.DetailEvent
import com.cinetrack.ui.viewmodel.DetailUiState
import com.cinetrack.ui.viewmodel.MovieDetailViewModel
import com.cinetrack.ui.viewmodel.SettingsViewModel
import com.cinetrack.ui.viewmodel.WatchState
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.WatchProviderDeepLinkUtils
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.toComposeColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MovieDetailScreenContent(
    viewModel: MovieDetailViewModel,
    movieId: Long,
    mediaType: String,
    openComments: Boolean = false,
    targetCommentId: String? = null,
    targetSeasonNumber: Int? = null,
    targetEpisodeNumber: Int? = null,
    preloadedTitle: String? = null,
    preloadedPosterPath: String? = null,
    preloadedBackdropPath: String? = null,
    preloadedLogoPath: String? = null,
    preloadedAccentColor: String? = null,
    settingsViewModel: SettingsViewModel,
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
    onHomeClick: () -> Unit = {},
    initialHasHandledComments: Boolean = false,
    onCommentsHandled: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.currentOrThrow
    val folders = (uiState as? DetailUiState.Success)?.folders ?: emptyList()
    val context = androidx.compose.ui.platform.LocalContext.current
    val extractedColor by viewModel.extractedColor.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    var isHeaderVisible by remember { mutableStateOf(true) }
    var showEpisodesSheet by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showRatingInfoDialog by remember { mutableStateOf(false) }
    var showCoverSelectionSheet by remember { mutableStateOf(false) }
    var showCheckInModal by remember { mutableStateOf(false) }
    var showWatchHistorySheet by remember { mutableStateOf(false) }
    var isDetailCastSheetOpen by remember { mutableStateOf(false) }
    var showShareStoryModal by remember { mutableStateOf(false) }
    var previousWatchState by remember { mutableStateOf<WatchState?>(null) }
    val showTranslationPrompt by viewModel.showTranslationPrompt.collectAsStateWithLifecycle()
    val translationStates by viewModel.translationStates.collectAsStateWithLifecycle()
    val movieActions = com.cinetrack.ui.components.shared.LocalMovieActions.current
    val useMovieLogo by viewModel.useMovieLogo.collectAsStateWithLifecycle()
    val globalStats by viewModel.globalStats.collectAsStateWithLifecycle()
    val allMediaImages by viewModel.allMediaImages.collectAsStateWithLifecycle()
    val promptWatchDateOnDetail by settingsViewModel.promptWatchDateOnDetail.collectAsStateWithLifecycle()
    val autoOpenVibeOnWatch by settingsViewModel.autoOpenVibeOnWatch.collectAsStateWithLifecycle()
    val guestAuthManager = com.cinetrack.ui.utils.rememberGuestAuthManager()
    var showWatchDatePrompt by remember { mutableStateOf(false) }
    var isPreviewWatchPrompt by remember { mutableStateOf(false) }
    var showTrailerModal by remember { mutableStateOf(false) }
    var trailerTriggerBounds by remember { mutableStateOf<Rect?>(null) }

    var hasCompletedFirstEnter by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasCompletedFirstEnter = true
    }

    var isBackInProgress by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = !isBackInProgress) {
        if (movieActions.isAnyModalOpen) {
            movieActions.closeAll()
        } else if (showCheckInModal) {
            showCheckInModal = false
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
        } else if (showTrailerModal) {
            showTrailerModal = false
        } else {
            isBackInProgress = true
            onBackClick()
        }
    }
    
    val currentImageQuality = LocalImageQuality.current
    // Estrazione colore dinamica se accentColor è nullo nel database
    // Detect NONE/BOOKMARKED → WATCHED transition to trigger check-in modal if enabled in settings
    LaunchedEffect(uiState) {
        val currentWatchState = (uiState as? DetailUiState.Success)?.watchState
        if (currentWatchState == WatchState.WATCHED && previousWatchState != null && previousWatchState != WatchState.WATCHED) {
            if (autoOpenVibeOnWatch) {
                showCheckInModal = true
            }
        }
        if (currentWatchState != null) {
            previousWatchState = currentWatchState
        }
    }

    LaunchedEffect(uiState, currentImageQuality) {
        val currentState = uiState
        if (currentState is DetailUiState.Success) {
            val movie = currentState.movieEntry
            val targetPath = movie.customBackdropPath ?: movie.backdropPath ?: movie.posterPath
            val imageType = if (movie.customBackdropPath != null || movie.backdropPath != null) ImageType.BACKDROP else ImageType.POSTER
            val imageUrl = buildTmdbImageUrl(targetPath, imageType, currentImageQuality)
            if (imageUrl != null) {
                // Determine target aspect ratio (ScreenWidth / 480dp)
                val configuration = context.resources.configuration
                val screenWidthDp = configuration.screenWidthDp.toFloat()
                val targetAspectRatio = screenWidthDp / 480f
                viewModel.fetchAccentColor(imageUrl, movie, targetAspectRatio = targetAspectRatio)
            }
        }
    }

    // Handle openComments auto-navigation
    var hasHandledOpenComments by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(initialHasHandledComments)
    }

    LaunchedEffect(hasCompletedFirstEnter, uiState) {
        if (openComments && !hasHandledOpenComments && !initialHasHandledComments && hasCompletedFirstEnter && uiState is DetailUiState.Success) {
            hasHandledOpenComments = true
            onCommentsHandled()
            val state = uiState as DetailUiState.Success
            val globalAccentColor = settingsViewModel.accentColor.value.toComposeColor()
            navigateToCommsUniComments(
                navigator = navigator,
                viewModel = viewModel,
                state = state,
                globalAccentColor = globalAccentColor,
                focusInput = false,
                targetCommentId = targetCommentId,
                targetSeasonNumber = targetSeasonNumber,
                targetEpisodeNumber = targetEpisodeNumber
            )
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

    val rawAccentColor = extractedColor
        ?: preloadedAccentColor?.toComposeColor()
        ?: (uiState as? DetailUiState.Success)?.movieEntry?.accentColor?.toComposeColor()
        ?: fallbackAccentColor
    val globalAccentColor = remember(rawAccentColor) {
        ColorUtils.ensureVividAccent(rawAccentColor)
    }

    val baseDarkColor = remember { com.cinetrack.ui.utils.ColorUtils.BaseDarkSlate }
    val targetBackgroundColor = when (uiState) {
        is DetailUiState.Success -> com.cinetrack.ui.utils.ColorUtils.calculateCarouselBackgroundColor(globalAccentColor)
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
    var pendingExternalUrl by remember { mutableStateOf<String?>(null) }
    var pendingExternalHost by remember { mutableStateOf("") }

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
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(rootHazeState, style = HazeStyles.PremiumDark)
                        .background(PremiumBackground)
                        .background(animatedBgColor)
                ) {
        
        AnimatedContent(
            targetState = when (uiState) {
                is DetailUiState.Loading -> 0
                is DetailUiState.Success -> 1
                is DetailUiState.Error -> 2
            },
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))
                    )
            },
            label = "DetailUiStateTransition"
        ) { targetIndex ->
            when (targetIndex) {
                0 -> {
                    DetailSkeleton(
                        hazeState = null,
                        paddingValues = paddingValues,
                        isTv = mediaType == "tv",
                        backgroundColor = animatedBgColor
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
                        
                        // Use extractedColor or preloadedAccentColor before stale DB accentColor
                        val rawTargetAccentColor = extractedColor
                            ?: preloadedAccentColor?.toComposeColor()
                            ?: activeMovie.accentColor?.toComposeColor()
                            ?: fallbackAccentColor
                        
                        val targetAccentColor = remember(rawTargetAccentColor) {
                            ColorUtils.ensureVividAccent(rawTargetAccentColor)
                        }
                        val accentColor by animateColorAsState(
                            targetValue = targetAccentColor,
                            animationSpec = tween(800),
                            label = "AccentColorAnimation"
                        )

                        var startCascade by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            startCascade = true
                        }

                        val cascadeBackdropAlpha by animateFloatAsState(
                            targetValue = if (startCascade) 1f else 0f,
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                            label = "cascadeBackdropAlpha"
                        )
                        val cascadeHeaderAlpha by animateFloatAsState(
                            targetValue = if (startCascade) 1f else 0f,
                            animationSpec = tween(durationMillis = 280, delayMillis = 40, easing = FastOutSlowInEasing),
                            label = "cascadeHeaderAlpha"
                        )
                        val cascadeHeaderOffsetY by animateFloatAsState(
                            targetValue = if (startCascade) 0f else 18f,
                            animationSpec = tween(durationMillis = 280, delayMillis = 40, easing = FastOutSlowInEasing),
                            label = "cascadeHeaderOffsetY"
                        )
                        val cascadeMetaAlpha by animateFloatAsState(
                            targetValue = if (startCascade) 1f else 0f,
                            animationSpec = tween(durationMillis = 280, delayMillis = 100, easing = FastOutSlowInEasing),
                            label = "cascadeMetaAlpha"
                        )
                        val cascadeMetaOffsetY by animateFloatAsState(
                            targetValue = if (startCascade) 0f else 18f,
                            animationSpec = tween(durationMillis = 280, delayMillis = 100, easing = FastOutSlowInEasing),
                            label = "cascadeMetaOffsetY"
                        )
                        val cascadeSectionsAlpha by animateFloatAsState(
                            targetValue = if (startCascade) 1f else 0f,
                            animationSpec = tween(durationMillis = 300, delayMillis = 160, easing = FastOutSlowInEasing),
                            label = "cascadeSectionsAlpha"
                        )
                        val cascadeSectionsOffsetY by animateFloatAsState(
                            targetValue = if (startCascade) 0f else 22f,
                            animationSpec = tween(durationMillis = 300, delayMillis = 160, easing = FastOutSlowInEasing),
                            label = "cascadeSectionsOffsetY"
                        )
                        val cascadeFabAlpha by animateFloatAsState(
                            targetValue = if (startCascade) 1f else 0f,
                            animationSpec = tween(durationMillis = 320, delayMillis = 220, easing = FastOutSlowInEasing),
                            label = "cascadeFabAlpha"
                        )
                        val cascadeFabOffsetY by animateFloatAsState(
                            targetValue = if (startCascade) 0f else 28f,
                            animationSpec = tween(durationMillis = 320, delayMillis = 220, easing = FastOutSlowInEasing),
                            label = "cascadeFabOffsetY"
                        )

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .haze(localHazeState, style = HazeStyles.PremiumDark)
                                    .background(animatedBgColor)
                                    .verticalScroll(scrollState)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .layout { measurable, constraints ->
                                            val placeable = measurable.measure(constraints)
                                            layout(placeable.width, 520.dp.roundToPx()) {
                                                placeable.place(0, 0)
                                            }
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .haze(backdropHazeState)
                                            .background(animatedBgColor)
                                    ) {
                                        DetailBackdrop(
                                            scrollState = scrollState,
                                            backdropPath = activeMovie.customBackdropPath ?: activeMovie.backdropPath,
                                            posterPath = activeMovie.posterPath,
                                            accentColor = accentColor,
                                            backgroundColor = animatedBgColor,
                                            isOffline = isOffline,
                                            modifier = Modifier
                                                .graphicsLayer { alpha = cascadeBackdropAlpha }
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onLongPress = {
                                                             showCoverSelectionSheet = true
                                                        }
                                                    )
                                                }
                                        )
                                        // Estensione di sfondo continua dietro DetailHeader per prevenire il taglio netto
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(260.dp)
                                                .background(animatedBgColor)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .offset(y = (-146).dp)
                                        .padding(bottom = paddingValues.calculateBottomPadding() + 60.dp)
                                 ) {
                                    val effectiveVideos = remember(state.videos, state.trailers) {
                                        if (state.videos.isNotEmpty()) {
                                            state.videos.filter { it.site == "YouTube" || it.site == "Vimeo" }
                                        } else {
                                            state.trailers.map { com.cinetrack.data.api.Video(key = it, site = "YouTube", type = "Trailer", name = "Trailer") }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier.graphicsLayer {
                                            alpha = cascadeHeaderAlpha
                                            translationY = cascadeHeaderOffsetY.dp.toPx()
                                        }
                                    ) {
                                        DetailHeader(
                                            movie = activeMovie,
                                            movieDetails = state.details,
                                            ratings = state.externalRatings,
                                            accentColor = accentColor,
                                            matchPercentage = state.matchPercentage,
                                            logoPath = if (useMovieLogo) {
                                                val currentLang = java.util.Locale.getDefault().language
                                                val logos = state.details.images?.logos
                                                val bestLogo = logos?.firstOrNull { it.iso6391 == currentLang } 
                                                    ?: logos?.firstOrNull { it.iso6391 == "en" } 
                                                    ?: logos?.firstOrNull()
                                                bestLogo?.filePath ?: preloadedLogoPath
                                            } else null,
                                            hazeState = backdropHazeState,
                                            onRatingClick = { showRatingInfoDialog = true },
                                            hasAlternativeCovers = activeMovie.customBackdropPath != null || (state.details.images?.backdrops?.size ?: 0) > 1,
                                            onCoverSelectClick = { showCoverSelectionSheet = true },
                                            onTrailerClick = if (effectiveVideos.isNotEmpty()) {
                                                { bounds ->
                                                    trailerTriggerBounds = bounds
                                                    showTrailerModal = true
                                                }
                                            } else null,
                                            isOffline = isOffline,
                                            movieStatus = state.details.status ?: activeMovie.status,
                                            scrollState = scrollState
                                        )
                                    }

                                    Box(
                                        modifier = Modifier.graphicsLayer {
                                            alpha = cascadeMetaAlpha
                                            translationY = cascadeMetaOffsetY.dp.toPx()
                                        }
                                    ) {
                                        DetailMetaRows(
                                            genres = state.movieEntry.genres ?: emptyList(),
                                            keywords = state.details.keywords?.getList() ?: emptyList(),
                                            streaming = state.streamingProviders,
                                            buyAndRent = state.buyRentProviders,
                                            accentColor = accentColor,
                                            isInTheaters = state.isInTheaters,
                                            onGenreClick = { genre, offset -> onGenreClick(genre.id, genre.name, offset) },
                                            onKeywordClick = { id, name, offset -> onKeywordClick(id, name, offset) },
                                            onProviderClick = { provider ->
                                                val deepLink = WatchProviderDeepLinkUtils.resolveProviderSearchUrl(
                                                    providerName = provider.providerName,
                                                    mediaTitle = state.movieEntry.displayName,
                                                    region = state.watchRegion,
                                                    fallbackUrl = state.watchProviderLink
                                                )
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
                                    }

                                    Column(
                                        modifier = Modifier.graphicsLayer {
                                            alpha = cascadeSectionsAlpha
                                            translationY = cascadeSectionsOffsetY.dp.toPx()
                                        }
                                    ) {
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
                                        globalStats = globalStats,
                                        logoPath = remember(state.details.images?.logos, preloadedLogoPath) {
                                            val currentLang = java.util.Locale.getDefault().language
                                            val logos = state.details.images?.logos
                                            val bestLogo = logos?.firstOrNull { it.iso6391 == currentLang } 
                                                ?: logos?.firstOrNull { it.iso6391 == "en" } 
                                                ?: logos?.firstOrNull()
                                            bestLogo?.filePath ?: preloadedLogoPath
                                        },
                                        initialImages = state.details.images,
                                        allMediaImages = allMediaImages,
                                        onLoadAllImages = { viewModel.loadAllMediaImages() },
                                        showShareStoryModal = showShareStoryModal,
                                        onShowShareStoryModalChange = { showShareStoryModal = it },
                                        onRate = { viewModel.onEvent(DetailEvent.Rate(it)) },
                                        onNoteUpdate = { viewModel.onEvent(DetailEvent.UpdateNote(it)) },
                                        onCheckInClick = { 
                                            showCheckInModal = true 
                                        },
                                        onHistoryClick = { showWatchHistorySheet = true },
                                        onShareToComments = { cardUri ->
                                            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true) {
                                                guestAuthManager?.triggerGuestAuthDialog()
                                            } else {
                                                navigateToCommsUniComments(
                                                    navigator = navigator,
                                                    viewModel = viewModel,
                                                    state = state,
                                                    globalAccentColor = globalAccentColor,
                                                    focusInput = true,
                                                    initialImageUri = cardUri.toString()
                                                )
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(40.dp))

                                    DetailComments(
                                        comments = state.appComments,
                                        accentColor = accentColor,
                                        isOffline = isOffline,
                                        onOpenThread = { focusInput ->
                                            if (focusInput && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true) {
                                                guestAuthManager?.triggerGuestAuthDialog()
                                            } else {
                                                navigateToCommsUniComments(
                                                    navigator = navigator,
                                                    viewModel = viewModel,
                                                    state = state,
                                                    globalAccentColor = globalAccentColor,
                                                    focusInput = focusInput
                                                )
                                            } 
                                        },
                                        onLikeClick = { commentId ->
                                            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true) {
                                                guestAuthManager?.triggerGuestAuthDialog()
                                            } else {
                                                viewModel.onEvent(DetailEvent.ToggleCommentLike(commentId))
                                            }
                                        },
                                        onOpenUrl = { rawUrl ->
                                            when (val check = com.cinetrack.util.LinkSecurityUtils.verifyUrl(rawUrl)) {
                                                is com.cinetrack.util.LinkSecurityCheck.Safe -> {
                                                    pendingExternalUrl = check.cleanUrl
                                                    pendingExternalHost = check.host
                                                }
                                                is com.cinetrack.util.LinkSecurityCheck.Blocked -> {
                                                    viewModel.emitMessage(com.cinetrack.ui.utils.UiText.StringResource(check.reasonResId))
                                                }
                                            }
                                        },
                                        onMediaTagClick = { mType, mId ->
                                            navigator.push(MovieDetailScreen(movieId = mId, mediaType = mType))
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(40.dp))

                                    DetailCast(
                                        crew = state.crew,
                                        cast = state.cast,
                                        accentColor = accentColor,
                                        globalStats = globalStats,
                                        hazeState = localHazeState,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onPersonClick = onPersonClick,
                                        onSheetStateChange = { isDetailCastSheetOpen = it }
                                    )

                                    Spacer(modifier = Modifier.height(40.dp))

                                    DetailRecommendations(
                                        collection = state.details.belongsToCollection,
                                        collectionMovies = state.collectionMovies,
                                        recommendedMovies = state.recommendations,
                                        currentId = activeMovie.id,
                                        accentColor = accentColor,
                                        backgroundColor = animatedBgColor,
                                        scrollState = scrollState,
                                        onMovieClick = onMovieClick,
                                        onLongPress = actionsState.onLongPress,
                                        onAction = { movie -> viewModel.onEvent(DetailEvent.CycleStatus(movie)) },
                                        onMessage = { viewModel.emitMessage(com.cinetrack.ui.utils.UiText.DynamicString(it)) },
                                        onCollectionClick = { id, name -> navigator.push(CollectionDetailScreen(id.toString(), name)) },
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }

                            DetailFloatingActionBar(
                                visible = !showEpisodesSheet && !isDetailCastSheetOpen && !showShareStoryModal,
                                movie = activeMovie,
                                movieStatus = state.details.status,
                                watchState = state.watchState,
                                progress = state.watchedProgress,
                                accentColor = accentColor,
                                hazeState = localHazeState,
                                bottomPadding = paddingValues.calculateBottomPadding(),
                                onStateChange = { newState ->
                                    if (newState == WatchState.WATCHED && promptWatchDateOnDetail && activeMovie.mediaType == "movie") {
                                        isPreviewWatchPrompt = false
                                        showWatchDatePrompt = true
                                    } else {
                                        viewModel.onEvent(DetailEvent.SetWatchState(newState))
                                    }
                                },
                                onRemove = { viewModel.onEvent(DetailEvent.DeleteMovie) },
                                onEpisodesClick = { showEpisodesSheet = true },
                                onManageRewatches = { showWatchHistorySheet = true },
                                onPreviewWatch = {
                                    isPreviewWatchPrompt = true
                                    showWatchDatePrompt = true
                                },
                                onToggleRemindersMuted = {
                                    viewModel.onEvent(DetailEvent.ToggleRemindersMuted)
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .graphicsLayer {
                                        alpha = cascadeFabAlpha
                                        translationY = cascadeFabOffsetY.dp.toPx()
                                    }
                            )
                        }
                    }
                }
            }
        }
        } // End of haze Box


        // Morphing Top Bar (pill → folder picker modal)
        val successState = uiState as? DetailUiState.Success
        val topBarAlpha by animateFloatAsState(
            targetValue = if (showShareStoryModal) 0f else 1f,
            animationSpec = tween(durationMillis = 200),
            label = "TopBarAlphaStoryModal"
        )
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
            onHomeClick = onHomeClick,
            modifier = Modifier.graphicsLayer { alpha = topBarAlpha }
        )

        } // end Box (fillMaxSize background)
    } // end MovieActionsWrapper

    MovieDetailModalsHost(
        cachedSuccess = cachedSuccess,
        viewModel = viewModel,
        navigator = navigator,
        localHazeState = localHazeState,
        rootHazeState = rootHazeState,
        globalAccentColor = globalAccentColor,
        globalStats = globalStats,
        currentImageQuality = currentImageQuality,
        showEpisodesSheet = showEpisodesSheet,
        onDismissEpisodesSheet = { showEpisodesSheet = false },
        showWatchHistorySheet = showWatchHistorySheet,
        onDismissWatchHistorySheet = { showWatchHistorySheet = false },
        showWatchDatePrompt = showWatchDatePrompt,
        onDismissWatchDatePrompt = {
            showWatchDatePrompt = false
            if (!isPreviewWatchPrompt) {
                viewModel.onEvent(DetailEvent.SetWatchState(WatchState.WATCHED, null))
            }
            isPreviewWatchPrompt = false
        },
        onDateSelected = { customDate ->
            showWatchDatePrompt = false
            isPreviewWatchPrompt = false
            viewModel.onEvent(DetailEvent.SetWatchState(WatchState.WATCHED, customDate))
        },
        showCheckInModal = showCheckInModal,
        onDismissCheckInModal = { showCheckInModal = false },
        pendingExternalUrl = pendingExternalUrl,
        pendingExternalHost = pendingExternalHost,
        onDismissExternalLink = { pendingExternalUrl = null },
        showRatingInfoDialog = showRatingInfoDialog,
        onDismissRatingInfoDialog = { showRatingInfoDialog = false },
        showTranslationPrompt = showTranslationPrompt,
        showCoverSelectionSheet = showCoverSelectionSheet,
        onDismissCoverSelectionSheet = { showCoverSelectionSheet = false },
        showTrailerModal = showTrailerModal,
        trailerTriggerBounds = trailerTriggerBounds,
        onDismissTrailerModal = { showTrailerModal = false }
    )
    } // end root Box
}

private fun navigateToCommsUniComments(
    navigator: Navigator,
    viewModel: MovieDetailViewModel,
    state: DetailUiState.Success,
    globalAccentColor: Color,
    focusInput: Boolean = false,
    targetCommentId: String? = null,
    targetSeasonNumber: Int? = null,
    targetEpisodeNumber: Int? = null,
    initialImageUri: String? = null
) {
    val mediaTitle = state.details.title ?: state.details.name ?: ""
    val image = buildTmdbImageUrl(state.details.posterPath ?: state.details.backdropPath, ImageType.POSTER, ImageQuality.HIGH)
    val backdrop = buildTmdbImageUrl(
        state.movieEntry.customBackdropPath ?: state.movieEntry.backdropPath ?: state.details.backdropPath ?: state.details.posterPath,
        if (state.movieEntry.customBackdropPath != null || state.movieEntry.backdropPath != null || state.details.backdropPath != null) ImageType.BACKDROP else ImageType.POSTER,
        ImageQuality.HIGH
    )
    val tvdbId = viewModel.tvdbId ?: state.details.externalIds?.tvdbId
    val imdbId = state.details.externalIds?.imdbId
    val releaseYear = (state.details.releaseDate ?: state.details.firstAirDate)?.take(4)
    navigator.push(
        CommsUniCommentsScreen(
            tvdbId = tvdbId,
            mediaId = viewModel.movieId.toString(),
            mediaType = viewModel.mediaType,
            seasonNumber = targetSeasonNumber,
            episodeNumber = targetEpisodeNumber,
            accentColorValue = globalAccentColor.value.toLong(),
            mediaTitle = mediaTitle,
            mediaImage = image,
            backdropUrl = backdrop,
            releaseYear = releaseYear,
            targetCommentId = targetCommentId,
            focusInputOnLaunch = focusInput,
            imdbId = imdbId,
            initialImageUri = initialImageUri
        )
    )
}
