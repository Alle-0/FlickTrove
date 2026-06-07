package com.cinetrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import dev.chrisbanes.haze.haze
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.hilt.getViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.cinetrack.ui.components.shared.MovieActionsManager
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.ui.screens.SplashScreen
import com.cinetrack.ui.theme.*
import com.cinetrack.ui.utils.LocalVibrationEnabled
import com.cinetrack.ui.viewmodel.SettingsViewModel
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.toComposeColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FlickTroveApp(deepLinkIntent: MutableState<Intent?>, settingsViewModel: SettingsViewModel) {
    val accentColorName by settingsViewModel.accentColor.collectAsStateWithLifecycle()
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val imageQuality by settingsViewModel.imageQuality.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Ask for POST_NOTIFICATIONS at startup if user has it enabled but hasn't granted yet
    val startupNotifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) settingsViewModel.toggleNotifications(false)
    }

    LaunchedEffect(notificationsEnabled) {
        if (notificationsEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            startupNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    val disabledBadges by settingsViewModel.disabledBadges.collectAsStateWithLifecycle()
    val titleTextSizeMultiplier by settingsViewModel.titleTextSizeMultiplier.collectAsStateWithLifecycle()
    val advancedVisualEffectsEnabled by settingsViewModel.advancedVisualEffectsEnabled.collectAsStateWithLifecycle()
    val appTheme by settingsViewModel.appTheme.collectAsStateWithLifecycle()
    
    val accentColor = remember(accentColorName) {
        when (accentColorName) {
            "Pink" -> NeonPink
            "Purple" -> NeonPurple
            "Amber" -> NeonAmber
            "Blue" -> NeonBlue
            "Teal" -> NeonTeal
            else -> accentColorName.toComposeColor(NeonTeal)
        }
    }

    FlickTrove_KotlinTheme(themeSetting = appTheme, accentColor = accentColor) {
        val movieActionsManager = remember { MovieActionsManager() }
        val globalHazeState = remember { dev.chrisbanes.haze.HazeState() }

        CompositionLocalProvider(
            LocalMovieActions provides movieActionsManager,
            LocalVibrationEnabled provides vibrationEnabled,
            LocalDisabledBadges provides disabledBadges,
            LocalTitleTextSizeMultiplier provides titleTextSizeMultiplier,
            LocalAdvancedVisualEffects provides advancedVisualEffectsEnabled,
            LocalImageQuality provides imageQuality,
            com.cinetrack.ui.LocalDeepLinkIntent provides deepLinkIntent
        ) {
            // Note: Deep link handling can be placed here or inside MainScreen/SplashScreen
            // if we need to access the Navigator directly.
            
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize().haze(globalHazeState, style = com.cinetrack.ui.theme.HazeStyles.PremiumDark)) {
                        Navigator(SplashScreen()) { navigator ->
                            cafe.adriel.voyager.transitions.ScreenTransition(
                                navigator = navigator,
                                transition = {
                                    val isPop = navigator.lastEvent == cafe.adriel.voyager.core.stack.StackEvent.Pop
                                    val enter = if (isPop) {
                                        androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300))
                                    } else {
                                        androidx.compose.animation.slideInVertically(
                                            initialOffsetY = { it },
                                            animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                        )
                                    }
                                    val exit = if (isPop) {
                                        androidx.compose.animation.slideOutVertically(
                                            targetOffsetY = { it },
                                            animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                        )
                                    } else {
                                        androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                                    }
                                    (enter togetherWith exit).apply {
                                        targetContentZIndex = if (isPop) -1f else 1f
                                    }
                                }
                            )
                        }
                    }
                    
                    com.cinetrack.ui.components.shared.GlobalMovieActions(
                        manager = movieActionsManager,
                        hazeState = globalHazeState
                    )
                }
            }
        }
    }
}
