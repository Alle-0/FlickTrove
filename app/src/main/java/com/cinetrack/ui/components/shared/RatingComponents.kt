package com.cinetrack.ui.components.shared

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

@Composable
fun FluidRatingBar(
    rating: Double,
    onRatingChange: (Double) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    starSize: Dp = 48.dp,
    numStars: Int = 5,
    maxRating: Double = 10.0,
    showGlow: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Animation for smooth sliding
    val animatedRating by animateFloatAsState(
        targetValue = rating.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy, 
            stiffness = Spring.StiffnessLow
        ),
        label = "RatingAnimation"
    )

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newRating = calculateRatingFromX(offset.x, containerSize.width.toFloat(), maxRating)
                    if (rating != newRating) {
                        onRatingChange(newRating)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val newRating = calculateRatingFromX(change.position.x, containerSize.width.toFloat(), maxRating)
                    if (rating != newRating) {
                        onRatingChange(newRating)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(numStars) { index ->
                val starValue = maxRating / numStars
                val starStart = index * starValue
                // Fill ratio for this specific star
                val fillRatio = ((animatedRating - starStart.toFloat()) / starValue.toFloat()).coerceIn(0f, 1f)
                
                PremiumStar(
                    fillRatio = fillRatio,
                    accentColor = accentColor,
                    starSize = starSize,
                    showGlow = showGlow
                )
            }
        }
    }
}

@Composable
fun PremiumStar(
    fillRatio: Float,
    accentColor: Color,
    starSize: Dp,
    showGlow: Boolean
) {
    Box(
        modifier = Modifier.size(starSize),
        contentAlignment = Alignment.Center
    ) {
        // Base Star (Empty/Background)
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            tint = Color.White.copy(alpha = 0.1f)
        )
        
        // Active Star (Filled with clipping)
        if (fillRatio > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        clip = true
                        shape = object : Shape {
                            override fun createOutline(
                                size: Size,
                                layoutDirection: LayoutDirection,
                                density: Density
                            ): Outline {
                                return Outline.Rectangle(
                                    Rect(0f, 0f, size.width * fillRatio, size.height)
                                )
                            }
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = accentColor
                )
            }
        }
    }
}

private fun calculateRatingFromX(x: Float, totalWidth: Float, maxRating: Double): Double {
    val rawRating = (x / totalWidth) * maxRating
    // Round to nearest 0.5 for a 10-point scale
    val stepped = (rawRating * 2.0).roundToInt() / 2.0
    return stepped.coerceIn(0.0, maxRating)
}
