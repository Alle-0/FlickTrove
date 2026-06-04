package com.cinetrack.ui.components.shared

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.ui.theme.HazeStyles

@Composable
fun CategoryPill(
    activeTab: String, // "movie" or "tv"
    onTabChange: (String) -> Unit,
    onFilterPress: (() -> Unit)? = null,
    hasActiveFilters: Boolean = false,
    movieCount: Int? = null,
    tvCount: Int? = null,
    gridColumns: Int = 3,
    onGridColumnsChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Tab Switcher
        Box(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .glassmorphic(RoundedCornerShape(50), blurRadius = HazeStyles.SmallGlassBlurRadius)
                .onGloballyPositioned { containerSize = it.size }
        ) {
            val itemWidth = if (containerSize.width > 0) {
                with(density) { (containerSize.width / 2).toDp() }
            } else 0.dp

            val indicatorOffset by animateDpAsState(
                targetValue = if (activeTab == "tv") itemWidth else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                label = "pillIndicator"
            )

            // Animated Indicator
            if (itemWidth > 6.dp) {
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .offset(x = indicatorOffset)
                        .width(itemWidth - 6.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                        .border(1.dp, HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop), RoundedCornerShape(50))
                )
            }

            // Tab Buttons
            Row(modifier = Modifier.fillMaxSize()) {
                TabButton(
                    text = "FILM",
                    count = movieCount,
                    isActive = activeTab == "movie",
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabChange("movie")
                    }
                )
                TabButton(
                    text = "SERIE TV",
                    count = tvCount,
                    isActive = activeTab == "tv",
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabChange("tv")
                    }
                )
            }
        }

        // Layout Toggle & Filter
        if (onFilterPress != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onGridColumnsChange != null) {
                    IconButton(
                        onClick = {
                            val next = nextGridColumns(gridColumns)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGridColumnsChange(next)
                        }
                    ) {
                        val icon = layoutToggleIcon(gridColumns)
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFilterPress()
                    },
                    borderColor = if (hasActiveFilters) accentColor else HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
                    borderWidth = if (hasActiveFilters) 1.5.dp else 1.2.dp
                ) {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_filtri),
                        null,
                        tint = if (hasActiveFilters) accentColor else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    count: Int?,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (isActive) accentColor else Color.White.copy(alpha = 0.45f),
        label = "tabText"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            if (count != null && count > 0) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 20.dp)
                        .height(20.dp)
                        .background(
                            if (isActive) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                            RoundedCornerShape(50)
                        )
                        .border(
                            1.dp,
                            if (isActive) accentColor.copy(alpha = HazeStyles.GlassBorderAlphaActive) else HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        color = textColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun IconButton(
    onClick: () -> Unit,
    borderColor: Color = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .glassmorphic(RoundedCornerShape(50), blurRadius = HazeStyles.SmallGlassBlurRadius, borderBrush = SolidColor(borderColor), borderWidth = borderWidth)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
