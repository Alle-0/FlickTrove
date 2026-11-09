package com.cinetrack.ui.components.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.theme.ErrorRed
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState

@Composable
fun DeleteFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    folderName: String,
    isLeave: Boolean = false,
    hazeState: HazeState? = null,
    zIndex: Float = 100f
) {
    val haptic = LocalHapticFeedback.current

    FlickTroveModal(
        isVisible = true,
        onDismissRequest = onDismiss,
        hazeState = hazeState,
        zIndex = zIndex,
        maxWidth = 440.dp,
        maxWidthFraction = 0.92f,
        contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Text(
            text = if (isLeave) stringResource(R.string.folder_leave) else stringResource(R.string.folder_delete_title),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = if (isLeave) {
                buildAnnotatedString {
                    append(stringResource(R.string.folder_leave_confirm))
                }
            } else {
                buildAnnotatedString {
                    append(stringResource(R.string.folder_delete_confirm_prefix).trimEnd())
                    append(" ")
                    withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                        append("\"$folderName\"")
                    }
                    append(stringResource(R.string.folder_delete_confirm_suffix))
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .bounceClick { onDismiss() }
                    .hazeGlass(
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color.White.copy(alpha = 0.08f),
                        borderColor = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
                        borderWidth = 1.dp,
                        useRefraction = true
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.main_exit_cancel),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            // Confirm (Delete / Leave)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .bounceClick {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm()
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(ErrorRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLeave) stringResource(R.string.folder_leave) else stringResource(R.string.quick_action_delete),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
