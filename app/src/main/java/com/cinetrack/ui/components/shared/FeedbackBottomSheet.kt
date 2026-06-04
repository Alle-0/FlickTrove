package com.cinetrack.ui.components.shared

import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FeedbackType(val label: String, val iconRes: Int) {
    BUG("Bug", R.drawable.ic_error),
    IDEA("Idea", R.drawable.ic_documento),
    OTHER("Altro", R.drawable.ic_documento)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(
    accentColor: Color,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (FeedbackType, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(FeedbackType.BUG) }
    var message by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    FlickTroveBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .imePadding(),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(ImageVector.vectorResource(id = R.drawable.ic_documento), contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                Text(
                    text = "Invia Feedback",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeedbackType.entries.forEach { type ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f),
                                RoundedCornerShape(16.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedType = type
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = type.iconRes),
                                contentDescription = null,
                                tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= 500) message = it },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                placeholder = { Text("Messaggio...", color = Color.White.copy(alpha = 0.2f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.15f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    cursorColor = accentColor,
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(20.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
            )

            if (errorMessage != null) {
                Row(modifier = Modifier.padding(vertical = 12.dp)) {
                    Icon(ImageVector.vectorResource(id = R.drawable.ic_error), contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = errorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSubmit(selectedType, message) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isSubmitting && message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                } else {
                    Text("Invia", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
