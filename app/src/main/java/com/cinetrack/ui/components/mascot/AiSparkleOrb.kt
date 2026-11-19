package com.cinetrack.ui.components.mascot

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sfera neurale / generativa dell'IA Cinema Mood (Cloud Gemini).
 * - A riposo (isSearching = false): morbido respiro del bagliore interno, micro-fluttuazione e presenza sobria.
 * - In ricerca / elaborazione (isSearching = true): aura generativa pulsante, anello orbitale con sweep gradient rotante,
 *   particelle stardust in orbita e oscillazione scintillante dell'icona Sparkle.
 */
@Composable
fun AiSparkleOrb(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    isSearching: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    glowPadding: Dp = 0.dp
) {
    val density = LocalDensity.current

    // Valori dimensionali e Stroke pre-calcolati per azzerare density churn
    val ringStrokeSearching = remember(density) { Stroke(width = with(density) { 1.8.dp.toPx() }) }
    val ringStrokeIdle = remember(density) { Stroke(width = with(density) { 1.2.dp.toPx() }) }
    val orbInnerRingStroke = remember(density) { Stroke(width = with(density) { 0.8.dp.toPx() }) }
    val dotRadius1 = remember(density) { with(density) { 2.2.dp.toPx() } }
    val dotRadius2 = remember(density) { with(density) { 1.6.dp.toPx() } }

    val infiniteTransition = rememberInfiniteTransition(label = "ai_sparkle_orb_loop")

    // Rotazione orbitale continua a 360° (più veloce in ricerca, lenta e rilassante a riposo)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSearching) 2400 else 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ai_orb_rotation"
    )

    // Pulsazione respirante dell'aura e dell'icona Sparkle
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (isSearching) 0.92f else 0.96f,
        targetValue = if (isSearching) 1.08f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSearching) 1300 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_orb_pulse"
    )

    // Bagliore dinamico centrale
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isSearching) 0.30f else 0.15f,
        targetValue = if (isSearching) 0.75f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSearching) 1300 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_orb_glow"
    )

    // Oscillazione angolare dello Sparkle interno
    val sparkleTilt by infiniteTransition.animateFloat(
        initialValue = if (isSearching) -14f else -4f,
        targetValue = if (isSearching) 14f else 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSearching) 1800 else 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_sparkle_tilt"
    )

    val containerSize = size + (glowPadding * 2)

    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = this.size.width
            val height = this.size.height
            val centerX = width / 2f
            val centerY = height / 2f
            val baseRadius = (size.toPx() / 2f) * 0.90f

            // 1. Aura esterna pulsante (Generative Thinking Aura)
            val auraRadius = baseRadius * pulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to accentColor.copy(alpha = glowAlpha * (if (isSearching) 0.45f else 0.20f)),
                    0.60f to accentColor.copy(alpha = glowAlpha * (if (isSearching) 0.15f else 0.05f)),
                    1.0f to Color.Transparent,
                    center = Offset(centerX, centerY),
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = Offset(centerX, centerY)
            )

            // 2. Disco di fondo in vetro scuro cinematico
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFF1B232A),
                    0.7f to Color(0xFF0F1419),
                    1.0f to Color(0xFF07090C),
                    center = Offset(centerX, centerY),
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = Offset(centerX, centerY)
            )

            // 3. Anello sweep orbitale luminoso rotante a 360°
            val sweepBrush = Brush.sweepGradient(
                listOf(
                    accentColor.copy(alpha = 0.05f),
                    accentColor.copy(alpha = 0.25f),
                    accentColor.copy(alpha = if (isSearching) 0.95f else 0.50f),
                    Color.White.copy(alpha = if (isSearching) 0.90f else 0.40f),
                    accentColor.copy(alpha = 0.15f),
                    accentColor.copy(alpha = 0.05f)
                ),
                center = Offset(centerX, centerY)
            )

            rotate(rotation, pivot = Offset(centerX, centerY)) {
                drawCircle(
                    brush = sweepBrush,
                    radius = baseRadius,
                    center = Offset(centerX, centerY),
                    style = if (isSearching) ringStrokeSearching else ringStrokeIdle
                )

                // Micro-particelle stardust in orbita sull'anello
                if (isSearching) {
                    val dotRad1 = 0f
                    val dotRad2 = 180f * (Math.PI.toFloat() / 180f)
                    drawCircle(
                        color = Color.White,
                        radius = dotRadius1,
                        center = Offset(centerX + baseRadius * cos(dotRad1), centerY + baseRadius * sin(dotRad1))
                    )
                    drawCircle(
                        color = accentColor.copy(alpha = 0.75f),
                        radius = dotRadius2,
                        center = Offset(centerX + baseRadius * cos(dotRad2), centerY + baseRadius * sin(dotRad2))
                    )
                }
            }

            // 4. Anello interno fisso sottile
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = baseRadius * 0.78f,
                center = Offset(centerX, centerY),
                style = orbInnerRingStroke
            )

            // 5. Nucleo luminoso centrale dietro l'icona
            val coreGlowRadius = baseRadius * 0.65f
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to accentColor.copy(alpha = glowAlpha * (if (isSearching) 0.55f else 0.25f)),
                    1.0f to Color.Transparent,
                    center = Offset(centerX, centerY),
                    radius = coreGlowRadius
                ),
                radius = coreGlowRadius,
                center = Offset(centerX, centerY)
            )
        }

        // Icona Sparkle centrale che pulsa ed oscilla armonicamente
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
            contentDescription = null,
            tint = if (isSearching) Color.White else accentColor,
            modifier = Modifier
                .size(size * 0.42f)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    rotationZ = sparkleTilt
                    alpha = if (isSearching) (0.75f + glowAlpha * 0.25f).coerceIn(0f, 1f) else 0.85f
                }
        )
    }
}
