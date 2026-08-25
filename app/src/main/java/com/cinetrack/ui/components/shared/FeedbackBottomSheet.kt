package com.cinetrack.ui.components.shared

import androidx.compose.ui.res.stringResource

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

enum class FeedbackType(@androidx.annotation.StringRes val labelRes: Int, val iconRes: Int) {
    BUG(R.string.feedback_type_bug, R.drawable.ic_error),
    IDEA(R.string.feedback_type_idea, R.drawable.ic_documento),
    OTHER(R.string.feedback_type_other, R.drawable.ic_documento)
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
    var message by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
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
                    text = stringResource(R.string.feedback_title),
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
                                text = stringResource(type.labelRes),
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
                onValueChange = { if (it.text.length <= 2000) message = it },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                placeholder = { Text(stringResource(R.string.feedback_hint), color = Color.White.copy(alpha = 0.2f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.15f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    cursorColor = accentColor,
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(20.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                visualTransformation = remember { com.cinetrack.ui.screens.MarkdownVisualTransformation() }
            )

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val mdAction = { prefix: String, suffix: String ->
                        val selection = message.selection
                        val text = message.text
                        if (selection.collapsed) {
                            val newText = text.substring(0, selection.start) + prefix + suffix + text.substring(selection.end)
                            message = androidx.compose.ui.text.input.TextFieldValue(newText, selection = androidx.compose.ui.text.TextRange(selection.start + prefix.length))
                        } else {
                            val newText = text.substring(0, selection.start) + prefix + text.substring(selection.start, selection.end) + suffix + text.substring(selection.end)
                            message = androidx.compose.ui.text.input.TextFieldValue(newText, selection = androidx.compose.ui.text.TextRange(selection.end + prefix.length + suffix.length))
                        }
                    }

                    @Composable
                    fun MdBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { onClick() }
                                .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            content()
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    androidx.compose.foundation.lazy.LazyRow {
                        item {
                            MdBtn(onClick = { mdAction("**", "**") }) { Text("B", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleSmall) }
                            MdBtn(onClick = { mdAction("*", "*") }) { Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.White, style = MaterialTheme.typography.titleSmall) }
                            MdBtn(onClick = { mdAction("~~", "~~") }) { Text("S", textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, color = Color.White, style = MaterialTheme.typography.titleSmall) }
                            MdBtn(onClick = { mdAction("> ", "") }) { Text("\"\"", color = Color.White, style = MaterialTheme.typography.titleSmall) }
                            MdBtn(onClick = { mdAction("- ", "") }) { Text("•", color = Color.White, style = MaterialTheme.typography.titleSmall) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.feedback_roadmap_note),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
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
                onClick = { onSubmit(selectedType, message.text) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isSubmitting && message.text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                } else {
                    Text(stringResource(R.string.feedback_submit), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
