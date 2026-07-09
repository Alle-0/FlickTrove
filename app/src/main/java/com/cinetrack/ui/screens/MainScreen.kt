package com.cinetrack.ui.screens

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import cafe.adriel.voyager.hilt.getViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.cinetrack.R
import com.cinetrack.util.toComposeColor
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalDeepLinkIntent
import com.cinetrack.ui.LocalFilterRequest
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.viewmodel.VistiViewModel
import com.cinetrack.ui.components.GlassyBottomBar
import com.cinetrack.ui.components.GlassyDrawer
import com.cinetrack.ui.components.GlassyTopBar
import com.cinetrack.ui.components.HomeFilterModal
import com.cinetrack.ui.components.stats.YearSelectionModal
import com.cinetrack.ui.components.UndoToast
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.main.MainFolderOptionsMenu
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.DiscoverViewModel
import com.cinetrack.ui.viewmodel.FolderDetailViewModel
import com.cinetrack.ui.viewmodel.HomeViewModel
import com.cinetrack.ui.viewmodel.StatsViewModel
import com.cinetrack.ui.viewmodel.UndoViewModel
import com.cinetrack.ui.viewmodel.UpdatesViewModel
import com.cinetrack.ui.viewmodel.TimeRange
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
        val searchOverlay = com.cinetrack.ui.LocalSearchOverlay.current
        
        var currentContext = LocalContext.current
        while (currentContext is android.content.ContextWrapper && currentContext !is androidx.activity.ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? androidx.activity.ComponentActivity

        val settingsViewModel = if (activity != null) {
            androidx.hilt.navigation.compose.hiltViewModel<com.cinetrack.ui.viewmodel.SettingsViewModel>(activity)
        } else {
            androidx.hilt.navigation.compose.hiltViewModel<com.cinetrack.ui.viewmodel.SettingsViewModel>()
        }
        val isSettingsDialogOpen by settingsViewModel.isAnyDialogOpen.collectAsStateWithLifecycle()
        

        // Hoisted Modals State
        var isFilterModalVisible by remember { mutableStateOf(false) }
        var filterButtonBounds by remember { mutableStateOf<Rect?>(null) }
        var isYearPickerVisible by remember { mutableStateOf(false) }
        var yearPickerButtonBounds by remember { mutableStateOf<Rect?>(null) }
        
        var showFolderOptions by remember { mutableStateOf(false) }
        var folderOptionsOffset by remember { mutableStateOf(Offset.Zero) }
        var showFolderEditDialog by remember { mutableStateOf(false) }
        var folderEditMode by remember { mutableStateOf(com.cinetrack.ui.components.shared.FolderEditMode.NAME) }
        var showFolderDeleteConfirm by remember { mutableStateOf(false) }
        
        var showExitConfirmation by remember { mutableStateOf(false) }

        var updatesOverlayOffsetX by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Float?>(null) }
        var updatesOverlayOffsetY by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Float?>(null) }
        val updatesOverlayOffset = if (updatesOverlayOffsetX != null && updatesOverlayOffsetY != null) Offset(updatesOverlayOffsetX!!, updatesOverlayOffsetY!!) else null
        var isOverlayClosing by remember { mutableStateOf(false) }
        
        var showSurpriseMeOverlay by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
        val surpriseMeViewModel: com.cinetrack.ui.viewmodel.SurpriseMeViewModel = getViewModel()
        val context = LocalContext.current
        
        val deepLinkIntent = LocalDeepLinkIntent.current
        val movieActions = com.cinetrack.ui.components.shared.LocalMovieActions.current

        val initialTab = if (initialTabStr == "visti") VistiTab else HomeTab

        TabNavigator(initialTab) { tabNavigator ->
            val currentTab = tabNavigator.current

            LaunchedEffect(deepLinkIntent.value) {
                val intent = deepLinkIntent.value ?: return@LaunchedEffect
                val uri = intent.data
                val action = intent.action
                if (uri != null || action?.startsWith("com.cinetrack.SHORTCUT_") == true) {
                    val isCustomScheme = uri != null && uri.scheme == "flicktrove" && (uri.host == "media" || uri.host == "detail")

                    if (uri?.scheme == "https" && uri.host == "alle-0.github.io" && uri.path?.startsWith("/FlickTrove/open.html") == true) {
                        // HTTPS App Link: https://alle-0.github.io/FlickTrove/open.html?type=movie&id=123
                        val type = uri.getQueryParameter("type") ?: "movie"
                        val id   = uri.getQueryParameter("id")?.toLongOrNull()
                        if (id != null) {
                            if (type == "person") rootNavigator.push(PersonDetailScreen(id, null))
                            else rootNavigator.push(MovieDetailScreen(id, type))
                        }
                    } else if (uri?.scheme == "flicktrove" && uri.host == "auth") {
                        // Trakt OAuth callback: flicktrove://auth?code=XXXXX
                        val code = uri.getQueryParameter("code")
                        if (!code.isNullOrEmpty()) {
                            settingsViewModel.exchangeTraktCode(code)
                        }
                    } else if (isCustomScheme) {
                        val segments = uri.pathSegments
                        
                        // For flicktrove://media/movie/123, segments are ["movie", "123"]
                        if (segments.size >= 2) {
                            val type = segments[0]
                            val id = segments[1].toLongOrNull()
                            if (id != null) {
                                if (type == "person") rootNavigator.push(PersonDetailScreen(id, null))
                                else rootNavigator.push(MovieDetailScreen(id, type))
                            }
                        }
                    } else if (uri?.scheme == "flicktrove" && uri.host == "search") {
                        searchOverlay?.invoke(androidx.compose.ui.geometry.Offset(540f, 1140f), null, null, null, null)
                    } else if (action == "com.cinetrack.SHORTCUT_SEARCH") {
                        searchOverlay?.invoke(androidx.compose.ui.geometry.Offset(540f, 1140f), null, null, null, null)
                    } else if (action == "com.cinetrack.SHORTCUT_VISTI") {
                        tabNavigator.current = VistiTab
                    } else if (action == "com.cinetrack.SHORTCUT_UPCOMING_MOVIES") {
                        DiscoverTab.requestedType = "upcoming_movies"
                        tabNavigator.current = DiscoverTab
                    } else if (action == "com.cinetrack.SHORTCUT_UPCOMING_TV") {
                        DiscoverTab.requestedType = "on_the_air_tv"
                        tabNavigator.current = DiscoverTab
                    }
                    
                    deepLinkIntent.value = null
                }
            }

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
                    val activeFilterConfig = remember { mutableStateOf<com.cinetrack.ui.FilterModalConfig?>(null) }
                    Box(modifier = Modifier.fillMaxSize().haze(globalHazeState)) {
                    CompositionLocalProvider(
                        LocalAppPadding provides PaddingValues(bottom = 80.dp),
                        LocalHazeState provides contentHazeState,
                        com.cinetrack.ui.LocalActiveFilterConfig provides activeFilterConfig,
                        LocalFilterRequest provides { bounds ->
                            filterButtonBounds = bounds
                            isFilterModalVisible = true
                        }
                    ) {
                        CurrentTab()
                    }

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

                    val context = LocalContext.current
                    var currentContext = context
                    while (currentContext is android.content.ContextWrapper && currentContext !is androidx.activity.ComponentActivity) {
                        currentContext = currentContext.baseContext
                    }
                    val activity = currentContext as? androidx.activity.ComponentActivity

                    val recommendationsViewModel: com.cinetrack.ui.viewmodel.RecommendationsViewModel? = if (currentTab is RecommendationsTab && activity != null) {
                        androidx.hilt.navigation.compose.hiltViewModel(activity)
                    } else null

                    Box(modifier = Modifier.align(Alignment.TopCenter).zIndex(50f)) {
                        var discoverHasActiveFilters = false
                        var discoverGridColumns: Int? = null
                        var discoverOnLayoutToggleClick: (() -> Unit)? = null

                        if (currentTab is DiscoverTab && activity != null) {
                            val discoverVm = androidx.hilt.navigation.compose.hiltViewModel<DiscoverViewModel>(activity)
                            val discoverUiStateForBar by discoverVm.uiState.collectAsStateWithLifecycle()
                            discoverHasActiveFilters = discoverUiStateForBar.sortConfig.selectedGenres.isNotEmpty() ||
                                discoverUiStateForBar.sortConfig.selectedProviders.isNotEmpty() ||
                                discoverUiStateForBar.sortConfig.selectedDecades.isNotEmpty()
                            if (discoverUiStateForBar.preferences.showLayoutToggle) {
                                val cols = if (discoverUiStateForBar.preferences.gridColumns in 1..4) discoverUiStateForBar.preferences.gridColumns else 3
                                discoverGridColumns = cols
                                discoverOnLayoutToggleClick = {
                                    discoverVm.updateGridColumns(com.cinetrack.ui.components.shared.nextGridColumns(cols))
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
                            layoutColumns = discoverGridColumns
                        )
                    }

                    // Bottom Bar Layer
                    val isPrimaryTab = currentTab is HomeTab || currentTab is VistiTab || currentTab is StatsTab || currentTab is DiscoverTab || currentTab is FoldersTab || currentTab is FolderDetailTab || currentTab is SettingsTab || currentTab is RecommendationsTab || currentTab is NewsTab
                    if (isPrimaryTab) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter).zIndex(50f)) {
                            GlassyBottomBar(
                                hazeState = contentHazeState,
                                isDimmed = isSettingsDialogOpen,
                                onDimmedAreaClick = { settingsViewModel.triggerCloseDialogs() },
                                selectedRoute = when (currentTab) {
                                    is HomeTab -> "index"
                                    is VistiTab -> "visti"
                                    is StatsTab -> "stats"
                                    else -> null
                                },
                                onNavigate = { routeStr ->
                                    when (routeStr) {
                                        "index" -> tabNavigator.current = HomeTab
                                        "visti" -> tabNavigator.current = VistiTab
                                        "stats" -> tabNavigator.current = StatsTab
                                    }
                                }
                            )
                        }
                    }

                    // Search FAB
                    if (isPrimaryTab && currentTab !is SettingsTab) {
                        val searchFabHaze = contentHazeState
                        val fabCenter = remember { arrayOf<androidx.compose.ui.geometry.Offset?>(null) }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = 90.dp)
                                .zIndex(60f)
                                .size(52.dp)
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInWindow()
                                    val size = coords.size
                                    fabCenter[0] = androidx.compose.ui.geometry.Offset(
                                        pos.x + size.width / 2f,
                                        pos.y + size.height / 2f
                                    )
                                }
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .hazeGlass(
                                    state = searchFabHaze,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .border(
                                    1.dp,
                                    com.cinetrack.ui.theme.HazeStyles.GlassBorderColor.copy(alpha = com.cinetrack.ui.theme.HazeStyles.GlassBorderAlphaTop),
                                    androidx.compose.foundation.shape.CircleShape
                                )
                                .bounceClick(scaleDown = 0.9f) {
                                    val center = fabCenter[0]
                                    val startX = center?.x ?: 540f
                                    val startY = center?.y ?: 1140f
                                    searchOverlay?.invoke(androidx.compose.ui.geometry.Offset(startX, startY), null, null, null, null)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_lente),
                                contentDescription = stringResource(R.string.main_cd_search),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

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
                                folderEditMode = com.cinetrack.ui.components.shared.FolderEditMode.NAME
                                showFolderEditDialog = true
                            },
                            onChangeColor = {
                                showFolderOptions = false
                                folderEditMode = com.cinetrack.ui.components.shared.FolderEditMode.COLOR
                                showFolderEditDialog = true
                            },
                            onDelete = {
                                showFolderOptions = false
                                showFolderDeleteConfirm = true
                            }
                        )
                    }

                    // Modals — wrapped in key(currentTab) to reset composable state safely on tab change
                    key(currentTab) {
                        if (currentTab is HomeTab) {
                            val homeViewModel = if (activity != null) {
                                androidx.hilt.navigation.compose.hiltViewModel<HomeViewModel>(activity)
                            } else {
                                getViewModel()
                            }
                            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                            Box(modifier = Modifier.zIndex(70000f)) {
                                HomeFilterModal(
                                    isVisible = isFilterModalVisible,
                                    sortConfig = homeUiState.sortConfig,
                                    hazeState = globalHazeState,
                                    triggerBounds = filterButtonBounds,
                                    onSortConfigChanged = { homeViewModel.updateSortConfig(it); isFilterModalVisible = false },
                                    onDismissRequest = { isFilterModalVisible = false }
                                )
                            }
                        } else if (currentTab is VistiTab) {
                            val vistiViewModel = if (activity != null) {
                                androidx.hilt.navigation.compose.hiltViewModel<VistiViewModel>(activity)
                            } else {
                                getViewModel()
                            }
                            val vistiUiState by vistiViewModel.uiState.collectAsStateWithLifecycle()
                            Box(modifier = Modifier.zIndex(70000f)) {
                                HomeFilterModal(
                                    isVisible = isFilterModalVisible,
                                    isVisti = true,
                                    sortConfig = vistiUiState.sortConfig,
                                    hazeState = globalHazeState,
                                    triggerBounds = filterButtonBounds,
                                    category = vistiUiState.activeTab,
                                    onSortConfigChanged = { vistiViewModel.updateSortConfig(it); isFilterModalVisible = false },
                                    onDismissRequest = { isFilterModalVisible = false }
                                )
                            }
                        } else if (currentTab is DiscoverTab) {
                            val discoverViewModel = if (activity != null) {
                                androidx.hilt.navigation.compose.hiltViewModel<DiscoverViewModel>(activity)
                            } else {
                                getViewModel()
                            }
                            val discoverUiState by discoverViewModel.uiState.collectAsStateWithLifecycle()
                            Box(modifier = Modifier.zIndex(70000f)) {
                                HomeFilterModal(
                                    isVisible = isFilterModalVisible,
                                    sortConfig = discoverUiState.sortConfig,
                                    hazeState = globalHazeState,
                                    triggerBounds = filterButtonBounds,
                                    showSortBy = false,
                                    onSortConfigChanged = { discoverViewModel.updateSortConfig(it); isFilterModalVisible = false },
                                    onDismissRequest = { isFilterModalVisible = false }
                                )
                            }
                        } else if (currentTab is StatsTab) {
                            val statsViewModel = if (activity != null) {
                                androidx.hilt.navigation.compose.hiltViewModel<StatsViewModel>(activity)
                            } else {
                                getViewModel()
                            }
                            val statsUiState by statsViewModel.uiState.collectAsStateWithLifecycle()
                            Box(modifier = Modifier.zIndex(70000f)) {
                                YearSelectionModal(
                                    isVisible = isYearPickerVisible,
                                    onDismiss = { isYearPickerVisible = false },
                                    currentRange = statsUiState.timeRange,
                                    availableYears = statsUiState.availableYears,
                                    hazeState = globalHazeState,
                                    triggerBounds = yearPickerButtonBounds,
                                    onYearSelected = { statsViewModel.setTimeRange(TimeRange.Year(it)) },
                                    onAllTimeSelected = { statsViewModel.setTimeRange(TimeRange.AllTime) }
                                )
                            }
                        } else if (currentTab is FolderDetailTab) {
                            val filterConfig = activeFilterConfig.value
                            if (filterConfig != null) {
                                Box(modifier = Modifier.zIndex(70000f)) {
                                    HomeFilterModal(
                                        isVisible = isFilterModalVisible,
                                        isVisti = filterConfig.isVisti,
                                        sortConfig = filterConfig.sortConfig,
                                        hazeState = globalHazeState,
                                        triggerBounds = filterButtonBounds,
                                        category = filterConfig.category,
                                        onSortConfigChanged = { 
                                            filterConfig.onSortConfigChanged(it)
                                            isFilterModalVisible = false
                                        },
                                        onDismissRequest = { isFilterModalVisible = false }
                                    )
                                }
                            }
                        }
                    }



                    // Exit Confirmation
                    if (showExitConfirmation) {
                        com.cinetrack.ui.components.shared.FlickTroveModal(
                            isVisible = true,
                            onDismissRequest = { showExitConfirmation = false },
                            hazeState = globalHazeState
                        ) {
                            Text(
                                text = stringResource(R.string.main_exit_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.main_exit_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val context = LocalContext.current
                                Box(
                                    modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .bounceClick { showExitConfirmation = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.main_exit_cancel), color = Color.White)
                                }
                                Box(
                                    modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .bounceClick { showExitConfirmation = false; (context as? ComponentActivity)?.finish() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.main_exit_confirm), color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (showFolderEditDialog && currentTab is FolderDetailTab) {
                        val foldersViewModel: com.cinetrack.ui.viewmodel.FoldersViewModel = getViewModel()
                        val folderId = currentTab.folderId
                        val folderFlow = foldersViewModel.folders.collectAsStateWithLifecycle()
                        val folder = folderFlow.value.find { it.id == folderId }
                        if (folder != null) {
                            com.cinetrack.ui.components.shared.FolderEditDialog(
                                initialName = folder.name,
                                initialColor = folder.color ?: "#FFFFFF",
                                editMode = folderEditMode,
                                hazeState = globalHazeState,
                                onDismiss = { showFolderEditDialog = false },
                                onSave = { newName, newColor ->
                                    foldersViewModel.updateFolder(folder.copy(name = newName, color = newColor))
                                    showFolderEditDialog = false
                                    // Aggiorna il tab con il nuovo nome/colore
                                    val tab = currentTab as FolderDetailTab
                                    tabNavigator.current = FolderDetailTab(
                                        folderId = tab.folderId,
                                        folderName = if (folderEditMode == com.cinetrack.ui.components.shared.FolderEditMode.NAME) newName else tab.folderName,
                                        folderColor = if (folderEditMode == com.cinetrack.ui.components.shared.FolderEditMode.COLOR) newColor else tab.folderColor
                                    )
                                }
                            )
                        }
                    }

                    if (showFolderDeleteConfirm && currentTab is FolderDetailTab) {
                        val foldersViewModel: com.cinetrack.ui.viewmodel.FoldersViewModel = getViewModel()
                        val folderId = currentTab.folderId
                        val folderFlow = foldersViewModel.folders.collectAsStateWithLifecycle()
                        val folder = folderFlow.value.find { it.id == folderId }
                        
                        if (folder != null) {
                            com.cinetrack.ui.components.shared.FlickTroveModal(
                                isVisible = true,
                                onDismissRequest = { showFolderDeleteConfirm = false },
                                hazeState = globalHazeState
                            ) {
                                Text(
                                    text = stringResource(R.string.folder_delete_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = stringResource(R.string.folder_delete_confirm_prefix_2) + "\"${folder.name}\"" + stringResource(R.string.folder_delete_confirm_suffix),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .bounceClick { showFolderDeleteConfirm = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(stringResource(R.string.folder_delete_cancel), color = Color.White)
                                    }
                                    Box(
                                        modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                                            .background(Color(0xFFFF3B30))
                                            .bounceClick { 
                                                showFolderDeleteConfirm = false
                                                foldersViewModel.deleteFolder(folderId)
                                                tabNavigator.current = FoldersTab
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(stringResource(R.string.folder_delete_short), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (updatesOverlayOffset != null) {
                        UpdatesScreen(
                            viewModel = getViewModel(),
                            paddingValues = PaddingValues(bottom = 80.dp),
                            startX = updatesOverlayOffset.x,
                            startY = updatesOverlayOffset.y,
                            onBack = { updatesOverlayOffsetX = null; updatesOverlayOffsetY = null },
                            onClosing = { isOverlayClosing = true },
                            onMovieClick = { movie ->
                                rootNavigator.push(MovieDetailScreen(movie.id, movie.mediaType))
                            },
                            modifier = Modifier.zIndex(80000f)
                        )
                    }

                    if (showSurpriseMeOverlay) {
                        Box(modifier = Modifier.zIndex(90000f)) {
                            SurpriseMeOverlay(
                                viewModel = surpriseMeViewModel,
                                globalHazeState = globalHazeState,
                                onMovieFound = { movie ->
                                    showSurpriseMeOverlay = false
                                    if (movie != null) {
                                        rootNavigator.push(MovieDetailScreen(movie.id, movie.mediaType))
                                    } else {
                                        android.widget.Toast.makeText(context, context.getString(R.string.main_surprise_me_not_found), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onClose = { showSurpriseMeOverlay = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
