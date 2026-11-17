package com.cinetrack.ui.components.mascot

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Stati emotivi e operativi di IRIS (Mascotte / Occhio Ottico Cinematografico).
 */
enum class IrisMood {
    IDLE,        // Respirazione calma del diaframma, battito periodico, sguardo vivace
    TYPING,      // Sguardo incuriosito rivolto in alto (verso la barra di ricerca), iride dilatata a f/2, micro-inclinazione attenta
    SEARCHING,   // Rotazione continua delle lamelle, focus hunting (messa a fuoco), fascio pulsante
    DELIGHTED,   // Apertura ampia f/1.2, bagliore luminoso
    EMPTY,       // Diaframma socchiuso f/11, sguardo riflessivo nel buio
    OFFLINE      // Otturatore a riposo
}

/**
 * IRIS: Mascotte Cinematografica Ufficiale di FlickTrove.
 *
 * Rappresenta un obiettivo cinematografico vivo (diaframma ad apertura ottica a 6 lamelle).
 * L'espressività scaturisce dalla fisica ottica:
 * - Dilatazione e contrazione delle lamelle (f-stops da f/1.2 a f/22).
 * - "Sguardo" ottico vivente mediante catchlights (riflessi di luce interni).
 * - Calibrazione della ghiera dei diaframmi sul barilotto esterno.
 */
@Composable
fun IrisMascot(
    modifier: Modifier = Modifier,
    mood: IrisMood = IrisMood.IDLE,
    size: Dp = 80.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    gazeBiasX: Float = 0f,
    enableTiltParallax: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Chiusura protettiva e ammiccamento (Wink & Touch Defense)
    var isTouched by remember { mutableStateOf(false) }
    var hasBeenTouched by remember { mutableStateOf(false) }
    val winkAnim = remember { Animatable(0f) }

    // Micro-parallasse giroscopica e rilevamento scuotimento dello smartphone (Shake to Dizzy)
    var rawTiltX by remember { mutableFloatStateOf(0f) }
    var rawTiltY by remember { mutableFloatStateOf(0f) }
    var isDizzy by remember { mutableStateOf(false) }
    val dizzyAnim = remember { Animatable(0f) }

    LaunchedEffect(isDizzy) {
        if (isDizzy) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            dizzyAnim.snapTo(1f)
            dizzyAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing)
            )
            // Chiusura rapida di ricalibrazione e riapertura elastica (shutter calibration blink)
            winkAnim.animateTo(1f, tween(85, easing = FastOutLinearInEasing))
            winkAnim.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            isDizzy = false
        }
    }

    if (enableTiltParallax) {
        DisposableEffect(Unit) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            if (sensorManager == null || sensor == null) {
                onDispose { }
            } else {
                var lastShakeTime = 0L
                var shakeCount = 0

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event == null) return
                        val gX = event.values[0]
                        val gY = event.values[1]
                        val gZ = event.values[2]

                        // 1. Inclinazione 2D (con filtro a m/s^2)
                        val x = (-gX / 5.5f).coerceIn(-1f, 1f)
                        val y = ((gY - 6.0f) / 5.5f).coerceIn(-1f, 1f)
                        rawTiltX = x
                        rawTiltY = y

                        // 2. Rilevamento scuotimento vigoroso (Shake)
                        val gMagnitude = sqrt(gX * gX + gY * gY + gZ * gZ) / SensorManager.GRAVITY_EARTH
                        if (gMagnitude > 2.2f) {
                            val now = System.currentTimeMillis()
                            if (now - lastShakeTime < 650L) {
                                shakeCount++
                                if (shakeCount >= 2 && !isDizzy) {
                                    isDizzy = true
                                    shakeCount = 0
                                }
                            } else {
                                shakeCount = 1
                            }
                            lastShakeTime = now
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                onDispose {
                    sensorManager.unregisterListener(listener)
                }
            }
        }
    }

    val smoothTiltX by animateFloatAsState(
        targetValue = if (enableTiltParallax) rawTiltX else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iris_tilt_x"
    )

    val smoothTiltY by animateFloatAsState(
        targetValue = if (enableTiltParallax) rawTiltY else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iris_tilt_y"
    )

    // 1. Chiusura protettiva al tocco (Blink & Touch defense)
    // IRIS chiude il diaframma istantaneamente appena viene toccata (press down)
    // e rimane protetta e chiusa per tutto il tempo in cui il dito tocca la lente,
    // per poi riaprirsi con un rimbalzo ottico elastico non appena il dito viene sollevato.
    LaunchedEffect(isTouched) {
        if (isTouched) {
            hasBeenTouched = true
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            winkAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 75, easing = FastOutLinearInEasing)
            )
        } else if (hasBeenTouched) {
            // Se rilasciato prima di aver raggiunto la chiusura completa (es. tap super rapido),
            // completa rapidamente la chiusura prima del rimbalzo di riapertura
            if (winkAnim.value < 0.75f) {
                winkAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 35, easing = LinearEasing)
                )
            }
            winkAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // Battito spontaneo periodico quando non è toccata dall'utente e non è stordita
    LaunchedEffect(mood) {
        if (mood == IrisMood.IDLE || mood == IrisMood.TYPING) {
            while (true) {
                delay(if (mood == IrisMood.TYPING) 6500L else 4500L + (0..2000).random())
                if (!isTouched && !isDizzy) {
                    // Chiusura rapida (shutter snap) e riapertura elastica
                    winkAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                    )
                    winkAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        } else {
            winkAnim.snapTo(0f)
        }
    }

    // 2. Transizioni per respirazione, rotazione e focus hunting
    val infiniteTransition = rememberInfiniteTransition(label = "iris_mascot_loop")

    // Respirazione diaframma a riposo (Aperture breathing IDLE)
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0.36f,
        targetValue = 0.46f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_idle_breathing"
    )

    // Dilatazione attenta ed empatica mentre l'utente digita (TYPING f/2)
    val typingBreathing by infiniteTransition.animateFloat(
        initialValue = 0.48f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_typing_breathing"
    )

    // Focus hunting rapido durante la ricerca
    val searchingBreathing by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_searching_breathing"
    )

    // Rotazione continua delle lamelle (Shutter Angle)
    val searchingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing)
        ),
        label = "iris_searching_rotation"
    )

    // Rotazione inversa della ghiera di messa a fuoco esterna (Focus barrel counter-rotation)
    val barrelRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing)
        ),
        label = "iris_barrel_rotation"
    )

    // Micromovimento dello sguardo orizzontale (Gaze drift dei catchlights)
    val gazeDrift by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_gaze_drift"
    )

    // Spostamento orizzontale fluido dello sguardo / pupilla
    val targetPupilShiftX = when (mood) {
        IrisMood.TYPING -> gazeBiasX.coerceIn(-1f, 1f)
        IrisMood.IDLE -> gazeDrift * 0.15f
        else -> 0f
    }

    val pupilShiftXRatio by animateFloatAsState(
        targetValue = targetPupilShiftX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iris_pupil_shift_x"
    )

    // Spostamento verticale fluido verso l'alto quando l'utente digita
    val pupilShiftYRatio by animateFloatAsState(
        targetValue = if (mood == IrisMood.TYPING) -1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iris_pupil_shift_y"
    )

    // Calcolo frazione di apertura effettiva
    val baseAperture = when (mood) {
        IrisMood.IDLE -> idleBreathing
        IrisMood.TYPING -> typingBreathing
        IrisMood.SEARCHING -> searchingBreathing
        IrisMood.DELIGHTED -> 0.65f
        IrisMood.EMPTY -> 0.22f
        IrisMood.OFFLINE -> 0.06f
    }
    // Effetto scuotimento/stordimento (Dizzy): flutter rapido del diaframma che cerca la calibrazione
    val dizzyProgress = dizzyAnim.value
    val dizzyWobble = if (dizzyProgress > 0f) {
        sin((1f - dizzyProgress) * 36f) * 0.16f * dizzyProgress
    } else 0f

    // L'ammiccamento (wink) forza la chiusura dell'iride verso f/22 (0.05f)
    val effectiveAperture = (baseAperture * (1f - winkAnim.value) + 0.05f * winkAnim.value + dizzyWobble).coerceIn(0.05f, 0.70f)

    val dizzyRotation = if (dizzyProgress > 0f) {
        sin((1f - dizzyProgress) * 20f) * 25f * dizzyProgress
    } else 0f

    // Angolo di otturazione corrente con micro-inclinazione direzionale e oscillazione stordita
    val effectiveAngle = (when (mood) {
        IrisMood.SEARCHING -> searchingRotation
        IrisMood.TYPING -> (pupilShiftXRatio * 6f) + (gazeDrift * 2f)
        IrisMood.IDLE -> gazeDrift * 8f
        IrisMood.EMPTY -> 15f
        IrisMood.DELIGHTED -> 0f
        IrisMood.OFFLINE -> 0f
    }) + dizzyRotation

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                awaitEachGesture {
                    try {
                        awaitFirstDown(requireUnconsumed = false)
                        isTouched = true
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                    } finally {
                        isTouched = false
                    }
                }
            }
            .bounceClick(
                scaleDown = 0.92f,
                onClick = { onClick?.invoke() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = this.size.width
            val height = this.size.height
            val centerX = width / 2f
            val centerY = height / 2f
            val maxR = width.coerceAtMost(height) / 2f

            // 1. BARILOTTO ESTERNO DELLA LENTE (Metal & Glass Barrel)
            val barrelOuterR = maxR * 0.96f
            val barrelInnerR = maxR * 0.82f

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

            // Anello metallico esterno con bordo rifinito
            drawCircle(
                color = accentColor.copy(alpha = 0.35f),
                radius = barrelOuterR,
                center = Offset(centerX, centerY),
                style = Stroke(width = with(density) { 1.2.dp.toPx() })
            )

            // 2. TACCHE DI CALIBRAZIONE FOCALE (Focus & F-Stop Ring)
            val tickAngleOffset = if (mood == IrisMood.SEARCHING) barrelRotation else 0f
            val tickCount = 16
            for (i in 0 until tickCount) {
                val rad = (tickAngleOffset + i * (360f / tickCount)) * (Math.PI.toFloat() / 180f)
                val isMajor = (i % 4 == 0)
                val tickStartR = barrelOuterR - with(density) { (if (isMajor) 4.5.dp else 2.5.dp).toPx() }
                val tickEndR = barrelOuterR - with(density) { 1.0.dp.toPx() }

                val startX = centerX + tickStartR * cos(rad)
                val startY = centerY + tickStartR * sin(rad)
                val endX = centerX + tickEndR * cos(rad)
                val endY = centerY + tickEndR * sin(rad)

                drawLine(
                    color = if (isMajor) accentColor.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.20f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = with(density) { (if (isMajor) 1.2.dp else 0.8.dp).toPx() }
                )
            }

            // Anello interno di raccordo alla camera del diaframma
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = barrelInnerR,
                center = Offset(centerX, centerY),
                style = Stroke(width = with(density) { 0.8.dp.toPx() })
            )

            // 3. DIAFRAMMA CINEMATOGRAFICO (La pupilla e la ghiera si orientano in 2D seguendo la scrittura, il giroscopio e la spirale da scuotimento)
            val pupilMaxShiftY = barrelInnerR * 0.22f
            val pupilMaxShiftX = barrelInnerR * 0.20f
            val tiltShiftX = barrelInnerR * 0.08f * smoothTiltX
            val tiltShiftY = barrelInnerR * 0.08f * smoothTiltY

            // Orbita a spirale decrescente quando stordita
            val dizzySpiralAngle = (1f - dizzyProgress) * 1440f * (Math.PI.toFloat() / 180f)
            val dizzyOrbitR = barrelInnerR * 0.22f * dizzyProgress
            val dizzyShiftX = dizzyOrbitR * cos(dizzySpiralAngle)
            val dizzyShiftY = dizzyOrbitR * sin(dizzySpiralAngle)

            val pupilCenterY = centerY + (pupilMaxShiftY * pupilShiftYRatio) + tiltShiftY + dizzyShiftY
            val pupilCenterX = centerX + (pupilMaxShiftX * pupilShiftXRatio) + tiltShiftX + dizzyShiftX

            val outerR = barrelInnerR * 1.35f
            val innerR = (barrelInnerR * effectiveAperture).coerceAtLeast(1.5f)
            val bladeCount = 6
            val tanLen = sqrt((outerR * outerR - innerR * innerR).coerceAtLeast(0f))

            // La camera interna è ritagliata all'interno del barilotto metallico fisso
            clipPath(
                Path().apply {
                    addOval(Rect(center = Offset(centerX, centerY), radius = barrelInnerR))
                }
            ) {
                // A. Bagliore profondo centrale nella pupilla (ancorato al centro pupillare mobile)
                val glowRadius = (innerR * 1.8f).coerceAtLeast(2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to accentColor.copy(alpha = if (mood == IrisMood.SEARCHING) 0.60f else 0.35f),
                        0.5f to accentColor.copy(alpha = 0.12f),
                        1.0f to Color.Transparent,
                        center = Offset(pupilCenterX, pupilCenterY),
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = Offset(pupilCenterX, pupilCenterY)
                )

                // B1. Superfici geometriche delle 6 lamelle (livello z-index inferiore)
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

                    // Superficie della singola lamella
                    val bladePath = Path().apply {
                        moveTo(tx, ty)
                        lineTo(px, py)
                        lineTo(nextPx, nextPy)
                        lineTo(nextTx, nextTy)
                        close()
                    }

                    drawPath(
                        path = bladePath,
                        color = Color(0xFF13181D).copy(alpha = 0.95f)
                    )
                }

                // B2. Bordi nitidi di scorrimento delle lamelle (livello z-index superiore, disegnati sopra per evitare che l'ultima lamella copra la prima)
                for (i in 0 until bladeCount) {
                    val phi = (effectiveAngle + i * (360f / bladeCount)) * (Math.PI.toFloat() / 180f)
                    val cosP = cos(phi)
                    val sinP = sin(phi)

                    val tx = pupilCenterX + innerR * cosP
                    val ty = pupilCenterY + innerR * sinP
                    val px = tx - tanLen * sinP
                    val py = ty + tanLen * cosP

                    drawLine(
                        color = accentColor.copy(alpha = 0.85f),
                        start = Offset(px, py),
                        end = Offset(tx, ty),
                        strokeWidth = with(density) { 1.2.dp.toPx() }
                    )
                }

                // C. Bordo dell'apertura centrale poligonale (la pupilla esagonale mobile)
                val aperturePath = Path()
                for (i in 0 until bladeCount) {
                    val phi = (effectiveAngle + i * (360f / bladeCount)) * (Math.PI.toFloat() / 180f)
                    val tx = pupilCenterX + innerR * cos(phi)
                    val ty = pupilCenterY + innerR * sin(phi)
                    if (i == 0) aperturePath.moveTo(tx, ty) else aperturePath.lineTo(tx, ty)
                }
                aperturePath.close()

                drawPath(
                    path = aperturePath,
                    color = accentColor.copy(alpha = 0.90f),
                    style = Stroke(width = with(density) { 1.3.dp.toPx() })
                )

                // 4. CATCHLIGHTS OTTICI (I riflessi di luce sfumati sul vetro ottico con parallasse da inclinazione fisica)
                if (effectiveAperture > 0.12f) {
                    val driftPx = with(density) { (gazeDrift * 1.0f).dp.toPx() }
                    val catchlightParallaxX = barrelInnerR * 0.05f * smoothTiltX
                    val catchlightParallaxY = barrelInnerR * 0.05f * smoothTiltY

                    // Riflesso primario superiore (Softbox Glint) con gradiente morbido, trasparenza da vetro ottico e parallasse
                    val glint1X = pupilCenterX + (innerR * 0.22f) + driftPx + catchlightParallaxX
                    val glint1Y = pupilCenterY - (innerR * 0.24f) + catchlightParallaxY
                    val glint1R = (innerR * 0.16f).coerceAtLeast(1f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to Color.White.copy(alpha = 0.60f),
                            0.65f to Color.White.copy(alpha = 0.35f),
                            1.0f to Color.White.copy(alpha = 0.06f),
                            center = Offset(glint1X, glint1Y),
                            radius = glint1R
                        ),
                        radius = glint1R,
                        center = Offset(glint1X, glint1Y)
                    )

                    // Riflesso secondario inferiore (Fill Glint) traslucido, soffuso e contro-parallasse
                    val glint2X = pupilCenterX - (innerR * 0.18f) + driftPx * 0.5f - catchlightParallaxX * 0.5f
                    val glint2Y = pupilCenterY + (innerR * 0.20f) - catchlightParallaxY * 0.5f
                    val glint2R = (innerR * 0.09f).coerceAtLeast(0.8f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to accentColor.copy(alpha = 0.42f),
                            0.75f to accentColor.copy(alpha = 0.18f),
                            1.0f to Color.Transparent,
                            center = Offset(glint2X, glint2Y),
                            radius = glint2R
                        ),
                        radius = glint2R,
                        center = Offset(glint2X, glint2Y)
                    )
                }
            }
        }
    }
}

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
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val density = LocalDensity.current
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

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = this.size.width
            val height = this.size.height
            val centerX = width / 2f
            val centerY = height / 2f
            val baseRadius = (width.coerceAtMost(height) / 2f) * 0.90f

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
                    style = Stroke(width = with(density) { (if (isSearching) 1.8.dp else 1.2.dp).toPx() })
                )

                // Micro-particelle stardust in orbita sull'anello
                if (isSearching) {
                    val dotRad1 = 0f
                    val dotRad2 = 180f * (Math.PI.toFloat() / 180f)
                    drawCircle(
                        color = Color.White,
                        radius = with(density) { 2.2.dp.toPx() },
                        center = Offset(centerX + baseRadius * cos(dotRad1), centerY + baseRadius * sin(dotRad1))
                    )
                    drawCircle(
                        color = accentColor.copy(alpha = 0.75f),
                        radius = with(density) { 1.6.dp.toPx() },
                        center = Offset(centerX + baseRadius * cos(dotRad2), centerY + baseRadius * sin(dotRad2))
                    )
                }
            }

            // 4. Anello interno fisso sottile
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = baseRadius * 0.78f,
                center = Offset(centerX, centerY),
                style = Stroke(width = with(density) { 0.8.dp.toPx() })
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

/**
 * Indicatore essenziale mostrato durante la ricerca Mood in corso:
 * invece di una card pesante ("malloppone"), IRIS (o lo Sparkle AI) si posiziona
 * semplicemente in alto al centro mentre ruota / esegue il focus hunting.
 */
@Composable
fun IrisSearchingHero(
    query: String,
    modifier: Modifier = Modifier,
    isMoodAiMode: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isMoodAiMode) {
            // Sfera generativa animata per Cinema Mood AI
            AiSparkleOrb(
                size = 72.dp,
                isSearching = true,
                accentColor = MaterialTheme.colorScheme.primary
            )
        } else {
            // IRIS pura in alto al centro mentre ruota le lamelle ed esegue il focus hunting
            IrisMascot(
                mood = IrisMood.SEARCHING,
                size = 72.dp,
                accentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Stato vuoto cinematografico quando la ricerca Mood non produce risultati.
 */
@Composable
fun IrisEmptyState(
    query: String,
    modifier: Modifier = Modifier,
    isMoodAiMode: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isMoodAiMode) {
            AiSparkleOrb(
                size = 64.dp,
                isSearching = false,
                accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.60f)
            )
        } else {
            IrisMascot(
                mood = IrisMood.EMPTY,
                size = 80.dp,
                accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.search_mood_iris_no_results),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.search_mood_iris_no_results_desc),
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.50f),
                lineHeight = 16.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 290.dp)
        )
    }
}
