package com.cinetrack.ui.components.updates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun UpdatesTimelineView(
    reminders: List<ReminderItem>,
    listState: LazyListState,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
    if (reminders.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.updates_no_reminders),
                color = Color.White.copy(alpha = 0.3f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    // Group reminders by YearMonth safely
    val groupedByMonth = remember(reminders) {
        reminders.groupBy { item ->
            try {
                if (item.arrivalDate.length >= 7) {
                    YearMonth.parse(item.arrivalDate.take(7))
                } else {
                    YearMonth.now()
                }
            } catch (_: Exception) {
                YearMonth.now()
            }
        }
    }

    val today = remember { LocalDate.now() }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Continuous lateral timeline track line spanning the entire screen height
        // Extends seamlessly all the way to the top behind the glass header
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .width(22.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.12f))
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            groupedByMonth.forEach { (yearMonth, monthReminders) ->
                // Month timeline pill header (Warm Amber/Gold for future releases)
                item(key = "reminders_month_${yearMonth}") {
                    RemindersMonthHeader(
                        yearMonth = yearMonth,
                        count = monthReminders.size
                    )
                }

                items(
                    items = monthReminders,
                    key = { it.id + "_rem" },
                    contentType = { "movie" }
                ) { item ->
                    // Dynamic urgency color on the timeline node:
                    // - Red for Today (0 days)
                    // - Warm Orange for Tomorrow (1 day)
                    // - Golden Amber for Upcoming
                    val nodeColor = remember(item.arrivalDate, today) {
                        try {
                            val target = LocalDate.parse(item.arrivalDate.take(10))
                            val days = ChronoUnit.DAYS.between(today, target)
                            when (days) {
                                0L -> Color(0xFFFF5252)
                                1L -> Color(0xFFFF9800)
                                else -> Color(0xFFF9A825)
                            }
                        } catch (_: Exception) {
                            Color(0xFFF9A825)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .animateItem(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lateral Timeline Node
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(nodeColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Reminder Card with vertical breathing room
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 5.dp)
                        ) {
                            UpdateCard(
                                movie = item.movie,
                                label = stringResource(R.string.updates_arriving_prefix, formatReleaseDate(item.arrivalDate)),
                                episodeBadge = item.episodeInfo,
                                rawDate = item.arrivalDate,
                                iconRes = R.drawable.ic_bell_piena,
                                color = MaterialTheme.colorScheme.primary,
                                isReminder = true,
                                onAction = {},
                                onPress = { onMovieClick(item.movie) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemindersMonthHeader(
    yearMonth: YearMonth,
    count: Int,
    modifier: Modifier = Modifier
) {
    val monthTitle = remember(yearMonth) {
        try {
            yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        } catch (_: Exception) {
            yearMonth.toString()
        }
    }

    // Warm Amber / Golden accent for Future Releases
    val accentColor = Color(0xFFF59E0B)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lateral timeline milestone node
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .border(2.dp, Color(0xFF0D0F14), CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Month Pill Capsule Infused with Warm Amber Accent Colors
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.10f))
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        fontSize = 14.5.sp,
                        letterSpacing = 0.3.sp
                    )
                )

                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.22f),
                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.50f))
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
