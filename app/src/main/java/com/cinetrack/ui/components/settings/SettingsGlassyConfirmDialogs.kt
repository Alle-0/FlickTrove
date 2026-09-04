package com.cinetrack.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.geometry.Offset
import com.cinetrack.R
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.theme.*
import com.cinetrack.ui.utils.verticalFadingEdges
import dev.chrisbanes.haze.HazeState
import com.cinetrack.ui.components.glass.GlassmorphicModal

@Composable
fun DeleteAccountDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = true,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_x),
                null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.settings_dialog_delete_account_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.settings_dialog_delete_account_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Text(stringResource(R.string.settings_yes_delete), fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
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

    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = true,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_x),
                null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.settings_dialog_reauth_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_dialog_reauth_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
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
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (password.isNotBlank()) onConfirm(password)
                    },
                    enabled = password.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Text(stringResource(R.string.settings_yes_delete), fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ClearCacheConfirmDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_svuota_trash),
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.settings_dialog_clear_cache_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.settings_dialog_clear_cache_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.settings_confirm), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DeepSyncConfirmDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = true,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_cloud),
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.settings_dialog_deep_sync_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.settings_dialog_deep_sync_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.settings_confirm), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BadgesInfoDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    disabledBadges: Set<String>,
    onToggleBadge: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Rounded.Info,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.settings_badges_meaning),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            val legendScrollState = rememberScrollState()
            val renderBadge = @Composable { text: String, color: Color, desc: String ->
                BadgeLegendItem(
                    text = text,
                    color = color,
                    desc = desc,
                    enabled = !disabledBadges.contains(text),
                    onToggle = { enabled -> onToggleBadge(text, enabled) }
                )
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .premiumScrollbar(legendScrollState)
                    .padding(end = 12.dp)
                    .verticalFadingEdges(legendScrollState, 16.dp, 16.dp)
                    .verticalScroll(legendScrollState)
            ) {
                renderBadge(stringResource(R.string.settings_badge_new), NeonPink, stringResource(R.string.settings_badge_new_desc))
                renderBadge(stringResource(R.string.settings_badge_masterpiece), Color(0xFFFFD700), stringResource(R.string.settings_badge_masterpiece_desc))
                renderBadge(stringResource(R.string.settings_badge_best), Color(0xFF00E5FF), stringResource(R.string.settings_badge_best_desc))
                renderBadge(stringResource(R.string.settings_badge_hot), HazeStyles.AccentYellow, stringResource(R.string.settings_badge_hot_desc))
                renderBadge(stringResource(R.string.settings_badge_wow), NeonTeal, stringResource(R.string.settings_badge_wow_desc))
                renderBadge(stringResource(R.string.settings_badge_hidden_gem), Color(0xFF00E676), stringResource(R.string.settings_badge_hidden_gem_desc))
                renderBadge(stringResource(R.string.settings_badge_cult), Color(0xFF9C27B0), stringResource(R.string.settings_badge_cult_desc))
                renderBadge(stringResource(R.string.settings_badge_classic), Color(0xFF8D6E63), stringResource(R.string.settings_badge_classic_desc))
                renderBadge(stringResource(R.string.settings_badge_epic), Color(0xFFFF5722), stringResource(R.string.settings_badge_epic_desc))
                renderBadge(stringResource(R.string.settings_badge_binge), Color(0xFF00BCD4), stringResource(R.string.settings_badge_binge_desc))
                renderBadge(stringResource(R.string.settings_badge_scifi), Color(0xFF2962FF), stringResource(R.string.settings_badge_scifi_desc))
                renderBadge(stringResource(R.string.settings_badge_comedy), Color(0xFFFFEA00), stringResource(R.string.settings_badge_comedy_desc))
                renderBadge(stringResource(R.string.settings_badge_horror), Color(0xFFE53935), stringResource(R.string.settings_badge_horror_desc))
                renderBadge(stringResource(R.string.settings_badge_animation), Color(0xFFFF9800), stringResource(R.string.settings_badge_animation_desc))
                renderBadge(stringResource(R.string.settings_badge_blockbuster), Color(0xFF6200EA), stringResource(R.string.settings_badge_blockbuster_desc))
                renderBadge(stringResource(R.string.settings_badge_indie), Color(0xFFAED581), stringResource(R.string.settings_badge_indie_desc))
                renderBadge(stringResource(R.string.settings_badge_quick), Color(0xFFC6FF00), stringResource(R.string.settings_badge_quick_desc))
                renderBadge(stringResource(R.string.settings_badge_snack), Color(0xFFC6FF00), stringResource(R.string.settings_badge_snack_desc))
                renderBadge(stringResource(R.string.settings_badge_divisive), Color(0xFFFF9800), stringResource(R.string.settings_badge_divisive_desc))
                renderBadge(stringResource(R.string.settings_badge_vintage), Color(0xFFBCAAA4), stringResource(R.string.settings_badge_vintage_desc))
                renderBadge(stringResource(R.string.settings_badge_docu), Color(0xFF9E9E9E), stringResource(R.string.settings_badge_docu_desc))
                renderBadge(stringResource(R.string.settings_badge_family), Color(0xFF81D4FA), stringResource(R.string.settings_badge_family_desc))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.settings_got_it), fontWeight = FontWeight.Bold)
            }
        }
    }
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
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = true,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_trash),
                null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun LogoutConfirmDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = true,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_exit),
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.settings_dialog_logout_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.settings_dialog_logout_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.settings_yes_logout), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WipeDataSelectionDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onSelectLocal: () -> Unit,
    onSelectTotal: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = true,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_trash),
                null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(id = R.string.settings_dialog_wipe_data_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(id = R.string.settings_wipe_data_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSelectLocal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(stringResource(id = R.string.settings_wipe_local_data_title), fontWeight = FontWeight.Bold)
                        Text(stringResource(id = R.string.settings_wipe_local_data_desc), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
                
                Button(
                    onClick = onSelectTotal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.2f),
                        contentColor = Color(0xFFFF9800)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(stringResource(id = R.string.settings_wipe_total_data_title), fontWeight = FontWeight.Bold)
                        Text(stringResource(id = R.string.settings_wipe_total_data_desc), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun SettingsColorSelectionDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String, Offset) -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        ColorSelectionDialog(
            current = current,
            onDismiss = onDismiss,
            onSelect = onSelect
        )
    }
}

@Composable
fun SettingsFeedbackDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    initialEmail: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Int, String) -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        FeedbackDialog(
            initialEmail = initialEmail,
            isLoading = isLoading,
            onDismiss = onDismiss,
            onSubmit = onSubmit
        )
    }
}

@Composable
fun SettingsBackupDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    isBackupLoading: Boolean,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        BackupDialog(
            isBackupLoading = isBackupLoading,
            onDismiss = onDismiss,
            onExport = onExport,
            onImport = onImport
        )
    }
}

@Composable
fun SettingsExternalMigrationDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        ExternalMigrationDialog(
            onDismiss = onDismiss,
            onImport = onImport
        )
    }
}

@Composable
fun SettingsLanguageSelectionDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    current: String,
    accentColor: Color,
    vibrationEnabled: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        LanguageSelectionDialog(
            current = current,
            accentColor = accentColor,
            vibrationEnabled = vibrationEnabled,
            onDismiss = onDismiss,
            onSelect = onSelect
        )
    }
}

@Composable
fun SettingsStartScreenSelectionDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    current: String,
    accentColor: Color,
    vibrationEnabled: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        StartScreenSelectionDialog(
            current = current,
            accentColor = accentColor,
            vibrationEnabled = vibrationEnabled,
            onDismiss = onDismiss,
            onSelect = onSelect
        )
    }
}

@Composable
fun SettingsDashboardSettingsDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    settingsViewModel: com.cinetrack.ui.viewmodel.SettingsViewModel,
    onDismiss: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        DashboardSettingsDialog(
            visible = visible,
            activeHazeState = activeHazeState,
            settingsViewModel = settingsViewModel,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun SettingsLoadingOverlay(
    visible: Boolean,
    activeHazeState: HazeState
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = true,
        dismissOnClickOutside = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
