package com.cinetrack.ui.components.shared

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt
import kotlin.math.ceil
import kotlin.math.pow
import kotlinx.coroutines.launch
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
    showGlow: Boolean = false,
    onRatingSettled: ((Double) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val gapPx = remember(density) { with(density) { 8.dp.toPx() } }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val currentOnRatingChange by rememberUpdatedState(onRatingChange)
    val currentOnRatingSettled by rememberUpdatedState(onRatingSettled)

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
            .pointerInput(numStars, maxRating, isRtl) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = containerSize.width.toFloat()
                    if (w <= 0f) return@awaitEachGesture
                    
                    down.consume()

                    var curRating = calculateRatingFromX(down.position.x, w, maxRating, numStars, gapPx, isRtl)
                    currentOnRatingChange(curRating)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    // Track drag movements
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            // Touch released!
                            break
                        }
                        if (change.position != down.position) {
                            change.consume()
                            val newRating = calculateRatingFromX(change.position.x, w, maxRating, numStars, gapPx, isRtl)
                            if (newRating != curRating) {
                                curRating = newRating
                                currentOnRatingChange(curRating)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }

                    // Finger released (whether from a quick tap or a drag)
                    if (curRating > 0.0) {
                        currentOnRatingSettled?.invoke(curRating)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        currentOnRatingSettled?.invoke(0.0)
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
                    showGlow = showGlow,
                    index = index
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
    index: Int = 0
) {
    // Smooth entrance animation without bouncy distortion
    val entranceAlpha = remember { Animatable(0f) }
    val entranceScale = remember { Animatable(0.8f) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 40L) // Subtle stagger
        launch { entranceAlpha.animateTo(1f, tween(180)) }
        launch { entranceScale.animateTo(1f, tween(200, easing = FastOutSlowInEasing)) }
    }

    Box(
        modifier = Modifier
            .size(starSize)
            .graphicsLayer {
                alpha = entranceAlpha.value
                scaleX = entranceScale.value
                scaleY = entranceScale.value
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
                                val rect = if (layoutDirection == LayoutDirection.Rtl) {
                                    Rect(size.width * (1f - fillRatio), 0f, size.width, size.height)
                                } else {
                                    Rect(0f, 0f, size.width * fillRatio, size.height)
                                }
                                return Outline.Rectangle(rect)
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

private fun calculateRatingFromX(
    x: Float,
    totalWidth: Float,
    maxRating: Double,
    numStars: Int,
    gapPx: Float,
    isRtl: Boolean = false
): Double {
    if (totalWidth <= 0f || numStars <= 0) return 0.0
    
    val starSize = (totalWidth - (numStars - 1) * gapPx) / numStars
    val starBlockWidth = starSize + gapPx
    
    val logicalX = if (isRtl) (totalWidth - x) else x
    val clampedX = logicalX.coerceIn(0f, totalWidth)
    val starIndex = (clampedX / starBlockWidth).toInt().coerceIn(0, numStars - 1)
    val localX = clampedX - starIndex * starBlockWidth
    
    val fraction = (localX / starSize).coerceIn(0f, 1f)
    val starValue = maxRating / numStars
    val rawRating = (starIndex + fraction) * starValue
    
    return ((rawRating * 2).roundToInt() / 2.0).coerceIn(0.0, maxRating)
}
