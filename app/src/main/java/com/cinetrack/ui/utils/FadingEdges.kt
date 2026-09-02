package com.cinetrack.ui.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Aggiunge bordi sfumati (fading edges) in alto e in basso a un contenitore scrollabile.
 */
fun Modifier.verticalFadingEdges(
    scrollState: ScrollState,
    topEdgeHeight: Dp = 24.dp,
    bottomEdgeHeight: Dp = 24.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val topColors = listOf(Color.Transparent, Color.Black)
        val bottomColors = listOf(Color.Black, Color.Transparent)
        
        val showTop = scrollState.value > 0
        val showBottom = scrollState.value < scrollState.maxValue

        if (showTop && topEdgeHeight > 0.dp) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = topColors,
                    startY = 0f,
                    endY = topEdgeHeight.toPx()
                ),
                blendMode = BlendMode.DstIn
            )
        }
        if (showBottom && bottomEdgeHeight > 0.dp) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = bottomColors,
                    startY = size.height - bottomEdgeHeight.toPx(),
                    endY = size.height
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * Aggiunge bordi sfumati (fading edges) in alto e in basso a un contenitore scrollabile LazyList.
 */
fun Modifier.verticalFadingEdges(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    topEdgeHeight: Dp = 24.dp,
    bottomEdgeHeight: Dp = 24.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val topColors = listOf(Color.Transparent, Color.Black)
        val bottomColors = listOf(Color.Black, Color.Transparent)
        
        val showTop = lazyListState.canScrollBackward
        val showBottom = lazyListState.canScrollForward

        if (showTop && topEdgeHeight > 0.dp) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = topColors,
                    startY = 0f,
                    endY = topEdgeHeight.toPx()
                ),
                blendMode = BlendMode.DstIn
            )
        }
        if (showBottom && bottomEdgeHeight > 0.dp) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = bottomColors,
                    startY = size.height - bottomEdgeHeight.toPx(),
                    endY = size.height
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

fun Modifier.verticalFadingEdges(
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    topEdgeHeight: Dp = 24.dp,
    bottomEdgeHeight: Dp = 24.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val topColors = listOf(Color.Transparent, Color.Black)
        val bottomColors = listOf(Color.Black, Color.Transparent)
        
        val showTop = lazyGridState.canScrollBackward
        val showBottom = lazyGridState.canScrollForward

        if (showTop && topEdgeHeight > 0.dp) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = topColors,
                    startY = 0f,
                    endY = topEdgeHeight.toPx()
                ),
                blendMode = BlendMode.DstIn
            )
        }
        if (showBottom && bottomEdgeHeight > 0.dp) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = bottomColors,
                    startY = size.height - bottomEdgeHeight.toPx(),
                    endY = size.height
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * Aggiunge bordi sfumati (fading edges) a sinistra e a destra a un contenitore scrollabile orizzontalmente.
 */
fun Modifier.horizontalFadingEdges(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    leftEdgeWidth: Dp = 24.dp,
    rightEdgeWidth: Dp = 24.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val leftColors = listOf(Color.Transparent, Color.Black)
        val rightColors = listOf(Color.Black, Color.Transparent)
        
        val showLeft = lazyListState.canScrollBackward
        val showRight = lazyListState.canScrollForward

        if (showLeft && leftEdgeWidth > 0.dp) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = leftColors,
                    startX = 0f,
                    endX = leftEdgeWidth.toPx()
                ),
                blendMode = BlendMode.DstIn
            )
        }
        if (showRight && rightEdgeWidth > 0.dp) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = rightColors,
                    startX = size.width - rightEdgeWidth.toPx(),
                    endX = size.width
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * Aggiunge bordi sfumati (fading edges) a sinistra e a destra a un contenitore scrollabile orizzontalmente.
 */
fun Modifier.horizontalFadingEdges(
    scrollState: ScrollState,
    leftEdgeWidth: Dp = 24.dp,
    rightEdgeWidth: Dp = 24.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val leftColors = listOf(Color.Transparent, Color.Black)
        val rightColors = listOf(Color.Black, Color.Transparent)
        
        val showLeft = scrollState.value > 0
        val showRight = scrollState.value < scrollState.maxValue

        if (showLeft && leftEdgeWidth > 0.dp) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = leftColors,
                    startX = 0f,
                    endX = leftEdgeWidth.toPx()
                ),
                blendMode = BlendMode.DstIn
            )
        }
        if (showRight && rightEdgeWidth > 0.dp) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = rightColors,
                    startX = size.width - rightEdgeWidth.toPx(),
                    endX = size.width
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }
