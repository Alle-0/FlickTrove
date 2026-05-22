package com.cinetrack.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    color1: Color = NeonPink,
    color2: Color = NeonTeal
) {
    if (data.isEmpty()) return

    val maxVal = remember(data) { data.maxOfOrNull { it.second }?.toFloat() ?: 1f }
    val numAxes = data.size
    
    // Animation for expansion
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1500, easing = FastOutSlowInEasing)
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.width.coerceAtMost(size.height) * 0.35f
            val angleStep = Math.PI * 2 / numAxes

            // Draw background webs (the "Glass" structure)
            for (i in 1..4) {
                val r = baseRadius * (i / 4f)
                val path = Path()
                for (j in 0 until numAxes) {
                    val angle = j * angleStep - Math.PI / 2
                    val x = center.x + r * cos(angle).toFloat()
                    val y = center.y + r * sin(angle).toFloat()
                    if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.05f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Draw axis lines
            for (i in 0 until numAxes) {
                val angle = i * angleStep - Math.PI / 2
                val x = center.x + baseRadius * cos(angle).toFloat()
                val y = center.y + baseRadius * sin(angle).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = center,
                    end = Offset(x, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw the data polygon
            val dataPath = Path()
            val points = mutableListOf<Offset>()
            for (i in 0 until numAxes) {
                val value = data[i].second.toFloat() / maxVal
                val r = baseRadius * value * animatedProgress.value * pulseScale
                val angle = i * angleStep - Math.PI / 2
                val x = center.x + r * cos(angle).toFloat()
                val y = center.y + r * sin(angle).toFloat()
                
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                points.add(Offset(x, y))
            }
            dataPath.close()

            // Fill with neon gradient
            drawPath(
                path = dataPath,
                brush = Brush.radialGradient(
                    colors = listOf(color1.copy(alpha = 0.4f), color2.copy(alpha = 0.1f)),
                    center = center,
                    radius = baseRadius
                )
            )

            // Border stroke
            drawPath(
                path = dataPath,
                color = color1,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw vertex points
            points.forEach { point ->
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = color1,
                    radius = 6.dp.toPx(),
                    center = point,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Labels (placed outside the canvas drawing for better text handling)
        data.forEachIndexed { i, (name, _) ->
            val angle = i * (Math.PI * 2 / numAxes) - Math.PI / 2
            val radius = 130.dp // Fixed offset for labels
            
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val xOffset = (140 * cos(angle)).dp
                val yOffset = (140 * sin(angle)).dp

                Text(
                    text = name.uppercase(),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.offset(x = xOffset, y = yOffset)
                )
            }
        }
    }
}
