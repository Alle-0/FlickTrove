package com.cinetrack.ui.components

import androidx.compose.ui.res.stringResource
import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cinetrack.data.generateBadges
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.data.Movie
import com.cinetrack.data.getLocalizedText
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.*
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.glass.glassmorphic
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.GenericShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

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
fun MovieMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = Color.White.copy(alpha = 0.8f),
    isDestructive: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .bounceClick(scaleDown = 0.96f, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (isDestructive) Color(0xFFE57373).copy(alpha = 0.15f)
                    else iconColor.copy(alpha = 0.12f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) Color(0xFFE57373) else iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            color = if (isDestructive) Color(0xFFE57373) else Color.White.copy(alpha = 0.95f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.sp
        )
    }
}

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

@Composable
fun MovieActionsPopup(
    movie: Movie,
    showMenu: Boolean,
    onDismiss: () -> Unit,
    pressOffset: Offset,
    cardPosition: Offset,
    hazeState: HazeState,
    onQuickVote: (Movie) -> Unit,
    onQuickNote: (Movie) -> Unit,
    onFolders: (Movie) -> Unit,
    onShare: (Movie) -> Unit,
    onDelete: (Movie) -> Unit
) {
    // Use MutableTransitionState to precisely track the AnimatedVisibility lifecycle
    val transitionState = remember { 
        MutableTransitionState(false).apply { targetState = showMenu } 
    }
    
    // Sync the target state
    transitionState.targetState = showMenu

    // Only render when visible or animating
    if (transitionState.targetState || !transitionState.isIdle) {
        val density = LocalDensity.current
        val config = LocalConfiguration.current
        
        // Calculate the absolute touch position
        val touchX = cardPosition.x + pressOffset.x
        val touchY = cardPosition.y + pressOffset.y

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(9999f)
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = fadeIn(tween(200, easing = LinearOutSlowInEasing)) + 
                        expandVertically(expandFrom = Alignment.Top, animationSpec = tween(250, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(200, easing = FastOutLinearInEasing)) + 
                       shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(200, easing = FastOutLinearInEasing))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .bounceClick(scaleDown = 1f, onClick = onDismiss)
                    )

                    val menuWidth = 180.dp
                    val menuHeight = 260.dp
                    
                    val menuWidthPx = with(density) { menuWidth.toPx() }
                    val menuHeightPx = with(density) { menuHeight.toPx() }
                    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
                    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

                    val statusBarsHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val navBarsHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    
                    val topSafetyPx = with(density) { (statusBarsHeight + 180.dp).toPx() }
                    val bottomSafetyPx = with(density) { (navBarsHeight + 12.dp).toPx() }
                    val sideSafetyPx = with(density) { 16.dp.toPx() }

                    var xPos = touchX
                    var yPos = touchY

                    if (xPos + menuWidthPx > screenWidthPx - sideSafetyPx) {
                        xPos -= menuWidthPx
                    }
                    if (xPos < sideSafetyPx) xPos = sideSafetyPx

                    if (yPos + menuHeightPx > screenHeightPx - bottomSafetyPx) {
                        yPos -= menuHeightPx
                    }
                    
                    yPos = yPos.coerceIn(topSafetyPx, (screenHeightPx - bottomSafetyPx - menuHeightPx).coerceAtLeast(topSafetyPx))

                    Column(
                        modifier = Modifier
                            .offset { IntOffset(xPos.toInt(), yPos.toInt()) }
                            .width(170.dp)
                            .hazeGlass(
                                state = hazeState,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .bounceClick(scaleDown = 1f) { }
                            .padding(6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = movie.title ?: movie.name ?: "Senza titolo",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.5.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        MovieMenuItem(
                            text = stringResource(R.string.card_quick_rate),
                            icon = ImageVector.vectorResource(id = R.drawable.ic_star),
                            iconColor = HazeStyles.AccentYellow,
                            modifier = Modifier.animateEnterExit(
                                enter = slideInVertically(tween(300, delayMillis = 40)) { it / 2 } + fadeIn(tween(300, delayMillis = 40))
                            ),
                            onClick = { onDismiss(); onQuickVote(movie) }
                        )
                        MovieMenuItem(
                            text = stringResource(R.string.card_quick_note),
                            icon = ImageVector.vectorResource(id = R.drawable.ic_pencil),
                            iconColor = Color(0xFF60A5FA),
                            modifier = Modifier.animateEnterExit(
                                enter = slideInVertically(tween(300, delayMillis = 80)) { it / 2 } + fadeIn(tween(300, delayMillis = 80))
                            ),
                            onClick = { onDismiss(); onQuickNote(movie) }
                        )
                        MovieMenuItem(
                            text = stringResource(R.string.card_folders),
                            icon = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                            iconColor = Color(0xFF34D399),
                            modifier = Modifier.animateEnterExit(
                                enter = slideInVertically(tween(300, delayMillis = 120)) { it / 2 } + fadeIn(tween(300, delayMillis = 120))
                            ),
                            onClick = { onDismiss(); onFolders(movie) }
                        )
                        MovieMenuItem(
                            text = stringResource(R.string.card_share),
                            icon = ImageVector.vectorResource(id = R.drawable.ic_share),
                            iconColor = Color(0xFF818CF8),
                            modifier = Modifier.animateEnterExit(
                                enter = slideInVertically(tween(300, delayMillis = 160)) { it / 2 } + fadeIn(tween(300, delayMillis = 160))
                            ),
                            onClick = { onDismiss(); onShare(movie) }
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .animateEnterExit(
                                enter = fadeIn(tween(300, delayMillis = 200))
                            )
                            .background(Color.White.copy(alpha = 0.1f)))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        MovieMenuItem(
                            text = stringResource(R.string.card_delete),
                            icon = ImageVector.vectorResource(id = R.drawable.ic_trash),
                            iconColor = Color(0xFFE57373),
                            isDestructive = true,
                            modifier = Modifier.animateEnterExit(
                                enter = slideInVertically(tween(300, delayMillis = 240)) { it / 2 } + fadeIn(tween(300, delayMillis = 240))
                            ),
                            onClick = { onDismiss(); onDelete(movie) }
                        )
                    }
                }
            }
        }
    }
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
    showActionHint: Boolean = true,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    folderColors: List<Color> = emptyList(),
    showFolderBookmarks: Boolean = true,
    showBadges: Boolean = true,
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
    val posterUrl = buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, LocalImageQuality.current)
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val cardPosition = remember { arrayOf(Offset.Zero) }

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
    val isVisible = hasAnimated.value || isScrollItem

    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (isScrollItem) 180 else 250, easing = LinearOutSlowInEasing),
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

    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .then(modifier)
            .graphicsLayer {
                alpha = cardAlpha
                scaleX = cardScale
                scaleY = cardScale
                translationY = cardTranslateY * density.density
            }
            .onGloballyPositioned { coordinates ->
                cardPosition[0] = coordinates.positionInWindow()
            }
            .clip(RoundedCornerShape(28.dp))
            .cardRipple(rippleState, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            .bounceClickWithOffset(
                scaleDown = 0.93f, 
                requireUnconsumed = false,
                onLongClick = { offset ->
                    onLongPress(movie, offset, cardPosition[0])
                }
            ) { offset -> 
                onPress(movie) 
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (posterUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(posterUrl)
                            .crossfade(false)
                            .allowHardware(true)
                            .memoryCacheKey(posterUrl)
                            .diskCacheKey(posterUrl)
                            .build(),
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
                            imageVector = if (isTv) ImageVector.vectorResource(id = R.drawable.ic_tv) else ImageVector.vectorResource(id = R.drawable.ic_ciack),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            if (showFolderBookmarks) {
                Box(modifier = Modifier.align(Alignment.TopStart)) {
                    MovieFolderBookmarks(folderColors)
                }
            }

            // Dynamic Badges (Top Right)
            if (showBadges) {
                val isLarge = cardWidth > 150.dp
                val disabledBadges = com.cinetrack.LocalDisabledBadges.current
                val badges = remember(movie) { movie.generateBadges() }
                
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    badges.filter { it.text !in disabledBadges }.forEach { badge ->
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
                            end = if (isLarge) 52.dp else 36.dp, 
                            bottom = if (hasRating) (if (isLarge) 13.dp else 9.dp) else (if (isLarge) 15.dp else 11.dp)
                        ),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = movie.title ?: movie.name ?: "",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = ((if (isLarge) 16 else 13) * multiplier).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        lineHeight = ((if (isLarge) 19 else 16) * multiplier).sp,
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
                    if (isTv && !isWatched && progress > 0f) {
                        MovieCircularProgress(progress = progress)
                    }

                    val appAccent = MaterialTheme.colorScheme.primary
                    val isReleased = movie.isReleased
                    val isPromemoriaAttiva = !isReleased && (isReminder || isFavorite)
                    val isOcchio = isReleased && (isReminder || isFavorite)

                    val actionBg = when {
                        isWatched -> Color(0xFF10B981).copy(alpha = 0.1f) // Green for Watched
                        isPromemoriaAttiva -> appAccent.copy(alpha = 0.1f) // Theme color for Reminder
                        isOcchio -> HazeStyles.AccentYellow.copy(alpha = 0.1f) // Yellow for Eye/Favorite
                        else -> HazeStyles.GlassColor.copy(alpha = 0.60f)
                    }

                    val actionBorder = when {
                        isWatched -> Color(0xFF10B981)
                        isPromemoriaAttiva -> appAccent
                        isOcchio -> HazeStyles.AccentYellow
                        else -> HazeStyles.GlassBorderColor
                    }.copy(alpha = 0.3f)

                    val actionTint = when {
                        isWatched -> Color(0xFF10B981)
                        isPromemoriaAttiva -> appAccent
                        isOcchio -> HazeStyles.AccentYellow
                        else -> Color.White
                    }

                    Box(
                        modifier = Modifier
                            .size(if (isLarge) 30.dp else 22.dp)
                            .background(color = actionBg, shape = CircleShape)
                            .border(width = 1.dp, color = actionBorder, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
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
                        
                        val isTick = isWatched
                        val isPlus = !isWatched && isReleased && !isOcchio
                        val iconSizeModifier = Modifier.size(
                            if (isLarge) {
                                if (isTick) 13.dp else if (isPlus) 16.dp else if (isBellOutlined) 20.dp else 21.dp
                            } else {
                                if (isTick) 10.dp else if (isPlus) 12.dp else if (isBellOutlined) 15.5.dp else 17.dp
                            }
                        )
                        
                        Icon(
                            imageVector = when {
                                isWatched -> ImageVector.vectorResource(id = R.drawable.ic_tick_card)
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

@Composable
fun MovieCardBadge(text: String, color: Color, modifier: Modifier = Modifier, hazeState: HazeState? = null, isLarge: Boolean = false) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
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
