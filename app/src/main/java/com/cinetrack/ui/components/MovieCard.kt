package com.cinetrack.ui.components

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import com.cinetrack.ui.assets.CustomIcons
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
    iconColor: Color = Color.White.copy(alpha = 0.8f),
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
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
private fun MovieCircularProgress(progress: Float) {
    val accentColor = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(1.dp)) {
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
private fun MovieFolderBookmarks(folderColors: List<Color>) {
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
                enter = fadeIn(tween(200, easing = LinearOutSlowInEasing)),
                exit = fadeOut(tween(200, easing = FastOutLinearInEasing))
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
                            .widthIn(min = 170.dp, max = 190.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = Color.Black.copy(alpha = 0.4f),
                                spotColor = Color.Black.copy(alpha = 0.4f)
                            )
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
                            text = "Voto rapido",
                            icon = CustomIcons.PremiumStar,
                            iconColor = HazeStyles.AccentYellow,
                            onClick = { onDismiss(); onQuickVote(movie) }
                        )
                        MovieMenuItem(
                            text = "Nota rapida",
                            icon = CustomIcons.PremiumEditNote,
                            iconColor = Color(0xFF60A5FA),
                            onClick = { onDismiss(); onQuickNote(movie) }
                        )
                        MovieMenuItem(
                            text = "Cartelle",
                            icon = CustomIcons.PremiumFolder,
                            iconColor = Color(0xFF34D399),
                            onClick = { onDismiss(); onFolders(movie) }
                        )
                        MovieMenuItem(
                            text = "Condividi",
                            icon = CustomIcons.PremiumShare,
                            iconColor = Color(0xFF818CF8),
                            onClick = { onDismiss(); onShare(movie) }
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        MovieMenuItem(
                            text = "Elimina",
                            icon = CustomIcons.PremiumDelete,
                            iconColor = Color(0xFFE57373),
                            isDestructive = true,
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
    onPress: (Movie) -> Unit = {},
    onLongPress: (Movie, Offset, Offset) -> Unit = { _, _, _ -> },
    onAction: (Movie) -> Unit = {},
    onMessage: (String) -> Unit = {}
) {
    val cardHeight = cardWidth * 1.5f
    val isTv = movie.mediaType == "tv"
    val posterUrl = movie.posterPath?.let { "https://image.tmdb.org/t/p/w185$it" }
    val context = LocalContext.current
    var cardPosition by remember { mutableStateOf(Offset.Zero) }

    val rippleState = rememberCardRippleState()
    val density = LocalDensity.current

    // Premium Staggered Entrance Animation States
    val hasAnimated = rememberSaveable(key = "anim_${movie.id}") { mutableStateOf(false) }

    val cardAlpha = remember { Animatable(if (hasAnimated.value) 1f else 0f) }
    val cardScale = remember { Animatable(if (hasAnimated.value) 1f else 0.85f) }
    val cardTranslateY = remember { Animatable(if (hasAnimated.value) 0f else 40f) }

    LaunchedEffect(movie.id) {
        if (hasAnimated.value) return@LaunchedEffect

        // Stagger delay ONLY for the initial screen-full of items (indices 0 to 8)
        // Scrolled-in items (index >= 9) or single items (-1) animate instantly
        val delayMillis = if (staggerIndex in 0..11) {
            staggerIndex * 40L
        } else {
            0L
        }
        if (delayMillis > 0) {
            delay(delayMillis)
        }

        val isScrollItem = staggerIndex < 0 || staggerIndex >= 12

        if (isScrollItem) {
            // Lightweight fade+slide for items entering during scroll — no spring, no bounce
            launch {
                cardAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                cardTranslateY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                )
            }
            cardScale.snapTo(1f) // no scale animation while scrolling
        } else {
            // Richer entrance for the initial grid (indices 0–8): use a stiffer spring (no bounce)
            val jobAlpha = launch {
                cardAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing)
                )
            }
            val jobScale = launch {
                cardScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            val jobTranslate = launch {
                cardTranslateY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            jobAlpha.join()
            jobScale.join()
            jobTranslate.join()
        }
        hasAnimated.value = true
    }

    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .then(modifier)
            .graphicsLayer {
                alpha = cardAlpha.value
                scaleX = cardScale.value
                scaleY = cardScale.value
                translationY = cardTranslateY.value * density.density
            }
            .onGloballyPositioned { coordinates ->
                cardPosition = coordinates.positionInWindow()
            }
            .clip(RoundedCornerShape(28.dp))
            .cardRipple(rippleState, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            .bounceClickWithOffset(
                scaleDown = 0.93f, 
                requireUnconsumed = false,
                onLongClick = { offset ->
                    onLongPress(movie, offset, cardPosition)
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
                            .crossfade(true)
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
                            imageVector = if (isTv) Icons.Rounded.Tv else Icons.Rounded.Movie,
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
                val disabledBadges = com.cinetrack.LocalDisabledBadges.current
                val addBadge = @Composable { text: String, color: Color ->
                    if (!disabledBadges.contains(text)) {
                        MovieCardBadge(text = text, color = color, hazeState = hazeState)
                    }
                }
                
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val newEpisodes = movie.newEpisodesFound
                    if (movie.isUpcoming == true || (newEpisodes != null && newEpisodes > 0)) {
                        addBadge("NEW", com.cinetrack.ui.theme.NeonPink)
                    }
    
                    if ((movie.voteAverage ?: 0.0) >= 8.8 && (movie.voteCount ?: 0) > 2000) {
                        addBadge("MASTERPIECE", Color(0xFFFFD700))
                    } else if ((movie.voteAverage ?: 0.0) >= 8.5 && (movie.voteCount ?: 0) > 300) {
                        addBadge("BEST", Color(0xFF00E5FF))
                    } else if ((movie.voteCount ?: 0) > 3000) {
                        addBadge("HOT", com.cinetrack.ui.theme.HazeStyles.AccentYellow)
                    } else if ((movie.voteAverage ?: 0.0) >= 8.0 && (movie.voteCount ?: 0) > 1000) {
                        addBadge("WOW", com.cinetrack.ui.theme.NeonTeal)
                    } else if ((movie.voteAverage ?: 0.0) >= 7.5 && (movie.voteCount ?: 0) in 50..500) {
                        addBadge("HIDDEN GEM", Color(0xFF00E676))
                    }
                    
                    if ((movie.voteCount ?: 0) > 1000 && (movie.voteAverage ?: 0.0) >= 5.0 && (movie.voteAverage ?: 0.0) <= 6.5) {
                        addBadge("DIVISIVE", Color(0xFFFF9800))
                    }

                    if ((movie.revenue ?: 0L) > 500_000_000L) {
                        addBadge("BLOCKBUSTER", Color(0xFF6200EA))
                    } else if ((movie.budget ?: 0L) in 1L..5_000_000L && (movie.voteAverage ?: 0.0) >= 7.0) {
                        addBadge("INDIE", Color(0xFFAED581))
                    }
                    
                    val releaseYear = movie.releaseYear?.toIntOrNull() ?: 
                                      movie.releaseDate?.take(4)?.toIntOrNull() ?: 
                                      movie.firstAirDate?.take(4)?.toIntOrNull() ?: 9999
                    
                    if (releaseYear < 1970) {
                        addBadge("VINTAGE", Color(0xFFBCAAA4))
                    } else if (releaseYear < 1990 && (movie.voteAverage ?: 0.0) >= 7.0) {
                        addBadge("CLASSIC", Color(0xFF8D6E63))
                    } else if (releaseYear in 1990..2010 && (movie.voteAverage ?: 0.0) >= 8.0) {
                        addBadge("CULT", Color(0xFF9C27B0))
                    }
                    
                    if ((movie.runtime ?: 0) > 160) {
                        addBadge("EPIC", Color(0xFFFF5722))
                    } else if (movie.mediaType != "tv" && (movie.runtime ?: 0) in 1..89) {
                        addBadge("QUICK", Color(0xFFC6FF00))
                    }
                    
                    if ((movie.numberOfSeasons ?: 0) >= 5 || (movie.numberOfEpisodes ?: 0) > 50) {
                        addBadge("BINGE", Color(0xFF00BCD4))
                    } else if (movie.mediaType == "tv" && (movie.episodeRunTime?.firstOrNull() ?: 0) in 1..25) {
                        addBadge("SNACK", Color(0xFFC6FF00))
                    }
    
                    val genresStr = movie.genreNamesString ?: ""
                    val hasGenre = { name: String -> 
                        genresStr.contains(name, ignoreCase = true) || 
                        movie.genres?.any { it.name?.equals(name, ignoreCase = true) == true } == true
                    }
                    
                    if (hasGenre("Horror")) {
                        addBadge("HORROR", Color(0xFFE53935))
                    } else if (hasGenre("Animation") || hasGenre("Anime")) {
                        addBadge("ANIME", Color(0xFFFF9800))
                    } else if (hasGenre("Science Fiction") || hasGenre("Sci-Fi")) {
                        addBadge("SCI-FI", Color(0xFF2962FF))
                    } else if (hasGenre("Comedy") && (movie.voteAverage ?: 0.0) >= 7.0) {
                        addBadge("COMEDY", Color(0xFFFFEA00))
                    } else if (hasGenre("Documentary")) {
                        addBadge("DOCU", Color(0xFF9E9E9E))
                    } else if (hasGenre("Family")) {
                        addBadge("FAMILY", Color(0xFF81D4FA))
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
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 12.dp, end = 8.dp, bottom = if (hasRating) 6.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = movie.title ?: movie.name ?: "",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            lineHeight = 13.sp,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                        if (hasRating) {
                            Text(
                                text = "★ ${String.format("%.1f", displayRating)}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
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

                                val isReleased = movie.isReleased
                                if (isTv) {
                                    if (!isWatched && !isFavorite && !isReminder) onAction(movie)
                                    else if (showActionHint) {
                                        onMessage("Gestisci gli episodi nella pagina dei dettagli")
                                    }
                                } else if (!isReleased && isReminder) {
                                    // For unreleased movies, reminders cannot be removed from the card
                                    if (showActionHint) {
                                        onMessage("Gestisci il promemoria nella pagina dei dettagli")
                                    }
                                } else {
                                    // Normal behavior for released movies or adding a reminder
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
                        val isPromemoriaAttiva = !isReleased && isReminder
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
                                .size(22.dp)
                                .background(color = actionBg, shape = CircleShape)
                                .border(width = 1.dp, color = actionBorder, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val isBellOutlined = !isReleased && !isReminder
                            
                            val bellRotation = remember { androidx.compose.animation.core.Animatable(0f) }
                            var bellHasInitialized by remember { mutableStateOf(false) }
                            
                            LaunchedEffect(isReminder) {
                                if (bellHasInitialized && !isReleased && isReminder) {
                                    bellRotation.animateTo(-25f, tween(60, easing = LinearEasing))
                                    bellRotation.animateTo(20f, tween(100, easing = LinearEasing))
                                    bellRotation.animateTo(-15f, tween(100, easing = LinearEasing))
                                    bellRotation.animateTo(10f, tween(100, easing = LinearEasing))
                                    bellRotation.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
                                }
                                bellHasInitialized = true
                            }
                            
                            Icon(
                                imageVector = when {
                                    isWatched -> CustomIcons.PremiumCheck
                                    !isReleased -> if (isReminder) CustomIcons.PremiumBellFilled else Icons.Rounded.NotificationsNone
                                    isOcchio -> Icons.Rounded.Visibility
                                    else -> CustomIcons.PremiumAdd
                                },
                                contentDescription = "Action",
                                tint = actionTint,
                                modifier = Modifier
                                    .size(if (isBellOutlined) 15.5.dp else 14.dp)
                                    .graphicsLayer {
                                        if (!isReleased && isReminder) {
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

@Composable
private fun MovieCardBadge(text: String, color: Color, modifier: Modifier = Modifier, hazeState: HazeState? = null) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .border(0.5.dp, color.copy(alpha = 0.8f), RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    }
}
