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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.history.model.WatchTimelineEntry
import kotlinx.coroutines.delay
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

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

    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val animatedEntryIds = remember { mutableSetOf<Long>() }

    val groupedByMonth = remember(entries) {
        entries.groupBy { YearMonth.from(it.parsedDate) }
    }

    // Stable global index mapping for clean, predictable stagger ordering
    val entryIndexMap = remember(entries) {
        entries.mapIndexed { idx, it -> it.historyId to idx }.toMap()
    }

    // Micro-Haptic on Month Scroll Crossing:
    // Fires an ultra-crisp tactile tick (analog watch bezel feel) when a month milestone capsule crosses near the top
    val currentTopMonth by remember {
        derivedStateOf {
            val visible = listState.layoutInfo.visibleItemsInfo
            visible.lastOrNull { it.key.toString().startsWith("month_") && it.offset <= 180 }?.key as? String
                ?: visible.firstOrNull { it.key.toString().startsWith("month_") }?.key as? String
        }
    }
    var lastHapticMonth by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(currentTopMonth) {
        if (listState.isScrollInProgress && currentTopMonth != null && currentTopMonth != lastHapticMonth) {
            lastHapticMonth = currentTopMonth
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
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
            state = listState,
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

                // Entries for this month with staggered entry animation
                items(
                    items = monthEntries,
                    key = { "timeline_entry_${it.historyId}" }
                ) { entry ->
                    val staggerIndex = entryIndexMap[entry.historyId] ?: 0
                    TimelineEntryRow(
                        entry = entry,
                        staggerIndex = staggerIndex,
                        animatedEntryIds = animatedEntryIds,
                        onMovieClick = onMovieClick,
                        onEditDate = onEditDate,
                        onDeleteSession = onDeleteSession,
                        hazeState = hazeState
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun androidx.compose.foundation.lazy.LazyItemScope.TimelineEntryRow(
    entry: WatchTimelineEntry,
    staggerIndex: Int,
    animatedEntryIds: MutableSet<Long>,
    onMovieClick: (movieId: Long, mediaType: String) -> Unit,
    onEditDate: (entry: WatchTimelineEntry) -> Unit,
    onDeleteSession: (entry: WatchTimelineEntry) -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState?,
    modifier: Modifier = Modifier
) {
    val sessionColor = if (entry.isTvShow) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    val density = LocalDensity.current

    // Canonical FlickTrove card entrance animation
    val hasAnimated = rememberSaveable(entry.historyId) {
        mutableStateOf(animatedEntryIds.contains(entry.historyId))
    }

    androidx.compose.runtime.LaunchedEffect(entry.historyId) {
        if (hasAnimated.value) return@LaunchedEffect
        if (staggerIndex in 0..11) delay(staggerIndex * 40L)
        hasAnimated.value = true
        animatedEntryIds.add(entry.historyId)
    }

    val isScrollItem = staggerIndex < 0 || staggerIndex >= 12
    val isVisible = hasAnimated.value || staggerIndex < 0

    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isScrollItem) 180 else 250,
            easing = LinearOutSlowInEasing
        ),
        label = "alpha"
    )

    val cardTranslateY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 12f,
        animationSpec = if (isScrollItem) tween(200, easing = FastOutSlowInEasing) else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "translateY"
    )

    val nodeScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = if (isScrollItem) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nodeScale"
    )

    val dayStr = remember(entry.parsedDate) {
        entry.parsedDate.dayOfMonth.toString()
    }
    val monthStr = remember(entry.parsedDate) {
        try {
            entry.parsedDate.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
                .uppercase(Locale.getDefault())
                .replace(".", "")
        } catch (e: Exception) {
            ""
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .animateItem(),
        verticalAlignment = Alignment.Top
    ) {
        // Lateral Timeline Node with bouncy spring scale
        Box(
            modifier = Modifier
                .width(22.dp)
                .padding(top = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            // Card session dot node
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = nodeScale
                        scaleY = nodeScale
                        alpha = nodeScale.coerceIn(0f, 1f)
                    }
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(sessionColor)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Editorial Date Column: Day (big, bold) + Month Abbreviation (subtle uppercase)
        Column(
            modifier = Modifier
                .width(36.dp)
                .padding(top = 9.dp)
                .graphicsLayer {
                    alpha = cardAlpha
                    translationY = cardTranslateY * density.density
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayStr,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    lineHeight = 18.sp,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = monthStr,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    letterSpacing = 0.6.sp,
                    color = sessionColor.copy(alpha = 0.90f)
                ),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // History Card with progressive staggered slide + fade (Date now cleanly outside in column)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 5.dp)
                .graphicsLayer {
                    alpha = cardAlpha
                    translationY = cardTranslateY * density.density
                }
        ) {
            HistoryCard(
                entry = entry,
                isCalendarDayMode = false,
                showDate = false,
                onClick = { onMovieClick(entry.movieId, entry.mediaType) },
                onEditDate = { onEditDate(entry) },
                onDeleteSession = { onDeleteSession(entry) },
                hazeState = hazeState,
                modifier = Modifier.fillMaxWidth()
            )
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

    val accentColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 8.dp),
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
                    .background(accentColor)
                    .border(2.dp, Color(0xFF0D0F14), CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Month Pill Capsule without gradient (flat clean accent)
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
                            color = accentColor,
                            fontSize = 11.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.5.dp)
                    )
                }
            }
        }
    }
}
