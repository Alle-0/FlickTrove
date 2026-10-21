package com.cinetrack.ui.history.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.history.model.WatchTimelineEntry
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState
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
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    hazeState: HazeState? = null
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        groupedByMonth.forEach { (yearMonth, monthEntries) ->
            // Sticky floating pill for each Month
            stickyHeader(key = "month_${yearMonth}") {
                MonthSectionHeader(
                    yearMonth = yearMonth,
                    count = monthEntries.size,
                    hazeState = hazeState
                )
            }

            // Entries for this month
            items(
                items = monthEntries,
                key = { "timeline_entry_${it.historyId}" }
            ) { entry ->
                HistoryCard(
                    entry = entry,
                    isCalendarDayMode = false,
                    onClick = { onMovieClick(entry.movieId, entry.mediaType) },
                    onEditDate = { onEditDate(entry) },
                    onDeleteSession = { onDeleteSession(entry) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun MonthSectionHeader(
    yearMonth: YearMonth,
    count: Int,
    hazeState: HazeState?,
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .hazeGlass(
                    state = hazeState,
                    shape = CircleShape,
                    blurRadius = HazeStyles.SmallGlassBlurRadius,
                    containerColor = Color(0xFF101418).copy(alpha = 0.70f),
                    borderColor = Color.White.copy(alpha = 0.14f),
                    borderWidth = 0.8.dp
                )
                .padding(horizontal = 16.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                )
            }

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f))
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.5.dp)
                )
            }
        }
    }
}
