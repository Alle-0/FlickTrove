package com.cinetrack.ui.screens

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.cinetrack.util.VibrationHelper
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import com.cinetrack.util.toComposeColor
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.cinetrack.ui.LocalModalHost
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.ui.viewmodel.FoldersViewModel
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.fastScrollToTop
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.components.shared.FolderColorPicker
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.HazeStyle
import com.cinetrack.ui.components.shared.DeleteFolderDialog
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.shared.FolderCard
import com.cinetrack.ui.components.shared.NewFolderCard
import com.cinetrack.ui.components.shared.JoinFolderCard
import com.cinetrack.ui.components.shared.FolderCreateDialog
import com.cinetrack.ui.components.shared.FolderJoinDialog
import com.cinetrack.ui.components.shared.FolderEditDialog
import com.cinetrack.ui.components.shared.FolderEditMode
import com.cinetrack.ui.components.shared.FolderShareModal
import com.cinetrack.ui.components.shared.FolderMembersModal
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.EaseInCirc
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalHazeState

enum class FolderSortOption { DATE, NAME, ITEMS }
enum class FolderTypeFilter { ALL, SHARED, PRIVATE }

object FoldersTab : Tab {
    var pendingJoinCode: String? = null

    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(id = R.string.folders_tab_title)
            return remember(title) {
                TabOptions(
                    index = 3u,
                    title = title,
                    icon = null
                )
            }
        }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        var currentContext = context
        while (currentContext is android.content.ContextWrapper && currentContext !is androidx.activity.ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? androidx.activity.ComponentActivity
        val viewModel = if (activity != null) {
            androidx.hilt.navigation.compose.hiltViewModel<FoldersViewModel>(activity)
        } else {
            getViewModel<FoldersViewModel>()
        }
        val settingsViewModel = if (activity != null) {
            androidx.hilt.navigation.compose.hiltViewModel<com.cinetrack.ui.viewmodel.SettingsViewModel>(activity)
        } else {
            getViewModel<com.cinetrack.ui.viewmodel.SettingsViewModel>()
        }
        val paddingValues = LocalAppPadding.current
        val hazeState = LocalHazeState.current
        val tabNavigator = LocalTabNavigator.current

        FoldersScreenContent(
            viewModel = viewModel,
            paddingValues = paddingValues,
            hazeState = hazeState,
            onFolderClick = { folder ->
                tabNavigator.current = FolderDetailTab(folder.id, folder.name, folder.color)
            },
            onGuestAuthRequired = {
                settingsViewModel.triggerGuestAuthDialog()
            }
        )
    }
}

@Composable
fun FoldersScreenContent(
    viewModel: FoldersViewModel,
    paddingValues: PaddingValues,
    hazeState: HazeState? = null,
    onFolderClick: (FolderEntity) -> Unit = {},
    onGuestAuthRequired: () -> Unit = {}
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val allMovies by viewModel.allMovies.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    var isCreateDialogOpen by remember { mutableStateOf(false) }
    var isJoinDialogOpen by remember { mutableStateOf(false) }
    var joinDialogInitialCode by remember { mutableStateOf("") }

    val isGuest = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous != false }

    LaunchedEffect(Unit) {
        viewModel.refreshFolders()
        val pending = FoldersTab.pendingJoinCode
        if (!pending.isNullOrBlank()) {
            if (isGuest) {
                onGuestAuthRequired()
                FoldersTab.pendingJoinCode = null
            } else {
                joinDialogInitialCode = pending
                isJoinDialogOpen = true
                FoldersTab.pendingJoinCode = null
            }
        }
    }

    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToEdit by remember { mutableStateOf<FolderEntity?>(null) }
    var folderEditMode by remember { mutableStateOf(FolderEditMode.ALL) }
    var folderToShare by remember { mutableStateOf<FolderEntity?>(null) }
    var folderForMembers by remember { mutableStateOf<FolderEntity?>(null) }
    
    var activeMenuFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var activeMenuBounds by remember { mutableStateOf(Rect.Zero) }
    
    val activeHazeState = hazeState ?: remember { HazeState() }
    val focusManager = LocalFocusManager.current
    
    androidx.activity.compose.BackHandler(enabled = activeMenuFolder != null) {
        activeMenuFolder = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackground(modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = activeHazeState)
        ) {
            if (folders.isNullOrEmpty() && searchQuery.isBlank() && typeFilter == FolderTypeFilter.ALL) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_cartella_piena),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.folders_empty_message),
                            color = Color.White.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(24.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .bounceClick { isCreateDialogOpen = true }
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(ImageVector.vectorResource(id = R.drawable.ic_plus), null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.folders_create_first), 
                                    color = Color.White, 
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .bounceClick {
                                        if (isGuest) onGuestAuthRequired() else isJoinDialogOpen = true
                                    }
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(ImageVector.vectorResource(id = R.drawable.ic_people), null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.folder_join_button), 
                                    color = Color.White, 
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            } else {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                
                val density = androidx.compose.ui.platform.LocalDensity.current
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current

                val scrollToTopStateHolder = com.cinetrack.ui.LocalScrollToTopState.current
                val showScrollToTop by remember {
                    androidx.compose.runtime.derivedStateOf {
                        listState.firstVisibleItemIndex > 3 || (listState.firstVisibleItemIndex > 0 && listState.firstVisibleItemScrollOffset > 150)
                    }
                }
                LaunchedEffect(showScrollToTop, listState) {
                    scrollToTopStateHolder.value = com.cinetrack.ui.ScrollToTopState(
                        isVisible = showScrollToTop,
                        scrollProgress = if (showScrollToTop) 1f else 0f,
                        onScrollToTop = {
                            coroutineScope.launch { listState.fastScrollToTop() }
                        }
                    )
                }
                DisposableEffect(Unit) {
                    onDispose {
                        scrollToTopStateHolder.value = com.cinetrack.ui.ScrollToTopState()
                    }
                }
                
                // Use fixed standard heights to prevent first-frame measurement clipping
                val searchBarMaxHeightDp = 64.dp // 56dp for the bar + 8dp bottom padding
                val newFolderMaxHeightDp = 56.dp // 44dp for the card + 12dp spacing approximately
                
                val searchBarMaxHeightPxDefault = with(density) { searchBarMaxHeightDp.toPx() }
                var searchBarMaxHeightPx by remember { mutableFloatStateOf(searchBarMaxHeightPxDefault) }
                var searchBarHeightPx by remember { mutableFloatStateOf(searchBarMaxHeightPxDefault) }
                
                val newFolderMaxHeightPxDefault = with(density) { newFolderMaxHeightDp.toPx() }
                var newFolderMaxHeightPx by remember { mutableFloatStateOf(newFolderMaxHeightPxDefault) }
                var newFolderHeightPx by remember { mutableFloatStateOf(newFolderMaxHeightPxDefault) }

                var snapJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

                fun performSnap(velocity: Float = 0f) {
                    val searchIsPartial = searchBarHeightPx > 0.5f && searchBarHeightPx < searchBarMaxHeightPx - 0.5f
                    val folderMax = if (searchQuery.isBlank()) newFolderMaxHeightPx else 0f
                    val folderIsPartial = folderMax > 0f && newFolderHeightPx > 0.5f && newFolderHeightPx < folderMax - 0.5f

                    if (!searchIsPartial && !folderIsPartial) return

                    val targetSearch = if (searchIsPartial) {
                        when {
                            velocity < -300f -> 0f
                            velocity > 300f -> searchBarMaxHeightPx
                            searchBarHeightPx < searchBarMaxHeightPx * 0.5f -> 0f
                            else -> searchBarMaxHeightPx
                        }
                    } else searchBarHeightPx

                    val targetFolder = if (folderIsPartial) {
                        when {
                            velocity < -300f -> 0f
                            velocity > 300f -> folderMax
                            newFolderHeightPx < folderMax * 0.5f -> 0f
                            else -> folderMax
                        }
                    } else newFolderHeightPx

                    val startSearch = searchBarHeightPx
                    val startFolder = newFolderHeightPx

                    snapJob?.cancel()
                    snapJob = coroutineScope.launch {
                        val anim = Animatable(0f)
                        anim.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        ) {
                            if (searchIsPartial) {
                                searchBarHeightPx = startSearch + (targetSearch - startSearch) * value
                            }
                            if (folderIsPartial) {
                                newFolderHeightPx = startFolder + (targetFolder - startFolder) * value
                            }
                        }
                        VibrationHelper.vibrateTick(context)
                    }
                }

                val nestedScrollConnection = remember(searchQuery, searchBarMaxHeightPx, newFolderMaxHeightPx) {
                    object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                        override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                            snapJob?.cancel()
                            var consumedY = 0f
                            var delta = available.y

                            // 1. Try to collapse search bar first
                            if (delta < 0 && searchBarMaxHeightPx > 0f && searchBarHeightPx > 0f) {
                                val newHeight = (searchBarHeightPx + delta).coerceIn(0f, searchBarMaxHeightPx)
                                val consumed = newHeight - searchBarHeightPx
                                searchBarHeightPx = newHeight
                                delta -= consumed
                                consumedY += consumed
                            }

                            // 2. If delta is still < 0 (search bar fully collapsed), collapse new folder
                            if (delta < 0 && newFolderMaxHeightPx > 0f && newFolderHeightPx > 0f) {
                                val newHeight = (newFolderHeightPx + delta).coerceIn(0f, newFolderMaxHeightPx)
                                val consumed = newHeight - newFolderHeightPx
                                newFolderHeightPx = newHeight
                                delta -= consumed
                                consumedY += consumed
                            }

                            return androidx.compose.ui.geometry.Offset(0f, consumedY)
                        }

                        override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                            snapJob?.cancel()
                            var consumedY = 0f
                            var delta = available.y

                            // When scrolling up (swiping down, delta > 0), expand in REVERSE order:
                            // First expand new folder
                            if (delta > 0 && newFolderMaxHeightPx > 0f && newFolderHeightPx < newFolderMaxHeightPx) {
                                val newHeight = (newFolderHeightPx + delta).coerceIn(0f, newFolderMaxHeightPx)
                                val consumedAmount = newHeight - newFolderHeightPx
                                newFolderHeightPx = newHeight
                                delta -= consumedAmount
                                consumedY += consumedAmount
                            }

                            // Then expand search bar
                            if (delta > 0 && searchBarMaxHeightPx > 0f && searchBarHeightPx < searchBarMaxHeightPx) {
                                val newHeight = (searchBarHeightPx + delta).coerceIn(0f, searchBarMaxHeightPx)
                                val consumedAmount = newHeight - searchBarHeightPx
                                searchBarHeightPx = newHeight
                                delta -= consumedAmount
                                consumedY += consumedAmount
                            }

                            return androidx.compose.ui.geometry.Offset(0f, consumedY)
                        }

                        override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                            val searchIsPartial = searchBarHeightPx > 0.5f && searchBarHeightPx < searchBarMaxHeightPx - 0.5f
                            val folderMax = if (searchQuery.isBlank()) newFolderMaxHeightPx else 0f
                            val folderIsPartial = folderMax > 0f && newFolderHeightPx > 0.5f && newFolderHeightPx < folderMax - 0.5f

                            if (searchIsPartial || folderIsPartial) {
                                performSnap(available.y)
                                return available
                            }
                            return androidx.compose.ui.unit.Velocity.Zero
                        }
                    }
                }
                
                LaunchedEffect(listState.isScrollInProgress) {
                    if (listState.isScrollInProgress) {
                        focusManager.clearFocus()
                        snapJob?.cancel()
                    } else {
                        performSnap(0f)
                    }
                }
                
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = paddingValues.calculateTopPadding() + androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 120.dp,
                        bottom = paddingValues.calculateBottomPadding() + 100.dp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                ) {
                    item {
                        val currentHeight = if (searchBarMaxHeightPx > 0f) with(density) { searchBarHeightPx.toDp() } else Dp.Unspecified
                        val pillHeight = if (currentHeight == Dp.Unspecified) Dp.Unspecified else (currentHeight - 8.dp).coerceAtLeast(0.dp)
                        
                        // Fast fade: fully opaque at 1.0, fully transparent already at 0.5 (half height)
                        val progress = if (searchBarMaxHeightPx > 0f) (searchBarHeightPx / searchBarMaxHeightPx).coerceIn(0f, 1f) else 1f
                        val alphaProgress = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(currentHeight)
                                .clipToBounds(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(pillHeight)
                                    .graphicsLayer { alpha = alphaProgress }
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (alphaProgress > 0f) {
                                    val textScale = 0.75f + 0.25f * alphaProgress
                                    Box(
                                        modifier = Modifier
                                            .wrapContentHeight(unbounded = true)
                                            .graphicsLayer {
                                                scaleX = textScale
                                                scaleY = textScale
                                                transformOrigin = TransformOrigin(0f, 0.5f)
                                            }
                                    ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(ImageVector.vectorResource(id = R.drawable.ic_lente), contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                            if (searchQuery.isEmpty()) {
                                                Text(stringResource(R.string.search_folders), color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
                                            }
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = searchQuery,
                                                onValueChange = viewModel::updateSearchQuery,
                                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                                decorationBox = { innerTextField -> 
                                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                                        innerTextField()
                                                    }
                                                }
                                            )
                                        }
                                        if (searchQuery.isNotEmpty()) {
                                            Spacer(Modifier.width(8.dp))
                                            IconButton(onClick = { viewModel.updateSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_x), 
                                                    contentDescription = null, 
                                                    tint = Color.White.copy(alpha = 0.5f), 
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }

                    if (searchQuery.isBlank()) {
                        item {
                            val currentHeight = if (newFolderMaxHeightPx > 0f) with(density) { newFolderHeightPx.toDp() } else Dp.Unspecified
                            val pillHeight = if (currentHeight == Dp.Unspecified) Dp.Unspecified else (currentHeight - 12.dp).coerceAtLeast(0.dp)
                            
                            // Fast fade: fully opaque at 1.0, fully transparent already at 0.5 (half height)
                            val progress = if (newFolderMaxHeightPx > 0f) (newFolderHeightPx / newFolderMaxHeightPx).coerceIn(0f, 1f) else 1f
                            val alphaProgress = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(currentHeight)
                                    .clipToBounds(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(pillHeight),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    NewFolderCard(
                                        onClick = { isCreateDialogOpen = true },
                                        modifier = Modifier.weight(1.05f),
                                        alphaProgress = alphaProgress
                                    )
                                    JoinFolderCard(
                                        onClick = {
                                            if (isGuest) onGuestAuthRequired() else isJoinDialogOpen = true
                                        },
                                        modifier = Modifier.weight(0.95f),
                                        alphaProgress = alphaProgress
                                    )
                                }
                            }
                        }
                    }
                    if (folders.isNullOrEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Nessun risultato per \"$searchQuery\"" else stringResource(R.string.collection_filter_empty),
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
    
                    items(folders.orEmpty(), key = { it.id }, contentType = { "folder" }) { folder ->
                        Box {
                            FolderCard(
                                folder = folder,
                                allMovies = allMovies,
                                onClick = { onFolderClick(folder) },
                                onLongClick = { bounds ->
                                    activeMenuBounds = bounds
                                    activeMenuFolder = folder
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    val modalHost = LocalModalHost.current
    val globalHazeState = com.cinetrack.ui.LocalGlobalHazeState.current
    val dialogHazeState = globalHazeState ?: activeHazeState

    LaunchedEffect(isCreateDialogOpen) {
        if (isCreateDialogOpen) {
            modalHost.show {
                FolderCreateDialog(
                    onDismiss = {
                        isCreateDialogOpen = false
                        modalHost.dismiss()
                    },
                    onCreate = { name, icon, color, description ->
                        viewModel.createFolder(name, icon, color, description ?: "")
                        isCreateDialogOpen = false
                        modalHost.dismiss()
                    },
                    hazeState = dialogHazeState
                )
            }
        }
    }

    LaunchedEffect(isJoinDialogOpen) {
        if (isJoinDialogOpen) {
            modalHost.show {
                FolderJoinDialog(
                    initialCode = joinDialogInitialCode,
                    onDismiss = {
                        isJoinDialogOpen = false
                        joinDialogInitialCode = ""
                        modalHost.dismiss()
                    },
                    onCheckCode = { code, callback ->
                        viewModel.checkInvite(code, callback)
                    },
                    onAcceptInvite = { code, callback ->
                        viewModel.acceptInvite(code, callback)
                    },
                    hazeState = dialogHazeState
                )
            }
        }
    }

    LaunchedEffect(folderToDelete) {
        val folder = folderToDelete
        if (folder != null) {
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val isLeave = folder.isShared && (folder.ownerUid != currentUserId)
            modalHost.show {
                DeleteFolderDialog(
                    onConfirm = {
                        viewModel.deleteFolder(folder.id)
                        folderToDelete = null
                        modalHost.dismiss()
                    },
                    onDismiss = {
                        folderToDelete = null
                        modalHost.dismiss()
                    },
                    folderName = folder.name,
                    isLeave = isLeave,
                    hazeState = dialogHazeState
                )
            }
        }
    }

    LaunchedEffect(folderToEdit) {
        val folder = folderToEdit
        if (folder != null) {
            modalHost.show {
                FolderEditDialog(
                    initialName = folder.name,
                    initialColor = folder.color ?: "#FFFFFF",
                    initialDescription = folder.description,
                    editMode = folderEditMode,
                    onDismiss = {
                        folderToEdit = null
                        modalHost.dismiss()
                    },
                    onSave = { newName, newColor, newDesc ->
                        viewModel.updateFolder(folder.copy(name = newName, color = newColor, description = newDesc))
                        folderToEdit = null
                        modalHost.dismiss()
                    },
                    hazeState = dialogHazeState
                )
            }
        }
    }

    folderToShare?.let { folder ->
        FolderShareModal(
            folder = folder,
            onDismiss = { folderToShare = null },
            onCreateInvite = { durationOption, callback ->
                viewModel.createInviteForFolder(folder, durationOption, callback)
            },
            hazeState = dialogHazeState
        )
    }

    folderForMembers?.let { folder ->
        val currentUserId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
        val isOwner = folder.ownerUid == currentUserId
        var members by remember(folder.sharedFolderId) {
            mutableStateOf<List<com.cinetrack.data.model.SharedFolderMemberDto>>(emptyList())
        }
        var activeInvites by remember(folder.sharedFolderId) {
            mutableStateOf<List<com.cinetrack.data.model.SharedFolderInviteDto>>(emptyList())
        }

        LaunchedEffect(folder.sharedFolderId) {
            folder.sharedFolderId?.let { sharedId ->
                viewModel.getFolderMembers(sharedId) { res ->
                    members = res.getOrDefault(emptyList())
                }
                viewModel.getFolderInvites(sharedId) { res ->
                    activeInvites = res.getOrDefault(emptyList())
                    if (isOwner) {
                        viewModel.checkAutoRevert(folder) { didRevert ->
                            if (didRevert) {
                                folderForMembers = null
                            }
                        }
                    }
                }
            }
        }

        FolderMembersModal(
            folder = folder,
            members = members,
            activeInvites = activeInvites,
            currentUserId = currentUserId,
            isOwner = isOwner,
            onDismiss = { folderForMembers = null },
            onRemoveMember = { member ->
                viewModel.removeFolderMember(folder, member.userId) { res ->
                    if (res.isSuccess) {
                        val didRevert = res.getOrDefault(false)
                        if (didRevert) {
                            folderForMembers = null
                        } else {
                            members = members.filter { it.userId != member.userId }
                        }
                    }
                }
            },
            onLeaveFolder = {
                viewModel.deleteFolder(folder.id)
                folderForMembers = null
            },
            onRevokeInvite = { invite ->
                viewModel.revokeFolderInvite(folder, invite.inviteCode) { res ->
                    if (res.isSuccess) {
                        val didRevert = res.getOrDefault(false)
                        if (didRevert) {
                            folderForMembers = null
                        } else {
                            activeInvites = activeInvites.filter { it.inviteCode != invite.inviteCode }
                        }
                    }
                }
            },
            onOpenShareModal = {
                val target = folder
                folderForMembers = null
                folderToShare = target
            },
            hazeState = dialogHazeState
        )
    }

    var rememberedMenuFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var isMenuVisible by remember { mutableStateOf(false) }

    LaunchedEffect(activeMenuFolder) {
        if (activeMenuFolder != null) {
            rememberedMenuFolder = activeMenuFolder
            isMenuVisible = true
        } else if (isMenuVisible) {
            isMenuVisible = false
            kotlinx.coroutines.delay(200)
            rememberedMenuFolder = null
        }
    }

    rememberedMenuFolder?.let { folder ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2000f)
                .pointerInput(Unit) { detectTapGestures { activeMenuFolder = null } }
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val offsetX = with(density) { activeMenuBounds.left.toDp() + 32.dp }
            val offsetY = with(density) { activeMenuBounds.top.toDp() + 48.dp }
            
            val menuAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isMenuVisible) 1f else 0f,
                animationSpec = tween(200),
                label = "menuAlpha"
            )
            AnimatedVisibility(
                visible = isMenuVisible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { -it / 4 },
                    animationSpec = tween(250, easing = EaseOutCirc)
                ),
                exit = fadeOut() + slideOutVertically(
                    targetOffsetY = { -it / 4 },
                    animationSpec = tween(200, easing = EaseInCirc)
                ),
                modifier = Modifier.absoluteOffset(x = offsetX, y = offsetY)
            ) {
                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .then(Modifier.hazeGlass(state = activeHazeState, shape = RoundedCornerShape(24.dp), alpha = menuAlpha))
                ) {
                    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    val isOwner = !folder.isShared || (folder.ownerUid == currentUserId)

                    if (folder.isShared || isOwner) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick { 
                                    val target = folder
                                    activeMenuFolder = null
                                    if (isGuest) {
                                        onGuestAuthRequired()
                                    } else if (target.isShared) {
                                        folderForMembers = target
                                    } else {
                                        folderToShare = target
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                ImageVector.vectorResource(id = R.drawable.ic_people), 
                                contentDescription = null, 
                                tint = Color.White, 
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (folder.isShared) {
                                    if (isOwner) stringResource(R.string.folder_members_title) else stringResource(R.string.folder_members_only_title)
                                } else {
                                    stringResource(R.string.folder_share_action)
                                }, 
                                color = Color.White, 
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    if (isOwner) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick { 
                                    activeMenuFolder = null
                                    folderEditMode = FolderEditMode.ALL
                                    folderToEdit = folder
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(ImageVector.vectorResource(id = R.drawable.ic_pencil), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.folder_edit_info), color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick { 
                                activeMenuFolder = null
                                folderToDelete = folder 
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_trash), contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (folder.isShared && !isOwner) {
                                stringResource(R.string.folder_leave)
                            } else {
                                stringResource(R.string.folders_delete)
                            },
                            color = Color(0xFFFF3B30),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

