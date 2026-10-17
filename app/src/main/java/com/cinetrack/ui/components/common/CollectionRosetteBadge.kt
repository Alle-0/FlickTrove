package com.cinetrack.ui.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Award Rosette Ribbon Badge (Coccarda di Premiazione) for completed collections/sagas.
 * Features realistic 3D satin pleated ruffles, twin ribbon tails with swallowtail notches,
 * a central medallion with gold/accent concentric rings, and [R.drawable.ic_star_piena] at the center.
 */
@Composable
fun CollectionRosetteBadge(
    accentColor: Color,
    modifier: Modifier = Modifier,
    width: Dp = 34.dp,
    height: Dp = 46.dp,
    starSize: Dp = 14.dp,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier.size(width, height),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val cx = w / 2f
            val cy = w / 2f
            val rOuter = (w / 2f) - 1.5f.dp.toPx()
            val rInner = rOuter * 0.58f
            val rDisc = rOuter * 0.60f

            // --- 1. Ribbon Tails (Nastri pendenti) ---
            val ribbonTailDrop = h
            val notchDepth = w * 0.14f
            val rw = w * 0.28f

            // Shadow for ribbons
            val leftRibbonShadow = Path().apply {
                moveTo(cx - rw * 0.1f, cy + rInner * 0.5f)
                lineTo(cx - w * 0.38f, ribbonTailDrop)
                lineTo(cx - w * 0.24f, ribbonTailDrop - notchDepth)
                lineTo(cx - w * 0.10f, ribbonTailDrop)
                lineTo(cx + rw * 0.4f, cy + rInner * 0.5f)
                close()
            }
            val rightRibbonShadow = Path().apply {
                moveTo(cx - rw * 0.4f, cy + rInner * 0.5f)
                lineTo(cx + w * 0.10f, ribbonTailDrop)
                lineTo(cx + w * 0.24f, ribbonTailDrop - notchDepth)
                lineTo(cx + w * 0.38f, ribbonTailDrop)
                lineTo(cx + rw * 0.1f, cy + rInner * 0.5f)
                close()
            }
            drawPath(leftRibbonShadow, color = Color.Black.copy(alpha = 0.35f))
            drawPath(rightRibbonShadow, color = Color.Black.copy(alpha = 0.35f))

            // Left ribbon
            val leftRibbon = Path().apply {
                moveTo(cx - rw * 0.1f, cy + rInner * 0.5f)
                lineTo(cx - w * 0.38f, ribbonTailDrop)
                lineTo(cx - w * 0.24f, ribbonTailDrop - notchDepth)
                lineTo(cx - w * 0.10f, ribbonTailDrop)
                lineTo(cx + rw * 0.4f, cy + rInner * 0.5f)
                close()
            }
            drawPath(
                path = leftRibbon,
                color = accentColor.copy(alpha = 0.85f),
                style = Fill
            )
            // Left ribbon highlight fold
            drawPath(
                path = leftRibbon,
                color = Color.Black.copy(alpha = 0.18f),
                style = Fill
            )

            // Right ribbon (slight highlight over left)
            val rightRibbon = Path().apply {
                moveTo(cx - rw * 0.4f, cy + rInner * 0.5f)
                lineTo(cx + w * 0.10f, ribbonTailDrop)
                lineTo(cx + w * 0.24f, ribbonTailDrop - notchDepth)
                lineTo(cx + w * 0.38f, ribbonTailDrop)
                lineTo(cx + rw * 0.1f, cy + rInner * 0.5f)
                close()
            }
            drawPath(
                path = rightRibbon,
                color = accentColor.copy(alpha = 0.95f),
                style = Fill
            )
            drawPath(
                path = rightRibbon,
                color = Color.White.copy(alpha = 0.08f),
                style = Fill
            )

            // --- 2. Rosette Head Drop Shadow ---
            drawCircle(
                color = Color.Black.copy(alpha = 0.42f),
                radius = rOuter + 1.5f.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(cx, cy + 1.5f.dp.toPx())
            )

            // --- 3. Pleated Ruffles (Coccarda pieghettata satinata) ---
            val pleats = 20
            val angleStep = (2 * PI / pleats).toFloat()

            for (i in 0 until pleats) {
                val a0 = i * angleStep
                val aMid = a0 + angleStep / 2f
                val a1 = a0 + angleStep

                val crestX = cx + rOuter * cos(aMid)
                val crestY = cy + rOuter * sin(aMid)

                val val0X = cx + (rOuter * 0.91f) * cos(a0)
                val val0Y = cy + (rOuter * 0.91f) * sin(a0)

                val val1X = cx + (rOuter * 0.91f) * cos(a1)
                val val1Y = cy + (rOuter * 0.91f) * sin(a1)

                // Left facet (highlight facet)
                val facetA = Path().apply {
                    moveTo(cx, cy)
                    lineTo(val0X, val0Y)
                    lineTo(crestX, crestY)
                    close()
                }
                drawPath(facetA, color = accentColor.copy(alpha = 0.98f))
                drawPath(facetA, color = Color.White.copy(alpha = 0.22f))

                // Right facet (shadow facet)
                val facetB = Path().apply {
                    moveTo(cx, cy)
                    lineTo(crestX, crestY)
                    lineTo(val1X, val1Y)
                    close()
                }
                drawPath(facetB, color = accentColor.copy(alpha = 0.68f))
                drawPath(facetB, color = Color.Black.copy(alpha = 0.24f))
            }

            // Outer delicate contour
            drawCircle(
                color = accentColor.copy(alpha = 0.5f),
                radius = rOuter,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style = Stroke(width = 0.75f.dp.toPx())
            )

            // --- 4. Center Medallion Disc ---
            // Dark elegant background disc
            drawCircle(
                color = Color(0xFF0C0C14).copy(alpha = 0.95f),
                radius = rDisc,
                center = androidx.compose.ui.geometry.Offset(cx, cy)
            )

            // Outer gold/accent rim on disc
            drawCircle(
                color = accentColor.copy(alpha = 0.92f),
                radius = rDisc - 0.75f.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style = Stroke(width = 1.25f.dp.toPx())
            )

            // Concentric delicate inner gold/accent ring
            drawCircle(
                color = accentColor.copy(alpha = 0.45f),
                radius = rDisc - 2.8f.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style = Stroke(width = 0.6f.dp.toPx())
            )
        }

        // --- 5. Center Star (ic_star_piena) ---
        // Exactly centered over the rosette head disc (which has center at (width/2, width/2))
        Box(
            modifier = Modifier
                .size(width)
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_star_piena),
                contentDescription = contentDescription,
                tint = accentColor,
                modifier = Modifier.size(starSize)
            )
        }
    }
}
