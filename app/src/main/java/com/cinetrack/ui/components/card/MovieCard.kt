package com.cinetrack.ui.components.card
import androidx.compose.ui.res.stringResource
import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cinetrack.ui.model.generateBadges
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import com.cinetrack.ui.components.shared.LocalMovieActions
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.model.getLocalizedText
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.*
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.util.toComposeColorOrNull
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.GenericShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze

// --- Constants & Shapes ---

private val FilmStripBookmarkShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    fillType = PathFillType.EvenOdd
    
    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h)
    lineTo(w / 2f, h * 0.9f)
    lineTo(0f, h)
    close()
    
    val holeW = w * 0.15f
    val holeH = h * 0.1f
    val margin = w * 0.1f
    val spacing = h * 0.22f
    
    var y = h * 0.2f
    while (y + holeH < h * 0.85f) {
        addRect(Rect(margin, y, margin + holeW, y + holeH))
        addRect(Rect(w - margin - holeW, y, w - margin, y + holeH))
        y += spacing
    }
}

// --- Helper Composables ---


@Composable
fun MovieCircularProgress(progress: Float, modifier: Modifier = Modifier) {
    val accentColor = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}

@Composable
fun MovieFolderBookmarks(folderColors: List<Color>) {
    if (folderColors.isEmpty()) return
    
    val indicatorBrush = if (folderColors.size > 1) {
        Brush.verticalGradient(folderColors)
    } else {
        Brush.linearGradient(listOf(folderColors.first(), folderColors.first()))
    }

    Box(
        modifier = Modifier
            .padding(start = 14.dp)
            .width(16.dp)
            .height(28.dp)
            .graphicsLayer {
                shadowElevation = 8.dp.toPx()
                shape = FilmStripBookmarkShape
                clip = true
            }
            .background(indicatorBrush)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = FilmStripBookmarkShape
            )
    )
}



// --- Main Component ---

@Composable
fun MovieCard(
    movie: Movie,
    cardWidth: Dp,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    isWatched: Boolean = false,
    isReminder: Boolean = false,
    progress: Float = 0f,
    personalRating: Double? = null,
    personalNote: String? = null,
    showActionHint: Boolean = true,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    folderColors: List<Color> = emptyList(),
    showFolderBookmarks: Boolean = true,
    showBadges: Boolean = true,
    showAdvancedBadges: Boolean = false,
    maxBadges: Int = if (cardWidth > 135.dp) 3 else 1,
    showActionButton: Boolean = true,
    bounceEnabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    hazeState: HazeState? = null,
    staggerIndex: Int = -1,
    hasAnimatedSet: MutableSet<String>? = null,
    onPress: (Movie) -> Unit = {},
    onLongPress: (Movie, Offset, Offset) -> Unit = { _, _, _ -> },
    onAction: (Movie) -> Unit = {},
    onMessage: (String) -> Unit = {}
) {
    val cardHeight = cardWidth * 1.5f
    val isTv = movie.mediaType == "tv"
    val nextInfo = remember(movie.seasons, movie.watchedEpisodes, movie.numberOfEpisodes, movie.status) {
        if (isTv) movie.calculateNextEpisode() else null
    }
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
    val cardSize = remember { arrayOf(Size.Zero) }

    val glowColor = remember(movie.accentColor) {
        movie.accentColor.toComposeColorOrNull() ?: Color.Transparent
    }

    val rippleState = rememberCardRippleState()

    // Premium Staggered Entrance Animation States
    val compositeId = "${movie.id}_${movie.mediaType}"
    val hasAnimated = rememberSaveable { mutableStateOf(hasAnimatedSet?.contains(compositeId) == true) }

    LaunchedEffect(movie.id) {
        if (hasAnimated.value) return@LaunchedEffect
        if (staggerIndex in 0..11) delay(staggerIndex * 40L)
        hasAnimated.value = true
        hasAnimatedSet?.add(compositeId)
    }

    val isScrollItem = staggerIndex < 0 || staggerIndex >= 12
    val isVisible = hasAnimated.value || staggerIndex < 0

    val isExploding = movieActions.explodingMovie?.id == movie.id
    val cardAlpha by animateFloatAsState(
        targetValue = if (isExploding) 0f else (if (isVisible) 1f else 0f),
        animationSpec = if (isExploding) snap() else tween(durationMillis = if (isScrollItem) 180 else 250, easing = LinearOutSlowInEasing),
        label = "alpha"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = if (isScrollItem) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    val cardTranslateY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 40f,
        animationSpec = if (isScrollItem) tween(200, easing = FastOutSlowInEasing) else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "translateY"
    )

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .then(modifier)
            .graphicsLayer {
                alpha = cardAlpha
                scaleX = cardScale
                scaleY = cardScale
                translationY = cardTranslateY * density.density
                clip = false
            }
            .onGloballyPositioned { coordinates ->
                cardPosition[0] = coordinates.positionInWindow()
                cardSize[0] = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .cardRipple(rippleState, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                .bounceClickWithOffset(
                    scaleDown = 0.93f,
                    animateOnPress = bounceEnabled,
                    requireUnconsumed = false,
                    onLongClick = { offset ->
                        movieActions.updatePopupCardSize(cardSize[0])
                        onLongPress(movie.apply {
                            this.favorite = isFavorite
                            this.watched = isWatched
                            this.reminder = isReminder
                            if (personalRating != null) this.personalRating = personalRating
                            if (!personalNote.isNullOrBlank()) this.personalNote = personalNote
                        }, offset, cardPosition[0])
                    }
                ) { offset -> 
                    onPress(movie) 
                },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (posterUrl != null) {
                    val imageRequest = remember(posterUrl) {
                        ImageRequest.Builder(context)
                            .data(posterUrl)
                            .crossfade(false)
                            .allowHardware(true)
                            .memoryCacheKey(posterUrl)
                            .diskCacheKey(posterUrl)
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
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isTv) ImageVector.vectorResource(id = R.drawable.ic_tv) else ImageVector.vectorResource(id = R.drawable.ic_ciak),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Subtle color tint at bottom from accent color
            if (glowColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.35f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    glowColor.copy(alpha = 0.20f)
                                )
                            )
                        )
                )
            }

            if (showFolderBookmarks) {
                Box(modifier = Modifier.align(Alignment.TopStart)) {
                    MovieFolderBookmarks(folderColors)
                }
            }

            // Dynamic Badges (Top Right)
            if (showBadges && maxBadges > 0) {
                val isLarge = cardWidth > 135.dp
                val disabledBadges = com.cinetrack.LocalDisabledBadges.current
                val effectiveShowAdvanced = showAdvancedBadges || isLarge
                val badges = remember(movie, effectiveShowAdvanced) { movie.generateBadges(effectiveShowAdvanced) }
                
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    badges.filter { it.text !in disabledBadges }.take(maxBadges).forEach { badge ->
                        MovieCardBadge(
                            text = badge.getLocalizedText(), 
                            color = Color(badge.colorValue), 
                            hazeState = hazeState, 
                            isLarge = isLarge
                        )
                    }
                }
            }

            val guiAlpha by if (animatedVisibilityScope != null) {
                animatedVisibilityScope.transition.animateFloat(
                    label = "guiAlpha",
                    transitionSpec = { tween(durationMillis = 300) }
                ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
            } else {
                remember { mutableStateOf(1f) }
            }

            Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = guiAlpha }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                val displayRating = personalRating ?: movie.personalRating
                val hasRating = displayRating != null && displayRating > 0.0
                val isLarge = cardWidth > 150.dp
                val multiplier = com.cinetrack.LocalTitleTextSizeMultiplier.current
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(
                            start = if (isLarge) 16.dp else 12.dp, 
                            end = if (!showActionButton) (if (isLarge) 16.dp else 12.dp) else (if (isLarge) 52.dp else 36.dp), 
                            bottom = if (hasRating) (if (isLarge) 13.dp else 9.dp) else (if (isLarge) 15.dp else 11.dp)
                        ),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = movie.title ?: movie.name ?: "",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = ((if (isLarge) 14.5f else 13f) * multiplier).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        lineHeight = ((if (isLarge) 17.5f else 16f) * multiplier).sp,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    if (hasRating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(((if (isLarge) 13.5f else 11.5f) * multiplier).dp)
                            )
                            Text(
                                text = String.format("%.1f", displayRating),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = ((if (isLarge) 13.5f else 11.5f) * multiplier).sp,
                                fontWeight = FontWeight.Black,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }
                }

                val hintReminder = stringResource(R.string.card_hint_manage_reminder)
                val hintEpisodes = stringResource(R.string.card_hint_manage_episodes)
                
                if (showActionButton) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = if (isLarge) 12.dp else 8.dp, bottom = if (isLarge) 12.dp else 8.dp)
                            .size(if (isLarge) 34.dp else 24.dp)
                            .bounceClick(
                                scaleDown = 0.85f,
                                onLongClick = {}, // Block long press menu from opening
                                vibrateOnLongClick = false,
                                onPress = null
                            ) {
                                // Trigger ripple on the parent card from the button center on release
                                val rippleX = with(density) { cardWidth.toPx() - 20.dp.toPx() }
                                val rippleY = with(density) { cardHeight.toPx() - 20.dp.toPx() }
                                rippleState.trigger(Offset(rippleX, rippleY))
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)

                                val isReleased = movie.isReleased
                                if (!isReleased) {
                                    if (!isWatched && !isFavorite && !isReminder) onAction(movie)
                                    else if (showActionHint) {
                                        onMessage(hintReminder)
                                    }
                                } else if (isTv) {
                                    if (!isWatched && !isFavorite && !isReminder) onAction(movie)
                                    else if (showActionHint) {
                                        onMessage(hintEpisodes)
                                    }
                                } else {
                                    // Normal behavior for released movies
                                    onAction(movie)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val isQuickWatch = isTv && !isWatched && nextInfo != null && !nextInfo.isUpToDateWithAirDate
                        val appAccent = MaterialTheme.colorScheme.primary
                        val isReleased = movie.isReleased
                        val isPromemoriaAttiva = !isReleased && (isReminder || isFavorite)
                        val isOcchio = !isQuickWatch && isReleased && (isReminder || isFavorite)

                        val showCircular = progressBarTarget == com.cinetrack.ui.components.settings.ProgressBarTarget.CIRCULAR_TOTAL ||
                                progressBarTarget == com.cinetrack.ui.components.settings.ProgressBarTarget.CIRCULAR_SEASON
                        val targetCircularProgress = when (progressBarTarget) {
                            com.cinetrack.ui.components.settings.ProgressBarTarget.CIRCULAR_TOTAL -> computedProgress
                            com.cinetrack.ui.components.settings.ProgressBarTarget.CIRCULAR_SEASON -> seasonProgress
                            else -> 0f
                        }
                        val animatedCircularProgress by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = targetCircularProgress,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                            ),
                            label = "movieCardCircularProgress"
                        )
                        val isHasCircularProgress = isTv && !isWatched && (showCircular || animatedCircularProgress > 0.005f)

                        val actionBg = when {
                            isQuickWatch -> appAccent.copy(alpha = 0.15f)
                            isWatched -> appAccent.copy(alpha = 0.1f) // Theme color for Watched
                            isPromemoriaAttiva -> appAccent.copy(alpha = 0.1f) // Theme color for Reminder
                            isOcchio -> HazeStyles.AccentYellow.copy(alpha = 0.1f) // Yellow for Eye/Favorite
                            else -> HazeStyles.GlassColor.copy(alpha = 0.60f)
                        }

                        val actionBorder = when {
                            isQuickWatch -> appAccent.copy(alpha = 0.45f)
                            isWatched -> appAccent
                            isPromemoriaAttiva -> appAccent
                            isOcchio -> HazeStyles.AccentYellow
                            else -> HazeStyles.GlassBorderColor
                        }.copy(alpha = 0.3f)

                        val actionTint = when {
                            isQuickWatch -> appAccent
                            isWatched -> appAccent
                            isPromemoriaAttiva -> appAccent
                            isOcchio -> HazeStyles.AccentYellow
                            else -> Color.White
                        }

                        if (isHasCircularProgress) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(if (isLarge) 2.dp else 1.5.dp)) {
                                val sw = 2.dp.toPx()
                                drawArc(
                                    color = Color.White.copy(alpha = 0.15f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw)
                                )
                                drawArc(
                                    color = appAccent,
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedCircularProgress.coerceIn(0f, 1f),
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }
                        }

                        val isBellOutlined = !isReleased && !(isReminder || isFavorite)
                        val bellRotation = remember { androidx.compose.animation.core.Animatable(0f) }
                        var bellHasInitialized by remember { mutableStateOf(false) }

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

                        val isTick = isWatched || isQuickWatch
                        val isPlus = !isWatched && isReleased && !isOcchio && !isQuickWatch
                        val iconSizeModifier = Modifier.size(
                            if (isLarge) {
                                if (isTick) 13.dp else if (isPlus) 16.dp else if (isBellOutlined) 20.dp else 21.dp
                            } else {
                                if (isTick) 10.dp else if (isPlus) 12.dp else if (isBellOutlined) 15.5.dp else 17.dp
                            }
                        )

                        val buttonModifier = if (isHasCircularProgress) {
                            Modifier
                                .size(if (isLarge) 26.dp else 18.dp)
                                .background(color = actionBg, shape = CircleShape)
                        } else {
                            Modifier
                                .size(if (isLarge) 30.dp else 22.dp)
                                .background(color = actionBg, shape = CircleShape)
                                .border(width = 1.dp, color = actionBorder, shape = CircleShape)
                        }

                        Box(
                            modifier = buttonModifier,
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isTick -> ImageVector.vectorResource(id = R.drawable.ic_tick_card)
                                    !isReleased -> if (isReminder || isFavorite) ImageVector.vectorResource(id = R.drawable.ic_bell_piena) else ImageVector.vectorResource(id = R.drawable.ic_bell)
                                    isOcchio -> ImageVector.vectorResource(id = R.drawable.ic_eye)
                                    else -> ImageVector.vectorResource(id = R.drawable.ic_plus)
                                },
                                contentDescription = "Action",
                                tint = actionTint,
                                modifier = iconSizeModifier.graphicsLayer {
                                    if (!isReleased && (isReminder || isFavorite)) {
                                        rotationZ = bellRotation.value
                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.2f)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun MovieCardBadge(text: String, color: Color, modifier: Modifier = Modifier, hazeState: HazeState? = null, isLarge: Boolean = false) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(50))
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(50))
            .border(0.5.dp, color.copy(alpha = 0.8f), RoundedCornerShape(50))
            .padding(horizontal = if (isLarge) 8.dp else 6.dp, vertical = if (isLarge) 4.dp else 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = if (isLarge) 9.5.sp else 7.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    }
}
