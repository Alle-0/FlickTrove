package com.cinetrack.ui.components.card

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.util.VibrationHelper
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * Checkmark button where the season progressive border is drawn directly ON the border of the button.
 * Features tactile spring squeeze and pop, micro-rotation snap on the tick, and an expanding
 * aperture shockwave ripple ring on tap without triggering card-level scale bounce.
 */
@Composable
internal fun SeasonProgressCheckButton(
    progress: Float,
    showProgress: Boolean = true,
    accentColor: Color,
    isUpdating: Boolean = false,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val buttonScale = remember { Animatable(1f) }
    val tickScale = remember { Animatable(1f) }
    val rippleProgress = remember { Animatable(0f) }

    fun triggerTapAnimation() {
        scope.launch {
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
        scope.launch {
            tickScale.snapTo(0.65f)
            tickScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = buttonScale.value
                scaleY = buttonScale.value
                clip = false
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                this.contentDescription = contentDescription
                this.onClick {
                    if (!isUpdating) {
                        VibrationHelper.vibrateTick(context)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        triggerTapAnimation()
                        onClick()
                        true
                    } else false
                }
            }
            .pointerInput(isUpdating) {
                if (isUpdating) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    scope.launch {
                        buttonScale.animateTo(
                            0.80f,
                            animationSpec = spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
                        )
                    }
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        up.consume()
                        scope.launch {
                            buttonScale.animateTo(
                                1.15f,
                                animationSpec = spring(stiffness = 12000f, dampingRatio = Spring.DampingRatioNoBouncy)
                            )
                            buttonScale.animateTo(
                                1.0f,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                )
                            )
                        }
                        VibrationHelper.vibrateTick(context)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        triggerTapAnimation()
                        onClick()
                    } else {
                        // Gesture cancelled (e.g. dragged outside)
                        scope.launch {
                            buttonScale.animateTo(
                                1.0f,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioNoBouncy
                                )
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Shockwave ripple ring expanding outward from the circle
        val p = rippleProgress.value
        if (p > 0f && p < 1f) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { clip = false }
            ) {
                val ringProgress = p
                val ringRadius = (12.dp.toPx() + 14.dp.toPx() * ringProgress)
                val ringAlpha = (1f - ringProgress).pow(1.5f) * 0.75f
                val strokeW = (2.dp * (1f - ringProgress)).toPx()
                drawCircle(
                    color = accentColor.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = strokeW)
                )
            }
        }

        // Inner circular button fill & icon with progressive border directly on boundary
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(accentColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (showProgress) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sw = 2.dp.toPx()
                    val strokeOffset = sw / 2
                    val arcSize = Size(size.width - sw, size.height - sw)
                    // Track
                    drawArc(
                        color = Color.White.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(strokeOffset, strokeOffset),
                        size = arcSize,
                        style = Stroke(width = sw)
                    )
                    // Progress fill
                    if (progress > 0f) {
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress.coerceIn(0f, 1f),
                            useCenter = false,
                            topLeft = Offset(strokeOffset, strokeOffset),
                            size = arcSize,
                            style = Stroke(width = sw, cap = StrokeCap.Round)
                        )
                    }
                }
            }
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick_card),
                contentDescription = null, // Handled by parent semantics
                tint = accentColor,
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        scaleX = tickScale.value
                        scaleY = tickScale.value
                    }
            )
        }
    }
}
