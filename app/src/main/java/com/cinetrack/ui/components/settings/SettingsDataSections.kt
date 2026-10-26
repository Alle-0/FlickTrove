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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
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
import com.cinetrack.ui.viewmodel.SettingsViewModel
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.VibrationHelper
import com.cinetrack.util.Keys
import com.google.firebase.auth.FirebaseUser
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.components.common.CategoryTabSelector

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
            customContent = {
                val qualityOptions = listOf(
                    stringResource(R.string.quality_low),
                    stringResource(R.string.quality_medium),
                    stringResource(R.string.quality_high)
                )
                val selectedIndex = when (imageQuality) {
                    ImageQuality.LOW -> 0
                    ImageQuality.MEDIUM -> 1
                    ImageQuality.HIGH -> 2
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    CategoryTabSelector(
                        options = qualityOptions,
                        selectedIndex = selectedIndex,
                        onOptionClick = { index ->
                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                            val newQuality = when (index) {
                                0 -> ImageQuality.LOW
                                1 -> ImageQuality.MEDIUM
                                else -> ImageQuality.HIGH
                            }
                            settingsViewModel.updateImageQuality(newQuality)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        fillWidth = true,
                        indicatorColor = currentAccentColor,
                        uppercase = false
                    )
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
fun SettingsAccountSection(
    user: FirebaseUser?,
    currentAccentColor: Color,
    vibrationEnabled: Boolean,
    onLoginClick: () -> Unit,
    onShowLogoutConfirm: () -> Unit,
    onShowDeleteDialog: () -> Unit,
    onShowWipeSelectionDialog: () -> Unit,
    onShowCleanupRewatchesDialog: () -> Unit,
    onLinkGoogleClick: () -> Unit,
    blockedUsersCount: Int = 0,
    onShowBlockedUsersDialog: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isGuest = user?.isAnonymous == true
    val hasGoogle = user?.providerData?.any { it.providerId == "google.com" } == true
    val email = when {
        isGuest -> stringResource(R.string.settings_guest)
        hasGoogle -> user.providerData
            .firstOrNull { it.providerId == "google.com" }
            ?.email?.takeIf { it.isNotBlank() }
            ?: user.email?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.settings_guest)
        else -> user?.email?.takeIf { it.isNotBlank() } ?: stringResource(R.string.settings_guest)
    }

    SettingsSection(
        title = stringResource(R.string.settings_account),
        subtitle = email,
        icon = ImageVector.vectorResource(id = R.drawable.ic_persona)
    ) {
        Text(
            text = stringResource(R.string.settings_group_account_security).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 11.sp
            ),
            color = currentAccentColor.copy(alpha = 0.85f),
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
        )

        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_exit),
            title = if (!isGuest) stringResource(R.string.settings_dialog_logout_title) else stringResource(R.string.settings_login),
            description = if (!isGuest) stringResource(R.string.settings_logout_desc) else stringResource(R.string.settings_login_desc),
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
        
        if (!isGuest && !hasGoogle) {
            SettingsItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_google),
                title = stringResource(R.string.settings_link_google),
                description = stringResource(R.string.settings_link_google_desc),
                tint = Color.Unspecified,
                onClick = {
                    if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                    onLinkGoogleClick()
                }
            )
        }

        if (!isGuest && onShowBlockedUsersDialog != null) {
            SettingsItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_block),
                title = stringResource(R.string.settings_blocked_users_title),
                description = stringResource(R.string.settings_blocked_users_desc),
                tint = Color.White,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (blockedUsersCount > 0) {
                            val countStr = "$blockedUsersCount"
                            Box(
                                modifier = Modifier
                                    .height(20.dp)
                                    .defaultMinSize(minWidth = 20.dp)
                                    .clip(CircleShape)
                                    .background(currentAccentColor.copy(alpha = 0.18f))
                                    .border(1.dp, currentAccentColor.copy(alpha = 0.35f), CircleShape)
                                    .padding(horizontal = if (countStr.length > 1) 5.dp else 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = countStr,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        lineHeight = 11.sp
                                    ),
                                    color = currentAccentColor
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                onClick = {
                    if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                    onShowBlockedUsersDialog()
                }
            )
        }
        
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.settings_group_data_danger).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 11.sp
            ),
            color = Color(0xFFFF5252).copy(alpha = 0.85f),
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
        )

        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_svuota_trash),
            title = stringResource(id = R.string.settings_cleanup_rewatches_title),
            description = stringResource(id = R.string.settings_cleanup_rewatches_desc),
            tint = Color(0xFFFFA000),
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                onShowCleanupRewatchesDialog()
            }
        )

        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_trash),
            title = stringResource(id = R.string.settings_dialog_wipe_data_title),
            description = stringResource(id = R.string.settings_wipe_data_desc),
            tint = Color(0xFFFF9800),
            onClick = {
                if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                onShowWipeSelectionDialog()
            }
        )
        
        if (!isGuest && user != null) {
            SettingsItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_trash),
                title = stringResource(R.string.settings_delete_account),
                description = stringResource(R.string.settings_delete_account_desc),
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
            icon = ImageVector.vectorResource(id = R.drawable.ic_heart),
            title = "Buy me a Coffee ☕",
            description = "Support the developer with a donation via Paypal",
            isExternal = true,
            tint = Color(0xFFFFA000),
            onClick = { 
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                uriHandler.openUri("https://paypal.me/AlessandroBasile0") 
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_reddit),
            title = "Community",
            description = "Join the discussion on r/FlickTrove",
            isExternal = true,
            tint = Color(0xFFFF4500),
            onClick = { 
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                uriHandler.openUri("https://www.reddit.com/r/FlickTrove") 
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_documento),
            title = stringResource(R.string.settings_terms_service),
            isExternal = true,
            onClick = { 
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                uriHandler.openUri("https://alle-0.github.io/FlickTrove/terms.html") 
            }
        )
        SettingsItem(
            icon = ImageVector.vectorResource(id = R.drawable.ic_scudo_privacy),
            title = stringResource(R.string.settings_privacy_policy),
            isExternal = true,
            onClick = { 
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                uriHandler.openUri("https://alle-0.github.io/FlickTrove/privacy.html") 
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        var creditsExpanded by remember { mutableStateOf(false) }
        val rotation by animateFloatAsState(
            targetValue = if (creditsExpanded) 90f else 0f,
            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            label = "creditsArrowRotation"
        )

        // Pill-shaped card that expands vertically (Concentric 16/16 with 32.dp outer container)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.04f),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                // Header row — clickable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .bounceClick(scaleDown = 0.98f) {
                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                            creditsExpanded = !creditsExpanded
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Credits & Attributions",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                        contentDescription = null,
                        tint = OnSurfaceMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(rotation)
                    )
                }

                // Expandable content — slides down vertically
                AnimatedVisibility(
                    visible = creditsExpanded,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(durationMillis = 240)),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(durationMillis = 180))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AttributionRow(
                            brand = "GIPHY",
                            text = stringResource(R.string.settings_giphy_notice)
                        )
                        AttributionRow(
                            brand = "TMDB",
                            text = stringResource(R.string.settings_tmdb_notice)
                        )
                        AttributionRow(
                            brand = "OMDb API",
                            text = stringResource(R.string.settings_omdb_notice)
                        )
                        AttributionRow(
                            brand = "TheTVDB",
                            text = stringResource(R.string.settings_tvdb_notice)
                        )
                        AttributionRow(
                            brand = "Trakt.tv",
                            text = stringResource(R.string.settings_trakt_notice)
                        )
                        AttributionRow(
                            brand = "SIMKL",
                            text = stringResource(R.string.settings_simkl_notice)
                        )
                        AttributionRow(
                            brand = "CommsUni (commsuni.tv)",
                            text = stringResource(R.string.settings_commsuni_notice)
                        )
                        AttributionRow(
                            brand = "Rotten Tomatoes",
                            text = stringResource(R.string.settings_rotten_tomatoes_notice)
                        )
                        AttributionRow(
                            brand = "Metacritic",
                            text = stringResource(R.string.settings_metacritic_notice)
                        )
                        AttributionRow(
                            brand = "Game-Icons.net",
                            text = stringResource(R.string.settings_game_icons_notice)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SettingsFooterSection(
    updateInfo: com.cinetrack.util.AppUpdateInfo? = null,
    accentColor: Color = Color(0xFF2DD4BF),
    onReplayTutorial: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (updateInfo != null && updateInfo.isUpdateAvailable) {
            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .bounceClick {
                        uriHandler.openUri(updateInfo.htmlUrl)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_rocket),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_new_version_available, updateInfo.latestVersion),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    )
                }
            }
        } else {
            Surface(
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .bounceClick {
                        uriHandler.openUri("https://github.com/Alle-0/FlickTrove/releases/latest")
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_check_updates_github),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_external_link),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Surface(
            color = Color.White.copy(alpha = 0.06f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(bottom = 14.dp)
                .bounceClick { onReplayTutorial() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.settings_review_tutorial),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }
        }

        Text(
            text = "FlickTrove v${com.cinetrack.BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            ),
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_made_with),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceMuted
            )
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_heart),
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

