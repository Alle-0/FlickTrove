package com.cinetrack.ui.components.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Stato condiviso per l'indicatore di pull-to-refresh globale del Simbionte.
 * Permette di disegnare il fluido in overlay su TUTTI gli elementi (top bar, category bar, filtri).
 */
@Stable
class SymbiontPullState {
    var progress by mutableFloatStateOf(0f)
    var isRefreshing by mutableStateOf(false)
}

val LocalSymbiontPullState = staticCompositionLocalOf { SymbiontPullState() }

/**
 * Indicatore Globale "Symbiont" per FlickTrove.
 *
 * Fisica organica del fluido:
 * 1. Parte da un punto compatto al centro esatto del bordo superiore dello schermo (soffitto, Y = 0).
 * 2. Tirando giù, la massa fluida scura scende rimanendo proporzionata e organica.
 * 3. Al raggiungimento della soglia prima dello stacco, la goccia inferiore si raccorda con tangenza C1 perfetta.
 * 4. Al raggiungimento della soglia (o avvio refresh): SNAP! Il filamento si spezza, la parte superiore
 *    si ritrae verso il soffitto e la sfera si stacca fluttuando a mezz'aria.
 * 5. Durante il caricamento, un diaframma cinepresa pulito e nitido ruota e respira con l'accent color.
 */
@Composable
fun SymbiontGlobalIndicator(
    state: SymbiontPullState,
    modifier: Modifier = Modifier
) {
    SymbiontFluidCanvas(
        progress = state.progress,
        isRefreshing = state.isRefreshing,
        modifier = modifier
    )
}

/**
 * Compatibilità retroattiva per BoxScope (se usato localmente).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.SymbiontPullToRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    SymbiontFluidCanvas(
        progress = state.distanceFraction,
        isRefreshing = isRefreshing,
        modifier = modifier.align(Alignment.TopCenter)
    )
}

@Composable
private fun SymbiontFluidCanvas(
    progress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val accentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val currentProgress = remember(progress) { progress.coerceAtLeast(0f) }

    // Haptic feedback snap vigoroso quando si stacca
    var hasHapticTriggered by remember { mutableStateOf(false) }

    // Animazione di ritrazione/riassorbimento al contrario quando finisce il refresh
    val isDetached = isRefreshing || currentProgress >= 1.0f
    var wasRefreshing by remember { mutableStateOf(false) }
    var isRetracting by remember { mutableStateOf(false) }
    val retractAnim = remember { Animatable(1f) }

    LaunchedEffect(isRefreshing) {
        if (wasRefreshing && !isRefreshing) {
            isRetracting = true
            retractAnim.snapTo(1f)
            retractAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
            isRetracting = false
        }
        wasRefreshing = isRefreshing
    }

    LaunchedEffect(isDetached) {
        if (isDetached && !hasHapticTriggered) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            hasHapticTriggered = true
        } else if (!isDetached && currentProgress < 0.1f) {
            hasHapticTriggered = false
        }
    }

    // Visibilità complessiva con dissolvenza fluida all'uscita
    val isVisible = isRefreshing || currentProgress > 0.01f || isRetracting
    val animAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "SymbiontAlpha"
    )
    val animScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.6f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "SymbiontScale"
    )

    if (animAlpha <= 0.005f && !isVisible) return

    // Spring elastico per il movimento verso il basso (resistenza organica viscosa)
    val springPull by animateFloatAsState(
        targetValue = if (isRetracting) retractAnim.value else currentProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SymbiontSpringPull"
    )

    // Pulsazione respiratoria organica della sfera durante il refresh
    val infiniteTransition = rememberInfiniteTransition(label = "SymbiontPulseInfinite")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SymbiontPulseScale"
    )

    // Rotazione continua del diaframma cinepresa dell'obiettivo
    val shutterAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CameraShutterAngle"
    )

    // Dilatazione / respiro del diaframma dell'obiettivo cinepresa (apertura e chiusura lamelle)
    val apertureFraction by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.54f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CameraApertureFraction"
    )

    // Progresso di distacco (transizione morbida ma elastica allo snap)
    val detachedProgress by animateFloatAsState(
        targetValue = if (isDetached) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "SymbiontDetached"
    )

    // Ritrazione rapida del filamento superiore verso il soffitto dopo lo snap
    val filamentRecoil by animateFloatAsState(
        targetValue = if (isDetached) 0f else 1f,
        animationSpec = tween(durationMillis = 130, easing = FastOutLinearInEasing),
        label = "SymbiontFilamentRecoil"
    )

    val floatingRestYPx = with(density) { 92.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .graphicsLayer {
                alpha = animAlpha
                scaleX = animScale
                scaleY = animScale
            }
    ) {
        val centerX = size.width / 2f

        if (!isDetached || isRetracting) {
            val pull = springPull
            val stretch = pull.coerceIn(0f, 1.15f)

            val rBottom = if (isRetracting) {
                with(density) { (22.dp * pull).toPx() }
            } else {
                with(density) { (2.dp + 20.dp * stretch.coerceIn(0f, 1f)).toPx() }
            }
            val rTop = with(density) { (1.8.dp + 2.dp * (1f - stretch * 0.50f)).toPx() }
            val dropY = if (isRetracting) {
                floatingRestYPx * pull
            } else {
                with(density) { (105.dp * stretch).toPx() }
            }

            val t = stretch.coerceIn(0f, 1f)

            val alphaTop = (70f - 40f * t) * (Math.PI.toFloat() / 180f)
            val xTop = rTop * kotlin.math.sin(alphaTop)
            val yTop = rTop * kotlin.math.cos(alphaTop)

            val alphaBottom = (65f - 30f * t) * (Math.PI.toFloat() / 180f)
            val xBottom = rBottom * kotlin.math.sin(alphaBottom)
            val yBottom = dropY - rBottom * kotlin.math.cos(alphaBottom)

            val waistHalfWidth = (xTop.coerceAtMost(xBottom) * (1f - t * 0.72f))
                .coerceAtLeast(with(density) { 1.2.dp.toPx() })

            val distY = (yBottom - yTop).coerceAtLeast(1f)
            val vx = (xBottom - waistHalfWidth) * 0.65f
            val vy = distY * 0.30f

            val symbiotePath = Path().apply {
                // 1. Calotta morbida a soffitto (Y = 0)
                moveTo(centerX - xTop, yTop)
                cubicTo(
                    x1 = centerX - xTop * 0.7f, y1 = 0f,
                    x2 = centerX + xTop * 0.7f, y2 = 0f,
                    x3 = centerX + xTop, y3 = yTop
                )

                // 2. Ponte liquido destro verso la spalla inferiore
                cubicTo(
                    x1 = centerX + (xTop * 0.35f + waistHalfWidth * 0.65f),
                    y1 = yTop + distY * 0.30f,
                    x2 = (centerX + xBottom) - vx,
                    y2 = yBottom - vy,
                    x3 = centerX + xBottom,
                    y3 = yBottom
                )

                // 3. Raccordo tangenziale collineare alla spalla destra (angolo 0° continuo)
                cubicTo(
                    x1 = (centerX + xBottom) + vx * 0.5f,
                    y1 = yBottom + vy * 0.5f,
                    x2 = centerX + rBottom,
                    y2 = dropY - (dropY - yBottom) * 0.35f,
                    x3 = centerX + rBottom,
                    y3 = dropY
                )

                // 4. Fondo arrotondato della sfera
                cubicTo(
                    x1 = centerX + rBottom,
                    y1 = dropY + rBottom * 0.552f,
                    x2 = centerX + rBottom * 0.552f,
                    y2 = dropY + rBottom,
                    x3 = centerX,
                    y3 = dropY + rBottom
                )
                cubicTo(
                    x1 = centerX - rBottom * 0.552f,
                    y1 = dropY + rBottom,
                    x2 = centerX - rBottom,
                    y2 = dropY + rBottom * 0.552f,
                    x3 = centerX - rBottom,
                    y3 = dropY
                )

                // 5. Raccordo tangenziale collineare alla spalla sinistra (angolo 0° continuo)
                cubicTo(
                    x1 = centerX - rBottom,
                    y1 = dropY - (dropY - yBottom) * 0.35f,
                    x2 = (centerX - xBottom) - vx * 0.5f,
                    y2 = yBottom + vy * 0.5f,
                    x3 = centerX - xBottom,
                    y3 = yBottom
                )

                // 6. Ritorno ponte liquido sinistro verso il soffitto
                cubicTo(
                    x1 = (centerX - xBottom) + vx,
                    y1 = yBottom - vy,
                    x2 = centerX - (xTop * 0.35f + waistHalfWidth * 0.65f),
                    y2 = yTop + distY * 0.30f,
                    x3 = centerX - xTop,
                    y3 = yTop
                )
                close()
            }

            // CORPO IN NERO PROFONDO / ASSOLUTO (Pitch Black ad alto alpha)
            val retractAlpha = if (isRetracting) (retractAnim.value / 0.10f).coerceIn(0f, 1f) else 1f
            drawPath(
                path = symbiotePath,
                color = Color.Black.copy(alpha = 0.96f * retractAlpha)
            )
            drawPath(
                path = symbiotePath,
                color = Color.White.copy(alpha = 0.08f * retractAlpha),
                style = Stroke(width = with(density) { 1.dp.toPx() })
            )

        } else {
            val orbY = androidx.compose.ui.util.lerp(
                start = with(density) { 120.dp.toPx() },
                stop = floatingRestYPx,
                fraction = detachedProgress
            )
            val baseRadius = with(density) { 22.dp.toPx() }
            val currentRadius = baseRadius * (if (isRefreshing) pulseScale else 1f)

            // 1. Ritrazione del filamento residuo verso il soffitto dopo la rottura
            if (filamentRecoil > 0.02f) {
                val snapHeight = with(density) { 14.dp.toPx() } * filamentRecoil
                val snapWidth = with(density) { 2.dp.toPx() } * filamentRecoil
                val filamentPath = Path().apply {
                    moveTo(centerX - snapWidth, 0f)
                    lineTo(centerX + snapWidth, 0f)
                    lineTo(centerX, snapHeight)
                    close()
                }
                drawPath(filamentPath, color = Color.Black.copy(alpha = 0.96f))
                drawPath(filamentPath, color = Color.White.copy(alpha = 0.08f), style = Stroke(width = with(density) { 1.dp.toPx() }))
            }

            // 2. Sfera del Simbionte in nero assoluto ad alto alpha
            drawCircle(
                color = Color.Black.copy(alpha = 0.96f),
                radius = currentRadius,
                center = Offset(centerX, orbY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = currentRadius,
                center = Offset(centerX, orbY),
                style = Stroke(width = with(density) { 1.dp.toPx() })
            )

            // 3. DIAFRAMMA CINEMATOGRAFICO MINIMAL (CAMERA IRIS ESSENTIAL)
            if (isRefreshing) {
                val outerR = currentRadius * 0.88f
                val innerR = outerR * apertureFraction
                val bladeCount = 6

                // 1. Bagliore morbido nel cuore dell'iride
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to accentColor.copy(alpha = 0.45f),
                        0.6f to accentColor.copy(alpha = 0.12f),
                        1.0f to Color.Transparent,
                        center = Offset(centerX, orbY),
                        radius = (innerR * 1.5f).coerceAtLeast(1f)
                    ),
                    radius = innerR * 1.5f,
                    center = Offset(centerX, orbY)
                )

                // 2. Anello esterno pulito e nitido dell'obiettivo
                drawCircle(
                    color = accentColor.copy(alpha = 0.85f),
                    radius = outerR,
                    center = Offset(centerX, orbY),
                    style = Stroke(width = with(density) { 1.4.dp.toPx() })
                )

                // 3. 6 Lamelle del diaframma (linee pulite, zero elementi superflui)
                val tanLen = kotlin.math.sqrt((outerR * outerR - innerR * innerR).coerceAtLeast(0f))

                for (i in 0 until bladeCount) {
                    val phi = (shutterAngle + i * (360f / bladeCount)) * (Math.PI.toFloat() / 180f)
                    val nextPhi = (shutterAngle + (i + 1) * (360f / bladeCount)) * (Math.PI.toFloat() / 180f)

                    val cosP = kotlin.math.cos(phi)
                    val sinP = kotlin.math.sin(phi)
                    val cosNext = kotlin.math.cos(nextPhi)
                    val sinNext = kotlin.math.sin(nextPhi)

                    // Vertice tangenziale all'apertura interna
                    val tx = centerX + innerR * cosP
                    val ty = orbY + innerR * sinP

                    // Vertice sul cerchio esterno
                    val px = tx - tanLen * sinP
                    val py = ty + tanLen * cosP

                    val nextTx = centerX + innerR * cosNext
                    val nextTy = orbY + innerR * sinNext
                    val nextPx = nextTx - tanLen * sinNext
                    val nextPy = nextTy + tanLen * cosNext

                    // Campitura leggera di ciascuna lamella
                    val bladePath = Path().apply {
                        moveTo(tx, ty)
                        lineTo(px, py)
                        lineTo(nextPx, nextPy)
                        lineTo(nextTx, nextTy)
                        close()
                    }
                    drawPath(
                        path = bladePath,
                        color = accentColor.copy(alpha = 0.10f)
                    )

                    // Linea affilata principale della lamella dell'iride
                    drawLine(
                        color = accentColor.copy(alpha = 0.95f),
                        start = Offset(px, py),
                        end = Offset(tx, ty),
                        strokeWidth = with(density) { 1.3.dp.toPx() }
                    )
                }

                // 4. Bordo dell'apertura centrale esagonale
                val aperturePath = Path()
                for (i in 0 until bladeCount) {
                    val phi = (shutterAngle + i * (360f / bladeCount)) * (Math.PI.toFloat() / 180f)
                    val tx = centerX + innerR * kotlin.math.cos(phi)
                    val ty = orbY + innerR * kotlin.math.sin(phi)
                    if (i == 0) aperturePath.moveTo(tx, ty) else aperturePath.lineTo(tx, ty)
                }
                aperturePath.close()

                drawPath(
                    path = aperturePath,
                    color = accentColor.copy(alpha = 0.70f),
                    style = Stroke(width = with(density) { 1.2.dp.toPx() })
                )
            }
        }
    }
}
