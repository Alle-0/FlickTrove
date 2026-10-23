package com.cinetrack.ui.components.stats

import com.cinetrack.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.theme.DarkSurface
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.viewmodel.TimeRange
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt
import com.cinetrack.ui.components.dialog.HomeFilterModal
import com.cinetrack.ui.components.shared.MorphGlassModal

// ════════════════════════════════════════════════════════════════════
// Improved Loading Skeleton
// ════════════════════════════════════════════════════════════════════

@Composable
fun StatsSkeleton(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top safe area spacer (same offset used by the loaded stats screen)
        Spacer(
            Modifier.height(
                paddingValues.calculateTopPadding() +
                    androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                    46.dp +
                    60.dp +
                    20.dp
            )
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {

            // Year selection placeholder (Title + Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(180.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
                Box(Modifier.width(100.dp).height(36.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect()) // Year picker
            }

            Spacer(Modifier.height(20.dp))

            // Wrapped Banner Pill Placeholder (Ticket Shape)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(TicketShape(cutoutRadius = 10.dp, cornerRadius = 18.dp))
                    .shimmerEffect()
            )

            Spacer(Modifier.height(24.dp))

            // Hero Card Skeleton (Total Time)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shimmerEffect()
            )

            Spacer(Modifier.height(36.dp))

            // Sections: Film and Series TV
            repeat(2) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
                Spacer(Modifier.height(14.dp))

                // Dual Pill
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(10.dp))

                // Media Time Card (The taller one)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(36.dp))
            }

            // Cast / Director Circles Skeleton
            repeat(2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).shimmerEffect())
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.width(140.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(4) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(72.dp).clip(CircleShape).shimmerEffect())
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.width(50.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        }
                    }
                }
            }

            // Genre Distribution Placeholder
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).shimmerEffect())
                Spacer(Modifier.width(14.dp))
                Box(Modifier.width(160.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shimmerEffect()
            )
            Spacer(Modifier.height(36.dp))

            // Decades Placeholder
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).shimmerEffect())
                Spacer(Modifier.width(14.dp))
                Box(Modifier.width(150.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .shimmerEffect()
            )
            Spacer(Modifier.height(36.dp))

            // Ratings Histogram Placeholder
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).shimmerEffect())
                Spacer(Modifier.width(14.dp))
                Box(Modifier.width(140.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shimmerEffect()
            )

            Spacer(Modifier.height(80.dp))
            Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
        }
    }
}

@Composable
fun YearSelectionButton(
    currentRange: TimeRange,
    onToggle: (Boolean, Rect?) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonCoords = remember { arrayOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .height(40.dp)
            .onGloballyPositioned { buttonCoords[0] = it }
            .bounceClick {
                val rect = buttonCoords[0]?.let {
                    val pos = it.positionInWindow()
                    Rect(
                        pos.x,
                        pos.y,
                        pos.x + it.size.width,
                        pos.y + it.size.height
                    )
                }
                onToggle(true, rect)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
        )
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_calendario),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (currentRange) {
                    is TimeRange.AllTime -> stringResource(R.string.stats_filter_all)
                    is TimeRange.Year -> currentRange.year.toString()
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Classic Centered Modal for Year Selection.
 * Standardized to match HomeFilterModal's glassmorphic design.
 */
@Composable
fun YearSelectionModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    currentRange: TimeRange,
    availableYears: List<Int>,
    hazeState: HazeState,
    triggerBounds: Rect? = null,
    titleRes: Int = R.string.stats_period,
    allTimeLabelRes: Int = R.string.stats_all_stats,
    onYearSelected: (Int) -> Unit,
    onAllTimeSelected: () -> Unit
) {
    MorphGlassModal(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        triggerBounds = triggerBounds,
        hazeState = hazeState,
        targetMaxWidth = 320.dp,
        targetWidthFraction = 0.85f,
        maxModalHeightFraction = 0.58f,
        minModalHeight = 220.dp,
        defaultModalHeight = 260.dp,
        zIndex = 200f
    ) { contentAlpha ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .graphicsLayer(
                    alpha = contentAlpha,
                    compositingStrategy = CompositingStrategy.Offscreen
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = Color.White
                )
            }

            // Scrollable Content
            val scrollState = rememberScrollState()
            val maxScrollHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.45f).coerceAtLeast(180.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxScrollHeight)
                    .verticalFadingEdges(
                        scrollState = scrollState,
                        topEdgeHeight = 16.dp,
                        bottomEdgeHeight = 16.dp
                    )
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                val isAllTime = currentRange is TimeRange.AllTime

                ModalItem(
                    label = stringResource(allTimeLabelRes),
                    isSelected = isAllTime,
                    onClick = {
                        onAllTimeSelected()
                        onDismiss()
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.1f)
                )

                availableYears.forEach { year ->
                    val isSelected = currentRange is TimeRange.Year &&
                        currentRange.year == year
                    ModalItem(
                        label = year.toString(),
                        isSelected = isSelected,
                        onClick = {
                            onYearSelected(year)
                            onDismiss()
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            // Close Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp)
                    .height(48.dp)
                    .bounceClick { onDismiss() }
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.settings_close).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ModalItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_tick),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
            )
        }
    }
}
