package com.cinetrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cinetrack.R
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.FlickTroveDatePickerModal
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.components.shared.ModalBackButton
import com.cinetrack.ui.history.HistoryViewModel
import com.cinetrack.ui.history.components.HistoryCalendarView
import com.cinetrack.ui.history.components.HistoryFilterModal
import com.cinetrack.ui.history.components.HistoryTimelineView
import com.cinetrack.ui.history.model.ViewMode
import com.cinetrack.ui.history.model.WatchTimelineEntry
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.haze
import java.time.LocalDate
import java.time.ZoneId

class HistoryScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = getViewModel<HistoryViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val internalHazeState = remember { dev.chrisbanes.haze.HazeState() }
        val haptic = LocalHapticFeedback.current

        var editingEntry by remember { mutableStateOf<WatchTimelineEntry?>(null) }
        var showDatePicker by remember { mutableStateOf(false) }
        var entryToDelete by remember { mutableStateOf<WatchTimelineEntry?>(null) }
        var filterButtonBounds by remember { mutableStateOf<Rect?>(null) }

        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
        val topBarClearance = maxOf(statusBarTop, cutoutTop) + 68.dp

        Box(modifier = Modifier.fillMaxSize()) {
            // Authentic FlickTrove cinematic dark background
            CinematicBackground()

            // Scrollable Content Layer (Hazed so items scrolling under the glass top bar are blurred)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(
                        state = internalHazeState,
                        style = HazeStyles.PremiumDark
                    )
            ) {
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
                                onMovieClick = { movieId, mediaType ->
                                    navigator.push(MovieDetailScreen(movieId, mediaType))
                                },
                                onEditDate = { entry ->
                                    editingEntry = entry
                                    showDatePicker = true
                                },
                                onDeleteSession = { entry ->
                                    entryToDelete = entry
                                },
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = topBarClearance + 8.dp,
                                    bottom = 24.dp
                                )
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
                                onMovieClick = { movieId, mediaType ->
                                    navigator.push(MovieDetailScreen(movieId, mediaType))
                                },
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
                                    bottom = 24.dp
                                )
                            )
                        }
                    }
                }
            }

            // Fixed / Floating Glass Top Bar (Placed on top of haze container, not inside)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(20f)
            ) {
                // Glass Background Layer (Renders dynamic blur behind top bar)
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeGlass(
                            state = internalHazeState,
                            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                            borderWidth = 0.dp
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .displayCutoutPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            ModalBackButton(onClick = { navigator.pop() })

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = stringResource(id = R.string.history_screen_title).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                        color = Color.White,
                                        fontSize = 17.sp
                                    )
                                )
                                val subtitleText = "${uiState.totalMoviesCount} film · ${uiState.totalTvCount} serie · ${uiState.totalRewatchCount} rewatch"
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Right Actions: View Switcher + Filter Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // View Switcher Pill
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    Color.White.copy(alpha = 0.12f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val isTimeline = uiState.selectedViewMode == ViewMode.TIMELINE
                                    ViewModeButton(
                                        icon = Icons.AutoMirrored.Rounded.List,
                                        isSelected = isTimeline,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.setViewMode(ViewMode.TIMELINE)
                                        }
                                    )
                                    ViewModeButton(
                                        icon = Icons.Rounded.CalendarMonth,
                                        isSelected = !isTimeline,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.setViewMode(ViewMode.CALENDAR)
                                        }
                                    )
                                }
                            }

                            // Filter Button (Opens HistoryFilterModal with morph animation)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .onGloballyPositioned { coords ->
                                        val pos = coords.positionInWindow()
                                        filterButtonBounds = Rect(pos.x, pos.y, pos.x + coords.size.width, pos.y + coords.size.height)
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.filterConfig.isFiltered) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        } else {
                                            Color.White.copy(alpha = 0.08f)
                                        }
                                    )
                                    .border(
                                        0.5.dp,
                                        if (uiState.filterConfig.isFiltered) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                        } else {
                                            Color.White.copy(alpha = 0.12f)
                                        },
                                        CircleShape
                                    )
                                    .bounceClick(scaleDown = 0.88f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.openFilterModal()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_filtri),
                                    contentDescription = stringResource(R.string.filter_title),
                                    tint = if (uiState.filterConfig.isFiltered) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.White.copy(alpha = 0.85f)
                                    },
                                    modifier = Modifier.size(19.dp)
                                )

                                if (uiState.filterConfig.isFiltered) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

            // Filter Modal (Morphs from Filter Button)
            HistoryFilterModal(
                isVisible = uiState.isFilterModalOpen,
                filterConfig = uiState.filterConfig,
                onFilterConfigChanged = { viewModel.setFilterConfig(it) },
                onDismissRequest = { viewModel.dismissFilterModal() },
                triggerBounds = filterButtonBounds,
                hazeState = internalHazeState
            )

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
                    hazeState = internalHazeState,
                    accentColor = MaterialTheme.colorScheme.primary
                )
            }

            // Delete Confirmation Modal
            if (entryToDelete != null) {
                val target = entryToDelete!!
                FlickTroveModal(
                    isVisible = true,
                    onDismissRequest = { entryToDelete = null },
                    hazeState = internalHazeState,
                    maxWidth = 380.dp,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
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
    private fun ViewModeButton(
        icon: ImageVector,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
                .bounceClick(scaleDown = 0.88f, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
