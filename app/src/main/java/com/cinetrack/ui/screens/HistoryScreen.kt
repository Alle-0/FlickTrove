package com.cinetrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
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

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarClearance = statusBarTop + 46.dp + 8.dp
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
                        top = topBarClearance + 6.dp,
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
                                    top = topBarClearance + 8.dp,
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
                                    top = topBarClearance + 8.dp,
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
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            Color.White.copy(alpha = 0.12f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryViewModeButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_lista),
                isSelected = isTimeline,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectMode(ViewMode.TIMELINE)
                }
            )
            HistoryViewModeButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                isSelected = !isTimeline,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectMode(ViewMode.CALENDAR)
                }
            )
        }
    }
}

@Composable
fun HistoryViewModeButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .bounceClick(scaleDown = 0.88f, onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}
