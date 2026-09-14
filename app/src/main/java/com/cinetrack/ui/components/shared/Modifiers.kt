package com.cinetrack.ui.components.shared

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

fun Modifier.shimmerEffect(): Modifier = composed {
    val advancedEffectsEnabled = com.cinetrack.LocalAdvancedVisualEffects.current
    if (!advancedEffectsEnabled) {
        return@composed this.background(Color(0xFF141419))
    }

    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    
    // Smooth, slow easing curve for a premium Glassmorphism feel
    val startOffsetX by transition.animateFloat(
        initialValue = -size.width.toFloat() * 1.5f,
        targetValue = size.width.toFloat() * 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val shimmerColors = remember {
        listOf(
            Color(0xFF0B0B0E), // Very dark base
            Color(0xFF16161D), // Subtle highlight
            Color(0xFF0B0B0E)  // Very dark base
        )
    }

    this.onGloballyPositioned { size = it.size }
        .drawBehind {
            if (size.width > 0) {
                // Soft, translucent glass-like gradient
                drawRect(
                    brush = Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(startOffsetX, 0f),
                        end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
                    )
                )
            }
        }
}
