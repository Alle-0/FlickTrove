package com.cinetrack

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.view.animation.AnticipateInterpolator
import androidx.core.animation.doOnEnd
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import com.cinetrack.ui.navigation.SettingsRoute
import com.cinetrack.ui.navigation.SplashRoute
import com.cinetrack.ui.navigation.SurpriseMeRoute
import com.cinetrack.ui.navigation.UpdatesRoute
import com.cinetrack.ui.navigation.VistiRoute
import com.cinetrack.ui.navigation.SearchRoute
import com.cinetrack.ui.navigation.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.toRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import com.cinetrack.ui.screens.*
import com.cinetrack.ui.viewmodel.*
import com.cinetrack.ui.theme.*
import com.cinetrack.ui.components.*
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.zIndex
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import com.cinetrack.ui.assets.CustomIcons
import dev.chrisbanes.haze.hazeChild
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import com.cinetrack.ui.components.glass.hazeGlass
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.graphicsLayer
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.ui.graphics.TransformOrigin
import com.cinetrack.ui.components.shared.MovieActionsManager
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.ui.components.shared.GlobalMovieActions
import com.cinetrack.ui.components.shared.nextGridColumns
import com.cinetrack.ui.components.glass.hazeGlass
import androidx.compose.runtime.CompositionLocalProvider
import com.cinetrack.util.toComposeColor
import com.cinetrack.util.toComposeColorOrNull
import androidx.compose.ui.platform.LocalView
import android.graphics.Bitmap
import kotlinx.serialization.Serializable
import dagger.hilt.android.AndroidEntryPoint
import com.cinetrack.util.LocalImageQuality

import androidx.compose.animation.ExperimentalSharedTransitionApi
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FlickTroveApp(deepLinkIntent: androidx.compose.runtime.MutableState<android.content.Intent?>) {

            val settingsViewModel: SettingsViewModel = hiltViewModel()
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

                CompositionLocalProvider(
                    LocalMovieActions provides movieActionsManager,
                    com.cinetrack.ui.utils.LocalVibrationEnabled provides vibrationEnabled,
                    LocalDisabledBadges provides disabledBadges,
                    LocalTitleTextSizeMultiplier provides titleTextSizeMultiplier,
                    LocalAdvancedVisualEffects provides advancedVisualEffectsEnabled,
                    LocalImageQuality provides imageQuality
                ) {
                val navController = rememberNavController()
                val detailNavController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                val scope = rememberCoroutineScope()
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                val contentHazeState = remember { HazeState() }
                val drawerHazeState = remember { HazeState() }
                val homeViewModel: HomeViewModel = hiltViewModel()
                val vistiViewModel: VistiViewModel = hiltViewModel()
                val statsViewModel: StatsViewModel = hiltViewModel()
                val undoViewModel: UndoViewModel = hiltViewModel()
                var isFilterModalVisible by remember { mutableStateOf(false) }
                var isActionModalVisible by remember { mutableStateOf(false) }
                var isYearPickerVisible by remember { mutableStateOf(false) }
                var filterButtonBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                var yearPickerButtonBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                var updatesOverlayOffset by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
                var searchOverlayOffset by remember { mutableStateOf<Offset?>(null) }
                var searchInitialQuery by remember { mutableStateOf<String?>(null) }
                var searchInitialGenreId by remember { mutableStateOf<Long?>(null) }
                var searchInitialGenreName by remember { mutableStateOf<String?>(null) }
                var searchInitialKeywordId by remember { mutableStateOf<Long?>(null) }
                var searchInitialKeywordName by remember { mutableStateOf<String?>(null) }
                var isOverlayClosing by remember { mutableStateOf(false) }
                var showFolderDeleteConfirm by remember { mutableStateOf(false) }
                var showFolderEditDialog by remember { mutableStateOf(false) }
                var folderEditMode by remember { mutableStateOf(com.cinetrack.ui.components.shared.FolderEditMode.NAME) }
                var showFolderOptions by remember { mutableStateOf(false) }
                var folderOptionsOffset by remember { mutableStateOf(Offset.Zero) }
                var showExitConfirmation by remember { mutableStateOf(false) }
                
                var globalTopZIndex by remember { mutableStateOf(1000f) }
                var searchZIndex by remember { mutableStateOf(700f) }
                var detailZIndex by remember { mutableStateOf(800f) }
                var updatesZIndex by remember { mutableStateOf(700f) }
                val detailZIndexMap = remember { mutableStateMapOf<String, Float>() }
                var detailDepthHint by remember { mutableIntStateOf(0) }

                val openDetail: (Long, String) -> Unit = { id, mediaType ->
                    detailDepthHint += 1
                    detailNavController.navigate(DetailRoute(id = id, mediaType = mediaType))
                }
                val openPerson: (Long, String?) -> Unit = { id, profilePath ->
                    detailDepthHint += 1
                    detailNavController.navigate(PersonRoute(id = id, profilePath = profilePath))
                }
                val popDetailOne: () -> Unit = {
                    if (detailDepthHint > 0) detailDepthHint -= 1
                    detailNavController.popBackStack()
                }
                val popDetailToRoot: () -> Unit = {
                    detailDepthHint = 0
                    detailNavController.popBackStack(
                        route = DetailOverlayPlaceholder,
                        inclusive = false
                    )
                }

                LaunchedEffect(deepLinkIntent.value, authState) {
                    val intent = deepLinkIntent.value ?: return@LaunchedEffect
                    val uri = intent.data
                    val action = intent.action
                    if (uri != null || action?.startsWith("com.cinetrack.SHORTCUT_") == true) {
                        // If auth state is not yet resolved, wait for it
                        if (authState !is AuthState.Authenticated && authState !is AuthState.Unauthenticated && authState !is AuthState.Error && authState !is AuthState.Anonymous) {
                            return@LaunchedEffect
                        }
                        
                        // Only process deep links if authenticated or guest
                        if (authState is AuthState.Authenticated || authState is AuthState.Anonymous) {
                            if (uri != null && uri.scheme == "flicktrove" && (uri.host == "media" || uri.host == "detail")) {
                                val segments = uri.pathSegments
                                if (segments.size >= 2) {
                                    val type = segments[0]
                                    val id = segments[1].toLongOrNull()
                                    if (id != null) {
                                        if (type == "person") openPerson(id, null)
                                        else openDetail(id, type)
                                    }
                                }
                            } else if (uri?.scheme == "flicktrove" && uri.host == "search") {
                                searchZIndex = ++globalTopZIndex
                                searchOverlayOffset = androidx.compose.ui.geometry.Offset(540f, 1140f)
                            } else if (action == "com.cinetrack.SHORTCUT_SEARCH") {
                                searchZIndex = ++globalTopZIndex
                                searchOverlayOffset = androidx.compose.ui.geometry.Offset(540f, 1140f)
                            } else if (action == "com.cinetrack.SHORTCUT_VISTI") {
                                navController.navigate(VistiRoute) { launchSingleTop = true }
                            } else if (action == "com.cinetrack.SHORTCUT_UPCOMING_MOVIES") {
                                navController.navigate(DiscoverRoute(type = "upcoming_movies")) { launchSingleTop = true }
                            } else if (action == "com.cinetrack.SHORTCUT_UPCOMING_TV") {
                                navController.navigate(DiscoverRoute(type = "airing_today_tv")) { launchSingleTop = true }
                            }
                        }
                        
                        // Always consume the intent once auth state is resolved
                        deepLinkIntent.value = null
                    }
                }
                
                val currentDetailEntry by detailNavController.currentBackStackEntryAsState()
                val detailVisibleEntries by detailNavController.visibleEntries.collectAsStateWithLifecycle()
                val detailBackStack by detailNavController.currentBackStack.collectAsStateWithLifecycle()
                val hasDetailScreens = remember(detailBackStack) {
                    detailBackStack.any { entry ->
                        entry.destination !is androidx.navigation.NavGraph && !entry.destination.hasRoute<DetailOverlayPlaceholder>()
                    }
                }
                var isDetailOverlayVisible by remember { mutableStateOf(false) }
                LaunchedEffect(hasDetailScreens) {
                    if (hasDetailScreens) {
                        isDetailOverlayVisible = true
                    } else {
                        kotlinx.coroutines.delay(500)
                        isDetailOverlayVisible = false
                    }
                }
                
                // Count real screens from the public currentBackStack flow.
                // Keep a short-lived peak value so transition phases do not hide the Home button.
                val currentDetailDepth = remember(detailBackStack) {
                    detailBackStack.count { entry ->
                        entry.destination.hasRoute<DetailRoute>() || entry.destination.hasRoute<PersonRoute>()
                    }
                }
                var peakDetailDepth by remember { mutableIntStateOf(0) }
                LaunchedEffect(currentDetailDepth) {
                    peakDetailDepth = when {
                        currentDetailDepth > peakDetailDepth -> currentDetailDepth
                        currentDetailDepth in 1 until peakDetailDepth -> currentDetailDepth
                        else -> peakDetailDepth
                    }
                    if (currentDetailDepth > detailDepthHint) {
                        detailDepthHint = currentDetailDepth
                    } else if (currentDetailDepth in 1 until detailDepthHint) {
                        detailDepthHint = currentDetailDepth
                    }
                }
                val detailStackDepth = maxOf(currentDetailDepth, peakDetailDepth, detailDepthHint)
                
                val isSearchOnTop = false
                val isUpdatesOnTop = updatesOverlayOffset != null && (!isDetailOverlayVisible || updatesZIndex > detailZIndex) && (searchOverlayOffset == null || updatesZIndex > searchZIndex)
                val isDetailOnTop = isDetailOverlayVisible && !isUpdatesOnTop

                LaunchedEffect(currentDetailEntry) {
                    currentDetailEntry?.let { entry ->
                        if (!entry.destination.hasRoute<DetailOverlayPlaceholder>()) {
                            val existingZ = detailZIndexMap[entry.id]
                            if (existingZ != null) {
                                detailZIndex = existingZ
                            } else {
                                detailZIndex = ++globalTopZIndex
                                detailZIndexMap[entry.id] = detailZIndex
                            }
                        }
                    }
                }

                LaunchedEffect(detailBackStack) {
                    val activeIds = detailBackStack.map { it.id }.toSet()
                    detailZIndexMap.keys.removeAll { it !in activeIds }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val activeTab = remember(navBackStackEntry) {
                    val entry = navBackStackEntry
                    val dest = entry?.destination
                    val route: FlickRoute = when {
                        entry == null || dest == null -> SplashRoute
                        dest.hasRoute<SplashRoute>() -> SplashRoute
                        dest.hasRoute<HomeRoute>() -> HomeRoute
                        dest.hasRoute<VistiRoute>() -> VistiRoute
                        dest.hasRoute<DiscoverRoute>() -> entry.toRoute<DiscoverRoute>()
                        dest.hasRoute<UpdatesRoute>() -> entry.toRoute<UpdatesRoute>()
                        dest.hasRoute<StatsRoute>() -> StatsRoute
                        dest.hasRoute<RecommendationsRoute>() -> RecommendationsRoute
                        dest.hasRoute<SurpriseMeRoute>() -> SurpriseMeRoute
                        dest.hasRoute<SettingsRoute>() -> SettingsRoute
                        dest.hasRoute<LoginRoute>() || dest.route?.contains("LoginRoute") == true -> LoginRoute
                        dest.hasRoute<FoldersRoute>() -> FoldersRoute
                        dest.hasRoute<FolderDetailRoute>() -> entry.toRoute<FolderDetailRoute>()
                        else -> SplashRoute
                    }
                    route
                }

                val isLoginOrSplash = activeTab is LoginRoute || activeTab is SplashRoute || navBackStackEntry?.destination?.route?.contains("Login") == true
                
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = !isDetailOverlayVisible && !isLoginOrSplash && updatesOverlayOffset == null,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color.Transparent,
                            drawerContentColor = Color.White,
                            drawerTonalElevation = 0.dp,
                            drawerShape = androidx.compose.ui.graphics.RectangleShape,
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            modifier = Modifier
                                .width(280.dp)
                                .zIndex(100f)
                        ) {
                            GlassyDrawer(
                                hazeState = drawerHazeState,
                                selectedRoute = when(activeTab) {
                                    is DiscoverRoute -> activeTab.type
                                    is RecommendationsRoute -> "recommendations"
                                    is SurpriseMeRoute -> "surprise_me"
                                    is SettingsRoute -> "settings"
                                    is LogoAnimationRoute -> "logo_animation"
                                    else -> null
                                },
                                onClose = { scope.launch { drawerState.close() } },
                                 onNavigate = { route -> 
                                    scope.launch { drawerState.close() }
                                    if (route == "logout") {
                                        authViewModel.logout()
                                        navController.navigate(LoginRoute) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    } else if (route == "updates") {
                                        updatesZIndex = ++globalTopZIndex
                                        updatesOverlayOffset = Offset(x = 540f, y = 1140f)
                                    } else {
                                        val finalRoute: FlickRoute = when (route) {
                                            "popular_movies" -> DiscoverRoute(type = "popular_movies")
                                            "now_playing_movies" -> DiscoverRoute(type = "now_playing_movies")
                                            "upcoming_movies" -> DiscoverRoute(type = "upcoming_movies")
                                            "popular_tv" -> DiscoverRoute(type = "popular_tv")
                                            "airing_today_tv" -> DiscoverRoute(type = "airing_today_tv")
                                            "on_the_air_tv" -> DiscoverRoute(type = "on_the_air_tv")
                                            "index" -> HomeRoute
                                            "visti" -> VistiRoute
                                            "stats" -> StatsRoute
                                            "recommendations" -> RecommendationsRoute
                                            "settings" -> SettingsRoute
                                            "surprise_me" -> SurpriseMeRoute
                                            "trending_movies" -> DiscoverRoute(type = "trending_movies")
                                            "trending_tv" -> DiscoverRoute(type = "trending_tv")
                                            "my_folders" -> FoldersRoute
                                            "logo_animation" -> LogoAnimationRoute
                                            else -> HomeRoute
                                        }
                                        navController.navigate(finalRoute)
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val topBarHeight = 96.dp
                    val drawerBarHeight = 96.dp
                    val bottomBarHeight = 84.dp 
                    
                    val homePadding = PaddingValues(top = topBarHeight, bottom = bottomBarHeight)
                    val drawerPadding = PaddingValues(top = drawerBarHeight, bottom = bottomBarHeight)
                    val discoverPadding = PaddingValues(top = drawerBarHeight + 16.dp, bottom = bottomBarHeight)
                    val immersivePadding = PaddingValues(top = 0.dp, bottom = 0.dp)

                    // --- SYSTEM BACK HANDLING HIERARCHY ---
                    val isAnyMovieActionModalOpen = movieActionsManager.showRatingDialog || 
                            movieActionsManager.showNotesDialog || 
                            movieActionsManager.showFolderDialog || 
                            movieActionsManager.showActionsPopup

                    val isModalActive = isAnyMovieActionModalOpen || showFolderDeleteConfirm || showFolderEditDialog || showFolderOptions || isYearPickerVisible || isFilterModalVisible

                    androidx.activity.compose.BackHandler(enabled = isAnyMovieActionModalOpen) {
                        movieActionsManager.closeAll()
                    }

                    androidx.activity.compose.BackHandler(enabled = showFolderOptions && !isAnyMovieActionModalOpen) {
                        showFolderOptions = false
                    }
                    
                    androidx.activity.compose.BackHandler(enabled = showFolderEditDialog && !isAnyMovieActionModalOpen && !showFolderOptions) {
                        showFolderEditDialog = false
                    }

                    androidx.activity.compose.BackHandler(enabled = showFolderDeleteConfirm && !isAnyMovieActionModalOpen && !showFolderOptions && !showFolderEditDialog) {
                        showFolderDeleteConfirm = false
                    }

                    androidx.activity.compose.BackHandler(enabled = isYearPickerVisible && !isAnyMovieActionModalOpen && !showFolderDeleteConfirm) {
                        isYearPickerVisible = false
                    }

                    androidx.activity.compose.BackHandler(enabled = isFilterModalVisible && !isAnyMovieActionModalOpen && !showFolderDeleteConfirm && !isYearPickerVisible) {
                        isFilterModalVisible = false
                    }

                    androidx.activity.compose.BackHandler(enabled = drawerState.isOpen && !isModalActive && !isSearchOnTop && !isUpdatesOnTop && !isDetailOnTop) {
                        scope.launch { drawerState.close() }
                    }

                    // Exit confirmation handler: only active at the root screen (when navController has no backstack)
                    // and when no modal or overlay is currently active.
                    val isRootScreen = activeTab is HomeRoute
                    val isAnyOverlayOrModalActive = isDetailOverlayVisible || 
                             
                            updatesOverlayOffset != null || 
                            isModalActive || 
                            drawerState.isOpen ||
                            showExitConfirmation

                    androidx.activity.compose.BackHandler(enabled = isRootScreen && !isAnyOverlayOrModalActive) {
                        showExitConfirmation = true
                    }

                    androidx.activity.compose.BackHandler(enabled = showExitConfirmation) {
                        showExitConfirmation = false
                    }


                    SharedTransitionLayout {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // This internal Box captures everything behind global overlays
                        // This prevents recursive blurring when overlays use hazeChild on the same state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(
                                    state = drawerHazeState,
                                    style = HazeStyles.PremiumDark
                                )
                        ) {
                        // --- BACKGROUND SOURCE LAYER ---
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            Scaffold(
                                containerColor = PremiumBackground,
                                contentWindowInsets = WindowInsets(0, 0, 0, 0)
                            ) { innerPadding ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = Color.Transparent
                                    ) {
                                @Composable
                                fun RootScreenWrapper(
                                    activeTab: FlickRoute,
                                    content: @Composable () -> Unit
                                ) {
                                    val isAnySettingsDialogOpen by settingsViewModel.isAnyDialogOpen.collectAsStateWithLifecycle()
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .haze(
                                                    state = contentHazeState,
                                                    style = HazeStyles.PremiumDark
                                                )

                                        ) {
                                            CinematicBackground(accentColor = accentColor)
                                            content()
                                        }
                                        
                                        val overlaysActive = ( updatesOverlayOffset != null) && !isOverlayClosing
                                        val dimBars = overlaysActive || isFilterModalVisible || isActionModalVisible || isYearPickerVisible || isAnySettingsDialogOpen
                                        val dimOverlay = overlaysActive || isFilterModalVisible || isActionModalVisible || isYearPickerVisible
                                        
                                        val overlayDimAlpha by animateFloatAsState(
                                            targetValue = if (dimOverlay) 0.65f else 0f,
                                            animationSpec = tween(400),
                                            label = "overlayDimAlpha"
                                        )

                                        val barsAlpha by animateFloatAsState(
                                            targetValue = if (overlaysActive || activeTab is LoginRoute || activeTab is SplashRoute) 0f else 1f,
                                            animationSpec = tween(300, delayMillis = 300),
                                            label = "barsAlpha"
                                        )

                                        if (barsAlpha > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .fillMaxWidth()
                                                    .graphicsLayer { alpha = barsAlpha }
                                            ) {
                                                var discoverColumns: Int? = null
                                                var onLayoutToggle: (() -> Unit)? = null
                                                
                                                val hasActiveFilters = if (activeTab is DiscoverRoute) {
                                                    val entry = navBackStackEntry
                                                    if (entry != null) {
                                                        val discoverViewModel: DiscoverViewModel = hiltViewModel(entry)
                                                        val discoverUiState by discoverViewModel.uiState.collectAsStateWithLifecycle()
                                                        
                                                        if (discoverUiState.preferences.showLayoutToggle) {
                                                            discoverColumns = if (discoverUiState.preferences.gridColumns in 1..3) discoverUiState.preferences.gridColumns else 3
                                                            onLayoutToggle = {
                                                                val next = nextGridColumns(discoverColumns ?: 3)
                                                                discoverViewModel.updateGridColumns(next)
                                                            }
                                                        }

                                                        discoverUiState.sortConfig.selectedGenres.isNotEmpty() || 
                                                        discoverUiState.sortConfig.selectedProviders.isNotEmpty() || 
                                                        discoverUiState.sortConfig.selectedDecades.isNotEmpty()
                                                    } else false
                                                } else false

                                                val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                                                val indicatorColor = if (activeTab is FolderDetailRoute) {
                                                    activeTab.folderColor.toComposeColorOrNull()
                                                } else null

                                                GlassyTopBar(
                                                    hazeState = contentHazeState,
                                                    title = when(activeTab) {
                                                        is HomeRoute -> "FlickTrove"
                                                        is VistiRoute -> "La tua Lista"
                                                        is StatsRoute -> "Statistiche"
                                                        is DiscoverRoute -> "Scopri"
                                                        is RecommendationsRoute -> "Consigliati per te"
                                                        is FoldersRoute -> "Le mie cartelle"
                                                        is FolderDetailRoute -> activeTab.folderName
                                                        is DetailRoute -> "Dettaglio"
                                                        is PersonRoute -> "Dettaglio Persona"
                                                        is SettingsRoute -> "Impostazioni"
                                                        is LogoAnimationRoute -> "Animazione Logo"
                                                        else -> "FlickTrove"
                                                    },
                                                    onMenuClick = { scope.launch { drawerState.open() } },
                                                    onBackPress = if (activeTab is DetailRoute || activeTab is PersonRoute || activeTab is FolderDetailRoute || activeTab is SettingsRoute) {
                                                        { navController.popBackStack() }
                                                    } else null,
                                                    onFolderOptionsClick = if (activeTab is FolderDetailRoute) {
                                                        { offset: Offset ->
                                                            folderOptionsOffset = offset
                                                            showFolderOptions = true
                                                        }
                                                    } else null,
                                                    onUpdatesClick = { offset: Offset -> 
                                                        updatesZIndex = ++globalTopZIndex
                                                        updatesOverlayOffset = offset 
                                                    },
                                                    onFilterClick = if (activeTab is DiscoverRoute) {
                                                        { offset: Offset ->
                                                            filterButtonBounds = Rect(
                                                                offset.x - 24f, offset.y - 24f, offset.x + 24f, offset.y + 24f
                                                            )
                                                            isFilterModalVisible = true
                                                        }
                                                    } else null,
                                                    hasActiveFilters = hasActiveFilters,
                                                    isSyncing = false,
                                                    isDimmed = dimBars,
                                                    notificationCount = homeUiState.notificationCount,
                                                    indicatorColor = indicatorColor,
                                                    onLayoutToggleClick = onLayoutToggle,
                                                    layoutColumns = discoverColumns
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .graphicsLayer { alpha = barsAlpha }
                                            ) {
                                                GlassyBottomBar(
                                                    hazeState = contentHazeState,
                                                    isDimmed = dimBars,
                                                    selectedRoute = when(activeTab) {
                                                        is HomeRoute -> "index"
                                                        is VistiRoute -> "visti"
                                                        is StatsRoute -> "stats"
                                                        is FoldersRoute -> "my_folders"
                                                        is FolderDetailRoute -> "my_folders"
                                                        is SettingsRoute -> "settings"
                                                        else -> null
                                                    },
                                                    onNavigate = { tab -> 
                                                        val routeObject = when(tab) {
                                                            "index" -> HomeRoute
                                                            "visti" -> VistiRoute
                                                            "stats" -> StatsRoute
                                                            "my_folders" -> FoldersRoute
                                                            else -> HomeRoute
                                                        }
                                                        navController.navigate(routeObject) {
                                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                        // GLOBAL DIMMING LAYER (Only for Search/Updates/Filters/Actions)
                                        if (overlayDimAlpha > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .zIndex(150f)
                                                    .background(Color.Black.copy(alpha = overlayDimAlpha))
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null,
                                                        enabled = isFilterModalVisible || isActionModalVisible
                                                    ) {
                                                        if (isFilterModalVisible) isFilterModalVisible = false
                                                        if (isActionModalVisible) isActionModalVisible = false
                                                    }
                                            )
                                        }

                                        val isButtonVisible =  updatesOverlayOffset == null && !isLoginOrSplash

                                        AnimatedVisibility(
                                            visible = isButtonVisible,
                                            enter = fadeIn(animationSpec = tween(600, delayMillis = 400)),
                                            exit = fadeOut(animationSpec = tween(300))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(end = 28.dp, bottom = 96.dp) 
                                                    .zIndex(100f)
                                            ) {
                                                var buttonCenter by remember { mutableStateOf(Offset.Zero) }
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .size(56.dp)
                                                        .onGloballyPositioned { layoutCoordinates ->
                                                            val position = layoutCoordinates.positionInWindow()
                                                            buttonCenter = Offset(
                                                                position.x + layoutCoordinates.size.width / 2f,
                                                                position.y + layoutCoordinates.size.height / 2f
                                                            )
                                                        }
                                                ) {
                                                    // Standardized Glass Layer
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .hazeGlass(
                                                                state = contentHazeState,
                                                                shape = CircleShape
                                                            )
                                                            .bounceClick(scaleDown = 0.92f, enabled = !dimBars) {
                                                                if (buttonCenter != Offset.Zero) {
                                                                    detailNavController.navigate(SearchRoute(startX = buttonCenter.x, startY = buttonCenter.y))
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = CustomIcons.PremiumSearch,
                                                            contentDescription = "Search",
                                                            modifier = Modifier.size(28.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                        
                                                        val searchDimAlpha by animateFloatAsState(
                                                            targetValue = if (dimBars) 0.6f else 0f,
                                                            animationSpec = tween(durationMillis = 300),
                                                            label = "SearchDimAlpha"
                                                        )
                                                        if (searchDimAlpha > 0f) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .matchParentSize()
                                                                    .background(Color.Black.copy(alpha = searchDimAlpha), CircleShape)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                    RootScreenWrapper(activeTab = activeTab) {
                                        NavHost(
                                            navController = navController, 
                                            startDestination = SplashRoute,
                                            modifier = Modifier.padding(innerPadding)
                                        ) {
                                            composable<SplashRoute>(
                                                exitTransition = {
                                                    fadeOut(animationSpec = tween(1000))
                                                }
                                            ) {
                                                var animationFinished by remember { mutableStateOf(false) }

                                                LaunchedEffect(authState, animationFinished) {
                                                    if (animationFinished) {
                                                        when (authState) {
                                                            is AuthState.Authenticated, is AuthState.Anonymous -> {
                                                                navController.navigate(HomeRoute) {
                                                                    popUpTo(SplashRoute) { inclusive = true }
                                                                }
                                                            }
                                                            is AuthState.Unauthenticated, is AuthState.Error -> {
                                                                navController.navigate(LoginRoute) {
                                                                    popUpTo(SplashRoute) { inclusive = true }
                                                                }
                                                            }
                                                            else -> {}
                                                        }
                                                    }
                                                }
                                                
                                                com.cinetrack.ui.screens.LogoAnimationScreen(
                                                    hazeState = null,
                                                    onAnimationEnd = {
                                                        animationFinished = true
                                                    }
                                                )
                                            }
                                            composable<HomeRoute>(
                                                exitTransition = {
                                                    if (targetState.destination.hasRoute<DetailRoute>() || targetState.destination.hasRoute<PersonRoute>()) {
                                                        ExitTransition.None
                                                    } else null
                                                },
                                                popEnterTransition = {
                                                    if (initialState.destination.hasRoute<DetailRoute>() || initialState.destination.hasRoute<PersonRoute>()) {
                                                        EnterTransition.None
                                                    } else null
                                                }
                                            ) {

                                                HomeScreen(
                                                    viewModel = homeViewModel,
                                                    paddingValues = homePadding,
                                                    hazeState = contentHazeState,
                                                    animatedVisibilityScope = this@composable,
                                                    isFilterVisible = isFilterModalVisible,
                                                    onToggleFilter = { visible, bounds -> 
                                                        isFilterModalVisible = visible
                                                        if (bounds != null) filterButtonBounds = bounds
                                                    },
                                                    onMovieClick = { movie -> 
                                                        openDetail(movie.id, movie.mediaType)
                                                    },
                                                    onActionModalVisibilityChanged = { isActionModalVisible = it }
                                                )
                                            }
                                            composable<VistiRoute>(
                                                exitTransition = {
                                                    if (targetState.destination.hasRoute<DetailRoute>() || targetState.destination.hasRoute<PersonRoute>()) {
                                                        ExitTransition.None
                                                    } else null
                                                },
                                                popEnterTransition = {
                                                    if (initialState.destination.hasRoute<DetailRoute>() || initialState.destination.hasRoute<PersonRoute>()) {
                                                        EnterTransition.None
                                                    } else null
                                                }
                                            ) {
                                                VistiScreen(
                                                    viewModel = vistiViewModel,
                                                    paddingValues = homePadding,
                                                    hazeState = contentHazeState,
                                                    animatedVisibilityScope = this@composable,
                                                    isFilterVisible = isFilterModalVisible,
                                                    onToggleFilter = { visible, bounds -> 
                                                        isFilterModalVisible = visible
                                                        if (bounds != null) filterButtonBounds = bounds
                                                    },
                                                    onMovieClick = { movie -> 
                                                        openDetail(movie.id, movie.mediaType)
                                                    },
                                                    onActionModalVisibilityChanged = { isActionModalVisible = it }
                                                )
                                            }
                                            composable<DiscoverRoute>(
                                                exitTransition = {
                                                    if (targetState.destination.hasRoute<DetailRoute>() || targetState.destination.hasRoute<PersonRoute>()) {
                                                        ExitTransition.None
                                                    } else null
                                                },
                                                popEnterTransition = {
                                                    if (initialState.destination.hasRoute<DetailRoute>() || initialState.destination.hasRoute<PersonRoute>()) {
                                                        EnterTransition.None
                                                    } else null
                                                }
                                            ) {
                                                val viewModel: DiscoverViewModel = hiltViewModel()
                                                DiscoverScreen(
                                                    viewModel = viewModel,
                                                    paddingValues = discoverPadding,
                                                    hazeState = contentHazeState,
                                                    animatedVisibilityScope = this@composable,
                                                    isFilterVisible = isFilterModalVisible,
                                                    onToggleFilter = { visible, bounds -> 
                                                        isFilterModalVisible = visible
                                                        if (bounds != null) filterButtonBounds = bounds
                                                    },
                                                    onMovieClick = { movie -> 
                                                        openDetail(movie.id, movie.mediaType)
                                                    },
                                                    onActionModalVisibilityChanged = { isActionModalVisible = it }
                                                )
                                            }
                                            composable<LogoAnimationRoute> {
                                                LogoAnimationScreen(hazeState = contentHazeState)
                                            }
                                            composable<SettingsRoute> {
                                                val authViewModel: AuthViewModel = hiltViewModel()
                                                SettingsScreen(
                                                    viewModel = authViewModel,
                                                    settingsViewModel = settingsViewModel,
                                                    paddingValues = homePadding,
                                                    onLoggedOut = {
                                                        navController.navigate(LoginRoute) {
                                                            popUpTo(0) { inclusive = true }
                                                        }
                                                    }
                                                )
                                            }
                                            composable<StatsRoute>(
                                                exitTransition = {
                                                    if (targetState.destination.hasRoute<DetailRoute>() || targetState.destination.hasRoute<PersonRoute>()) {
                                                        ExitTransition.None
                                                    } else null
                                                },
                                                popEnterTransition = {
                                                    if (initialState.destination.hasRoute<DetailRoute>() || initialState.destination.hasRoute<PersonRoute>()) {
                                                        EnterTransition.None
                                                    } else null
                                                }
                                            ) {
                                                StatsScreen(
                                                    viewModel = statsViewModel,
                                                    paddingValues = homePadding,
                                                    onToggleYearPicker = { visible, bounds ->
                                                        isYearPickerVisible = visible
                                                        if (bounds != null) yearPickerButtonBounds = bounds
                                                    },
                                                    onPersonClick = { personId ->
                                                        openPerson(personId, null)
                                                    },
                                                    onMovieClick = { movie ->
                                                        openDetail(movie.id, movie.mediaType)
                                                    }
                                                )
                                            }
                                            composable<RecommendationsRoute>(
                                                exitTransition = {
                                                    if (targetState.destination.hasRoute<DetailRoute>() || targetState.destination.hasRoute<PersonRoute>()) {
                                                        ExitTransition.None
                                                    } else null
                                                },
                                                popEnterTransition = {
                                                    if (initialState.destination.hasRoute<DetailRoute>() || initialState.destination.hasRoute<PersonRoute>()) {
                                                        EnterTransition.None
                                                    } else null
                                                }
                                            ) {
                                                val viewModel: RecommendationsViewModel = hiltViewModel()
                                                RecommendationsScreen(
                                                    viewModel = viewModel,
                                                    paddingValues = drawerPadding,
                                                    onMovieClick = { movie -> 
                                                        openDetail(movie.id, movie.mediaType)
                                                    },
                                                    onActionModalVisibilityChanged = { isActionModalVisible = it }
                                                )
                                            }
                                            composable<SurpriseMeRoute> {
                                                val viewModel: SurpriseMeViewModel = hiltViewModel()
                                                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                                                LaunchedEffect(uiState.randomMovie, uiState.hasNoMovies) {
                                                    val movie = uiState.randomMovie
                                                    if (movie != null) {
                                                        openDetail(movie.id, movie.mediaType)
                                                        navController.navigate(HomeRoute) {
                                                            popUpTo(HomeRoute) { inclusive = false }
                                                        }
                                                    } else if (uiState.hasNoMovies) {
                                                        navController.navigate(HomeRoute) { popUpTo(HomeRoute) }
                                                    }
                                                }
                                            }
                                            composable<LoginRoute> {
                                                AuthScreen(onLoginSuccess = {
                                                    navController.navigate(HomeRoute) {
                                                        popUpTo(LoginRoute) { inclusive = true }
                                                    }
                                                })
                                            }
                                            composable<LogoAnimationRoute> {
                                                com.cinetrack.ui.screens.LogoAnimationScreen()
                                            }
                                            
                                            composable<FoldersRoute>(
                                                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                            ) {
                                                val viewModel: FoldersViewModel = hiltViewModel()
                                                FoldersScreen(
                                                    viewModel = viewModel,
                                                    paddingValues = drawerPadding,
                                                    hazeState = contentHazeState,
                                                    onFolderClick = { folder ->
                                                        navController.navigate(FolderDetailRoute(folder.id, folder.name, folder.color))
                                                    }
                                                )
                                            }
                                            composable<FolderDetailRoute>(
                                                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                            ) { backStackEntry ->
                                                val route = backStackEntry.toRoute<FolderDetailRoute>()
                                                val viewModel: FolderDetailViewModel = hiltViewModel()
                                                FolderDetailScreen(
                                                    viewModel = viewModel,
                                                    paddingValues = drawerPadding,
                                                    hazeState = contentHazeState,
                                                    animatedVisibilityScope = this@composable,
                                                    onMovieClick = { movie ->
                                                        openDetail(movie.id, movie.mediaType)
                                                    },
                                                    onBack = { navController.popBackStack() },
                                                    showDeleteConfirm = showFolderDeleteConfirm,
                                                    onShowDeleteConfirmChange = { showFolderDeleteConfirm = it },
                                                    showEditDialog = showFolderEditDialog,
                                                    onShowEditDialogChange = { showFolderEditDialog = it },
                                                    folderEditMode = folderEditMode,
                                                    onFolderUpdated = { newName, newColor ->
                                                        navController.navigate(FolderDetailRoute(route.folderId, newName, newColor)) {
                                                            popUpTo<FolderDetailRoute> { inclusive = true }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- OVERLAY LAYER (Dynamic Bars) ---
                        
                        if (showFolderOptions && activeTab is FolderDetailRoute) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(2000f)
                                    .pointerInput(Unit) { detectTapGestures { showFolderOptions = false } }
                            ) {
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val offsetX = with(density) { folderOptionsOffset.x.toDp() }
                                val offsetY = with(density) { folderOptionsOffset.y.toDp() }
                                
                                var isMenuVisible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) { isMenuVisible = true }
                                
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isMenuVisible,
                                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(
                                        initialOffsetY = { -it / 4 },
                                        animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.EaseOutCirc)
                                    ),
                                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(
                                        targetOffsetY = { -it / 4 },
                                        animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.EaseInCirc)
                                    ),
                                    modifier = Modifier.absoluteOffset(x = offsetX - 200.dp + 32.dp, y = offsetY + 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .width(200.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .hazeGlass(state = contentHazeState, shape = RoundedCornerShape(24.dp))
                                            .background(Color(0xFF1E1E1E).copy(alpha = 0.5f))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .bounceClick { 
                                                    showFolderOptions = false
                                                    folderEditMode = com.cinetrack.ui.components.shared.FolderEditMode.NAME
                                                    showFolderEditDialog = true 
                                                }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Text("Rinomina Cartella", color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .bounceClick { 
                                                    showFolderOptions = false
                                                    folderEditMode = com.cinetrack.ui.components.shared.FolderEditMode.COLOR
                                                    showFolderEditDialog = true 
                                                }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Text("Cambia Colore", color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .bounceClick { 
                                                    showFolderOptions = false
                                                    showFolderDeleteConfirm = true 
                                                }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Text("Elimina Cartella", color = Color(0xFFFF3B30), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                        }
                                    }
                                }
                            }
                        }


                        } // End of BACKGROUND SOURCE LAYER

                        val detailPadding = PaddingValues(top = topBarHeight, bottom = 0.dp)
                        NavHost(
                            navController = detailNavController,
                            startDestination = DetailOverlayPlaceholder,
                            modifier = Modifier.fillMaxSize().zIndex(if (isDetailOverlayVisible) detailZIndex else -1f)
                        ) {
                            composable<DetailOverlayPlaceholder> {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))
                            }
                            composable<SearchRoute>(
                                enterTransition = { EnterTransition.None },
                                exitTransition = { fadeOut(tween(10)) },
                                popEnterTransition = { EnterTransition.None },
                                popExitTransition = { fadeOut(tween(10)) }
                            ) { backStackEntry ->
                                val route = backStackEntry.toRoute<SearchRoute>()
                                val searchViewModel: SearchViewModel = hiltViewModel()
                                
                                LaunchedEffect(route) {
                                    route.initialGenreId?.let { gid ->
                                        searchViewModel.updateSortConfig(
                                            searchViewModel.uiState.value.sortConfig.copy(
                                                selectedGenres = listOf(gid)
                                            )
                                        )
                                    }
                                    route.initialKeywordId?.let { kid ->
                                        searchViewModel.updateSortConfig(
                                            searchViewModel.uiState.value.sortConfig.copy(
                                                selectedKeywords = listOf(kid)
                                            )
                                        )
                                    }
                                }

                                SearchScreen(
                                    viewModel = searchViewModel,
                                    paddingValues = immersivePadding,
                                    startX = route.startX,
                                    startY = route.startY,
                                    initialGenreName = route.initialGenreName,
                                    initialKeywordName = route.initialKeywordName,
                                    onBack = { 
                                        searchViewModel.onQueryChanged("")
                                        searchViewModel.updateSortConfig(com.cinetrack.data.models.SortConfig())
                                        isFilterModalVisible = false
                                        popDetailOne()
                                    },
                                    onMovieClick = { movie ->
                                        openDetail(movie.id, movie.mediaType)
                                    },
                                    onPersonClick = { personId ->
                                        openPerson(personId, null)
                                    }
                                )
                            }

                            composable<DetailRoute>(
                                enterTransition = { 
                                    slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                                    ) + fadeIn(tween(450))
                                },
                                exitTransition = { 
                                    if (targetState.destination.hasRoute<SearchRoute>()) {
                                        fadeOut(tween(10, delayMillis = 800))
                                    } else {
                                        fadeOut(tween(450))
                                    }
                                },
                                popEnterTransition = { 
                                    if (initialState.destination.hasRoute<SearchRoute>()) {
                                        fadeIn(tween(10))
                                    } else {
                                        fadeIn(tween(450))
                                    }
                                },
                                popExitTransition = { 
                                    if (targetState.destination.hasRoute<SearchRoute>()) {
                                        fadeOut(tween(10, delayMillis = 800))
                                    } else {
                                        slideOutVertically(
                                            targetOffsetY = { it },
                                            animationSpec = tween(450, easing = FastOutSlowInEasing)
                                        ) + fadeOut(tween(450))
                                    }
                                }
                            ) {
                                MovieDetailScreen(
                                    viewModel = hiltViewModel(),
                                    paddingValues = detailPadding,
                                    hazeState = contentHazeState,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    onBackClick = { popDetailOne() },
                                    onPersonClick = { personId, profilePath ->
                                        openPerson(personId, profilePath)
                                    },
                                    onMovieClick = { movie ->
                                        openDetail(movie.id, movie.mediaType)
                                    },
                                    onGenreClick = { id, name, offset ->
                                        detailNavController.navigate(SearchRoute(startX = offset.x, startY = offset.y, initialGenreId = id, initialGenreName = name))
                                    },
                                    onKeywordClick = { keywordId, keywordName, offset ->
                                        detailNavController.navigate(SearchRoute(startX = offset.x, startY = offset.y, initialKeywordId = keywordId, initialKeywordName = keywordName))
                                    },
                                    detailStackDepth = detailStackDepth,
                                    onHomeClick = {
                                        popDetailToRoot()
                                    }
                                )
                            }
                            composable<PersonRoute>(
                                enterTransition = { 
                                    slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                                    ) + fadeIn(tween(450))
                                },
                                exitTransition = { 
                                    if (targetState.destination.hasRoute<SearchRoute>()) {
                                        fadeOut(tween(10, delayMillis = 800))
                                    } else {
                                        fadeOut(tween(450))
                                    }
                                },
                                popEnterTransition = { 
                                    if (initialState.destination.hasRoute<SearchRoute>()) {
                                        fadeIn(tween(10))
                                    } else {
                                        fadeIn(tween(450))
                                    }
                                },
                                popExitTransition = { 
                                    if (targetState.destination.hasRoute<SearchRoute>()) {
                                        fadeOut(tween(10, delayMillis = 800))
                                    } else {
                                        slideOutVertically(
                                            targetOffsetY = { it },
                                            animationSpec = tween(450, easing = FastOutSlowInEasing)
                                        ) + fadeOut(tween(450))
                                    }
                                }
                            ) {
                                PersonDetailScreen(
                                    viewModel = hiltViewModel(),
                                    paddingValues = detailPadding,
                                    hazeState = contentHazeState,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    onBackClick = { popDetailOne() },
                                    onMovieClick = { movie ->
                                        openDetail(movie.id, movie.mediaType)
                                    },
                                    detailStackDepth = detailStackDepth,
                                    onHomeClick = {
                                        popDetailToRoot()
                                    }
                                )
                            }
                        }
                        // --- MODALS AND OVERLAYS (Topmost) ---
                        AnimatedVisibility(
                            visible = updatesOverlayOffset != null,
                            modifier = Modifier.zIndex(updatesZIndex),
                            enter = EnterTransition.None,
                            exit = ExitTransition.None
                        ) {
                            val viewModel: UpdatesViewModel = hiltViewModel()
                            UpdatesScreen(
                                viewModel = viewModel,
                                modifier = Modifier,
                                startX = updatesOverlayOffset?.x,
                                startY = updatesOverlayOffset?.y,
                                paddingValues = immersivePadding,
                                onBack = { 
                                    updatesOverlayOffset = null
                                    isOverlayClosing = false
                                },
                                onClosing = { isOverlayClosing = true },
                                onMovieClick = { movie -> 
                                    openDetail(movie.id, movie.mediaType)
                                }
                            )
                        }

                        
                        // --- MODALS (Rendered last to stay on top) ---
                        // Note: Search modal is handled inside SearchScreen.kt to use local hazeState
                        if (true) {
                            if (activeTab is HomeRoute) {
                                val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                                Box(modifier = Modifier.zIndex(70000f)) {
                                    HomeFilterModal(
                                        isVisible = isFilterModalVisible,
                                        sortConfig = homeUiState.sortConfig,
                                        hazeState = contentHazeState,
                                        triggerBounds = filterButtonBounds,
                                        onSortConfigChanged = { homeViewModel.updateSortConfig(it) },
                                        onDismissRequest = { isFilterModalVisible = false }
                                    )
                                }
                            }

                            if (activeTab is VistiRoute) {
                                val vistiUiState by vistiViewModel.uiState.collectAsStateWithLifecycle()
                                Box(modifier = Modifier.zIndex(70000f)) {
                                    HomeFilterModal(
                                        isVisible = isFilterModalVisible,
                                        isVisti = true,
                                        sortConfig = vistiUiState.sortConfig,
                                        hazeState = contentHazeState,
                                        triggerBounds = filterButtonBounds,
                                        onSortConfigChanged = { vistiViewModel.updateSortConfig(it) },
                                        onDismissRequest = { isFilterModalVisible = false }
                                    )
                                }
                            }

                            if (activeTab is DiscoverRoute) {
                                val entry = navBackStackEntry
                                if (entry != null) {
                                    val discoverViewModel: DiscoverViewModel = hiltViewModel(entry)
                                    val discoverUiState by discoverViewModel.uiState.collectAsStateWithLifecycle()
                                    Box(modifier = Modifier.zIndex(70000f)) {
                                        HomeFilterModal(
                                            isVisible = isFilterModalVisible,
                                            sortConfig = discoverUiState.sortConfig,
                                            hazeState = contentHazeState,
                                            triggerBounds = filterButtonBounds,
                                            onSortConfigChanged = { discoverViewModel.updateSortConfig(it) },
                                            onDismissRequest = { isFilterModalVisible = false }
                                        )
                                    }
                                }
                            }

                            if (activeTab is StatsRoute) {
                                val statsUiState by statsViewModel.uiState.collectAsStateWithLifecycle()
                                Box(modifier = Modifier.zIndex(70000f)) {
                                    YearSelectionModal(
                                        isVisible = isYearPickerVisible,
                                        onDismiss = { isYearPickerVisible = false },
                                        currentRange = statsUiState.timeRange,
                                        availableYears = statsUiState.availableYears,
                                        hazeState = contentHazeState,
                                        triggerBounds = yearPickerButtonBounds,
                                        onYearSelected = { year ->
                                            statsViewModel.setTimeRange(TimeRange.Year(year))
                                        },
                                        onAllTimeSelected = {
                                            statsViewModel.setTimeRange(TimeRange.AllTime)
                                        }
                                    )
                                }
                            }

                            if (showExitConfirmation) {
                                com.cinetrack.ui.components.shared.FlickTroveModal(
                                    isVisible = true,
                                    onDismissRequest = { showExitConfirmation = false },
                                    hazeState = contentHazeState
                                ) {
                                    Text(
                                        text = "Esci da FlickTrove?",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "Sei sicuro di voler uscire dall'applicazione?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = Color.White.copy(alpha = 0.15f),
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                                                )
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .bounceClick {
                                                    showExitConfirmation = false
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Annulla",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color.White,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                            )
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                                                .background(accentColor)
                                                .bounceClick {
                                                    showExitConfirmation = false
                                                    (context as? ComponentActivity)?.finish()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Esci",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color.Black,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Moved SYSTEM BACK HANDLING HIERARCHY to the top of the Surface


                        } // End Haze Capture Box (drawerHazeState)

                        // --- GLOBAL FEEDBACK (Topmost) ---
                        UndoToast(
                            actionFeedbackManager = undoViewModel.actionFeedbackManager,
                            hazeState = drawerHazeState, 
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 110.dp) 
                                .zIndex(50000f) 
                        )

                        GlobalMovieActions(
                            manager = movieActionsManager,
                            hazeState = drawerHazeState,
                            modifier = Modifier.zIndex(60000f)
                        )

                        // --- CIRCULAR REVEAL OVERLAY (Theme change animation) ---
                        val pendingReveal by settingsViewModel.pendingReveal.collectAsStateWithLifecycle()
                        val view = LocalView.current
                        val oldThemeBitmap = remember { mutableStateOf<Bitmap?>(null) }

                        LaunchedEffect(pendingReveal) {
                            pendingReveal?.let {
                                if (oldThemeBitmap.value == null) {
                                    if (view.width > 0 && view.height > 0) {
                                        try {
                                            val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                                            val canvas = android.graphics.Canvas(bmp)
                                            view.draw(canvas)
                                            oldThemeBitmap.value = bmp
                                        } catch (e: Exception) {
                                            // Fallback if capture fails
                                        }
                                    }
                                    // Apply the new theme color now that the screen is captured!
                                    settingsViewModel.applyPendingTheme()
                                }
                            }
                        }

                        oldThemeBitmap.value?.let { bitmap ->
                            CircularRevealOverlay(
                                oldBitmap = bitmap,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(70000f),
                                onRevealComplete = {
                                    oldThemeBitmap.value = null
                                    settingsViewModel.clearPendingReveal()
                                }
                            )
                        }
                    } // Box Root
                } // SharedTransitionLayout
            } // ModalNavigationDrawer
        } // CompositionLocalProvider
    } // FlickTrove_KotlinTheme

}
