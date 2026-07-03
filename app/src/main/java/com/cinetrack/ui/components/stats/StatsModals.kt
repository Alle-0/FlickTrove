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
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.theme.DarkSurface
import com.cinetrack.ui.viewmodel.TimeRange
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

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
                Box(Modifier.width(100.dp).height(36.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
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
    onYearSelected: (Int) -> Unit,
    onAllTimeSelected: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val targetWidth = (screenWidth * 0.85f).coerceAtMost(with(density) { 320.dp.toPx() })
    
    var contentHeightPx by remember { mutableStateOf(0f) }
    val topSafetyPx = with(density) { 96.dp.toPx() }
    val bottomSafetyPx = with(density) { 56.dp.toPx() }
    val maxAllowedHeight = screenHeight - topSafetyPx - bottomSafetyPx
    
    val targetHeightPx by animateFloatAsState(
        targetValue = if (contentHeightPx > 0) contentHeightPx.coerceIn(with(density) { 300.dp.toPx() }, maxAllowedHeight) 
                      else screenHeight * 0.4f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dynamicHeight"
    )

    val targetRect = Rect(
        left = (screenWidth - targetWidth) / 2f,
        top = (screenHeight - targetHeightPx) / 2f,
        right = (screenWidth + targetWidth) / 2f,
        bottom = (screenHeight + targetHeightPx) / 2f
    )

    val transition = updateTransition(targetState = isVisible, label = "YearModalTransition")

    androidx.activity.compose.BackHandler(enabled = isVisible) {
        onDismiss()
    }

    val progress by transition.animateFloat(
        transitionSpec = {
            if (initialState == false && targetState == true) {
                spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
            } else {
                spring(stiffness = Spring.StiffnessMedium)
            }
        },
        label = "expansionProgress"
    ) { state -> if (state) 1f else 0f }

    val alpha by transition.animateFloat(label = "scrimAlpha") { state -> if (state) 1f else 0f }

    if (transition.currentState || transition.targetState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(200f)
                .graphicsLayer(alpha = alpha)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            // --- GHOST MEASUREMENT LAYER ---
            Box(
                modifier = Modifier
                    .width(with(density) { targetWidth.toDp() })
                    .alpha(0f)
                    .onSizeChanged { size ->
                        if (size.height > 0) contentHeightPx = size.height.toFloat()
                    }
                    .align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(stringResource(R.string.stats_period), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(24.dp))
                    ModalItem(stringResource(R.string.stats_all_time), false, {})
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Column {
                        availableYears.forEach { year ->
                            ModalItem(year.toString(), false, {})
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(48.dp))
                }
            }

            val startRect = triggerBounds ?: targetRect.copy(
                left = targetRect.center.x - 20f,
                top = targetRect.center.y - 20f,
                right = targetRect.center.x + 20f,
                bottom = targetRect.center.y + 20f
            )

            val currentRect = lerp(startRect, targetRect, progress)
            val currentCornerRadius = androidx.compose.ui.util.lerp(
                if (triggerBounds != null) startRect.width / 2f else with(density) { 32.dp.toPx() },
                with(density) { 32.dp.toPx() },
                progress
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(currentRect.left.roundToInt(), currentRect.top.roundToInt()) }
                    .size(
                        width = with(density) { currentRect.width.toDp() },
                        height = with(density) { currentRect.height.toDp() }
                    )
                    .bounceClick(scaleDown = 1f) { /* Prevent dismissal */ }
            ) {
                // Background Layer (Blurred glass)
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeGlass(
                            state = hazeState,
                            shape = RoundedCornerShape(with(density) { currentCornerRadius.toDp() }),
                            containerColor = DarkSurface.copy(alpha = 0.45f),
                            useOffscreenStrategy = false
                        )
                )

                // Foreground Content
                if (progress > 0.4f) {
                    val contentAlpha = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)

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
                                text = stringResource(R.string.stats_period),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp,
                                color = Color.White
                            )
                        }

                        // Scrollable Content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                        ) {
                            val isAllTime = currentRange is TimeRange.AllTime
                            
                            ModalItem(
                                label = stringResource(R.string.stats_all_stats),
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
                            
                            Spacer(Modifier.height(12.dp))
                        }

                        // Close Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
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
