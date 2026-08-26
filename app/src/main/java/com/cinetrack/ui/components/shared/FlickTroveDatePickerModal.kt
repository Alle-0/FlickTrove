package com.cinetrack.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.components.updates.MonthYearPickerDialog
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun FlickTroveDatePickerModal(
    initialDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit,
    hazeState: HazeState? = null,
    accentColor: Color,
    disableFutureDates: Boolean = true
) {
    var selectedDate by remember { mutableStateOf(initialDate ?: LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val internalHazeState = remember { HazeState() }

    FlickTroveModal(
        onDismissRequest = onDismissRequest,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .haze(state = internalHazeState)
        ) {
            // Header: Selected Date
            val headerFormatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(Locale.getDefault())
            Text(
                text = selectedDate.format(headerFormatter),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .bounceClick(scaleDown = 0.95f) { showMonthPicker = true }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentMonth.format(monthFormatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                        contentDescription = "Change Date",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(scaleDown = 0.8f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentMonth = currentMonth.minusMonths(1)
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_left),
                            contentDescription = "Previous Month",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(scaleDown = 0.8f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentMonth = currentMonth.plusMonths(1)
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                            contentDescription = "Next Month",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Days of week header
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
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
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
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
                            val isSelected = selectedDate == date
                            val isToday = date == LocalDate.now()
                            val isFuture = disableFutureDates && date.isAfter(LocalDate.now())

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> accentColor
                                            isToday -> Color.White.copy(alpha = 0.15f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .bounceClick(scaleDown = if (isFuture) 1f else 0.8f) {
                                        if (!isFuture) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedDate = date
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentDay.toString(),
                                    color = if (isSelected) Color.White else if (isFuture) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.8f),
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            currentDay++
                        }
                    }
                }
                weekRow++
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .bounceClick(scaleDown = 0.9f, onClick = onDismissRequest)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Box(
                    modifier = Modifier
                        .bounceClick(scaleDown = 0.9f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDateSelected(selectedDate)
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(android.R.string.ok),
                        color = accentColor,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    MonthYearPickerDialog(
        showMonthPicker = showMonthPicker,
        initialMonth = currentMonth,
        onMonthSelected = { currentMonth = it },
        onDismiss = { showMonthPicker = false },
        internalHazeState = internalHazeState,
        accentColor = accentColor
    )
}
