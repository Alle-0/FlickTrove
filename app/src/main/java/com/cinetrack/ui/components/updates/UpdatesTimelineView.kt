package com.cinetrack.ui.components.updates

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.bounceClick
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

    var collapsedMonths by rememberSaveable {
        mutableStateOf(listOf<String>())
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
    val haptic = LocalHapticFeedback.current
    val animatedEntryIds = remember { mutableSetOf<String>() }

    // Stable global index mapping for clean, predictable stagger ordering
    val entryIndexMap = remember(reminders) {
        reminders.mapIndexed { idx, it -> it.id to idx }.toMap()
    }

    // Micro-Haptic on Month Scroll Crossing:
    // Fires an ultra-crisp tactile tick (analog watch bezel feel) when a month milestone capsule crosses near the top
    val currentTopMonth by remember {
        derivedStateOf {
            val visible = listState.layoutInfo.visibleItemsInfo
            visible.lastOrNull { it.key.toString().startsWith("reminders_month_") && it.offset <= 180 }?.key as? String
                ?: visible.firstOrNull { it.key.toString().startsWith("reminders_month_") }?.key as? String
        }
    }
    var lastHapticMonth by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentTopMonth) {
        if (listState.isScrollInProgress && currentTopMonth != null && currentTopMonth != lastHapticMonth) {
            lastHapticMonth = currentTopMonth
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

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
                val monthKey = yearMonth.toString()
                val isCollapsed = monthKey in collapsedMonths

                // Month timeline pill header (Warm Amber/Gold for future releases)
                item(key = "reminders_month_${yearMonth}") {
                    RemindersMonthHeader(
                        yearMonth = yearMonth,
                        count = monthReminders.size,
                        isCollapsed = isCollapsed,
                        onToggleCollapse = {
                            collapsedMonths = if (isCollapsed) {
                                collapsedMonths - monthKey
                            } else {
                                collapsedMonths + monthKey
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                if (!isCollapsed) {
                    items(
                        items = monthReminders,
                        key = { it.id + "_rem" },
                        contentType = { "movie" }
                    ) { item ->
                        val staggerIndex = entryIndexMap[item.id] ?: 0
                        RemindersEntryRow(
                            item = item,
                            staggerIndex = staggerIndex,
                            animatedEntryIds = animatedEntryIds,
                            today = today,
                            onMovieClick = onMovieClick
                        )
                    }
                }
        }
    }
}
}

@Composable
private fun androidx.compose.foundation.lazy.LazyItemScope.RemindersEntryRow(
    item: ReminderItem,
    staggerIndex: Int,
    animatedEntryIds: MutableSet<String>,
    today: LocalDate,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
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

    val density = LocalDensity.current

    // Canonical FlickTrove card entrance animation
    val hasAnimated = rememberSaveable(item.id) {
        mutableStateOf(animatedEntryIds.contains(item.id))
    }

    LaunchedEffect(item.id) {
        if (hasAnimated.value) return@LaunchedEffect
        if (staggerIndex in 0..11) delay(staggerIndex * 40L)
        hasAnimated.value = true
        animatedEntryIds.add(item.id)
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .animateItem(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lateral Timeline Node with bouncy spring scale
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = nodeScale
                        scaleY = nodeScale
                        alpha = nodeScale.coerceIn(0f, 1f)
                    }
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(nodeColor)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Reminder Card with vertical breathing room and progressive staggered slide + fade
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 5.dp)
                .graphicsLayer {
                    alpha = cardAlpha
                    translationY = cardTranslateY * density.density
                }
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

@Composable
private fun RemindersMonthHeader(
    yearMonth: YearMonth,
    count: Int,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
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

    val chevronRotation by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "reminders_chevron_rotation"
    )

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
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggleCollapse() },
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
                .bounceClick(scaleDown = 0.98f) { onToggleCollapse() }
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.10f))
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .padding(start = 16.dp, end = 12.dp, top = 9.dp, bottom = 9.dp)
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
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                        contentDescription = stringResource(if (isCollapsed) R.string.search_expand else R.string.search_collapse),
                        tint = accentColor.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(14.dp)
                            .graphicsLayer { rotationZ = chevronRotation }
                    )
                }
            }
        }
    }
}
