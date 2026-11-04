package com.cinetrack.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.history.model.WatchTimelineEntry
import com.cinetrack.ui.utils.bounceClick
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════════════
// 1. Modello Dati Immutabile per le Statistiche della Heatmap
// ══════════════════════════════════════════════════════════════════════════════

@Immutable
data class HeatmapStats(
    val totalWatches: Int,
    val activeDays: Int,
    val totalDaysInYear: Int,
    val displayStreak: Int,
    val streakDates: Set<LocalDate>,
    val isCurrentStreak: Boolean
)

@Composable
fun rememberHeatmapStats(
    currentYear: Int,
    entriesByDate: Map<LocalDate, List<WatchTimelineEntry>>
): HeatmapStats {
    val today = remember { LocalDate.now() }

    return remember(currentYear, entriesByDate) {
        val yearEntries = entriesByDate.filterKeys { it.year == currentYear }
        val activeDays = yearEntries.filterValues { it.isNotEmpty() }.size
        val totalWatches = yearEntries.values.flatten().sumOf { entry: WatchTimelineEntry ->
            entry.sessionEpisodes?.values?.sumOf { it.size } ?: 1
        }
        val totalDaysInYear = if (LocalDate.of(currentYear, 1, 1).isLeapYear) 366 else 365

        val allValidDates = entriesByDate.filterValues { it.isNotEmpty() }.keys.sorted()

        // 1. Streak attuale consecutiva che tocca oggi o ieri
        val currRun = mutableListOf<LocalDate>()
        var checkDate = today
        if (!allValidDates.contains(checkDate)) {
            checkDate = today.minusDays(1)
        }
        while (allValidDates.contains(checkDate)) {
            currRun.add(checkDate)
            checkDate = checkDate.minusDays(1)
        }

        // 2. Miglior streak dell'anno selezionato
        val yearDates = allValidDates.filter { it.year == currentYear }
        var longestRun = emptyList<LocalDate>()
        var tempRun = mutableListOf<LocalDate>()
        var prevD: LocalDate? = null

        for (d in yearDates) {
            if (prevD != null && d == prevD.plusDays(1)) {
                tempRun.add(d)
            } else {
                tempRun = mutableListOf(d)
            }
            if (tempRun.size > longestRun.size) {
                longestRun = tempRun.toList()
            }
            prevD = d
        }

        // 3. Selezione streak da mostrare: priorità alla Longest Streak (Miglior serie dell'anno).
        // Se la streak attuale eguaglia o supera il record dell'anno, viene segnalata come serie attuale.
        val isCurrent = currentYear == today.year && currRun.isNotEmpty() && currRun.size >= longestRun.size
        val (displayStreak, streakDates) = if (isCurrent) {
            currRun.size to currRun.toSet()
        } else {
            longestRun.size to longestRun.toSet()
        }

        HeatmapStats(
            totalWatches = totalWatches,
            activeDays = activeDays,
            totalDaysInYear = totalDaysInYear,
            displayStreak = displayStreak,
            streakDates = streakDates,
            isCurrentStreak = isCurrent
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 2. Heatmap Annuale Stile GitHub (Watch Activity)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun HistoryYearHeatmapWidget(
    currentYear: Int,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    entriesByDate: Map<LocalDate, List<WatchTimelineEntry>>,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val horizontalScrollState = rememberScrollState()
    val cardBg = Color(0xFF14171E)

    val stats = rememberHeatmapStats(currentYear = currentYear, entriesByDate = entriesByDate)
    val today = remember { LocalDate.now() }

    // Struttura griglia 53 settimane Lun-Dom
    val startDate = remember(currentYear) { LocalDate.of(currentYear, 1, 1) }
    val endDate = remember(currentYear) { LocalDate.of(currentYear, 12, 31) }
    val firstMonday = remember(startDate) {
        var d = startDate
        while (d.dayOfWeek != DayOfWeek.MONDAY) {
            d = d.minusDays(1)
        }
        d
    }
    val weeksCount = remember(firstMonday, endDate) {
        val totalDays = ChronoUnit.DAYS.between(firstMonday, endDate) + 1
        ((totalDays + 6) / 7).toInt()
    }

    // Auto-scroll alla settimana corrente se siamo nell'anno corrente
    LaunchedEffect(currentYear) {
        if (today.year == currentYear) {
            val weekIndex = ChronoUnit.WEEKS.between(firstMonday, today).toInt().coerceIn(0, weeksCount - 1)
            val initialScrollPx = with(density) {
                (16.dp * (weekIndex - 5).coerceAtLeast(0)).toPx().toInt()
            }
            horizontalScrollState.scrollTo(initialScrollPx)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(cardBg.copy(alpha = 0.35f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(36.dp)
            )
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Column {
            // ── 1. Stepper Anno (< 2026 >) ──────────────────────────────────
            HeatmapYearStepper(
                currentYear = currentYear,
                onPreviousYear = onPreviousYear,
                onNextYear = onNextYear
            )

            // ── 2. Statistiche Hero & Pillole Streak / Giorni ────────────────
            HeatmapHeroSection(
                stats = stats,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                onStreakClick = {
                    if (stats.streakDates.isNotEmpty()) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val firstStreakDate = stats.streakDates.minOrNull() ?: today
                        val streakWeekIndex = ChronoUnit.WEEKS.between(firstMonday, firstStreakDate).toInt().coerceIn(0, weeksCount - 1)
                        val targetScrollPx = with(density) {
                            (16.dp * (streakWeekIndex - 4).coerceAtLeast(0)).toPx().toInt()
                        }
                        coroutineScope.launch {
                            horizontalScrollState.animateScrollTo(targetScrollPx)
                        }
                        onDateSelected(firstStreakDate)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 3. Griglia delle 53 Settimane con Dissolvenza Alpha ──────────
            HeatmapGrid(
                currentYear = currentYear,
                firstMonday = firstMonday,
                weeksCount = weeksCount,
                entriesByDate = entriesByDate,
                selectedDate = selectedDate,
                streakDates = stats.streakDates,
                primaryColor = primaryColor,
                horizontalScrollState = horizontalScrollState,
                onDateSelected = { date ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDateSelected(date)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── 4. Legenda Heatmap (0  ■ ■ ■ ■ ■  6+) ─────────────────────────
            HeatmapLegend(primaryColor = primaryColor)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 3. Sub-Componenti Modulari
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HeatmapYearStepper(
    currentYear: Int,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
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
                    onPreviousYear()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_left),
                contentDescription = "Anno precedente",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 18.dp, vertical = 7.dp)
        ) {
            Text(
                text = currentYear.toString(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .bounceClick(scaleDown = 0.8f) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNextYear()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                contentDescription = "Anno successivo",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun HeatmapHeroSection(
    stats: HeatmapStats,
    primaryColor: Color,
    secondaryColor: Color,
    onStreakClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        // Numero Visioni Hero
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stats.totalWatches.toString(),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            Text(
                text = stringResource(
                    id = R.string.panorama_watches_across_days,
                    stats.activeDays
                ),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pillole Streak & Giorni Attivi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Streak Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = primaryColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
                modifier = Modifier
                    .weight(1f)
                    .bounceClick(scaleDown = 0.96f, onClick = onStreakClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_bolt),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(id = R.string.panorama_day_streak, stats.displayStreak),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = if (stats.isCurrentStreak) {
                                stringResource(id = R.string.panorama_current_streak_label)
                            } else {
                                stringResource(id = R.string.panorama_best_streak_label)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.60f)
                        )
                    }
                }
            }

            // Active Days Ratio Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                        contentDescription = null,
                        tint = secondaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(id = R.string.panorama_active_days_ratio, stats.activeDays, stats.totalDaysInYear),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = stringResource(id = R.string.panorama_active_days_label),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.60f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapGrid(
    currentYear: Int,
    firstMonday: LocalDate,
    weeksCount: Int,
    entriesByDate: Map<LocalDate, List<WatchTimelineEntry>>,
    selectedDate: LocalDate?,
    streakDates: Set<LocalDate>,
    primaryColor: Color,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Colonna fissa con tutte le 7 iniziali dei giorni (Lun..Dom)
        Column(
            modifier = Modifier.padding(end = 6.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            for (dayIndex in 0..6) {
                val d = DayOfWeek.of(dayIndex + 1)
                val narrowLabel = d.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = narrowLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = if (dayIndex % 2 == 0) 0.55f else 0.35f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.offset(y = (-1.5).dp)
                    )
                }
            }
        }

        // Area scrollabile con vera dissolvenza alpha ad accelerazione hardware
        Box(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    val fadeWidth = 28.dp.toPx()
                    val w = size.width
                    if (w > fadeWidth * 2) {
                        val scroll = horizontalScrollState.value.toFloat()
                        val maxScroll = horizontalScrollState.maxValue.toFloat().coerceAtLeast(1f)
                        val leftAlpha = (scroll / fadeWidth).coerceIn(0f, 1f)
                        val rightAlpha = ((maxScroll - scroll) / fadeWidth).coerceIn(0f, 1f)

                        drawRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 1f - leftAlpha),
                                    (fadeWidth / w) to Color.Black,
                                    (1f - fadeWidth / w) to Color.Black,
                                    1.0f to Color.Black.copy(alpha = 1f - rightAlpha)
                                ),
                                startX = 0f,
                                endX = w
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column {
                    // Intestazione Mesi (Gen, Feb, Mar...)
                    Row(
                        modifier = Modifier
                            .height(18.dp)
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (w in 0 until weeksCount) {
                            val weekStartDate = firstMonday.plusWeeks(w.toLong())
                            val isFirstWeekOfMonth = weekStartDate.dayOfMonth <= 7
                            Box(
                                modifier = Modifier.width(13.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (isFirstWeekOfMonth && weekStartDate.year == currentYear) {
                                    val monthName = weekStartDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                    Text(
                                        text = monthName,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.wrapContentSize(unbounded = true, align = Alignment.CenterStart)
                                    )
                                }
                            }
                        }
                    }

                    // 7 righe (Lun-Dom)
                    for (dayOfWeek in 0..6) {
                        Row(
                            modifier = Modifier.height(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (w in 0 until weeksCount) {
                                val date = firstMonday.plusWeeks(w.toLong()).plusDays(dayOfWeek.toLong())
                                val isInsideYear = date.year == currentYear
                                val entriesOnDate = if (isInsideYear) entriesByDate[date].orEmpty() else emptyList()
                                val count = if (isInsideYear) entriesOnDate.sumOf { entry: WatchTimelineEntry -> entry.sessionEpisodes?.values?.sumOf { it.size } ?: 1 } else -1
                                val isSelected = selectedDate == date
                                val isStreakDay = isInsideYear && streakDates.contains(date)

                                if (!isInsideYear) {
                                    Spacer(modifier = Modifier.size(13.dp))
                                } else {
                                    val cellColor = when {
                                        count <= 0 -> Color.White.copy(alpha = 0.05f)
                                        count == 1 -> primaryColor.copy(alpha = 0.35f)
                                        count in 2..3 -> primaryColor.copy(alpha = 0.60f)
                                        count in 4..5 -> primaryColor.copy(alpha = 0.82f)
                                        else -> primaryColor
                                    }

                                    val borderColor = when {
                                        isSelected -> Color.White
                                        isStreakDay -> primaryColor
                                        count > 0 -> primaryColor.copy(alpha = 0.40f)
                                        else -> Color.White.copy(alpha = 0.08f)
                                    }

                                    val borderWidth = when {
                                        isSelected -> 1.5.dp
                                        isStreakDay -> 1.5.dp
                                        else -> 0.5.dp
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(cellColor)
                                            .border(
                                                width = borderWidth,
                                                color = borderColor,
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                onDateSelected(date)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isStreakDay) {
                                            Box(
                                                modifier = Modifier
                                                    .size(3.5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (count >= 2) Color.White else primaryColor)
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
    }
}

@Composable
private fun HeatmapLegend(
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "0",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = Color.White.copy(alpha = 0.40f),
            modifier = Modifier.padding(end = 4.dp)
        )
        Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.05f)))
        Spacer(modifier = Modifier.width(3.dp))
        Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(primaryColor.copy(alpha = 0.35f)))
        Spacer(modifier = Modifier.width(3.dp))
        Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(primaryColor.copy(alpha = 0.60f)))
        Spacer(modifier = Modifier.width(3.dp))
        Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(primaryColor.copy(alpha = 0.82f)))
        Spacer(modifier = Modifier.width(3.dp))
        Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(primaryColor))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "6+",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = Color.White.copy(alpha = 0.40f)
        )
    }
}
