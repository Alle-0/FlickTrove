package com.cinetrack.ui.components.home

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Overlay "Obiettivo Cinema" con messa a fuoco a pressione prolungata (Cinema Lens)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun TroveHoldToUnsealOverlay(
    chargeProgress: Float,
    isHolding: Boolean,
    alpha: Float,
    scale: Float,
    matchScore: Int?,
    year: String,
    genres: List<String>,
    onMedallionPositioned: (LayoutCoordinates) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Respiro ottico a riposo (3.2 secondi) che dà vita all'obiettivo prima del tocco
    val infiniteTransition = rememberInfiniteTransition(label = "TroveLensIdle")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TroveLensIdlePulse"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.scaleX = scale
            this.scaleY = scale
        },
        contentAlignment = Alignment.Center
    ) {
        // Sottile vignettatura radiale di sala al centro per far risaltare l'ottica e il teaser dal blur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.50f),
                            0.55f to Color.Black.copy(alpha = 0.35f),
                            1.00f to Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.offset(y = (-8).dp)
        ) {
            // Obiettivo Cinema con ghiera di messa a fuoco
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .onGloballyPositioned { onMedallionPositioned(it) },
                contentAlignment = Alignment.Center
            ) {
                // 0. Bolle gravitazionali che convergono venendo risucchiate nel fuoco dell'obiettivo
                if (chargeProgress > 0.005f) {
                    TroveGravitationalBubbles(
                        chargeProgress = chargeProgress,
                        modifier = Modifier
                            .size(76.dp)
                            .graphicsLayer { clip = false }
                    )
                }

                // 1. Alone luminoso dell'ottica: delicato respiro in idle, fascio focalizzato durante la carica
                val baseAuraAlpha = if (chargeProgress > 0.01f) {
                    (chargeProgress * 0.38f).coerceIn(0f, 0.38f)
                } else {
                    0.08f + idlePulse * 0.12f
                }
                val auraSize = if (chargeProgress > 0.01f) {
                    (52 + chargeProgress * 28).dp
                } else {
                    (56 + idlePulse * 10).dp
                }
                Box(
                    modifier = Modifier
                        .size(auraSize)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    primaryColor.copy(alpha = baseAuraAlpha),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                // 2. Corpo lente in vetro ottico multistrato con finitura antiriflesso cinema
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to Color(0xFF1C2028).copy(alpha = 0.94f),
                                    0.55f to Color(0xFF0F1116).copy(alpha = 0.92f),
                                    0.90f to Color(0xFF060709).copy(alpha = 0.96f),
                                    1.00f to primaryColor.copy(alpha = 0.18f)
                                )
                            ),
                            CircleShape
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isHolding) 0.50f else 0.28f),
                                    primaryColor.copy(alpha = if (isHolding) 0.60f else 0.20f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // 3. Ghiera di messa a fuoco con tacche di calibrazione e anello di carica progressivo
                Canvas(modifier = Modifier.size(68.dp)) {
                    val strokeWidth = 2.5.dp.toPx()
                    val radius = (size.minDimension / 2f) - strokeWidth / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Tacche millimetriche della ghiera di messa a fuoco (12 tacche a 30°)
                    // Ruotano leggermente mentre si mette a fuoco
                    val ringRotation = chargeProgress * 45f
                    val tickRadiusInner = radius - 4.dp.toPx()
                    val tickRadiusOuter = radius - 1.5.dp.toPx()
                    val tickColor = Color.White.copy(alpha = if (isHolding) 0.35f else 0.18f)

                    for (i in 0 until 12) {
                        val angleDeg = (i * 30f) + ringRotation
                        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                        val cosA = cos(angleRad)
                        val sinA = sin(angleRad)
                        val isMajor = (i % 3 == 0)
                        val rIn = if (isMajor) tickRadiusInner - 1.5.dp.toPx() else tickRadiusInner

                        drawLine(
                            color = if (isMajor) primaryColor.copy(alpha = 0.50f) else tickColor,
                            start = Offset(center.x + rIn * cosA, center.y + rIn * sinA),
                            end = Offset(center.x + tickRadiusOuter * cosA, center.y + tickRadiusOuter * sinA),
                            strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Anello interno di guida del diaframma
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = radius * 0.62f,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )

                    // Arco di messa a fuoco attivo (Focus Arc)
                    if (chargeProgress > 0.005f) {
                        val sweep = chargeProgress * 360f

                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                            size = Size(radius * 2, radius * 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )

                        // Testa luminosa dell'arco che segue la rotazione della messa a fuoco
                        val angleRad = Math.toRadians((-90.0 + sweep)).toFloat()
                        val dotX = center.x + radius * cos(angleRad)
                        val dotY = center.y + radius * sin(angleRad)

                        drawCircle(
                            color = primaryColor.copy(alpha = 0.60f),
                            radius = 5.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }

                // 4. Icona scintillio al centro dell'ottica: brilla ed entra in pieno fuoco (> 0.85f)
                val iconScale = 1f + chargeProgress * 0.12f
                Icon(
                    painter = painterResource(id = R.drawable.ic_sparkle),
                    contentDescription = null,
                    tint = if (chargeProgress > 0.85f) Color.White else primaryColor,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }

            // ── Esca psicologica: micro-indizio metadati (Pillola Glass con Badge Inset) ──
            val hasTeaserData = (matchScore != null && matchScore > 0) || year.isNotEmpty() || genres.isNotEmpty()
            if (hasTeaserData) {
                val hasMatch = matchScore != null && matchScore > 0
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF131822).copy(alpha = 0.92f),
                                    Color(0xFF090C10).copy(alpha = 0.88f)
                                )
                            ),
                            CircleShape
                        )
                        .border(
                            0.5.dp,
                            if (isHolding) primaryColor.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.18f),
                            CircleShape
                        )
                        .padding(
                            start = if (hasMatch) 4.5.dp else 12.dp,
                            end = 12.dp,
                            top = if (hasMatch) 4.dp else 5.dp,
                            bottom = if (hasMatch) 4.dp else 5.dp
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge inset pieno teal per la percentuale di affinità
                        if (hasMatch) {
                            Box(
                                modifier = Modifier
                                    .background(primaryColor, CircleShape)
                                    .padding(horizontal = 7.5.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = "${matchScore}% " + stringResource(R.string.match_score).uppercase(),
                                    color = Color(0xFF07090E),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }

                        // Anno e Genere principale
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (year.isNotEmpty()) {
                                Text(
                                    text = year,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (year.isNotEmpty() && genres.isNotEmpty()) {
                                Text(
                                    text = "•",
                                    color = Color.White.copy(alpha = 0.35f),
                                    fontSize = 10.sp
                                )
                            }

                            if (genres.isNotEmpty()) {
                                Text(
                                    text = genres.first(),
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ── Call-To-Action: "Tieni premuto per scoprire il Trove di oggi" ──
            Text(
                text = stringResource(R.string.home_trove_pick_sealed_cta),
                color = if (isHolding) primaryColor else Color.White.copy(alpha = 0.82f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                letterSpacing = 0.25.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bolle gravitazionali che viaggiano dall'esterno verso il centro e si deformano
// ─────────────────────────────────────────────────────────────────────────────

private data class TroveGravitationalBubble(
    val baseAngle: Float,
    val spawnDistDp: Float,
    val speed: Float,
    val phaseOffset: Float,
    val baseRadiusDp: Float,
    val color: Color
)

@Composable
internal fun TroveGravitationalBubbles(
    chargeProgress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TroveBubblesInf")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TroveBubblePhase"
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    // Palette cromatica armonica basata sul tema dell'app (Primary, Secondary, Tertiary, White)
    val bubbles = remember(primary, secondary, tertiary) {
        val colors = listOf(
            primary,
            secondary,
            primary.copy(alpha = 0.85f),
            tertiary,
            Color.White.copy(alpha = 0.90f)
        )
        val count = 10 // Pochi elementi nitidi e visibili, non ammassati
        List(count) { i ->
            val angle = (2.0 * PI * i / count).toFloat() + (((i * 17) % 5) - 2) * 0.10f
            val spawnDist = 135f + ((i * 13) % 4) * 8f // 135dp .. 159dp (lontano, nei bordi della card)
            val speed = 0.90f + ((i * 7) % 5) * 0.05f
            val offset = (i.toFloat() / count) // Distribuite uniformemente nel tempo
            val radius = 5.0f + (i % 3) * 1.5f // 5.0dp .. 8.0dp (leggere e discrete)
            val color = colors[i % colors.size]
            TroveGravitationalBubble(
                baseAngle = angle,
                spawnDistDp = spawnDist,
                speed = speed,
                phaseOffset = offset,
                baseRadiusDp = radius,
                color = color
            )
        }
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f) // Baricentro perfetto del medaglione
        val deformThreshold = 52.dp.toPx() // Sotto i 52dp si deforma venendo risucchiata verso il centro
        val diskRadiusPx = 34.dp.toPx() // Raggio del disco centrale da 68dp
        val borderFadeDistPx = 6.dp.toPx() // Distanza di transizione sfocatura / scomparsa bordo
        val minDist = 14.dp.toPx() // Scompare sotto il disco scuro

        // Dimensioni e posizione della pillola teaser dei metadati
        val pillCenterY = center.y + 58.dp.toPx()
        val pillHalfWidth = 92.dp.toPx()
        val pillCornerRadius = 14.dp.toPx()

        // All'aumentare della carica col tocco, il flusso accelera naturalmente
        val speedBoost = 1f + chargeProgress * 2.0f
        val masterAlpha = (chargeProgress / 0.08f).coerceIn(0f, 1f)

        for (bubble in bubbles) {
            val progress = ((phase * bubble.speed * speedBoost + bubble.phaseOffset) % 1f)
            val spawnDistPx = bubble.spawnDistDp.dp.toPx()

            // Traiettoria dall'esterno (spawnDist) verso il centro:
            // Procede a velocità fluida, accelerando solo nell'ultimo tratto
            val travelProgress = progress.pow(1.3f)
            val dist = spawnDistPx - (spawnDistPx - minDist) * travelProgress

            if (dist < minDist) continue

            val angle = bubble.baseAngle
            val bx = center.x + dist * cos(angle)
            val by = center.y + dist * sin(angle)

            // Deformazione solo in prossimità del centro (spaghettificazione gravitazionale sotto i 52dp):
            val stretchFactor: Float
            val compressFactor: Float
            if (dist > deformThreshold) {
                stretchFactor = 1.0f
                compressFactor = 1.0f
            } else {
                val t = ((deformThreshold - dist) / (deformThreshold - minDist)).coerceIn(0f, 1f)
                stretchFactor = 1.0f + t * (1.8f + chargeProgress * 0.7f)
                compressFactor = (1.0f - t * 0.40f).coerceAtLeast(0.55f)
            }

            // Calcolo sfocatura:
            // 1. Sotto il disco centrale
            val diskDist = kotlin.math.hypot(bx - center.x, by - center.y)
            val diskT = ((diskRadiusPx + borderFadeDistPx - diskDist) / borderFadeDistPx).coerceIn(0f, 1f)

            // 2. Sotto la pillola teaser dei metadati (capsula)
            val pillDx = (kotlin.math.abs(bx - center.x) - (pillHalfWidth - pillCornerRadius)).coerceAtLeast(0f)
            val pillDy = kotlin.math.abs(by - pillCenterY)
            val pillDist = kotlin.math.hypot(pillDx, pillDy)
            val pillT = ((pillCornerRadius + borderFadeDistPx - pillDist) / borderFadeDistPx).coerceIn(0f, 1f)

            // Fattore unificato di sfocatura (attivo sia sotto il disco che sotto la pillola teaser)
            val blurT = maxOf(diskT, pillT)
            val strokeAlpha = (1f - blurT).coerceIn(0f, 1f)

            val r = bubble.baseRadiusDp.dp.toPx()
            val fadeIn = (progress / 0.12f).coerceIn(0f, 1f)
            val fadeOut = ((1f - progress) / 0.10f).coerceIn(0f, 1f)
            val alpha = (fadeIn * fadeOut * masterAlpha * (0.60f + chargeProgress * 0.40f)).coerceIn(0f, 1f)

            if (alpha <= 0.01f) continue

            val rx = r * stretchFactor
            val ry = r * compressFactor
            val angleDeg = if (stretchFactor != 1.0f) Math.toDegrees(angle.toDouble()).toFloat() else 0f

            rotate(degrees = angleDeg, pivot = Offset(bx, by)) {
                // Se la bolla passa sotto il disco o sotto la pillola teaser, applica sfocatura gaussiana diffusa
                if (blurT > 0.04f) {
                    drawIntoCanvas { canvas ->
                        val blurPx = (6.dp.toPx() + blurT * 12.dp.toPx())
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = bubble.color.toArgb()
                            this.alpha = (alpha * (0.35f + blurT * 0.35f) * 255).toInt().coerceIn(0, 255)
                            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                        }
                        canvas.nativeCanvas.drawOval(
                            bx - rx * (1.1f + blurT * 0.4f),
                            by - ry * (1.1f + blurT * 0.4f),
                            bx + rx * (1.1f + blurT * 0.4f),
                            by + ry * (1.1f + blurT * 0.4f),
                            paint
                        )
                    }
                }

                // Bagliore soffice
                val glowExpand = 1.35f + blurT * 0.8f
                drawOval(
                    color = bubble.color.copy(alpha = alpha * (0.20f + blurT * 0.15f)),
                    topLeft = Offset(bx - rx * glowExpand, by - ry * glowExpand),
                    size = Size(rx * glowExpand * 2f, ry * glowExpand * 2f)
                )

                // Interno traslucido sfumato
                val bodyExpand = 1f + blurT * 0.4f
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            bubble.color.copy(alpha = alpha * (0.18f + blurT * 0.15f)),
                            bubble.color.copy(alpha = alpha * 0.45f * (1f - blurT * 0.6f)),
                            Color.Transparent
                        ),
                        center = Offset(bx, by),
                        radius = rx * bodyExpand
                    ),
                    topLeft = Offset(bx - rx * bodyExpand, by - ry * bodyExpand),
                    size = Size(rx * bodyExpand * 2f, ry * bodyExpand * 2f)
                )

                // Bordo netto della bolla: sfuma fino a sparire COMPLETAMENTE sotto il disco o la pillola
                if (strokeAlpha > 0.01f) {
                    drawOval(
                        color = bubble.color.copy(alpha = alpha * 0.90f * strokeAlpha),
                        topLeft = Offset(bx - rx, by - ry),
                        size = Size(rx * 2f, ry * 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.3.dp.toPx() * strokeAlpha)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Canvas burst (esplosione cinematografica con 36 particelle + onde d'urto)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun TroveUnsealBurstCanvas(
    progress: Float,
    burstCenter: Offset = Offset.Zero
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = if (burstCenter != Offset.Zero) {
            burstCenter
        } else {
            Offset(size.width / 2f, size.height * 0.30f)
        }

        val maxDist = kotlin.math.hypot(size.width, size.height) * 0.75f

        // 1. Bagliore centrale istantaneo
        val flashAlpha = ((1f - progress * 2.2f)).coerceIn(0f, 1f)
        if (flashAlpha > 0.01f) {
            val flashRadius = 60.dp.toPx() + progress * 140.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = flashAlpha * 0.70f),
                        primary.copy(alpha = flashAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = flashRadius
                ),
                center = center,
                radius = flashRadius
            )
        }

        // 2. Anelli d'urto concentrici veloci (2 anelli sfasati)
        for (ringIdx in 0..1) {
            val ringDelay = ringIdx * 0.12f
            val ringProgress = ((progress - ringDelay) / (1f - ringDelay)).coerceIn(0f, 1f)
            if (ringProgress > 0f) {
                val ringRadius = maxDist * 0.85f * (ringProgress.pow(0.65f))
                val ringAlpha = ((1f - ringProgress).pow(1.5f)).coerceIn(0f, 1f)
                val ringStroke = (3.dp.toPx() * (1f - ringProgress * 0.6f)).coerceAtLeast(0.5.dp.toPx())

                drawCircle(
                    color = if (ringIdx == 0) primary.copy(alpha = ringAlpha * 0.55f) else secondary.copy(alpha = ringAlpha * 0.40f),
                    radius = ringRadius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = ringStroke)
                )
            }
        }

        // 3. Particelle stellari ed energetiche organiche (36 particelle con velocità e dimensioni variegate)
        val numParticles = 36
        val particleFade = ((1f - progress.pow(1.3f))).coerceIn(0f, 1f)
        for (i in 0 until numParticles) {
            val baseAngle = (2.0 * PI * i / numParticles).toFloat()
            val jitter = (((i * 17) % 11) - 5) * 0.035f
            val angle = baseAngle + jitter

            val speedFactor = 0.45f + (((i * 23) % 10) / 10f) * 0.65f
            val dist = maxDist * speedFactor * (progress.pow(0.72f))

            val x = center.x + dist * cos(angle)
            val y = center.y + dist * sin(angle)

            val pRadius = ((1.5f + (i % 4) * 0.9f).dp.toPx() * (1f - progress * 0.55f)).coerceAtLeast(0.5.dp.toPx())
            val pColor = when (i % 3) {
                0 -> primary
                1 -> secondary
                else -> Color.White
            }

            drawCircle(
                color = pColor.copy(alpha = particleFade * 0.90f),
                radius = pRadius,
                center = Offset(x, y)
            )
        }
    }
}
