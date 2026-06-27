package com.cinetrack.ui.assets

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {
    val PremiumBellFilled: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumBellFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd
        ) {
            // Main Bell Body (Filled Icons8 Style - Wider)
            moveTo(12f, 4f)
            curveTo(8f, 4f, 6f, 6f, 6f, 9f)
            verticalLineTo(12f)
            curveTo(6f, 15f, 3f, 17f, 3f, 18f)
            horizontalLineTo(21f)
            curveTo(21f, 17f, 18f, 15f, 18f, 12f)
            verticalLineTo(9f)
            curveTo(18f, 6f, 16f, 4f, 12f, 4f)
            close()
            
            // Top Loop
            moveTo(10f, 4f)
            curveTo(10f, 2.5f, 11f, 2f, 12f, 2f)
            curveTo(13f, 2f, 14f, 2.5f, 14f, 4f)
            
            // Clapper
            moveTo(10f, 20f)
            curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f)
            curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f)
        }.build()

    val PremiumCheck: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 3.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 12f)
            lineTo(10f, 17f)
            lineTo(19f, 8f)
        }.build()
}
