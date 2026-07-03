package com.cinetrack.ui.screens

import com.cinetrack.ui.components.shared.ColorWheel
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.util.toComposeColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.window.DialogProperties

import com.cinetrack.ui.theme.OnSurfaceMuted
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.theme.NeonTeal
import com.cinetrack.ui.theme.NeonPink
import com.cinetrack.ui.theme.NeonPurple
import com.cinetrack.ui.theme.NeonAmber
import com.cinetrack.ui.theme.NeonBlue
import com.cinetrack.ui.theme.*
import com.cinetrack.ui.viewmodel.*
import com.cinetrack.ui.components.*
import com.cinetrack.ui.components.settings.*
import com.cinetrack.ui.components.CinematicBackground
import com.cinetrack.ui.components.glass.*
import com.cinetrack.ui.components.shared.*
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.cinetrack.util.VibrationHelper
import com.google.firebase.auth.FirebaseAuth
import cafe.adriel.voyager.hilt.getViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import coil.compose.AsyncImage
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.ui.graphics.*
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.cinetrack.ui.LocalAppPadding

object SettingsTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(id = R.string.settings_tab_title)
            return remember(title) {
                TabOptions(
                    index = 6u,
                    title = title,
                    icon = null
                )
            }
        }

    @Composable
    override fun Content() {
        var currentContext = androidx.compose.ui.platform.LocalContext.current
        while (currentContext is android.content.ContextWrapper && currentContext !is androidx.activity.ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? androidx.activity.ComponentActivity

        val viewModel = if (activity != null) {
            androidx.hilt.navigation.compose.hiltViewModel<AuthViewModel>(activity)
        } else {
            androidx.hilt.navigation.compose.hiltViewModel<AuthViewModel>()
        }
        val settingsViewModel = if (activity != null) {
            androidx.hilt.navigation.compose.hiltViewModel<SettingsViewModel>(activity)
        } else {
            androidx.hilt.navigation.compose.hiltViewModel<SettingsViewModel>()
        }
        
        val paddingValues = LocalAppPadding.current
        val hazeState = com.cinetrack.ui.LocalHazeState.current
        val navigator = LocalNavigator.currentOrThrow.parent ?: LocalNavigator.currentOrThrow

        val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
        val pendingReveal by settingsViewModel.pendingReveal.collectAsStateWithLifecycle()
        
        val graphicsLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
        var capturedImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
        var isScreenshotReady by remember { mutableStateOf(false) }
        val animatedRadius = remember { androidx.compose.animation.core.Animatable(0f) }
        
        val currentBackgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background
        var oldBackgroundColor by remember { mutableStateOf(androidx.compose.ui.graphics.Color.Black) }

        LaunchedEffect(pendingReveal) {
            if (pendingReveal != null) {
                val (colorName, origin) = pendingReveal!!
                
                // Save the old background color before applying the new theme
                oldBackgroundColor = currentBackgroundColor
                
                // 1. Capture the image NOW, before we change the theme or structure
                try {
                    capturedImage = graphicsLayer.toImageBitmap()
                } catch (e: Exception) {
                    // Ignore
                }
                
                // 2. Reset the radius immediately BEFORE we show the top layer
                animatedRadius.snapTo(0f)
                
                // 3. Enable screenshot drawing and clipping
                isScreenshotReady = true
                
                // 4. Apply the new theme
                settingsViewModel.applyPendingTheme()
                
                // 5. Wait a moment to hide the heavy theme recomposition lag
                kotlinx.coroutines.delay(50)
                
                // 6. Animate the circle
                // We calculate max radius safely here using arbitrary large value 
                // (2500dp converted to px is approx 7500 on xxhdpi, 10000 is safer)
                animatedRadius.animateTo(
                    targetValue = 10000f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 900, 
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                )
                
                // 7. Cleanup
                val finalColor = pendingReveal!!.first
                capturedImage = null
                isScreenshotReady = false
                settingsViewModel.clearPendingReveal()
                
                // 8. Update Icon AFTER animation and cleanup
                settingsViewModel.applyPendingIcon(finalColor)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Bottom Layer: Old UI Screenshot
            if (isScreenshotReady && capturedImage != null) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(oldBackgroundColor)
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = capturedImage!!,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            // Top Layer: The actual UI (records when not animating, clips when animating)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        if (!isScreenshotReady) {
                            graphicsLayer.record { this@drawWithContent.drawContent() }
                        }
                        drawContent()
                    }
                    .graphicsLayer {
                        if (isScreenshotReady && pendingReveal != null) {
                            val origin = pendingReveal!!.second
                            clip = true
                            shape = object : androidx.compose.ui.graphics.Shape {
                                override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
                                    val p = androidx.compose.ui.graphics.Path().apply {
                                        addOval(androidx.compose.ui.geometry.Rect(
                                            origin.x - animatedRadius.value, 
                                            origin.y - animatedRadius.value, 
                                            origin.x + animatedRadius.value, 
                                            origin.y + animatedRadius.value
                                        ))
                                    }
                                    return androidx.compose.ui.graphics.Outline.Generic(p)
                                }
                            }
                        }
                    }
                    .then(
                        if (isScreenshotReady) Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                        else Modifier
                    )
            ) {
                SettingsScreenContent(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    paddingValues = paddingValues,
                    hazeState = hazeState,
                    scrollState = scrollState,
                    onLoggedOut = {
                        navigator.replaceAll(com.cinetrack.ui.screens.LoginScreen())
                    },
                    onLoginClick = {
                        navigator.push(com.cinetrack.ui.screens.LoginScreen())
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreenContent(
    viewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    paddingValues: PaddingValues,
    hazeState: HazeState? = null,
    scrollState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    onLoggedOut: () -> Unit,
    onLoginClick: () -> Unit
) {
    val activeHazeState = hazeState ?: remember { HazeState() }
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Procediamo comunque alla sincronizzazione. Se il permesso è negato su Android 14+, 
        // WorkManager non mostrerà la notifica, ma potremo gestirlo o il worker continuerà senza di essa
        settingsViewModel.syncTraktNow()
    }
    
    // State from SettingsViewModel
    val accentColorName by settingsViewModel.accentColor.collectAsStateWithLifecycle()
    val currentAccentColor = remember(accentColorName) {
        when (accentColorName) {
            "Pink" -> NeonPink
            "Purple" -> NeonPurple
            "Amber" -> NeonAmber
            "Blue" -> NeonBlue
            "Teal" -> NeonTeal
            else -> accentColorName.toComposeColor(NeonTeal)
        }
    }
    val dynamicAppIconEnabled by settingsViewModel.dynamicAppIconEnabled.collectAsStateWithLifecycle()
    val showFolderBookmarks by settingsViewModel.showFolderBookmarks.collectAsStateWithLifecycle()
    val showBadges by settingsViewModel.showBadges.collectAsStateWithLifecycle()
    val disabledBadges by settingsViewModel.disabledBadges.collectAsStateWithLifecycle()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val syncWorkInfo by settingsViewModel.syncWorkInfo.collectAsStateWithLifecycle()
    val libraryDetailsSyncWorkInfo by settingsViewModel.libraryDetailsSyncWorkInfo.collectAsStateWithLifecycle()
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val showLayoutToggle by settingsViewModel.showLayoutToggle.collectAsStateWithLifecycle()
    val showSplitReleasesHome by settingsViewModel.showSplitReleasesHome.collectAsStateWithLifecycle()
    val showAppEntryAnimation by settingsViewModel.showAppEntryAnimation.collectAsStateWithLifecycle()
    val useMovieLogo by settingsViewModel.useMovieLogo.collectAsStateWithLifecycle()

    val appTheme by settingsViewModel.appTheme.collectAsStateWithLifecycle()
    val contentLanguage by settingsViewModel.contentLanguage.collectAsStateWithLifecycle()
    val defaultStartTab by settingsViewModel.defaultStartTab.collectAsStateWithLifecycle()
    val imageQuality by settingsViewModel.imageQuality.collectAsStateWithLifecycle()
    val titleTextSizeMultiplier by settingsViewModel.titleTextSizeMultiplier.collectAsStateWithLifecycle()
    val advancedVisualEffectsEnabled by settingsViewModel.advancedVisualEffectsEnabled.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showBadgesInfoDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showCacheConfirm by remember { mutableStateOf(false) }
    var showDeepSyncConfirm by remember { mutableStateOf(false) }

    val topPadding = paddingValues.calculateTopPadding() + androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 90.dp
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showExternalMigrationDialog by remember { mutableStateOf(false) }

    val isBackupLoading by settingsViewModel.isBackupLoading.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Runtime permission launcher for notifications (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        settingsViewModel.toggleNotifications(isGranted)
    }

    val anyDialogVisible = showDeleteDialog || showColorDialog || showFeedbackDialog || 
                           showCacheConfirm || showLogoutConfirm || showBackupDialog || 
                           showExternalMigrationDialog || showBadgesInfoDialog || isBackupLoading ||
                           showDeepSyncConfirm || false

    var cacheSizeString by remember { mutableStateOf("0 MB") }
    
    @kotlin.OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun updateCacheSize() {
        scope.launch(Dispatchers.IO) {
            val sizeBytes = context.imageLoader.diskCache?.size ?: 0L
            val sizeMb = sizeBytes / (1024f * 1024f)
            cacheSizeString = String.format(java.util.Locale.US, "%.1f MB", sizeMb)
        }
    }

    LaunchedEffect(Unit) {
        updateCacheSize()
    }

    val settingsDimAlpha by animateFloatAsState(
        targetValue = if (anyDialogVisible) 0.7f else 0f,
        animationSpec = tween(200),
        label = "settingsDimAlpha"
    )

    LaunchedEffect(anyDialogVisible) {
        settingsViewModel.setAnyDialogOpen(anyDialogVisible)
    }

    LaunchedEffect(Unit) {
        settingsViewModel.closeDialogsEvent.collect {
            focusManager.clearFocus()
            showDeleteDialog = false
            showColorDialog = false
            showFeedbackDialog = false
            showBadgesInfoDialog = false
            showCacheConfirm = false
            showLogoutConfirm = false
            showBackupDialog = false
            showExternalMigrationDialog = false
        }
    }

    // File Pickers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val json = settingsViewModel.getBackupData()
                if (json != null) {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { stream ->
                            OutputStreamWriter(stream).use { writer ->
                                writer.write(json)
                            }
                        }
                    } catch (e: Exception) {
                        // Error handling is managed by ViewModel through ActionFeedbackManager
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.bufferedReader().use { reader -> reader.readText() }
                    settingsViewModel.restoreData(json)
                }
            } catch (e: Exception) {
                // Error handling is managed by ViewModel
            }
        }
    }

    val externalMigrationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader().use { reader -> reader.readText() }
                    settingsViewModel.migrateExternalData(content)
                }
            } catch (e: Exception) {
                // Error handling is managed by ViewModel
            }
        }
    }

    // If logout or delete succeeds, navigate back to login
    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            onLoggedOut()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        CinematicBackground(modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(
                    state = activeHazeState,
                    style = HazeStyles.PremiumDark
                )
        ) {
            // Content Layer - Recorded by localHazeState
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { focusManager.clearFocus() }
                        },
                    contentPadding = PaddingValues(
                        top = topPadding + 48.dp, // Aumentato padding superiore
                        bottom = paddingValues.calculateBottomPadding() + 32.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                 ) {
                    // Section: Interfaccia e Layout
                    item {
                        SettingsUILayoutSection(
                            settingsViewModel = settingsViewModel,
                            currentAccentColor = currentAccentColor,
                            vibrationEnabled = vibrationEnabled,
                            onShowBadgesInfo = { showBadgesInfoDialog = true }
                        )
                    }

                    // Section: Estetica
                    item {
                        SettingsAestheticsSection(
                            settingsViewModel = settingsViewModel,
                            currentAccentColor = currentAccentColor,
                            vibrationEnabled = vibrationEnabled,
                            onShowColorDialog = { showColorDialog = true }
                        )
                    }

                    // Section: Accessibilità
                    item {
                        SettingsAccessibilitySection(
                            settingsViewModel = settingsViewModel,
                            currentAccentColor = currentAccentColor,
                            vibrationEnabled = vibrationEnabled
                        )
                    }
                    
                    // Section: Notifiche e Vibrazione
                    item {
                        SettingsNotificationsSection(
                            settingsViewModel = settingsViewModel,
                            currentAccentColor = currentAccentColor,
                            vibrationEnabled = vibrationEnabled,
                            notificationPermissionLauncher = notificationPermissionLauncher
                        )
                    }

                    // Section: Archiviazione e Rete
                    item {
                        SettingsImagesStorageSection(
                            settingsViewModel = settingsViewModel,
                            currentAccentColor = currentAccentColor,
                            vibrationEnabled = vibrationEnabled,
                            cacheSizeString = cacheSizeString,
                            onShowCacheConfirm = { showCacheConfirm = true }
                        )
                    }

                    // Section: Sincronizzazione e Backup
                    item {
                        SettingsSyncBackupSection(
                            settingsViewModel = settingsViewModel,
                            currentAccentColor = currentAccentColor,
                            vibrationEnabled = vibrationEnabled,
                            user = user,
                            permissionLauncher = permissionLauncher,
                            onShowExternalMigrationDialog = { showExternalMigrationDialog = true },
                            onShowBackupDialog = { showBackupDialog = true },
                            onShowDeepSyncConfirm = { showDeepSyncConfirm = true }
                        )
                    }

                    // Section: Account
                    item {
                        SettingsAccountSection(
                            user = user,
                            currentAccentColor = currentAccentColor,
                            vibrationEnabled = vibrationEnabled,
                            onLoginClick = onLoginClick,
                            onShowLogoutConfirm = { showLogoutConfirm = true },
                            onShowDeleteDialog = { showDeleteDialog = true }
                        )
                    }

                    // Section: Supporto
                    item {
                        SettingsSupportSection(
                            vibrationEnabled = vibrationEnabled,
                            onShowFeedbackDialog = { showFeedbackDialog = true }
                        )
                    }

                    item { SettingsFooterSection() }

                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }

            // Centralized darkening overlay inside Haze capture
            if (settingsDimAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = settingsDimAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { 
                                // Dismiss all possible dialogs on background tap
                                focusManager.clearFocus()
                                showDeleteDialog = false
                                showColorDialog = false
                                showFeedbackDialog = false
                                showBadgesInfoDialog = false
                                showCacheConfirm = false
                                showLogoutConfirm = false
                                showBackupDialog = false
                                showExternalMigrationDialog = false
                            }
                        )
                )
            }
        }
        } // End of haze capture Box

        // Custom Glassy Confirm Dialogs
        DeleteAccountDialog(
            visible = showDeleteDialog,
            activeHazeState = activeHazeState,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteAccount { _ ->
                    // Navigation handled by LaunchedEffect(authState)
                }
            }
        )

        ClearCacheConfirmDialog(
            visible = showCacheConfirm,
            activeHazeState = activeHazeState,
            onDismiss = { showCacheConfirm = false },
            onConfirm = {
                settingsViewModel.clearImageCache()
                updateCacheSize()
                showCacheConfirm = false
            }
        )

        DeepSyncConfirmDialog(
            visible = showDeepSyncConfirm,
            activeHazeState = activeHazeState,
            onDismiss = { showDeepSyncConfirm = false },
            onConfirm = {
                settingsViewModel.syncLibraryDetails()
                showDeepSyncConfirm = false
            }
        )

        BadgesInfoDialog(
            visible = showBadgesInfoDialog,
            activeHazeState = activeHazeState,
            disabledBadges = disabledBadges,
            onToggleBadge = { text, enabled -> settingsViewModel.toggleBadgeEnabled(text, enabled) },
            onDismiss = { showBadgesInfoDialog = false }
        )

        LogoutConfirmDialog(
            visible = showLogoutConfirm,
            activeHazeState = activeHazeState,
            onDismiss = { showLogoutConfirm = false },
            onConfirm = {
                showLogoutConfirm = false
                viewModel.logout()
            }
        )





        SettingsColorSelectionDialog(
            visible = showColorDialog,
            activeHazeState = activeHazeState,
            current = accentColorName,
            onDismiss = { showColorDialog = false },
            onSelect = { colorName, origin ->
                scope.launch {
                    kotlinx.coroutines.delay(350)
                    settingsViewModel.updateAccentColor(colorName, origin)
                }
            }
        )

        val isFeedbackLoading by settingsViewModel.isFeedbackLoading.collectAsStateWithLifecycle()
        SettingsFeedbackDialog(
            visible = showFeedbackDialog,
            activeHazeState = activeHazeState,
            initialEmail = user?.email ?: "",
            isLoading = isFeedbackLoading,
            onDismiss = { if (!isFeedbackLoading) showFeedbackDialog = false },
            onSubmit = { t, d, r, e ->
                settingsViewModel.sendFeedback(t, d, r, e) {
                    showFeedbackDialog = false
                }
            }
        )

        SettingsBackupDialog(
            visible = showBackupDialog,
            activeHazeState = activeHazeState,
            isBackupLoading = isBackupLoading,
            onDismiss = { if (!isBackupLoading) showBackupDialog = false },
            onExport = { 
                showBackupDialog = false
                exportLauncher.launch("FlickTrove_Backup_${System.currentTimeMillis()}.json")
            },
            onImport = { 
                showBackupDialog = false
                importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
            }
        )

        SettingsExternalMigrationDialog(
            visible = showExternalMigrationDialog,
            activeHazeState = activeHazeState,
            onDismiss = { showExternalMigrationDialog = false },
            onImport = { 
                showExternalMigrationDialog = false
                externalMigrationLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "application/octet-stream", "*/*")) 
            }
        )

        SettingsLoadingOverlay(
            visible = isBackupLoading,
            activeHazeState = activeHazeState
        )
    }
}

