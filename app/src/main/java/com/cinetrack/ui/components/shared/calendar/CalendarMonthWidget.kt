package com.cinetrack.ui.components.shared.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Reusable month calendar card with navigation controls, localized weekday headers,
 * and a 7-column grid with selection, today highlighting, and dynamic event indicators.
 */
@Composable
fun CalendarMonthWidget(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onShowMonthPicker: () -> Unit,
    hasIndicator: (LocalDate) -> Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onAccentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 12.dp)
    ) {
        Column {
            // Month Header with navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                        modifier = Modifier.size(22.dp)
                    )
                }

                val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    .replaceFirstChar { it.uppercase() }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .bounceClick(scaleDown = 0.95f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShowMonthPicker()
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "$monthName ${currentMonth.year}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                        contentDescription = stringResource(R.string.updates_calendar_change_date),
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Days of week header (L M M G V S D)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Calendar Day Grid
            val firstDayOfMonth = currentMonth.atDay(1)
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1..7 (Monday=1, Sunday=7)
            val daysInMonth = currentMonth.lengthOfMonth()

            var currentDay = 1
            var weekRow = 0

            while (currentDay <= daysInMonth) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    for (i in 1..7) {
                        if (weekRow == 0 && i < firstDayOfWeek) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else if (currentDay > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val date = currentMonth.atDay(currentDay)
                            val hasEvent = hasIndicator(date)
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
                                            isSelected -> accentColor
                                            isToday -> Color.White.copy(alpha = 0.12f)
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
                                        color = if (isSelected) onAccentColor
                                        else if (hasEvent) accentColor
                                        else Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || hasEvent || isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(4.dp)
                                            .background(
                                                if (hasEvent) {
                                                    if (isSelected) onAccentColor else accentColor
                                                } else {
                                                    Color.Transparent
                                                },
                                                CircleShape
                                            )
                                    )
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
