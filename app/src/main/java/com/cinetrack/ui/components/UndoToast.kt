package com.cinetrack.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TOAST_DURATION_MS = 4500L

/**
 * UndoToast
 * Global floating toast that slides up from the bottom.
 * Observes ActionFeedbackManager and auto-dismisses after TOAST_DURATION_MS.
 */
@Composable
fun UndoToast(
    actionFeedbackManager: ActionFeedbackManager,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    var currentAction by remember { mutableStateOf<com.cinetrack.ui.utils.UndoAction?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // Collect events from the manager
    LaunchedEffect(actionFeedbackManager) {
        actionFeedbackManager.events.collect { action ->
            currentAction = action
            isVisible = true
        }
    }

    // Auto-dismiss timer: restarted whenever isVisible becomes true
    LaunchedEffect(isVisible, currentAction) {
        if (isVisible) {
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
            delay(TOAST_DURATION_MS)
            isVisible = false
        }
    }

    Box(
        modifier = modifier.imePadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(200))
        ) {
            val action = currentAction ?: return@AnimatedVisibility

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .width(320.dp)
                    .zIndex(20000f) // Ensure internal layering
                    .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (abs(offsetX.value) > 150f || abs(offsetY.value) > 150f) {
                                        val targetX = if (abs(offsetX.value) > 150f) sign(offsetX.value) * 1000f else offsetX.value
                                        val targetY = if (abs(offsetY.value) > 150f) sign(offsetY.value) * 1000f else offsetY.value
                                        launch { offsetX.animateTo(targetX, tween(200)) }
                                        launch { offsetY.animateTo(targetY, tween(200)) }
                                        delay(150)
                                        isVisible = false
                                    } else {
                                        launch { offsetX.animateTo(0f, spring()) }
                                        launch { offsetY.animateTo(0f, spring()) }
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                }
                            }
                        )
                    }
            ) {
                // Blur Background with Haze - Modern Pill Design
                Box(
                    modifier = Modifier
                        .hazeGlass(
                            state = hazeState,
                            shape = RoundedCornerShape(16.dp),
                            useOffscreenStrategy = false // We apply it more precisely to the content below
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen } // Fix text blur precisely
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Message
                        Text(
                            text = action.message.asString(),
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )

                        // Undo button
                        if (action.undoFn != null) {
                            Spacer(Modifier.width(8.dp))
                            val accent = Color(0xFF00E676)
                            Box(
                                modifier = Modifier
                                    .bounceClick(scaleDown = 0.92f) {
                                        isVisible = false
                                        scope.launch { action.undoFn.invoke() }
                                    }
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.12f))
                                    .border(
                                        1.dp,
                                        accent.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(com.cinetrack.R.string.action_undo),
                                    color = accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
