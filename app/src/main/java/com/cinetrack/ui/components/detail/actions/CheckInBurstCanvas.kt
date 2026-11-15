package com.cinetrack.ui.components.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

data class SaveBurstParticle(
    val angle: Float,
    val distance: Float,
    val radius: Float,
    val color: Color
)

/**
 * Celebration particle burst and radial glow rendered upon confirming check-in save.
 */
@Composable
fun CheckInBurstCanvas(
    burstProgress: Float,
    particles: List<SaveBurstParticle>,
    modifier: Modifier = Modifier
) {
    if (burstProgress <= 0f) return

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { clip = false }
    ) {
        val p = burstProgress
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Expanding soft radial glow
        val glowRadius = (size.width / 2f + 28.dp.toPx()) * p
        val glowAlpha = (1f - p) * 0.45f
        if (glowAlpha > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF10B981).copy(alpha = glowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = glowRadius
                ),
                center = Offset(centerX, centerY),
                radius = glowRadius
            )
        }

        // Radial flying particles
        for (particle in particles) {
            val distPx = particle.distance.dp.toPx() * p
            val px = centerX + cos(particle.angle) * distPx
            val py = centerY + sin(particle.angle) * distPx
            val pAlpha = (1f - p).coerceIn(0f, 1f)
            val pRadius = particle.radius.dp.toPx() * (1f - p * 0.35f)

            drawCircle(
                color = particle.color.copy(alpha = pAlpha),
                radius = pRadius,
                center = Offset(px, py)
            )
        }
    }
}
