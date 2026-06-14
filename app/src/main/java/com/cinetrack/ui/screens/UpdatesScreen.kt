package com.cinetrack.ui.screens

import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cinetrack.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cinetrack.data.Movie
import com.cinetrack.ui.viewmodel.UpdatesViewModel
import com.cinetrack.ui.assets.CustomIcons
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.zIndex
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.compose.ui.util.lerp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
// StatsBackground/AppBackground removed; using CinematicBackground directly
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.CinematicBackground
import com.cinetrack.ui.utils.bounceClick

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UpdatesScreen(
    viewModel: UpdatesViewModel,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    startX: Float? = null,
    startY: Float? = null,
    onBack: () -> Unit,
    onClosing: () -> Unit = {},
    onMovieClick: (Movie) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val today = LocalDate.now().toString()
    
    val movies = uiState.movies
    val newEpisodes = movies.filter { (it.newEpisodesFound ?: 0) > 0 }
    val releasedRecently = movies.filter { it.migratedAt == today && (it.newEpisodesFound ?: 0) == 0 }
    val futureReminders = movies.filter { it.reminder && (it.releaseDate ?: it.firstAirDate ?: "") > today }
        .sortedBy { it.releaseDate ?: it.firstAirDate }

    val pagerState = rememberPagerState(pageCount = { 2 })
    var isMeasured by remember { mutableStateOf(false) }
    var hasRevealed by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val revealAmount = remember(hasRevealed) { Animatable(if (hasRevealed) 1f else 0f) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(isMeasured) {
        if (isMeasured && !hasRevealed) {
            revealAmount.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 800, 
                    easing = CubicBezierEasing(0.7f, 0f, 0.2f, 1f)
                )
            )
            hasRevealed = true
        }
    }

    val triggerExit = {
        if (!isClosing) {
            isClosing = true
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

    BackHandler(enabled = !isClosing) {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(0) }
        } else {
            triggerExit()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        
        if (width > 0 && !isMeasured) {
            LaunchedEffect(Unit) { isMeasured = true }
        }
        
        val density = androidx.compose.ui.platform.LocalDensity.current
        val statusBarsTopPx = androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(density).toFloat()
        val center = remember(width, height, statusBarsTopPx, startX, startY) {
            if (startX != null && startY != null) {
                Offset(startX, startY)
            } else {
                with(density) {
                    Offset(
                        x = width - 28.dp.toPx(),
                        y = statusBarsTopPx + 32.dp.toPx()
                    )
                }
            }
        }
        
        val maxRadius = remember(center, width, height) {
            val distTopLeft = sqrt(center.x.pow(2) + center.y.pow(2))
            val distTopRight = sqrt((width - center.x).pow(2) + center.y.pow(2))
            val distBottomLeft = sqrt(center.x.pow(2) + (height - center.y).pow(2))
            val distBottomRight = sqrt((width - center.x).pow(2) + (height - center.y).pow(2))
            // Multiply by 1.1 to ensure the circle fully covers the screen edges even with anti-aliasing
            max(max(distTopLeft, distTopRight), max(distBottomLeft, distBottomRight)) * 1.1f
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                    val radius = revealAmount.value * maxRadius
                    clip = true
                    shape = object : Shape {
                        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                            val path = Path().apply {
                                addOval(Rect(center = center, radius = radius))
                            }
                            return Outline.Generic(path)
                        }
                    }
                }
                
            ) {
            val internalHazeState = remember { HazeState() }

            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                // 1. Content Layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(
                            state = internalHazeState,
                            style = HazeStyles.PremiumDark
                        )
                        
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true
                    ) { pageIndex ->
                        if (pageIndex == 1) {
                            // REMINDERS VIEW
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp, 
                                    end = 16.dp, 
                                    bottom = paddingValues.calculateBottomPadding() + 80.dp, 
                                    top = 124.dp 
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (futureReminders.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillParentMaxSize(), 
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Nessun promemoria", color = Color.White.copy(alpha = 0.3f))
                                        }
                                    }
                                } else {
                                    items(futureReminders, key = { it.id.toString() + it.mediaType + "_rem" }, contentType = { "movie" }) { movie ->
                                        androidx.compose.foundation.layout.Box(modifier = Modifier.animateItem()) {
                                            UpdateCard(
                                                movie = movie,
                                                label = "In Arrivo: ${movie.releaseDate ?: movie.firstAirDate}",
                                                iconRes = R.drawable.ic_bell_piena,
                                                color = MaterialTheme.colorScheme.primary,
                                                onAction = { /* Optional: toggle reminder */ },
                                                onPress = { onMovieClick(movie) }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // MAIN NOTIFICATIONS VIEW
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 20.dp, 
                                    end = 20.dp, 
                                    bottom = paddingValues.calculateBottomPadding() + 80.dp, 
                                    top = 124.dp 
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Promemoria Summary Card
                                item {
                                    RemindersSummaryCard(
                                        count = futureReminders.size,
                                        onClick = { 
                                            scope.launch { pagerState.animateScrollToPage(1) }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                if (uiState.isLoading) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp), 
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                } else {
                                    val hasUpdates = newEpisodes.isNotEmpty() || releasedRecently.isNotEmpty()
                                    
                                    if (newEpisodes.isNotEmpty()) {
                                        item { 
                                            SectionHeader(
                                                text = "Nuovi Episodi",
                                                action = {
                                                    val markInteractionSource = remember { MutableInteractionSource() }
                                                    val markIsPressed by markInteractionSource.collectIsPressedAsState()
                                                    val markScale by animateFloatAsState(if (markIsPressed) 0.9f else 1f, label = "markScale")
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .graphicsLayer { scaleX = markScale; scaleY = markScale }
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                            .clickable(
                                                                interactionSource = markInteractionSource,
                                                                indication = null
                                                            ) { 
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                viewModel.clearAllNewEpisodes() 
                                                            }
                                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            "SEGNA TUTTI", 
                                                            color = MaterialTheme.colorScheme.primary, 
                                                            fontSize = 10.sp, 
                                                            fontWeight = FontWeight.Black,
                                                            letterSpacing = 1.sp
                                                        )
                                                    }
                                                }
                                            ) 
                                        }
                                        items(newEpisodes, key = { it.id.toString() + it.mediaType + "_new" }, contentType = { "movie" }) { movie ->
                                            androidx.compose.foundation.layout.Box(modifier = Modifier.animateItem()) {
                                                UpdateCard(
                                                    movie = movie,
                                                    label = "${movie.newEpisodesFound} nuovi episodi",
                                                    iconRes = R.drawable.ic_tick_card,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    onAction = { viewModel.clearUpdate(movie.id, movie.mediaType) },
                                                    onPress = { onMovieClick(movie) }
                                                )
                                            }
                                        }
                                    }

                                    if (releasedRecently.isNotEmpty()) {
                                        item { 
                                            SectionHeader(
                                                text = "Usciti Oggi!",
                                                action = {
                                                    val markInteractionSource = remember { MutableInteractionSource() }
                                                    val markIsPressed by markInteractionSource.collectIsPressedAsState()
                                                    val markScale by animateFloatAsState(if (markIsPressed) 0.9f else 1f, label = "markMigratedScale")
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .graphicsLayer { scaleX = markScale; scaleY = markScale }
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                            .clickable(
                                                                interactionSource = markInteractionSource,
                                                                indication = null
                                                            ) { 
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                viewModel.clearAllMigrated() 
                                                            }
                                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            "SEGNA TUTTI", 
                                                            color = MaterialTheme.colorScheme.primary, 
                                                            fontSize = 10.sp, 
                                                            fontWeight = FontWeight.Black,
                                                            letterSpacing = 1.sp
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        items(releasedRecently, key = { it.id.toString() + it.mediaType + "_recent" }, contentType = { "movie" }) { movie ->
                                            androidx.compose.foundation.layout.Box(modifier = Modifier.animateItem()) {
                                                UpdateCard(
                                                    movie = movie,
                                                    label = "Disponibile ora",
                                                    iconRes = R.drawable.ic_tick_card,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    onAction = { viewModel.clearMigrated(movie.id, movie.mediaType) },
                                                    onPress = { onMovieClick(movie) }
                                                )
                                            }
                                        }
                                    }

                                    if (!hasUpdates) {
                                        item {
                                            EmptyNotificationsState()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Fixed Header (Layered on top of content)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .offset(y = (-10).dp)
                        .hazeGlass(
                            state = internalHazeState,
                            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                            borderWidth = 0.dp
                        )
                        .padding(top = 10.dp)
                        .zIndex(10f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .displayCutoutPadding()
                            .height(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                    // Back Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .bounceClick { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (pagerState.currentPage > 0) {
                                        scope.launch { pagerState.animateScrollToPage(0) }
                                    } else {
                                        triggerExit() 
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Title with Animation
                    val titleText = if (pagerState.currentPage == 1) "Promemoria" else "Notifiche"
                    AnimatedContent(
                        targetState = titleText,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) + slideInVertically { it / 2 } togetherWith
                            fadeOut(animationSpec = tween(300)) + slideOutVertically { -it / 2 }
                        },
                        modifier = Modifier,
                        label = "TitleAnimation"
                    ) { text ->
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } // end inner Box (statusBarsPadding)
            } // end outer Box (hazeGlass header)
        } // end Box(fillMaxSize)
    } // end Box(graphicsLayer clip)
        } // end Box(fillMaxSize)
    } // end BoxWithConstraints
} // end UpdatesScreen

@Composable
fun RemindersSummaryCard(count: Int, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isPressed) Spring.StiffnessHigh else Spring.StiffnessLow
        ),
        label = "summaryScale"
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hazeGlass(
                state = null, // Header uses its own state, cards use global or none for now to avoid complexity, but usually null is fine for subtle fallback or we can pass state
                shape = RoundedCornerShape(32.dp),
                blurRadius = HazeStyles.SmallGlassBlurRadius
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_bell_piena),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = "I tuoi Promemoria",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$count in attesa",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            }

            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun EmptyNotificationsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_bell),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.05f),
                modifier = Modifier.size(60.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Nessuna notifica recente.",
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SectionHeader(text: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp, start = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        action?.invoke()
    }
}

@Composable
fun UpdateCard(
    movie: Movie, 
    label: String, 
    iconRes: Int, 
    color: Color, 
    onAction: () -> Unit, 
    onPress: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isPressed) Spring.StiffnessHigh else Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hazeGlass(
                state = null,
                shape = RoundedCornerShape(26.dp),
                blurRadius = HazeStyles.SmallGlassBlurRadius
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onPress()
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, LocalImageQuality.current),
                contentDescription = null,
                modifier = Modifier.width(44.dp).height(58.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    text = movie.title ?: movie.name ?: "", 
                    color = Color.White, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1
                )
                Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            val actionInteractionSource = remember { MutableInteractionSource() }
            val isActionPressed by actionInteractionSource.collectIsPressedAsState()
            val actionScale by animateFloatAsState(
                targetValue = if (isActionPressed) 0.8f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = if (isActionPressed) Spring.StiffnessHigh else Spring.StiffnessLow
                ),
                label = "actionScale"
            )

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onAction()
                },
                interactionSource = actionInteractionSource,
                modifier = Modifier.graphicsLayer {
                    scaleX = actionScale
                    scaleY = actionScale
                }
            ) {
                Icon(imageVector = ImageVector.vectorResource(id = iconRes), contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            }
        }
    }
}
