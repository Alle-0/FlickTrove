package com.cinetrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.cinetrack.R
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.navigation.GlassyTopBar
import com.cinetrack.ui.components.shared.FlickTroveDatePickerModal
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.history.HistoryViewModel
import com.cinetrack.ui.history.components.HistoryCalendarView
import com.cinetrack.ui.history.components.HistoryFilterModal
import com.cinetrack.ui.history.components.HistoryTimelineSkeleton
import com.cinetrack.ui.history.components.HistoryTimelineView
import com.cinetrack.ui.history.model.ViewMode
import com.cinetrack.ui.history.model.WatchTimelineEntry
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * HistoryTab – Tab interno di MainScreen, integrato nella navigazione tramite TabNavigator.
 * Condivide la GlassyTopBar e la GlassyBottomBar fisse di MainScreen, esattamente come TrophyRoomTab,
 * FoldersTab, StatsTab e FlowTab.
 */
object HistoryTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(id = R.string.history_screen_title)
            return remember(title) {
                TabOptions(
                    index = 8u,
                    title = title,
                    icon = null
                )
            }
        }

    @Composable
    override fun Content() {
        val rootNavigator = LocalNavigator.currentOrThrow.parent ?: LocalNavigator.currentOrThrow
        val paddingValues = LocalAppPadding.current
        val hazeState = LocalHazeState.current

        HistoryScreenContent(
            paddingValues = paddingValues,
            hazeState = hazeState,
            onMovieClick = { movieId, mediaType ->
                rootNavigator.push(MovieDetailScreen(movieId, mediaType))
            },
            showTopBar = false
        )
    }
}

/**
 * Fallback Voyager Screen per eventuale apertura diretta.
 */
class HistoryScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val hazeState = LocalHazeState.current
        BackHandler { navigator.pop() }

        HistoryScreenContent(
            paddingValues = PaddingValues(),
            hazeState = hazeState,
            onMovieClick = { movieId, mediaType ->
                navigator.push(MovieDetailScreen(movieId, mediaType))
            },
            showTopBar = true,
            onBackPress = { navigator.pop() }
        )
    }
}

@Composable
fun HistoryScreenContent(
    paddingValues: PaddingValues = PaddingValues(),
    hazeState: HazeState? = null,
    onMovieClick: (Long, String) -> Unit,
    showTopBar: Boolean = false,
    onBackPress: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentContext = context
    while (currentContext is android.content.ContextWrapper && currentContext !is androidx.activity.ComponentActivity) {
        currentContext = currentContext.baseContext
    }
    val activity = currentContext as? androidx.activity.ComponentActivity

    val viewModel = if (activity != null) {
        androidx.hilt.navigation.compose.hiltViewModel<HistoryViewModel>(activity)
    } else {
        androidx.hilt.navigation.compose.hiltViewModel<HistoryViewModel>()
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val internalHazeState = hazeState ?: remember { HazeState() }

    var editingEntry by remember { mutableStateOf<WatchTimelineEntry?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<WatchTimelineEntry?>(null) }
    var filterButtonBounds by remember { mutableStateOf<Rect?>(null) }

    val statusBarsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topBarClearance = paddingValues.calculateTopPadding() + statusBarsTop + cutoutTop + 46.dp + 16.dp
    val bottomClearance = paddingValues.calculateBottomPadding() + 100.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Authentic FlickTrove cinematic dark background
        CinematicBackground()

        // Scrollable Content Layer (Hazed so items scrolling under the glass bars are blurred)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(
                    state = internalHazeState,
                    style = HazeStyles.PremiumDark
                )
        ) {
            if (uiState.isLoading) {
                HistoryTimelineSkeleton(
                    topBarClearance = topBarClearance,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = topBarClearance,
                        bottom = bottomClearance
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AnimatedContent(
                    targetState = uiState.selectedViewMode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(240)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "HistoryViewModeTransition"
                ) { mode ->
                    when (mode) {
                        ViewMode.TIMELINE -> {
                            HistoryTimelineView(
                                entries = uiState.filteredEntries,
                                onMovieClick = onMovieClick,
                                onEditDate = { entry ->
                                    editingEntry = entry
                                    showDatePicker = true
                                },
                                onDeleteSession = { entry ->
                                    entryToDelete = entry
                                },
                                topBarClearance = topBarClearance,
                                hazeState = internalHazeState,
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = topBarClearance,
                                    bottom = bottomClearance
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        ViewMode.CALENDAR -> {
                            HistoryCalendarView(
                                calendarMonth = uiState.selectedCalendarMonth,
                                calendarEntriesByDate = uiState.calendarEntriesByDate,
                                selectedDate = uiState.selectedCalendarDate,
                                onDateSelected = { viewModel.setSelectedCalendarDate(it) },
                                onPreviousMonth = { viewModel.previousMonth() },
                                onNextMonth = { viewModel.nextMonth() },
                                onMonthChanged = { viewModel.setCalendarMonth(it) },
                                onMovieClick = onMovieClick,
                                onEditDate = { entry ->
                                    editingEntry = entry
                                    showDatePicker = true
                                },
                                onDeleteSession = { entry ->
                                    entryToDelete = entry
                                },
                                hazeState = internalHazeState,
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = topBarClearance,
                                    bottom = bottomClearance
                                )
                            )
                        }
                    }
                }
            }
        }

        // Standalone Top Bar (Only rendered when not embedded inside MainScreen)
        if (showTopBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(20f)
            ) {
                GlassyTopBar(
                    hazeState = internalHazeState,
                    title = stringResource(id = R.string.history_screen_title),
                    onBackPress = onBackPress,
                    onFilterClick = { bounds ->
                        filterButtonBounds = bounds
                        viewModel.openFilterModal()
                    },
                    hasActiveFilters = uiState.filterConfig.isFiltered,
                    customActions = {
                        HistoryViewSwitcher(
                            isTimeline = uiState.selectedViewMode == ViewMode.TIMELINE,
                            onSelectMode = { viewModel.setViewMode(it) }
                        )
                    }
                )
            }

            // Standalone Filter Modal
            HistoryFilterModal(
                isVisible = uiState.isFilterModalOpen,
                filterConfig = uiState.filterConfig,
                onFilterConfigChanged = { viewModel.setFilterConfig(it) },
                onDismissRequest = { viewModel.dismissFilterModal() },
                triggerBounds = filterButtonBounds,
                hazeState = internalHazeState
            )
        }

        // Edit Watch Date Modal
        if (showDatePicker) {
            FlickTroveDatePickerModal(
                initialDate = editingEntry?.parsedDate ?: LocalDate.now(),
                isVisible = showDatePicker,
                onDateSelected = { newDate ->
                    editingEntry?.let { entry ->
                        val newIso = newDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
                        viewModel.updateWatchDate(entry.historyId, newIso)
                    }
                    showDatePicker = false
                    editingEntry = null
                },
                onDismissRequest = {
                    showDatePicker = false
                    editingEntry = null
                },
                accentColor = MaterialTheme.colorScheme.primary,
                hazeState = internalHazeState
            )
        }

        // Confirm Delete Watch Session Modal
        entryToDelete?.let { target ->
            FlickTroveModal(
                isVisible = true,
                onDismissRequest = { entryToDelete = null },
                hazeState = internalHazeState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_trash),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(id = R.string.history_delete_session_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(id = R.string.history_delete_session_confirm),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.65f),
                                lineHeight = 19.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .weight(1f)
                                .bounceClick(scaleDown = 0.94f) { entryToDelete = null }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.settings_cancel),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .weight(1f)
                                .bounceClick(scaleDown = 0.94f) {
                                    viewModel.deleteWatchSession(target.historyId)
                                    entryToDelete = null
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.quick_action_delete),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
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
fun HistoryViewSwitcher(
    isTimeline: Boolean,
    onSelectMode: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val selectedIndex = if (isTimeline) 0 else 1
    val primaryColor = MaterialTheme.colorScheme.primary
    val activeContentColor = ColorUtils.contentColorForAccent(primaryColor)

    val tabWidthDp = 30.dp
    val tabHeightDp = 28.dp
    val paddingDp = 2.dp

    val tabWidthPx = with(density) { tabWidthDp.toPx() }
    val tabHeightPx = with(density) { tabHeightDp.toPx() }
    val paddingPx = with(density) { paddingDp.toPx() }

    val normalWidthPx = tabWidthPx - (paddingPx * 2f)
    val pillHeightPx = tabHeightPx - (paddingPx * 2f)

    val initialLeft = selectedIndex * tabWidthPx + paddingPx
    val initialRight = (selectedIndex + 1) * tabWidthPx - paddingPx

    val animLeft = remember { Animatable(initialLeft) }
    val animRight = remember { Animatable(initialRight) }

    var isDragging by remember { mutableStateOf(false) }
    var previousIndex by remember { mutableStateOf(selectedIndex) }

    // Tab change animation: Asymmetric spring physics (Leading edge stretches, trailing edge catches up)
    LaunchedEffect(selectedIndex, tabWidthPx) {
        val targetLeft = selectedIndex * tabWidthPx + paddingPx
        val targetRight = (selectedIndex + 1) * tabWidthPx - paddingPx

        val isTabChangedByClick = previousIndex != selectedIndex && !isDragging
        previousIndex = selectedIndex

        if (targetLeft > animLeft.value) {
            // Moving right: Right edge leads (fast spring), Left edge trails (softer spring) -> jelly stretch
            val jobRight = launch {
                animRight.animateTo(
                    targetValue = targetRight,
                    animationSpec = spring(stiffness = 650f, dampingRatio = 0.65f)
                )
            }
            val jobLeft = launch {
                animLeft.animateTo(
                    targetValue = targetLeft,
                    animationSpec = spring(stiffness = 450f, dampingRatio = 0.68f)
                )
            }
            jobRight.join()
            jobLeft.join()
        } else if (targetLeft < animLeft.value) {
            // Moving left: Left edge leads (fast spring), Right edge trails (softer spring) -> jelly stretch
            val jobLeft = launch {
                animLeft.animateTo(
                    targetValue = targetLeft,
                    animationSpec = spring(stiffness = 650f, dampingRatio = 0.65f)
                )
            }
            val jobRight = launch {
                animRight.animateTo(
                    targetValue = targetRight,
                    animationSpec = spring(stiffness = 450f, dampingRatio = 0.68f)
                )
            }
            jobLeft.join()
            jobRight.join()
        }
    }

    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "dragScale"
    )

    val pillPath = remember { Path() }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            Color.White.copy(alpha = 0.12f)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(tabWidthDp * 2)
                .height(tabHeightDp)
                .pointerInput(selectedIndex, tabWidthPx) {
                    var isDragActiveOnPill = false
                    val touchMargin = 6.dp.toPx()
                    var dragTargetLeft = 0f
                    var dragTargetRight = 0f

                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val currentLeft = animLeft.value
                            val currentRight = animRight.value
                            if (startOffset.x in (currentLeft - touchMargin)..(currentRight + touchMargin)) {
                                isDragActiveOnPill = true
                                isDragging = true
                                dragTargetLeft = currentLeft
                                dragTargetRight = currentRight
                                coroutineScope.launch {
                                    launch { animLeft.stop() }
                                    launch { animRight.stop() }
                                }
                            } else {
                                isDragActiveOnPill = false
                            }
                        },
                        onDragEnd = {
                            if (!isDragActiveOnPill) return@detectDragGestures
                            isDragActiveOnPill = false
                            isDragging = false

                            val currentCenter = (animLeft.value + animRight.value) / 2f
                            val targetIndex = (currentCenter / tabWidthPx).toInt().coerceIn(0, 1)

                            if (targetIndex != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectMode(if (targetIndex == 0) ViewMode.TIMELINE else ViewMode.CALENDAR)
                            } else {
                                val targetLeft = selectedIndex * tabWidthPx + paddingPx
                                val targetRight = (selectedIndex + 1) * tabWidthPx - paddingPx
                                coroutineScope.launch {
                                    launch { animLeft.animateTo(targetLeft, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                    launch { animRight.animateTo(targetRight, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                }
                            }
                        },
                        onDragCancel = {
                            if (!isDragActiveOnPill) return@detectDragGestures
                            isDragActiveOnPill = false
                            isDragging = false
                            val targetLeft = selectedIndex * tabWidthPx + paddingPx
                            val targetRight = (selectedIndex + 1) * tabWidthPx - paddingPx
                            coroutineScope.launch {
                                launch { animLeft.animateTo(targetLeft, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                launch { animRight.animateTo(targetRight, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                            }
                        }
                    ) { change, dragAmount ->
                        if (!isDragActiveOnPill) return@detectDragGestures
                        change.consume()

                        val dragX = dragAmount.x
                        dragTargetLeft += dragX
                        dragTargetRight += dragX

                        coroutineScope.launch {
                            val maxRight = 2f * tabWidthPx - paddingPx
                            val maxLeft = maxRight - normalWidthPx

                            var targetLeft = dragTargetLeft
                            var targetRight = dragTargetRight

                            // Soft elastic edge squish
                            if (dragTargetLeft < paddingPx) {
                                val overscroll = (paddingPx - dragTargetLeft).coerceAtMost(normalWidthPx * 0.5f)
                                targetLeft = paddingPx - (overscroll * 0.2f)
                                targetRight = (paddingPx + normalWidthPx) - overscroll * 0.1f
                            } else if (dragTargetRight > maxRight) {
                                val overscroll = (dragTargetRight - maxRight).coerceAtMost(normalWidthPx * 0.5f)
                                targetRight = maxRight + (overscroll * 0.2f)
                                targetLeft = maxLeft + overscroll * 0.1f
                            }

                            val leftStiffness = if (dragX > 0) 300f else 750f
                            val rightStiffness = if (dragX > 0) 750f else 300f

                            launch { animLeft.animateTo(targetLeft, spring(stiffness = leftStiffness, dampingRatio = 0.65f)) }
                            launch { animRight.animateTo(targetRight, spring(stiffness = rightStiffness, dampingRatio = 0.65f)) }
                        }
                    }
                }
        ) {
            // 1. Base Inactive Layer: Icons in soft muted white
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Timeline
                Box(
                    modifier = Modifier
                        .width(tabWidthDp)
                        .height(tabHeightDp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (selectedIndex != 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectMode(ViewMode.TIMELINE)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_lista),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Tab 1: Calendar
                Box(
                    modifier = Modifier
                        .width(tabWidthDp)
                        .height(tabHeightDp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (selectedIndex != 1) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectMode(ViewMode.CALENDAR)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 2. Sliding Indicator Pill + 3. Active Masked Layer (Dual-layer clipPath morphing)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val currentLeft = animLeft.value
                        val currentRight = animRight.value
                        val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                        val centerXPx = (currentLeft + currentRight) / 2f

                        val scaledWidth = (currentWidth * dragScale).coerceAtLeast(1f)
                        val pillLeft = centerXPx - (scaledWidth / 2f)
                        val pillTop = paddingPx
                        val cornerRadius = pillHeightPx / 2f

                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(pillLeft, pillTop),
                            size = Size(scaledWidth, pillHeightPx),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )
                    }
                    .drawWithContent {
                        val currentLeft = animLeft.value
                        val currentRight = animRight.value
                        val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                        val centerXPx = (currentLeft + currentRight) / 2f

                        val scaledWidth = (currentWidth * dragScale).coerceAtLeast(1f)
                        val pillLeft = centerXPx - (scaledWidth / 2f)
                        val pillTop = paddingPx
                        val cornerRadius = pillHeightPx / 2f

                        pillPath.reset()
                        pillPath.addRoundRect(
                            RoundRect(
                                left = pillLeft,
                                top = pillTop,
                                right = pillLeft + scaledWidth,
                                bottom = pillTop + pillHeightPx,
                                radiusX = cornerRadius,
                                radiusY = cornerRadius
                            )
                        )

                        // Mask: Only the portion of the icon under the indicator pill is rendered in active color!
                        clipPath(pillPath) {
                            this@drawWithContent.drawContent()
                        }
                    }
            ) {
                // Active Icons Layer: Rendered in active high-contrast color (masked by clipPath)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(tabWidthDp)
                            .height(tabHeightDp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_lista),
                            contentDescription = null,
                            tint = activeContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(tabWidthDp)
                            .height(tabHeightDp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                            contentDescription = null,
                            tint = activeContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
