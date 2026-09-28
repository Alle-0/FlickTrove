package com.cinetrack.ui.components.badge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Collezione di Icone Vettoriali Personalizzate disegnate su misura per i Trofei FlickTrove.
 * Nessuna icona riciclata, nessuna emoji di sistema.
 * Tratto di precisione 1.5–2.0 dp, geometrie cinematografiche pure e dettagli raffinati.
 */
object BadgeCustomIcons {

    /**
     * 1. BOBINA CINEMATOGRAFICA D'AUTORE (Badge Frequenza 24fps - Visione Film)
     * Bobina classica a razze sagomate con mozzo centrale e nastro di pellicola che scorre.
     */
    @Composable
    fun CinemaReel(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = (this.size.minDimension / 2f) - strokeW

                // Anello esterno della bobina
                drawCircle(
                    color = tint,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeW)
                )

                // Mozzo centrale
                val hubR = radius * 0.28f
                drawCircle(
                    color = tint,
                    radius = hubR,
                    center = center,
                    style = Stroke(width = strokeW)
                )
                drawCircle(
                    color = tint,
                    radius = hubR * 0.35f,
                    center = center
                )

                // 4 Fori sagomati a mandorla / finestra della bobina (a 90°)
                val holeDist = radius * 0.62f
                val holeR = radius * 0.16f
                for (i in 0 until 4) {
                    val angle = Math.toRadians(i * 90.0 + 45.0)
                    val hx = center.x + (holeDist * cos(angle)).toFloat()
                    val hy = center.y + (holeDist * sin(angle)).toFloat()
                    drawCircle(
                        color = tint,
                        radius = holeR,
                        center = Offset(hx, hy),
                        style = Stroke(width = strokeW * 0.85f)
                    )
                }

                // Coda di pellicola cinematografica che scorre in uscita in basso a destra
                val filmPath = Path().apply {
                    moveTo(center.x + radius * 0.85f, center.y + radius * 0.55f)
                    cubicTo(
                        center.x + radius * 1.15f, center.y + radius * 0.85f,
                        center.x + radius * 0.70f, center.y + radius * 1.15f,
                        center.x + radius * 0.40f, center.y + radius * 1.05f
                    )
                }
                drawPath(
                    path = filmPath,
                    color = tint,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
        }
    }

    /**
     * 2. CORONA MAESTOSA DEL PIONIERE (Badge Pioniere della Prima Bobina - Day One)
     * Corona nobiliare con gemme intagliate, fascia dorata e stella cinematografica incisa.
     */
    @Composable
    fun PioneerCrown(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val w = this.size.width * 0.82f
                val h = this.size.height * 0.68f
                val left = center.x - w / 2f
                val top = center.y - h / 2f
                val bottom = top + h

                // Sagoma della corona a 5 punte
                val crownPath = Path().apply {
                    moveTo(left, bottom - h * 0.18f)
                    lineTo(left - w * 0.04f, top + h * 0.25f) // Punta sinistra esterna
                    lineTo(left + w * 0.25f, top + h * 0.52f) // Gola 1
                    lineTo(left + w * 0.35f, top + h * 0.08f) // Punta 2
                    lineTo(left + w * 0.50f, top + h * 0.38f) // Gola centrale
                    lineTo(left + w * 0.65f, top + h * 0.08f) // Punta 4
                    lineTo(left + w * 0.75f, top + h * 0.52f) // Gola 3
                    lineTo(left + w * 1.04f, top + h * 0.25f) // Punta destra esterna
                    lineTo(left + w, bottom - h * 0.18f)
                    close()
                }
                drawPath(
                    path = crownPath,
                    color = tint,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Fascia inferiore della corona
                val baseRect = Rect(left, bottom - h * 0.16f, left + w, bottom)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(baseRect.left, baseRect.top),
                    size = Size(baseRect.width, baseRect.height),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = strokeW)
                )

                // 3 Perle / Gemme incastonate alla base
                val gemR = w * 0.045f
                drawCircle(color = tint, radius = gemR, center = Offset(center.x - w * 0.28f, baseRect.center.y))
                drawCircle(color = tint, radius = gemR, center = Offset(center.x, baseRect.center.y))
                drawCircle(color = tint, radius = gemR, center = Offset(center.x + w * 0.28f, baseRect.center.y))

                // Stella centrale scintillante sopra la corona
                drawStarShape(Offset(center.x, top - h * 0.05f), w * 0.14f, tint, strokeW)
            }
        }
    }

    /**
     * 3. MONITOR CRT RETRÒ & ONDA SERIALE (Badge Maratoneta Seriale - Serie TV)
     * Televisore d'epoca con antenne a V orientate, schermo curvo e quadrante canali.
     */
    @Composable
    fun TvBinge(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val w = this.size.width * 0.84f
                val h = this.size.height * 0.62f
                val left = center.x - w / 2f
                val top = center.y - h * 0.35f

                // Due antenne a V telescopiche con palline terminali
                val antLeft = Path().apply {
                    moveTo(center.x - w * 0.15f, top)
                    lineTo(center.x - w * 0.38f, top - h * 0.40f)
                }
                val antRight = Path().apply {
                    moveTo(center.x + w * 0.15f, top)
                    lineTo(center.x + w * 0.38f, top - h * 0.40f)
                }
                drawPath(antLeft, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                drawPath(antRight, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                drawCircle(tint, radius = 2.dp.toPx(), center = Offset(center.x - w * 0.38f, top - h * 0.40f))
                drawCircle(tint, radius = 2.dp.toPx(), center = Offset(center.x + w * 0.38f, top - h * 0.40f))

                // Corpo TV arrotondato
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(width = strokeW)
                )

                // Schermo interno curvo
                val screenW = w * 0.64f
                val screenH = h * 0.72f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left + w * 0.08f, top + h * 0.14f),
                    size = Size(screenW, screenH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = strokeW * 0.9f)
                )

                // Onda di maratona / Play sullo schermo
                val wavePath = Path().apply {
                    val sx = left + w * 0.16f
                    val sy = top + h * 0.50f
                    moveTo(sx, sy)
                    lineTo(sx + screenW * 0.20f, sy - screenH * 0.25f)
                    lineTo(sx + screenW * 0.45f, sy + screenH * 0.25f)
                    lineTo(sx + screenW * 0.68f, sy)
                }
                drawPath(wavePath, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Manopole e fessure altoparlante a destra
                val knobX = left + w * 0.85f
                drawCircle(tint, radius = 2.4.dp.toPx(), center = Offset(knobX, top + h * 0.32f), style = Stroke(width = strokeW * 0.8f))
                drawCircle(tint, radius = 2.4.dp.toPx(), center = Offset(knobX, top + h * 0.65f), style = Stroke(width = strokeW * 0.8f))
            }
        }
    }

    /**
     * 4. OROLOGIO ASTRONOMICO DELLA PELLICOLA (Badge Odissea del Tempo - Ore di Visione)
     * Clessidra/Orologio con anello equatoriale a pellicola e lancette di precisione.
     */
    @Composable
    fun TimeOdyssey(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = (this.size.minDimension / 2f) * 0.85f

                // Quadrante circolare
                drawCircle(
                    color = tint,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeW)
                )

                // 4 Tacche delle ore cardinali (12, 3, 6, 9)
                val tickLen = radius * 0.22f
                drawLine(tint, Offset(center.x, center.y - radius), Offset(center.x, center.y - radius + tickLen), strokeW)
                drawLine(tint, Offset(center.x + radius, center.y), Offset(center.x + radius - tickLen, center.y), strokeW)
                drawLine(tint, Offset(center.x, center.y + radius), Offset(center.x, center.y + radius - tickLen), strokeW)
                drawLine(tint, Offset(center.x - radius, center.y), Offset(center.x - radius + tickLen, center.y), strokeW)

                // Lancette geometriche (ore e minuti che indicano il cinema perpetuo)
                val hourHand = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + radius * 0.42f, center.y - radius * 0.28f)
                }
                val minHand = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x, center.y - radius * 0.62f)
                }
                drawPath(hourHand, tint, style = Stroke(width = strokeW * 1.2f, cap = StrokeCap.Round))
                drawPath(minHand, tint, style = Stroke(width = strokeW * 1.2f, cap = StrokeCap.Round))
                drawCircle(tint, radius = 2.5.dp.toPx(), center = center)

                // Anello orbitale a pellicola cinematografica (arco ellittico attorno)
                val orbitPath = Path().apply {
                    moveTo(center.x - radius * 1.15f, center.y + radius * 0.40f)
                    cubicTo(
                        center.x - radius * 0.6f, center.y + radius * 1.20f,
                        center.x + radius * 0.8f, center.y + radius * 1.10f,
                        center.x + radius * 1.20f, center.y + radius * 0.30f
                    )
                }
                drawPath(orbitPath, tint, style = Stroke(width = strokeW * 0.8f, cap = StrokeCap.Round))
            }
        }
    }

    /**
     * 5. LOOP TEMPORALE INFINITO DEL GIORNO DELLA MARMOTTA (Lost Reel #02)
     * Due frecce sinuose a nastro Möbius che si rincorrono attorno a una clessidra segreta.
     */
    @Composable
    fun GroundhogDayLoop(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val r = (this.size.minDimension / 2f) * 0.80f

                // Freccia circolare superiore (senso orario)
                drawArc(
                    color = tint,
                    startAngle = 190f,
                    sweepAngle = 145f,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                // Punta freccia in alto a destra
                val arrow1 = Path().apply {
                    val tipX = center.x + (r * cos(Math.toRadians(335.0))).toFloat()
                    val tipY = center.y + (r * sin(Math.toRadians(335.0))).toFloat()
                    moveTo(tipX - r * 0.22f, tipY - r * 0.05f)
                    lineTo(tipX, tipY)
                    lineTo(tipX - r * 0.08f, tipY + r * 0.22f)
                }
                drawPath(arrow1, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Freccia circolare inferiore (senso orario)
                drawArc(
                    color = tint,
                    startAngle = 10f,
                    sweepAngle = 145f,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                // Punta freccia in basso a sinistra
                val arrow2 = Path().apply {
                    val tipX = center.x + (r * cos(Math.toRadians(155.0))).toFloat()
                    val tipY = center.y + (r * sin(Math.toRadians(155.0))).toFloat()
                    moveTo(tipX + r * 0.22f, tipY + r * 0.05f)
                    lineTo(tipX, tipY)
                    lineTo(tipX + r * 0.08f, tipY - r * 0.22f)
                }
                drawPath(arrow2, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Clessidra alchemica al centro
                val hgW = r * 0.38f
                val hgH = r * 0.52f
                val hgPath = Path().apply {
                    moveTo(center.x - hgW, center.y - hgH)
                    lineTo(center.x + hgW, center.y - hgH)
                    lineTo(center.x, center.y)
                    lineTo(center.x + hgW, center.y + hgH)
                    lineTo(center.x - hgW, center.y + hgH)
                    lineTo(center.x, center.y)
                    close()
                }
                drawPath(hgPath, tint, style = Stroke(width = strokeW * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }

    /**
     * 6. PRISMA DEL FATO & COSTELLEZIONE (Lost Reel #05 - Roulette del Fato / Shake)
     * Dado geometrico intagliato con scintille di sorpresa e raggio magico.
     */
    @Composable
    fun RouletteOfFate(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val s = (this.size.minDimension / 2f) * 0.75f

                // Dado isometrico in 3D (3 facce visibili)
                val topCenter = Offset(center.x, center.y - s * 0.25f)
                val topUp = Offset(center.x, topCenter.y - s * 0.70f)
                val topL = Offset(center.x - s * 0.75f, topCenter.y - s * 0.25f)
                val topR = Offset(center.x + s * 0.75f, topCenter.y - s * 0.25f)
                val botL = Offset(topL.x, topL.y + s * 0.85f)
                val botR = Offset(topR.x, topR.y + s * 0.85f)
                val botMid = Offset(center.x, topCenter.y + s * 0.85f)

                // Profilo esterno
                val cubePath = Path().apply {
                    moveTo(topUp.x, topUp.y)
                    lineTo(topR.x, topR.y)
                    lineTo(botR.x, botR.y)
                    lineTo(botMid.x, botMid.y)
                    lineTo(botL.x, botL.y)
                    lineTo(topL.x, topL.y)
                    close()
                }
                drawPath(cubePath, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Spigoli interni
                drawLine(tint, topCenter, topUp, strokeW)
                drawLine(tint, topCenter, topL, strokeW)
                drawLine(tint, topCenter, topR, strokeW)
                drawLine(tint, topCenter, botMid, strokeW)

                // Punti sulle facce (stile dado magico)
                drawCircle(tint, radius = 1.8.dp.toPx(), center = Offset(center.x, topCenter.y - s * 0.35f))
                drawCircle(tint, radius = 1.8.dp.toPx(), center = Offset(center.x - s * 0.38f, topCenter.y + s * 0.30f))
                drawCircle(tint, radius = 1.8.dp.toPx(), center = Offset(center.x + s * 0.38f, topCenter.y + s * 0.30f))

                // Scintille di sorpresa all'angolo in alto a destra
                drawSparkle(Offset(center.x + s * 0.85f, center.y - s * 0.80f), s * 0.28f, tint, strokeW)
            }
        }
    }

    /**
     * 7. CHIAVE DELL'ARCHIVIO SEGRETO (Lost Reel Generica / Mistero Bloccato)
     * Chiave antica la cui impugnatura è formata da una bobina di pellicola a 3 fori.
     */
    @Composable
    fun SecretKey(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val s = this.size.minDimension * 0.78f

                // Chiave inclinata a 45°
                val bowCenter = Offset(center.x - s * 0.28f, center.y - s * 0.28f)
                val bowRadius = s * 0.24f

                // Impugnatura a bobina
                drawCircle(tint, radius = bowRadius, center = bowCenter, style = Stroke(width = strokeW))
                drawCircle(tint, radius = bowRadius * 0.38f, center = bowCenter, style = Stroke(width = strokeW * 0.8f))

                // Asta della chiave verso il basso a destra
                val shaftEnd = Offset(center.x + s * 0.35f, center.y + s * 0.35f)
                val shaftStart = Offset(bowCenter.x + bowRadius * 0.707f, bowCenter.y + bowRadius * 0.707f)
                drawLine(tint, shaftStart, shaftEnd, strokeW)

                // Denti di cifratura della chiave
                val tooth1Start = Offset(shaftEnd.x - s * 0.15f, shaftEnd.y - s * 0.15f)
                val tooth1End = Offset(tooth1Start.x + s * 0.12f, tooth1Start.y - s * 0.12f)
                drawLine(tint, tooth1Start, tooth1End, strokeW)

                val tooth2Start = shaftEnd
                val tooth2End = Offset(tooth2Start.x + s * 0.16f, tooth2Start.y - s * 0.16f)
                drawLine(tint, tooth2Start, tooth2End, strokeW)
            }
        }
    }

    /**
     * 8. LUCCHETTO MINIMALISTA DI SICUREZZA (Stato Bloccato)
     */
    @Composable
    fun VaultLock(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val w = this.size.width * 0.65f
                val h = this.size.height * 0.52f
                val top = center.y - h * 0.10f

                // Corpo cassa
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(center.x - w / 2f, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                    style = Stroke(width = strokeW)
                )

                // Arco superiore
                val shackleR = w * 0.34f
                val shacklePath = Path().apply {
                    moveTo(center.x - shackleR, top)
                    lineTo(center.x - shackleR, top - shackleR * 1.1f)
                    arcTo(
                        rect = Rect(center.x - shackleR, top - shackleR * 1.8f, center.x + shackleR, top - shackleR * 0.4f),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false
                    )
                    lineTo(center.x + shackleR, top)
                }
                drawPath(shacklePath, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))

                // Foro chiave
                drawCircle(tint, radius = 2.5.dp.toPx(), center = Offset(center.x, top + h * 0.45f))
                drawLine(tint, Offset(center.x, top + h * 0.45f), Offset(center.x, top + h * 0.70f), strokeW)
            }
        }
    }

    /**
     * 9. ARCHIVIO CINETECA & SEGNALIBRO (Badge L'Archivio Infinito - Watchlist)
     * Tre faldoni di pellicole d'archivio sovrapposti con segnalibro cineteca pendente.
     */
    @Composable
    fun ArchiveStack(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val w = this.size.width * 0.72f
                val h = this.size.height * 0.68f
                val left = center.x - w / 2f
                val top = center.y - h / 2f

                // Cofanetto/Archivio esterno
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                    style = Stroke(width = strokeW)
                )

                // 3 Tramezzature verticali di custodie di pellicole
                val colW = w / 3f
                drawLine(tint, Offset(left + colW, top), Offset(left + colW, top + h), strokeW * 0.8f)
                drawLine(tint, Offset(left + colW * 2f, top), Offset(left + colW * 2f, top + h), strokeW * 0.8f)

                // Segnalibro / Nastro cineteca pendente dal primo scomparto
                val bmPath = Path().apply {
                    val bx = left + colW * 0.5f
                    moveTo(bx - 3.dp.toPx(), top)
                    lineTo(bx - 3.dp.toPx(), top + h * 0.55f)
                    lineTo(bx, top + h * 0.45f)
                    lineTo(bx + 3.dp.toPx(), top + h * 0.55f)
                    lineTo(bx + 3.dp.toPx(), top)
                }
                drawPath(bmPath, tint, style = Stroke(width = strokeW * 0.9f))

                // Cerchi perforati d'archivio sui faldoni 2 e 3
                drawCircle(tint, radius = 2.dp.toPx(), center = Offset(left + colW * 1.5f, top + h * 0.75f))
                drawCircle(tint, radius = 2.dp.toPx(), center = Offset(left + colW * 2.5f, top + h * 0.75f))
            }
        }
    }

    /**
     * 10. TRILOGIA DELLA SAGA (Badge Signore delle Saghe)
     * Tre bobine/anelli cinematografici concatenati a triangolo cosmico (la trilogia classica).
     */
    @Composable
    fun SagaTrilogy(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val ringR = (this.size.minDimension / 2f) * 0.36f
                val offsetDist = ringR * 0.85f

                // Centro dei tre anelli (in alto, in basso a sinistra, in basso a destra)
                val c1 = Offset(center.x, center.y - offsetDist * 0.95f)
                val c2 = Offset(center.x - offsetDist, center.y + offsetDist * 0.65f)
                val c3 = Offset(center.x + offsetDist, center.y + offsetDist * 0.65f)

                // Anello 1 (In alto)
                drawCircle(tint, radius = ringR, center = c1, style = Stroke(width = strokeW))
                drawCircle(tint, radius = ringR * 0.35f, center = c1, style = Stroke(width = strokeW * 0.8f))

                // Anello 2 (Basso sx)
                drawCircle(tint, radius = ringR, center = c2, style = Stroke(width = strokeW))
                drawCircle(tint, radius = ringR * 0.35f, center = c2, style = Stroke(width = strokeW * 0.8f))

                // Anello 3 (Basso dx)
                drawCircle(tint, radius = ringR, center = c3, style = Stroke(width = strokeW))
                drawCircle(tint, radius = ringR * 0.35f, center = c3, style = Stroke(width = strokeW * 0.8f))

                // Stella di completamento saga al centro del nodo trilogia
                drawStarShape(center, ringR * 0.30f, tint, strokeW)
            }
        }
    }

    /**
     * 11. TESSERA SOCIO CINECLUB (Badge Tessera del Cineclub - Fedeltà)
     * Tessera perforata ad ammissione esclusiva con stella cinefila e nastro vintage.
     */
    @Composable
    fun CineclubPass(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val w = this.size.width * 0.84f
                val h = this.size.height * 0.58f
                val left = center.x - w / 2f
                val top = center.y - h / 2f

                // Sagoma tessera / biglietto
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                    style = Stroke(width = strokeW)
                )

                // Tratteggio perforato laterale di controllo ingresso
                val lineX = left + w * 0.32f
                var dotY = top + 4.dp.toPx()
                while (dotY < top + h - 4.dp.toPx()) {
                    drawLine(tint, Offset(lineX, dotY), Offset(lineX, dotY + 3.dp.toPx()), strokeW * 0.8f)
                    dotY += 6.dp.toPx()
                }

                // Simbolo cinefilo d'onore a destra (stella e stemma)
                val badgeCenterX = left + w * 0.68f
                val badgeCenterY = center.y
                drawCircle(tint, radius = h * 0.34f, center = Offset(badgeCenterX, badgeCenterY), style = Stroke(width = strokeW * 0.9f))
                drawStarShape(Offset(badgeCenterX, badgeCenterY), h * 0.20f, tint, strokeW)
            }
        }
    }

    /**
     * 12. LUNA CRESCENTE DELLA NOTTE (Lost Reel #04 - Creatura della Notte)
     * Falce di luna notturna solcata da fori di pellicola cinematografica sotto le stelle.
     */
    @Composable
    fun NightOwlMoon(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val r = (this.size.minDimension / 2f) * 0.78f

                // Falce di luna elegante
                val moonPath = Path().apply {
                    moveTo(center.x, center.y - r)
                    arcTo(
                        rect = Rect(center.x - r, center.y - r, center.x + r, center.y + r),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false
                    )
                    arcTo(
                        rect = Rect(center.x - r * 0.4f, center.y - r, center.x + r * 1.6f, center.y + r),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    close()
                }
                drawPath(moonPath, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))

                // Stelle notturne attorno alla luna
                drawSparkle(Offset(center.x + r * 0.35f, center.y - r * 0.55f), r * 0.25f, tint, strokeW)
                drawSparkle(Offset(center.x + r * 0.65f, center.y + r * 0.15f), r * 0.18f, tint, strokeW)
                drawCircle(tint, radius = 1.5.dp.toPx(), center = Offset(center.x + r * 0.25f, center.y + r * 0.65f))
            }
        }
    }

    /**
     * 13. CIAK & STELLA DELLA GIURIA (Badge La Giuria - Valutazioni e Voti)
     * Ciak cinematografico aperto con stella di punteggio intagliata.
     */
    @Composable
    fun StarRating(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val w = this.size.width * 0.80f
                val h = this.size.height * 0.65f
                val left = center.x - w / 2f
                val top = center.y - h / 2f

                // Corpo ciak
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left, top + h * 0.28f),
                    size = Size(w, h * 0.72f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = strokeW)
                )

                // Barra superiore inclinata del ciak
                val clapperPath = Path().apply {
                    moveTo(left, top + h * 0.22f)
                    lineTo(left + w, top)
                    lineTo(left + w + 2.dp.toPx(), top + h * 0.18f)
                    lineTo(left + 2.dp.toPx(), top + h * 0.35f)
                    close()
                }
                drawPath(clapperPath, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Stella della giuria al centro
                drawStarShape(Offset(center.x, top + h * 0.64f), w * 0.18f, tint, strokeW)
            }
        }
    }

    /**
     * 14. SEDIA DA REGISTA (Badge Regista del Cuore / Collezione d'Autore)
     * Sedia d'epoca a X con braccioli, telo sagomato e poggiapiedi.
     */
    @Composable
    fun DirectorChair(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val w = this.size.width * 0.70f
                val h = this.size.height * 0.78f
                val left = center.x - w / 2f
                val top = center.y - h / 2f

                // Schienale in tela tesa
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left + w * 0.12f, top),
                    size = Size(w * 0.76f, h * 0.26f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = strokeW)
                )

                // Montanti verticali dello schienale
                drawLine(tint, Offset(left + w * 0.12f, top - 2.dp.toPx()), Offset(left + w * 0.12f, top + h * 0.44f), strokeW)
                drawLine(tint, Offset(left + w * 0.88f, top - 2.dp.toPx()), Offset(left + w * 0.88f, top + h * 0.44f), strokeW)

                // Seduta orizzontale
                drawLine(
                    color = tint,
                    start = Offset(left + w * 0.05f, top + h * 0.44f),
                    end = Offset(left + w * 0.95f, top + h * 0.44f),
                    strokeWidth = strokeW * 1.2f,
                    cap = StrokeCap.Round
                )

                // Braccioli orizzontali
                drawLine(tint, Offset(left + w * 0.02f, top + h * 0.28f), Offset(left + w * 0.24f, top + h * 0.28f), strokeW, cap = StrokeCap.Round)
                drawLine(tint, Offset(left + w * 0.76f, top + h * 0.28f), Offset(left + w * 0.98f, top + h * 0.28f), strokeW, cap = StrokeCap.Round)

                // Gambe incrociate a X
                drawLine(tint, Offset(left + w * 0.16f, top + h * 0.44f), Offset(left + w * 0.84f, top + h * 0.94f), strokeW, cap = StrokeCap.Round)
                drawLine(tint, Offset(left + w * 0.84f, top + h * 0.44f), Offset(left + w * 0.16f, top + h * 0.94f), strokeW, cap = StrokeCap.Round)

                // Poggiapiedi inferiore
                drawLine(tint, Offset(left + w * 0.24f, top + h * 0.82f), Offset(left + w * 0.76f, top + h * 0.82f), strokeW * 0.9f, cap = StrokeCap.Round)
            }
        }
    }

    /**
     * 15. MONUMENTO EPICO (Badge Titanico - Opere Oltre 3h30m)
     * Colonna trionfale classica con capitello dorico e alloro cinematografico.
     */
    @Composable
    fun ColossalMonument(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val w = this.size.width * 0.68f
                val h = this.size.height * 0.78f
                val left = center.x - w / 2f
                val top = center.y - h / 2f

                // Base a gradoni
                drawLine(tint, Offset(left, top + h), Offset(left + w, top + h), strokeW * 1.3f, cap = StrokeCap.Round)
                drawLine(tint, Offset(left + w * 0.10f, top + h * 0.90f), Offset(left + w * 0.90f, top + h * 0.90f), strokeW)

                // Capitello superiore
                drawLine(tint, Offset(left + w * 0.05f, top), Offset(left + w * 0.95f, top), strokeW * 1.3f, cap = StrokeCap.Round)
                drawLine(tint, Offset(left + w * 0.12f, top + h * 0.10f), Offset(left + w * 0.88f, top + h * 0.10f), strokeW)

                // Fusto della colonna con scanalature
                val colLeft = left + w * 0.22f
                val colRight = left + w * 0.78f
                drawLine(tint, Offset(colLeft, top + h * 0.10f), Offset(colLeft, top + h * 0.90f), strokeW)
                drawLine(tint, Offset(colRight, top + h * 0.10f), Offset(colRight, top + h * 0.90f), strokeW)

                // Scanalature verticali
                val colMid1 = left + w * 0.40f
                val colMid2 = left + w * 0.60f
                drawLine(tint, Offset(colMid1, top + h * 0.14f), Offset(colMid1, top + h * 0.86f), strokeW * 0.8f)
                drawLine(tint, Offset(colMid2, top + h * 0.14f), Offset(colMid2, top + h * 0.86f), strokeW * 0.8f)

                // Corona d'alloro o sole raggiante in alto al centro
                drawSparkle(Offset(center.x, top - 3.dp.toPx()), 4.dp.toPx(), tint, strokeW)
            }
        }
    }

    /**
     * 16. BOBINA CLOUD SYNC (Badge Il Trasloco / Integrazione Trakt & SIMKL)
     * Bobina classica con anello di sincronizzazione orbitale e frecce dinamiche.
     */
    @Composable
    fun SyncCloudReel(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val r = (this.size.minDimension / 2f) * 0.72f

                // Bobina centrale
                drawCircle(tint, radius = r * 0.55f, center = center, style = Stroke(width = strokeW))
                drawCircle(tint, radius = r * 0.20f, center = center)

                // 3 fori della bobina
                val holeDist = r * 0.36f
                for (i in 0 until 3) {
                    val angle = Math.toRadians(i * 120.0)
                    val hx = center.x + (holeDist * cos(angle)).toFloat()
                    val hy = center.y + (holeDist * sin(angle)).toFloat()
                    drawCircle(tint, radius = 2.dp.toPx(), center = Offset(hx, hy))
                }

                // Arco orbitale superiore con freccia
                val arcTop = Path().apply {
                    arcTo(
                        rect = Rect(center.x - r, center.y - r, center.x + r, center.y + r),
                        startAngleDegrees = 200f,
                        sweepAngleDegrees = 130f,
                        forceMoveTo = true
                    )
                }
                drawPath(arcTop, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))

                // Freccia superiore
                val arrowHeadTop = Path().apply {
                    val ax = center.x + (r * cos(Math.toRadians(330.0))).toFloat()
                    val ay = center.y + (r * sin(Math.toRadians(330.0))).toFloat()
                    moveTo(ax - 2.dp.toPx(), ay - 5.dp.toPx())
                    lineTo(ax + 2.dp.toPx(), ay)
                    lineTo(ax - 5.dp.toPx(), ay + 2.dp.toPx())
                }
                drawPath(arrowHeadTop, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Arco orbitale inferiore con freccia
                val arcBottom = Path().apply {
                    arcTo(
                        rect = Rect(center.x - r, center.y - r, center.x + r, center.y + r),
                        startAngleDegrees = 20f,
                        sweepAngleDegrees = 130f,
                        forceMoveTo = true
                    )
                }
                drawPath(arcBottom, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))

                // Freccia inferiore
                val arrowHeadBottom = Path().apply {
                    val bx = center.x + (r * cos(Math.toRadians(150.0))).toFloat()
                    val by = center.y + (r * sin(Math.toRadians(150.0))).toFloat()
                    moveTo(bx + 2.dp.toPx(), by + 5.dp.toPx())
                    lineTo(bx - 2.dp.toPx(), by)
                    lineTo(bx + 5.dp.toPx(), by - 2.dp.toPx())
                }
                drawPath(arrowHeadBottom, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }

    /**
     * 17. DOPPIO BIGLIETTO CINEMA (Badge Doppia Proiezione - Lost Reel #04)
     * Due biglietti intagliati a silhouette cinematografica d'epoca incrociati.
     */
    @Composable
    fun DoubleTicket(
        tint: Color,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.8.dp.toPx()
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val tw = this.size.width * 0.44f
                val th = this.size.height * 0.70f

                // Biglietto 1 (inclinato a sinistra di -15 gradi)
                // Disegniamo la sagoma con tacche laterali
                val notchR = 3.dp.toPx()
                val ticket1 = Path().apply {
                    moveTo(center.x - tw * 0.9f, center.y - th * 0.45f)
                    lineTo(center.x + tw * 0.1f, center.y - th * 0.45f)
                    lineTo(center.x + tw * 0.1f, center.y - notchR)
                    arcTo(
                        rect = Rect(center.x + tw * 0.1f - notchR, center.y - notchR, center.x + tw * 0.1f + notchR, center.y + notchR),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    lineTo(center.x + tw * 0.1f, center.y + th * 0.45f)
                    lineTo(center.x - tw * 0.9f, center.y + th * 0.45f)
                    lineTo(center.x - tw * 0.9f, center.y + notchR)
                    arcTo(
                        rect = Rect(center.x - tw * 0.9f - notchR, center.y - notchR, center.x - tw * 0.9f + notchR, center.y + notchR),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    close()
                }
                drawPath(ticket1, tint.copy(alpha = 0.55f), style = Stroke(width = strokeW))

                // Biglietto 2 (in primo piano, leggermente traslato a destra)
                val ticket2 = Path().apply {
                    moveTo(center.x - tw * 0.1f, center.y - th * 0.35f)
                    lineTo(center.x + tw * 0.9f, center.y - th * 0.35f)
                    lineTo(center.x + tw * 0.9f, center.y + 0.1f * th - notchR)
                    arcTo(
                        rect = Rect(center.x + tw * 0.9f - notchR, center.y + 0.1f * th - notchR, center.x + tw * 0.9f + notchR, center.y + 0.1f * th + notchR),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    lineTo(center.x + tw * 0.9f, center.y + th * 0.55f)
                    lineTo(center.x - tw * 0.1f, center.y + th * 0.55f)
                    lineTo(center.x - tw * 0.1f, center.y + 0.1f * th + notchR)
                    arcTo(
                        rect = Rect(center.x - tw * 0.1f - notchR, center.y + 0.1f * th - notchR, center.x - tw * 0.1f + notchR, center.y + 0.1f * th + notchR),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    close()
                }
                drawPath(ticket2, tint, style = Stroke(width = strokeW))

                // Linea tratteggiata di strappo nel biglietto 2
                val tearY = center.y + 0.1f * th
                drawLine(
                    color = tint,
                    start = Offset(center.x + tw * 0.05f, tearY),
                    end = Offset(center.x + tw * 0.75f, tearY),
                    strokeWidth = strokeW * 0.75f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )
            }
        }
    }

    // Helper per disegnare micro-stelle
    private fun DrawScope.drawStarShape(center: Offset, size: Float, color: Color, strokeWidth: Float) {
        val path = Path().apply {
            moveTo(center.x, center.y - size)
            quadraticTo(center.x, center.y, center.x + size, center.y)
            quadraticTo(center.x, center.y, center.x, center.y + size)
            quadraticTo(center.x, center.y, center.x - size, center.y)
            quadraticTo(center.x, center.y, center.x, center.y - size)
            close()
        }
        drawPath(path, color, style = Stroke(width = strokeWidth * 0.8f))
    }

    // Helper per disegnare scintille
    private fun DrawScope.drawSparkle(center: Offset, size: Float, color: Color, strokeWidth: Float) {
        drawLine(color, Offset(center.x - size, center.y), Offset(center.x + size, center.y), strokeWidth * 0.8f)
        drawLine(color, Offset(center.x, center.y - size), Offset(center.x, center.y + size), strokeWidth * 0.8f)
    }
}
