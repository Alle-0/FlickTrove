package com.cinetrack.ui.components.halloween

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural Spiderweb corner with an optional hanging spider.
 * Designed to sit in the top corner of the screen without intercepting touches.
 */
@Composable
fun SpiderwebCorner(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 110.dp,
    webColor: Color = Color.White.copy(alpha = 0.38f),
    showSpider: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spiderSwing")
    val spiderSwingAngle by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spiderAngle"
    )

    Canvas(
        modifier = modifier
            .size(sizeDp)
    ) {
        val w = size.width
        val h = size.height

        // Draw radial anchor threads emanating from corner (0, 0)
        val numRays = 5
        val rayAngleStep = (Math.PI / 2.0) / (numRays - 1)
        val maxRadius = w.coerceAtLeast(h)

        val rayEndPoints = mutableListOf<Offset>()
        for (i in 0 until numRays) {
            val angle = i * rayAngleStep
            val rx = (cos(angle) * maxRadius).toFloat()
            val ry = (sin(angle) * maxRadius).toFloat()
            rayEndPoints.add(Offset(rx, ry))
            drawLine(
                color = webColor,
                start = Offset.Zero,
                end = Offset(rx, ry),
                strokeWidth = 1.3f
            )
        }

        // Draw inward-curving concentric web strands
        val numStrands = 5
        for (s in 1..numStrands) {
            val fraction = (s.toFloat() / numStrands) * 0.92f
            val path = Path()
            val firstPt = Offset(rayEndPoints[0].x * fraction, rayEndPoints[0].y * fraction)
            path.moveTo(firstPt.x, firstPt.y)

            for (i in 1 until numRays) {
                val prevPt = Offset(rayEndPoints[i - 1].x * fraction, rayEndPoints[i - 1].y * fraction)
                val currPt = Offset(rayEndPoints[i].x * fraction, rayEndPoints[i].y * fraction)

                // Control point pulls slightly toward the corner (0,0) for classic sagging curve
                val midX = (prevPt.x + currPt.x) * 0.5f * 0.88f
                val midY = (prevPt.y + currPt.y) * 0.5f * 0.88f

                path.quadraticTo(midX, midY, currPt.x, currPt.y)
            }

            drawPath(
                path = path,
                color = webColor,
                style = Stroke(width = 1.1f, cap = StrokeCap.Round)
            )
        }

        // Hanging spider on a dangling thread
        if (showSpider) {
            val threadAnchorX = w * 0.62f
            val threadAnchorY = h * 0.22f
            val threadLength = h * 0.48f

            val swingRad = Math.toRadians(spiderSwingAngle.toDouble())
            val spiderX = threadAnchorX + (sin(swingRad) * threadLength).toFloat()
            val spiderY = threadAnchorY + (cos(swingRad) * threadLength).toFloat()

            // Silk thread
            drawLine(
                color = webColor.copy(alpha = 0.55f),
                start = Offset(threadAnchorX, threadAnchorY),
                end = Offset(spiderX, spiderY),
                strokeWidth = 1f
            )

            // Spider Body
            drawCircle(
                color = Color(0xFF18151D),
                radius = 5.5f,
                center = Offset(spiderX, spiderY)
            )
            drawCircle(
                color = Color(0xFFFF6D00), // Tiny glowing orange spider abdomen spot
                radius = 1.8f,
                center = Offset(spiderX, spiderY)
            )
            // Spider head
            drawCircle(
                color = Color(0xFF231F2A),
                radius = 3.2f,
                center = Offset(spiderX, spiderY + 4.5f)
            )

            // Spider legs
            val legColor = Color(0xFF231F2A)
            val legLength = 7.5f
            for (side in listOf(-1f, 1f)) {
                for (legIdx in 0..3) {
                    val angleOffset = (legIdx - 1.5f) * 0.35f
                    val bendX = spiderX + (side * 6f)
                    val bendY = spiderY + (angleOffset * 8f)
                    val tipX = spiderX + (side * (6f + legLength))
                    val tipY = bendY + (side * 2.5f)

                    drawLine(color = legColor, start = Offset(spiderX, spiderY), end = Offset(bendX, bendY), strokeWidth = 1f)
                    drawLine(color = legColor, start = Offset(bendX, bendY), end = Offset(tipX, tipY), strokeWidth = 0.9f)
                }
            }
        }
    }
}

/**
 * Organic dripping blood overlay hanging from a horizontal boundary (e.g. bottom of header or top of screen).
 */
@Composable
fun BloodDripHeader(
    modifier: Modifier = Modifier,
    heightDp: Dp = 32.dp,
    dripColorDark: Color = Color(0xFF7A0000),
    dripColorLight: Color = Color(0xFFBA0C0C)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bloodPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bloodGlow"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
    ) {
        val w = size.width
        val h = size.height

        val dripBrush = Brush.verticalGradient(
            0.0f to dripColorDark,
            0.65f to dripColorLight,
            1.0f to Color(0xFFE50914)
        )

        val path = Path()
        val baseH = 3.5f

        path.moveTo(0f, 0f)
        path.lineTo(0f, baseH)

        val dripInterval = 28f
        val numDrips = (w / dripInterval).toInt() + 1

        for (i in 0..numDrips) {
            val dripX = (i + 0.5f) * dripInterval
            if (dripX - 6f > w) break

            val dripLength = when (i % 7) {
                0 -> h * 0.88f
                1 -> h * 0.32f
                2 -> h * 0.68f
                3 -> h * 0.20f
                4 -> h * 0.96f
                5 -> h * 0.44f
                else -> h * 0.56f
            }
            val dripRadius = 3.2f + (i % 3) * 1.3f

            path.lineTo((dripX - dripRadius).coerceAtLeast(0f), baseH)
            path.lineTo((dripX - dripRadius).coerceAtLeast(0f), (dripLength - dripRadius).coerceAtLeast(baseH))
            path.quadraticTo(
                dripX,
                dripLength + dripRadius,
                (dripX + dripRadius).coerceAtMost(w),
                (dripLength - dripRadius).coerceAtLeast(baseH)
            )
            path.lineTo((dripX + dripRadius).coerceAtMost(w), baseH)
        }

        path.lineTo(w, baseH)
        path.lineTo(w, 0f)
        path.close()

        // Draw main blood body
        drawPath(
            path = path,
            brush = dripBrush,
            alpha = pulseAlpha
        )

        // Glossy highlight shine along the blood drops
        for (i in 0..numDrips) {
            val dripX = (i + 0.5f) * dripInterval
            if (dripX > w) break

            val dripLength = when (i % 7) {
                0 -> h * 0.88f
                2 -> h * 0.68f
                4 -> h * 0.96f
                6 -> h * 0.56f
                else -> 0f
            }
            if (dripLength > 0f) {
                val dripRadius = 3.2f + (i % 3) * 1.3f
                drawCircle(
                    color = Color.White.copy(alpha = 0.45f),
                    radius = 1.6f,
                    center = Offset(dripX + 1.2f, dripLength - dripRadius * 0.3f)
                )
            }
        }
    }
}

/**
 * Animated Jack-O'-Lantern (carved Halloween pumpkin) with candle-lit glowing eyes and mouth.
 */
@Composable
fun JackOLantern(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 44.dp,
    pumpkinColor: Color = Color(0xFFFF6D00),
    candleGlowColor: Color = Color(0xFFFFE082)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "candleFlicker")
    val flickerGlow by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    Canvas(modifier = modifier.size(sizeDp)) {
        val w = size.width
        val h = size.height

        val cx = w * 0.5f
        val cy = h * 0.55f
        val pRadiusX = w * 0.44f
        val pRadiusY = h * 0.38f

        // 1. Stem on top
        val stemPath = Path().apply {
            moveTo(cx - 3f, cy - pRadiusY)
            quadraticTo(cx - 7f, cy - pRadiusY - 10f, cx - 4f, cy - pRadiusY - 14f)
            lineTo(cx + 4f, cy - pRadiusY - 13f)
            quadraticTo(cx + 1f, cy - pRadiusY - 7f, cx + 3f, cy - pRadiusY)
            close()
        }
        drawPath(stemPath, color = Color(0xFF388E3C))

        // 2. Pumpkin Lobes (3 overlapping ovals for 3D pumpkin shape)
        // Outer lobes (darker orange)
        val outerLobeBrush = Brush.radialGradient(
            colors = listOf(pumpkinColor, Color(0xFFD84315)),
            center = Offset(cx, cy),
            radius = pRadiusX * 1.2f
        )
        drawOval(
            brush = outerLobeBrush,
            topLeft = Offset(cx - pRadiusX * 1.02f, cy - pRadiusY),
            size = Size(pRadiusX * 2.04f, pRadiusY * 2f)
        )
        // Middle lobes (brighter highlights)
        drawOval(
            color = Color(0xFFFF8F00),
            topLeft = Offset(cx - pRadiusX * 0.65f, cy - pRadiusY * 0.98f),
            size = Size(pRadiusX * 1.30f, pRadiusY * 1.96f)
        )
        // Center lobe
        drawOval(
            color = Color(0xFFFF9E1B),
            topLeft = Offset(cx - pRadiusX * 0.32f, cy - pRadiusY * 0.96f),
            size = Size(pRadiusX * 0.64f, pRadiusY * 1.92f)
        )

        // 3. Carved Eyes (Triangles)
        val eyeLeft = Path().apply {
            moveTo(cx - pRadiusX * 0.50f, cy - pRadiusY * 0.32f)
            lineTo(cx - pRadiusX * 0.20f, cy - pRadiusY * 0.32f)
            lineTo(cx - pRadiusX * 0.35f, cy - pRadiusY * 0.62f)
            close()
        }
        val eyeRight = Path().apply {
            moveTo(cx + pRadiusX * 0.20f, cy - pRadiusY * 0.32f)
            lineTo(cx + pRadiusX * 0.50f, cy - pRadiusY * 0.32f)
            lineTo(cx + pRadiusX * 0.35f, cy - pRadiusY * 0.62f)
            close()
        }

        // 4. Carved Nose (Small Triangle)
        val nose = Path().apply {
            moveTo(cx - 3.5f, cy - 2f)
            lineTo(cx + 3.5f, cy - 2f)
            lineTo(cx, cy - 8f)
            close()
        }

        // 5. Carved Spooky Zig-Zag Mouth
        val mouth = Path().apply {
            moveTo(cx - pRadiusX * 0.60f, cy + pRadiusY * 0.25f)
            lineTo(cx - pRadiusX * 0.40f, cy + pRadiusY * 0.45f)
            lineTo(cx - pRadiusX * 0.25f, cy + pRadiusY * 0.25f)
            lineTo(cx, cy + pRadiusY * 0.48f)
            lineTo(cx + pRadiusX * 0.25f, cy + pRadiusY * 0.25f)
            lineTo(cx + pRadiusX * 0.40f, cy + pRadiusY * 0.45f)
            lineTo(cx + pRadiusX * 0.60f, cy + pRadiusY * 0.25f)
            lineTo(cx + pRadiusX * 0.45f, cy + pRadiusY * 0.60f)
            lineTo(cx + pRadiusX * 0.20f, cy + pRadiusY * 0.42f)
            lineTo(cx, cy + pRadiusY * 0.65f)
            lineTo(cx - pRadiusX * 0.20f, cy + pRadiusY * 0.42f)
            lineTo(cx - pRadiusX * 0.45f, cy + pRadiusY * 0.60f)
            close()
        }

        // Draw carvings with dark inner depth + glowing candle flicker
        val innerDark = Color(0xFF210800)
        drawPath(eyeLeft, color = innerDark)
        drawPath(eyeRight, color = innerDark)
        drawPath(nose, color = innerDark)
        drawPath(mouth, color = innerDark)

        val glowColor = candleGlowColor.copy(alpha = flickerGlow)
        drawPath(eyeLeft, color = glowColor, style = Stroke(width = 2.2f))
        drawPath(eyeRight, color = glowColor, style = Stroke(width = 2.2f))
        drawPath(nose, color = glowColor, style = Stroke(width = 1.8f))
        drawPath(mouth, color = glowColor, style = Stroke(width = 2.5f))
        drawPath(mouth, color = glowColor.copy(alpha = flickerGlow * 0.85f), style = Fill)
        drawPath(eyeLeft, color = glowColor.copy(alpha = flickerGlow * 0.85f), style = Fill)
        drawPath(eyeRight, color = glowColor.copy(alpha = flickerGlow * 0.85f), style = Fill)
    }
}

/**
 * Animated Silhouette Bat with wing flapping.
 */
@Composable
fun FlyingBat(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 26.dp,
    batColor: Color = Color(0xFF1E1428)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "batFlap")
    val flapScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flapScale"
    )

    Canvas(modifier = modifier.size(sizeDp)) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        val wingSpan = w * 0.48f * flapScale

        // Bat wing path
        val batPath = Path().apply {
            moveTo(cx, cy - 3f)
            // Left wing
            cubicTo(cx - wingSpan * 0.4f, cy - h * 0.42f, cx - wingSpan * 0.8f, cy - h * 0.35f, cx - wingSpan, cy - h * 0.15f)
            cubicTo(cx - wingSpan * 0.85f, cy + h * 0.15f, cx - wingSpan * 0.55f, cy + h * 0.20f, cx - wingSpan * 0.45f, cy + h * 0.05f)
            cubicTo(cx - wingSpan * 0.35f, cy + h * 0.25f, cx - wingSpan * 0.15f, cy + h * 0.22f, cx, cy + h * 0.18f)

            // Right wing
            cubicTo(cx + wingSpan * 0.15f, cy + h * 0.22f, cx + wingSpan * 0.35f, cy + h * 0.25f, cx + wingSpan * 0.45f, cy + h * 0.05f)
            cubicTo(cx + wingSpan * 0.55f, cy + h * 0.20f, cx + wingSpan * 0.85f, cy + h * 0.15f, cx + wingSpan, cy - h * 0.15f)
            cubicTo(cx + wingSpan * 0.8f, cy - h * 0.35f, cx + wingSpan * 0.4f, cy - h * 0.42f, cx, cy - 3f)
            close()
        }

        drawPath(batPath, color = batColor)
        // Little glowing red eyes
        drawCircle(color = Color(0xFFFF1744), radius = 1.1f, center = Offset(cx - 2f, cy - 2f))
        drawCircle(color = Color(0xFFFF1744), radius = 1.1f, center = Offset(cx + 2f, cy - 2f))
    }
}

/**
 * Top-level Halloween Home Feed Decoration Overlay.
 * Includes:
 * 1. Spiderwebs in top corners (left & right) with swinging spider.
 * 2. Dripping blood strip along the top sticky header.
 * 3. Flying bats near the top corners.
 * 4. Carved Jack-o'-Lantern with glowing eyes sitting on the category tab selector.
 */
@Composable
fun HalloweenHomeFeedOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Dripping blood hanging from the very top of the screen
        BloodDripHeader(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            heightDp = 24.dp
        )

        // 1. Top-Left Spiderweb (mirrored)
        SpiderwebCorner(
            modifier = Modifier
                .align(Alignment.TopStart)
                .graphicsLayer(scaleX = -1f),
            sizeDp = 115.dp,
            showSpider = false
        )

        // 2. Top-Right Spiderweb with swinging Spider
        SpiderwebCorner(
            modifier = Modifier
                .align(Alignment.TopEnd),
            sizeDp = 125.dp,
            showSpider = true
        )

        // 3. Spooky flying bats
        FlyingBat(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 82.dp, top = 38.dp)
                .graphicsLayer(rotationZ = -14f),
            sizeDp = 28.dp
        )

        FlyingBat(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 94.dp, top = 54.dp)
                .graphicsLayer(rotationZ = 18f),
            sizeDp = 22.dp
        )

        FlyingBat(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 130.dp, top = 24.dp)
                .graphicsLayer(rotationZ = 6f),
            sizeDp = 18.dp
        )
    }
}
