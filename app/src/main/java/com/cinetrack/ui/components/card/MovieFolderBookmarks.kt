package com.cinetrack.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Sagoma cinematografica a nastro di pellicola 35mm con fori di trascinamento laterali (sprocket holes).
 */
val FilmStripBookmarkShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    fillType = PathFillType.EvenOdd

    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h)
    lineTo(w / 2f, h * 0.9f)
    lineTo(0f, h)
    close()

    val holeW = w * 0.15f
    val holeH = h * 0.1f
    val margin = w * 0.1f
    val spacing = h * 0.22f

    var y = h * 0.2f
    while (y + holeH < h * 0.85f) {
        addRect(Rect(margin, y, margin + holeW, y + holeH))
        addRect(Rect(w - margin - holeW, y, w - margin, y + holeH))
        y += spacing
    }
}

/**
 * Segnalibro colorato a forma di pellicola 35mm che indica l'appartenenza a una o più cartelle personalizzate.
 */
@Composable
fun MovieFolderBookmarks(
    folderColors: List<Color>,
    modifier: Modifier = Modifier
) {
    if (folderColors.isEmpty()) return

    val indicatorBrush = if (folderColors.size > 1) {
        Brush.verticalGradient(folderColors)
    } else {
        Brush.linearGradient(listOf(folderColors.first(), folderColors.first()))
    }

    Box(
        modifier = modifier
            .padding(start = 14.dp)
            .width(16.dp)
            .height(28.dp)
            .graphicsLayer {
                shadowElevation = 8.dp.toPx()
                shape = FilmStripBookmarkShape
                clip = true
            }
            .background(indicatorBrush)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = FilmStripBookmarkShape
            )
    )
}
