package com.cinetrack.ui.history.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.updates.MonthYearPickerDialog
import com.cinetrack.ui.components.updates.formatReleaseDate
import com.cinetrack.ui.history.model.WatchTimelineEntry
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryCalendarView(
    calendarMonth: YearMonth,
    calendarEntriesByDate: Map<LocalDate, List<WatchTimelineEntry>>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onMovieClick: (movieId: Long, mediaType: String) -> Unit,
    onEditDate: (entry: WatchTimelineEntry) -> Unit,
    onDeleteSession: (entry: WatchTimelineEntry) -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
    var showMonthPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Authentic FlickTrove CalendarWidget
            item(key = "calendar_widget") {
                HistoryCalendarWidget(
                    currentMonth = calendarMonth,
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onShowMonthPicker = { showMonthPicker = true },
                    entriesByDate = calendarEntriesByDate,
                    internalHazeState = hazeState
                )
            }

            // Entries section below the calendar
            if (selectedDate != null) {
                val displayedEntries = calendarEntriesByDate[selectedDate].orEmpty()

                if (displayedEntries.isEmpty()) {
                    item(key = "empty_date_viewings") {
                        Text(
                            text = stringResource(R.string.history_no_entries_calendar),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    item(key = "header_date_viewings") {
                        Text(
                            text = stringResource(
                                R.string.history_calendar_entries_on_date,
                                formatReleaseDate(selectedDate.toString())
                            ),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }

                    items(
                        items = displayedEntries,
                        key = { "cal_entry_${it.historyId}" }
                    ) { entry ->
                        HistoryCard(
                            entry = entry,
                            isCalendarDayMode = true,
                            onClick = { onMovieClick(entry.movieId, entry.mediaType) },
                            onEditDate = { onEditDate(entry) },
                            onDeleteSession = { onDeleteSession(entry) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            } else {
                item(key = "prompt_select_day") {
                    Text(
                        text = stringResource(R.string.history_calendar_select_prompt),
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Year/Month Picker Dialog matching UpdatesScreen
        MonthYearPickerDialog(
            showMonthPicker = showMonthPicker,
            initialMonth = calendarMonth,
            onMonthSelected = { month ->
                onMonthChanged(month)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
            internalHazeState = hazeState,
            accentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun HistoryCalendarWidget(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onShowMonthPicker: () -> Unit,
    entriesByDate: Map<LocalDate, List<WatchTimelineEntry>>,
    internalHazeState: HazeState? = null
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hazeGlass(
                state = internalHazeState,
                shape = RoundedCornerShape(26.dp),
                blurRadius = HazeStyles.SmallGlassBlurRadius
            )
            .padding(16.dp)
    ) {
        Column {
            // Header with month navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .bounceClick(scaleDown = 0.8f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPreviousMonth()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_left),
                        contentDescription = stringResource(R.string.updates_calendar_prev_month),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    .replaceFirstChar { it.uppercase() }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .bounceClick(scaleDown = 0.95f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShowMonthPicker()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "$monthName ${currentMonth.year}",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                        contentDescription = stringResource(R.string.updates_calendar_change_date),
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .bounceClick(scaleDown = 0.8f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNextMonth()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                        contentDescription = "Successivo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Days of week header (L M M G V S D)
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY
                )
                for (day in daysOfWeek) {
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1).uppercase(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            val firstDayOfMonth = currentMonth.atDay(1)
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1..7 (Monday=1, Sunday=7)
            val daysInMonth = currentMonth.lengthOfMonth()

            var currentDay = 1
            var weekRow = 0

            while (currentDay <= daysInMonth) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    for (i in 1..7) {
                        if (weekRow == 0 && i < firstDayOfWeek) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else if (currentDay > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val date = currentMonth.atDay(currentDay)
                            val hasEntries = entriesByDate.containsKey(date)
                            val isSelected = selectedDate == date
                            val isToday = date == LocalDate.now()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> Color.White.copy(alpha = 0.15f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .bounceClick(scaleDown = 0.9f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onDateSelected(date)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = currentDay.toString(),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else if (hasEntries) MaterialTheme.colorScheme.primary
                                        else Color.White.copy(alpha = 0.8f),
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected || hasEntries || isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (hasEntries) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(4.dp)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.primary,
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                            currentDay++
                        }
                    }
                }
                weekRow++
            }
        }
    }
}
