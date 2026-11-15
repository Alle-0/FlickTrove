package com.cinetrack.ui.components.settings

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import com.cinetrack.R
import com.cinetrack.ui.theme.OnSurfaceMuted
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.SettingsViewModel
import com.cinetrack.util.Keys
import com.cinetrack.util.VibrationHelper
import com.google.firebase.auth.FirebaseUser

@Composable
fun SettingsSyncBackupSection(
    settingsViewModel: SettingsViewModel,
    currentAccentColor: Color,
    vibrationEnabled: Boolean,
    user: FirebaseUser?,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onShowExternalMigrationDialog: () -> Unit,
    onShowBackupDialog: () -> Unit,
    onShowDeepSyncConfirm: () -> Unit,
    onShowUnmatchedItems: () -> Unit
) {
    val context = LocalContext.current
    val isTraktLoggedIn by settingsViewModel.isTraktLoggedIn.collectAsStateWithLifecycle()
    val traktNeedsReconnect by settingsViewModel.traktNeedsReconnect.collectAsStateWithLifecycle()
    val isSimklLoggedIn by settingsViewModel.isSimklLoggedIn.collectAsStateWithLifecycle()
    val syncWorkInfo by settingsViewModel.syncWorkInfo.collectAsStateWithLifecycle()
    val simklSyncWorkInfo by settingsViewModel.simklSyncWorkInfo.collectAsStateWithLifecycle()
    val libraryDetailsSyncWorkInfo by settingsViewModel.libraryDetailsSyncWorkInfo.collectAsStateWithLifecycle()
    val externalImportWorkInfo by settingsViewModel.externalImportWorkInfo.collectAsStateWithLifecycle()
    val unmatchedMovies by settingsViewModel.unmatchedMovies.collectAsStateWithLifecycle()
    val unmatchedCount = remember(unmatchedMovies) { unmatchedMovies.count { it.posterPath == null && it.overview == null } }

    SettingsSection(
        title = stringResource(R.string.settings_sync_backup),
        icon = ImageVector.vectorResource(id = R.drawable.ic_ricarica_cloud),
        footerText = stringResource(R.string.settings_smart_merge)
    ) {
        // --- 1. CLOUD TRACKING ---
        Text(
            text = stringResource(R.string.settings_group_cloud_tracking).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 4.dp)
        )

        // Trakt Sync Card
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_trakt_logo),
            title = "Trakt.tv",
            description = stringResource(R.string.settings_trakt_sync_desc),
            iconTint = Color.Unspecified,
            alpha = if (isSimklLoggedIn) 0.35f else 1f,
            trailing = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTraktLoggedIn) {
                        SettingsActionButton(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_ricarica_cloud),
                            onClick = {
                                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        settingsViewModel.syncTraktNow()
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    settingsViewModel.syncTraktNow()
                                }
                            }
                        )
                        SettingsActionButton(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_x),
                            tint = Color(0xFFFF5252),
                            onClick = {
                                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                settingsViewModel.disconnectTrakt()
                            }
                        )
                    } else {
                        SettingsActionButton(
                            text = stringResource(R.string.trakt_connect),
                            enabled = !isSimklLoggedIn,
                            onClick = {
                                if (isSimklLoggedIn) return@SettingsActionButton
                                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                val clientId = Keys.getTraktKey()
                                val oauthState = java.util.UUID.randomUUID().toString()
                                settingsViewModel.savePendingOAuthState(oauthState)
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(
                                        "https://trakt.tv/oauth/authorize" +
                                        "?response_type=code" +
                                        "&client_id=$clientId" +
                                        "&redirect_uri=flicktrove://auth" +
                                        "&state=$oauthState"
                                    )
                                )
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            },
            customContent = {
                if (traktNeedsReconnect && !isTraktLoggedIn) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFA000).copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Session expired due to Trakt security updates. Tap Connect to reconnect.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFA000)
                        )
                    }
                }

                if (syncWorkInfo != null && syncWorkInfo!!.state == WorkInfo.State.RUNNING) {
                    val progressData = syncWorkInfo!!.progress
                    val current = progressData.getInt("current", 0)
                    val total = progressData.getInt("total", 0)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (total > 0) {
                        Text(
                            text = stringResource(R.string.trakt_syncing_progress, current, total),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { current.toFloat() / total.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = currentAccentColor,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.trakt_sync_prep),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = currentAccentColor,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        )

        // SIMKL Sync Card
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_simkl_logo),
            title = "SIMKL",
            description = stringResource(R.string.simkl_sync_desc),
            iconTint = Color.Unspecified,
            alpha = if (isTraktLoggedIn) 0.35f else 1f,
            trailing = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSimklLoggedIn) {
                        SettingsActionButton(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_ricarica_cloud),
                            onClick = {
                                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                settingsViewModel.syncSimklNow()
                            }
                        )
                        SettingsActionButton(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_x),
                            tint = Color(0xFFFF5252),
                            onClick = {
                                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                settingsViewModel.disconnectSimkl()
                            }
                        )
                    } else {
                        SettingsActionButton(
                            text = stringResource(R.string.simkl_connect),
                            enabled = !isTraktLoggedIn,
                            onClick = {
                                if (isTraktLoggedIn) return@SettingsActionButton
                                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                val verifier = com.cinetrack.util.PkceHelper.generateCodeVerifier()
                                val challenge = com.cinetrack.util.PkceHelper.generateCodeChallenge(verifier)
                                val state = com.cinetrack.util.PkceHelper.generateState()
                                settingsViewModel.savePendingSimklOAuth(verifier, state)
                                val clientId = Keys.getSimklKey()
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(
                                        "https://simkl.com/oauth2/authorize" +
                                        "?response_type=code" +
                                        "&client_id=$clientId" +
                                        "&redirect_uri=flicktrove://simkl_login" +
                                        "&code_challenge=$challenge" +
                                        "&code_challenge_method=S256" +
                                        "&scope=media:read+media:write" +
                                        "&state=$state"
                                    )
                                )
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            },
            customContent = {
                if (simklSyncWorkInfo != null && simklSyncWorkInfo!!.state == WorkInfo.State.RUNNING) {
                    val progressData = simklSyncWorkInfo!!.progress
                    val current = progressData.getInt("current", 0)
                    val total = progressData.getInt("total", 0)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (total > 0) {
                        Text(
                            text = stringResource(R.string.trakt_syncing_progress, current, total),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { current.toFloat() / total.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = currentAccentColor,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    } else {
                        val statusString = progressData.getString("status")
                        if (statusString != null) {
                            Text(
                                text = statusString,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = currentAccentColor,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. BACKUP & MIGRATION ---
        Text(
            text = stringResource(R.string.settings_group_backup_import).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 4.dp)
        )

        // Universal Data Import
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_ricarica_cloud),
            title = stringResource(R.string.settings_external_migration),
            description = stringResource(R.string.settings_external_migration_desc),
            onClick = {
                if (externalImportWorkInfo?.state != WorkInfo.State.RUNNING) {
                    if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                    onShowExternalMigrationDialog()
                }
            },
            customContent = {
                if (externalImportWorkInfo != null && externalImportWorkInfo!!.state == WorkInfo.State.RUNNING) {
                    val progressData = externalImportWorkInfo!!.progress
                    val current = progressData.getInt("current", 0)
                    val total = progressData.getInt("total", 0)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (total > 0) {
                        Text(
                            text = "$current / $total",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { current.toFloat() / total.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = currentAccentColor,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.settings_processing),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = currentAccentColor,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        )

        // View Unmatched Items (Conditional: shown ONLY if there are unmatched items)
        if (unmatchedCount > 0) {
            SettingsItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_error),
                title = "${stringResource(R.string.unmatched_items_title)} ($unmatchedCount)",
                description = stringResource(R.string.unmatched_items_desc),
                tint = MaterialTheme.colorScheme.error,
                onClick = {
                    if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                    onShowUnmatchedItems()
                }
            )
        }

        // Grouped Backup Card
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_cartella),
            title = stringResource(R.string.settings_device_backup),
            description = stringResource(R.string.settings_device_backup_desc),
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                onShowBackupDialog()
            }
        )

        // Deep Details Sync
        if (user?.isAnonymous != true) {
            SettingsItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_cloud),
                title = stringResource(R.string.settings_sync_missing_details),
                description = stringResource(R.string.settings_sync_missing_details_desc),
                onClick = {
                    if (libraryDetailsSyncWorkInfo?.state != WorkInfo.State.RUNNING) {
                        if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                        onShowDeepSyncConfirm()
                    }
                },
                customContent = {
                    if (libraryDetailsSyncWorkInfo != null && libraryDetailsSyncWorkInfo!!.state == WorkInfo.State.RUNNING) {
                        val progressData = libraryDetailsSyncWorkInfo!!.progress
                        val current = progressData.getInt("current", 0)
                        val total = progressData.getInt("total", 0)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (total > 0) {
                            Text(
                                text = "$current / $total",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { current.toFloat() / total.toFloat() },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = currentAccentColor,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = currentAccentColor,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            )
        }
    }
}

