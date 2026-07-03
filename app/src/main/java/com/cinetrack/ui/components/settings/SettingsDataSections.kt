package com.cinetrack.ui.components.settings

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.cinetrack.ui.viewmodel.SettingsViewModel
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.VibrationHelper
import com.cinetrack.utils.Keys
import com.google.firebase.auth.FirebaseUser

@Composable
fun SettingsImagesStorageSection(
    settingsViewModel: SettingsViewModel,
    currentAccentColor: Color,
    vibrationEnabled: Boolean,
    cacheSizeString: String,
    onShowCacheConfirm: () -> Unit
) {
    val context = LocalContext.current
    val imageQuality by settingsViewModel.imageQuality.collectAsStateWithLifecycle()

    SettingsSection(
        title = stringResource(R.string.settings_images),
        icon = ImageVector.vectorResource(id = R.drawable.ic_image)
    ) {
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_hd),
            title = stringResource(R.string.settings_image_quality),
            description = stringResource(R.string.settings_image_quality_desc),
            trailing = { },
            onClick = { },
            customContent = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(
                        Triple(ImageQuality.LOW, stringResource(R.string.quality_low), imageQuality == ImageQuality.LOW),
                        Triple(ImageQuality.MEDIUM, stringResource(R.string.quality_medium), imageQuality == ImageQuality.MEDIUM),
                        Triple(ImageQuality.HIGH, stringResource(R.string.quality_high), imageQuality == ImageQuality.HIGH)
                    )
                    options.forEach { (value, label, isSelected) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) currentAccentColor else Color.White.copy(alpha = 0.05f))
                                .clickable { 
                                    if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                    settingsViewModel.updateImageQuality(value) 
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color(0xFF1E1E1E) else Color.White
                            )
                        }
                    }
                }
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_svuota_trash),
            title = stringResource(R.string.settings_clear_cache),
            description = stringResource(R.string.settings_clear_cache_desc, cacheSizeString),
            tint = Color(0xFFFFA000),
            onClick = { 
                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                onShowCacheConfirm() 
            }
        )
    }
}

@Composable
fun SettingsSyncBackupSection(
    settingsViewModel: SettingsViewModel,
    currentAccentColor: Color,
    vibrationEnabled: Boolean,
    user: FirebaseUser?,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onShowExternalMigrationDialog: () -> Unit,
    onShowBackupDialog: () -> Unit,
    onShowDeepSyncConfirm: () -> Unit
) {
    val context = LocalContext.current
    val isTraktLoggedIn by settingsViewModel.isTraktLoggedIn.collectAsStateWithLifecycle()
    val syncWorkInfo by settingsViewModel.syncWorkInfo.collectAsStateWithLifecycle()
    val libraryDetailsSyncWorkInfo by settingsViewModel.libraryDetailsSyncWorkInfo.collectAsStateWithLifecycle()

    SettingsSection(
        title = stringResource(R.string.settings_sync_backup),
        icon = ImageVector.vectorResource(id = R.drawable.ic_ricarica_cloud),
        footerText = stringResource(R.string.settings_smart_merge)
    ) {
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_ricarica_cloud),
            title = stringResource(R.string.settings_external_migration),
            description = stringResource(R.string.settings_external_migration_desc),
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                onShowExternalMigrationDialog()
            }
        )
        
        // Trakt Sync Card
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_trakt_logo),
            title = "Trakt.tv",
            iconTint = Color.Unspecified,
            onClick = {},
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
                            icon = Icons.Rounded.Close,
                            tint = Color(0xFFFF5252),
                            onClick = {
                                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                settingsViewModel.disconnectTrakt()
                            }
                        )
                    } else {
                        SettingsActionButton(
                            text = stringResource(R.string.trakt_connect),
                            onClick = {
                                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                val clientId = Keys.getTraktClientId()
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://trakt.tv/oauth/authorize?response_type=code&client_id=$clientId&redirect_uri=flicktrove://auth")
                                )
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            },
            customContent = {
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

        // Grouped Backup Card
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_cartella),
            title = stringResource(R.string.settings_device_backup),
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

@Composable
fun SettingsAccountSection(
    user: FirebaseUser?,
    currentAccentColor: Color,
    vibrationEnabled: Boolean,
    onLoginClick: () -> Unit,
    onShowLogoutConfirm: () -> Unit,
    onShowDeleteDialog: () -> Unit
) {
    val context = LocalContext.current
    val isGuest = user?.isAnonymous == true
    val email = user?.email?.takeIf { it.isNotBlank() } ?: stringResource(R.string.settings_guest)

    SettingsSection(
        title = stringResource(R.string.settings_account),
        subtitle = email,
        icon = ImageVector.vectorResource(id = R.drawable.ic_persona)
    ) {
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_exit),
            title = if (!isGuest) stringResource(R.string.settings_dialog_logout_title) else stringResource(R.string.settings_login),
            tint = if (!isGuest) Color.White else currentAccentColor,
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                if (!isGuest) {
                    onShowLogoutConfirm()
                } else {
                    onLoginClick()
                }
            }
        )
        
        if (!isGuest && user != null) {
            SettingsItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_trash),
                title = stringResource(R.string.settings_delete_account),
                tint = Color(0xFFFF5252),
                borderColor = Color(0xFFFF5252),
                onClick = { 
                    if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                    onShowDeleteDialog() 
                }
            )
        }
    }
}

@Composable
fun SettingsSupportSection(
    vibrationEnabled: Boolean,
    onShowFeedbackDialog: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    SettingsSection(
        title = stringResource(R.string.settings_support_info),
        icon = ImageVector.vectorResource(id = R.drawable.ic_question_mark_pieno)
    ) {
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_comment),
            title = stringResource(R.string.settings_send_feedback),
            description = stringResource(R.string.settings_send_feedback_desc),
            onClick = { 
                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                onShowFeedbackDialog() 
            }
        )
        SettingsItem(
            icon = Icons.Rounded.Favorite,
            title = "Buy me a Coffee ☕",
            description = "Support the developer with a donation",
            isExternal = true,
            tint = Color(0xFFFFA000),
            onClick = { 
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                uriHandler.openUri("https://paypal.me/AlessandroBasile0") 
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_documento),
            title = stringResource(R.string.settings_terms_service),
            isExternal = true,
            onClick = { 
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                uriHandler.openUri("https://raw.githubusercontent.com/Alle-0/FlickTrove/main/TERMS_OF_SERVICE.md") 
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_scudo_privacy),
            title = stringResource(R.string.settings_privacy_policy),
            isExternal = true,
            onClick = { 
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                uriHandler.openUri("https://raw.githubusercontent.com/Alle-0/FlickTrove/main/PRIVACY_POLICY.md") 
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Attribution Row with better styling
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AttributionRow(
                brand = "TMDB",
                text = stringResource(R.string.settings_tmdb_notice)
            )
            AttributionRow(
                brand = "OMDb API",
                text = stringResource(R.string.settings_omdb_notice)
            )
            AttributionRow(
                brand = "Trakt.tv",
                text = stringResource(R.string.settings_trakt_notice)
            )
        }
    }
}

@Composable
fun SettingsFooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.settings_app_version),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            ),
            color = Color.White.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_made_with),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceMuted
            )
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.padding(horizontal = 4.dp).size(12.dp)
            )
            Text(
                text = stringResource(R.string.settings_for_lovers),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceMuted
            )
        }
    }
}
