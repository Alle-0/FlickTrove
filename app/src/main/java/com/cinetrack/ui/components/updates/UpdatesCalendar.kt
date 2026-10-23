package com.cinetrack.ui.components.updates

import com.cinetrack.ui.components.shared.calendar.CalendarMonthWidget
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.theme.*
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.components.shared.FlickTroveModal
import dev.chrisbanes.haze.HazeState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun UpdatesCalendarView(
    reminders: List<ReminderItem>,
    paddingValues: PaddingValues,
    onMovieClick: (Movie) -> Unit,
    currentMonth: java.time.YearMonth,
    onMonthChanged: (java.time.YearMonth) -> Unit,
    onShowMonthPicker: () -> Unit,
    internalHazeState: HazeState? = null
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    
    val remindersByDate = remember(reminders) {
        reminders.groupBy { it.arrivalDate }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, 
                end = 16.dp, 
                bottom = paddingValues.calculateBottomPadding() + 80.dp, 
                top = 180.dp 
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                CalendarWidget(
                    currentMonth = currentMonth,
                    onMonthChanged = onMonthChanged,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    remindersByDate = remindersByDate,
                    onShowMonthPicker = onShowMonthPicker,
                    internalHazeState = internalHazeState
                )
            }

        val displayedReminders = if (selectedDate != null) {
            remindersByDate[selectedDate.toString()] ?: emptyList()
        } else {
            emptyList()
        }

        if (selectedDate != null) {
            if (displayedReminders.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.updates_no_reminders),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.updates_calendar_reminder_date, formatReleaseDate(selectedDate.toString())),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }
                items(displayedReminders, key = { it.id + "_cal_rem" }) { item ->
                    androidx.compose.foundation.layout.Box(modifier = Modifier.animateItem()) {
                        UpdateCard(
                            movie = item.movie,
                            label = stringResource(R.string.updates_arriving_prefix, formatReleaseDate(item.arrivalDate)),
                            episodeBadge = item.episodeInfo,
                            rawDate = item.arrivalDate,
                            iconRes = R.drawable.ic_bell_piena,
                            color = MaterialTheme.colorScheme.primary,
                            isReminder = true,
                            onAction = {},
                            onPress = { onMovieClick(item.movie) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.updates_calendar_select_day),
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
            }
        }
    }
}


@Composable
fun CalendarWidget(
    currentMonth: YearMonth,
    onMonthChanged: (YearMonth) -> Unit,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    remindersByDate: Map<String, List<ReminderItem>>,
    onShowMonthPicker: () -> Unit,
    internalHazeState: HazeState? = null
) {
    CalendarMonthWidget(
        currentMonth = currentMonth,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        onPreviousMonth = { onMonthChanged(currentMonth.minusMonths(1)) },
        onNextMonth = { onMonthChanged(currentMonth.plusMonths(1)) },
        onShowMonthPicker = onShowMonthPicker,
        hasIndicator = { remindersByDate.containsKey(it.toString()) },
        modifier = Modifier
            .clip(RoundedCornerShape(36.dp))
            .background(Color(0xFF14171E).copy(alpha = 0.35f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(36.dp)
            )
    )
}

@Composable
fun MonthYearPickerDialog(
    showMonthPicker: Boolean,
    initialMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    internalHazeState: HazeState? = null,
    accentColor: Color = Color.Unspecified
) {
    com.cinetrack.ui.components.shared.calendar.MonthYearPickerDialog(
        showMonthPicker = showMonthPicker,
        initialMonth = initialMonth,
        onMonthSelected = onMonthSelected,
        onDismiss = onDismiss,
        internalHazeState = internalHazeState,
        accentColor = accentColor
    )
}
