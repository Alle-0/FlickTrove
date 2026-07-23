package com.cinetrack.ui.components.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import com.cinetrack.R
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.theme.*
import com.cinetrack.ui.utils.verticalFadingEdges
import dev.chrisbanes.haze.HazeState
import com.cinetrack.ui.components.glass.hazeGlass

@Composable
fun DeleteAccountDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.85f)
                    .hazeGlass(state = activeHazeState, alpha = alpha, shape = RoundedCornerShape(32.dp))
                    .clickable(enabled = false) {}
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
    }
}

/**
 * Second-step dialog shown when Firebase requires re-authentication before account deletion.
 * The user must enter their password; on confirm we call [onConfirm] with the typed password.
 */
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
    // Reset field every time the dialog becomes visible
    LaunchedEffect(visible) { if (visible) password = "" }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(101f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.85f)
                    .hazeGlass(state = activeHazeState, alpha = alpha, shape = RoundedCornerShape(32.dp))
                    .clickable(enabled = false) {}
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
    }
}

@Composable
fun ClearCacheConfirmDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.85f)
                    .hazeGlass(
                        state = activeHazeState, alpha = alpha,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable(enabled = false) {}
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
    }
}

@Composable
fun DeepSyncConfirmDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.85f)
                    .hazeGlass(
                        state = activeHazeState, alpha = alpha,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable(enabled = false) {}
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
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(200f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.85f)
                    .hazeGlass(
                        state = activeHazeState, alpha = alpha,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable(enabled = false) {}
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
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.85f)
                    .hazeGlass(
                        state = activeHazeState, alpha = alpha,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable(enabled = false) {}
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
    }
}

@Composable
fun LogoutConfirmDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.85f)
                    .hazeGlass(
                        state = activeHazeState, alpha = alpha,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable(enabled = false) {}
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
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        ColorSelectionDialog(
            hazeState = activeHazeState, alpha = alpha,
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
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        FeedbackDialog(
            hazeState = activeHazeState, alpha = alpha,
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
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        BackupDialog(
            hazeState = activeHazeState, alpha = alpha,
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
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        ExternalMigrationDialog(
            hazeState = activeHazeState, alpha = alpha,
            onDismiss = onDismiss,
            onImport = onImport
        )
    }
}

@Composable
fun SettingsRewatchMigrationDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onKeepLatest: () -> Unit,
    onKeepFirst: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        RewatchMigrationDialog(
            hazeState = activeHazeState, alpha = alpha,
            onDismiss = onDismiss,
            onKeepLatest = onKeepLatest,
            onKeepFirst = onKeepFirst
        )
    }
}

@Composable
fun SettingsLoadingOverlay(
    visible: Boolean,
    activeHazeState: HazeState
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(200f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.settings_processing),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
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
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == EnterExitState.Visible) 1f else 0f }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.85f)
                    .hazeGlass(
                        state = activeHazeState, alpha = alpha,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable(enabled = false) {}
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
    }
}
