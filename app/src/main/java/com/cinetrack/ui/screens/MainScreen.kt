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
import androidx.compose.ui.unit.Velocity
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.R
import com.cinetrack.ui.FilterModalConfig
import com.cinetrack.ui.LocalActiveFilterConfig
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalDeepLinkIntent
import com.cinetrack.ui.LocalFilterRequest
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.LocalBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.cinetrack.ui.LocalSearchOverlay
import com.cinetrack.ui.components.main.MainBottomBarOverlay
import com.cinetrack.ui.components.main.MainDeepLinkHandler
import com.cinetrack.ui.components.main.MainFolderOptionsMenu
import com.cinetrack.ui.components.main.MainGlobalDialogs
import com.cinetrack.ui.components.common.LocalSymbiontPullState
import com.cinetrack.ui.components.common.SymbiontGlobalIndicator
import com.cinetrack.ui.components.common.SymbiontPullState
import com.cinetrack.ui.components.main.MainModalsContainer
import com.cinetrack.ui.components.main.MainSearchFab
import com.cinetrack.ui.components.main.MainScrollToTopFab
import com.cinetrack.ui.components.navigation.GlassyTopBar
import com.cinetrack.ui.components.folder.FolderEditMode
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.ui.components.shared.nextGridColumns
import com.cinetrack.ui.viewmodel.DiscoverViewModel
import com.cinetrack.ui.viewmodel.HomeViewModel
import com.cinetrack.ui.viewmodel.VistiViewModel
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
import com.cinetrack.ui.screens.TrophyRoomTab
import com.cinetrack.ui.screens.HistoryTab
import com.cinetrack.ui.screens.HistoryViewSwitcher
import com.cinetrack.ui.history.HistoryViewModel
import com.cinetrack.ui.history.model.ViewMode
import com.cinetrack.ui.viewmodel.FlowViewModel
import com.cinetrack.ui.screens.BoxOfficeTab
import com.cinetrack.ui.viewmodel.BoxOfficeViewModel
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
        val bottomBarBackdrop = rememberLayerBackdrop {
            drawRect(androidx.compose.ui.graphics.Color(0xFF0D0D0E))
            drawContent()
        }
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
        val homeViewModel = if (activity != null) {
            hiltViewModel<HomeViewModel>(activity)
        } else {
            hiltViewModel<HomeViewModel>()
        }
        val vistiViewModel = if (activity != null) {
            hiltViewModel<VistiViewModel>(activity)
        } else {
            hiltViewModel<VistiViewModel>()
        }
        val isSettingsDialogOpen by settingsViewModel.isAnyDialogOpen.collectAsStateWithLifecycle()
        val updateInfo by settingsViewModel.updateInfo.collectAsStateWithLifecycle()
        val dismissedUpdateVersion by settingsViewModel.dismissedUpdateVersion.collectAsStateWithLifecycle()
        val ignoredUpdateVersion by settingsViewModel.ignoredUpdateVersion.collectAsStateWithLifecycle()
        val lastSeenAppVersion by settingsViewModel.lastSeenAppVersion.collectAsStateWithLifecycle()
        val hasSeenOnboarding by settingsViewModel.hasSeenOnboarding.collectAsStateWithLifecycle()
        val enableBottomBarSwipe by settingsViewModel.enableBottomBarSwipe.collectAsStateWithLifecycle()

        val updatesViewModel: UpdatesViewModel = getViewModel()
        val updatesUiState by updatesViewModel.uiState.collectAsStateWithLifecycle()
        val hasAppUpdateBadge = updateInfo != null && updateInfo!!.isUpdateAvailable && ignoredUpdateVersion != updateInfo!!.latestVersion

        // Hoisted Modals State
        var isFilterModalVisible by remember { mutableStateOf(false) }
        var filterButtonBounds by remember { mutableStateOf<Rect?>(null) }
        var isYearPickerVisible by remember { mutableStateOf(false) }
        var yearPickerButtonBounds by remember { mutableStateOf<Rect?>(null) }
        var surpriseMeButtonBounds by remember { mutableStateOf<Rect?>(null) }
        
        var showFolderOptions by remember { mutableStateOf(false) }
        var folderOptionsOffset by remember { mutableStateOf(Offset.Zero) }
        var showFolderEditDialog by remember { mutableStateOf(false) }
        var folderEditMode by remember { mutableStateOf(FolderEditMode.NAME) }
        var showFolderDeleteConfirm by remember { mutableStateOf(false) }
        var showFolderShareModal by remember { mutableStateOf(false) }
        var showFolderMembersModal by remember { mutableStateOf(false) }
        
        var showFoldersSortMenu by remember { mutableStateOf(false) }
        var foldersFilterButtonBounds by remember { mutableStateOf<Rect?>(null) }
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

        val initialTab = when (initialTabStr) {
            "feed" -> HomeFeedTab
            "home" -> HomeTab
            "visti" -> VistiTab
            else -> HomeFeedTab
        }

        val avatarSelection = com.cinetrack.ui.components.account.LocalAvatarSelection.current
        val currentUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
        val prefs = remember { currentContext.getSharedPreferences("user_name_changes", android.content.Context.MODE_PRIVATE) }

        val primaryTabs = remember {
            listOf(HomeFeedTab, HomeTab, VistiTab, AccountTab)
        }
        val initialPrimaryIndex = remember { primaryTabs.indexOf(initialTab).coerceAtLeast(0) }
        val pagerState = rememberPagerState(initialPage = initialPrimaryIndex) { primaryTabs.size }

        TabNavigator(initialTab) { tabNavigator ->
            val currentTab = tabNavigator.current
            var previousTab by remember { mutableStateOf<Tab>(currentTab) }
            var _lastTab by remember { mutableStateOf<Tab>(currentTab) }
            val scrollToTopState = remember { mutableStateOf(com.cinetrack.ui.ScrollToTopState()) }

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }.collect { settledPage ->
                    if (settledPage in primaryTabs.indices) {
                        val targetTab = primaryTabs[settledPage]
                        if (tabNavigator.current in primaryTabs && tabNavigator.current != targetTab) {
                            tabNavigator.current = targetTab
                        }
                    }
                }
            }

            val foldersViewModel: com.cinetrack.ui.viewmodel.FoldersViewModel? = if (activity != null) {
                hiltViewModel(activity)
            } else null

            LaunchedEffect(Unit) {
                foldersViewModel?.refreshFolders()
            }

            val allFoldersList by (foldersViewModel?.folders?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) })
            val activeFolderForDetail = if (currentTab is FolderDetailTab) allFoldersList?.find { it.id == currentTab.folderId } else null

            LaunchedEffect(currentTab) {
                // previousTab = il tab in cui eravamo PRIMA di questo cambio
                if (currentTab != _lastTab) {
                    scrollToTopState.value = com.cinetrack.ui.ScrollToTopState()
                    previousTab = _lastTab
                    _lastTab = currentTab
                }
                val targetIndex = primaryTabs.indexOf(currentTab)
                if (targetIndex >= 0 && pagerState.currentPage != targetIndex && !pagerState.isScrollInProgress) {
                    pagerState.scrollToPage(targetIndex)
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
                } else if (currentTab is StatsTab || currentTab is FoldersTab || currentTab is FlowTab || currentTab is FlowStatsTab || currentTab is TrophyRoomTab || currentTab is HistoryTab) {
                    tabNavigator.current = AccountTab
                } else if (currentTab is DiscoverTab || currentTab is RecommendationsTab || currentTab is NewsTab || currentTab is SettingsTab || currentTab is BoxOfficeTab) {
                    tabNavigator.current = previousTab.takeIf { it != currentTab } ?: HomeFeedTab
                } else {
                    showExitConfirmation = true
                }
            }
            
            BackHandler(enabled = currentTab is HomeFeedTab) {
                showExitConfirmation = true
            }

            val symbiontPullState = remember { SymbiontPullState() }
            Box(modifier = Modifier.fillMaxSize().zIndex(-100f).graphicsLayer { }) {
                val activeFilterConfig = remember { mutableStateOf<FilterModalConfig?>(null) }
                val activeTrophyFilterConfig = remember { mutableStateOf<com.cinetrack.ui.TrophyFilterModalConfig?>(null) }
                val activeTrophyDetailConfig = remember { mutableStateOf<com.cinetrack.ui.TrophyDetailModalConfig?>(null) }
                val folderReorderRequest = remember { mutableStateOf<(() -> Unit)?>(null) }
                val folderReorderConfig = remember { mutableStateOf<com.cinetrack.ui.FolderReorderModalConfig?>(null) }
                val folderDetailMediaHandler = remember { mutableStateOf<com.cinetrack.ui.FolderDetailMediaHandler?>(null) }

                val isTrophyDetailOpen = activeTrophyDetailConfig.value != null
                val isBarDimmed = isSettingsDialogOpen
                val isPagerScrollEnabled = !isBarDimmed &&
                        !isTrophyDetailOpen &&
                        !isFilterModalVisible &&
                        !isYearPickerVisible &&
                        !showFolderOptions &&
                        !showFolderEditDialog &&
                        !showFolderDeleteConfirm &&
                        !showFolderShareModal &&
                        !showFolderMembersModal &&
                        !showFoldersSortMenu &&
                        !showExitConfirmation &&
                        !showSurpriseMeOverlay

                BackHandler(enabled = isTrophyDetailOpen) {
                    activeTrophyDetailConfig.value?.onClose?.invoke()
                }

                CompositionLocalProvider(
                    LocalAppPadding provides PaddingValues(bottom = 80.dp),
                    LocalBackdrop provides bottomBarBackdrop,
                    com.cinetrack.ui.LocalGlobalHazeState provides globalHazeState,
                    LocalActiveFilterConfig provides activeFilterConfig,
                    com.cinetrack.ui.LocalActiveTrophyFilterConfig provides activeTrophyFilterConfig,
                    com.cinetrack.ui.LocalActiveTrophyDetailConfig provides activeTrophyDetailConfig,
                    com.cinetrack.ui.LocalFolderReorderRequest provides folderReorderRequest,
                    com.cinetrack.ui.LocalFolderReorderConfig provides folderReorderConfig,
                    com.cinetrack.ui.LocalFolderDetailMediaHandler provides folderDetailMediaHandler,
                    com.cinetrack.ui.LocalScrollToTopState provides scrollToTopState,
                    LocalFilterRequest provides { bounds ->
                        filterButtonBounds = bounds
                        isFilterModalVisible = true
                    },
                    com.cinetrack.ui.LocalSurpriseMeRequest provides { bounds ->
                        surpriseMeButtonBounds = bounds
                        showSurpriseMeOverlay = true
                    },
                    LocalSymbiontPullState provides symbiontPullState
                ) {
                    Box(modifier = Modifier.fillMaxSize().haze(globalHazeState)) {
                        CinematicBackground(modifier = Modifier.fillMaxSize())
                        Box(modifier = Modifier.fillMaxSize().haze(contentHazeState)) {
                            val activeSecondaryTab = if (currentTab in primaryTabs) null else currentTab

                                AnimatedContent(
                                    targetState = activeSecondaryTab,
                                    modifier = Modifier.fillMaxSize().layerBackdrop(bottomBarBackdrop),
                                    contentKey = { tab ->
                                        when (tab) {
                                            null -> "PRIMARY_PAGER"
                                            is FolderDetailTab -> "FolderDetailTab_${tab.folderId}"
                                            else -> tab::class.qualifiedName ?: "secondary_tab"
                                        }
                                    },
                                    transitionSpec = {
                                        val target = targetState
                                        val initial = initialState
                                        val targetDepth = when (target) {
                                            null -> 0
                                            is DiscoverTab, is RecommendationsTab, is SettingsTab, is NewsTab, is BoxOfficeTab -> 0
                                            is StatsTab, is FoldersTab, is FlowTab, is FlowStatsTab, is TrophyRoomTab, is HistoryTab -> 1
                                            is FolderDetailTab -> 2
                                            else -> 0
                                        }
                                        val initialDepth = when (initial) {
                                            null -> 0
                                            is DiscoverTab, is RecommendationsTab, is SettingsTab, is NewsTab, is BoxOfficeTab -> 0
                                            is StatsTab, is FoldersTab, is FlowTab, is FlowStatsTab, is TrophyRoomTab, is HistoryTab -> 1
                                            is FolderDetailTab -> 2
                                            else -> 0
                                        }

                                        if (target != null && initial != null && target::class == initial::class) {
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
                                ) { secondaryTab ->
                                    CompositionLocalProvider(LocalBackdrop provides null) {
                                        if (secondaryTab == null) {
                                            val carouselScrollBoundary = remember {
                                            object : NestedScrollConnection {
                                                override fun onPostScroll(
                                                    consumed: Offset,
                                                    available: Offset,
                                                    source: NestedScrollSource
                                                ): Offset {
                                                    return if (available.x != 0f) Offset(available.x, 0f) else Offset.Zero
                                                }

                                                override suspend fun onPostFling(
                                                    consumed: Velocity,
                                                    available: Velocity
                                                ): Velocity {
                                                    return if (available.x != 0f) Velocity(available.x, 0f) else Velocity.Zero
                                                }
                                            }
                                        }

                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxSize(),
                                            userScrollEnabled = isPagerScrollEnabled,
                                            key = { page -> primaryTabs[page]::class.qualifiedName ?: page.toString() }
                                        ) { page ->
                                            val pageTab = primaryTabs[page]
                                            val pageModifier = Modifier
                                                .fillMaxSize()
                                                .nestedScroll(carouselScrollBoundary)

                                            val contentModifier = if (pageTab is AccountTab) {
                                                pageModifier
                                            } else {
                                                pageModifier.graphicsLayer {
                                                    val rawOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                                    val absOffset = kotlin.math.abs(rawOffset).coerceIn(0f, 1f)
                                                    alpha = (1f - absOffset).coerceIn(0f, 1f)
                                                }
                                            }

                                            val offsetProvider = remember(pagerState, page) {
                                                {
                                                    ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceAtMost(0f)
                                                }
                                            }
                                            val alphaProvider = remember(offsetProvider) {
                                                {
                                                    (1f - kotlin.math.abs(offsetProvider())).coerceIn(0f, 1f)
                                                }
                                            }
                                            CompositionLocalProvider(
                                                com.cinetrack.ui.components.common.LocalRenderCinematicBackground provides false,
                                                com.cinetrack.ui.LocalAccountBackdropAlpha provides alphaProvider,
                                                com.cinetrack.ui.LocalAccountBackdropOffset provides offsetProvider
                                            ) {
                                                Box(modifier = contentModifier) {
                                                    tabNavigator.saveableState(pageTab::class.simpleName ?: "tab", pageTab) {
                                                        pageTab.Content()
                                                    }
                                                }
                                            }
                                        }
                                    } else if (secondaryTab is FolderDetailTab) {
                                        secondaryTab.Content()
                                    } else {
                                        tabNavigator.saveableState(secondaryTab::class.simpleName ?: "tab", secondaryTab) {
                                            secondaryTab.Content()
                                        }
                                    }
                                }
                                }
                            }

                            // Top Bar Layer
                            val title = when (currentTab) {
                                is HomeFeedTab -> stringResource(R.string.app_name)
                                is HomeTab -> stringResource(R.string.bottom_bar_to_watch)
                                is VistiTab -> stringResource(R.string.main_tab_visti)
                                is DiscoverTab -> {
                                    when (DiscoverTab.requestedType) {
                                        "now_playing_movies", "now_playing" -> stringResource(R.string.home_section_now_in_theaters)
                                        "upcoming_movies", "upcoming", "upcoming_tv" -> stringResource(R.string.home_section_upcoming)
                                        "airing_today_tv", "on_the_air_tv", "streaming_tv" -> stringResource(R.string.home_section_now_streaming)
                                        "popular_movies", "popular", "popular_tv" -> stringResource(R.string.home_section_popular)
                                        else -> stringResource(R.string.main_tab_discover)
                                    }
                                }
                                is RecommendationsTab -> stringResource(R.string.main_tab_recommendations)
                                is AccountTab -> stringResource(R.string.bottom_bar_account)
                                is FoldersTab -> stringResource(R.string.main_tab_folders)
                                is StatsTab -> stringResource(R.string.bottom_bar_stats)
                                is SettingsTab -> stringResource(R.string.main_tab_settings)
                                is NewsTab -> stringResource(R.string.news_tab_title)
                                is BoxOfficeTab -> stringResource(R.string.box_office_title)
                                is FolderDetailTab -> activeFolderForDetail?.name ?: currentTab.folderName
                                is FlowTab -> "Flow"
                                is FlowStatsTab -> stringResource(R.string.flow_stats_title)
                                is TrophyRoomTab -> stringResource(R.string.dashboard_trophies)
                                is HistoryTab -> stringResource(R.string.history_screen_title)
                                else -> stringResource(R.string.app_name)
                            }

                            val recommendationsViewModel: RecommendationsViewModel? = if (currentTab is RecommendationsTab && activity != null) {
                                hiltViewModel(activity)
                            } else null

                            val boxOfficeViewModel: BoxOfficeViewModel? = if (currentTab is BoxOfficeTab && activity != null) {
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

                                var flowHasActiveFilters = false
                                if (currentTab is FlowTab && activity != null) {
                                    val flowVm = hiltViewModel<FlowViewModel>(activity)
                                    val flowUiState by flowVm.uiState.collectAsStateWithLifecycle()
                                    flowHasActiveFilters = flowUiState.filterConfig.hasActiveFilters
                                }

                                var historyHasActiveFilters = false
                                var historyCustomActions: (@Composable () -> Unit)? = null
                                if (currentTab is HistoryTab && activity != null) {
                                    val historyVm = hiltViewModel<HistoryViewModel>(activity)
                                    val historyUiState by historyVm.uiState.collectAsStateWithLifecycle()
                                    historyHasActiveFilters = historyUiState.filterConfig.isFiltered
                                    historyCustomActions = {
                                        HistoryViewSwitcher(
                                            isTimeline = historyUiState.selectedViewMode == ViewMode.TIMELINE,
                                            onSelectMode = { historyVm.setViewMode(it) },
                                            hazeState = contentHazeState
                                        )
                                    }
                                }

                                var foldersHasActiveFilters = false
                                if (currentTab is FoldersTab && activity != null) {
                                    val foldersVm = hiltViewModel<com.cinetrack.ui.viewmodel.FoldersViewModel>(activity)
                                    val currentTypeFilter by foldersVm.typeFilter.collectAsStateWithLifecycle()
                                    foldersHasActiveFilters = currentTypeFilter != com.cinetrack.ui.screens.FolderTypeFilter.ALL
                                }

                                GlassyTopBar(
                                    title = title,
                                    hazeState = contentHazeState,
                                    isDimmed = isBarDimmed,
                                    onDimmedAreaClick = {
                                        if (isTrophyDetailOpen) {
                                            activeTrophyDetailConfig.value?.onClose?.invoke()
                                        } else {
                                            settingsViewModel.triggerCloseDialogs()
                                        }
                                    },
                                    onMenuClick = null, // Menu rimosso
                                    onBackPress = if (currentTab is FolderDetailTab) { { tabNavigator.current = FoldersTab } } else if (currentTab is StatsTab || currentTab is FoldersTab || currentTab is FlowTab || currentTab is FlowStatsTab || currentTab is SettingsTab || currentTab is TrophyRoomTab || currentTab is HistoryTab) { { tabNavigator.current = AccountTab } } else if (currentTab is DiscoverTab || currentTab is RecommendationsTab || currentTab is NewsTab || currentTab is BoxOfficeTab) { { tabNavigator.current = previousTab.takeIf { it != currentTab } ?: HomeFeedTab } } else null,
                                    onFolderOptionsClick = if (currentTab is FolderDetailTab) { { offset -> showFolderOptions = true; folderOptionsOffset = offset } } else null,
                                    indicatorColor = if (currentTab is FolderDetailTab) (activeFolderForDetail?.color ?: currentTab.folderColor)?.toComposeColor() else null,
                                    onRefreshClick = if (currentTab is RecommendationsTab) {
                                        {
                                            recommendationsViewModel?.lazyGridState?.let { gridState ->
                                                scope.launch { gridState.scrollToItem(0) }
                                            }
                                            recommendationsViewModel?.onRefresh()
                                        }
                                    } else null,
                                    onFilterClick = if (currentTab is DiscoverTab) { { bounds -> isFilterModalVisible = true; filterButtonBounds = bounds } } else if (currentTab is FoldersTab) { { bounds -> showFoldersSortMenu = true; foldersFilterButtonBounds = bounds } } else if (currentTab is FlowTab) { { bounds -> isFilterModalVisible = true; filterButtonBounds = bounds } } else if (currentTab is TrophyRoomTab) { { bounds -> isFilterModalVisible = true; filterButtonBounds = bounds } } else if (currentTab is HistoryTab) { { bounds -> isFilterModalVisible = true; filterButtonBounds = bounds } } else null,
                                    hasActiveFilters = if (currentTab is FlowTab) flowHasActiveFilters else if (currentTab is TrophyRoomTab) (activeTrophyFilterConfig.value?.config?.hasActiveFilters == true) else if (currentTab is HistoryTab) historyHasActiveFilters else if (currentTab is FoldersTab) foldersHasActiveFilters else discoverHasActiveFilters,
                                    onLayoutToggleClick = discoverOnLayoutToggleClick,
                                    onUpdatesClick = if (currentTab is HomeFeedTab || currentTab is HomeTab || currentTab is VistiTab || currentTab is AccountTab) {
                                        { offset ->
                                            updatesOverlayOffsetX = offset.x
                                            updatesOverlayOffsetY = offset.y
                                        }
                                    } else null,
                                    notificationCount = updatesUiState.totalUnreadCount,
                                    hasAppUpdateBadge = hasAppUpdateBadge,
                                    customActions = historyCustomActions,
                                    onEditBackdropClick = if (currentTab is AccountTab) { 
                                        { bounds ->
                                             settingsViewModel.triggerEditProfileMenu(bounds)
                                        } 
                                    } else null,
                                    onEditBackdropBoundsChanged = if (currentTab is AccountTab) {
                                        { bounds ->
                                            settingsViewModel.editProfilePencilBounds = bounds
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
                            val isMediaSwitchSupported = enableBottomBarSwipe && (currentTab is HomeFeedTab || currentTab is HomeTab || currentTab is VistiTab || currentTab is FolderDetailTab)

                            val currentMediaMode = when (currentTab) {
                                is VistiTab -> vistiViewModel.uiState.value.activeTab
                                is FolderDetailTab -> folderDetailMediaHandler.value?.activeTab ?: "all"
                                is HomeFeedTab, is HomeTab -> homeViewModel.uiState.value.activeTab
                                else -> null
                            }

                            val onSwitchMediaMode: ((String) -> Unit)? = if (isMediaSwitchSupported) {
                                { newMode ->
                                    when (currentTab) {
                                        is FolderDetailTab -> {
                                            folderDetailMediaHandler.value?.onTabChanged?.invoke(newMode)
                                        }
                                        is VistiTab -> {
                                            vistiViewModel.onTabChanged(newMode)
                                        }
                                        is HomeTab -> {
                                            homeViewModel.onTabChanged(newMode)
                                        }
                                        is HomeFeedTab -> {
                                            homeViewModel.onTabChanged(newMode)
                                            scope.launch {
                                                homeViewModel.feedListState.scrollToItem(0)
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            } else null

                            MainBottomBarOverlay(
                                currentTab = currentTab,
                                contentHazeState = contentHazeState,
                                isSettingsDialogOpen = isBarDimmed,
                                onDimmedAreaClick = {
                                    if (isTrophyDetailOpen) {
                                        activeTrophyDetailConfig.value?.onClose?.invoke()
                                    } else {
                                        settingsViewModel.triggerCloseDialogs()
                                    }
                                },
                                onNavigate = { routeStr ->
                                    val targetTab = when (routeStr) {
                                        "feed" -> HomeFeedTab
                                        "index" -> HomeTab
                                        "visti" -> VistiTab
                                        "account" -> AccountTab
                                        else -> null
                                    }
                                    if (targetTab != null) {
                                        val targetIndex = primaryTabs.indexOf(targetTab)
                                        if (targetIndex >= 0) {
                                            if (targetIndex == pagerState.currentPage && currentTab == targetTab) {
                                                scrollToTopState.value.onScrollToTop.invoke()
                                            } else {
                                                scope.launch {
                                                    pagerState.scrollToPage(targetIndex)
                                                }
                                            }
                                        }
                                        tabNavigator.current = targetTab
                                    }
                                },
                                currentMediaMode = currentMediaMode,
                                onSwitchMediaMode = onSwitchMediaMode
                            )

                            MainScrollToTopFab(
                                visible = scrollToTopState.value.isVisible && currentTab !is SettingsTab,
                                scrollProgress = if (currentTab !is SettingsTab) scrollToTopState.value.scrollProgress else 0f,
                                contentHazeState = contentHazeState,
                                isDimmed = isBarDimmed,
                                onDimmedAreaClick = {
                                    if (isTrophyDetailOpen) {
                                        activeTrophyDetailConfig.value?.onClose?.invoke()
                                    } else {
                                        settingsViewModel.triggerCloseDialogs()
                                    }
                                },
                                onScrollToTop = scrollToTopState.value.onScrollToTop
                            )

                            MainSearchFab(
                                currentTab = currentTab,
                                contentHazeState = contentHazeState,
                                isDimmed = isBarDimmed,
                                onDimmedAreaClick = {
                                    if (isTrophyDetailOpen) {
                                        activeTrophyDetailConfig.value?.onClose?.invoke()
                                    } else {
                                        settingsViewModel.triggerCloseDialogs()
                                    }
                                },
                                onSearchClick = { offset ->
                                    val initialCategory = when {
                                        currentTab is HomeFeedTab || currentTab is HomeTab -> {
                                            if (homeViewModel.uiState.value.activeTab == "tv") "tv" else "movie"
                                        }
                                        else -> null
                                    }
                                    searchOverlay?.invoke(offset = offset, category = initialCategory)
                                }
                            )

                            // Overlay Globale Simbionte (Renderizzato sopra top bar, filtri e categorie)
                            SymbiontGlobalIndicator(
                                state = symbiontPullState,
                                hazeState = contentHazeState,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .zIndex(200f)
                            )
                        }

                        // --- FOLDER OPTIONS MODAL ---
                        val currentFolderForMenu = activeFolderForDetail ?: if (currentTab is FolderDetailTab) allFoldersList?.find { it.id == currentTab.folderId } else null
                        val isCurrentFolderShared = currentFolderForMenu?.isShared == true
                        val isCurrentFolderOwner = currentFolderForMenu == null || currentFolderForMenu.ownerUid == currentUser?.uid

                        if (currentTab is FolderDetailTab || showFolderOptions) {
                            MainFolderOptionsMenu(
                                visible = showFolderOptions && currentTab is FolderDetailTab,
                                offset = folderOptionsOffset,
                                hazeState = contentHazeState,
                                isShared = isCurrentFolderShared,
                                isOwner = isCurrentFolderOwner,
                                onDismiss = { showFolderOptions = false },
                                onShareOrMembers = {
                                    showFolderOptions = false
                                    if (currentUser == null || currentUser.isAnonymous) {
                                        settingsViewModel.triggerGuestAuthDialog()
                                    } else if (isCurrentFolderShared) {
                                        showFolderMembersModal = true
                                    } else {
                                        showFolderShareModal = true
                                    }
                                },
                                onEdit = {
                                    showFolderOptions = false
                                    folderEditMode = FolderEditMode.ALL
                                    showFolderEditDialog = true
                                },
                                onReorder = {
                                    showFolderOptions = false
                                    folderReorderRequest.value?.invoke()
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

                        if (foldersViewModelForSort != null) {
                            val currentSortOption by foldersViewModelForSort.sortOption.collectAsStateWithLifecycle()
                            val currentSortOrder by foldersViewModelForSort.sortOrder.collectAsStateWithLifecycle()
                            val currentTypeFilter by foldersViewModelForSort.typeFilter.collectAsStateWithLifecycle()

                            com.cinetrack.ui.components.folder.FoldersFilterModal(
                                isVisible = showFoldersSortMenu,
                                triggerBounds = foldersFilterButtonBounds,
                                hazeState = contentHazeState,
                                currentSortOption = currentSortOption,
                                currentSortOrder = currentSortOrder,
                                currentTypeFilter = currentTypeFilter,
                                onDismissRequest = { showFoldersSortMenu = false },
                                onApplyFilters = { newOption, newOrder, newTypeFilter ->
                                    foldersViewModelForSort.updateFilters(newOption, newOrder, newTypeFilter)
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
                            onYearPickerDismiss = { isYearPickerVisible = false },
                            rootNavigator = rootNavigator
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
                            showFolderShareModal = showFolderShareModal,
                            onFolderShareModalChange = { showFolderShareModal = it },
                            showFolderMembersModal = showFolderMembersModal,
                            onFolderMembersModalChange = { showFolderMembersModal = it },
                            updatesOverlayOffset = updatesOverlayOffset,
                            onUpdatesOverlayClose = { updatesOverlayOffsetX = null; updatesOverlayOffsetY = null },
                            settingsOverlayOffset = settingsOverlayOffset,
                            onSettingsOverlayClose = { settingsOverlayOffsetX = null; settingsOverlayOffsetY = null },
                            onOverlayClosing = { isOverlayClosing = true },
                            showSurpriseMeOverlay = showSurpriseMeOverlay,
                            surpriseMeButtonBounds = surpriseMeButtonBounds,
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
