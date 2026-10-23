package com.cinetrack.ui.history.components

import com.cinetrack.ui.components.shared.calendar.CalendarMonthWidget
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    onShowMonthPicker: () -> Unit = {},
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
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
                    onShowMonthPicker = onShowMonthPicker,
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
                            hazeState = hazeState,
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
    CalendarMonthWidget(
        currentMonth = currentMonth,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        onShowMonthPicker = onShowMonthPicker,
        hasIndicator = { entriesByDate.containsKey(it) },
        modifier = Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF14171E).copy(alpha = 0.35f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(26.dp)
            )
    )
}
