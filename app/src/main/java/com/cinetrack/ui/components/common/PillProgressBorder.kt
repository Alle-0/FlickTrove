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
    width: Dp,
    height: Dp,
    progress: Float, // 0.0 to 1.0
    color: Color,
    modifier: Modifier = Modifier
) {
    val pathMeasure = remember { PathMeasure() }
    
    Canvas(modifier = modifier.size(width, height)) {
        val strokeWidth = 3.dp.toPx()
        val radius = (height.toPx() / 2) - (strokeWidth / 2)
        val w = width.toPx()
        val h = height.toPx()

        // Create the full pill path
        val fullPath = Path().apply {
            // Start from middle-top to follow a natural clockwise movement
            moveTo(radius + strokeWidth / 2, strokeWidth / 2)
            lineTo(w - radius - strokeWidth / 2, strokeWidth / 2)
            arcTo(
                rect = Rect(w - 2 * radius - strokeWidth / 2, strokeWidth / 2, w - strokeWidth / 2, h - strokeWidth / 2),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(radius + strokeWidth / 2, h - strokeWidth / 2)
            arcTo(
                rect = Rect(strokeWidth / 2, strokeWidth / 2, 2 * radius + strokeWidth / 2, h - strokeWidth / 2),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            close()
        }

        // Draw background track
        drawPath(
            path = fullPath,
            color = Color.White.copy(alpha = 0.05f),
            style = Stroke(width = strokeWidth)
        )

        if (progress > 0f) {
            pathMeasure.setPath(fullPath, false)
            val pathLength = pathMeasure.length
            val segmentPath = Path()
            pathMeasure.getSegment(0f, pathLength * progress, segmentPath, true)

            drawPath(
                path = segmentPath,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}
