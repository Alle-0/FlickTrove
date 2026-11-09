package com.cinetrack.ui.history.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.ui.components.common.CategoryTabSelector
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.calendar.CalendarMonthWidget
import com.cinetrack.ui.components.updates.formatReleaseDate
import com.cinetrack.ui.history.model.WatchTimelineEntry
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HistoryCalendarView(
    calendarMonth: YearMonth,
    calendarEntriesByDate: Map<LocalDate, List<WatchTimelineEntry>>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onShowMonthPicker: () -> Unit,
    onMovieClick: (Long, String) -> Unit,
    onEditDate: (WatchTimelineEntry) -> Unit,
    onDeleteSession: (WatchTimelineEntry) -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
    onScopeChanged: ((Int) -> Unit)? = null,
    listState: LazyListState = rememberLazyListState()
) {
    // 0 = Vista Mese (default), 1 = Vista Anno (heatmap stile GitHub)
    var selectedScopeIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(selectedScopeIndex) {
        onScopeChanged?.invoke(selectedScopeIndex)
    }

    val topClearance = contentPadding.calculateTopPadding()
    val bottomClearance = contentPadding.calculateBottomPadding()
    val calendarHazeState = remember { HazeState() }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .haze(calendarHazeState, style = HazeStyles.PremiumDark),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = topClearance + 48.dp,
                bottom = bottomClearance
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Calendario Mensile OPPURE Heatmap Annuale ────────────────────
            item(key = "calendar_or_heatmap") {
                AnimatedContent(
                    targetState = selectedScopeIndex,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                    label = "CalendarOrHeatmapTransition"
                ) { scopeIndex ->
                    if (scopeIndex == 0) {
                        HistoryCalendarWidget(
                            currentMonth = calendarMonth,
                            selectedDate = selectedDate,
                            onDateSelected = onDateSelected,
                            onPreviousMonth = onPreviousMonth,
                            onNextMonth = onNextMonth,
                            onShowMonthPicker = onShowMonthPicker,
                            entriesByDate = calendarEntriesByDate,
                            internalHazeState = hazeState
                        )
                    } else {
                        HistoryYearHeatmapWidget(
                            currentYear = calendarMonth.year,
                            selectedDate = selectedDate,
                            onDateSelected = onDateSelected,
                            onPreviousYear = { onMonthChanged(calendarMonth.minusYears(1)) },
                            onNextYear = { onMonthChanged(calendarMonth.plusYears(1)) },
                            entriesByDate = calendarEntriesByDate
                        )
                    }
                }
            }

            // ── Dettaglio visioni del giorno selezionato ─────────────────────
            if (selectedDate != null) {
                val displayedEntries = calendarEntriesByDate[selectedDate].orEmpty()

                if (displayedEntries.isEmpty()) {
                    item(key = "empty_date_viewings") {
                        Text(
                            text = stringResource(R.string.history_no_entries_calendar),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    item(key = "header_date_viewings") {
                        Text(
                            text = stringResource(
                                R.string.history_calendar_entries_on_date,
                                formatReleaseDate(selectedDate.toString())
                            ),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }

                    items(
                        items = displayedEntries,
                        key = { "cal_entry_${it.uniqueKey}" }
                    ) { entry ->
                        HistoryCard(
                            entry = entry,
                            isCalendarDayMode = true,
                            onClick = { onMovieClick(entry.movieId, entry.mediaType) },
                            onEditDate = { onEditDate(entry) },
                            onDeleteSession = { onDeleteSession(entry) },
                            hazeState = hazeState,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            } else {
                item(key = "prompt_select_day") {
                    Text(
                        text = stringResource(R.string.history_calendar_select_prompt),
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ── Selettore Fisso in Alto (Floating Sticky Island stile HomeScreen / VistiScreen) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topClearance)
                .align(Alignment.TopCenter)
                .zIndex(10f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeGlass(
                            state = calendarHazeState,
                            shape = CircleShape,
                            useRefraction = true
                        )
                )
                CategoryTabSelector(
                    options = listOf(
                        stringResource(R.string.history_activity_scope_month),
                        stringResource(R.string.history_activity_scope_year)
                    ),
                    selectedIndex = selectedScopeIndex,
                    onOptionClick = { index -> selectedScopeIndex = index },
                    tabWidth = 100.dp
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 1. Calendario Mensile
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HistoryCalendarWidget(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onShowMonthPicker: () -> Unit,
    entriesByDate: Map<LocalDate, List<WatchTimelineEntry>>,
    internalHazeState: HazeState? = null
) {
    CalendarMonthWidget(
        currentMonth = currentMonth,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        onShowMonthPicker = onShowMonthPicker,
        hasIndicator = { entriesByDate.containsKey(it) },
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
