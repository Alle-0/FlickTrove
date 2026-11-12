package com.cinetrack.ui.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private fun DrawScope.drawScrollbarThumb(
    thumbHeight: Float,
    thumbOffset: Float,
    width: Float,
    paddingEnd: Float,
    color: Color
) {
    if (thumbHeight <= 0f) return
    val widthPx = width.dp.toPx()
    val paddingEndPx = paddingEnd.dp.toPx()
    val topLeftX = if (layoutDirection == LayoutDirection.Rtl) {
        paddingEndPx
    } else {
        size.width - widthPx - paddingEndPx
    }
    drawRoundRect(
        color = color,
        topLeft = Offset(topLeftX, thumbOffset),
        size = Size(widthPx, thumbHeight),
        cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f)
    )
}

private fun DrawScope.drawLazyScrollbar(
    visibleCount: Int,
    totalItemsCount: Int,
    firstVisibleItemIndex: Int,
    width: Float,
    paddingEnd: Float,
    paddingVertical: Float,
    color: Color
) {
    if (visibleCount <= 0 || totalItemsCount <= visibleCount) return
    val paddingVerticalPx = paddingVertical.dp.toPx()
    val trackHeight = (size.height - (paddingVerticalPx * 2f)).coerceAtLeast(0f)
    val thumbHeight = (visibleCount / totalItemsCount.toFloat()) * trackHeight
    val maxIndex = (totalItemsCount - visibleCount).coerceAtLeast(1)
    val scrollOffset = (firstVisibleItemIndex.toFloat() / maxIndex).coerceIn(0f, 1f)
    val thumbOffset = paddingVerticalPx + scrollOffset * (trackHeight - thumbHeight)
    drawScrollbarThumb(thumbHeight, thumbOffset, width, paddingEnd, color)
}

fun Modifier.premiumScrollbar(
    state: ScrollState,
    width: Float = 2.5f,
    paddingEnd: Float = 3f,
    paddingVertical: Float = 20f,
    color: Color = Color.White.copy(alpha = 0.35f)
): Modifier = drawWithContent {
    drawContent()

    val maxScroll = state.maxValue.toFloat()
    if (maxScroll > 0f) {
        val startY = paddingVertical.dp.toPx()
        val trackHeight = (size.height - (startY * 2f)).coerceAtLeast(0f)
        val contentHeight = size.height + maxScroll
        val thumbHeight = (size.height / contentHeight) * trackHeight
        val scrollOffset = state.value.toFloat()
        val thumbOffset = startY + (scrollOffset / maxScroll) * (trackHeight - thumbHeight)
        drawScrollbarThumb(thumbHeight, thumbOffset, width, paddingEnd, color)
    }
}

fun Modifier.premiumScrollbar(
    state: androidx.compose.foundation.lazy.LazyListState,
    width: Float = 2.5f,
    paddingEnd: Float = 3f,
    paddingVertical: Float = 20f,
    color: Color = Color.White.copy(alpha = 0.35f)
): Modifier = drawWithContent {
    drawContent()

    val layoutInfo = state.layoutInfo
    drawLazyScrollbar(
        visibleCount = layoutInfo.visibleItemsInfo.size,
        totalItemsCount = layoutInfo.totalItemsCount,
        firstVisibleItemIndex = state.firstVisibleItemIndex,
        width = width,
        paddingEnd = paddingEnd,
        paddingVertical = paddingVertical,
        color = color
    )
}

fun Modifier.premiumScrollbar(
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    width: Float = 2.5f,
    paddingEnd: Float = 3f,
    paddingVertical: Float = 20f,
    color: Color = Color.White.copy(alpha = 0.35f)
): Modifier = drawWithContent {
    drawContent()

    val layoutInfo = state.layoutInfo
    drawLazyScrollbar(
        visibleCount = layoutInfo.visibleItemsInfo.size,
        totalItemsCount = layoutInfo.totalItemsCount,
        firstVisibleItemIndex = state.firstVisibleItemIndex,
        width = width,
        paddingEnd = paddingEnd,
        paddingVertical = paddingVertical,
        color = color
    )
}

fun Modifier.premiumScrollbar(
    state: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    width: Float = 2.5f,
    paddingEnd: Float = 3f,
    paddingVertical: Float = 20f,
    color: Color = Color.White.copy(alpha = 0.35f)
): Modifier = drawWithContent {
    drawContent()

    val layoutInfo = state.layoutInfo
    drawLazyScrollbar(
        visibleCount = layoutInfo.visibleItemsInfo.size,
        totalItemsCount = layoutInfo.totalItemsCount,
        firstVisibleItemIndex = state.firstVisibleItemIndex,
        width = width,
        paddingEnd = paddingEnd,
        paddingVertical = paddingVertical,
        color = color
    )
}

/**
 * Pattern "Fast Jump + Smooth Glide" consigliato da Google per Jetpack Compose:
 * Se la lista si trova molto in basso (oltre [glideItemIndex]), esegue prima un salto istantaneo
 * (senza animazione) in prossimità della cima per evitare di comporre e misurare centinaia di card
 * intermedie con decine di locandine Coil. Dopodiché esegue un'animazione morbida e fluida degli ultimi
 * elementi fino a zero, garantendo 60/120 FPS costanti senza alcun micro-scatto o allocazioni GC massive.
 */
suspend fun androidx.compose.foundation.lazy.LazyListState.fastScrollToTop(glideItemIndex: Int = 10) {
    if (firstVisibleItemIndex > glideItemIndex) {
        scrollToItem(glideItemIndex)
    }
    animateScrollToItem(0)
}

suspend fun androidx.compose.foundation.lazy.grid.LazyGridState.fastScrollToTop(glideItemIndex: Int = 12) {
    if (firstVisibleItemIndex > glideItemIndex) {
        scrollToItem(glideItemIndex)
    }
    animateScrollToItem(0)
}

suspend fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState.fastScrollToTop(glideItemIndex: Int = 12) {
    if (firstVisibleItemIndex > glideItemIndex) {
        scrollToItem(glideItemIndex)
    }
    animateScrollToItem(0)
}


