package com.cinetrack.ui.components.shared.calendar

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

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

    var selectedYear by remember(showMonthPicker, initialMonth.year) { mutableIntStateOf(initialMonth.year) }
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
