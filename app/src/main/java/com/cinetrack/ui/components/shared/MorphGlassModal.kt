package com.cinetrack.ui.components.shared

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.cinetrack.LocalAdvancedVisualEffects
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.DarkSurface
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import kotlin.math.roundToInt

/**
 * Shared modal container that provides a unified expanding morph animation.
 * The modal animates directly from the trigger button bounds (or center if null)
 * with bouncy spring physics into the centered glass modal, morphing the corner radius
 * and fading in content once expanded. On dismiss, it smoothly shrinks back into the trigger button.
 *
 * When [LocalAdvancedVisualEffects] is disabled, it switches to an ultra-lightweight centered
 * fade & scale transition, completely bypassing frame-by-frame geometry and blur recalculations.
 */
@Composable
fun MorphGlassModal(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    triggerBounds: Rect? = null,
    hazeState: HazeState? = null,
    targetMaxWidth: Dp = 420.dp,
    targetWidthFraction: Float = 0.90f,
    maxModalHeightFraction: Float = 0.78f,
    minModalHeight: Dp = 260.dp,
    targetCornerRadius: Dp = 32.dp,
    style: HazeStyle = HazeStyles.glassmorphicDialog,
    scrimAlpha: Float = 0.6f,
    zIndex: Float = 100f,
    content: @Composable BoxScope.(contentAlpha: Float) -> Unit
) {
    val advancedEffectsEnabled = LocalAdvancedVisualEffects.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val targetWidth = (screenWidth * targetWidthFraction).coerceAtMost(with(density) { targetMaxWidth.toPx() })
    
    var contentHeightPx by remember { mutableFloatStateOf(0f) }
    val maxAllowedHeight = screenHeight * maxModalHeightFraction
    val minAllowedHeightPx = with(density) { minModalHeight.toPx() }
    
    val targetHeightPx by animateFloatAsState(
        targetValue = if (contentHeightPx > 0) contentHeightPx.coerceIn(minAllowedHeightPx, maxAllowedHeight) 
                      else with(density) { 340.dp.toPx() },
        animationSpec = if (advancedEffectsEnabled) spring(stiffness = Spring.StiffnessLow) 
                        else spring(stiffness = Spring.StiffnessMedium),
        label = "dynamicHeight"
    )

    val targetRect = Rect(
        left = (screenWidth - targetWidth) / 2f,
        top = (screenHeight - targetHeightPx) / 2f,
        right = (screenWidth + targetWidth) / 2f,
        bottom = (screenHeight + targetHeightPx) / 2f
    )

    val transition = updateTransition(targetState = isVisible, label = "MorphModalTransition")

    BackHandler(enabled = isVisible) {
        onDismissRequest()
    }

    val progress by transition.animateFloat(
        transitionSpec = {
            if (!advancedEffectsEnabled) {
                tween(durationMillis = 150, easing = FastOutSlowInEasing)
            } else if (initialState == false && targetState == true) {
                spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
            } else {
                spring(stiffness = Spring.StiffnessMedium)
            }
        },
        label = "expansionProgress"
    ) { state -> if (state) 1f else 0f }

    if (transition.currentState || transition.targetState) {
        val effectiveScrimAlpha = if (!advancedEffectsEnabled) {
            scrimAlpha * progress
        } else if (triggerBounds != null) {
            val scrimProgress = ((progress - 0.08f) / 0.92f).coerceIn(0f, 1f)
            scrimAlpha * FastOutSlowInEasing.transform(scrimProgress)
        } else {
            scrimAlpha * progress
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(zIndex)
                .background(Color.Black.copy(alpha = effectiveScrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
        ) {
            // --- GHOST MEASUREMENT LAYER ---
            Box(
                modifier = Modifier
                    .width(with(density) { targetWidth.toDp() })
                    .graphicsLayer { this.alpha = 0f }
                    .onSizeChanged { size ->
                        if (size.height > 0) contentHeightPx = size.height.toFloat()
                    }
                    .align(Alignment.Center)
            ) {
                content(0f)
            }

            if (!advancedEffectsEnabled) {
                // Static, ultra-lightweight dialog layout: fade + subtle scale, no frame-by-frame shape or layout recalculation
                val shape = remember(targetCornerRadius) { RoundedCornerShape(targetCornerRadius) }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(targetRect.left.roundToInt(), targetRect.top.roundToInt()) }
                        .size(
                            width = with(density) { targetRect.width.toDp() },
                            height = with(density) { targetRect.height.toDp() }
                        )
                        .graphicsLayer {
                            alpha = progress
                            val scale = lerp(0.95f, 1f, progress)
                            scaleX = scale
                            scaleY = scale
                        }
                        .bounceClick(scaleDown = 1f) { /* Prevent dismissal on inner tap */ }
                ) {
                    // Background Layer (Glass / Surface)
                    Spacer(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeGlass(
                                state = hazeState,
                                shape = shape,
                                style = style,
                                useOffscreenStrategy = false
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = HazeStyles.ModalBorderAlpha),
                                shape = shape
                            )
                    )

                    // Content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f)
                    ) {
                        content(progress)
                    }
                }
            } else {
                // Expanding morph from trigger button bounds (or center)
                val startRect = triggerBounds ?: targetRect.copy(
                    left = targetRect.center.x - 20f,
                    top = targetRect.center.y - 20f,
                    right = targetRect.center.x + 20f,
                    bottom = targetRect.center.y + 20f
                )

                // Center moves smoothly towards screen center
                val currentCenterX = lerp(startRect.center.x, targetRect.center.x, progress)
                val currentCenterY = lerp(startRect.center.y, targetRect.center.y, progress)

                // Delayed size expansion: stays compact/circular during liftoff (0..0.18), then blossoms into full size
                val sizeProgress = if (triggerBounds != null) {
                    val delay = 0.18f
                    if (progress <= delay) {
                        (progress / delay) * 0.05f
                    } else {
                        val raw = (progress - delay) / (1f - delay)
                        0.05f + 0.95f * FastOutSlowInEasing.transform(raw.coerceIn(0f, 1f))
                    }
                } else {
                    progress
                }

                // Corner radius stays circular during liftoff, then morphs into target rounded corners
                val cornerRadiusProgress = if (triggerBounds != null) {
                    val delay = 0.18f
                    if (progress <= delay) 0f
                    else FastOutSlowInEasing.transform(((progress - delay) / (1f - delay)).coerceIn(0f, 1f))
                } else {
                    progress
                }

                val currentWidth = lerp(startRect.width, targetRect.width, sizeProgress)
                val currentHeight = lerp(startRect.height, targetRect.height, sizeProgress)

                val currentRect = Rect(
                    left = currentCenterX - currentWidth / 2f,
                    top = currentCenterY - currentHeight / 2f,
                    right = currentCenterX + currentWidth / 2f,
                    bottom = currentCenterY + currentHeight / 2f
                )

                val startRadius = if (triggerBounds != null) startRect.width / 2f else with(density) { targetCornerRadius.toPx() }
                val endRadius = with(density) { targetCornerRadius.toPx() }
                val currentCornerRadius = lerp(startRadius, endRadius, cornerRadiusProgress)
                val currentShape = RoundedCornerShape(with(density) { currentCornerRadius.toDp() })

                val currentTintAlpha = if (triggerBounds != null) {
                    lerp(0.48f, style.tint.alpha, sizeProgress)
                } else {
                    style.tint.alpha
                }
                // Keep blurRadius constant at style.blurRadius to avoid GPU shader re-allocations during animation
                val animatedStyle = remember(style, currentTintAlpha) {
                    style.copy(
                        tint = style.tint.copy(alpha = currentTintAlpha),
                        blurRadius = style.blurRadius
                    )
                }
                val borderAlpha = if (triggerBounds != null) {
                    lerp(HazeStyles.ModalBorderAlphaStart, HazeStyles.ModalBorderAlpha, sizeProgress)
                } else {
                    HazeStyles.ModalBorderAlpha
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(currentRect.left.roundToInt(), currentRect.top.roundToInt()) }
                        .size(
                            width = with(density) { currentRect.width.toDp() },
                            height = with(density) { currentRect.height.toDp() }
                        )
                        .bounceClick(scaleDown = 1f) { /* Prevent dismissal on inner tap */ }
                ) {
                    // Background Layer (Blurred glass)
                    Spacer(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeGlass(
                                state = hazeState,
                                shape = currentShape,
                                style = animatedStyle,
                                useOffscreenStrategy = false
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = borderAlpha),
                                shape = currentShape
                            )
                    )

                    // Foreground Content
                    if (progress > 0.38f) {
                        val contentAlpha = ((progress - 0.38f) / 0.62f).coerceIn(0f, 1f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1f)
                                .graphicsLayer(
                                    alpha = contentAlpha,
                                    compositingStrategy = CompositingStrategy.Offscreen
                                )
                        ) {
                            content(contentAlpha)
                        }
                    }
                }
            }
        }
    }
}
