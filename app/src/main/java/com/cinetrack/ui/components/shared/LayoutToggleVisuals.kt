package com.cinetrack.ui.components.shared

import com.cinetrack.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

fun normalizedGridColumns(columns: Int): Int = if (columns in 1..3) columns else 3

fun nextGridColumns(columns: Int): Int = when (normalizedGridColumns(columns)) {
    3 -> 2
    2 -> 1
    else -> 3
}

@Composable
fun layoutToggleIcon(columns: Int): ImageVector = when (normalizedGridColumns(columns)) {
    1 -> ImageVector.vectorResource(id = R.drawable.ic_strisce)
    2 -> ImageVector.vectorResource(id = R.drawable.ic_grid_a_4)
    else -> ImageVector.vectorResource(id = R.drawable.ic_grid)
}

/**
 * Reusable premium glass toggle button for switching grid columns (1 -> 2 -> 3 -> 1).
 */
@Composable
fun LayoutToggleButton(
    columns: Int,
    hazeState: HazeState?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.folder_detail_change_columns)
) {
    Box(modifier = modifier.size(36.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeGlass(
                    state = hazeState,
                    shape = CircleShape,
                    blurRadius = HazeStyles.SmallGlassBlurRadius,
                    useOffscreenStrategy = false
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .bounceClick(scaleDown = 0.92f, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = layoutToggleIcon(columns),
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
