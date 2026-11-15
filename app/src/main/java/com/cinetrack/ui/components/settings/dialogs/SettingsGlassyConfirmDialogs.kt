package com.cinetrack.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.GlassmorphicModal
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

/**
 * Reusable modal base for settings confirmation dialogs.
 */
@Composable
fun SettingsConfirmBaseDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    onDismiss: () -> Unit,
    dimBackground: Boolean = true,
    dismissOnClickOutside: Boolean = false,
    contentBelowText: (@Composable () -> Unit)? = null,
    actionButtons: @Composable () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = dimBackground,
        dismissOnClickOutside = dismissOnClickOutside,
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            icon()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            if (contentBelowText != null) {
                contentBelowText()
            }
            Spacer(modifier = Modifier.height(if (contentBelowText != null) 24.dp else 32.dp))
            actionButtons()
        }
    }
}

/**
 * Standard confirmation dialog layout with Cancel + Confirm buttons.
 */
@Composable
fun SettingsStandardConfirmDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    iconRes: Int,
    iconTint: Color,
    title: String,
    description: String,
    confirmText: String = stringResource(R.string.settings_confirm),
    cancelText: String = stringResource(R.string.settings_cancel),
    confirmColor: Color = MaterialTheme.colorScheme.primary,
    confirmTextColor: Color = Color.Black,
    confirmEnabled: Boolean = true,
    cancelEnabled: Boolean = true,
    dimBackground: Boolean = true,
    dismissOnClickOutside: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SettingsConfirmBaseDialog(
        visible = visible,
        activeHazeState = activeHazeState,
        icon = {
            Icon(
                ImageVector.vectorResource(id = iconRes),
                null,
                tint = iconTint,
                modifier = Modifier.size(48.dp)
            )
        },
        title = title,
        description = description,
        dimBackground = dimBackground,
        dismissOnClickOutside = dismissOnClickOutside,
        onDismiss = onDismiss,
        actionButtons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsDialogCancelButton(
                    text = cancelText,
                    onClick = onDismiss,
                    enabled = cancelEnabled,
                    modifier = Modifier.weight(1f)
                )
                SettingsDialogConfirmButton(
                    text = confirmText,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    containerColor = confirmColor,
                    contentColor = confirmTextColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    )
}

@Composable
fun DeleteAccountDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SettingsStandardConfirmDialog(
        visible = visible,
        activeHazeState = activeHazeState,
        iconRes = R.drawable.ic_x,
        iconTint = Color(0xFFFF5252),
        title = stringResource(R.string.settings_dialog_delete_account_title),
        description = stringResource(R.string.settings_dialog_delete_account_desc),
        confirmText = stringResource(R.string.settings_yes_delete),
        confirmColor = Color(0xFFFF5252),
        confirmTextColor = Color.White,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun ReauthDeleteAccountDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(visible) { if (visible) password = "" }

    SettingsConfirmBaseDialog(
        visible = visible,
        activeHazeState = activeHazeState,
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_x),
                null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(40.dp)
            )
        },
        title = stringResource(R.string.settings_dialog_reauth_title),
        description = stringResource(R.string.settings_dialog_reauth_desc),
        onDismiss = onDismiss,
        contentBelowText = {
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.settings_dialog_reauth_password_hint)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (password.isNotBlank()) onConfirm(password)
                    }
                ),
                singleLine = true,
                isError = errorMessage != null,
                supportingText = if (errorMessage != null) {
                    { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF5252),
                    focusedLabelColor = Color(0xFFFF5252)
                )
            )
        },
        actionButtons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsDialogCancelButton(
                    text = stringResource(R.string.settings_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                SettingsDialogConfirmButton(
                    text = stringResource(R.string.settings_yes_delete),
                    onClick = {
                        focusManager.clearFocus()
                        if (password.isNotBlank()) onConfirm(password)
                    },
                    enabled = password.isNotBlank(),
                    containerColor = Color(0xFFFF5252),
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    )
}

@Composable
fun ClearCacheConfirmDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SettingsStandardConfirmDialog(
        visible = visible,
        activeHazeState = activeHazeState,
        iconRes = R.drawable.ic_svuota_trash,
        iconTint = MaterialTheme.colorScheme.primary,
        title = stringResource(R.string.settings_dialog_clear_cache_title),
        description = stringResource(R.string.settings_dialog_clear_cache_desc),
        dismissOnClickOutside = true,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun DeepSyncConfirmDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SettingsStandardConfirmDialog(
        visible = visible,
        activeHazeState = activeHazeState,
        iconRes = R.drawable.ic_cloud,
        iconTint = MaterialTheme.colorScheme.primary,
        title = stringResource(R.string.settings_dialog_deep_sync_title),
        description = stringResource(R.string.settings_dialog_deep_sync_desc),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun WipeDataConfirmDialog(
    visible: Boolean,
    title: String,
    description: String,
    buttonText: String,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SettingsStandardConfirmDialog(
        visible = visible,
        activeHazeState = activeHazeState,
        iconRes = R.drawable.ic_trash,
        iconTint = Color(0xFFFF9800),
        title = title,
        description = description,
        confirmText = buttonText,
        confirmColor = Color(0xFFFF9800),
        confirmTextColor = Color.Black,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun CleanupRewatchesConfirmDialog(
    visible: Boolean,
    duplicateCount: Int,
    isLoading: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SettingsConfirmBaseDialog(
        visible = visible,
        activeHazeState = activeHazeState,
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_svuota_trash),
                null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.size(48.dp)
            )
        },
        title = stringResource(R.string.settings_cleanup_rewatches_dialog_title),
        description = if (duplicateCount > 0) {
            stringResource(R.string.settings_cleanup_rewatches_dialog_desc, duplicateCount)
        } else {
            stringResource(R.string.settings_cleanup_rewatches_dialog_desc_zero)
        },
        dismissOnClickOutside = !isLoading,
        onDismiss = { if (!isLoading) onDismiss() },
        actionButtons = {
            if (duplicateCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsDialogCancelButton(
                        text = stringResource(R.string.settings_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
                    SettingsDialogConfirmButton(
                        text = stringResource(R.string.settings_cleanup_rewatches_confirm_btn),
                        onClick = onConfirm,
                        containerColor = Color(0xFFFFA000),
                        contentColor = Color.Black,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
                }
            } else {
                SettingsDialogConfirmButton(
                    text = stringResource(R.string.settings_got_it),
                    onClick = onDismiss,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
fun LogoutConfirmDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SettingsStandardConfirmDialog(
        visible = visible,
        activeHazeState = activeHazeState,
        iconRes = R.drawable.ic_exit,
        iconTint = MaterialTheme.colorScheme.primary,
        title = stringResource(R.string.settings_dialog_logout_title),
        description = stringResource(R.string.settings_dialog_logout_desc),
        confirmText = stringResource(R.string.settings_yes_logout),
        confirmColor = MaterialTheme.colorScheme.primary,
        confirmTextColor = Color.Black,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun WipeDataSelectionDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onSelectLocal: () -> Unit,
    onSelectTotal: () -> Unit
) {
    SettingsConfirmBaseDialog(
        visible = visible,
        activeHazeState = activeHazeState,
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_trash),
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = stringResource(id = R.string.settings_dialog_wipe_data_title),
        description = stringResource(id = R.string.settings_wipe_data_desc),
        onDismiss = onDismiss,
        actionButtons = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onSelectLocal() }
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(id = R.string.settings_wipe_local_data_title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(id = R.string.settings_wipe_local_data_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onSelectTotal() }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(id = R.string.settings_wipe_total_data_title),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5252)
                        )
                        Text(
                            stringResource(id = R.string.settings_wipe_total_data_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF5252).copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                SettingsDialogCancelButton(
                    text = stringResource(R.string.settings_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
