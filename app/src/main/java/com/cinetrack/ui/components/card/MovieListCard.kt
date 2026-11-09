package com.cinetrack.ui.components.card
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.cinetrack.ui.components.shared.LocalMovieActions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.NextEpisodeInfo
import com.cinetrack.ui.model.getLocalizedText
import com.cinetrack.ui.utils.bounceClickWithOffset
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.cardRipple
import com.cinetrack.ui.utils.rememberCardRippleState
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cinetrack.ui.model.generateBadges
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cinetrack.R

@Composable
fun MovieListCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    isWatched: Boolean = false,
    isReminder: Boolean = false,
    personalRating: Double? = null,
    personalNote: String? = null,
    progress: Float = 0f,
    folderColors: List<Color> = emptyList(),
    showFolderBookmarks: Boolean = true,
    showBadges: Boolean = true,
    showAdvancedBadges: Boolean = true,
    hazeState: HazeState? = null,
    staggerIndex: Int = -1,
    hasAnimatedSet: MutableSet<String>? = null,
    onPress: (Movie) -> Unit = {},
    onLongPress: (Movie, Offset, Offset) -> Unit = { _, _, _ -> },
    onAction: (Movie) -> Unit = {},
    onMessage: (String) -> Unit = {},
    onQuickMarkWatched: ((Movie) -> Unit)? = null,
    precalculatedNextInfo: NextEpisodeInfo? = null
) {
    val isTv = movie.mediaType == "tv"
    val isEffectiveWatched = isWatched || movie.watched
    val nextInfo = if (isEffectiveWatched) null else (precalculatedNextInfo ?: remember(movie.seasons, movie.watchedEpisodes, movie.numberOfEpisodes, movie.status) {
        if (isTv) movie.calculateNextEpisode() else null
    })
    val episodeInfoStyle = com.cinetrack.LocalEpisodeInfoStyle.current
    val progressBarTarget = com.cinetrack.LocalProgressBarTarget.current

    val computedProgress = remember(movie.watchedEpisodes, movie.numberOfEpisodes, progress) {
        if (progress > 0f) progress
        else if (isTv && !movie.watchedEpisodes.isNullOrEmpty() && (movie.numberOfEpisodes ?: 0) > 0) {
            val totalWatched = movie.watchedEpisodes!!.filter { it.key != "0" }.values.sumOf { it.size }
            (totalWatched.toFloat() / movie.numberOfEpisodes!!.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    val seasonProgress = remember(nextInfo, movie.seasons, movie.watchedEpisodes) {
        if (isTv && nextInfo != null) com.cinetrack.ui.components.settings.calculateSeasonProgress(nextInfo, movie) else 0f
    }
    val posterUrl = buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, LocalImageQuality.current)
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val movieActions = LocalMovieActions.current
    val cardPosition = remember { arrayOf(Offset.Zero) }
    val cardSize = remember { arrayOf(androidx.compose.ui.unit.IntSize.Zero) }

    val rippleState = rememberCardRippleState()

    val entranceAnim = rememberCardEntranceAnimation(
        itemId = "${movie.id}_${movie.mediaType}",
        staggerIndex = staggerIndex,
        hasAnimatedSet = hasAnimatedSet,
        isExploding = movieActions.explodingMovie?.id == movie.id
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .graphicsLayer {
                alpha = entranceAnim.alpha
                scaleX = entranceAnim.scale
                scaleY = entranceAnim.scale
                translationY = entranceAnim.translateY * density.density
            }
            .onGloballyPositioned { coordinates -> 
                cardPosition[0] = coordinates.positionInWindow()
                cardSize[0] = coordinates.size
            }
            .clip(RoundedCornerShape(24.dp))
            .bounceClickWithOffset(
                scaleDown = 0.95f, 
                requireUnconsumed = false,
                onLongClick = { offset -> 
                    movieActions.updatePopupCardSize(Size(cardSize[0].width.toFloat(), cardSize[0].height.toFloat()))
                    onLongPress(movie.apply {
                        this.favorite = isFavorite
                        this.watched = isWatched
                        this.reminder = isReminder
                        if (personalRating != null) this.personalRating = personalRating
                        if (!personalNote.isNullOrBlank()) this.personalNote = personalNote
                    }, offset, cardPosition[0]) 
                }
            ) { offset -> 
                rippleState.trigger(offset)
                onPress(movie) 
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .cardRipple(rippleState, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            val bgUrl = buildTmdbImageUrl(movie.backdropPath, ImageType.BACKDROP, LocalImageQuality.current) ?: posterUrl
            if (bgUrl != null) {
                val imageRequest = remember(bgUrl) {
                    ImageRequest.Builder(context)
                        .data(bgUrl)
                        .crossfade(false)
                        .allowHardware(true)
                        .memoryCacheKey(bgUrl)
                        .diskCacheKey(bgUrl)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = movie.title ?: movie.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = if (isTv) ImageVector.vectorResource(id = R.drawable.ic_tv) else ImageVector.vectorResource(id = R.drawable.ic_ciak),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(24.dp)
                            .bounceClick(scaleDown = 0.85f, onLongClick = {}) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onAction(movie)
                            },)
                }
            }

            // Cinematic horizontal scrim: strong contrast on text zone (left), extending past midpoint for long episode subtitles
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            0.00f to Color.Black.copy(alpha = 0.96f),
                            0.32f to Color.Black.copy(alpha = 0.88f),
                            0.52f to Color.Black.copy(alpha = 0.72f),
                            0.68f to Color.Black.copy(alpha = 0.35f),
                            0.82f to Color.Transparent,
                            0.92f to Color.Black.copy(alpha = 0.25f),
                            1.00f to Color.Black.copy(alpha = 0.55f)
                        )
                    )
            )

            // Bottom-up vertical gradient for strong metadata readability over light backdrops + top badge vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0.00f to Color.Black.copy(alpha = 0.35f),
                            0.20f to Color.Transparent,
                            0.45f to Color.Transparent,
                            0.70f to Color.Black.copy(alpha = 0.50f),
                            1.00f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
            )

            // Animated Folder Bookmarks (Top Left)
            val hasFolderBookmarks = showFolderBookmarks && folderColors.isNotEmpty()
            var lastFolderColors by remember(movie.id) { mutableStateOf(folderColors) }
            if (folderColors.isNotEmpty()) {
                lastFolderColors = folderColors
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = hasFolderBookmarks,
                enter = fadeIn(tween(250)) + slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut(tween(180)) + slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                MovieFolderBookmarks(lastFolderColors)
            }

            // Dynamic Badges (Top Right) with smooth animated entrance/exit
            val disabledBadges = com.cinetrack.LocalDisabledBadges.current
            val badges = remember(movie, showAdvancedBadges) { movie.generateBadges(showAdvancedBadges) }
            val visibleBadges = remember(badges, disabledBadges) {
                badges.filter { it.text !in disabledBadges }.take(3)
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showBadges && visibleBadges.isNotEmpty(),
                enter = fadeIn(tween(250)) + scaleIn(tween(250, easing = FastOutSlowInEasing), initialScale = 0.8f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200, easing = FastOutSlowInEasing), targetScale = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    visibleBadges.forEach { badge ->
                        MovieCardBadge(
                            text = badge.getLocalizedText(), 
                            color = Color(badge.colorValue), 
                            hazeState = hazeState, 
                            isLarge = false
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 56.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                val multiplier = com.cinetrack.LocalTitleTextSizeMultiplier.current
                Text(
                    text = movie.title ?: movie.name ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = (17 * multiplier).sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val releaseYear = movie.releaseYear?.toIntOrNull()?.toString() ?: movie.releaseDate?.take(4) ?: movie.firstAirDate?.take(4) ?: ""
                
                val displayRating = personalRating ?: movie.personalRating
                val hasRating = displayRating != null && displayRating > 0.0
                val vote = if (hasRating) String.format("%.1f", displayRating) else ""
                
                val contextLocale = LocalConfiguration.current.locales[0].language
                val genres = remember(movie.genreIds, movie.genreNamesString, contextLocale) {
                    if (!movie.genreIds.isNullOrEmpty()) {
                        movie.genreIds!!.mapNotNull { id ->
                            val defaultName = com.cinetrack.data.model.GenreConstants.ALL_GENRES.find { it.id == id }?.name ?: ""
                            val localized = com.cinetrack.data.model.GenreConstants.getLocalizedName(id, contextLocale, defaultName)
                            localized.takeIf { it.isNotEmpty() }
                        }.joinToString(", ")
                    } else {
                        movie.genreNamesString ?: movie.genres?.mapNotNull { it.name }?.joinToString(", ") ?: ""
                    }
                }

                val isReleased = movie.isReleased
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (releaseYear.isNotEmpty()) {
                        Text(
                            text = releaseYear,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (hasRating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size((12 * multiplier).dp)
                            )
                            Text(
                                text = vote,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, fontSize = (12.5f * multiplier).sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (isTv && !isEffectiveWatched && nextInfo != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val isUpToDate = isReleased && nextInfo.isUpToDateWithAirDate
                        val statusText = when {
                            !isReleased -> stringResource(R.string.lbl_coming_soon)
                            isUpToDate -> stringResource(R.string.lbl_up_to_date)
                            else -> nextInfo.episodeCode
                        }
                        val statusColor = if (isUpToDate) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp),
                            color = statusColor
                        )
                        if (isReleased && !isUpToDate) {
                            val epText = formatEpisodeInfo(
                                style = episodeInfoStyle,
                                nextInfo = nextInfo,
                                movie = movie,
                                seriesProgress = computedProgress
                            )
                            if (epText.isNotEmpty()) {
                                Text(
                                    text = epText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
                if (!genres.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = genres,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Action Button
            val appAccent = MaterialTheme.colorScheme.primary
            val isReleased = movie.isReleased

            var optimisticFavorite by remember(isFavorite) { mutableStateOf(isFavorite) }
            var optimisticWatched by remember(isEffectiveWatched) { mutableStateOf(isEffectiveWatched) }
            var optimisticReminder by remember(isReminder) { mutableStateOf(isReminder) }

            val currentEffectiveWatched = optimisticWatched
            val currentEffectiveFavorite = optimisticFavorite
            val currentEffectiveReminder = optimisticReminder

            val isQuickWatch = isTv && !currentEffectiveWatched && onQuickMarkWatched != null && nextInfo != null && !nextInfo.isUpToDateWithAirDate
            val isPromemoriaAttiva = !isReleased && !currentEffectiveWatched && (currentEffectiveReminder || currentEffectiveFavorite)
            val isOcchio = !isQuickWatch && isReleased && !currentEffectiveWatched && (currentEffectiveReminder || currentEffectiveFavorite)
            val isTick = isQuickWatch || currentEffectiveWatched
            val isPlus = !currentEffectiveWatched && isReleased && !isOcchio && !isQuickWatch
            val isBellOutlined = !isReleased && !currentEffectiveWatched && !(currentEffectiveReminder || currentEffectiveFavorite)

            val actionButtonScale = remember { Animatable(1f) }
            val actionIconScale = remember { Animatable(1f) }
            val actionScope = rememberCoroutineScope()

            val bellRotation = remember { Animatable(0f) }
            var bellHasInitialized by remember { mutableStateOf(false) }

            fun triggerActionAnimation() {
                actionScope.launch {
                    if (isPlus) {
                        // Shutter scale pop
                        actionIconScale.snapTo(0.70f)
                        actionIconScale.animateTo(
                            1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    } else if (isOcchio) {
                        // Iris expansion pop
                        actionIconScale.snapTo(0.65f)
                        actionIconScale.animateTo(
                            1.28f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        actionIconScale.animateTo(
                            1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    } else if (isTick) {
                        // Tick scale pop
                        actionIconScale.snapTo(0.65f)
                        actionIconScale.animateTo(
                            1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    } else if (!isReleased) {
                        // Bell ring wobble
                        bellRotation.animateTo(-25f, tween(60, easing = LinearEasing))
                        bellRotation.animateTo(20f, tween(100, easing = LinearEasing))
                        bellRotation.animateTo(-15f, tween(100, easing = LinearEasing))
                        bellRotation.animateTo(10f, tween(100, easing = LinearEasing))
                        bellRotation.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
                    }
                }
            }

            LaunchedEffect(isReminder, isFavorite) {
                if (bellHasInitialized && !isReleased && (isReminder || isFavorite)) {
                    bellRotation.animateTo(-25f, tween(60, easing = LinearEasing))
                    bellRotation.animateTo(20f, tween(100, easing = LinearEasing))
                    bellRotation.animateTo(-15f, tween(100, easing = LinearEasing))
                    bellRotation.animateTo(10f, tween(100, easing = LinearEasing))
                    bellRotation.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
                }
                bellHasInitialized = true
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 12.dp, end = 12.dp)
                    .size(34.dp)
                    .graphicsLayer {
                        scaleX = actionButtonScale.value
                        scaleY = actionButtonScale.value
                        clip = false
                    }
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        this.contentDescription = "Action"
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            actionScope.launch {
                                actionButtonScale.animateTo(
                                    0.80f,
                                    animationSpec = spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
                                )
                            }
                            val up = waitForUpOrCancellation()
                            if (up != null) {
                                up.consume()
                                actionScope.launch {
                                    actionButtonScale.animateTo(
                                        1.15f,
                                        animationSpec = spring(stiffness = 12000f, dampingRatio = Spring.DampingRatioNoBouncy)
                                    )
                                    actionButtonScale.animateTo(
                                        1.0f,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow,
                                            dampingRatio = Spring.DampingRatioMediumBouncy
                                        )
                                    )
                                }

                                // Trigger ripple on the parent card from the button center on release
                                val rippleX = if (cardSize[0].width > 0) {
                                    cardSize[0].width.toFloat() - with(density) { 29.dp.toPx() } // button size 34/2 + end padding 12 = 29
                                } else {
                                    with(density) { 300.dp.toPx() - 29.dp.toPx() }
                                }
                                
                                val rippleY = if (cardSize[0].height > 0) {
                                    cardSize[0].height.toFloat() - with(density) { 29.dp.toPx() } // bottom padding 12 + 17 = 29
                                } else {
                                    with(density) { 110.dp.toPx() - 29.dp.toPx() }
                                }
                                rippleState.trigger(Offset(rippleX, rippleY))
                                com.cinetrack.util.VibrationHelper.vibrateTick(context)
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)

                                if (!isReleased && !currentEffectiveWatched) {
                                    if (!currentEffectiveWatched && !currentEffectiveFavorite && !currentEffectiveReminder) {
                                        optimisticReminder = true
                                        onAction(movie)
                                    } else {
                                        onMessage(context.getString(R.string.card_hint_manage_reminder))
                                    }
                                } else if (isTv) {
                                    if (!currentEffectiveWatched && onQuickMarkWatched != null && nextInfo != null && !nextInfo.isUpToDateWithAirDate) {
                                        onQuickMarkWatched(movie)
                                    } else if (!currentEffectiveWatched && !currentEffectiveFavorite && !currentEffectiveReminder) {
                                        optimisticFavorite = true
                                        onAction(movie)
                                    } else {
                                        onMessage(context.getString(R.string.card_hint_manage_episodes))
                                    }
                                } else {
                                    // Normal behavior for released movies: Plus -> Eye -> Check
                                    if (!currentEffectiveWatched && !currentEffectiveFavorite && !currentEffectiveReminder) {
                                        optimisticFavorite = true
                                        optimisticWatched = false
                                        optimisticReminder = false
                                    } else if (!currentEffectiveWatched && (currentEffectiveFavorite || currentEffectiveReminder)) {
                                        optimisticWatched = true
                                        optimisticFavorite = false
                                        optimisticReminder = false
                                    }
                                    onAction(movie)
                                }
                                triggerActionAnimation()
                            } else {
                                // Cancelled
                                actionScope.launch {
                                    actionButtonScale.animateTo(
                                        1.0f,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow,
                                            dampingRatio = Spring.DampingRatioNoBouncy
                                        )
                                    )
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val circularBar = com.cinetrack.LocalCircularProgressBar.current
                val targetCircularProgress = when (circularBar) {
                    "SEASON" -> if (seasonProgress > 0f) seasonProgress else computedProgress
                    else -> computedProgress
                }
                val animatedCircularProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = targetCircularProgress,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    ),
                    label = "listCircularProgress"
                )
                val isHasCircularProgress = isQuickWatch || (isTv && !currentEffectiveWatched && (isOcchio || isPromemoriaAttiva || computedProgress > 0f))

                val actionBg = when {
                    isQuickWatch -> appAccent.copy(alpha = 0.15f)
                    currentEffectiveWatched -> appAccent.copy(alpha = 0.1f) // Theme color for Watched
                    isPromemoriaAttiva -> appAccent.copy(alpha = 0.1f) // Theme color for Reminder
                    isOcchio -> com.cinetrack.ui.theme.HazeStyles.AccentYellow.copy(alpha = 0.1f) // Yellow for Eye/Favorite
                    else -> com.cinetrack.ui.theme.HazeStyles.GlassColor.copy(alpha = 0.60f)
                }

                val actionBorder = when {
                    isQuickWatch -> appAccent.copy(alpha = 0.45f)
                    currentEffectiveWatched -> appAccent
                    isPromemoriaAttiva -> appAccent
                    isOcchio -> com.cinetrack.ui.theme.HazeStyles.AccentYellow
                    else -> com.cinetrack.ui.theme.HazeStyles.GlassBorderColor
                }.copy(alpha = 0.3f)

                val actionTint = when {
                    isQuickWatch -> appAccent
                    currentEffectiveWatched -> appAccent
                    isPromemoriaAttiva -> appAccent
                    isOcchio -> com.cinetrack.ui.theme.HazeStyles.AccentYellow
                    else -> Color.White
                }

                val iconSize = when {
                    isTick -> 13.dp
                    !isReleased -> if (isBellOutlined) 20.dp else 21.dp
                    isOcchio -> 18.dp
                    else -> 16.dp
                }

                val buttonSize = 30.dp
                val buttonModifier = Modifier
                    .size(buttonSize)
                    .background(color = actionBg, shape = CircleShape)
                    .then(
                        if (!isHasCircularProgress) Modifier.border(width = 1.dp, color = actionBorder, shape = CircleShape)
                        else Modifier
                    )

                Box(
                    modifier = buttonModifier,
                    contentAlignment = Alignment.Center
                ) {
                    if (isHasCircularProgress) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val sw = 2.dp.toPx()
                            val strokeOffset = sw / 2
                            val arcSize = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
                            drawArc(
                                color = Color.White.copy(alpha = 0.15f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset(strokeOffset, strokeOffset),
                                size = arcSize,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw)
                            )
                            if (animatedCircularProgress > 0f) {
                                drawArc(
                                    color = appAccent,
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedCircularProgress.coerceIn(0f, 1f),
                                    useCenter = false,
                                    topLeft = androidx.compose.ui.geometry.Offset(strokeOffset, strokeOffset),
                                    size = arcSize,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = when {
                            isTick -> ImageVector.vectorResource(id = R.drawable.ic_tick_card)
                            !isReleased -> if (isPromemoriaAttiva) ImageVector.vectorResource(id = R.drawable.ic_bell_piena) else ImageVector.vectorResource(id = R.drawable.ic_bell)
                            isOcchio -> ImageVector.vectorResource(id = R.drawable.ic_eye)
                            else -> ImageVector.vectorResource(id = R.drawable.ic_plus)
                        },
                        contentDescription = "Action",
                        tint = actionTint,
                        modifier = Modifier
                            .size(iconSize)
                            .graphicsLayer {
                                scaleX = actionIconScale.value
                                scaleY = actionIconScale.value
                                rotationZ = if (!isReleased && isPromemoriaAttiva) {
                                    bellRotation.value
                                } else {
                                    0f
                                }
                                if (!isReleased && isPromemoriaAttiva) {
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.2f)
                                }
                            }
                    )
                }
            }
        }
    }
}
