package com.cinetrack.ui.screens

import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
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
import com.cinetrack.ui.screens.StatsTab
import com.cinetrack.ui.screens.HomeTab
import com.cinetrack.ui.screens.HomeFeedTab
import com.cinetrack.ui.screens.FlowTab
import com.cinetrack.ui.screens.FlowStatsTab
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch

class MainScreen(val initialTabStr: String? = null) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val rootNavigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
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
        
        var showFoldersSortMenu by remember { mutableStateOf(false) }
        var foldersSortMenuOffset by remember { mutableStateOf<Offset?>(null) }
        
        var showExitConfirmation by remember { mutableStateOf(false) }

        var updatesOverlayOffsetX by rememberSaveable { mutableStateOf<Float?>(null) }
        var updatesOverlayOffsetY by rememberSaveable { mutableStateOf<Float?>(null) }
        val updatesOverlayOffset = if (updatesOverlayOffsetX != null && updatesOverlayOffsetY != null) Offset(updatesOverlayOffsetX!!, updatesOverlayOffsetY!!) else null

        var settingsOverlayOffsetX by rememberSaveable { mutableStateOf<Float?>(null) }
        var settingsOverlayOffsetY by rememberSaveable { mutableStateOf<Float?>(null) }
        val settingsOverlayOffset = if (settingsOverlayOffsetX != null && settingsOverlayOffsetY != null) Offset(settingsOverlayOffsetX!!, settingsOverlayOffsetY!!) else null
        var isOverlayClosing by remember { mutableStateOf(false) }
        
        var showSurpriseMeOverlay by rememberSaveable { mutableStateOf(false) }
        val deepLinkIntent = LocalDeepLinkIntent.current
        val movieActions = LocalMovieActions.current

        val initialTab = HomeFeedTab

        val avatarSelection = com.cinetrack.ui.components.account.LocalAvatarSelection.current
        val currentUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
        val prefs = remember { currentContext.getSharedPreferences("user_name_changes", android.content.Context.MODE_PRIVATE) }

        TabNavigator(initialTab) { tabNavigator ->
            val currentTab = tabNavigator.current
            var previousTab by remember { mutableStateOf<Tab>(initialTab) }
            var _lastTab by remember { mutableStateOf<Tab>(initialTab) }

            LaunchedEffect(currentTab) {
                // previousTab = il tab in cui eravamo PRIMA di questo cambio
                if (currentTab != _lastTab) {
                    previousTab = _lastTab
                    _lastTab = currentTab
                }
            }

            MainDeepLinkHandler(
                deepLinkIntent = deepLinkIntent,
                rootNavigator = rootNavigator,
                tabNavigator = tabNavigator,
                settingsViewModel = settingsViewModel,
                searchOverlay = searchOverlay
            )

            BackHandler(enabled = currentTab !is HomeFeedTab) {
                if (currentTab is FolderDetailTab) {
                    tabNavigator.current = FoldersTab
                } else if (currentTab is StatsTab || currentTab is FoldersTab || currentTab is FlowTab || currentTab is FlowStatsTab) {
                    tabNavigator.current = AccountTab
                } else if (currentTab is DiscoverTab || currentTab is RecommendationsTab || currentTab is NewsTab || currentTab is SettingsTab) {
                    tabNavigator.current = previousTab.takeIf { it != currentTab } ?: HomeFeedTab
                } else {
                    showExitConfirmation = true
                }
            }
            
            BackHandler(enabled = currentTab is HomeFeedTab) {
                showExitConfirmation = true
            }

            Box(modifier = Modifier.fillMaxSize().zIndex(-100f).graphicsLayer { }) {
                val activeFilterConfig = remember { mutableStateOf<FilterModalConfig?>(null) }
                CompositionLocalProvider(
                    LocalAppPadding provides PaddingValues(bottom = 80.dp),
                        LocalActiveFilterConfig provides activeFilterConfig,
                        LocalFilterRequest provides { bounds ->
                            filterButtonBounds = bounds
                            isFilterModalVisible = true
                        },
                        com.cinetrack.ui.LocalSurpriseMeRequest provides {
                            showSurpriseMeOverlay = true
                        }
                    ) {
                        Box(modifier = Modifier.fillMaxSize().haze(globalHazeState)) {
                            Box(modifier = Modifier.fillMaxSize().haze(contentHazeState)) {
                                AnimatedContent(
                                    targetState = currentTab,
                                transitionSpec = {
                                    val targetDepth = when (targetState) {
                                        is HomeTab, is HomeFeedTab, is DiscoverTab, is VistiTab, is RecommendationsTab, is AccountTab, is SettingsTab, is NewsTab -> 0
                                        is StatsTab, is FoldersTab, is FlowTab, is FlowStatsTab -> 1
                                        is FolderDetailTab -> 2
                                        else -> 0
                                    }
                                    val initialDepth = when (initialState) {
                                        is HomeTab, is HomeFeedTab, is DiscoverTab, is VistiTab, is RecommendationsTab, is AccountTab, is SettingsTab, is NewsTab -> 0
                                        is StatsTab, is FoldersTab, is FlowTab, is FlowStatsTab -> 1
                                        is FolderDetailTab -> 2
                                        else -> 0
                                    }

                                    if (targetState::class == initialState::class) {
                                        // Same tab class (e.g. rename of FolderDetailTab): no animation
                                        EnterTransition.None togetherWith ExitTransition.None
                                    } else if (targetDepth > initialDepth) {
                                        slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it }) togetherWith
                                                slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it })
                                    } else if (targetDepth < initialDepth) {
                                        slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it }) togetherWith
                                                slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it })
                                    } else {
                                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                                    }
                                },
                                label = "TabTransition"
                            ) { tab ->
                                val key = when (tab) {
                                    is FolderDetailTab -> "FolderDetailTab_${tab.folderId}"
                                    else -> tab::class.simpleName ?: "tab"
                                }
                                tabNavigator.saveableState(key, tab) {
                                    tab.Content()
                                }
                            }
                        }

                            // Top Bar Layer
                            val title = when (currentTab) {
                                is HomeFeedTab -> stringResource(R.string.app_name)
                                is HomeTab -> stringResource(R.string.bottom_bar_to_watch)
                                is VistiTab -> stringResource(R.string.main_tab_visti)
                                is DiscoverTab -> stringResource(R.string.main_tab_discover)
                                is RecommendationsTab -> stringResource(R.string.main_tab_recommendations)
                                is AccountTab -> stringResource(R.string.bottom_bar_account)
                                is FoldersTab -> stringResource(R.string.main_tab_folders)
                                is StatsTab -> stringResource(R.string.bottom_bar_stats)
                                is SettingsTab -> stringResource(R.string.main_tab_settings)
                                is NewsTab -> stringResource(R.string.news_tab_title)
                                is FolderDetailTab -> currentTab.folderName
                                is FlowTab -> "Flow"
                                is FlowStatsTab -> "Flow stats"
                                else -> stringResource(R.string.app_name)
                            }

                            val recommendationsViewModel: RecommendationsViewModel? = if (currentTab is RecommendationsTab && activity != null) {
                                hiltViewModel(activity)
                            } else null

                            val foldersViewModel: com.cinetrack.ui.viewmodel.FoldersViewModel? = if (currentTab is FoldersTab && activity != null) {
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

                                var statsIncludeRewatches: Boolean? = null
                                var statsOnRewatchToggle: ((Boolean) -> Unit)? = null
                                
                                if (currentTab is StatsTab && activity != null) {
                                    val statsVm = hiltViewModel<com.cinetrack.ui.viewmodel.StatsViewModel>(activity)
                                    val statsUiState by statsVm.uiState.collectAsStateWithLifecycle()
                                    statsIncludeRewatches = statsUiState.includeRewatches
                                    statsOnRewatchToggle = { statsVm.toggleIncludeRewatches(it) }
                                }

                                GlassyTopBar(
                                    title = title,
                                    hazeState = contentHazeState,
                                    isDimmed = isSettingsDialogOpen,
                                    onDimmedAreaClick = { settingsViewModel.triggerCloseDialogs() },
                                    onMenuClick = null, // Menu rimosso
                                    onBackPress = if (currentTab is FolderDetailTab) { { tabNavigator.current = FoldersTab } } else if (currentTab is StatsTab || currentTab is FoldersTab || currentTab is FlowTab || currentTab is FlowStatsTab || currentTab is SettingsTab) { { tabNavigator.current = AccountTab } } else if (currentTab is DiscoverTab || currentTab is RecommendationsTab || currentTab is NewsTab) { { tabNavigator.current = previousTab.takeIf { it != currentTab } ?: HomeFeedTab } } else null,
                                    onFolderOptionsClick = if (currentTab is FolderDetailTab) { { offset -> showFolderOptions = true; folderOptionsOffset = offset } } else null,
                                    indicatorColor = if (currentTab is FolderDetailTab) currentTab.folderColor?.toComposeColor() else null,
                                    onUpdatesClick = if (currentTab is HomeFeedTab || currentTab is HomeTab || currentTab is VistiTab || currentTab is AccountTab || currentTab is NewsTab || currentTab is RecommendationsTab || currentTab is DiscoverTab) { { offset -> updatesOverlayOffsetX = offset.x; updatesOverlayOffsetY = offset.y } } else null,
                                    onRefreshClick = if (currentTab is RecommendationsTab) { { recommendationsViewModel?.onRefresh() } } else null,
                                    onFilterClick = if (currentTab is DiscoverTab) { { offset -> isFilterModalVisible = true; filterButtonBounds = Rect(offset, Size.Zero) } } else if (currentTab is FoldersTab) { { offset -> showFoldersSortMenu = true; foldersSortMenuOffset = offset } } else null,
                                    hasActiveFilters = discoverHasActiveFilters,
                                    onLayoutToggleClick = discoverOnLayoutToggleClick,
                                    layoutColumns = discoverGridColumns,
                                    notificationCount = updatesUiState.totalUnreadCount,
                                    hasAppUpdateBadge = hasAppUpdateBadge,
                                    onEditBackdropClick = if (currentTab is AccountTab) { 
                                        {
                                            settingsViewModel.triggerEditProfileMenu()
                                        } 
                                    } else null,
                                    onSettingsClick = if (currentTab is AccountTab) {
                                        { offset ->
                                            settingsOverlayOffsetX = offset.x
                                            settingsOverlayOffsetY = offset.y
                                        }
                                    } else null,
                                    isStatsRewatchesEnabled = statsIncludeRewatches,
                                    onStatsRewatchToggle = statsOnRewatchToggle
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
                                        "feed" -> tabNavigator.current = HomeFeedTab
                                        "index" -> tabNavigator.current = HomeTab
                                        "visti" -> tabNavigator.current = VistiTab
                                        "account" -> tabNavigator.current = AccountTab
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

                        val foldersViewModelForSort: com.cinetrack.ui.viewmodel.FoldersViewModel? = if (currentTab is FoldersTab && activity != null) {
                            hiltViewModel(activity)
                        } else null

                        if (showFoldersSortMenu && foldersViewModelForSort != null && foldersSortMenuOffset != null) {
                            val currentSortOption by foldersViewModelForSort.sortOption.collectAsStateWithLifecycle()
                            val currentSortOrder by foldersViewModelForSort.sortOrder.collectAsStateWithLifecycle()

                            com.cinetrack.ui.components.dialog.FoldersFilterModal(
                                isVisible = showFoldersSortMenu,
                                hazeState = contentHazeState,
                                currentSortOption = currentSortOption,
                                currentSortOrder = currentSortOrder,
                                onDismissRequest = { showFoldersSortMenu = false },
                                onSortChange = { newOption, newOrder ->
                                    foldersViewModelForSort.updateSort(newOption, newOrder)
                                    showFoldersSortMenu = false
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
                            settingsOverlayOffset = settingsOverlayOffset,
                            onSettingsOverlayClose = { settingsOverlayOffsetX = null; settingsOverlayOffsetY = null },
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
