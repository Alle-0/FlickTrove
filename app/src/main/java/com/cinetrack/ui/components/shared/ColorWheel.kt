package com.cinetrack.ui.components.shared

import android.graphics.Color.HSVToColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.cinetrack.util.toComposeColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlinx.coroutines.launch


@Composable
fun ColorWheel(
    selectedColor: String,
    onColorChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onInteractionStart: () -> Unit = {},
    onInteractionEnd: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    // 1. Efficient Color Parsing
    val initialHsv = remember(selectedColor) {
        val hsv = FloatArray(3)
        try {
            if (selectedColor.length == 7 && selectedColor.startsWith("#")) {
                android.graphics.Color.colorToHSV(selectedColor.toComposeColor().toArgb(), hsv)
                hsv
            } else null
        } catch (e: Exception) { null }
    }

    // 2. Local state for zero-latency UI feedback
    var currentHue by remember { mutableStateOf(initialHsv?.get(0) ?: 0f) }
    var currentSat by remember { mutableStateOf(initialHsv?.get(1) ?: 1f) }
    var isUserInteracting by remember { mutableStateOf(false) }

    // Use Animatable for the selector's fractional position (-1..1, -1..1)
    // This ensures smooth straight-line movement (passing through center)
    val selectorPos = remember { 
        val angle = Math.toRadians((initialHsv?.get(0) ?: 0f).toDouble())
        val sat = initialHsv?.get(1) ?: 1f
        Animatable(
            Offset(
                (sat * cos(angle)).toFloat(),
                (sat * sin(angle)).toFloat()
            ),
            Offset.VectorConverter
        )
    }

    // Sync from external changes
    LaunchedEffect(selectedColor) {
        if (!isUserInteracting && initialHsv != null) {
            currentHue = initialHsv[0]
            currentSat = initialHsv[1]
            val angle = Math.toRadians(currentHue.toDouble())
            val target = Offset(
                (currentSat * cos(angle)).toFloat(),
                (currentSat * sin(angle)).toFloat()
            )
            selectorPos.animateTo(target, tween(450, easing = FastOutSlowInEasing))
        }
    }

    // Derived color for the handle
    val handleColor = remember(selectorPos.value) {
        val x = selectorPos.value.x
        val y = selectorPos.value.y
        var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
        if (angle < 0) angle += 360f
        val sat = sqrt(x * x + y * y).coerceIn(0f, 1f)
        Color(HSVToColor(floatArrayOf(angle, sat, 1f)))
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { 
                            isUserInteracting = true
                            onInteractionStart()
                            try {
                                awaitRelease()
                            } finally {
                                isUserInteracting = false
                                onInteractionEnd()
                            }
                        }
                    ) { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = min(centerX, centerY)
                        
                        val dx = (offset.x - centerX) / radius
                        val dy = (offset.y - centerY) / radius
                        val dist = sqrt(dx * dx + dy * dy)
                        val limited = if (dist > 1f) Offset(dx / dist, dy / dist) else Offset(dx, dy)
                        
                        coroutineScope.launch {
                            selectorPos.snapTo(limited)
                            val (hue, sat) = calculateHueSatFromOffset(limited)
                            currentHue = hue
                            currentSat = sat
                            onColorChanged(hsvToHex(hue, sat))
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { 
                            isUserInteracting = true
                            onInteractionStart()
                        },
                        onDragEnd = { 
                            isUserInteracting = false
                            onInteractionEnd()
                        },
                        onDragCancel = { 
                            isUserInteracting = false
                            onInteractionEnd()
                        }
                    ) { change, _ ->
                        change.consume()
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = min(centerX, centerY)
                        
                        val dx = (change.position.x - centerX) / radius
                        val dy = (change.position.y - centerY) / radius
                        val dist = sqrt(dx * dx + dy * dy)
                        val limited = if (dist > 1f) Offset(dx / dist, dy / dist) else Offset(dx, dy)
                        
                        coroutineScope.launch {
                            selectorPos.snapTo(limited)
                            val (hue, sat) = calculateHueSatFromOffset(limited)
                            currentHue = hue
                            currentSat = sat
                            onColorChanged(hsvToHex(hue, sat))
                        }
                    }
                }
        ) {
            val radius = size.minDimension / 2
            val centerOffset = center
            
            // Draw Hue Wheel (Sweep Gradient)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    )
                ),
                radius = radius
            )
            
            // Draw Saturation (Radial Gradient)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color.White,
                    1.0f to Color.Transparent,
                    center = centerOffset,
                    radius = radius
                ),
                radius = radius
            )
            
            // Subtle Outer Border
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = radius,
                style = Stroke(width = 1.dp.toPx())
            )

            // Calculate absolute handle position
            val handleCenter = Offset(
                centerOffset.x + selectorPos.value.x * radius,
                centerOffset.y + selectorPos.value.y * radius
            )

            // 1. Shadow for depth
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = 15.dp.toPx(),
                center = handleCenter + Offset(0f, 2.dp.toPx())
            )
            
            // 2. White outer ring
            drawCircle(
                color = Color.White,
                radius = 14.dp.toPx(),
                center = handleCenter
            )
            
            // 3. Inner color fill
            drawCircle(
                color = handleColor,
                radius = 11.dp.toPx(),
                center = handleCenter
            )
        }
    }
}

private fun calculateHueSatFromOffset(offset: Offset): Pair<Float, Float> {
    var angle = Math.toDegrees(atan2(offset.y.toDouble(), offset.x.toDouble())).toFloat()
    if (angle < 0) angle += 360f
    val saturation = sqrt(offset.x * offset.x + offset.y * offset.y).coerceIn(0f, 1f)
    return Pair(angle, saturation)
}

private fun hsvToHex(hue: Float, saturation: Float): String {
    val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f))
    return String.format("#%06X", 0xFFFFFF and color)
}

