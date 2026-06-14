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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FlickTroveApp(deepLinkIntent: MutableState<Intent?>, settingsViewModel: SettingsViewModel) {
    val accentColorName by settingsViewModel.accentColor.collectAsStateWithLifecycle()
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val imageQuality by settingsViewModel.imageQuality.collectAsStateWithLifecycle()

    val undoViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.cinetrack.ui.viewmodel.UndoViewModel>()

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
        
        var searchOverlayTriggerX by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(-1f) }
        var searchOverlayTriggerY by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(-1f) }
        var isSearchOverlayOpen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
        var searchInitialGenreId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Long?>(null) }
        var searchInitialGenreName by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
        var searchInitialKeywordId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Long?>(null) }
        var searchInitialKeywordName by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
        var searchOverlaySourceScreenKey by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }

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
                            CompositionLocalProvider(
                                com.cinetrack.ui.LocalSearchOverlay provides { offset, genreId, genreName, keywordId, keywordName ->
                                    searchOverlayTriggerX = offset?.x ?: -1f
                                    searchOverlayTriggerY = offset?.y ?: -1f
                                    searchInitialGenreId = genreId
                                    searchInitialGenreName = genreName
                                    searchInitialKeywordId = keywordId
                                    searchInitialKeywordName = keywordName
                                    searchOverlaySourceScreenKey = navigator.lastItem.key
                                    isSearchOverlayOpen = true
                                }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    cafe.adriel.voyager.transitions.ScreenTransition(
                                        navigator = navigator,
                                        transition = {
                                            val isPop = navigator.lastEvent == cafe.adriel.voyager.core.stack.StackEvent.Pop
                                            val isReplace = navigator.lastEvent == cafe.adriel.voyager.core.stack.StackEvent.Replace
                                            
                                            val isTargetSearch = targetState is com.cinetrack.ui.screens.SearchScreen
                                            val isInitialSearch = initialState is com.cinetrack.ui.screens.SearchScreen
                                            val isInitialSplash = initialState is com.cinetrack.ui.screens.SplashScreen
                                            
                                            val enter = if (isPop || isReplace || isInitialSplash) {
                                                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500))
                                            } else {
                                                if (isTargetSearch) {
                                                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300))
                                                } else {
                                                    androidx.compose.animation.slideInVertically(
                                                        initialOffsetY = { it },
                                                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                                    )
                                                }
                                            }
                                            
                                            val exit = if (isPop) {
                                                if (isInitialSearch) {
                                                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                                                } else {
                                                    androidx.compose.animation.slideOutVertically(
                                                        targetOffsetY = { it },
                                                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                                    )
                                                }
                                            } else if (isReplace || isInitialSplash) {
                                                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                                            } else {
                                                if (isInitialSearch) {
                                                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                                                } else {
                                                    androidx.compose.animation.ExitTransition.KeepUntilTransitionsFinished
                                                }
                                            }
                                            
                                            (enter togetherWith exit).apply {
                                                targetContentZIndex = if (isPop) -1f else 1f
                                            }
                                        }
                                    ) { screen ->
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            screen.Content()
                                            
                                            if (isSearchOverlayOpen && screen.key == searchOverlaySourceScreenKey) {
                                                Box(modifier = Modifier.fillMaxSize().zIndex(100000f)) {
                                                    val context = LocalContext.current
                                                    var currentContext = context
                                                    while (currentContext is android.content.ContextWrapper && currentContext !is androidx.activity.ComponentActivity) {
                                                        currentContext = currentContext.baseContext
                                                    }
                                                    val activity = currentContext as? androidx.activity.ComponentActivity
                                                    
                                                    val viewModel = if (activity != null) {
                                                        androidx.hilt.navigation.compose.hiltViewModel<com.cinetrack.ui.viewmodel.SearchViewModel>(activity)
                                                    } else {
                                                        androidx.hilt.navigation.compose.hiltViewModel<com.cinetrack.ui.viewmodel.SearchViewModel>()
                                                    }

                                                    androidx.compose.runtime.LaunchedEffect(searchInitialGenreId, searchInitialGenreName, searchInitialKeywordId, searchInitialKeywordName) {
                                                        if (searchInitialGenreId != null) {
                                                            viewModel.updateSortConfig(viewModel.uiState.value.sortConfig.copy(selectedGenres = listOf(searchInitialGenreId!!), selectedKeywords = emptyList()))
                                                            viewModel.onQueryChanged("")
                                                        } else if (searchInitialGenreName != null) {
                                                            viewModel.onQueryChanged("")
                                                        } else if (searchInitialKeywordId != null) {
                                                            viewModel.updateSortConfig(viewModel.uiState.value.sortConfig.copy(selectedGenres = emptyList(), selectedKeywords = listOf(searchInitialKeywordId!!)))
                                                            viewModel.onQueryChanged("")
                                                        } else if (searchInitialKeywordName != null) {
                                                            viewModel.onQueryChanged("")
                                                        }
                                                    }

                                                    com.cinetrack.ui.screens.SearchScreenContent(
                                                        viewModel = viewModel,
                                                        paddingValues = PaddingValues(0.dp),
                                                        startX = if (searchOverlayTriggerX >= 0) searchOverlayTriggerX else null,
                                                        startY = if (searchOverlayTriggerY >= 0) searchOverlayTriggerY else null,
                                                        initialGenreName = searchInitialGenreName,
                                                        initialKeywordName = searchInitialKeywordName,
                                                        onBack = { isSearchOverlayOpen = false },
                                                        onMovieClick = { 
                                                            navigator.push(com.cinetrack.ui.screens.MovieDetailScreen(it.id, it.mediaType)) 
                                                        },
                                                        onPersonClick = { 
                                                            navigator.push(com.cinetrack.ui.screens.PersonDetailScreen(it, null)) 
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    
                    com.cinetrack.ui.components.shared.GlobalMovieActions(
                        manager = movieActionsManager,
                        hazeState = globalHazeState
                    )
                    
                    com.cinetrack.ui.components.UndoToast(
                        actionFeedbackManager = undoViewModel.actionFeedbackManager,
                        hazeState = globalHazeState,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp).zIndex(200000f)
                    )
                }
            }
        }
    }
}
