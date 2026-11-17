package com.cinetrack.ui.components.mascot

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
    onClick: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // 1. Chiusura protettiva al tocco (Blink & Touch defense)
    // IRIS chiude il diaframma istantaneamente appena viene toccata (press down)
    // e rimane protetta e chiusa per tutto il tempo in cui il dito tocca la lente,
    // per poi riaprirsi con un rimbalzo ottico elastico non appena il dito viene sollevato.
    var isTouched by remember { mutableStateOf(false) }
    var hasBeenTouched by remember { mutableStateOf(false) }
    val winkAnim = remember { Animatable(0f) }

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

    // Battito spontaneo periodico quando non è toccata dall'utente
    LaunchedEffect(mood) {
        if (mood == IrisMood.IDLE || mood == IrisMood.TYPING) {
            while (true) {
                delay(if (mood == IrisMood.TYPING) 6500L else 4500L + (0..2000).random())
                if (!isTouched) {
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

    // Spostamento fisico verso l'alto dell'intera pupilla/diaframma interno quando l'utente digita
    val pupilShiftRatio by animateFloatAsState(
        targetValue = if (mood == IrisMood.TYPING) -1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iris_pupil_shift"
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
    // L'ammiccamento (wink) forza la chiusura dell'iride verso f/22 (0.05f)
    val effectiveAperture = (baseAperture * (1f - winkAnim.value) + 0.05f * winkAnim.value).coerceIn(0.05f, 0.70f)

    // Angolo di otturazione corrente
    val effectiveAngle = when (mood) {
        IrisMood.SEARCHING -> searchingRotation
        IrisMood.TYPING -> gazeDrift * 3f
        IrisMood.IDLE -> gazeDrift * 8f
        IrisMood.EMPTY -> 15f
        IrisMood.DELIGHTED -> 0f
        IrisMood.OFFLINE -> 0f
    }

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

            // 3. DIAFRAMMA CINEMATOGRAFICO (La pupilla e la ghiera si spostano fisicamente in alto)
            val pupilMaxShiftY = barrelInnerR * 0.22f
            val pupilCenterY = centerY + pupilMaxShiftY * pupilShiftRatio
            val pupilCenterX = centerX

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

                // B. 6 Lamelle geometriche del diaframma (la ghiera/iride mobile)
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

                    // Bordo nitido di scorrimento della lamella
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

                // 4. CATCHLIGHTS OTTICI (I riflessi di luce sfumati e traslucidi sul vetro ottico)
                if (effectiveAperture > 0.12f) {
                    val driftPx = with(density) { (gazeDrift * 1.0f).dp.toPx() }

                    // Riflesso primario superiore (Softbox Glint) con gradiente morbido e trasparenza da vetro ottico
                    val glint1X = pupilCenterX + (innerR * 0.22f) + driftPx
                    val glint1Y = pupilCenterY - (innerR * 0.24f)
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

                    // Riflesso secondario inferiore (Fill Glint) traslucido e soffuso
                    val glint2X = pupilCenterX - (innerR * 0.18f) + driftPx * 0.5f
                    val glint2Y = pupilCenterY + (innerR * 0.20f)
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
 * Hero Card elegante mostrata durante la ricerca Mood in corso.
 * Presenta IRIS al lavoro (fase di messa a fuoco ottica del catalogo)
 * con messaggi cinematici rotanti e la query dell'utente.
 */
@Composable
fun IrisSearchingHero(
    query: String,
    modifier: Modifier = Modifier,
    isMoodAiMode: Boolean = true
) {
    val messages = remember {
        listOf(
            R.string.search_mood_iris_analyzing,
            R.string.search_mood_iris_status_focus,
            R.string.search_mood_iris_status_scanning
        )
    }

    var messageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2400)
            messageIndex = (messageIndex + 1) % messages.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // IRIS in stato di ricerca attiva (focus hunting)
            IrisMascot(
                mood = IrisMood.SEARCHING,
                size = 64.dp,
                accentColor = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                // Micro-badge cinematografico IRIS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.search_mood_iris_badge),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.6.sp
                            )
                        )
                    }

                    if (query.isNotBlank()) {
                        Text(
                            text = "“$query”",
                            color = Color.White.copy(alpha = 0.50f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Messaggio ottico rotante
                AnimatedContent(
                    targetState = messageIndex,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180)))
                    },
                    label = "iris_searching_msg"
                ) { idx ->
                    Text(
                        text = stringResource(messages[idx]),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Stato vuoto cinematografico quando la ricerca Mood non produce risultati.
 */
@Composable
fun IrisEmptyState(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IrisMascot(
            mood = IrisMood.EMPTY,
            size = 80.dp,
            accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
        )

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
