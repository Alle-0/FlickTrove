package com.cinetrack.ui.components.updates

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import com.cinetrack.ui.components.glass.hazeGlass
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
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hazeGlass(
                state = null,
                shape = RoundedCornerShape(26.dp),
                blurRadius = HazeStyles.SmallGlassBlurRadius
            )
            .padding(16.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .bounceClick(scaleDown = 0.8f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMonthChanged(currentMonth.minusMonths(1))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_left), contentDescription = stringResource(R.string.updates_calendar_prev_month), tint = Color.White, modifier = Modifier.size(24.dp))
                }

                val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }
                
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
                            onMonthChanged(currentMonth.plusMonths(1))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_right), contentDescription = "Successivo", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            // Days of week
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
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

            // Grid
            val firstDayOfMonth = currentMonth.atDay(1)
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1..7
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
                            val dateString = date.toString()
                            val hasReminders = remindersByDate.containsKey(dateString)
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
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (hasReminders) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected || hasReminders || isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (hasReminders) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(4.dp)
                                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, CircleShape)
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

@Composable
fun MonthYearPickerDialog(
    showMonthPicker: Boolean,
    initialMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    internalHazeState: HazeState? = null,
    accentColor: Color = Color.Unspecified
) {
    if (!showMonthPicker) return

    var selectedYear by remember(showMonthPicker, initialMonth.year) { mutableStateOf(initialMonth.year) }
    val haptic = LocalHapticFeedback.current

    FlickTroveModal(
        isVisible = showMonthPicker,
        onDismissRequest = onDismiss,
        hazeState = internalHazeState,
        maxWidth = 360.dp,
        maxWidthFraction = 0.88f,
        contentPadding = PaddingValues(20.dp)
    ) {
        // Year selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .bounceClick(scaleDown = 0.8f) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedYear--
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_left),
                    contentDescription = stringResource(R.string.updates_calendar_prev_year),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = selectedYear.toString(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .bounceClick(scaleDown = 0.8f) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedYear++
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_right),
                    contentDescription = stringResource(R.string.updates_calendar_next_year),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Months grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0..3) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0..2) {
                        val month = row * 3 + col + 1
                        val isSelected = initialMonth.year == selectedYear && initialMonth.monthValue == month
                        val monthName = java.time.Month.of(month)
                            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            .replaceFirstChar { it.uppercase() }

                        val selectedBgColor = if (accentColor != Color.Unspecified) accentColor else MaterialTheme.colorScheme.primary
                        val selectedContentColor = if (accentColor != Color.Unspecified) {
                            ColorUtils.contentColorForAccent(accentColor)
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) selectedBgColor else Color.White.copy(alpha = 0.05f))
                                .bounceClick(scaleDown = 0.95f) {
                                    onMonthSelected(YearMonth.of(selectedYear, month))
                                    onDismiss()
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = monthName,
                                color = if (isSelected) selectedContentColor else Color.White.copy(alpha = 0.8f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
