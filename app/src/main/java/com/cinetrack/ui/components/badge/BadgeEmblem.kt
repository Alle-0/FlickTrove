package com.cinetrack.ui.components.badge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.util.VibrationHelper
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


// ════════════════════════════════════════════════════════════════════
// 1. EMBLEMA CINEMATOGRAFICO SU MISURA FLICKTROVE
// Purezza cromatica: nessun doppio colore ibrido. Il metallo satinato
// adotta esclusivamente la palette tonale del Tier con riflesso speculare naturale.
// ════════════════════════════════════════════════════════════════════

private fun Float.safeRoundToInt(): Int {
    if (this.isNaN() || this.isInfinite()) return 0
    return this.roundToInt()
}

private fun Float.safeCoerceTilt(): Float {
    if (this.isNaN() || this.isInfinite()) return 0f
    return this.coerceIn(-1.2f, 1.2f)
}

@Composable
fun FlickTroveBadgeEmblem(
    tier: PreviewBadgeTier,
    iconKind: CustomBadgeIconKind,
    isUnlocked: Boolean = true,
    progressFraction: Float = 1f,
    showProgressRing: Boolean = false,
    size: Dp = 92.dp,
    enableInteractiveParallax: Boolean = size >= 90.dp,
    enablePeriodicGleam: Boolean = isUnlocked && size >= 90.dp,
    modifier: Modifier = Modifier
) {
    val tierColor by animateColorAsState(
        targetValue = if (isUnlocked) tier.primaryColor else Color(0xFF64748B),
        animationSpec = tween(300),
        label = "EmblemColor"
    )

    val glowColor by animateColorAsState(
        targetValue = if (isUnlocked) tier.glowColor else Color.Transparent,
        animationSpec = tween(300),
        label = "EmblemGlow"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "EmblemProgress"
    )

    // ── 1. EFFETTO LUCCICHIO PERIODICO (Gleam calmo, attivo SOLO su emblema ingrandito >= 90.dp) ──
    val shimmerDelayMs = remember(iconKind, tier) {
        ((iconKind.hashCode() and 0x7FFFFFFF) % 4000)
    }
    val gleamProgress: Float = if (enablePeriodicGleam && isUnlocked) {
        val infiniteTransition = rememberInfiniteTransition(label = "EmblemInfiniteTransition")
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 9000
                    0.0f at 0 using LinearOutSlowInEasing
                    1.0f at 1300 using FastOutSlowInEasing
                    1.0f at 9000
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(shimmerDelayMs)
            ),
            label = "GleamProgress"
        )
        progress
    } else {
        0f
    }

    // ── 2. EFFETTO PARALLASSE TATTILE 3D + GIROSCOPIO FISICO ──
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Valori effettivi di inclinazione che pilotano prospettiva, luce e ombre
    val effectiveTiltX = remember { mutableFloatStateOf(0f) }
    val effectiveTiltY = remember { mutableFloatStateOf(0f) }

    // Tracciamento stato tocco
    var isTouchActive by remember { mutableStateOf(false) }
    var touchPosX by remember { mutableFloatStateOf(0f) }
    var touchPosY by remember { mutableFloatStateOf(0f) }
    val touchSpringOffsetX = remember { Animatable(0f) }
    val touchSpringOffsetY = remember { Animatable(0f) }

    // Valori correnti dal sensore di movimento del dispositivo
    val gyroFilteredX = remember { mutableFloatStateOf(0f) }
    val gyroFilteredY = remember { mutableFloatStateOf(0f) }

    // Registrazione del sensore (Rotation Vector / Game Rotation Vector / Accelerometro)
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    DisposableEffect(enableInteractiveParallax, sensorManager) {
        if (!enableInteractiveParallax || sensorManager == null) {
            return@DisposableEffect onDispose {}
        }

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensor == null) {
            return@DisposableEffect onDispose {}
        }

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val remappedMatrix = FloatArray(9)
            private val orientation = FloatArray(3)
            private var baselinePitch = Float.NaN
            private var baselineRoll = Float.NaN
            private var warmupSamples = 0
            private var filteredX = 0f
            private var filteredY = 0f

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                val targetX: Float
                val targetY: Float

                if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                    // Il giroscopio misura la velocità angolare reale (rad/s)
                    // A riposo è rigorosamente 0.0: l'emblema parte 100% centrato e dritto
                    val rawGyroPitch = event.values[0]
                    val rawGyroRoll = event.values[1]
                    if (rawGyroPitch.isNaN() || rawGyroRoll.isNaN() || rawGyroPitch.isInfinite() || rawGyroRoll.isInfinite()) {
                        return
                    }
                    val rateX = rawGyroRoll.coerceIn(-5f, 5f)
                    val rateY = (-rawGyroPitch).coerceIn(-5f, 5f)

                    // Risposta dinamica proporzionale al moto di inclinazione del polso
                    val impulseX = (rateX * 0.45f).safeCoerceTilt()
                    val impulseY = (rateY * 0.45f).safeCoerceTilt()

                    // Ritorno elastico continuo al centro (0f) non appena il moto si arresta
                    filteredX = (filteredX * 0.82f + impulseX * 0.18f)
                    filteredY = (filteredY * 0.82f + impulseY * 0.18f)

                    // Deadzone minimale per azzerare micro-rumore
                    targetX = if (kotlin.math.abs(filteredX) < 0.015f) 0f else filteredX.safeCoerceTilt()
                    targetY = if (kotlin.math.abs(filteredY) < 0.015f) 0f else filteredY.safeCoerceTilt()
                } else {
                    // Fallback per dispositivi sprovvisti di giroscopio puro
                    val (rawPitch, rawRoll) = when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            SensorManager.remapCoordinateSystem(
                                rotationMatrix,
                                SensorManager.AXIS_X,
                                SensorManager.AXIS_Z,
                                remappedMatrix
                            )
                            SensorManager.getOrientation(remappedMatrix, orientation)
                            orientation[1] to orientation[2]
                        }
                        Sensor.TYPE_ACCELEROMETER -> {
                            val ax = event.values[0]
                            val ay = event.values[1]
                            val az = event.values[2]
                            val denom = kotlin.math.sqrt((ay * ay + az * az).toDouble())
                            val r = if (denom > 0.0001) kotlin.math.atan2(ax.toDouble(), denom).toFloat() else 0f
                            val p = kotlin.math.atan2(-ay.toDouble(), az.toDouble()).toFloat()
                            p to r
                        }
                        else -> return
                    }

                    if (rawPitch.isNaN() || rawRoll.isNaN() || rawPitch.isInfinite() || rawRoll.isInfinite()) {
                        return
                    }
                    val pitch = rawPitch.coerceIn(-3.14f, 3.14f)
                    val roll = rawRoll.coerceIn(-3.14f, 3.14f)

                    // Finestra di calibrazione iniziale: cattura la postura reale della mano all'apertura del modale
                    if (warmupSamples < 15) {
                        warmupSamples++
                        baselinePitch = if (baselinePitch.isNaN()) pitch else baselinePitch * 0.70f + pitch * 0.30f
                        baselineRoll = if (baselineRoll.isNaN()) roll else baselineRoll * 0.70f + roll * 0.30f
                        return
                    }

                    // Compensazione deriva lenta
                    baselinePitch = baselinePitch * 0.994f + pitch * 0.006f
                    baselineRoll = baselineRoll * 0.994f + roll * 0.006f

                    // Deadzone di 3.5 gradi per evitare che l'inclinazione naturale della mano ruoti l'emblema da fermo
                    val deltaRoll = roll - baselineRoll
                    val deltaPitch = pitch - baselinePitch
                    val rawTargetX = if (kotlin.math.abs(deltaRoll) < 0.06f) 0f else (deltaRoll / 0.32f)
                    val rawTargetY = if (kotlin.math.abs(deltaPitch) < 0.06f) 0f else (-deltaPitch / 0.32f)

                    filteredX = filteredX * 0.80f + rawTargetX.safeCoerceTilt() * 0.20f
                    filteredY = filteredY * 0.80f + rawTargetY.safeCoerceTilt() * 0.20f

                    targetX = filteredX.safeCoerceTilt()
                    targetY = filteredY.safeCoerceTilt()
                }

                gyroFilteredX.floatValue = if (targetX.isNaN()) 0f else targetX
                gyroFilteredY.floatValue = if (targetY.isNaN()) 0f else targetY

                if (!isTouchActive) {
                    effectiveTiltX.floatValue = (targetX + touchSpringOffsetX.value).safeCoerceTilt()
                    effectiveTiltY.floatValue = (targetY + touchSpringOffsetY.value).safeCoerceTilt()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val touchModifier = if (enableInteractiveParallax) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                VibrationHelper.vibrateTick(context)
                val halfW = this.size.width / 2f
                val halfH = this.size.height / 2f
                val startNormX = if (halfW > 0f) ((down.position.x - halfW) / halfW).safeCoerceTilt() else 0f
                val startNormY = if (halfH > 0f) ((down.position.y - halfH) / halfH).safeCoerceTilt() else 0f

                isTouchActive = true
                touchPosX = startNormX
                touchPosY = startNormY

                // Glissato armonico dal punto corrente del giroscopio al punto di tocco del dito
                coroutineScope.launch {
                    val initialDeltaX = effectiveTiltX.floatValue - startNormX
                    val initialDeltaY = effectiveTiltY.floatValue - startNormY
                    touchSpringOffsetX.snapTo(if (initialDeltaX.isNaN()) 0f else initialDeltaX)
                    touchSpringOffsetY.snapTo(if (initialDeltaY.isNaN()) 0f else initialDeltaY)
                    launch {
                        touchSpringOffsetX.animateTo(
                            0f,
                            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                        ) {
                            if (isTouchActive) {
                                effectiveTiltX.floatValue = (touchPosX + value).safeCoerceTilt()
                            }
                        }
                    }
                    launch {
                        touchSpringOffsetY.animateTo(
                            0f,
                            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                        ) {
                            if (isTouchActive) {
                                effectiveTiltY.floatValue = (touchPosY + value).safeCoerceTilt()
                            }
                        }
                    }
                }

                var pointerId = down.id
                while (true) {
                    val event = awaitPointerEvent()
                    val currentPointer = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (!currentPointer.pressed) break
                    currentPointer.consume()
                    val normX = if (halfW > 0f) ((currentPointer.position.x - halfW) / halfW).safeCoerceTilt() else 0f
                    val normY = if (halfH > 0f) ((currentPointer.position.y - halfH) / halfH).safeCoerceTilt() else 0f
                    touchPosX = normX
                    touchPosY = normY
                    effectiveTiltX.floatValue = (normX + touchSpringOffsetX.value).safeCoerceTilt()
                    effectiveTiltY.floatValue = (normY + touchSpringOffsetY.value).safeCoerceTilt()
                }

                // Rilascio del tocco: ritorno elastico armonico e continuo alla posizione viva del giroscopio
                isTouchActive = false
                val releaseDeltaX = touchPosX - gyroFilteredX.floatValue
                val releaseDeltaY = touchPosY - gyroFilteredY.floatValue

                coroutineScope.launch {
                    touchSpringOffsetX.snapTo(if (releaseDeltaX.isNaN()) 0f else releaseDeltaX)
                    touchSpringOffsetY.snapTo(if (releaseDeltaY.isNaN()) 0f else releaseDeltaY)
                    launch {
                        touchSpringOffsetX.animateTo(
                            0f,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) {
                            if (!isTouchActive) {
                                effectiveTiltX.floatValue = (gyroFilteredX.floatValue + value).safeCoerceTilt()
                            }
                        }
                    }
                    launch {
                        touchSpringOffsetY.animateTo(
                            0f,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) {
                            if (!isTouchActive) {
                                effectiveTiltY.floatValue = (gyroFilteredY.floatValue + value).safeCoerceTilt()
                            }
                        }
                    }
                }
            }
        }
    } else {
        Modifier
    }

    // CONTENITORE RADICE (Stazionario, riceve il tocco ed ospita l'ombra dinamica sul piano di fondo)
    Box(
        modifier = modifier
            .size(size)
            .then(touchModifier),
        contentAlignment = Alignment.Center
    ) {
        // 0. OMBRA PROIETTATA SUL PIANO SOTTOSTANTE (Cast Drop Shadow & Ambilight Ambient Glow)
        // Questo layer rimane sul piano piatto del layout e trasla in direzione opposta al tilt,
        // dando l'illusione fisica che la moneta sia sospesa a mezz'aria sopra lo schermo!
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            // L'ombra si allontana dal punto di sollevamento della moneta in base al tilt
            val shadowOffsetX = -effectiveTiltX.floatValue * radius * 0.16f
            val shadowOffsetY = -effectiveTiltY.floatValue * radius * 0.16f + radius * 0.08f // sorgente di luce dall'alto
            val shadowCenter = Offset(center.x + shadowOffsetX, center.y + shadowOffsetY)

            // 0a. Bagliore Ambilight volumetrico sul fondo (se sbloccato)
            if (isUnlocked) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.38f),
                            glowColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(center.x + shadowOffsetX * 0.5f, center.y + shadowOffsetY * 0.5f),
                        radius = radius * 1.25f
                    ),
                    radius = radius * 1.25f,
                    center = Offset(center.x + shadowOffsetX * 0.5f, center.y + shadowOffsetY * 0.5f)
                )
            }

            // 0b. Ombra di contatto scura morbida e sagomata sul fondo
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.65f),
                        Color.Black.copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = shadowCenter,
                    radius = radius * 0.88f
                ),
                topLeft = Offset(shadowCenter.x - radius * 0.85f, shadowCenter.y - radius * 0.65f),
                size = androidx.compose.ui.geometry.Size(radius * 1.70f, radius * 1.30f)
            )
        }

        // 1. CORPO ROTANTE 3D DELLA MONETA (GraphicsLayer con prospettiva e cameraDistance)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = effectiveTiltX.floatValue * 16f
                    rotationX = -effectiveTiltY.floatValue * 16f
                    cameraDistance = 18f * density
                },
            contentAlignment = Alignment.Center
        ) {
            // A. BACKGROUND CANVAS (Spessore Estruso Moneta, Lente Dark, Tacche ottiche, Ghiera Speculare Dinamica)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = (this.size.minDimension / 2f)

                val lensRadius = if (showProgressRing) radius * 0.92f else radius * 0.88f
                val tickInnerR = if (showProgressRing) radius * 0.72f else radius * 0.82f
                val tickOuterR = if (showProgressRing) radius * 0.78f else radius * 0.88f
                val innerRingR = if (showProgressRing) radius * 0.60f else radius * 0.68f

                // ── A1. SPESSORE ESTRUSO 3D DELLA MONETA (Coin Rim Extrusion) ──
                // In prospettiva, lo spessore posteriore di una moneta inclinata proietta
                // uno scostamento scalare lungo l'asse (-tiltX, -tiltY).
                // Disegnando 4 fette scalari arretrate, emerge fisicamente il fianco metallico della medaglia!
                val maxRimDepth = radius * 0.065f
                val rimOffsetMagnitude = (effectiveTiltX.floatValue * effectiveTiltX.floatValue + effectiveTiltY.floatValue * effectiveTiltY.floatValue).coerceAtMost(1.5f)

                if (rimOffsetMagnitude > 0.001f) {
                    val rimColorDark = Color(0xFF0B0C12)
                    val rimColorMetal = if (isUnlocked) {
                        tier.secondaryColor.copy(alpha = 0.65f)
                    } else {
                        Color(0xFF334155).copy(alpha = 0.35f)
                    }

                    for (slice in 4 downTo 1) {
                        val depthFrac = slice / 4f
                        val sliceOffset = Offset(
                            -effectiveTiltX.floatValue * maxRimDepth * depthFrac,
                            -effectiveTiltY.floatValue * maxRimDepth * depthFrac
                        )
                        val sliceCenter = center + sliceOffset

                        // Fetta del corpo metallico della costa della moneta
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(rimColorDark, rimColorMetal, rimColorDark),
                                center = sliceCenter,
                                radius = lensRadius
                            ),
                            radius = lensRadius,
                            center = sliceCenter
                        )

                        // Zigrinatura / Bordo conico esterno (Costa della moneta)
                        drawCircle(
                            color = rimColorMetal.copy(alpha = 0.40f * (1f - depthFrac * 0.5f)),
                            radius = lensRadius,
                            center = sliceCenter,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = radius * 0.015f
                            )
                        )
                    }
                }

                // ── A2. BASE SATINATA DELLA LENTE (Faccia frontale dark cinema) ──
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isUnlocked) {
                            listOf(
                                Color(0xFF0F1118),
                                Color(0xFF0A0C12),
                                Color(0xFF06070B)
                            )
                        } else {
                            listOf(
                                Color(0xFF13151D),
                                Color(0xFF0B0C12)
                            )
                        },
                        center = center,
                        radius = lensRadius
                    ),
                    radius = lensRadius,
                    center = center
                )

                // ── A3. TACCHE OTTICHE GRADUATE A TELEMETRO CINEMATOGRAFICO ──
                val tickCount = 24
                val tickColor = if (isUnlocked) tierColor.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.08f)

                for (i in 0 until tickCount) {
                    val angle = Math.toRadians((i * (360.0 / tickCount)))
                    val startX = center.x + (tickInnerR * cos(angle)).toFloat()
                    val startY = center.y + (tickInnerR * sin(angle)).toFloat()
                    val endX = center.x + (tickOuterR * cos(angle)).toFloat()
                    val endY = center.y + (tickOuterR * sin(angle)).toFloat()

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = radius * 0.018f
                    )
                }

                // ── A4. ANELLO CONCENTRICO DI RIFINITURA INTERNA ──
                drawCircle(
                    color = if (isUnlocked) tierColor.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.08f),
                    radius = innerRingR,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = radius * 0.02f
                    )
                )

                // ── A5. GHIERA METALLICA SPECULARE DINAMICA / PROGRESS PILL ──
                if (showProgressRing) {
                    val ringRadius = radius * 0.92f
                    val ringStroke = (radius * 0.065f).coerceAtLeast(2.5.dp.toPx())

                    // Traccia satinata di sfondo scuro
                    drawCircle(
                        color = Color.White.copy(alpha = 0.09f),
                        radius = ringRadius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = ringStroke
                        )
                    )

                    // Pillola di progresso curva con estremità arrotondate
                    if (animatedProgress >= 0.999f) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    tierColor,
                                    tier.secondaryColor,
                                    Color.White.copy(alpha = 0.95f),
                                    tierColor,
                                    tier.secondaryColor,
                                    tierColor
                                ),
                                center = center
                            ),
                            radius = ringRadius,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = ringStroke
                            )
                        )
                    } else if (animatedProgress > 0f) {
                        val sweep = 360f * animatedProgress
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    tierColor,
                                    tier.secondaryColor,
                                    tierColor
                                ),
                                center = center
                            ),
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                            size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = ringStroke,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                } else {
                    // GHIERA A BISIELLO METALLICO 3D CON RIFLESSO SPECULARE DINAMICO
                    // 1. Smusso esterno (Outer Chamfer): metallo pieno lucidato
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = if (isUnlocked) {
                                listOf(
                                    tierColor,
                                    tier.secondaryColor,
                                    Color.White.copy(alpha = 0.95f),
                                    tierColor,
                                    tier.secondaryColor,
                                    tierColor
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.20f),
                                    Color.White.copy(alpha = 0.06f),
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.06f),
                                    Color.White.copy(alpha = 0.20f)
                                )
                            },
                            center = center
                        ),
                        radius = radius * 0.88f,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = radius * 0.045f
                        )
                    )

                    // 2. Filo di luce diamantato interno (Inner Diamond-cut rim)
                    drawCircle(
                        color = if (isUnlocked) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.06f),
                        radius = radius * 0.86f,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = radius * 0.012f
                        )
                    )
                }
            }

            // B. AREA CENTRALE CON ICONA A BASSORILIEVO CONIATO (EFFETTO 3D SCULPTED)
            val innerCircleSize = if (showProgressRing) size * 0.60f else size * 0.68f
            val standardIconSize = if (showProgressRing) size * 0.46f else size * 0.52f

            val currentIconSize = when {
                iconKind.iconRes == R.drawable.ic_badge_light_projector ||
                iconKind.iconRes == R.drawable.ic_badge_screaming ||
                iconKind.iconRes == R.drawable.ic_badge_shaking_hands -> innerCircleSize * 1.15f
                iconKind.iconRes == R.drawable.ic_badge_theater_curtains -> innerCircleSize * 1.15f
                iconKind.iconRes == R.drawable.ic_badge_pocket_watch -> innerCircleSize * 1.15f
                else -> standardIconSize
            }

            val isShiftedUpIcon = iconKind.iconRes == R.drawable.ic_badge_hot_meal ||
                iconKind.iconRes == R.drawable.ic_badge_star_altar ||
                iconKind.iconRes == R.drawable.ic_badge_tv ||
                iconKind.iconRes == R.drawable.ic_badge_magnifying_glass ||
                iconKind.iconRes == R.drawable.ic_badge_film_projector ||
                iconKind.iconRes == R.drawable.ic_badge_scales ||
                iconKind.iconRes == R.drawable.ic_badge_acoustic_megaphone ||
                iconKind.iconRes == R.drawable.ic_badge_lantern_flame

            val isShiftedDownIcon = iconKind.iconRes == R.drawable.ic_badge_star_medal

            val iconOffsetY = when {
                isShiftedUpIcon -> -size * 0.032f
                isShiftedDownIcon -> size * 0.028f
                else -> 0.dp
            }

            // Parallasse dell'icona in secondo piano
            val maxParallaxDistance = innerCircleSize * 0.14f
            val density = LocalDensity.current
            val maxParallaxPx = with(density) { maxParallaxDistance.toPx() }
            val iconOffsetPx = with(density) { iconOffsetY.toPx() }

            Box(
                modifier = Modifier
                    .size(innerCircleSize)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    // Icona limpida e razor-sharp, senza sdoppiamenti, recesso in profondità con pura parallasse cinematografica
                    Icon(
                        painter = painterResource(id = iconKind.iconRes),
                        contentDescription = null,
                        tint = tierColor,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (-effectiveTiltX.floatValue * maxParallaxPx).safeRoundToInt(),
                                    y = (iconOffsetPx + (-effectiveTiltY.floatValue * maxParallaxPx)).safeRoundToInt()
                                )
                            }
                            .wrapContentSize(align = Alignment.Center, unbounded = true)
                            .requiredSize(currentIconSize)
                    )
                } else if (tier == PreviewBadgeTier.LOST_REEL) {
                    // I trofei segreti Lost Reel mantengono il lucchetto
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lock),
                        contentDescription = "Trofeo Segreto",
                        tint = Color(0xFF818CF8).copy(alpha = 0.70f),
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (-effectiveTiltX.floatValue * maxParallaxPx * 0.7f).safeRoundToInt(),
                                    y = (-effectiveTiltY.floatValue * maxParallaxPx * 0.7f).safeRoundToInt()
                                )
                            }
                            .size(standardIconSize * 0.82f)
                    )
                } else {
                    // I trofei standard non sbloccati
                    Icon(
                        painter = painterResource(id = iconKind.iconRes),
                        contentDescription = "Non sbloccato",
                        tint = Color(0xFF94A3B8).copy(alpha = 0.40f),
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (-effectiveTiltX.floatValue * maxParallaxPx).safeRoundToInt(),
                                    y = (iconOffsetPx + (-effectiveTiltY.floatValue * maxParallaxPx)).safeRoundToInt()
                                )
                            }
                            .wrapContentSize(align = Alignment.Center, unbounded = true)
                            .requiredSize(currentIconSize)
                    )
                }
            }

            // C. FOREGROUND GLASS CANVAS: OMBRE DI INCASSO & LUCCICHIO DIAGONALE SUL VETRO
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = (this.size.minDimension / 2f)
                val lensRadius = if (showProgressRing) radius * 0.92f else radius * 0.88f
                val innerRingR = if (showProgressRing) radius * 0.60f else radius * 0.68f

                // 1. Ombra di incasso (Bezel Inner Shadow): la ghiera esterna proietta una sfumatura
                // morbida continua e uniforme sul perimetro dell'oblò, senza stacchi netti o tagli improvvisi
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        0.68f to Color.Black.copy(alpha = 0.30f),
                        0.86f to Color.Black.copy(alpha = 0.68f),
                        0.96f to Color.Black.copy(alpha = 0.88f),
                        1.0f to Color.Black.copy(alpha = 0.96f),
                        center = center,
                        radius = innerRingR
                    ),
                    radius = innerRingR,
                    center = center
                )

                val glassPath = Path().apply {
                    addOval(
                        Rect(
                            center.x - lensRadius,
                            center.y - lensRadius,
                            center.x + lensRadius,
                            center.y + lensRadius
                        )
                    )
                }

                clipPath(glassPath) {
                    // 2. Luccichio Periodico a Spazzamento Diagonale sul Vetro (ogni ~9s)
                    // Fading continuo sfumato ai bordi con Color.Transparent a 45°, nessun bordo tronco o netto
                    if (enablePeriodicGleam && isUnlocked && gleamProgress in 0.001f..0.999f) {
                        val sweepMin = -lensRadius * 1.6f
                        val sweepMax = lensRadius * 1.6f
                        val currentSweep = sweepMin + (sweepMax - sweepMin) * gleamProgress

                        val dirX = 0.7071f
                        val dirY = 0.7071f
                        val beamHalfWidth = lensRadius * 0.35f

                        val startSweep = Offset(
                            center.x + dirX * (currentSweep - beamHalfWidth),
                            center.y + dirY * (currentSweep - beamHalfWidth)
                        )
                        val endSweep = Offset(
                            center.x + dirX * (currentSweep + beamHalfWidth),
                            center.y + dirY * (currentSweep + beamHalfWidth)
                        )

                        val beamColors = if (isUnlocked) {
                            listOf(
                                Color.Transparent,
                                tierColor.copy(alpha = 0.04f),
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.24f),
                                Color.White.copy(alpha = 0.12f),
                                tierColor.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.03f),
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.03f),
                                Color.Transparent
                            )
                        }

                        drawRect(
                            brush = Brush.linearGradient(
                                colors = beamColors,
                                start = startSweep,
                                end = endSweep
                            ),
                            size = this.size
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
