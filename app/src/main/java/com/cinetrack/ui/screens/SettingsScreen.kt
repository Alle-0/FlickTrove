package com.cinetrack.ui.screens

import com.cinetrack.ui.components.shared.ColorWheel
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
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
import com.cinetrack.ui.components.CinematicBackground
import com.cinetrack.ui.components.glass.*
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.cinetrack.util.VibrationHelper
import com.google.firebase.auth.FirebaseAuth
import androidx.hilt.navigation.compose.hiltViewModel
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

@Composable
fun SettingsScreen(
    viewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    paddingValues: PaddingValues,
    onLoggedOut: () -> Unit
) {
    val localHazeState = remember { HazeState() }
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val focusManager = LocalFocusManager.current
    
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
    val showFolderBookmarks by settingsViewModel.showFolderBookmarks.collectAsStateWithLifecycle()
    val showBadges by settingsViewModel.showBadges.collectAsStateWithLifecycle()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsStateWithLifecycle()


    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showBadgesInfoDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showCacheConfirm by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showExternalMigrationDialog by remember { mutableStateOf(false) }

    val isBackupLoading by settingsViewModel.isBackupLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Runtime permission launcher for notifications (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        settingsViewModel.toggleNotifications(isGranted)
    }

    val anyDialogVisible = showDeleteDialog || showColorDialog || showFeedbackDialog || 
                           showCacheConfirm || showLogoutConfirm || showBackupDialog || 
                           showExternalMigrationDialog || showBadgesInfoDialog || isBackupLoading

    var cacheSizeString by remember { mutableStateOf("0 MB") }
    
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
        targetValue = if (anyDialogVisible) 0.45f else 0f,
        animationSpec = tween(200),
        label = "settingsDimAlpha"
    )

    LaunchedEffect(anyDialogVisible) {
        settingsViewModel.setAnyDialogOpen(anyDialogVisible)
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
        // Content Layer - Recorded by localHazeState
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(
                    state = localHazeState,
                    style = HazeStyles.PremiumDark
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { focusManager.clearFocus() }
                        },
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding() + 24.dp,
                        bottom = paddingValues.calculateBottomPadding() + 32.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                 ) {
                    // Section: Interfaccia
                    item {
                        SettingsSection(
                            title = "Interfaccia",
                            icon = Icons.Rounded.Dashboard
                        ) {
                            SettingsItem(
                                icon = Icons.Rounded.Palette,
                                title = "Colore accento",
                                description = "Personalizza l'aspetto dell'app",
                                trailing = {
                                    // Stunning Premium Circular Accent Selector with outer glow ring
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        // Dynamic outer glow
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
                                        // Main color circle
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
                                    showColorDialog = true 
                                }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.Bookmarks,
                                title = "Segnalibri cartelle",
                                description = "Mostra un nastro colorato sulle card per indicare l'appartenenza alle cartelle.",
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
                                icon = Icons.Rounded.Style,
                                title = "Badge informativi",
                                description = "Mostra i badge colorati con indicazioni come NEW, HOT, BEST, ecc.",
                                trailing = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { 
                                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                            showBadgesInfoDialog = true 
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

                        }
                    }

                    // Section: Notifiche
                    item {
                        SettingsSection(
                            title = "Notifiche",
                            icon = Icons.Rounded.NotificationsActive
                        ) {
                            SettingsItem(
                                icon = Icons.Rounded.Notifications,
                                title = "Attiva Notifiche",
                                description = "Ricevi un avviso il giorno dell'uscita dei film o delle serie che segui.",
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
                                icon = Icons.Rounded.Vibration,
                                title = "Feedback aptico",
                                description = "Vibrazione alle azioni dell'interfaccia",
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

                    // Section: Memoria e Dati
                    item {
                        SettingsSection(
                            title = "Memoria e Dati",
                            icon = Icons.Rounded.Dns,
                            footerText = "Lo Smart Merge unirà i dati proteggendo sempre i tuoi voti e note locali."
                        ) {
                            SettingsItem(
                                icon = Icons.Rounded.CloudSync,
                                title = "Migrazione Esterna",
                                description = "Trakt, Letterboxd, IMDb",
                                onClick = { 
                                    if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                    showExternalMigrationDialog = true 
                                }
                            )
                            
                             // Grouped Backup Card
                            SettingsItem(
                                icon = Icons.Rounded.Storage,
                                title = "Backup Dispositivo FlickTrove",
                                onClick = {},
                                trailing = { }, // No arrow
                                customContent = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Esporta
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                                .clickable { 
                                                    if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                                    exportLauncher.launch("FlickTrove_Backup_${System.currentTimeMillis()}.json")
                                                }
                                                .padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Rounded.Download, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Esporta", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        
                                        // Ripristina
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                                .clickable { 
                                                    if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                                    importLauncher.launch(arrayOf("application/json"))
                                                }
                                                .padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Rounded.Upload, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Ripristina", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            )

                            SettingsItem(
                                icon = Icons.Rounded.DeleteSweep,
                                title = "Svuota Cache Immagini",
                                description = "$cacheSizeString • Libera spazio occupato da poster e backdrop",
                                tint = Color(0xFFFFA000),
                                onClick = { 
                                    if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                    showCacheConfirm = true 
                                }
                            )
                        }
                    }

                    // Section: Account
                    item {
                        val isGuest = user?.isAnonymous == true
                        val email = user?.email ?: "Ospite"
                        
                        SettingsSection(
                            title = "Account",
                            subtitle = email,
                            icon = Icons.Rounded.Person
                        ) {
                            SettingsItem(
                                icon = if (!isGuest) Icons.AutoMirrored.Rounded.Logout else Icons.AutoMirrored.Rounded.Login,
                                title = if (!isGuest) "Esci" else "Accedi",
                                tint = if (!isGuest) Color.White else currentAccentColor,
                                onClick = {
                                    if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                    if (!isGuest) {
                                        showLogoutConfirm = true
                                    } else {
                                        onLoggedOut()
                                    }
                                }
                            )
                            
                            if (!isGuest && user != null) {
                                SettingsItem(
                                    icon = Icons.Rounded.DeleteOutline,
                                    title = "Elimina Account",
                                    tint = Color(0xFFFF5252),
                                    borderColor = Color(0xFFFF5252),
                                    onClick = { 
                                        if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                        showDeleteDialog = true 
                                    }
                                )
                            }
                        }
                    }

                    // Section: Supporto
                    item {
                        SettingsSection(
                            title = "Supporto e Informazioni",
                            icon = Icons.AutoMirrored.Rounded.Help
                        ) {
                            SettingsItem(
                                icon = Icons.Rounded.ChatBubbleOutline,
                                title = "Invia Feedback",
                                description = "Segnala bug o suggerisci nuove funzioni",
                                onClick = { 
                                    if (vibrationEnabled) VibrationHelper.vibrateLongClick(context)
                                    showFeedbackDialog = true 
                                }
                            )
                            SettingsItem(
                                icon = Icons.AutoMirrored.Rounded.Article,
                                title = "Termini di Servizio",
                                isExternal = true,
                                onClick = { /* Open URL */ }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.Security,
                                title = "Informativa sulla Privacy",
                                isExternal = true,
                                onClick = { /* Open URL */ }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Attribution Row with better styling
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                AttributionRow(
                                    brand = "TMDB",
                                    text = "Questo prodotto utilizza l'API di TMDB ma non è approvato o certificato da TMDB."
                                )
                                AttributionRow(
                                    brand = "OMDb API",
                                    text = "Dati addizionali e rating forniti da OMDb API."
                                )
                                AttributionRow(
                                    brand = "Trakt.tv",
                                    text = "Sincronizzazione e statistiche alimentate da Trakt.tv."
                                )
                            }
                        }
                    }
                    
                    item { 
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "FlickTrove v3.1.3",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Sviluppato con ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceMuted
                                )
                                Icon(
                                    Icons.Rounded.Favorite,
                                    null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = " per amanti del cinema",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceMuted
                                )
                            }
                        }
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

        // Custom Glassy Delete Account Dialog
        AnimatedVisibility(
            visible = showDeleteDialog,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.zIndex(100f)
        ) {
            val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth(0.85f)
                        .hazeGlass(state = localHazeState, alpha = alpha, shape = RoundedCornerShape(32.dp))
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.PersonRemove,
                            null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Elimina Account",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Sei sicuro di voler procedere? Tutti i tuoi film salvati, valutazioni e cartelle verranno rimossi permanentemente dal cloud.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showDeleteDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Annulla", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Button(
                                onClick = {
                                    showDeleteDialog = false
                                    viewModel.deleteAccount { success ->
                                        if (success) onLoggedOut()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5252)
                                )
                            ) {
                                Text("Sì, Elimina", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Custom Glassy Cache Confirm Dialog
        AnimatedVisibility(
            visible = showCacheConfirm,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.zIndex(100f)
        ) {
            val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth(0.85f)
                        .hazeGlass(
                            state = localHazeState, alpha = alpha,
                            shape = RoundedCornerShape(32.dp)
                        )
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Svuota Cache",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Tutti i poster e i loghi scaricati verranno rimossi. Dovranno essere ricaricati all'apertura successiva.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showCacheConfirm = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Annulla", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Button(
                                onClick = {
                                    settingsViewModel.clearImageCache()
                                    updateCacheSize()
                                    showCacheConfirm = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Conferma", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Custom Glassy Info Badges Dialog
        AnimatedVisibility(
            visible = showBadgesInfoDialog,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.zIndex(100f)
        ) {
            val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth(0.85f)
                        .hazeGlass(
                            state = localHazeState, alpha = alpha,
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
                                "Significato Badge",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Badge Legend
                        val legendScrollState = rememberScrollState()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .heightIn(max = 420.dp)
                                .verticalScroll(legendScrollState)
                                .premiumScrollbar(legendScrollState)
                        ) {
                            BadgeLegendItem(text = "NEW", color = NeonPink, desc = "Prossimamente o Nuovi episodi rilasciati")
                            BadgeLegendItem(text = "MASTERPIECE", color = Color(0xFFFFD700), desc = "Capolavoro assoluto (media ≥8.8, >2000 voti)")
                            BadgeLegendItem(text = "BEST", color = Color(0xFF00E5FF), desc = "Media voto eccezionale (≥8.5, >300 voti)")
                            BadgeLegendItem(text = "HOT", color = HazeStyles.AccentYellow, desc = "Molto popolare (voto > 3000 su TMDB)")
                            BadgeLegendItem(text = "WOW", color = NeonTeal, desc = "Ottimo gradimento (media ≥8.0, >1000 voti)")
                            BadgeLegendItem(text = "HIDDEN GEM", color = Color(0xFF00E676), desc = "Perla Nascosta (media ≥7.5, <500 voti)")
                            BadgeLegendItem(text = "CULT", color = Color(0xFF9C27B0), desc = "Titolo iconico ('90 - '10, media ≥8.0)")
                            BadgeLegendItem(text = "CLASSIC", color = Color(0xFF8D6E63), desc = "Classico del passato (< 1990, media ≥7.0)")
                            BadgeLegendItem(text = "EPIC", color = Color(0xFFFF5722), desc = "Lunga durata, colossale (> 160 min)")
                            BadgeLegendItem(text = "BINGE", color = Color(0xFF00BCD4), desc = "Ideale per abbuffate (Serie lunga, >50 ep.)")
                            BadgeLegendItem(text = "SCI-FI", color = Color(0xFF2962FF), desc = "Genere Fantascienza")
                            BadgeLegendItem(text = "COMEDY", color = Color(0xFFFFEA00), desc = "Commedia apprezzata (media ≥7.0)")
                            BadgeLegendItem(text = "HORROR", color = Color(0xFFE53935), desc = "Genere Horror")
                            BadgeLegendItem(text = "ANIME", color = Color(0xFFFF9800), desc = "Genere Animazione / Anime")
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { showBadgesInfoDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Ho capito", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Custom Glassy Logout Confirm Dialog
        AnimatedVisibility(
            visible = showLogoutConfirm,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.zIndex(100f)
        ) {
            val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth(0.85f)
                        .hazeGlass(
                            state = localHazeState, alpha = alpha,
                            shape = RoundedCornerShape(32.dp)
                        )
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Logout,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Esci",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Sei sicuro di voler uscire dal tuo account? Dovrai effettuare nuovamente l'accesso per sincronizzare i tuoi dati.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showLogoutConfirm = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Annulla", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Button(
                                onClick = {
                                    showLogoutConfirm = false
                                    viewModel.logout()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Sì, Esci", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Color Selection Dialog
        AnimatedVisibility(
            visible = showColorDialog,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.zIndex(100f)
        ) {
            val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }
            ColorSelectionDialog(
                hazeState = localHazeState, alpha = alpha,
                current = accentColorName,
                onDismiss = { showColorDialog = false },
                onSelect = { colorName, origin ->
                    // Dismiss fires immediately (dialog exit animation ~300ms).
                    // Delay the reveal so the circle appears on a clean screen.
                    scope.launch {
                        kotlinx.coroutines.delay(350)
                        settingsViewModel.updateAccentColor(colorName, origin)
                    }
                }
            )
        }

        // Feedback Dialog
        val isFeedbackLoading by settingsViewModel.isFeedbackLoading.collectAsStateWithLifecycle()
        AnimatedVisibility(
            visible = showFeedbackDialog,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.zIndex(100f)
        ) {
            val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }
            FeedbackDialog(
                hazeState = localHazeState, alpha = alpha,
                initialEmail = user?.email ?: "",
                isLoading = isFeedbackLoading,
                onDismiss = { if (!isFeedbackLoading) showFeedbackDialog = false },
                onSubmit = { t, d, r, e ->
                    settingsViewModel.sendFeedback(t, d, r, e) {
                        showFeedbackDialog = false
                    }
                }
            )
        }

        // Backup & Restore Dialog
        AnimatedVisibility(
            visible = showBackupDialog,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.zIndex(100f)
        ) {
            val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }
            BackupDialog(
                hazeState = localHazeState, alpha = alpha,
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
        }

        // Trakt Migration Dialog
        AnimatedVisibility(
            visible = showExternalMigrationDialog,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.zIndex(100f)
        ) {
            val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }
            ExternalMigrationDialog(
                hazeState = localHazeState, alpha = alpha,
                onDismiss = { showExternalMigrationDialog = false },
                onImport = { 
                    showExternalMigrationDialog = false
                    externalMigrationLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "application/octet-stream", "*/*")) 
                }
            )
        }

        // Loading Overlay
        AnimatedVisibility(
            visible = isBackupLoading,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.zIndex(200f)
        ) {
            val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }
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
                        "Elaborazione in corso...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun BackupDialog(
    hazeState: HazeState,
    isBackupLoading: Boolean,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    alpha: Float = 1f
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {


        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(0.85f)
                .hazeGlass(
                    state = hazeState, alpha = alpha,
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable(enabled = false) {}
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Rounded.Backup,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Backup e Ripristino",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Esporta una copia dei tuoi dati (preferiti, cartelle e impostazioni) o ripristina da un file precedentemente salvato.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                val isExportEnabled = !isBackupLoading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isExportEnabled) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        .bounceClick(enabled = isExportEnabled) {
                            onExport()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FileUpload, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Esporta Backup", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .bounceClick {
                            onImport()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FileDownload, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ripristina Backup", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Chiudi", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun ExternalMigrationDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    alpha: Float = 1f
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {


        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(0.85f)
                .hazeGlass(
                    state = hazeState, alpha = alpha,
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable(enabled = false) {}
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Rounded.CloudSync,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Migrazione Esterna",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Puoi importare la tua lista film caricando il file JSON esportato da Trakt.tv, o i file CSV di Letterboxd (es. watched.csv) e IMDb (es. watchlist.csv). I film verranno aggiunti ai tuoi preferiti.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .bounceClick {
                            onImport()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FileDownload, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seleziona file (JSON o CSV)", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Chiudi", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    initialEmail: String = "",
    isLoading: Boolean = false,
    onSubmit: (String, String, Int, String) -> Unit,
    alpha: Float = 1f
) {
    var title by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(initialEmail) }
    var description by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(5) }
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {


        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.9f)
                .hazeGlass(
                    state = hazeState, alpha = alpha,
                    shape = RoundedCornerShape(32.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                }
        ) {

        val feedbackScrollState = rememberScrollState()
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(28.dp)
                    .heightIn(max = 600.dp)
                    .verticalScroll(feedbackScrollState)
                    .premiumScrollbar(feedbackScrollState)
            ) {
                Text(
                    "Invia Feedback",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Aiutaci a migliorare FlickTrove",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMuted
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Rating Section
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { index ->
                        val isSelected = index <= rating
                        val starColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
                        
                        IconButton(
                            onClick = { 
                                rating = index 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                contentDescription = null,
                                tint = starColor,
                                modifier = Modifier
                                    .size(32.dp)
                                    .animateContentSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GlassyTextField(
                        value = title,
                        onValueChange = { if (it.length <= 50) title = it },
                        label = "Oggetto",
                        placeholder = "Es. Suggerimento, Bug...",
                        singleLine = true
                    )

                    GlassyTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email (opzionale)",
                        placeholder = "Per poterti rispondere",
                        singleLine = true
                    )

                    Column {
                        GlassyTextField(
                            value = description,
                            onValueChange = { if (it.length <= 500) description = it },
                            label = "Descrizione",
                            placeholder = "Raccontaci i dettagli...",
                            minHeight = 120.dp
                        )
                        Text(
                            text = "${description.length}/500",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceMuted.copy(alpha = 0.5f),
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp, end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                val isEnabled = title.isNotBlank() && description.isNotBlank() && !isLoading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isEnabled) MaterialTheme.colorScheme.primary 
                            else if (isLoading) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .bounceClick(enabled = isEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSubmit(title, description, rating, email)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = "Invia Messaggio",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) Color.Black else Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Chiudi",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean = false,
    minHeight: Dp = Dp.Unspecified
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.3f)) },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (minHeight != Dp.Unspecified) Modifier.heightIn(min = minHeight) else Modifier),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = singleLine
        )
    }
}

@Composable
fun ColorSelectionDialog(
    hazeState: HazeState,
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String, Offset) -> Unit,
    alpha: Float = 1f
) {
    val focusManager = LocalFocusManager.current
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        var tempSelectedColor by remember { mutableStateOf(current) }
        var isCustomMode by remember { mutableStateOf(current.startsWith("#")) }
        
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(24.dp)
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                }
        ) {
            
            val previewAccentColor = remember(tempSelectedColor) {
                try {
                    if (tempSelectedColor.startsWith("#")) tempSelectedColor.toComposeColor()
                    else when(tempSelectedColor) {
                        "Pink" -> NeonPink
                        "Purple" -> NeonPurple
                        "Amber" -> NeonAmber
                        "Blue" -> NeonBlue
                        else -> NeonTeal
                    }
                } catch(e: Exception) { NeonTeal }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .hazeGlass(
                        state = hazeState, alpha = alpha,
                        shape = RoundedCornerShape(32.dp),
                        style = HazeStyles.glassmorphicDialog
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(400))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Colore Interfaccia",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Unified Presets Row with "+" for Custom
                    val presets = listOf(
                        "Teal" to NeonTeal,
                        "Pink" to NeonPink,
                        "Purple" to NeonPurple,
                        "Amber" to NeonAmber,
                        "Blue" to NeonBlue
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        presets.forEach { (name, color) ->
                            val isSelected = !isCustomMode && tempSelectedColor == name
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable { 
                                            isCustomMode = false
                                            tempSelectedColor = name
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isSelected) color else Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // The "+" Button for Custom mode
                        val customSelected = isCustomMode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (customSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        width = if (customSelected) 2.dp else 0.dp,
                                        color = if (customSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { isCustomMode = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Colore personalizzato",
                                    tint = if (customSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Custom",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (customSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp
                                ),
                                color = if (customSelected) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Animated Custom Section
                    AnimatedVisibility(
                        visible = isCustomMode,
                        enter = scaleIn(initialScale = 0.95f, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
                        exit = scaleOut(targetScale = 0.95f, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                    ) {
                        // Custom Color Section (Wheel + Hex)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            var hexInput by remember(current) { 
                                mutableStateOf(if (current.startsWith("#")) current.uppercase() else "#00BCD4") 
                            }
                            var isHexFocused by remember { mutableStateOf(false) }
                            var isDragging by remember { mutableStateOf(false) }

                            // Sync hex ← wheel: only when drag ends and hex field not focused
                            LaunchedEffect(isDragging, tempSelectedColor) {
                                if (!isDragging && !isHexFocused && tempSelectedColor.length == 7) {
                                    hexInput = tempSelectedColor.uppercase()
                                }
                            }
                            
                            ColorWheel(
                                selectedColor = tempSelectedColor,
                                onColorChanged = { 
                                    tempSelectedColor = it
                                },
                                onInteractionStart = { 
                                    isDragging = true
                                    focusManager.clearFocus()
                                },
                                onInteractionEnd = { 
                                    isDragging = false
                                },
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Hex Input
                             Row(
                                 modifier = Modifier
                                     .width(160.dp)
                                     .clip(RoundedCornerShape(12.dp))
                                     .background(Color.White.copy(alpha = 0.05f))
                                     .border(
                                         width = if (isHexFocused) 2.dp else 1.dp,
                                         color = if (isHexFocused) previewAccentColor else Color.White.copy(alpha = 0.1f),
                                         shape = RoundedCornerShape(12.dp)
                                     )
                                     .padding(horizontal = 12.dp, vertical = 8.dp),
                                 verticalAlignment = Alignment.CenterVertically,
                                 horizontalArrangement = Arrangement.Center
                             ) {
                                 Text(
                                     text = "#",
                                     style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                                     color = Color.White.copy(alpha = 0.3f)
                                 )
                                 BasicTextField(
                                     value = if (hexInput.startsWith("#")) hexInput.substring(1) else hexInput,
                                     onValueChange = { newValue ->
                                         if (newValue.length <= 6) {
                                             val clean = newValue.uppercase().filter { it.isDigit() || it in 'A'..'F' }
                                             hexInput = clean
                                             // Update wheel only when we have a full valid hex
                                             if (clean.length == 6) {
                                                 tempSelectedColor = "#$clean"
                                             }
                                         }
                                     },
                                     textStyle = MaterialTheme.typography.bodyLarge.copy(
                                         color = Color.White,
                                         fontWeight = FontWeight.Bold,
                                         textAlign = TextAlign.Start,
                                         letterSpacing = 1.sp
                                     ),
                                     cursorBrush = SolidColor(previewAccentColor),
                                     singleLine = true,
                                     modifier = Modifier
                                         .width(80.dp)
                                         .padding(start = 4.dp)
                                         .onFocusChanged { focusState ->
                                             isHexFocused = focusState.isFocused
                                             // When losing focus, sync hex display to current color
                                             if (!focusState.isFocused && tempSelectedColor.length == 7) {
                                                 hexInput = tempSelectedColor.uppercase().removePrefix("#")
                                             }
                                         }
                                 )
                             }
                        }
                    }

                    

                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Track the screen-space centre of the Conferma button
                    var confirmButtonCenter by remember { mutableStateOf(Offset.Zero) }

                    Button(
                        onClick = {
                            onSelect(tempSelectedColor, confirmButtonCenter)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInWindow()
                                confirmButtonCenter = Offset(
                                    x = pos.x + coords.size.width / 2f,
                                    y = pos.y + coords.size.height / 2f
                                )
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Conferma",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Chiudi", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

fun Color.toArgb(): Int {
    return (alpha * 255.0f + 0.5f).toInt() shl 24 or
            (red * 255.0f + 0.5f).toInt() shl 16 or
            (green * 255.0f + 0.5f).toInt() shl 8 or
            (blue * 255.0f + 0.5f).toInt()
}
 @Composable
fun SettingsSection(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    footerText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thick elegant vertical bar with accent color
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
            
            Spacer(modifier = Modifier.width(10.dp))
            
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Items Container - Grouped Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
                
                if (footerText != null) {
                    Text(
                        text = footerText,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .padding(horizontal = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    description: String? = null,
    tint: Color = Color.White,
    borderColor: Color? = null,
    isExternal: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    customContent: @Composable (ColumnScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val itemThemeColor = borderColor ?: tint
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .bounceClick { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick() 
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(itemThemeColor.copy(alpha = 0.08f))
                        .border(
                            width = 1.dp,
                            color = itemThemeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = itemThemeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = if (borderColor != null) borderColor else Color.White
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted,
                            lineHeight = 14.sp,
                            fontSize = 12.sp
                        )
                    }
                }
                
                if (trailing != null) {
                    trailing()
                } else if (customContent == null) {
                    Icon(
                        imageVector = if (isExternal) Icons.AutoMirrored.Rounded.OpenInNew else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            if (customContent != null) {
                Spacer(modifier = Modifier.height(14.dp))
                customContent()
            }
        }
    }
}

@Composable
fun AttributionRow(
    brand: String,
    text: String
) {
    val logoRes = when (brand) {
        "TMDB" -> com.cinetrack.R.drawable.ic_tmdb_logo
        "Trakt.tv" -> com.cinetrack.R.drawable.ic_trakt_logo
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo Representation with local vector or fallback text
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (brand) {
                        "TMDB" -> Color(0xFF032541)
                        "OMDb API" -> Color(0xFFF5C518)
                        "Trakt.tv" -> Color(0xFFED1C24)
                        else -> Color.White.copy(alpha = 0.05f)
                    }
                )
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (logoRes != null) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = "$brand Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (brand == "TMDB") 6.dp else 0.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Stylized Fallback for OMDb or missing logos
                Text(
                    text = if (brand == "OMDb API") "OMDb" else brand.take(1),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = if (brand == "OMDb API") 10.sp else 14.sp
                    ),
                    color = if (brand == "OMDb API") Color.Black else Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        val annotatedString = buildAnnotatedString {
            val parts = text.split(brand)
            parts.forEachIndexed { index, part ->
                append(part)
                if (index < parts.size - 1) {
                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color.White)
                    ) {
                        append(brand)
                    }
                }
            }
        }
        
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = OnSurfaceMuted,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BadgeLegendItem(text: String, color: Color, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .widthIn(min = 55.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
    }
}
