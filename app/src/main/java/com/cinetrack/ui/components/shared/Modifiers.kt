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
            Color.White.copy(alpha = 0.03f),
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.03f)
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

@Composable
fun rememberShimmerBrush(
    targetValue: Float = 1200f,
    durationMillis: Int = 1600
): Brush {
    val advancedEffectsEnabled = com.cinetrack.LocalAdvancedVisualEffects.current
    if (!advancedEffectsEnabled) {
        return Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    }

    val transition = rememberInfiniteTransition(label = "unified_shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = -targetValue,
        targetValue = targetValue * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.03f),
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.03f)
        ),
        start = Offset(translateAnimation, translateAnimation * 0.5f),
        end = Offset(translateAnimation + targetValue, translateAnimation * 0.5f + targetValue)
    )
}
