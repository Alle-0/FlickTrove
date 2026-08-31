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
import com.cinetrack.ui.components.settings.WipeDataSelectionDialog
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.dialog.UnmatchedItemsModal
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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.io.File
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
fun SettingsOverlayScreen(
    viewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    paddingValues: PaddingValues,
    startX: Float,
    startY: Float,
    onBack: () -> Unit,
    onClosing: () -> Unit,
    onLoggedOut: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isMeasured by remember { mutableStateOf(false) }
    var hasRevealed by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val revealAmount = remember(hasRevealed) { androidx.compose.animation.core.Animatable(if (hasRevealed) 1f else 0f) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(isMeasured) {
        if (isMeasured && !hasRevealed) {
            revealAmount.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 800, 
                    easing = androidx.compose.animation.core.CubicBezierEasing(0.7f, 0f, 0.2f, 1f)
                )
            )
            hasRevealed = true
        }
    }

    val triggerExit = {
        if (!isClosing) {
            isClosing = true
            onClosing()
            scope.launch {
                revealAmount.animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 800, 
                        easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
                    )
                )
                onBack()
            }
        }
    }

    BackHandler(enabled = !isClosing) {
        triggerExit()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        
        if (width > 0 && !isMeasured) {
            LaunchedEffect(Unit) { isMeasured = true }
        }
        
        val center = androidx.compose.ui.geometry.Offset(startX, startY)
        val maxRadius = remember(center, width, height) {
            val distTopLeft = kotlin.math.sqrt((center.x * center.x) + (center.y * center.y).toDouble()).toFloat()
            val distTopRight = kotlin.math.sqrt(((width - center.x) * (width - center.x)) + (center.y * center.y).toDouble()).toFloat()
            val distBottomLeft = kotlin.math.sqrt((center.x * center.x) + ((height - center.y) * (height - center.y)).toDouble()).toFloat()
            val distBottomRight = kotlin.math.sqrt(((width - center.x) * (width - center.x)) + ((height - center.y) * (height - center.y)).toDouble()).toFloat()
            kotlin.math.max(kotlin.math.max(distTopLeft, distTopRight), kotlin.math.max(distBottomLeft, distBottomRight)) * 1.1f
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val radius = revealAmount.value * maxRadius
                    clip = true
                    shape = object : androidx.compose.ui.graphics.Shape {
                        override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                addOval(androidx.compose.ui.geometry.Rect(center = center, radius = radius))
                            }
                            return androidx.compose.ui.graphics.Outline.Generic(path)
                        }
                    }
                }
        ) {
            val internalHazeState = remember { dev.chrisbanes.haze.HazeState() }
            val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
            
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                SettingsScreenContent(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    paddingValues = paddingValues,
                    hazeState = internalHazeState,
                    scrollState = scrollState,
                    onLoggedOut = onLoggedOut,
                    onLoginClick = onLoginClick
                )
                
                Box(modifier = Modifier.align(Alignment.TopCenter).zIndex(10f)) {
                    com.cinetrack.ui.components.navigation.GlassyTopBar(
                        hazeState = internalHazeState,
                        title = androidx.compose.ui.res.stringResource(com.cinetrack.R.string.settings_tab_title),
                        onBackPress = triggerExit
                    )
                }
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

    val syncWorkInfo by settingsViewModel.syncWorkInfo.collectAsStateWithLifecycle()
    val updateInfo by settingsViewModel.updateInfo.collectAsStateWithLifecycle()
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
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthErrorMessage by remember { mutableStateOf<String?>(null) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showBadgesInfoDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showCacheConfirm by remember { mutableStateOf(false) }
    var showDeepSyncConfirm by remember { mutableStateOf(false) }

    val topPadding = paddingValues.calculateTopPadding() + androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 90.dp
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showWipeSelectionDialog by remember { mutableStateOf(false) }
    var showWipeLocalDataConfirm by remember { mutableStateOf(false) }
    var showWipeTotalDataConfirm by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showExternalMigrationDialog by remember { mutableStateOf(false) }

    val isBackupLoading by settingsViewModel.isBackupLoading.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    val handleGoogleLink = {
        scope.launch {
            try {
                val activityContext = generateSequence(context) {
                    (it as? android.content.ContextWrapper)?.baseContext
                }.firstOrNull { it is android.app.Activity } ?: context

                val credentialManager = androidx.credentials.CredentialManager.create(activityContext)
                val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(com.cinetrack.util.Keys.getGoogleClientId())
                    .setAutoSelectEnabled(false)
                    .build()

                val request = androidx.credentials.GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(activityContext, request)
                val credential = result.credential

                if (credential is androidx.credentials.CustomCredential && credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.linkGoogleAccount(googleIdTokenCredential.idToken)
                } else {
                    android.widget.Toast.makeText(context, "Unexpected credential type", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (e !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    android.widget.Toast.makeText(context, "Google Sign In Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Runtime permission launcher for notifications (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            settingsViewModel.toggleNotificationsReleases(false)
            settingsViewModel.toggleNotificationsSocial(false)
        }
    }

    val anyDialogVisible = showDeleteDialog || showReauthDialog || showColorDialog || showLanguageDialog || showFeedbackDialog || 
                           showCacheConfirm || showLogoutConfirm || showWipeSelectionDialog || showWipeLocalDataConfirm || showWipeTotalDataConfirm || showBackupDialog || 
                           showExternalMigrationDialog || showBadgesInfoDialog || isBackupLoading ||
                           showDeepSyncConfirm

    var showUnmatchedItemsModal by remember { mutableStateOf(false) }
    val unmatchedMovies by settingsViewModel.unmatchedMovies.collectAsStateWithLifecycle()

    BackHandler(enabled = anyDialogVisible) {
        focusManager.clearFocus()
        showDeleteDialog = false
        showReauthDialog = false
        showColorDialog = false
        showLanguageDialog = false
        showFeedbackDialog = false
        showBadgesInfoDialog = false
        showCacheConfirm = false
        showLogoutConfirm = false
        showBackupDialog = false
        showExternalMigrationDialog = false
        showDeepSyncConfirm = false
        showWipeSelectionDialog = false
        showWipeLocalDataConfirm = false
        showWipeTotalDataConfirm = false
        var showUnmatchedItemsModal = false
    }

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
            showReauthDialog = false
            showColorDialog = false
            showLanguageDialog = false
            showFeedbackDialog = false
            showBadgesInfoDialog = false
            showCacheConfirm = false
            showLogoutConfirm = false
            showBackupDialog = false
            showExternalMigrationDialog = false
        }
    }

    // When Firebase requires re-authentication before account deletion, show the reauth dialog
    LaunchedEffect(authState) {
        if (authState is AuthState.NeedsReauth) {
            reauthErrorMessage = null
            showReauthDialog = true
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
                    val tempFile = File(context.cacheDir, "restore_payload_${System.currentTimeMillis()}.tmp")
                    tempFile.outputStream().use { out -> stream.copyTo(out) }
                    settingsViewModel.restoreFile(tempFile.absolutePath)
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
                    val tempFile = File(context.cacheDir, "migrate_payload_${System.currentTimeMillis()}.tmp")
                    tempFile.outputStream().use { out -> stream.copyTo(out) }
                    settingsViewModel.migrateExternalFile(tempFile.absolutePath)
                }
            } catch (e: Exception) {
                // Error handling is managed by ViewModel
            }
        }
    }





    // If logout or delete succeeds, navigate back to login; if error, show toast
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Unauthenticated -> {
                onLoggedOut()
            }
            is AuthState.Error -> {
                settingsViewModel.emitToast(state.message)
                viewModel.clearError()
            }
            else -> {}
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
                    item {
                        DonationBanner(modifier = Modifier.padding(bottom = 16.dp))
                    }

                    // Section: Interfaccia e Layout
                    item {
                        SettingsUILayoutSection(
                            settingsViewModel = settingsViewModel,
                            currentAccentColor = currentAccentColor,
                            vibrationEnabled = vibrationEnabled,
                            onShowBadgesInfo = { showBadgesInfoDialog = true },
                            onShowLanguageDialog = { showLanguageDialog = true }
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
                            onShowDeepSyncConfirm = { showDeepSyncConfirm = true },
                            onShowUnmatchedItems = { showUnmatchedItemsModal = true }
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
                            onShowDeleteDialog = { showDeleteDialog = true },
                            onShowWipeSelectionDialog = { showWipeSelectionDialog = true },
                            onLinkGoogleClick = { handleGoogleLink() }
                        )
                    }

                    // Section: Supporto
                    item {
                        SettingsSupportSection(
                            vibrationEnabled = vibrationEnabled,
                            onShowFeedbackDialog = { showFeedbackDialog = true }
                        )
                    }

                    item {
                        SettingsFooterSection(
                            updateInfo = updateInfo,
                            accentColor = currentAccentColor,
                            onReplayTutorial = { settingsViewModel.resetOnboarding() }
                        )
                    }

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
                                showLanguageDialog = false
                                showFeedbackDialog = false
                                showBadgesInfoDialog = false
                                showCacheConfirm = false
                                showLogoutConfirm = false
                                showBackupDialog = false
                                showExternalMigrationDialog = false
                                showUnmatchedItemsModal = false
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
                viewModel.deleteAccount {
                    // Navigation to LoginScreen is handled cleanly by LaunchedEffect observing AuthState.Unauthenticated
                }
            }
        )

        ReauthDeleteAccountDialog(
            visible = showReauthDialog,
            activeHazeState = activeHazeState,
            errorMessage = reauthErrorMessage,
            onDismiss = {
                showReauthDialog = false
                viewModel.resetProcessState()
            },
            onConfirm = { password ->
                reauthErrorMessage = null
                viewModel.deleteAccountWithReauth(password) { success ->
                    if (!success) {
                        // Keep dialog open and show the error from authState
                        val state = viewModel.authState.value
                        if (state is AuthState.Error) {
                            reauthErrorMessage = (state.message as? com.cinetrack.ui.utils.UiText.StringResource)
                                ?.let { context.getString(it.resId) }
                                ?: (state.message as? com.cinetrack.ui.utils.UiText.DynamicString)?.value
                                ?: "Incorrect password"
                        }
                    }
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
        
        WipeDataSelectionDialog(
            visible = showWipeSelectionDialog,
            activeHazeState = activeHazeState,
            onDismiss = { showWipeSelectionDialog = false },
            onSelectLocal = {
                showWipeSelectionDialog = false
                showWipeLocalDataConfirm = true
            },
            onSelectTotal = {
                showWipeSelectionDialog = false
                showWipeTotalDataConfirm = true
            }
        )

        WipeDataConfirmDialog(
            visible = showWipeLocalDataConfirm,
            title = stringResource(id = R.string.settings_dialog_wipe_local_data_title),
            description = stringResource(id = R.string.settings_dialog_wipe_local_data_desc),
            buttonText = stringResource(id = R.string.settings_yes_wipe_local),
            activeHazeState = activeHazeState,
            onDismiss = { showWipeLocalDataConfirm = false },
            onConfirm = {
                showWipeLocalDataConfirm = false
                settingsViewModel.wipeLocalData()
            }
        )

        WipeDataConfirmDialog(
            visible = showWipeTotalDataConfirm,
            title = stringResource(id = R.string.settings_dialog_wipe_total_data_title),
            description = stringResource(id = R.string.settings_dialog_wipe_total_data_desc),
            buttonText = stringResource(id = R.string.settings_yes_wipe_total),
            activeHazeState = activeHazeState,
            onDismiss = { showWipeTotalDataConfirm = false },
            onConfirm = {
                showWipeTotalDataConfirm = false
                settingsViewModel.wipeTotalData()
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

        val contentLanguage by settingsViewModel.contentLanguage.collectAsStateWithLifecycle()
        SettingsLanguageSelectionDialog(
            visible = showLanguageDialog,
            activeHazeState = activeHazeState,
            current = contentLanguage,
            accentColor = currentAccentColor,
            vibrationEnabled = vibrationEnabled,
            onDismiss = { showLanguageDialog = false },
            onSelect = { value ->
                if (contentLanguage != value) {
                    settingsViewModel.updateContentLanguage(value) {
                        var actContext = context
                        while (actContext is android.content.ContextWrapper && actContext !is android.app.Activity) {
                            actContext = (actContext as android.content.ContextWrapper).baseContext
                        }
                        (actContext as? android.app.Activity)?.recreate()
                    }
                }
                showLanguageDialog = false
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
                importLauncher.launch(arrayOf("application/json", "application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*"))
            }
        )

        SettingsExternalMigrationDialog(
            visible = showExternalMigrationDialog,
            activeHazeState = activeHazeState,
            onDismiss = { showExternalMigrationDialog = false },
            onImport = {
                showExternalMigrationDialog = false
                externalMigrationLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*"))
            }
        )

        UnmatchedItemsModal(
            isVisible = showUnmatchedItemsModal,
            onClose = { showUnmatchedItemsModal = false },
            movies = unmatchedMovies,
            onRemoveItem = { item ->
                settingsViewModel.deleteUnmatchedItem(item)
            }
        )

        SettingsLoadingOverlay(
            visible = isBackupLoading,
            activeHazeState = activeHazeState
        )
    }
}
