@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
package com.cinetrack.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
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

import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.Coil
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.theme.PremiumBackground
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.util.toComposeColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MovieDetailScreen(
    viewModel: MovieDetailViewModel,
    paddingValues: PaddingValues,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    hazeState: HazeState? = null,
    onBackClick: () -> Unit = {},
    onMovieClick: (Movie) -> Unit = {},
    onPersonClick: (Long) -> Unit = {},
    onGenreClick: (Long, String, Offset) -> Unit = { _, _, _ -> },
    onKeywordClick: (Long, String, Offset) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var extractedColor by remember { mutableStateOf<Color?>(null) }
    var showEpisodesSheet by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (showFolderPicker) {
            showFolderPicker = false
        } else {
            onBackClick()
        }
    }
    
    // Estrazione colore dinamica se accentColor è nullo nel database
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is DetailUiState.Success) {
            val movie = state.movieEntry
            val mediaType = movie.mediaType.ifEmpty { "movie" }
            val sharedBoundsKey = "media_${mediaType}_${movie.id}"
            val imageUrl = "https://image.tmdb.org/t/p/w92${movie.posterPath ?: movie.backdropPath}"
            if (movie.accentColor == null && (movie.posterPath != null || movie.backdropPath != null)) {
                val loader = Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable && drawable.bitmap.width > 0 && drawable.bitmap.height > 0) {
                        val bitmap = drawable.bitmap
                        val averageColor = ColorUtils.extractAverageColor(bitmap)
                        val brightColor = ColorUtils.ensureMinimumLuminance(averageColor, 0.5f)
                        extractedColor = ColorUtils.saturateColor(brightColor, 2.0f)
                    }
                }
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

    var showRatingInfoDialog by remember { mutableStateOf(false) }

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
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.White.copy(alpha = 0.3f)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Qualcosa è andato storto",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = errorState?.message ?: "Errore sconosciuto",
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
                            Text("RIPROVA", fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        }

                        TextButton(
                            onClick = onBackClick,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Torna indietro", color = Color.White.copy(alpha = 0.5f))
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
                                        hazeState = backdropHazeState,
                                        titleAlpha = 1f,
                                        titleTranslationY = 0f,
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
                                        onProviderClick = {
                                            state.watchProviderLink?.let { link ->
                                                try {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    viewModel.emitMessage("Impossibile aprire il link")
                                                }
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(40.dp))

                                    DetailInfo(
                                        overview = activeMovie.overview ?: "Nessuna trama disponibile.",
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

                                    DetailRecommendations(
                                        collection = state.details.belongsToCollection,
                                        collectionMovies = state.collectionMovies,
                                        recommendedMovies = state.recommendations,
                                        currentId = activeMovie.id,
                                        accentColor = accentColor,
                                        onMovieClick = onMovieClick,
                                        onLongPress = actionsState.onLongPress,
                                        onAction = { movie -> viewModel.onEvent(DetailEvent.CycleStatus(movie)) },
                                        onMessage = { viewModel.emitMessage(it) },
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
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back Button - ALWAYS functional
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
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(28.dp)
                                    .graphicsLayer {
                                        scaleX = backIconScale
                                        scaleY = backIconScale
                                    }
                            )
                        }

                        // Title Area: Swaps between Movie Title (on scroll) and Modal Title (on expansion)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
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
                                    text = "Gestisci Cartelle",
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
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = "Folder",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { showRatingInfoDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.85f)
                    .hazeGlass(
                        state = localHazeState,
                        alpha = 0.85f,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable(enabled = false) {}
            ) {
                val dialogScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .heightIn(max = 420.dp)
                        .premiumScrollbar(dialogScrollState)
                        .verticalScroll(dialogScrollState)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Legenda Rating",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    // Film (MPAA)
                    Text(
                        "🎬  Film (USA / MPAA)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RatingLegendItem("G", Color(0xFF4CAF50), "Per tutti (General Audiences).")
                        RatingLegendItem("PG", Color(0xFF8BC34A), "Si consiglia la presenza dei genitori.")
                        RatingLegendItem("PG-13", Color(0xFFFF9800), "Sconsigliato ai minori di 13 anni.")
                        RatingLegendItem("R", Color(0xFFF44336), "Vietato ai minori di 17 anni non accompagnati.")
                        RatingLegendItem("NC-17", Color(0xFFD32F2F), "Vietato ai minori di 18 anni.")
                        RatingLegendItem("NR", Color(0xFF9E9E9E), "Non classificato (Not Rated).")
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Serie TV (TV Parental Guidelines)
                    Text(
                        "📺  Serie TV (USA / TV Parental Guidelines)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RatingLegendItem("TV-Y", Color(0xFF4CAF50), "Per tutti i bambini.")
                        RatingLegendItem("TV-Y7", Color(0xFF8BC34A), "Per bambini dai 7 anni in su.")
                        RatingLegendItem("TV-G", Color(0xFF66BB6A), "Per tutto il pubblico.")
                        RatingLegendItem("TV-PG", Color(0xFFFF9800), "Supervisione genitoriale consigliata.")
                        RatingLegendItem("TV-14", Color(0xFFF44336), "Sconsigliato ai minori di 14 anni.")
                        RatingLegendItem("TV-MA", Color(0xFFD32F2F), "Solo per adulti (18+).")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showRatingInfoDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Text("Chiudi", fontWeight = FontWeight.Bold, color = Color.White)
                    }
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
