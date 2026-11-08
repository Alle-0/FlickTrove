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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.cinetrack.ui.history.HistoryViewModel
import com.cinetrack.ui.history.components.HistoryCalendarView
import com.cinetrack.ui.history.components.HistoryFilterModal
import com.cinetrack.ui.history.components.HistoryModalsHost
import com.cinetrack.ui.history.components.HistoryTimelineSkeleton
import com.cinetrack.ui.history.components.HistoryTimelineView
import com.cinetrack.ui.history.model.ViewMode
import com.cinetrack.ui.history.model.WatchTimelineEntry
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.fastScrollToTop
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

    val timelineListState = viewModel.timelineListState
    val calendarListState = viewModel.calendarListState
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = uiState.selectedViewMode.ordinal,
        pageCount = { 2 }
    )

    LaunchedEffect(uiState.selectedViewMode) {
        if (pagerState.currentPage != uiState.selectedViewMode.ordinal && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(uiState.selectedViewMode.ordinal)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val targetMode = if (pagerState.currentPage == 0) ViewMode.TIMELINE else ViewMode.CALENDAR
        if (uiState.selectedViewMode != targetMode) {
            viewModel.setViewMode(targetMode)
        }
    }

    val currentListState = if (pagerState.currentPage == 0) timelineListState else calendarListState

    val scrollToTopStateHolder = com.cinetrack.ui.LocalScrollToTopState.current
    val showScrollToTop by remember {
        derivedStateOf {
            val ls = if (pagerState.currentPage == 0) timelineListState else calendarListState
            ls.firstVisibleItemIndex > 3 || (ls.firstVisibleItemIndex > 0 && ls.firstVisibleItemScrollOffset > 150)
        }
    }
    LaunchedEffect(showScrollToTop, currentListState, pagerState.currentPage) {
        scrollToTopStateHolder.value = com.cinetrack.ui.ScrollToTopState(
            isVisible = showScrollToTop,
            scrollProgress = if (showScrollToTop) 1f else 0f,
            onScrollToTop = {
                scope.launch { currentListState.fastScrollToTop() }
            }
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            scrollToTopStateHolder.value = com.cinetrack.ui.ScrollToTopState()
        }
    }

    var editingEntry by remember { mutableStateOf<WatchTimelineEntry?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    when (pageIndex) {
                        0 -> {
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
                                listState = timelineListState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        1 -> {
                            HistoryCalendarView(
                                calendarMonth = uiState.selectedCalendarMonth,
                                calendarEntriesByDate = uiState.calendarEntriesByDate,
                                selectedDate = uiState.selectedCalendarDate,
                                onDateSelected = { viewModel.setSelectedCalendarDate(it) },
                                onPreviousMonth = { viewModel.previousMonth() },
                                onNextMonth = { viewModel.nextMonth() },
                                onMonthChanged = { viewModel.setCalendarMonth(it) },
                                onShowMonthPicker = { showMonthPicker = true },
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
                                ),
                                listState = calendarListState
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
                    isDimmed = uiState.isFilterModalOpen,
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

        HistoryModalsHost(
            showMonthPicker = showMonthPicker,
            onDismissMonthPicker = { showMonthPicker = false },
            onMonthSelected = { month ->
                viewModel.setCalendarMonth(month)
                showMonthPicker = false
            },
            selectedCalendarMonth = uiState.selectedCalendarMonth,
            editingEntry = editingEntry,
            showDatePicker = showDatePicker,
            onDismissDatePicker = {
                showDatePicker = false
                editingEntry = null
            },
            onDateSelected = { newDate ->
                editingEntry?.let { entry ->
                    val newIso = newDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
                    viewModel.updateWatchDate(entry.historyId, newIso)
                }
                showDatePicker = false
                editingEntry = null
            },
            entryToDelete = entryToDelete,
            onDismissDeleteSession = { entryToDelete = null },
            onDeleteSessionEpisodes = { entry ->
                viewModel.deleteSessionEpisodes(entry)
                entryToDelete = null
            },
            onDeleteWatchSession = { historyId ->
                viewModel.deleteWatchSession(historyId)
                entryToDelete = null
            },
            hazeState = internalHazeState
        )
    }
}

@Composable
fun HistoryViewSwitcher(
    isTimeline: Boolean,
    onSelectMode: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    com.cinetrack.ui.components.shared.ListCalendarSwitcher(
        isCalendar = !isTimeline,
        onSelectMode = { isCalendar ->
            onSelectMode(if (isCalendar) ViewMode.CALENDAR else ViewMode.TIMELINE)
        },
        modifier = modifier
    )
}

