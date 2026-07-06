package com.cinetrack.ui.components.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.R
import com.cinetrack.ui.components.FlickTroveSwitch
import com.cinetrack.ui.viewmodel.SettingsViewModel
import com.cinetrack.util.VibrationHelper
import com.cinetrack.ui.utils.bounceClick

@Composable
fun SettingsUILayoutSection(
    settingsViewModel: SettingsViewModel,
    currentAccentColor: Color,
    vibrationEnabled: Boolean,
    onShowBadgesInfo: () -> Unit
) {
    val context = LocalContext.current
    val showFolderBookmarks by settingsViewModel.showFolderBookmarks.collectAsStateWithLifecycle()
    val showBadges by settingsViewModel.showBadges.collectAsStateWithLifecycle()
    val showLayoutToggle by settingsViewModel.showLayoutToggle.collectAsStateWithLifecycle()
    val showSplitReleasesHome by settingsViewModel.showSplitReleasesHome.collectAsStateWithLifecycle()
    val useMovieLogo by settingsViewModel.useMovieLogo.collectAsStateWithLifecycle()
    val contentLanguage by settingsViewModel.contentLanguage.collectAsStateWithLifecycle()
    val defaultStartTab by settingsViewModel.defaultStartTab.collectAsStateWithLifecycle()

    SettingsSection(
        title = stringResource(R.string.settings_ui_layout),
        icon = ImageVector.vectorResource(id = R.drawable.ic_interfaccia)
    ) {
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_segnalibro),
            title = stringResource(R.string.settings_folder_bookmarks),
            description = stringResource(R.string.settings_folder_bookmarks_desc),
            trailing = {
                FlickTroveSwitch(
                    checked = showFolderBookmarks,
                    onCheckedChange = { 
                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                        settingsViewModel.toggleFolderBookmarks(it) 
                    },
                    accentColor = currentAccentColor
                )
            },
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                settingsViewModel.toggleFolderBookmarks(!showFolderBookmarks)
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_temi),
            title = stringResource(R.string.settings_badges),
            description = stringResource(R.string.settings_badges_desc),
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { 
                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                        onShowBadgesInfo() 
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Info Badge",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    FlickTroveSwitch(
                        checked = showBadges,
                        onCheckedChange = { 
                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                            settingsViewModel.toggleBadges(it) 
                        },
                        accentColor = currentAccentColor
                    )
                }
            },
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                settingsViewModel.toggleBadges(!showBadges)
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_grid),
            title = stringResource(R.string.settings_layout_toggle),
            description = stringResource(R.string.settings_layout_toggle_desc),
            trailing = {
                FlickTroveSwitch(
                    checked = showLayoutToggle,
                    onCheckedChange = { 
                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                        settingsViewModel.toggleLayoutToggle(it) 
                    },
                    accentColor = currentAccentColor
                )
            },
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                settingsViewModel.toggleLayoutToggle(!showLayoutToggle)
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_strisce),
            title = stringResource(R.string.settings_split_home),
            description = stringResource(R.string.settings_split_home_desc),
            trailing = {
                FlickTroveSwitch(
                    checked = showSplitReleasesHome,
                    onCheckedChange = { 
                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                        settingsViewModel.toggleSplitReleasesHome(it) 
                    },
                    accentColor = currentAccentColor
                )
            },
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                settingsViewModel.toggleSplitReleasesHome(!showSplitReleasesHome)
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_ciak),
            title = stringResource(R.string.settings_use_movie_logo),
            description = stringResource(R.string.settings_use_movie_logo_desc),
            trailing = {
                FlickTroveSwitch(
                    checked = useMovieLogo,
                    onCheckedChange = { 
                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                        settingsViewModel.toggleUseMovieLogo(it) 
                    },
                    accentColor = currentAccentColor
                )
            },
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                settingsViewModel.toggleUseMovieLogo(!useMovieLogo)
            }
        )
        // App Language
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_world),
            title = stringResource(R.string.settings_language),
            description = stringResource(R.string.settings_language_desc),
            trailing = { },
            onClick = { },
            customContent = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(
                        Triple("system", stringResource(R.string.settings_language_system), contentLanguage == "system"),
                        Triple("en", stringResource(R.string.settings_language_en), contentLanguage == "en"),
                        Triple("it", stringResource(R.string.settings_language_it), contentLanguage == "it")
                    )
                    options.forEach { (value, label, isSelected) ->
                        key(value) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) currentAccentColor else Color.White.copy(alpha = 0.05f))
                                    .bounceClick { 
                                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                        if (contentLanguage != value) {
                                            settingsViewModel.updateContentLanguage(value) {
                                                var actContext = context
                                                while (actContext is android.content.ContextWrapper && actContext !is android.app.Activity) {
                                                    actContext = (actContext as android.content.ContextWrapper).baseContext
                                                }
                                                (actContext as? android.app.Activity)?.recreate()
                                            }
                                        }
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
            }
        )
        // Start Screen
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_home),
            title = stringResource(R.string.settings_default_start_tab),
            description = stringResource(R.string.settings_default_start_tab_desc),
            trailing = { },
            onClick = { },
            customContent = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(
                        Triple("home", stringResource(R.string.settings_default_start_home), defaultStartTab == "home"),
                        Triple("visti", stringResource(R.string.settings_default_start_visti), defaultStartTab == "visti")
                    )
                    options.forEach { (value, label, isSelected) ->
                        key(value) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) currentAccentColor else Color.White.copy(alpha = 0.05f))
                                    .bounceClick { 
                                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                        if (defaultStartTab != value) {
                                            settingsViewModel.setDefaultStartTab(value)
                                        }
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
            }
        )
    }
}

@Composable
fun SettingsLanguageSection(
    settingsViewModel: SettingsViewModel,
    currentAccentColor: Color,
    vibrationEnabled: Boolean
) {
    val context = LocalContext.current
    val contentLanguage by settingsViewModel.contentLanguage.collectAsStateWithLifecycle()

    SettingsItem(
        icon = ImageVector.vectorResource(id = R.drawable.ic_world),
        title = stringResource(R.string.settings_language),
        description = stringResource(R.string.settings_language_desc),
        trailing = { },
        onClick = { },
        customContent = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(
                    Triple("system", stringResource(R.string.settings_language_system), contentLanguage == "system"),
                    Triple("en", stringResource(R.string.settings_language_en), contentLanguage == "en"),
                    Triple("it", stringResource(R.string.settings_language_it), contentLanguage == "it")
                )
                options.forEach { (value, label, isSelected) ->
                    key(value) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) currentAccentColor else Color.White.copy(alpha = 0.05f))
                                .bounceClick { 
                                    if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                    if (contentLanguage != value) {
                                        settingsViewModel.updateContentLanguage(value) {
                                            var actContext = context
                                            while (actContext is android.content.ContextWrapper && actContext !is android.app.Activity) {
                                                actContext = (actContext as android.content.ContextWrapper).baseContext
                                            }
                                            (actContext as? android.app.Activity)?.recreate()
                                        }
                                    }
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
        }
    )
}

@Composable
fun SettingsStartTabSection(
    settingsViewModel: SettingsViewModel,
    currentAccentColor: Color,
    vibrationEnabled: Boolean
) {
    val context = LocalContext.current
    val defaultStartTab by settingsViewModel.defaultStartTab.collectAsStateWithLifecycle()

    SettingsItem(
        icon = ImageVector.vectorResource(id = R.drawable.ic_home),
        title = stringResource(R.string.settings_default_start_tab),
        description = stringResource(R.string.settings_default_start_tab_desc),
        trailing = { },
        onClick = { },
        customContent = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(
                    Triple("home", stringResource(R.string.settings_default_start_home), defaultStartTab == "home"),
                    Triple("visti", stringResource(R.string.settings_default_start_visti), defaultStartTab == "visti")
                )
                options.forEach { (value, label, isSelected) ->
                    key(value) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) currentAccentColor else Color.White.copy(alpha = 0.05f))
                                .bounceClick { 
                                    if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                    if (defaultStartTab != value) {
                                        settingsViewModel.setDefaultStartTab(value)
                                    }
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
        }
    )
}

@Composable
fun SettingsAestheticsSection(
    settingsViewModel: SettingsViewModel,
    currentAccentColor: Color,
    vibrationEnabled: Boolean,
    onShowColorDialog: () -> Unit
) {
    val context = LocalContext.current
    val dynamicAppIconEnabled by settingsViewModel.dynamicAppIconEnabled.collectAsStateWithLifecycle()
    val showAppEntryAnimation by settingsViewModel.showAppEntryAnimation.collectAsStateWithLifecycle()
    val appTheme by settingsViewModel.appTheme.collectAsStateWithLifecycle()

    SettingsSection(
        title = stringResource(R.string.settings_aesthetics),
        icon = ImageVector.vectorResource(id = R.drawable.ic_palette)
    ) {
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_palette),
            title = stringResource(R.string.settings_accent_color),
            description = stringResource(R.string.settings_accent_color_desc),
            trailing = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(currentAccentColor.copy(alpha = 0.15f))
                            .border(
                                width = 1.dp,
                                color = currentAccentColor.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(currentAccentColor)
                            .border(
                                width = 1.5.dp,
                                color = Color.White.copy(alpha = 0.8f),
                                shape = CircleShape
                            )
                    )
                }
            },
            onClick = { 
                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                onShowColorDialog() 
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_smartphone_magia),
            title = stringResource(R.string.settings_dynamic_icon),
            description = stringResource(R.string.settings_dynamic_icon_desc),
            trailing = {
                FlickTroveSwitch(
                    checked = dynamicAppIconEnabled,
                    onCheckedChange = { 
                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                        settingsViewModel.toggleDynamicAppIcon(it) 
                    },
                    accentColor = currentAccentColor
                )
            },
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                settingsViewModel.toggleDynamicAppIcon(!dynamicAppIconEnabled)
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
            title = stringResource(R.string.settings_entry_animation),
            description = stringResource(R.string.settings_entry_animation_desc),
            trailing = {
                FlickTroveSwitch(
                    checked = showAppEntryAnimation,
                    onCheckedChange = { 
                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                        settingsViewModel.toggleAppEntryAnimation(it) 
                    },
                    accentColor = currentAccentColor
                )
            },
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                settingsViewModel.toggleAppEntryAnimation(!showAppEntryAnimation)
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_temi),
            title = stringResource(R.string.settings_amoled_bg),
            description = stringResource(R.string.settings_amoled_bg_desc),
            trailing = {
                FlickTroveSwitch(
                    checked = appTheme == "AMOLED",
                    onCheckedChange = { 
                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                        settingsViewModel.updateAppTheme(if (it) "AMOLED" else "System")
                    },
                    accentColor = currentAccentColor
                )
            },
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                settingsViewModel.updateAppTheme(if (appTheme == "AMOLED") "System" else "AMOLED")
            }
        )
    }
}

@Composable
fun SettingsAccessibilitySection(
    settingsViewModel: SettingsViewModel,
    currentAccentColor: Color,
    vibrationEnabled: Boolean
) {
    val context = LocalContext.current
    val titleTextSizeMultiplier by settingsViewModel.titleTextSizeMultiplier.collectAsStateWithLifecycle()
    val advancedVisualEffectsEnabled by settingsViewModel.advancedVisualEffectsEnabled.collectAsStateWithLifecycle()

    SettingsSection(
        title = stringResource(R.string.settings_accessibility),
        icon = ImageVector.vectorResource(id = R.drawable.ic_accessibilita)
    ) {
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_maiuscolo),
            title = stringResource(R.string.settings_title_size),
            description = stringResource(R.string.settings_title_size_desc),
            trailing = { },
            onClick = { },
            customContent = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(
                        Triple(0.8f, stringResource(R.string.settings_size_small), titleTextSizeMultiplier == 0.8f),
                        Triple(1.0f, stringResource(R.string.settings_size_medium), titleTextSizeMultiplier == 1.0f),
                        Triple(1.2f, stringResource(R.string.settings_size_large), titleTextSizeMultiplier == 1.2f)
                    )
                    options.forEach { (value, label, isSelected) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) currentAccentColor else Color.White.copy(alpha = 0.05f))
                                .bounceClick { 
                                    if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                    settingsViewModel.updateTitleTextSizeMultiplier(value) 
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
            icon = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
            title = stringResource(R.string.settings_visual_effects),
            description = stringResource(R.string.settings_visual_effects_desc),
            trailing = {
                FlickTroveSwitch(
                    checked = advancedVisualEffectsEnabled,
                    onCheckedChange = { 
                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                        settingsViewModel.toggleAdvancedVisualEffects(it) 
                    },
                    accentColor = currentAccentColor
                )
            },
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                settingsViewModel.toggleAdvancedVisualEffects(!advancedVisualEffectsEnabled)
            }
        )
    }
}

@Composable
fun SettingsNotificationsSection(
    settingsViewModel: SettingsViewModel,
    currentAccentColor: Color,
    vibrationEnabled: Boolean,
    notificationPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    val context = LocalContext.current
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()

    SettingsSection(
        title = stringResource(R.string.settings_notifications_vibration),
        icon = ImageVector.vectorResource(id = R.drawable.ic_bell_piena)
    ) {
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_bell_vibra),
            title = stringResource(R.string.settings_enable_notifications),
            description = stringResource(R.string.settings_enable_notifications_desc),
            trailing = {
                FlickTroveSwitch(
                    checked = notificationsEnabled,
                    onCheckedChange = { checked ->
                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                        if (checked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                settingsViewModel.toggleNotifications(true)
                            }
                        } else {
                            settingsViewModel.toggleNotifications(false)
                        }
                    },
                    accentColor = currentAccentColor
                )
            },
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                val newState = !notificationsEnabled
                if (newState) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        settingsViewModel.toggleNotifications(true)
                    }
                } else {
                    settingsViewModel.toggleNotifications(false)
                }
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_smartphone_vibra),
            title = stringResource(R.string.settings_haptic_feedback),
            description = stringResource(R.string.settings_haptic_feedback_desc),
            trailing = {
                Switch(
                    checked = vibrationEnabled,
                    onCheckedChange = { checked ->
                        if (checked) VibrationHelper.vibrateTick(context)
                        settingsViewModel.toggleVibration(checked) 
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = currentAccentColor,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            },
            onClick = {
                val newVib = !vibrationEnabled
                if (newVib) VibrationHelper.vibrateTick(context)
                settingsViewModel.toggleVibration(newVib)
            }
        )
    }
}
