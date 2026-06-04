package com.cinetrack.ui.components.shared

import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.utils.bounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionBottomSheet(
    movieTitle: String,
    accentColor: Color,
    onStartRating: () -> Unit,
    onStartNote: () -> Unit,
    onStartFolder: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    FlickTroveBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = movieTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Azioni rapide",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }

            // Action Items
            QuickActionItem(
                label = "Voto rapido",
                icon = ImageVector.vectorResource(id = R.drawable.ic_star),
                iconColor = accentColor,
                onClick = {
                    onDismiss()
                    onStartRating()
                }
            )
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

            QuickActionItem(
                label = "Nota rapida",
                icon = ImageVector.vectorResource(id = R.drawable.ic_pencil),
                iconColor = Color.White,
                onClick = {
                    onDismiss()
                    onStartNote()
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

            QuickActionItem(
                label = "Cartelle",
                icon = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                iconColor = Color(0xFF6366F1),
                onClick = {
                    onDismiss()
                    onStartFolder()
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

            QuickActionItem(
                label = "Elimina",
                icon = ImageVector.vectorResource(id = R.drawable.ic_trash),
                iconColor = Color(0xFFFF4444),
                labelColor = Color(0xFFFF4444),
                onClick = {
                    onDismiss()
                    onConfirmDelete()
                }
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    label: String,
    icon: ImageVector,
    iconColor: Color,
    labelColor: Color = Color.White,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .bounceClick {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = labelColor
        )
    }
}
