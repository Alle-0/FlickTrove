package com.cinetrack.ui.components.shared

import com.cinetrack.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
    Box(
        modifier = modifier
            .size(36.dp)
            .bounceClick(scaleDown = 0.92f, onClick = onToggle)
            .hazeGlass(
                state = hazeState,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = columns,
            transitionSpec = {
                (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.7f, animationSpec = tween(180)))
                    .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.7f, animationSpec = tween(120)))
            },
            label = "LayoutToggleIconTransition"
        ) { currentColumns ->
            Icon(
                imageVector = layoutToggleIcon(currentColumns),
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
