package com.cinetrack.ui.components.mascot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Canvas rendering of IRIS's 6-blade optical aperture mechanism,
 * volumetric back-glow, focus tick marks, and glossy Pixar-style catchlights.
 */
@Composable
fun IrisLensCanvas(
    size: Dp,
    glowPadding: Dp,
    enableBacklight: Boolean,
    effectiveGlowAlpha: Float,
    apertureRatio: Float,
    activeAccentColor: Color,
    mood: IrisMood,
    barrelRotation: Float,
    effectiveAngle: Float,
    pupilShiftXRatio: Float,
    pupilShiftYRatio: Float,
    smoothTiltX: Float,
    smoothTiltY: Float,
    dizzyProgress: Float,
    annoyedProgress: Float,
    effectiveAperture: Float,
    gazeDrift: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Reusable Path per eliminare allocazioni native SkPath nel draw loop a 60/120 FPS
    val clipCameraPath = remember { Path() }
    val bladeReusablePath = remember { Path() }
    val apertureReusablePath = remember { Path() }

    // Valori dimensionali e Stroke pre-calcolati (zero density churn durante il disegno)
    val outerRingStroke = remember(density) { Stroke(width = with(density) { 1.2.dp.toPx() }) }
    val innerRingStroke = remember(density) { Stroke(width = with(density) { 0.8.dp.toPx() }) }
    val apertureStroke = remember(density) { Stroke(width = with(density) { 1.3.dp.toPx() }) }
    val majorTickWidth = remember(density) { with(density) { 1.2.dp.toPx() } }
    val minorTickWidth = remember(density) { with(density) { 0.8.dp.toPx() } }
    val majorTickStartOffset = remember(density) { with(density) { 4.5.dp.toPx() } }
    val minorTickStartOffset = remember(density) { with(density) { 2.5.dp.toPx() } }
    val tickEndOffset = remember(density) { with(density) { 1.0.dp.toPx() } }
    val bladeLineWidth = remember(density) { with(density) { 1.2.dp.toPx() } }
    val oneDpPx = remember(density) { with(density) { 1.dp.toPx() } }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = this.size.width
        val height = this.size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val maxR = width.coerceAtMost(height) / 2f

        // Il raggio del barilotto corrisponde esattamente al diametro visivo 'size'
        val barrelOuterR = (size.toPx() / 2f) * 0.88f
        val barrelInnerR = (size.toPx() / 2f) * 0.75f

        // 0. CONTROLUCE CINEMATOGRAFICO POSTERIORE (Volumetric Cinematic Back-Glow)
        // L'alone si sviluppa entro i confini ampi del contenitore senza mai toccare il bordo esterno,
        // azzerando il rischio di clipping a rettangolo durante transizioni o animazioni alpha.
        if (enableBacklight && effectiveGlowAlpha > 0.01f) {
            // A. Alone d'ambiente ampio e soffuso (Wide Volumetric Halo)
            val ambientRadius = if (glowPadding > 0.dp) {
                (barrelOuterR * (1.45f + 0.25f * apertureRatio)).coerceAtMost(maxR * 0.95f)
            } else {
                barrelOuterR * (1.35f + 0.20f * apertureRatio)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to activeAccentColor.copy(alpha = effectiveGlowAlpha * 0.45f),
                    (barrelOuterR / ambientRadius).coerceIn(0.35f, 0.70f) to activeAccentColor.copy(alpha = effectiveGlowAlpha * 0.32f),
                    0.72f to activeAccentColor.copy(alpha = effectiveGlowAlpha * 0.10f),
                    1.0f to Color.Transparent,
                    center = Offset(centerX, centerY),
                    radius = ambientRadius
                ),
                radius = ambientRadius,
                center = Offset(centerX, centerY)
            )

            // B. Bloom brillante intermedio a ridosso del bordo (Core Rim Bloom)
            val rimBloomRadius = (barrelOuterR * (1.25f + 0.12f * apertureRatio)).coerceAtMost(maxR * 0.88f)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to activeAccentColor.copy(alpha = effectiveGlowAlpha * 0.85f),
                    (barrelOuterR / rimBloomRadius).coerceIn(0.55f, 0.85f) to activeAccentColor.copy(alpha = effectiveGlowAlpha * 0.55f),
                    0.90f to activeAccentColor.copy(alpha = effectiveGlowAlpha * 0.12f),
                    1.0f to Color.Transparent,
                    center = Offset(centerX, centerY),
                    radius = rimBloomRadius
                ),
                radius = rimBloomRadius,
                center = Offset(centerX, centerY)
            )

            // C. Flare supplementare del fascio di proiezione in ricerca attiva o gioia (Projector Beam Flare)
            if (mood == IrisMood.SEARCHING || mood == IrisMood.DELIGHTED) {
                val flareRadius = (ambientRadius * 1.10f).coerceAtMost(maxR * 0.98f)
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to activeAccentColor.copy(alpha = effectiveGlowAlpha * 0.30f),
                        0.55f to activeAccentColor.copy(alpha = effectiveGlowAlpha * 0.12f),
                        1.0f to Color.Transparent,
                        center = Offset(centerX, centerY),
                        radius = flareRadius
                    ),
                    radius = flareRadius,
                    center = Offset(centerX, centerY)
                )
            }
        }

        // Fondo scuro del corpo ottica
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFF1B232A),
                0.7f to Color(0xFF0F1419),
                1.0f to Color(0xFF07090C),
                center = Offset(centerX, centerY),
                radius = barrelOuterR
            ),
            radius = barrelOuterR,
            center = Offset(centerX, centerY)
        )

        // Anello metallico esterno con bordo rifinito (zero allocazioni Stroke)
        drawCircle(
            color = activeAccentColor.copy(alpha = 0.35f),
            radius = barrelOuterR,
            center = Offset(centerX, centerY),
            style = outerRingStroke
        )

        // 2. TACCHE DI CALIBRAZIONE FOCALE (Focus & F-Stop Ring - zero density churn)
        val tickAngleOffset = if (mood == IrisMood.SEARCHING) barrelRotation else 0f
        val tickCount = 16
        for (i in 0 until tickCount) {
            val rad = (tickAngleOffset + i * (360f / tickCount)) * (Math.PI.toFloat() / 180f)
            val isMajor = (i % 4 == 0)
            val tickStartR = barrelOuterR - (if (isMajor) majorTickStartOffset else minorTickStartOffset)
            val tickEndR = barrelOuterR - tickEndOffset

            val startX = centerX + tickStartR * cos(rad)
            val startY = centerY + tickStartR * sin(rad)
            val endX = centerX + tickEndR * cos(rad)
            val endY = centerY + tickEndR * sin(rad)

            drawLine(
                color = if (isMajor) activeAccentColor.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.20f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (isMajor) majorTickWidth else minorTickWidth
            )
        }

        // Anello interno di raccordo alla camera del diaframma (zero allocazioni Stroke)
        drawCircle(
            color = Color.White.copy(alpha = 0.12f),
            radius = barrelInnerR,
            center = Offset(centerX, centerY),
            style = innerRingStroke
        )

        // 3. DIAFRAMMA CINEMATOGRAFICO
        val pupilMaxShiftY = barrelInnerR * 0.30f
        val pupilMaxShiftX = barrelInnerR * 0.30f
        val tiltShiftX = barrelInnerR * 0.08f * smoothTiltX
        val tiltShiftY = barrelInnerR * 0.08f * smoothTiltY

        // Orbita a spirale decrescente quando stordita
        val dizzySpiralAngle = (1f - dizzyProgress) * 1440f * (Math.PI.toFloat() / 180f)
        val dizzyOrbitR = barrelInnerR * 0.22f * dizzyProgress
        val dizzyShiftX = dizzyOrbitR * cos(dizzySpiralAngle)
        val dizzyShiftY = dizzyOrbitR * sin(dizzySpiralAngle)

        // Micro-tremore da stizza/agitazione al tocco ripetuto
        val annoyedJitterX = if (annoyedProgress > 0f) {
            sin((1f - annoyedProgress) * 60f) * (barrelInnerR * 0.08f) * annoyedProgress
        } else 0f

        val pupilCenterY = centerY + (pupilMaxShiftY * pupilShiftYRatio) + tiltShiftY + dizzyShiftY
        val pupilCenterX = centerX + (pupilMaxShiftX * pupilShiftXRatio) + tiltShiftX + dizzyShiftX + annoyedJitterX

        val outerR = barrelInnerR * 1.35f
        val innerR = (barrelInnerR * effectiveAperture).coerceAtLeast(1.5f)
        val bladeCount = 6
        val tanLen = sqrt((outerR * outerR - innerR * innerR).coerceAtLeast(0f))

        // La camera interna è ritagliata all'interno del barilotto metallico fisso (zero allocazioni Path)
        clipCameraPath.reset()
        clipCameraPath.addOval(Rect(center = Offset(centerX, centerY), radius = barrelInnerR))
        clipPath(clipCameraPath) {
            // A. Bagliore profondo centrale nella pupilla (ancorato al centro pupillare mobile)
            val glowRadius = (innerR * 1.8f).coerceAtLeast(2f)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to activeAccentColor.copy(alpha = if (mood == IrisMood.SEARCHING) 0.60f else 0.35f),
                    0.5f to activeAccentColor.copy(alpha = 0.12f),
                    1.0f to Color.Transparent,
                    center = Offset(pupilCenterX, pupilCenterY),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(pupilCenterX, pupilCenterY)
            )

            // B1. Superfici geometriche delle 6 lamelle (livello z-index inferiore - zero allocazioni Path)
            for (i in 0 until bladeCount) {
                val phi = (effectiveAngle + i * (360f / bladeCount)) * (Math.PI.toFloat() / 180f)
                val nextPhi = (effectiveAngle + (i + 1) * (360f / bladeCount)) * (Math.PI.toFloat() / 180f)

                val cosP = cos(phi)
                val sinP = sin(phi)
                val cosNext = cos(nextPhi)
                val sinNext = sin(nextPhi)

                // Vertice tangenziale sull'apertura interna
                val tx = pupilCenterX + innerR * cosP
                val ty = pupilCenterY + innerR * sinP

                // Vertice tangenziale sul cerchio esterno
                val px = tx - tanLen * sinP
                val py = ty + tanLen * cosP

                val nextTx = pupilCenterX + innerR * cosNext
                val nextTy = pupilCenterY + innerR * sinNext
                val nextPx = nextTx - tanLen * sinNext
                val nextPy = nextTy + tanLen * cosNext

                // Superficie della singola lamella (riutilizzo bladeReusablePath)
                bladeReusablePath.reset()
                bladeReusablePath.moveTo(tx, ty)
                bladeReusablePath.lineTo(px, py)
                bladeReusablePath.lineTo(nextPx, nextPy)
                bladeReusablePath.lineTo(nextTx, nextTy)
                bladeReusablePath.close()

                drawPath(
                    path = bladeReusablePath,
                    color = Color(0xFF13181D).copy(alpha = 0.95f)
                )
            }

            // B2. Bordi nitidi di scorrimento delle lamelle (livello z-index superiore)
            for (i in 0 until bladeCount) {
                val phi = (effectiveAngle + i * (360f / bladeCount)) * (Math.PI.toFloat() / 180f)
                val cosP = cos(phi)
                val sinP = sin(phi)

                val tx = pupilCenterX + innerR * cosP
                val ty = pupilCenterY + innerR * sinP
                val px = tx - tanLen * sinP
                val py = ty + tanLen * cosP

                drawLine(
                    color = activeAccentColor.copy(alpha = 0.85f),
                    start = Offset(px, py),
                    end = Offset(tx, ty),
                    strokeWidth = bladeLineWidth
                )
            }

            // C. Bordo dell'apertura centrale poligonale (la pupilla esagonale mobile - zero allocazioni Path/Stroke)
            apertureReusablePath.reset()
            for (i in 0 until bladeCount) {
                val phi = (effectiveAngle + i * (360f / bladeCount)) * (Math.PI.toFloat() / 180f)
                val tx = pupilCenterX + innerR * cos(phi)
                val ty = pupilCenterY + innerR * sin(phi)
                if (i == 0) apertureReusablePath.moveTo(tx, ty) else apertureReusablePath.lineTo(tx, ty)
            }
            apertureReusablePath.close()

            drawPath(
                path = apertureReusablePath,
                color = activeAccentColor.copy(alpha = 0.90f),
                style = apertureStroke
            )

            // 4. CATCHLIGHTS OTTICI (Riflessi di luce grandi, lucidi ed espressivi sul vetro ottico stile Pixar / Anime)
            if (effectiveAperture > 0.10f) {
                val driftPx = gazeDrift * oneDpPx
                val catchlightParallaxX = barrelInnerR * 0.05f * smoothTiltX
                val catchlightParallaxY = barrelInnerR * 0.05f * smoothTiltY

                // A. Riflesso primario superiore (Big Anime/Pixar Sparkle Glint)
                val glint1X = pupilCenterX + (innerR * 0.24f) + driftPx + catchlightParallaxX
                val glint1Y = pupilCenterY - (innerR * 0.26f) + catchlightParallaxY
                val glint1R = (innerR * 0.28f).coerceAtLeast(2.4f)

                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.95f),
                        0.50f to Color.White.copy(alpha = 0.75f),
                        0.80f to Color.White.copy(alpha = 0.25f),
                        1.0f to Color.Transparent,
                        center = Offset(glint1X, glint1Y),
                        radius = glint1R
                    ),
                    radius = glint1R,
                    center = Offset(glint1X, glint1Y)
                )

                // B. Riflesso secondario inferiore (Fill Glint)
                val glint2X = pupilCenterX - (innerR * 0.20f) + driftPx * 0.5f - catchlightParallaxX * 0.5f
                val glint2Y = pupilCenterY + (innerR * 0.22f) - catchlightParallaxY * 0.5f
                val glint2R = (innerR * 0.15f).coerceAtLeast(1.5f)

                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.75f),
                        0.55f to activeAccentColor.copy(alpha = 0.40f),
                        1.0f to Color.Transparent,
                        center = Offset(glint2X, glint2Y),
                        radius = glint2R
                    ),
                    radius = glint2R,
                    center = Offset(glint2X, glint2Y)
                )

                // C. Micro-scintilla lucida supplementare (Dewy Lens Specular Sparkle)
                val glint3X = pupilCenterX + (innerR * 0.08f) + driftPx * 0.7f + catchlightParallaxX * 0.7f
                val glint3Y = pupilCenterY - (innerR * 0.04f) + catchlightParallaxY * 0.7f
                val glint3R = (innerR * 0.09f).coerceAtLeast(1.0f)

                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.90f),
                        0.60f to Color.White.copy(alpha = 0.45f),
                        1.0f to Color.Transparent,
                        center = Offset(glint3X, glint3Y),
                        radius = glint3R
                    ),
                    radius = glint3R,
                    center = Offset(glint3X, glint3Y)
                )
            }
        }
    }
}
