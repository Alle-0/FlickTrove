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
import com.cinetrack.data.Movie
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.*
import dev.chrisbanes.haze.HazeState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun UpdatesCalendarView(
    reminders: List<Movie>,
    paddingValues: PaddingValues,
    onMovieClick: (Movie) -> Unit,
    currentMonth: java.time.YearMonth,
    onMonthChanged: (java.time.YearMonth) -> Unit,
    onShowMonthPicker: () -> Unit,
    internalHazeState: HazeState? = null
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    
    val remindersByDate = remember(reminders) {
        reminders.groupBy { it.releaseDate ?: it.firstAirDate ?: "" }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, 
                end = 16.dp, 
                bottom = paddingValues.calculateBottomPadding() + 80.dp, 
                top = 124.dp 
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CalendarWidget(
                    currentMonth = currentMonth,
                    onMonthChanged = onMonthChanged,
                    selectedDate = selectedDate,
                    onDateSelected = { 
                        selectedDate = if (selectedDate == it) null else it 
                    },
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
                items(displayedReminders, key = { it.id.toString() + it.mediaType + "_cal_rem" }) { movie ->
                    androidx.compose.foundation.layout.Box(modifier = Modifier.animateItem()) {
                        UpdateCard(
                            movie = movie,
                            label = stringResource(R.string.updates_arriving_prefix, formatReleaseDate(movie.releaseDate ?: movie.firstAirDate)),
                            iconRes = R.drawable.ic_bell_piena,
                            color = MaterialTheme.colorScheme.primary,
                            onAction = { /* Optional: toggle reminder */ },
                            onPress = { onMovieClick(movie) }
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
    remindersByDate: Map<String, List<Movie>>,
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
                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMonthChanged(currentMonth.minusMonths(1)) 
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.updates_calendar_prev_month), tint = Color.White)
                }

                val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { 
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

                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMonthChanged(currentMonth.plusMonths(1)) 
                }) {
                    Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = "Successivo", tint = Color.White)
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
                                    .clickable {
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
                                        color = if (isSelected) Color.White else if (hasReminders) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected || hasReminders || isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (hasReminders) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(4.dp)
                                                .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary, CircleShape)
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
    internalHazeState: HazeState? = null
) {
    val animatedAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (showMonthPicker) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "dialogAlpha"
    )

    if (animatedAlpha == 0f && !showMonthPicker) return

    var selectedYear by remember(showMonthPicker) { mutableStateOf(initialMonth.year) }
    
    // Full screen overlay box catching clicks
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.6f * animatedAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clickable( // Catch clicks inside the dialog so it doesn't dismiss
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .hazeGlass(
                    state = internalHazeState,
                    shape = RoundedCornerShape(26.dp),
                    blurRadius = HazeStyles.SmallGlassBlurRadius,
                    useOffscreenStrategy = true,
                    alpha = animatedAlpha
                )
                .graphicsLayer {
                    alpha = animatedAlpha
                    val scale = 0.95f + (0.05f * animatedAlpha)
                    scaleX = scale
                    scaleY = scale
                }
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Year selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedYear-- },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.updates_calendar_prev_year), tint = Color.White)
                    }
                    Text(
                        text = selectedYear.toString(), 
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { selectedYear++ },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.updates_calendar_next_year), tint = Color.White)
                    }
                }

                // Months grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0..3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (col in 0..2) {
                                val month = row * 3 + col + 1
                                val isSelected = initialMonth.year == selectedYear && initialMonth.monthValue == month
                                val monthName = java.time.Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar { it.uppercase() }
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            onMonthSelected(YearMonth.of(selectedYear, month))
                                            onDismiss()
                                        }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = monthName,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
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
    }
}
