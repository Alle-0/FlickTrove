package com.cinetrack.ui.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.premiumScrollbar(
    state: ScrollState,
    width: Float = 4f,
    color: Color = Color.White.copy(alpha = 0.3f)
): Modifier = drawWithContent {
    drawContent()
    
    val maxScroll = state.maxValue.toFloat()
    if (maxScroll > 0) {
        val visibleHeight = size.height
        val contentHeight = visibleHeight + maxScroll
        val thumbHeight = (visibleHeight / contentHeight) * visibleHeight
        val scrollOffset = state.value.toFloat()
        val thumbOffset = (scrollOffset / maxScroll) * (visibleHeight - thumbHeight)
        
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - width.dp.toPx() - 4.dp.toPx(), thumbOffset),
            size = Size(width.dp.toPx(), thumbHeight),
            cornerRadius = CornerRadius(width.dp.toPx() / 2, width.dp.toPx() / 2)
        )
    }
}

fun Modifier.premiumScrollbar(
    state: androidx.compose.foundation.lazy.LazyListState,
    width: Float = 4f,
    color: Color = Color.White.copy(alpha = 0.3f)
): Modifier = drawWithContent {
    drawContent()
    
    val layoutInfo = state.layoutInfo
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    if (visibleItemsInfo.isNotEmpty() && layoutInfo.totalItemsCount > visibleItemsInfo.size) {
        val visibleHeight = size.height
        val firstItem = visibleItemsInfo.first()
        val lastItem = visibleItemsInfo.last()
        
        val totalItemsCount = layoutInfo.totalItemsCount.toFloat()
        val thumbHeight = (visibleItemsInfo.size / totalItemsCount) * visibleHeight
        
        val scrollOffset = firstItem.index.toFloat() / totalItemsCount
        val thumbOffset = scrollOffset * visibleHeight
        
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - width.dp.toPx() - 4.dp.toPx(), thumbOffset),
            size = Size(width.dp.toPx(), thumbHeight),
            cornerRadius = CornerRadius(width.dp.toPx() / 2, width.dp.toPx() / 2)
        )
    }
}
