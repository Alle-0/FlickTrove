package com.cinetrack.ui.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.launch
import kotlin.math.pow
import androidx.compose.ui.graphics.drawscope.clipPath

/**
 * bounceClick
 * Feedback di click standard.
 * @param onPress Azione eseguita immediatamente alla pressione.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bounceClick(
    scaleDown: Float = 0.92f,
    onLongClick: (() -> Unit)? = null,
    vibrateOnLongClick: Boolean = true,
    requireUnconsumed: Boolean = false,
    enabled: Boolean = true,
    onPress: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = bounceClickWithOffset(
    scaleDown = scaleDown,
    onLongClick = onLongClick?.let { action -> { _ -> action() } },
    vibrateOnLongClick = vibrateOnLongClick,
    requireUnconsumed = requireUnconsumed,
    enabled = enabled,
    onPress = onPress?.let { action -> { _ -> action() } },
    onClick = { _ -> onClick() }
)

val LocalVibrationEnabled = staticCompositionLocalOf { true }

/**
 * bounceClickWithOffset
 * Feedback di click avanzato che cattura l'Offset del tocco.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bounceClickWithOffset(
    scaleDown: Float = 0.92f,
    onLongClick: ((Offset) -> Unit)? = null,
    vibrateOnLongClick: Boolean = true,
    requireUnconsumed: Boolean = false,
    enabled: Boolean = true,
    onPress: ((Offset) -> Unit)? = null,
    onClick: (Offset) -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val vibrationEnabled = LocalVibrationEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }
    
    var lastOffset by remember { mutableStateOf(Offset.Zero) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnPress by rememberUpdatedState(onPress)

    LaunchedEffect(interactionSource, enabled) {
        if (!enabled) {
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        var pressJob: kotlinx.coroutines.Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is androidx.compose.foundation.interaction.PressInteraction.Press -> {
                    pressJob = launch {
                        scale.animateTo(
                            targetValue = scaleDown,
                            animationSpec = spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
                        )
                    }
                }
                is androidx.compose.foundation.interaction.PressInteraction.Release,
                is androidx.compose.foundation.interaction.PressInteraction.Cancel -> {
                    launch {
                        pressJob?.join() // Attendi che la discesa sia completata
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        )
                    }
                }
            }
        }
    }

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(enabled, requireUnconsumed) {
            if (enabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = requireUnconsumed)
                    lastOffset = down.position
                    currentOnPress?.invoke(down.position)
                }
            }
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = { 
                if (vibrationEnabled) {
                    com.cinetrack.util.VibrationHelper.vibrateTick(context)
                }
                currentOnClick(lastOffset) 
            },
            onLongClick = currentOnLongClick?.let { action ->
                {
                    if (vibrateOnLongClick && vibrationEnabled) {
                        com.cinetrack.util.VibrationHelper.vibrateLongClick(context)
                    }
                    action(lastOffset)
                }
            }
        )
}

/**
 * CardRippleState
 * Manages active ripples for a card component.
 */
class CardRippleState {
    private val _ripples = mutableStateListOf<RippleInstance>()
    val ripples: List<RippleInstance> get() = _ripples

    fun trigger(offset: Offset) {
        _ripples.add(RippleInstance(offset))
    }

    fun remove(ripple: RippleInstance) {
        _ripples.remove(ripple)
    }
}

data class RippleInstance(
    val center: Offset,
    val animation: Animatable<Float, AnimationVector1D> = Animatable(0f)
)

@Composable
fun rememberCardRippleState() = remember { CardRippleState() }

/**
 * cardRipple
 * A modifier that draws localized ripples managed by a [CardRippleState].
 */
fun Modifier.cardRipple(
    state: CardRippleState,
    color: Color = Color.White.copy(alpha = 0.25f)
) = this.composed {
    val scope = rememberCoroutineScope()
    
    // Process animations for new ripples
    LaunchedEffect(state.ripples.size) {
        state.ripples.forEach { ripple ->
            if (!ripple.animation.isRunning && ripple.animation.value == 0f) {
                scope.launch {
                    ripple.animation.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(1000, easing = LinearOutSlowInEasing)
                    )
                    state.remove(ripple)
                }
            }
        }
    }
    
    this.drawWithContent {
        drawContent()
        state.ripples.forEach { ripple ->
            val progress = ripple.animation.value
            if (progress < 1f) {
                // Reaches 0 at 85% of progress, with a smooth curve for an elegant finish
                val alphaProgress = (progress / 0.85f).coerceIn(0f, 1f)
                val alpha = (1f - alphaProgress).pow(2f)
                val radius = size.maxDimension * 1.5f * progress
                
                drawCircle(
                    color = color.copy(alpha = color.alpha * alpha),
                    radius = radius,
                    center = ripple.center
                )
            }
        }
    }
}
