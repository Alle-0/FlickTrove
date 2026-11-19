package com.cinetrack.ui.components.mascot

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan2
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
 * L'espressivitÃ  scaturisce dalla fisica ottica:
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
    gazeBiasY: Float = 0f,
    enableTiltParallax: Boolean = true,
    enableBacklight: Boolean = true,
    enableTouchInteractions: Boolean = true,
    isSaccade: Boolean = false,
    scrollInertia: Float = 0f,
    isFocusHunting: Boolean = false,
    glowPadding: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Chiusura protettiva e ammiccamento (Wink & Touch Defense)
    var isTouched by remember { mutableStateOf(false) }
    var hasBeenTouched by remember { mutableStateOf(false) }
    val winkAnim = remember { Animatable(0f) }

    // Animazione dello Starnuto Ottico (Lens Dust Sneeze - Feature 4)
    var isSneezing by remember { mutableStateOf(false) }
    val sneezeAnim = remember { Animatable(0f) }

    // Oscillazione rapida della ghiera per il Focus Hunting incuriosito (Feature 6)
    val focusHuntingAngle = remember { Animatable(0f) }
    LaunchedEffect(isFocusHunting) {
        if (isFocusHunting) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            focusHuntingAngle.animateTo(28f, tween(110, easing = FastOutSlowInEasing))
            focusHuntingAngle.animateTo(-22f, tween(130, easing = FastOutSlowInEasing))
            focusHuntingAngle.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        } else {
            // Rilascio morbido e continuo della ghiera: zero scatti bruschi (no snapTo)
            focusHuntingAngle.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // Micro-parallasse giroscopica e rilevamento scuotimento dello smartphone (Shake to Dizzy)
    var isDizzy by remember { mutableStateOf(false) }
    val tiltMotion = rememberIrisTiltMotion(
        enableTiltParallax = enableTiltParallax,
        onShakeDetected = { if (!isDizzy) isDizzy = true }
    )
    val dizzyAnim = remember { Animatable(0f) }

    // Sguardo saccadico naturale in IDLE (Saccadic Exploration & Fixation come un occhio vero)
    var idleGazeX by remember { mutableFloatStateOf(0f) }
    var idleGazeY by remember { mutableFloatStateOf(0f) }
    var idleIsSaccade by remember { mutableStateOf(false) }

    // Reazione di agitazione e stizza al tocco ripetuto (Annoyed Reaction: 2 o 3 click seguiti da pausa di 500ms)
    var tapEventCount by remember { mutableIntStateOf(0) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var isAnnoyed by remember { mutableStateOf(false) }
    val annoyedAnim = remember { Animatable(0f) }

    LaunchedEffect(lastTapTimestamp) {
        if (lastTapTimestamp > 0L) {
            if (tapEventCount >= 2) {
                // L'utente ha toccato ripetutamente (2 o 3+ volte).
                // Se l'utente non tocca piÃ¹ per 500ms, IRIS si agita infastidita!
                delay(500L)
                tapEventCount = 0
                if (!isAnnoyed && !isDizzy) {
                    try {
                        isAnnoyed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Arrossamento e contrazione infastidita che salgono fluidamente in 260ms (niente scatto secco a 0ms)
                        annoyedAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                        )
                        // Raffreddamento graduale e riapertura progressiva a respiro in 1400ms
                        annoyedAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
                        )
                    } finally {
                        withContext(NonCancellable) {
                            annoyedAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                            )
                            isAnnoyed = false
                        }
                    }
                }
            } else {
                delay(600L)
                tapEventCount = 0
            }
        }
    }

    LaunchedEffect(isDizzy) {
        if (isDizzy) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                dizzyAnim.snapTo(1f)
                dizzyAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing)
                )
                // Chiusura rapida di ricalibrazione e riapertura elastica (shutter calibration blink)
                winkAnim.animateTo(1f, tween(85, easing = FastOutLinearInEasing))
                winkAnim.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            } finally {
                withContext(NonCancellable) {
                    dizzyAnim.snapTo(0f)
                    isDizzy = false
                }
            }
        }
    }

    // 1. Chiusura protettiva al tocco (Blink & Touch Defense)
    // IRIS chiude il diaframma istantaneamente appena viene toccata (press down).
    // Al rilascio si riapre a piena apertura con un rimbalzo morbido ed accogliente.
    LaunchedEffect(isTouched) {
        if (isTouched) {
            hasBeenTouched = true
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            winkAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 75, easing = FastOutLinearInEasing)
            )
        } else if (hasBeenTouched) {
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

    // 2. Sguardo organico e naturale come un occhio vero (Fluid Gaze, Calm Fixation & Rare Subtle Blinks)
    // In IDLE perlustra lo spazio circostante in modo fluido e morbido, senza scatti bruschi.
    // Si sofferma con curiositÃ  su ciascun punto per 3 - 5 secondi e batte le palpebre solo occasionalmente.
    LaunchedEffect(mood) {
        if (mood == IrisMood.IDLE) {
            var stepIndex = 0
            var stepsSinceSnap = 0
            while (true) {
                // Easter egg raro: Starnuto Ottico da granello di polvere sulla lente (Feature 4, ~1.5% in IDLE)
                val rollSneeze = (1..100).random()
                if (!isSneezing && !isTouched && !isDizzy && !isAnnoyed && rollSneeze == 42) {
                    isSneezing = true
                    // 1. Solletico sulla lente: 3 contrazioni crescenti rapide
                    repeat(3) { i ->
                        winkAnim.animateTo(0.35f + (i * 0.22f), tween(85, easing = FastOutSlowInEasing))
                        winkAnim.animateTo(0.08f, tween(70, easing = LinearEasing))
                    }
                    // 2. ETCIÃ™! Chiusura fulminea con vibrazione aptica e scatto in avanti della testa
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    sneezeAnim.snapTo(1f)
                    winkAnim.animateTo(1f, tween(35))
                    // 3. Riapertura elastica e riallineamento timido
                    sneezeAnim.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                    winkAnim.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
                    isSneezing = false
                    delay(1200L)
                }

                // Ogni 4-6 passi organici c'Ã¨ l'occasione di un "salto scattoso" spontaneo (saccade)
                val shouldSnap = stepsSinceSnap >= 4 && (stepsSinceSnap >= 6 || (1..100).random() <= 45)

                if (shouldSnap) {
                    stepsSinceSnap = 0
                    idleIsSaccade = true
                    // Salto scattoso verso l'estremo opposto o un raggio ampio (50% - 75%)
                    val currentAngle = atan2(idleGazeY.toDouble(), idleGazeX.toDouble())
                    val snapAngle = currentAngle + Math.PI + (((-35..35).random()) * (Math.PI / 180.0))
                    val magnitude = (48..72).random() / 100f
                    idleGazeX = (cos(snapAngle) * magnitude).toFloat().coerceIn(-0.85f, 0.85f)
                    idleGazeY = (sin(snapAngle) * magnitude).toFloat().coerceIn(-0.85f, 0.85f)

                    // Sosta attenta e curiosa sul nuovo punto agganciato (1.8s - 2.5s)
                    delay((1800L..2500L).random())
                    idleIsSaccade = false
                } else {
                    stepsSinceSnap++
                    idleIsSaccade = false
                    // Selezione del punto di fissazione:
                    // Sguardi dolci e organici a raggio moderato (20% - 50% del raggio visivo)
                    // alternati a soste rilassate verso il centro
                    val (nextX, nextY) = if (stepIndex % 3 == 0) {
                        // Sosta centrale rilassata
                        Pair(((-8..8).random() / 100f), ((-8..8).random() / 100f))
                    } else {
                        val angle = (0..359).random() * (Math.PI / 180.0)
                        val magnitude = (20..50).random() / 100f
                        Pair((cos(angle) * magnitude).toFloat(), (sin(angle) * magnitude).toFloat())
                    }
                    stepIndex++

                    // Sposta lo sguardo verso il nuovo punto
                    idleGazeX = nextX
                    idleGazeY = nextY

                    // Tempo di fissazione calmo e naturale: circa 2.8 a 4.2 secondi
                    val fixationDuration = (2800L..4200L).random()

                    // Ammiccamenti naturali e vivi (~60% delle volte, un battito ogni ~5-7 secondi)
                    val rollBlink = (1..100).random()
                    if (rollBlink <= 60) {
                        val preWait = (1000L..1800L).random().coerceAtMost(fixationDuration - 700L)
                        delay(preWait)

                        if (!isTouched && !isDizzy && !isAnnoyed) {
                            when {
                                // 2% Rarissimo triplo ammiccamento vivace
                                rollBlink <= 2 -> {
                                    repeat(3) { i ->
                                        winkAnim.animateTo(1f, tween(75, easing = FastOutSlowInEasing))
                                        winkAnim.animateTo(
                                            targetValue = if (i < 2) 0.15f else 0f,
                                            animationSpec = if (i < 2) tween(65, easing = LinearEasing)
                                            else spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                                        )
                                    }
                                }
                                // 6% Doppio ammiccamento morbido
                                rollBlink <= 8 -> {
                                    repeat(2) { i ->
                                        winkAnim.animateTo(1f, tween(85, easing = FastOutSlowInEasing))
                                        winkAnim.animateTo(
                                            targetValue = if (i < 1) 0.15f else 0f,
                                            animationSpec = if (i < 1) tween(75, easing = LinearEasing)
                                            else spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                                        )
                                    }
                                }
                                // Singolo battito d'otturatore calmo e rilassato
                                else -> {
                                    winkAnim.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
                                    winkAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        }

                        val postWait = (fixationDuration - preWait - 350L).coerceAtLeast(600L)
                        delay(postWait)
                    } else {
                        // Mantiene la fissazione calma senza battere le ciglia
                        delay(fixationDuration)
                    }
                }
            }
        } else if (mood == IrisMood.TYPING) {
            while (true) {
                delay((3800L..5800L).random())
                if (!isTouched && !isDizzy && !isAnnoyed) {
                    winkAnim.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
                    winkAnim.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                }
            }
        } else if (mood == IrisMood.DELIGHTED) {
            // Ammiccamenti piacevoli mentre contempla i risultati (ogni 5-8.5 secondi con occasionali doppi)
            while (true) {
                delay((5000L..8500L).random())
                if (!isTouched && !isDizzy && !isAnnoyed) {
                    val roll = (1..100).random()
                    if (roll <= 25) {
                        // Raro doppio battito leggero
                        repeat(2) { i ->
                            winkAnim.animateTo(1f, tween(80, easing = FastOutSlowInEasing))
                            winkAnim.animateTo(
                                targetValue = if (i < 1) 0.15f else 0f,
                                animationSpec = if (i < 1) tween(70, easing = LinearEasing)
                                else spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                            )
                        }
                    } else {
                        // Singolo battito morbido
                        winkAnim.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
                        winkAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                        )
                    }
                }
            }
        } else if (mood == IrisMood.EMPTY) {
            // Malinconia riflessiva (Nessun risultato trovato / EMPTY):
            // Sguardo basso, micro-sospiro e battiti lenti e pesanti ogni 5-7.5 secondi (come WALL-E sconsolato)
            while (true) {
                delay((5000L..7500L).random())
                if (!isTouched && !isDizzy && !isAnnoyed) {
                    winkAnim.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
                    winkAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
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

    // Respirazione diaframma a riposo: apertura accogliente ed empatica (f/1.8 - f/1.4, pupilla dilatata vivace)
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0.52f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_idle_breathing"
    )

    // Dilatazione attenta ed empatica mentre l'utente digita (TYPING f/1.4)
    val typingBreathing by infiniteTransition.animateFloat(
        initialValue = 0.58f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_typing_breathing"
    )

    // Focus hunting rapido durante la ricerca
    val searchingBreathing by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_searching_breathing"
    )

    // Respirazione malinconica diaframma in EMPTY (f/2.4 a f/2.0: apertura accogliente, espressiva e viva, non socchiusa)
    val emptyBreathing by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.44f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_empty_breathing"
    )

    // Lieve oscillazione sconsolata dello sguardo in EMPTY (perlustra timidamente a terra verso destra e sinistra)
    val emptyGazeDriftX by infiniteTransition.animateFloat(
        initialValue = -0.18f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(4400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_empty_gaze_drift_x"
    )

    // Micro-inclinazione sconsolata della testa in EMPTY (dondolio triste alla WALL-E)
    val emptyHeadTilt by infiniteTransition.animateFloat(
        initialValue = -6.5f,
        targetValue = -2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_empty_head_tilt"
    )

    // Micro-inclinazione curiosa della testa (Curious Head-Tilt alla WALL-E / Pixar)
    val headTiltAnim by infiniteTransition.animateFloat(
        initialValue = -5.0f,
        targetValue = 5.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_head_tilt"
    )

    // Respiro elastico organico (Squash & Stretch che toglie rigiditÃ  al metallo)
    val breathSquash by infiniteTransition.animateFloat(
        initialValue = 0.982f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_breath_squash"
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
    val targetPupilShiftX = when {
        mood == IrisMood.IDLE && gazeBiasX == 0f -> idleGazeX.coerceIn(-1f, 1f)
        mood == IrisMood.TYPING -> if (gazeBiasX != 0f) gazeBiasX.coerceIn(-1f, 1f) else 0f
        mood == IrisMood.EMPTY -> if (gazeBiasX != 0f) gazeBiasX.coerceIn(-1f, 1f) else emptyGazeDriftX
        else -> gazeBiasX.coerceIn(-1f, 1f)
    }

    // Inerzia da scroll veloce (Vento di pellicola - Feature 3):
    // La pupilla e la testa vengono trascinate con dolcezza dal flusso di scorrimento,
    // e al rilascio compiono un elastico rimbalzo morbido!
    val smoothScrollInertia by animateFloatAsState(
        targetValue = scrollInertia,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iris_smooth_scroll_inertia"
    )

    // Spostamento verticale fluido dello sguardo / pupilla (guarda in alto durante la digitazione, traccia i risultati, esplora in idle o abbassa sconsolata lo sguardo in empty)
    val targetPupilShiftY = when {
        isFocusHunting -> (gazeBiasY + (smoothScrollInertia * 0.15f)).coerceIn(0.40f, 1f)
        mood == IrisMood.IDLE && gazeBiasY == 0f -> (idleGazeY + (smoothScrollInertia * 0.15f)).coerceIn(-1f, 1f)
        mood == IrisMood.TYPING -> if (gazeBiasY != 0f) gazeBiasY.coerceIn(-1f, 1f) else -0.90f
        mood == IrisMood.EMPTY -> if (gazeBiasY != 0f) gazeBiasY.coerceIn(-1f, 1f) else (0.52f + ((emptyBreathing - 0.24f) * 0.4f))
        mood == IrisMood.DELIGHTED -> {
            // Sui risultati lo sguardo perlustra in basso; durante un fling veloce compie solo una micro-reazione discreta
            (0.80f + (smoothScrollInertia * 0.15f)).coerceIn(0.50f, 1f)
        }
        else -> (gazeBiasY + (smoothScrollInertia * 0.15f)).coerceIn(-1f, 1f)
    }

    val effectiveIsSaccade = isSaccade || idleIsSaccade || isFocusHunting

    val pupilShiftXRatio by animateFloatAsState(
        targetValue = targetPupilShiftX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = if (isFocusHunting) Spring.StiffnessMedium else if (effectiveIsSaccade) Spring.StiffnessMediumLow else Spring.StiffnessLow
        ),
        label = "iris_pupil_shift_x"
    )

    val pupilShiftYRatio by animateFloatAsState(
        targetValue = targetPupilShiftY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = if (isFocusHunting) Spring.StiffnessMedium else if (effectiveIsSaccade) Spring.StiffnessMediumLow else Spring.StiffnessLow
        ),
        label = "iris_pupil_shift_y"
    )

    // Inclinazione curiosa del telaio dell'ottica (Head-Tilt orientato ed empatico alla Pixar)
    val targetHeadTilt = (when {
        isFocusHunting -> (gazeBiasX * 10f).coerceIn(-12f, 12f)
        mood == IrisMood.TYPING -> (gazeBiasX * 6f) + 3f
        mood == IrisMood.IDLE && gazeBiasX == 0f -> (idleGazeX * 5.5f) + (idleGazeY * 1.5f) + (headTiltAnim * 0.35f)
        mood == IrisMood.DELIGHTED -> (gazeBiasX * 6f).coerceIn(-7f, 7f) + 2f
        mood == IrisMood.SEARCHING -> 0f
        mood == IrisMood.EMPTY -> emptyHeadTilt
        mood == IrisMood.OFFLINE -> 0f
        else -> (gazeBiasX * 6f).coerceIn(-7f, 7f)
    } + (smoothScrollInertia * 2.2f)).coerceIn(-16f, 16f)

    val smoothHeadTilt by animateFloatAsState(
        targetValue = targetHeadTilt,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = if (isFocusHunting) Spring.StiffnessMediumLow else Spring.StiffnessVeryLow
        ),
        label = "iris_smooth_head_tilt"
    )

    // Calcolo frazione di apertura effettiva
    val baseAperture = when {
        isFocusHunting -> 0.30f // f/5.6 - Chiusura di precisione per messa a fuoco dei dettagli (Feature 6)
        mood == IrisMood.IDLE -> idleBreathing
        mood == IrisMood.TYPING -> typingBreathing
        mood == IrisMood.SEARCHING -> searchingBreathing
        mood == IrisMood.DELIGHTED -> 0.74f
        mood == IrisMood.EMPTY -> emptyBreathing // Respira e sospira a vuoto
        else -> 0.06f
    }

    // Transizione graduale e morbida dell'apertura del diaframma (nessuno scatto secco tra f/5.6 e f/1.4 al rilascio del popup)
    val animatedBaseAperture by animateFloatAsState(
        targetValue = baseAperture,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "iris_animated_base_aperture"
    )

    // Effetto scuotimento/stordimento (Dizzy): flutter rapido del diaframma che cerca la calibrazione
    val dizzyProgress = dizzyAnim.value
    val dizzyWobble = if (dizzyProgress > 0f) {
        sin((1f - dizzyProgress) * 36f) * 0.16f * dizzyProgress
    } else 0f

    val annoyedProgress = annoyedAnim.value

    // Effetto agitazione / stizza al tocco ripetuto:
    // Scuote energicamente la testa a destra e sinistra in protesta rapida ("No! Smettila di toccarmi l'occhio!")
    val annoyedShake = if (annoyedProgress > 0f) {
        sin((1f - annoyedProgress) * 44f) * 14f * annoyedProgress
    } else 0f

    // Restringimento infastidito dell'iride a f/22 con micro-tremolio di stizza (squint indignato, l'occhio rimane vivo)
    val annoyedSquint = if (annoyedProgress > 0f) {
        (-0.36f * annoyedProgress) + (sin((1f - annoyedProgress) * 56f) * 0.03f * annoyedProgress)
    } else 0f

    val effectiveAperture = ((animatedBaseAperture + annoyedSquint) * (1f - winkAnim.value) + 0.05f * winkAnim.value + dizzyWobble).coerceIn(0.12f, 0.78f)

    // Rapporto di apertura normalizzato per modulare l'intensitÃ  del controluce ottico
    val apertureRatio = ((effectiveAperture - 0.05f) / 0.73f).coerceIn(0f, 1f)

    // Shift cromatico progressivo e morbido verso ambra/rosso vivo (sale in 260ms e sfuma in 1400ms, zero scatti a 0ms)
    val activeAccentColor = if (annoyedProgress > 0f) {
        val irritatedRed = Color(0xFFFF453A)
        lerp(accentColor, irritatedRed, annoyedProgress)
    } else {
        accentColor
    }

    val moodGlowAlphaMultiplier = when (mood) {
        IrisMood.IDLE -> if (annoyedProgress > 0f) 0.85f else 0.48f
        IrisMood.TYPING -> 0.58f
        IrisMood.SEARCHING -> 0.85f
        IrisMood.DELIGHTED -> 0.92f
        IrisMood.EMPTY -> 0.30f
        IrisMood.OFFLINE -> 0.0f
    }
    val sneezeFlash = if (sneezeAnim.value > 0f) sneezeAnim.value * 0.35f else 0f
    val effectiveGlowAlpha = ((moodGlowAlphaMultiplier * (0.60f + 0.40f * apertureRatio) * (1f - winkAnim.value * 0.5f)) + sneezeFlash).coerceIn(0f, 1f)

    val dizzyRotation = if (dizzyProgress > 0f) {
        sin((1f - dizzyProgress) * 20f) * 25f * dizzyProgress
    } else 0f

    val sneezeNod = if (sneezeAnim.value > 0f) sneezeAnim.value * 10f else 0f
    val totalChassisRotation = smoothHeadTilt + (if (dizzyProgress > 0f) dizzyRotation * 0.5f else 0f) + annoyedShake + sneezeNod

    // Angolo di otturazione corrente con micro-inclinazione direzionale, oscillazione stordita e focus hunting
    val effectiveAngle = (when (mood) {
        IrisMood.SEARCHING -> searchingRotation
        IrisMood.TYPING -> (pupilShiftXRatio * 6f) + (gazeDrift * 2f)
        IrisMood.IDLE -> (idleGazeX * 8f) + (idleGazeY * 4f)
        IrisMood.EMPTY -> 12f + (emptyGazeDriftX * 6f)
        IrisMood.DELIGHTED -> 0f
        IrisMood.OFFLINE -> 0f
    }) + dizzyRotation + focusHuntingAngle.value

    val containerSize = size + (glowPadding * 2)

    Box(
        modifier = modifier
            .size(containerSize)
            .graphicsLayer {
                // Inerzia fisica dello scroll (Vento di pellicola - Feature 3):
                // Micro-spostamento sobrio ed elegante solo durante lanci veloci (max 5dp)
                translationY = smoothScrollInertia * 5.dp.toPx()
                rotationZ = totalChassisRotation
                scaleX = ((2f - breathSquash) - (kotlin.math.abs(smoothScrollInertia) * 0.02f)).coerceAtLeast(0.85f)
                scaleY = (breathSquash + (kotlin.math.abs(smoothScrollInertia) * 0.025f)).coerceAtLeast(0.85f)
            },
        contentAlignment = Alignment.Center
    ) {
        IrisLensCanvas(
            size = size,
            glowPadding = glowPadding,
            enableBacklight = enableBacklight,
            effectiveGlowAlpha = effectiveGlowAlpha,
            apertureRatio = apertureRatio,
            activeAccentColor = activeAccentColor,
            mood = mood,
            barrelRotation = barrelRotation,
            effectiveAngle = effectiveAngle,
            pupilShiftXRatio = pupilShiftXRatio,
            pupilShiftYRatio = pupilShiftYRatio,
            smoothTiltX = tiltMotion.smoothTiltX,
            smoothTiltY = tiltMotion.smoothTiltY,
            dizzyProgress = dizzyProgress,
            annoyedProgress = annoyedProgress,
            effectiveAperture = effectiveAperture,
            gazeDrift = gazeDrift
        )

        // Area interattiva centrata sul barilotto della lente per rilevare i tocchi in modo preciso
        if (enableTouchInteractions) {
            Box(
                modifier = Modifier
                    .size(size)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            try {
                                awaitFirstDown(requireUnconsumed = false)
                                isTouched = true
                                val downTime = System.currentTimeMillis()
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })
                                val pressDuration = System.currentTimeMillis() - downTime
                                if (pressDuration < 450L) {
                                    if (!isAnnoyed && !isDizzy) {
                                        tapEventCount++
                                        lastTapTimestamp = System.currentTimeMillis()
                                    } else {
                                        // Se la tocchi mentre Ã¨ giÃ  stizzita o stordita, dÃ  un feedback di protesta senza interrompere la ripresa
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            } finally {
                                isTouched = false
                            }
                        }
                    }
                    .bounceClick(
                        scaleDown = 0.92f,
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick?.let { action ->
                            {
                                if (!isDizzy) {
                                    coroutineScope.launch {
                                        focusHuntingAngle.animateTo(28f, tween(110, easing = FastOutSlowInEasing))
                                        focusHuntingAngle.animateTo(-22f, tween(130, easing = FastOutSlowInEasing))
                                        focusHuntingAngle.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                                    }
                                    action()
                                }
                            }
                        }
                    )
            )
        }
    }
}
