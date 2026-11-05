package com.cinetrack.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@Composable
fun PremiumConfirmDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String = "Annulla",
    type: ConfirmType = ConfirmType.INFO,
    icon: ImageVector? = null,
    hazeState: HazeState? = null
) {
    val haptic = LocalHapticFeedback.current

    val accentColor = when (type) {
        ConfirmType.DANGER -> Color(0xFFEF4444)
        ConfirmType.WARNING -> Color(0xFFFBBF24)
        ConfirmType.INFO -> Color(0xFF0D9488)
    }

    FlickTroveModal(
        isVisible = visible,
        onDismissRequest = onDismiss,
        hazeState = hazeState,
        maxWidth = 420.dp,
        maxWidthFraction = 0.90f,
        contentPadding = PaddingValues(start = 20.dp, top = 26.dp, end = 20.dp, bottom = 20.dp)
    ) {
        // Icon Header (concentric CircleShape standard)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon ?: ImageVector.vectorResource(id = R.drawable.ic_error),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            color = Color.White.copy(alpha = 0.88f),
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp
            ),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons (concentric RoundedCornerShape 16dp: Router 32dp - P 16dp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .bounceClick(shape = RoundedCornerShape(16.dp)) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm()
                        onDismiss()
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = confirmLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (type == ConfirmType.DANGER) Color.White else Color.Black
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .bounceClick(shape = RoundedCornerShape(16.dp)) { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cancelLabel,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
