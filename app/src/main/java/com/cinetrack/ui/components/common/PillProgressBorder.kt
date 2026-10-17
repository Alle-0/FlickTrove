package com.cinetrack.ui.components.common
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PillProgressBorder(
    progress: Float, // 0.0 to 1.0
    color: Color,
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    strokeWidth: Dp = 1.5.dp,
    trackColor: Color = Color.White.copy(alpha = 0.35f)
) {
    val pathMeasure = remember { PathMeasure() }
    val canvasModifier = if (width != Dp.Unspecified && height != Dp.Unspecified) {
        modifier.size(width, height)
    } else {
        modifier
    }

    Canvas(modifier = canvasModifier) {
        val strokeWidthPx = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        if (w < 1f || h < 1f) return@Canvas

        val halfStroke = strokeWidthPx / 2f
        val radius = (h / 2f) - halfStroke
        val safeRadius = kotlin.math.min(radius, (w / 2f) - halfStroke).coerceAtLeast(0f)

        // Create the full pill path starting from the leftmost point (middle-left) to match series detail progress
        val fullPath = Path().apply {
            moveTo(halfStroke, h / 2f)
            // Left cap top quadrant: sweeps 90° clockwise from 180° up to 270° (top)
            arcTo(
                rect = Rect(halfStroke, halfStroke, halfStroke + 2 * safeRadius, h - halfStroke),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Top straight edge: left to right
            lineTo(w - halfStroke - safeRadius, halfStroke)
            // Right cap: sweeps 180° clockwise from -90° (270°) around to 90° (bottom)
            arcTo(
                rect = Rect(w - halfStroke - 2 * safeRadius, halfStroke, w - halfStroke, h - halfStroke),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            // Bottom straight edge: right to left
            lineTo(halfStroke + safeRadius, h - halfStroke)
            // Left cap bottom quadrant: sweeps 90° clockwise from 90° back to 180° (middle-left)
            arcTo(
                rect = Rect(halfStroke, halfStroke, halfStroke + 2 * safeRadius, h - halfStroke),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }

        // Draw background track
        drawPath(
            path = fullPath,
            color = trackColor,
            style = Stroke(width = strokeWidthPx)
        )

        if (progress > 0f) {
            pathMeasure.setPath(fullPath, false)
            val pathLength = pathMeasure.length
            val segmentPath = Path()
            pathMeasure.getSegment(0f, pathLength * progress.coerceIn(0f, 1f), segmentPath, true)

            drawPath(
                path = segmentPath,
                color = color,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
    }
}
