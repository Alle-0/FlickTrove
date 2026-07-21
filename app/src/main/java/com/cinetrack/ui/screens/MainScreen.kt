package com.cinetrack.ui.screens

import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.cinetrack.R
import com.cinetrack.ui.FilterModalConfig
import com.cinetrack.ui.LocalActiveFilterConfig
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalDeepLinkIntent
import com.cinetrack.ui.LocalFilterRequest
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.LocalSearchOverlay
import com.cinetrack.ui.components.main.MainBottomBarOverlay
import com.cinetrack.ui.components.main.MainDeepLinkHandler
import com.cinetrack.ui.components.main.MainFolderOptionsMenu
import com.cinetrack.ui.components.main.MainGlobalDialogs
import com.cinetrack.ui.components.main.MainModalsContainer
import com.cinetrack.ui.components.main.MainSearchFab
import com.cinetrack.ui.components.navigation.GlassyDrawer
import com.cinetrack.ui.components.navigation.GlassyTopBar
import com.cinetrack.ui.components.shared.FolderEditMode
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.ui.components.shared.nextGridColumns
import com.cinetrack.ui.viewmodel.DiscoverViewModel
import com.cinetrack.ui.viewmodel.HomeViewModel
import com.cinetrack.ui.viewmodel.RecommendationsViewModel
import com.cinetrack.ui.viewmodel.SettingsViewModel
import com.cinetrack.ui.viewmodel.UndoViewModel
import com.cinetrack.ui.viewmodel.UpdatesViewModel
import com.cinetrack.util.AppUpdateInfo
import com.cinetrack.util.toComposeColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch

class MainScreen(val initialTabStr: String? = null) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val rootNavigator = LocalNavigator.currentOrThrow
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val drawerHazeState = remember { HazeState() }
        val contentHazeState = remember { HazeState() }
        val globalHazeState = remember { HazeState() }
        val undoViewModel: UndoViewModel = getViewModel()
        val searchOverlay = LocalSearchOverlay.current
        
        var currentContext = LocalContext.current
        while (currentContext is ContextWrapper && currentContext !is ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? ComponentActivity

        val settingsViewModel = if (activity != null) {
            hiltViewModel<SettingsViewModel>(activity)
        } else {
            hiltViewModel<SettingsViewModel>()
        }
        val isSettingsDialogOpen by settingsViewModel.isAnyDialogOpen.collectAsStateWithLifecycle()
        val updateInfo by settingsViewModel.updateInfo.collectAsStateWithLifecycle()
        val dismissedUpdateVersion by settingsViewModel.dismissedUpdateVersion.collectAsStateWithLifecycle()
        val ignoredUpdateVersion by settingsViewModel.ignoredUpdateVersion.collectAsStateWithLifecycle()
        val lastSeenAppVersion by settingsViewModel.lastSeenAppVersion.collectAsStateWithLifecycle()
        val hasSeenOnboarding by settingsViewModel.hasSeenOnboarding.collectAsStateWithLifecycle()

        val updatesViewModel: UpdatesViewModel = getViewModel()
        val updatesUiState by updatesViewModel.uiState.collectAsStateWithLifecycle()
        val hasAppUpdateBadge = updateInfo != null && updateInfo!!.isUpdateAvailable && ignoredUpdateVersion != updateInfo!!.latestVersion

        // Hoisted Modals State
        var isFilterModalVisible by remember { mutableStateOf(false) }
        var filterButtonBounds by remember { mutableStateOf<Rect?>(null) }
        var isYearPickerVisible by remember { mutableStateOf(false) }
        var yearPickerButtonBounds by remember { mutableStateOf<Rect?>(null) }
        
        var showFolderOptions by remember { mutableStateOf(false) }
        var folderOptionsOffset by remember { mutableStateOf(Offset.Zero) }
        var showFolderEditDialog by remember { mutableStateOf(false) }
        var folderEditMode by remember { mutableStateOf(FolderEditMode.NAME) }
        var showFolderDeleteConfirm by remember { mutableStateOf(false) }
        
        var showExitConfirmation by remember { mutableStateOf(false) }

        var updatesOverlayOffsetX by rememberSaveable { mutableStateOf<Float?>(null) }
        var updatesOverlayOffsetY by rememberSaveable { mutableStateOf<Float?>(null) }
        val updatesOverlayOffset = if (updatesOverlayOffsetX != null && updatesOverlayOffsetY != null) Offset(updatesOverlayOffsetX!!, updatesOverlayOffsetY!!) else null
        var isOverlayClosing by remember { mutableStateOf(false) }
        
        var showSurpriseMeOverlay by rememberSaveable { mutableStateOf(false) }
        val deepLinkIntent = LocalDeepLinkIntent.current
        val movieActions = LocalMovieActions.current

        val initialTab = if (initialTabStr == "visti") VistiTab else HomeTab

        TabNavigator(initialTab) { tabNavigator ->
            val currentTab = tabNavigator.current

            MainDeepLinkHandler(
                deepLinkIntent = deepLinkIntent,
                rootNavigator = rootNavigator,
                tabNavigator = tabNavigator,
                settingsViewModel = settingsViewModel,
                searchOverlay = searchOverlay
            )

            BackHandler(enabled = drawerState.isOpen || currentTab !is HomeTab) {
                if (drawerState.isOpen) {
                    scope.launch { drawerState.close() }
                } else if (currentTab is FolderDetailTab) {
                    tabNavigator.current = FoldersTab
                } else {
                    showExitConfirmation = true
                }
            }
            
            BackHandler(enabled = currentTab is HomeTab && !drawerState.isOpen) {
                showExitConfirmation = true
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = updatesOverlayOffset == null &&
                        !showSurpriseMeOverlay &&
                        !isFilterModalVisible &&
                        !isYearPickerVisible &&
                        !showFolderOptions &&
                        !showFolderEditDialog &&
                        !showFolderDeleteConfirm &&
                        !showExitConfirmation &&
                        !movieActions.isAnyModalOpen,
                scrimColor = Color.Black.copy(alpha = 0.5f),
                drawerContent = {
                    GlassyDrawer(
                        hazeState = drawerHazeState,
                        hasAppUpdateBadge = hasAppUpdateBadge,
                        selectedRoute = when (currentTab) {
                            is HomeTab -> null
                            is DiscoverTab -> DiscoverTab.requestedType
                            is VistiTab -> "visti"
                            is RecommendationsTab -> "recommendations"
                            is FoldersTab -> "my_folders"
                            is StatsTab -> "stats"
                            is SettingsTab -> "settings"
                            is NewsTab -> "news"
                            else -> null
                        },
                        onClose = { scope.launch { drawerState.close() } },
                        onNavigate = { routeStr ->
                            scope.launch {
                                drawerState.close()
                                when (routeStr) {
                                    "my_folders" -> tabNavigator.current = FoldersTab
                                    "recommendations" -> tabNavigator.current = RecommendationsTab
                                    "settings" -> tabNavigator.current = SettingsTab
                                    "stats" -> tabNavigator.current = StatsTab
                                    "visti" -> tabNavigator.current = VistiTab
                                    "popular_movies", "now_playing_movies", "upcoming_movies", "popular_tv", "airing_today_tv", "on_the_air_tv" -> {
                                        DiscoverTab.requestedType = routeStr
                                        tabNavigator.current = DiscoverTab
                                    }
                                    "news" -> tabNavigator.current = NewsTab
                                    "surprise_me" -> {
                                        showSurpriseMeOverlay = true
                                    }
                                    else -> tabNavigator.current = HomeTab
                                }
                            }
                        }
                    )
                }
            ) {
                Box(modifier = Modifier.fillMaxSize().zIndex(-100f).graphicsLayer { }) {
                    val activeFilterConfig = remember { mutableStateOf<FilterModalConfig?>(null) }
                    CompositionLocalProvider(
                        LocalAppPadding provides PaddingValues(bottom = 80.dp),
                        LocalHazeState provides contentHazeState,
                        LocalActiveFilterConfig provides activeFilterConfig,
                        LocalFilterRequest provides { bounds ->
                            filterButtonBounds = bounds
                            isFilterModalVisible = true
                        }
                    ) {
                        Box(modifier = Modifier.fillMaxSize().haze(globalHazeState)) {
                            CurrentTab()

                            // Top Bar Layer
                            val title = when (currentTab) {
                                is HomeTab -> stringResource(R.string.app_name)
                                is VistiTab -> stringResource(R.string.main_tab_visti)
                                is DiscoverTab -> stringResource(R.string.main_tab_discover)
                                is RecommendationsTab -> stringResource(R.string.main_tab_recommendations)
                                is StatsTab -> stringResource(R.string.main_tab_stats)
                                is FoldersTab -> stringResource(R.string.main_tab_folders)
                                is SettingsTab -> stringResource(R.string.main_tab_settings)
                                is NewsTab -> stringResource(R.string.news_tab_title)
                                is FolderDetailTab -> currentTab.folderName
                                else -> stringResource(R.string.app_name)
                            }

                            val recommendationsViewModel: RecommendationsViewModel? = if (currentTab is RecommendationsTab && activity != null) {
                                hiltViewModel(activity)
                            } else null

                            Box(modifier = Modifier.align(Alignment.TopCenter).zIndex(50f)) {
                                var discoverHasActiveFilters = false
                                var discoverGridColumns: Int? = null
                                var discoverOnLayoutToggleClick: (() -> Unit)? = null

                                if (currentTab is DiscoverTab && activity != null) {
                                    val discoverVm = hiltViewModel<DiscoverViewModel>(activity)
                                    val discoverUiStateForBar by discoverVm.uiState.collectAsStateWithLifecycle()
                                    discoverHasActiveFilters = discoverUiStateForBar.sortConfig.selectedGenres.isNotEmpty() ||
                                        discoverUiStateForBar.sortConfig.selectedProviders.isNotEmpty() ||
                                        discoverUiStateForBar.sortConfig.selectedDecades.isNotEmpty()
                                    if (discoverUiStateForBar.preferences.showLayoutToggle) {
                                        val cols = if (discoverUiStateForBar.preferences.gridColumns in 1..4) discoverUiStateForBar.preferences.gridColumns else 3
                                        discoverGridColumns = cols
                                        discoverOnLayoutToggleClick = {
                                            discoverVm.updateGridColumns(nextGridColumns(cols))
                                        }
                                    }
                                }

                                GlassyTopBar(
                                    title = title,
                                    hazeState = contentHazeState,
                                    isDimmed = isSettingsDialogOpen,
                                    onDimmedAreaClick = { settingsViewModel.triggerCloseDialogs() },
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    onBackPress = if (currentTab is FolderDetailTab) { { tabNavigator.current = FoldersTab } } else null,
                                    onFolderOptionsClick = if (currentTab is FolderDetailTab) { { offset -> showFolderOptions = true; folderOptionsOffset = offset } } else null,
                                    indicatorColor = if (currentTab is FolderDetailTab) currentTab.folderColor?.toComposeColor() else null,
                                    onUpdatesClick = if (currentTab is HomeTab || currentTab is VistiTab || currentTab is StatsTab || currentTab is NewsTab || currentTab is RecommendationsTab || currentTab is DiscoverTab) { { offset -> updatesOverlayOffsetX = offset.x; updatesOverlayOffsetY = offset.y } } else null,
                                    onRefreshClick = if (currentTab is RecommendationsTab) { { recommendationsViewModel?.onRefresh() } } else null,
                                    onFilterClick = if (currentTab is DiscoverTab) { { offset -> isFilterModalVisible = true; filterButtonBounds = Rect(offset, Size.Zero) } } else null,
                                    hasActiveFilters = discoverHasActiveFilters,
                                    onLayoutToggleClick = discoverOnLayoutToggleClick,
                                    layoutColumns = discoverGridColumns,
                                    notificationCount = updatesUiState.notificationCount,
                                    hasAppUpdateBadge = hasAppUpdateBadge
                                )
                            }

                            // Bottom Bar & Search FAB Layer
                            MainBottomBarOverlay(
                                currentTab = currentTab,
                                contentHazeState = contentHazeState,
                                isSettingsDialogOpen = isSettingsDialogOpen,
                                onDimmedAreaClick = { settingsViewModel.triggerCloseDialogs() },
                                onNavigate = { routeStr ->
                                    when (routeStr) {
                                        "index" -> tabNavigator.current = HomeTab
                                        "visti" -> tabNavigator.current = VistiTab
                                        "stats" -> tabNavigator.current = StatsTab
                                    }
                                }
                            )

                            MainSearchFab(
                                currentTab = currentTab,
                                contentHazeState = contentHazeState,
                                onSearchClick = { offset ->
                                    searchOverlay?.invoke(offset, null, null, null, null)
                                }
                            )
                        }

                        // --- FOLDER OPTIONS MODAL ---
                        var rememberedShowFolderOptions by remember { mutableStateOf(false) }
                        var isFolderMenuVisible by remember { mutableStateOf(false) }

                        LaunchedEffect(showFolderOptions, currentTab) {
                            if (showFolderOptions && currentTab is FolderDetailTab) {
                                rememberedShowFolderOptions = true
                                isFolderMenuVisible = true
                            } else if (rememberedShowFolderOptions) {
                                isFolderMenuVisible = false
                                kotlinx.coroutines.delay(200)
                                rememberedShowFolderOptions = false
                            }
                        }

                        if (rememberedShowFolderOptions) {
                            MainFolderOptionsMenu(
                                visible = isFolderMenuVisible,
                                offset = folderOptionsOffset,
                                hazeState = contentHazeState,
                                onDismiss = { showFolderOptions = false },
                                onRename = {
                                    showFolderOptions = false
                                    folderEditMode = FolderEditMode.NAME
                                    showFolderEditDialog = true
                                },
                                onChangeColor = {
                                    showFolderOptions = false
                                    folderEditMode = FolderEditMode.COLOR
                                    showFolderEditDialog = true
                                },
                                onDelete = {
                                    showFolderOptions = false
                                    showFolderDeleteConfirm = true
                                }
                            )
                        }

                        // Modals
                        MainModalsContainer(
                            screen = this@MainScreen,
                            currentTab = currentTab,
                            activity = activity,
                            isFilterModalVisible = isFilterModalVisible,
                            isYearPickerVisible = isYearPickerVisible,
                            filterButtonBounds = filterButtonBounds,
                            yearPickerButtonBounds = yearPickerButtonBounds,
                            globalHazeState = globalHazeState,
                            onFilterModalDismiss = { isFilterModalVisible = false },
                            onYearPickerDismiss = { isYearPickerVisible = false }
                        )

                        // Global Dialogs & Overlays
                        MainGlobalDialogs(
                            screen = this@MainScreen,
                            currentTab = currentTab,
                            tabNavigator = tabNavigator,
                            rootNavigator = rootNavigator,
                            activity = activity,
                            globalHazeState = globalHazeState,
                            settingsViewModel = settingsViewModel,
                            updatesViewModel = updatesViewModel,
                            showExitConfirmation = showExitConfirmation,
                            onExitConfirmationChange = { showExitConfirmation = it },
                            showFolderEditDialog = showFolderEditDialog,
                            onFolderEditDialogChange = { showFolderEditDialog = it },
                            folderEditMode = folderEditMode,
                            showFolderDeleteConfirm = showFolderDeleteConfirm,
                            onFolderDeleteConfirmChange = { showFolderDeleteConfirm = it },
                            updatesOverlayOffset = updatesOverlayOffset,
                            onUpdatesOverlayClose = { updatesOverlayOffsetX = null; updatesOverlayOffsetY = null },
                            onOverlayClosing = { isOverlayClosing = true },
                            showSurpriseMeOverlay = showSurpriseMeOverlay,
                            onSurpriseMeClose = { showSurpriseMeOverlay = false },
                            updateInfo = updateInfo,
                            dismissedUpdateVersion = dismissedUpdateVersion,
                            ignoredUpdateVersion = ignoredUpdateVersion,
                            lastSeenAppVersion = lastSeenAppVersion,
                            hasSeenOnboarding = hasSeenOnboarding
                        )
                    }
                }
            }
        }
    }
}
