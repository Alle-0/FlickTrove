package com.cinetrack.ui.history.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.history.model.WatchTimelineEntry
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryTimelineView(
    entries: List<WatchTimelineEntry>,
    onMovieClick: (movieId: Long, mediaType: String) -> Unit,
    onEditDate: (entry: WatchTimelineEntry) -> Unit,
    onDeleteSession: (entry: WatchTimelineEntry) -> Unit,
    topBarClearance: androidx.compose.ui.unit.Dp = 0.dp,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
    if (entries.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.history_no_entries_filter),
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
        return
    }

    val groupedByMonth = remember(entries) {
        entries.groupBy { YearMonth.from(it.parsedDate) }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Continuous lateral timeline track line spanning the entire screen height
        // Extends seamlessly all the way to the top behind the GlassyTopBar and status bar
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            groupedByMonth.forEach { (yearMonth, monthEntries) ->
                // Clean month timeline pill header (scrolls naturally with cards)
                item(key = "month_${yearMonth}") {
                    MonthTimelineHeader(
                        yearMonth = yearMonth,
                        count = monthEntries.size
                    )
                }

                // Entries for this month with seamless lateral timeline track
                items(
                    items = monthEntries,
                    key = { "timeline_entry_${it.historyId}" }
                ) { entry ->
                    val sessionColor = if (entry.isTvShow) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
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
                            // Card session dot node
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(sessionColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // History Card with padded vertical breathing room
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 5.dp)
                        ) {
                            HistoryCard(
                                entry = entry,
                                isCalendarDayMode = false,
                                onClick = { onMovieClick(entry.movieId, entry.mediaType) },
                                onEditDate = { onEditDate(entry) },
                                onDeleteSession = { onDeleteSession(entry) },
                                hazeState = hazeState,
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
private fun MonthTimelineHeader(
    yearMonth: YearMonth,
    count: Int,
    modifier: Modifier = Modifier
) {
    val monthTitle = remember(yearMonth) {
        try {
            yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        } catch (e: Exception) {
            yearMonth.toString()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lateral timeline node for Month
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Month Milestone Node
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, Color(0xFF0D0F14), CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Month Pill Capsule
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .clip(CircleShape)
                .background(Color(0xFF14171E))
                .border(
                    width = 0.8.dp,
                    color = Color.White.copy(alpha = 0.14f),
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
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        letterSpacing = 0.2.sp
                    )
                )

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 11.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
