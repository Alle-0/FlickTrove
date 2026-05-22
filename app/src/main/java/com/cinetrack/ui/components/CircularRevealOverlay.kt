package com.cinetrack.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntSize
import kotlin.math.hypot

/**
 * Full-screen circular reveal overlay.
 *
 * Renders the [oldBitmap] snapshot of the old theme state, and animates a circular
 * hole expanding from the center of the screen to reveal the underlying new theme state.
 * Once the animation completes, calls [onRevealComplete] to clear the state.
 */
@Composable
fun CircularRevealOverlay(
    oldBitmap: Bitmap,
    modifier: Modifier = Modifier,
    durationMs: Int = 750, // Majestic, smooth animation speed
    onRevealComplete: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(oldBitmap) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMs,
                easing = FastOutSlowInEasing
            )
        )
        onRevealComplete()
    }

    val imageBitmap = remember(oldBitmap) { oldBitmap.asImageBitmap() }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = hypot(center.x, center.y) * 1.05f
        val currentRadius = progress.value * maxRadius

        val revealPath = Path().apply {
            addOval(Rect(center = center, radius = currentRadius))
        }

        // Draw only outside the expanding circle, revealing the actual composable
        // beneath (which runs on the new theme color) inside the circle!
        clipPath(revealPath, clipOp = ClipOp.Difference) {
            drawImage(
                image = imageBitmap,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )
        }
    }
}
