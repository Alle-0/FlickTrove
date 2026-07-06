package com.cinetrack.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.res.stringResource
import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cinetrack.ui.theme.ErrorRed
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import androidx.compose.animation.*

@Composable
fun DeleteFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    folderName: String,
    hazeState: HazeState? = null
) {
    val haptic = LocalHapticFeedback.current
    var isDismissing by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var pendingConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { isVisible = true }
    LaunchedEffect(isDismissing) {
        if (isDismissing) {
            isVisible = false
            kotlinx.coroutines.delay(250)
            if (pendingConfirm) {
                onConfirm()
            } else {
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = { isDismissing = true },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val scrimAlpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isVisible) HazeStyles.ModalScrimAlpha else 0f,
            animationSpec = androidx.compose.animation.core.tween(250),
            label = "scrimAlpha"
        )
        val blurAlpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = androidx.compose.animation.core.tween(250),
            label = "blurAlpha"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .pointerInput(Unit) {
                    detectTapGestures { isDismissing = true }
                },
            contentAlignment = Alignment.Center
        ) {

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(androidx.compose.animation.core.tween(250)),
                exit = fadeOut(androidx.compose.animation.core.tween(250))
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .hazeGlass(state = hazeState, shape = RoundedCornerShape(32.dp), alpha = blurAlpha)
                        .pointerInput(Unit) { detectTapGestures { } }
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Destructive icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = ErrorRed.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_error_pieno),
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.folder_delete_title),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.folder_delete_confirm_prefix))
                            withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                                append("\"$folderName\"")
                            }
                            append(stringResource(R.string.folder_delete_confirm_suffix))
                        },
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Delete - high priority
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                pendingConfirm = true
                                isDismissing = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ErrorRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(ImageVector.vectorResource(id = R.drawable.ic_trash), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.folder_delete_action), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                        }

                        // Cancel
                        TextButton(
                            onClick = { isDismissing = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel).uppercase(),
                                color = Color.White.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
