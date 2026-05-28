package com.cinetrack.ui.components.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.ui.graphics.vector.ImageVector

fun normalizedGridColumns(columns: Int): Int = if (columns in 1..3) columns else 3

fun nextGridColumns(columns: Int): Int = when (normalizedGridColumns(columns)) {
    3 -> 2
    2 -> 1
    else -> 3
}

fun layoutToggleIcon(columns: Int): ImageVector = when (normalizedGridColumns(columns)) {
    1 -> Icons.Rounded.ViewAgenda
    2 -> Icons.Rounded.GridView
    else -> Icons.Rounded.Apps
}
