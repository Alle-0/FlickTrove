package com.cinetrack.ui.components.shared

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp as lerpFloat
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.util.toComposeColor
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private data class ExplosionParticle(
    val angle: Float,
    val distance: Float,
    val startRadius: Float,
    val color: Color
)

@Composable
fun CardExplosionOverlay(
    movie: Movie,
    cardPosition: Offset,
    cardSize: Size,
    onPopComplete: (Movie) -> Unit,
    onExplosionFinished: (Movie) -> Unit
) {
    val density = LocalDensity.current
    val appPrimary = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val rawAccent = remember(movie, appPrimary) {
        movie.accentColor.toComposeColor(appPrimary)
    }
    val accentColor = remember(rawAccent) {
        ColorUtils.ensureVividAccent(rawAccent)
    }

    // Prepare cohesive particle palette strictly derived from card accent color (no random/rainbow tones)
    val particleColors = remember(accentColor) {
        listOf(
            accentColor,
            lerp(accentColor, Color.White, 0.25f),
            lerp(accentColor, Color.Black, 0.20f),
            accentColor.copy(alpha = 0.85f),
            lerp(accentColor, Color.White, 0.45f).copy(alpha = 0.9f)
        )
    }

    val actualSize = remember(cardSize) {
        if (cardSize.width <= 0f || cardSize.height <= 0f) {
            Size(350f, 525f)
        } else {
            cardSize
        }
    }

    val particles = remember(movie.id, actualSize) {
        val maxSpread = max(actualSize.width, actualSize.height) * 1.6f
        val random = Random(movie.id.hashCode() + 999)
        List(45) {
            val theta = random.nextFloat() * (2.0 * Math.PI).toFloat()
            val dist = random.nextFloat() * (maxSpread - actualSize.width * 0.3f) + actualSize.width * 0.3f
            val radius = random.nextFloat() * 10f + 4f // 4px to 14px base radius
            val color = particleColors[random.nextInt(particleColors.size)]
            ExplosionParticle(
                angle = theta,
                distance = dist,
                startRadius = radius,
                color = color
            )
        }
    }

    val animatable = remember { Animatable(0f) }

    LaunchedEffect(movie.id) {
        launch {
            snapshotFlow { animatable.value }
                .first { it >= 0.05f }
            onPopComplete(movie)
        }
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = LinearEasing)
        )
        onExplosionFinished(movie)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val progress = animatable.value
        val centerX = cardPosition.x + actualSize.width / 2f
        val centerY = cardPosition.y + actualSize.height / 2f

        // 1. Card Pop Phase (0.0 .. 0.22)
        if (progress < 0.22f) {
            val popProgress = (progress / 0.22f).coerceIn(0f, 1f)
            // Scale up from 1.0 to 1.15
            val scale = 1f + 0.15f * popProgress
            val scaledWidth = actualSize.width * scale
            val scaledHeight = actualSize.height * scale
            val left = centerX - scaledWidth / 2f
            val top = centerY - scaledHeight / 2f

            val cardAlpha = (1f - popProgress).coerceIn(0f, 1f)
            val flashColor = lerp(accentColor, Color.White, popProgress)

            drawRoundRect(
                color = flashColor.copy(alpha = cardAlpha),
                topLeft = Offset(left, top),
                size = Size(scaledWidth, scaledHeight),
                cornerRadius = CornerRadius(with(density) { 24.dp.toPx() })
            )
        }

        // 2. Soft Glowing Particles Phase (0.18 .. 1.0)
        if (progress >= 0.18f) {
            val phaseProgress = ((progress - 0.18f) / 0.82f).coerceIn(0f, 1f)
            val easeOutCubic = 1f - (1f - phaseProgress) * (1f - phaseProgress) * (1f - phaseProgress)
            val easeInQuad = phaseProgress * phaseProgress

            // Soft, diffused glowing particles scattering around (radial gradient instead of solid circle)
            val gravityY = with(density) { 160.dp.toPx() } * phaseProgress * phaseProgress
            for (p in particles) {
                val currentDist = p.distance * easeOutCubic
                val px = centerX + cos(p.angle) * currentDist
                val py = centerY + sin(p.angle) * currentDist + gravityY

                val currentRadius = p.startRadius * (1f - easeInQuad)
                if (currentRadius > 0.5f) {
                    val pAlpha = if (phaseProgress < 0.65f) 1f else ((1f - phaseProgress) / 0.35f).coerceIn(0f, 1f)
                    val softRadius = currentRadius * 1.8f
                    val particleBrush = Brush.radialGradient(
                        colors = listOf(
                            p.color.copy(alpha = pAlpha),
                            p.color.copy(alpha = (pAlpha * 0.6f).coerceIn(0f, 1f)),
                            p.color.copy(alpha = (pAlpha * 0.15f).coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        center = Offset(px, py),
                        radius = softRadius
                    )
                    drawCircle(
                        brush = particleBrush,
                        radius = softRadius,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}
