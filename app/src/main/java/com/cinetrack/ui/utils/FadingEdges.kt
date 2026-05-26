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
