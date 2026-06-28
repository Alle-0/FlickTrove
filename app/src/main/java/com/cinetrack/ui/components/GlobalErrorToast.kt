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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.ui.utils.GlobalErrorHandler
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ERROR_TOAST_DURATION_MS = 5000L

/**
 * GlobalErrorToast
 * Global floating error toast that slides down from the top.
 * Observes GlobalErrorHandler safely respecting the lifecycle to avoid ghost snackbars.
 */
@Composable
fun GlobalErrorToast(
    globalErrorHandler: GlobalErrorHandler,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    // La sottoscrizione al GlobalErrorHandler utilizza rigorosamente le estensioni lifecycle.
    // L'ascolto si iberna non appena la UI non è più visibile.
    val errorEvent by globalErrorHandler.errors.collectAsStateWithLifecycle(initialValue = null)
    
    var currentAction by remember { mutableStateOf<com.cinetrack.ui.utils.ErrorEvent?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // Quando arriva un nuovo evento, aggiorniamo lo stato interno visibile
    LaunchedEffect(errorEvent) {
        if (errorEvent != null) {
            currentAction = errorEvent
            isVisible = true
        }
    }

    // Timer auto-dismiss
    LaunchedEffect(isVisible, currentAction) {
        if (isVisible) {
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
            delay(ERROR_TOAST_DURATION_MS)
            isVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(200))
        ) {
            val action = currentAction ?: return@AnimatedVisibility

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .widthIn(max = 340.dp)
                    .zIndex(20000f)
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
                Box(
                    modifier = Modifier
                        .hazeGlass(
                            state = hazeState,
                            shape = RoundedCornerShape(16.dp),
                            useOffscreenStrategy = false
                        )
                        .border(1.dp, Color(0xFFE53935).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️",
                            modifier = Modifier.padding(end = 12.dp),
                            fontSize = 16.sp
                        )
                        
                        Text(
                            text = action.message.asString(),
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )

                        if (action.retryFn != null) {
                            Spacer(Modifier.width(12.dp))
                            val accent = Color(0xFFFF5252)
                            Box(
                                modifier = Modifier
                                    .bounceClick(scaleDown = 0.92f) {
                                        isVisible = false
                                        scope.launch { action.retryFn.invoke() }
                                    }
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "RIPROVA",
                                    color = accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
