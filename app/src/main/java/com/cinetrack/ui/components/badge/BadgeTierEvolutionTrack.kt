package com.cinetrack.ui.components.badge

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R

// 2. TIMELINE EVOLUTIVA DEI 5 TIERS (SPUNTA TICK SU TUTTI I NODI CONQUISTATI)
// ════════════════════════════════════════════════════════════════════

@Composable
fun BadgeTierEvolutionTrack(
    currentTier: PreviewBadgeTier,
    isUnlocked: Boolean = true,
    progressCurrent: Int = 0,
    maxTarget: Int = 0,
    stepProgressFraction: Float = 0f,
    tierSteps: List<PreviewBadgeTier> = listOf(
        PreviewBadgeTier.SUPER_8,
        PreviewBadgeTier.MM_16,
        PreviewBadgeTier.MM_35,
        PreviewBadgeTier.MM_70,
        PreviewBadgeTier.THE_FINAL_CUT
    ),
    modifier: Modifier = Modifier
) {
    val currentOrdinal = tierSteps.indexOf(currentTier).takeIf { it >= 0 } ?: 0
    val isCompletedAll = isUnlocked && currentOrdinal == tierSteps.lastIndex

    val animatedStepProgress by animateFloatAsState(
        targetValue = stepProgressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "StepLineProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        // Intestazione con percentuale sul massimo o badge COMPLETATO
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.trophy_track_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = Color.White.copy(alpha = 0.50f)
            )

            if (isCompletedAll) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(currentTier.primaryColor.copy(alpha = 0.18f))
                        .border(1.dp, currentTier.primaryColor, CircleShape)
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.trophy_completed_badge),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        ),
                        color = currentTier.primaryColor
                    )
                }
            } else {
                val percent = if (maxTarget > 0) {
                    ((progressCurrent.toFloat() / maxTarget) * 100).toInt().coerceIn(0, 100)
                } else {
                    if (isUnlocked) (((currentOrdinal + 1) / tierSteps.size.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = if (isUnlocked) currentTier.primaryColor else Color.White.copy(alpha = 0.50f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // STRUTTURA ATOMICA CON ALLINEAMENTO PERFETTO NODI-TESTI
        val stepCount = tierSteps.size
        Box(modifier = Modifier.fillMaxWidth()) {
            if (stepCount > 1) {
                // Linee di connessione disegnate rigorosamente tra i bordi esterni dei nodi (senza attraversare l'interno dei cerchi)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    val colW = size.width / stepCount.toFloat()
                    val strokeH = 2.5.dp.toPx()
                    val y = size.height / 2f
                    val capRadius = strokeH / 2f
                    val clearance = 1.dp.toPx()

                    for (i in 0 until (stepCount - 1)) {
                        val x1 = colW * (i + 0.5f)
                        val x2 = colW * (i + 1.5f)

                        val r1 = if (isUnlocked && i == currentOrdinal) 12.dp.toPx() else 8.dp.toPx()
                        val r2 = if (isUnlocked && (i + 1) == currentOrdinal) 12.dp.toPx() else 8.dp.toPx()

                        // Calcolo dei punti di arresto esterni: la linea non penetra mai all'interno del cerchio
                        val startX = x1 + r1 + clearance + capRadius
                        val endX = x2 - r2 - clearance - capRadius

                        if (startX < endX) {
                            // 1. Traccia satinata di sfondo tra i nodi
                            drawLine(
                                color = Color.White.copy(alpha = 0.12f),
                                start = Offset(startX, y),
                                end = Offset(endX, y),
                                strokeWidth = strokeH,
                                cap = StrokeCap.Round
                            )

                            // 2. Tratto colorato in proporzione all'avanzamento
                            val segmentFraction = when {
                                !isUnlocked -> 0f
                                isCompletedAll || i < currentOrdinal -> 1f
                                i == currentOrdinal -> animatedStepProgress
                                else -> 0f
                            }

                            if (segmentFraction > 0f) {
                                val activeX2 = startX + (endX - startX) * segmentFraction
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        listOf(tierSteps[i].primaryColor, tierSteps[i + 1].primaryColor),
                                        startX = startX,
                                        endX = endX
                                    ),
                                    start = Offset(startX, y),
                                    end = Offset(activeX2, y),
                                    strokeWidth = strokeH,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }

            // Colonne centrate: ciascuna contiene il nodo e la sua etichetta sullo stesso asse verticale X
            Row(modifier = Modifier.fillMaxWidth()) {
                tierSteps.forEachIndexed { index, stepTier ->
                    val isNodeAchieved = isUnlocked && index <= currentOrdinal
                    val isCurrent = isUnlocked && index == currentOrdinal

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Nodo da 24.dp
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 24.dp else 16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isNodeAchieved -> stepTier.primaryColor
                                            else -> Color(0xFF141722)
                                        }
                                    )
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = when {
                                            isCurrent -> Color.White
                                            isNodeAchieved -> stepTier.primaryColor
                                            else -> Color.White.copy(alpha = 0.18f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isNodeAchieved) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick),
                                        contentDescription = null,
                                        tint = if (stepTier == PreviewBadgeTier.THE_FINAL_CUT) Color.White else Color.Black,
                                        modifier = Modifier.size(if (isCurrent) 11.dp else 9.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Etichetta del formato rigorosamente centrata sotto il proprio cerchio
                        Text(
                            text = stepTier.shortLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.8.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            ),
                            color = when {
                                isCurrent -> stepTier.primaryColor
                                isNodeAchieved -> Color.White.copy(alpha = 0.90f)
                                else -> Color.White.copy(alpha = 0.40f)
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
