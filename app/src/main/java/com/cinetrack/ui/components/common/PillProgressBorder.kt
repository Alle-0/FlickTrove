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
    strokeWidth: Dp = 2.dp
) {
    val pathMeasure = remember { PathMeasure() }
    val canvasModifier = if (width != Dp.Unspecified && height != Dp.Unspecified) {
        modifier.size(width, height)
    } else {
        modifier
    }

    Canvas(modifier = canvasModifier) {
        val strokeWidthPx = strokeWidth.toPx()
        val radius = (size.height / 2f) - (strokeWidthPx / 2f)
        val w = size.width
        val h = size.height

        // Create the full pill path
        val fullPath = Path().apply {
            // Start from middle-top to follow a natural clockwise movement
            moveTo(radius + strokeWidthPx / 2f, strokeWidthPx / 2f)
            lineTo(w - radius - strokeWidthPx / 2f, strokeWidthPx / 2f)
            arcTo(
                rect = Rect(w - 2 * radius - strokeWidthPx / 2f, strokeWidthPx / 2f, w - strokeWidthPx / 2f, h - strokeWidthPx / 2f),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(radius + strokeWidthPx / 2f, h - strokeWidthPx / 2f)
            arcTo(
                rect = Rect(strokeWidthPx / 2f, strokeWidthPx / 2f, 2 * radius + strokeWidthPx / 2f, h - strokeWidthPx / 2f),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            close()
        }

        // Draw background track
        drawPath(
            path = fullPath,
            color = Color.White.copy(alpha = 0.2f),
            style = Stroke(width = strokeWidthPx)
        )

        if (progress > 0f) {
            pathMeasure.setPath(fullPath, false)
            val pathLength = pathMeasure.length
            val segmentPath = Path()
            pathMeasure.getSegment(0f, pathLength * progress, segmentPath, true)

            drawPath(
                path = segmentPath,
                color = color,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
    }
}
