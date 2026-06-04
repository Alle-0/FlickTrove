package com.cinetrack.ui.components.shared

import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector

fun normalizedGridColumns(columns: Int): Int = if (columns in 1..4) columns else 3

fun nextGridColumns(columns: Int): Int = when (normalizedGridColumns(columns)) {
    3 -> 4
    4 -> 1
    else -> 3
}

@androidx.compose.runtime.Composable
fun layoutToggleIcon(columns: Int): ImageVector = when (normalizedGridColumns(columns)) {
    1 -> ImageVector.vectorResource(id = R.drawable.ic_strisce)
    4 -> ImageVector.vectorResource(id = R.drawable.ic_grid_a_4)
    else -> ImageVector.vectorResource(id = R.drawable.ic_grid)
}
