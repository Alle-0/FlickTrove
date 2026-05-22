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
    val PremiumBell: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumBell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Main Bell Body (Icons8 Style - Wider)
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

    val PremiumAdd: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumAdd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 3.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 6f)
            lineTo(12f, 18f)
            moveTo(6f, 12f)
            horizontalLineTo(18f)
        }.build()

    val PremiumSearch: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumSearch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(11f, 17f)
            curveTo(14.3137f, 17f, 17f, 14.3137f, 17f, 11f)
            curveTo(17f, 7.68629f, 14.3137f, 5f, 11f, 5f)
            curveTo(7.68629f, 5f, 5f, 7.68629f, 5f, 11f)
            curveTo(5f, 14.3137f, 7.68629f, 17f, 11f, 17f)
            close()
            moveTo(15.35f, 15.35f)
            lineTo(19.5f, 19.5f)
        }.build()

    val PremiumFilmStrip: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumFilmStrip",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd
        ) {
            // Film strip outer frame
            moveTo(4f, 4f)
            verticalLineTo(20f)
            horizontalLineTo(20f)
            verticalLineTo(4f)
            horizontalLineTo(4f)
            close()

            // Punch holes - Top
            moveTo(6f, 6f)
            horizontalLineTo(8f)
            verticalLineTo(8f)
            horizontalLineTo(6f)
            verticalLineTo(6f)
            close()
            moveTo(11f, 6f)
            horizontalLineTo(13f)
            verticalLineTo(8f)
            horizontalLineTo(11f)
            verticalLineTo(6f)
            close()
            moveTo(16f, 6f)
            horizontalLineTo(18f)
            verticalLineTo(8f)
            horizontalLineTo(16f)
            verticalLineTo(6f)
            close()

            // Punch holes - Bottom
            moveTo(6f, 16f)
            horizontalLineTo(8f)
            verticalLineTo(18f)
            horizontalLineTo(6f)
            verticalLineTo(16f)
            close()
            moveTo(11f, 16f)
            horizontalLineTo(13f)
            verticalLineTo(18f)
            horizontalLineTo(11f)
            verticalLineTo(16f)
            close()
            moveTo(16f, 16f)
            horizontalLineTo(18f)
            verticalLineTo(18f)
            horizontalLineTo(16f)
            verticalLineTo(16f)
            close()
        }.build()

    val PremiumEditNote: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumEditNote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 10f)
            horizontalLineTo(12f)
            moveTo(3f, 6f)
            horizontalLineTo(12f)
            moveTo(3f, 14f)
            horizontalLineTo(8f)
            // Pencil
            moveTo(13.5f, 18.5f)
            lineTo(19f, 13f)
            curveTo(19.5f, 12.5f, 19.5f, 11.5f, 19f, 11f)
            curveTo(18.5f, 10.5f, 17.5f, 10.5f, 17f, 11f)
            lineTo(11.5f, 16.5f)
            verticalLineTo(18.5f)
            horizontalLineTo(13.5f)
        }.build()

    val PremiumFolder: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumFolder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 7f)
            curveTo(3f, 5.89543f, 3.89543f, 5f, 5f, 5f)
            horizontalLineTo(9.5f)
            lineTo(11.5f, 7f)
            horizontalLineTo(19f)
            curveTo(20.1046f, 7f, 21f, 7.89543f, 21f, 9f)
            verticalLineTo(17f)
            curveTo(21f, 18.1046f, 20.1046f, 19f, 19f, 19f)
            horizontalLineTo(5f)
            curveTo(3.89543f, 19f, 3f, 18.1046f, 3f, 17f)
            verticalLineTo(7f)
            close()
        }.build()

    val PremiumDelete: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumDelete",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 6f)
            horizontalLineTo(21f)
            moveTo(19f, 6f)
            verticalLineTo(19f)
            curveTo(19f, 20.1046f, 18.1046f, 21f, 17f, 21f)
            horizontalLineTo(7f)
            curveTo(5.89543f, 21f, 5f, 20.1046f, 5f, 19f)
            verticalLineTo(6f)
            moveTo(8f, 6f)
            verticalLineTo(4f)
            curveTo(8f, 2.89543f, 8.89543f, 2f, 10f, 2f)
            horizontalLineTo(14f)
            curveTo(15.1046f, 2f, 16f, 2.89543f, 16f, 4f)
            verticalLineTo(6f)
            moveTo(10f, 11f)
            verticalLineTo(16f)
            moveTo(14f, 11f)
            verticalLineTo(16f)
        }.build()

    val PremiumDetails: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumDetails",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 12f)
            verticalLineTo(16f)
            moveTo(12f, 8f)
            horizontalLineTo(12.01f)
            moveTo(12f, 22f)
            curveTo(17.5228f, 22f, 22f, 17.5228f, 22f, 12f)
            curveTo(22f, 6.47715f, 17.5228f, 2f, 12f, 2f)
            curveTo(6.47715f, 2f, 2f, 6.47715f, 2f, 12f)
            curveTo(2f, 17.5228f, 6.47715f, 22f, 12f, 22f)
            close()
        }.build()

    val PremiumTrailer: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumTrailer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7f, 17.27f)
            lineTo(18f, 12f)
            lineTo(7f, 6.73f)
            verticalLineTo(17.27f)
            close()
        }.build()

    val PremiumStar: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumStar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 17.27f)
            lineTo(18.18f, 21f)
            lineTo(16.54f, 13.97f)
            lineTo(22f, 9.24f)
            lineTo(14.81f, 8.63f)
            lineTo(12f, 2f)
            lineTo(9.19f, 8.63f)
            lineTo(2f, 9.24f)
            lineTo(7.46f, 13.97f)
            lineTo(5.82f, 21f)
            lineTo(12f, 17.27f)
            close()
        }.build()

    val PremiumStarFilled: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumStarFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(12f, 17.27f)
            lineTo(18.18f, 21f)
            lineTo(16.54f, 13.97f)
            lineTo(22f, 9.24f)
            lineTo(14.81f, 8.63f)
            lineTo(12f, 2f)
            lineTo(9.19f, 8.63f)
            lineTo(2f, 9.24f)
            lineTo(7.46f, 13.97f)
            lineTo(5.82f, 21f)
            lineTo(12f, 17.27f)
            close()
        }.build()

    val PremiumShare: ImageVector
        get() = ImageVector.Builder(
            name = "PremiumShare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(18f, 8f)
            curveTo(19.6569f, 8f, 21f, 6.65685f, 21f, 5f)
            curveTo(21f, 3.34315f, 19.6569f, 2f, 18f, 2f)
            curveTo(16.3431f, 2f, 15f, 3.34315f, 15f, 5f)
            curveTo(15f, 5.12547f, 15.0077f, 5.24911f, 15.0227f, 5.37061f)
            lineTo(8.08261f, 8.84066f)
            curveTo(7.46218f, 8.31264f, 6.66179f, 8f, 5.78571f, 8f)
            curveTo(4.12886f, 8f, 2.78571f, 9.34315f, 2.78571f, 11f)
            curveTo(2.78571f, 12.6569f, 4.12886f, 14f, 5.78571f, 14f)
            curveTo(6.66179f, 14f, 7.46218f, 13.6874f, 8.08261f, 13.1593f)
            lineTo(15.0227f, 16.6294f)
            curveTo(15.0077f, 16.7509f, 15f, 16.8745f, 15f, 17f)
            curveTo(15f, 18.6569f, 16.3431f, 20f, 18f, 20f)
            curveTo(19.6569f, 20f, 21f, 18.6569f, 21f, 17f)
            curveTo(21f, 15.3431f, 19.6569f, 14f, 18f, 14f)
            curveTo(17.1239f, 14f, 16.3235f, 14.3126f, 15.7031f, 14.8407f)
            lineTo(8.76295f, 11.3706f)
            curveTo(8.77793f, 11.2491f, 8.78571f, 11.1255f, 8.78571f, 11f)
            curveTo(8.78571f, 10.8745f, 8.77793f, 10.7509f, 8.76295f, 10.6294f)
            lineTo(15.7031f, 7.15934f)
            curveTo(16.3235f, 7.68736f, 17.1239f, 8f, 18f, 8f)
            close()
        }.build()
}
