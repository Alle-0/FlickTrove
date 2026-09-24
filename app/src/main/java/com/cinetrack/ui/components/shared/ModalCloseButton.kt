package com.cinetrack.ui.components.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick

/**
 * Standard unified close button (naked X with alpha, touch target and bounce feedback)
 * for all dialogs, modals, and sheets across FlickTrove.
 */
@Composable
fun ModalCloseButton(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    iconSize: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    contentDescription: String = stringResource(R.string.settings_close),
    onClick: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    val action = onClick ?: onClose ?: {}
    Box(
        modifier = modifier
            .size(size)
            .bounceClick(scaleDown = 0.88f, onClick = action),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
