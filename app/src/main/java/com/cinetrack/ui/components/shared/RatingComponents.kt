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
import kotlin.math.ceil
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.cinetrack.R

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
    val density = LocalDensity.current
    val gapPx = remember(density) { with(density) { 8.dp.toPx() } }
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
                    val newRating = calculateRatingFromX(offset.x, containerSize.width.toFloat(), maxRating, numStars, gapPx)
                    onRatingChange(newRating)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newRating = calculateRatingFromX(change.position.x, containerSize.width.toFloat(), maxRating, numStars, gapPx)
                    onRatingChange(newRating)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                    showGlow = showGlow,
                    index = index,
                    currentRating = rating,
                    starCenter = starStart + starValue / 2.0,
                    starValue = starValue
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
    showGlow: Boolean,
    index: Int = 0,
    currentRating: Double = 0.0,
    starCenter: Double = 0.0,
    starValue: Double = 2.0
) {
    // Entrance animation
    val entranceAlpha = remember { Animatable(0f) }
    val entranceScale = remember { Animatable(0.5f) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 60L) // Stagger
        launch { entranceAlpha.animateTo(1f, tween(200)) }
        launch { entranceScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
    }
    
    // Wave animation based on distance from current drag
    val distanceFromCurrent = kotlin.math.abs(currentRating - starCenter)
    val waveScale by animateFloatAsState(
        targetValue = if (distanceFromCurrent < starValue) {
            1f + (0.25f * (1f - (distanceFromCurrent / starValue).toFloat()))
        } else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "WaveScale"
    )

    Box(
        modifier = Modifier
            .size(starSize)
            .graphicsLayer {
                alpha = entranceAlpha.value
                scaleX = entranceScale.value * waveScale
                scaleY = entranceScale.value * waveScale
            },
        contentAlignment = Alignment.Center
    ) {
        // Base Star (Empty/Background)
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_star),
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
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = accentColor
                )
            }
        }
    }
}

private fun calculateRatingFromX(x: Float, totalWidth: Float, maxRating: Double, numStars: Int, gapPx: Float): Double {
    if (totalWidth <= 0f || numStars <= 0) return 0.0
    
    val starSize = (totalWidth - (numStars - 1) * gapPx) / numStars
    val starBlockWidth = starSize + gapPx
    
    val clampedX = x.coerceIn(0f, totalWidth)
    val starIndex = (clampedX / starBlockWidth).toInt().coerceIn(0, numStars - 1)
    val localX = clampedX - starIndex * starBlockWidth
    
    val fraction = (localX / starSize).coerceIn(0f, 1f)
    val starValue = maxRating / numStars
    val rawRating = (starIndex + fraction) * starValue
    
    return ((rawRating * 2).roundToInt() / 2.0).coerceIn(0.0, maxRating)
}
