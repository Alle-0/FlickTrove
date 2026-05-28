package com.cinetrack.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Screen root
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LogoAnimationScreen(hazeState: HazeState? = null, onAnimationEnd: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(if (hazeState != null) Modifier.haze(hazeState) else Modifier)
    ) {
        MetamorphosisLogo(
            modifier = Modifier.fillMaxSize(),
            onAnimationEnd = onAnimationEnd
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// La Metamorfosi dei Serpenti Vettoriali (V3)
//
// 1. Centratura nativa: nessun offset TX/TY arbitrario, usiamo il Path puro.
// 2. Aggancio perfetto: il segmento dritto e il path curvo condividono lo 
//    stesso startPoint (0f).
// 3. Trasferimento di lunghezza (Train logic): l'ago è lungo esattamente
//    sp.length. Quando entra nella curva, la linea dritta si accorcia in modo
//    identico a quanto si allunga la curva, preservando la materia.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MetamorphosisLogo(modifier: Modifier = Modifier, onAnimationEnd: () -> Unit = {}) {

    val fullPathData =
        "M116.934 351.021C147.681 367.151 182.789 366.979 232.617 298.312L351.796 135.803" +
        "C352.53 134.803 354.593 136.08 354.134 137.234C327.209 204.99 389.104 240.954 442 200.01" +
        "C442.969 199.26 444.678 200.781 443.953 201.768L318.112 373.265" +
        "C276.104 423.885 200.154 428.716 152.071 383.826L116.934 351.021Z" +
        "M116.934 351.021L58.7977 296.746C45.0417 283.903 44.3346 262.329 57.2201 248.613" +
        "L86.0595 217.915C98.9382 204.207 120.5 203.559 134.178 216.471L215.627 293.354" +
        "M183.002 326.191C189.058 319.756 188.751 309.63 182.316 303.574L170.563 292.515" +
        "C164.128 286.459 154.002 286.766 147.946 293.201L136.887 304.953" +
        "C130.831 311.388 131.138 321.514 137.573 327.57L149.271 338.579" +
        "M386.805 115.855L381.82 161.544C380.151 176.849 395.675 188.222 409.765 182.015" +
        "L451.824 163.488C465.914 157.281 468.001 138.15 455.581 129.051" +
        "L418.506 101.89C406.086 92.7918 388.474 100.55 386.805 115.855Z" +
        "M240.912 358.122L261.262 373.095C270.159 379.641 282.678 377.736 289.225 368.839" +
        "L304.198 348.488C310.744 339.592 308.839 327.072 299.942 320.526" +
        "L279.591 305.553C270.694 299.007 258.175 300.912 251.629 309.809" +
        "L236.656 330.16C230.109 339.056 232.015 351.575 240.912 358.122Z" +
        "M297.907 284.914L318.299 299.83C327.214 306.352 339.728 304.411 346.249 295.496" +
        "L361.166 275.104C367.687 266.189 365.747 253.675 356.832 247.154" +
        "L336.44 232.237C327.524 225.716 315.011 227.656 308.489 236.571" +
        "L293.573 256.963C287.051 265.878 288.992 278.392 297.907 284.914Z" +
        "M134.178 280.813L128.21 287.154L123.033 292.515" +
        "C116.326 299.458 105.32 299.83 98.1608 293.354L86.3765 282.263" +
        "C78.7381 275.075 78.3733 263.056 85.5617 255.417L95.7214 244.621" +
        "C102.91 236.983 114.929 236.618 122.568 243.807L133.363 253.966" +
        "C141.002 261.155 141.367 273.174 134.178 280.813Z"

    // ── Parse una volta sola (NO TRANSLATE HACKS) ─────────────────────────────

    data class SubPath(
        val path: android.graphics.Path,
        val measure: android.graphics.PathMeasure,
        val length: Float,
        val startX: Float,
        val startY: Float,
        val entryAngleRad: Float
    )

    val (logoBounds, subPaths) = remember {
        val strs = fullPathData.split(Regex("(?=M)")).filter { it.isNotBlank() }

        val list = strs.map { str ->
            val p  = PathParser.createPathFromPathData(str.trim())
            val m  = android.graphics.PathMeasure(p, false)
            val pos = FloatArray(2)
            val tan = FloatArray(2)
            m.getPosTan(0f, pos, tan)

            val tLen = sqrt((tan[0] * tan[0] + tan[1] * tan[1]).toDouble()).toFloat()
            val entryAngle = if (tLen > 0.001f)
                atan2((-tan[1] / tLen).toDouble(), (-tan[0] / tLen).toDouble()).toFloat()
            else
                0f

            SubPath(p, m, m.length, pos[0], pos[1], entryAngle)
        }

        // Bounding box PURA
        val combined = android.graphics.Path()
        list.forEach { combined.addPath(it.path) }
        val bounds = android.graphics.RectF()
        combined.computeBounds(bounds, true)

        Pair(bounds, list)
    }

    val drawBuffers = remember { List(subPaths.size) { androidx.compose.ui.graphics.Path() } }

    // ── Animazione (Decomposizione Inversa) ───────────────────────────────────

    val progress  = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Pausa iniziale per far combaciare con la fine della splash nativa
        kotlinx.coroutines.delay(250L)
        
        // Esegue l'animazione di scomposizione
        launch {
            progress.animateTo(
                targetValue   = 0f,
                animationSpec = tween(durationMillis = 1600, easing = LinearEasing)
            )
        }
        
        // Inizia la dissolvenza alla home a metà dell'animazione
        kotlinx.coroutines.delay(800L)
        onAnimationEnd()
    }

    val globalP = progress.value

    // ── Gradiente (Coordinate native dell'SVG) ────────────────────────────────

    val accentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val gradient = remember(accentColor) {
        val secondaryColor = when (accentColor) {
            com.cinetrack.ui.theme.NeonAmber -> com.cinetrack.ui.theme.NeonPink
            com.cinetrack.ui.theme.NeonPink -> com.cinetrack.ui.theme.NeonPurple
            com.cinetrack.ui.theme.NeonPurple -> Color(0xFF448AFF)
            Color(0xFF448AFF), com.cinetrack.ui.theme.NeonBlue -> com.cinetrack.ui.theme.NeonTeal
            com.cinetrack.ui.theme.NeonTeal -> Color(0xFFF386F8)
            else -> {
                // Per i colori HEX personalizzati, calcoliamo un colore secondario shiftando la tonalità (hue)
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(accentColor.toArgb(), hsv)
                hsv[0] = (hsv[0] + 50f) % 360f // Sposta la tonalità per creare un bel gradiente
                Color(android.graphics.Color.HSVToColor(hsv))
            }
        }
        
        Brush.linearGradient(
            colors = listOf(accentColor, secondaryColor),
            start  = Offset(48f, 274f),
            end    = Offset(464f, 155f)
        )
    }

    // ── Canvas ────────────────────────────────────────────────────────────────

    Canvas(modifier = modifier) {
        // Traslazione PERFETTA per farla combaciare con l'icona di sistema Android
        // La splash screen nativa su Android 12+ impone una dimensione di 288dp
        val iconSizePx = 265.dp.toPx()
        val sf = iconSizePx / 768f
        
        // Centriamo il viewport 768x768 (che ha centro in 384, 384) nello schermo
        val originX = size.width  / 2f - 384f * sf
        val originY = size.height / 2f - 384f * sf

        translate(left = originX, top = originY) {
            scale(sf, sf, pivot = Offset.Zero) {
                // Nel file ic_launcher_foreground_vector.xml il path è traslato di 128
                translate(128f, 128f) {

                val ENTRY_DIST = 850f
                val STAGGER    = 0.05f
                val APPROACH_T = 0.35f

                subPaths.forEachIndexed { idx, sp ->
                    val delay = idx * STAGGER
                    val pp = ((globalP - delay) / (1f - delay)).coerceIn(0f, 1f)
                    if (pp <= 0f) return@forEachIndexed
                    // Disabling the strand skipping performance optimization to ensure all pieces are drawn.

                    // ── Modello cinematico: la variabile "u" ──────────────────
                    // Rappresenta la posizione della punta dell'ago lungo il tracciato
                    // continuo (linea retta off-screen -> path curvo).
                    // u = 0 è ESATTAMENTE lo startPoint del path.
                    
                    val u = if (pp <= APPROACH_T) {
                        // Fase 1: Invasione (Movimento lineare balistico, nessuno stop)
                        val t = pp / APPROACH_T
                        -ENTRY_DIST * (1f - t)
                    } else {
                        // Fase 2: Metamorfosi (Decelera dolcemente lungo la curva)
                        val t = (pp - APPROACH_T) / (1f - APPROACH_T)
                        val ease = 1f - (1f - t) * (1f - t) * (1f - t)
                        sp.length * ease
                    }

                    // Il corpo dell'ago ha una lunghezza ESATTA pari a sp.length.
                    // Questo garantisce il "trasferimento di massa" perfetto:
                    // quando la testa (u) arriva in fondo alla curva (sp.length),
                    // la coda (u - sp.length) arriva a 0 (fine della linea dritta).
                    val head = u
                    val tail = u - sp.length

                    val sx = sp.startX
                    val sy = sp.startY
                    val ax = cos(sp.entryAngleRad)
                    val ay = sin(sp.entryAngleRad)

                    // 1. Tratto Dritto (Fuori dal Path)
                    val headOnStraight = head.coerceAtMost(0f)
                    val tailOnStraight = tail.coerceAtMost(0f)

                    if (headOnStraight > tailOnStraight) {
                        // Le distanze sono negative lungo l'asse, le invertiamo
                        val startDist = -tailOnStraight
                        val endDist   = -headOnStraight
                        
                        drawLine(
                            brush = gradient,
                            start = Offset(sx + ax * startDist, sy + ay * startDist),
                            end   = Offset(sx + ax * endDist, sy + ay * endDist),
                            strokeWidth = 10f,
                            cap = StrokeCap.Round
                        )
                    }

                    // 2. Tratto Curvo (Sul Path)
                    val headOnCurve = head.coerceAtLeast(0f)
                    val tailOnCurve = tail.coerceAtLeast(0f)

                    if (headOnCurve > tailOnCurve) {
                        drawBuffers[idx].reset()
                        sp.measure.getSegment(tailOnCurve, headOnCurve, drawBuffers[idx].asAndroidPath(), true)
                        drawPath(
                            path  = drawBuffers[idx],
                            brush = gradient,
                            style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
fun FlickTroveAnimatedLogo(modifier: Modifier = Modifier) = MetamorphosisLogo(modifier = modifier)
