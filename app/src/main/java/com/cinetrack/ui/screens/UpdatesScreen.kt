package com.cinetrack.ui.screens

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.cinetrack.R

import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.viewmodel.UpdatesViewModel
import com.cinetrack.ui.components.updates.*
import com.cinetrack.ui.assets.CustomIcons
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.zIndex
import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.format.TextStyle
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.compose.ui.util.lerp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.common.CinematicBackground
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
    var remindersCategoryTab by rememberSaveable { mutableIntStateOf(0) }

    val rawFutureReminders = remember(movies, today) {
        movies.flatMap { it.generateReminderItems(today) }
            .sortedBy { it.arrivalDate }
    }

    val futureReminders = remember(rawFutureReminders, remindersCategoryTab) {
        when (remindersCategoryTab) {
            1 -> rawFutureReminders.filter { !it.isOngoingSeriesEpisode }
            2 -> rawFutureReminders.filter { it.isOngoingSeriesEpisode }
            else -> rawFutureReminders
        }
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    var isMeasured by remember { mutableStateOf(false) }
    var hasRevealed by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val revealAmount = remember(hasRevealed) { Animatable(if (hasRevealed) 1f else 0f) }
    var isClosing by remember { mutableStateOf(false) }
    var isCalendarView by rememberSaveable { mutableStateOf(false) }
    var currentMonth by remember { mutableStateOf(java.time.YearMonth.now()) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val remindersListState = rememberLazyListState()
    LaunchedEffect(remindersCategoryTab) {
        remindersListState.scrollToItem(0)
    }

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
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true
                    ) { pageIndex ->
                        if (pageIndex == 1) {
                            // REMINDERS VIEW
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .haze(
                                            state = internalHazeState,
                                            style = HazeStyles.PremiumDark
                                        )
                                ) {
                                    AnimatedContent(
                                        targetState = isCalendarView,
                                        transitionSpec = {
                                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                        },
                                        label = "CalendarViewToggle"
                                    ) { showCalendar ->
                                        if (showCalendar) {
                                            UpdatesCalendarView(
                                                reminders = futureReminders,
                                                paddingValues = paddingValues,
                                                onMovieClick = onMovieClick,
                                                currentMonth = currentMonth,
                                                onMonthChanged = { currentMonth = it },
                                                onShowMonthPicker = { showMonthPicker = true },
                                                internalHazeState = internalHazeState
                                            )
                                        } else {
                                            LazyColumn(
                                                state = remindersListState,
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(
                                                    start = 16.dp, 
                                                    end = 16.dp, 
                                                    bottom = paddingValues.calculateBottomPadding() + 80.dp, 
                                                    top = 180.dp 
                                                ),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                if (futureReminders.isEmpty()) {
                                                    item {
                                                        Box(
                                                            modifier = Modifier.fillParentMaxSize(), 
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(stringResource(R.string.updates_no_reminders), color = Color.White.copy(alpha = 0.3f))
                                                        }
                                                    }
                                                } else {
                                                    items(futureReminders, key = { it.id + "_rem" }, contentType = { "movie" }) { item ->
                                                        androidx.compose.foundation.layout.Box(modifier = Modifier.animateItem()) {
                                                            UpdateCard(
                                                                movie = item.movie,
                                                                label = stringResource(R.string.updates_arriving_prefix, formatReleaseDate(item.arrivalDate)) + item.episodeInfo,
                                                                iconRes = R.drawable.ic_bell_piena,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                onAction = { /* Optional: toggle reminder */ },
                                                                onPress = { onMovieClick(item.movie) }
                                                            )
                                                        }
                                                    }
                                                }
                                        }
                                }
                            }
                        }

                        if (rawFutureReminders.isNotEmpty() || remindersCategoryTab != 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                            .displayCutoutPadding()
                                            .padding(top = 78.dp)
                                            .padding(horizontal = 24.dp)
                                            .zIndex(9f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Spacer(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .hazeGlass(
                                                    state = internalHazeState,
                                                    shape = CircleShape,
                                                    blurRadius = HazeStyles.SmallGlassBlurRadius,
                                                    useOffscreenStrategy = false
                                                )
                                        )
                                        val options = listOf(
                                            stringResource(R.string.updates_tab_all),
                                            stringResource(R.string.updates_tab_new_releases),
                                            stringResource(R.string.updates_tab_ongoing_episodes)
                                        )
                                        val counts = listOf(
                                            rawFutureReminders.size,
                                            rawFutureReminders.count { !it.isOngoingSeriesEpisode },
                                            rawFutureReminders.count { it.isOngoingSeriesEpisode }
                                        )
                                        com.cinetrack.ui.components.common.CategoryTabSelector(
                                            options = options,
                                            counts = counts,
                                            selectedIndex = remindersCategoryTab,
                                            onOptionClick = { index ->
                                                remindersCategoryTab = index
                                                scope.launch { remindersListState.scrollToItem(0) }
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // MAIN NOTIFICATIONS VIEW
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .haze(
                                        state = internalHazeState,
                                        style = HazeStyles.PremiumDark
                                    )
                            ) {
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
                                                text = stringResource(R.string.updates_new_episodes),
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
                                                            stringResource(R.string.updates_mark_all), 
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
                                                    label = stringResource(R.string.updates_new_episodes_count, movie.newEpisodesFound ?: 0),
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
                                                text = stringResource(R.string.updates_out_today),
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
                                                            stringResource(R.string.updates_mark_all), 
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
                                                    label = stringResource(R.string.updates_available_now),
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
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_left),
                                contentDescription = stringResource(R.string.detail_content_desc_back),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Title with Animation
                    val titleText = if (pagerState.currentPage == 1) stringResource(R.string.updates_tab_reminders) else stringResource(R.string.updates_tab_notifications)
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

                    // Calendar Toggle Button
                    androidx.compose.animation.AnimatedVisibility(
                        visible = pagerState.currentPage == 1,
                        enter = fadeIn() + slideInHorizontally { it / 2 },
                        exit = fadeOut() + slideOutHorizontally { it / 2 },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                                .bounceClick { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isCalendarView = !isCalendarView 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = if (isCalendarView) R.drawable.ic_lista else R.drawable.ic_calendario),
                                contentDescription = "Toggle Calendar",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } // end inner Box (statusBarsPadding)
            } // end outer Box (hazeGlass header)


        } // end Box(fillMaxSize)
        
        // Dialog OVERLAY - Outside the haze capturing scope to avoid blurring its own text
        // Dialog OVERLAY - Outside the haze capturing scope to avoid blurring its own text
        val dialogHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        MonthYearPickerDialog(
            showMonthPicker = showMonthPicker,
            initialMonth = currentMonth,
            onMonthSelected = { 
                dialogHaptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                currentMonth = it
            },
            onDismiss = { showMonthPicker = false },
            internalHazeState = internalHazeState
        )
    } // end Box(graphicsLayer clip)
        } // end Box(fillMaxSize)
    } // end BoxWithConstraints
} // end UpdatesScreen
