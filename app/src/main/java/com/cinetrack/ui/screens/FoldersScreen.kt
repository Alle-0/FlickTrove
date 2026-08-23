package com.cinetrack.ui.screens

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.ui.viewmodel.FoldersViewModel
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.components.shared.FolderColorPicker
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.HazeStyle
import com.cinetrack.ui.components.shared.DeleteFolderDialog
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.shared.FolderEditDialog
import com.cinetrack.ui.components.shared.FolderEditMode
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

object FoldersTab : Tab {
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
        val paddingValues = LocalAppPadding.current
        val hazeState = LocalHazeState.current
        val tabNavigator = LocalTabNavigator.current

        FoldersScreenContent(
            viewModel = viewModel,
            paddingValues = paddingValues,
            hazeState = hazeState,
            onFolderClick = { folder ->
                tabNavigator.current = FolderDetailTab(folder.id, folder.name, folder.color)
            }
        )
    }
}

@Composable
fun FoldersScreenContent(
    viewModel: FoldersViewModel,
    paddingValues: PaddingValues,
    hazeState: HazeState? = null,
    onFolderClick: (FolderEntity) -> Unit = {}
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val allMovies by viewModel.allMovies.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var isCreateDialogOpen by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToEdit by remember { mutableStateOf<FolderEntity?>(null) }
    var folderEditMode by remember { mutableStateOf(FolderEditMode.NAME) }
    
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
            if (folders.isNullOrEmpty() && searchQuery.isBlank()) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                                .bounceClick { isCreateDialogOpen = true }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(ImageVector.vectorResource(id = R.drawable.ic_plus), null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.folders_create_first), 
                                color = Color.White, 
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            )
                        }
                    }
                }
            } else {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                
                val density = androidx.compose.ui.platform.LocalDensity.current
                
                // Use fixed standard heights to prevent first-frame measurement clipping
                val searchBarMaxHeightDp = 64.dp // 56dp for the bar + 8dp bottom padding
                val newFolderMaxHeightDp = 56.dp // 44dp for the card + 12dp spacing approximately
                
                val searchBarMaxHeightPxDefault = with(density) { searchBarMaxHeightDp.toPx() }
                var searchBarMaxHeightPx by remember { mutableFloatStateOf(searchBarMaxHeightPxDefault) }
                var searchBarHeightPx by remember { mutableFloatStateOf(searchBarMaxHeightPxDefault) }
                
                val newFolderMaxHeightPxDefault = with(density) { newFolderMaxHeightDp.toPx() }
                var newFolderMaxHeightPx by remember { mutableFloatStateOf(newFolderMaxHeightPxDefault) }
                var newFolderHeightPx by remember { mutableFloatStateOf(newFolderMaxHeightPxDefault) }

                val nestedScrollConnection = remember {
                    object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                        override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
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
                    }
                }
                
                LaunchedEffect(listState.isScrollInProgress) {
                    if (listState.isScrollInProgress) {
                        focusManager.clearFocus()
                    }
                }
                
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = paddingValues.calculateTopPadding() + androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 120.dp,
                        bottom = paddingValues.calculateBottomPadding() + 32.dp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                ) {
                    item {
                        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                        val currentHeight = if (searchBarMaxHeightPx > 0f) with(density) { searchBarHeightPx.toDp() } else Dp.Unspecified
                        val pillHeight = if (currentHeight == Dp.Unspecified) Dp.Unspecified else (currentHeight - 8.dp).coerceAtLeast(0.dp)
                        
                        // Fast fade: fully opaque at 1.0, fully transparent at 0.5
                        val progress = if (searchBarMaxHeightPx > 0f) (searchBarHeightPx / searchBarMaxHeightPx).coerceIn(0f, 1f) else 1f
                        val alphaProgress = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
                        
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
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (alphaProgress > 0f) {
                                    Box(modifier = Modifier
                                        .wrapContentHeight(unbounded = true)
                                        .graphicsLayer {
                                            alpha = alphaProgress
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
                            
                            // Fast fade: fully opaque at 1.0, fully transparent at 0.6 to disappear earlier
                            val progress = if (newFolderMaxHeightPx > 0f) (newFolderHeightPx / newFolderMaxHeightPx).coerceIn(0f, 1f) else 1f
                            val alphaProgress = ((progress - 0.6f) * 2.5f).coerceIn(0f, 1f)
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(currentHeight)
                                    .clipToBounds(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                NewFolderCard(
                                    onClick = { isCreateDialogOpen = true },
                                    modifier = Modifier.height(pillHeight),
                                    alphaProgress = alphaProgress
                                )
                            }
                        }
                    }
                    if (folders.isNullOrEmpty() && searchQuery.isNotBlank()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nessun risultato per \"$searchQuery\"",
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

    if (isCreateDialogOpen) {
        FolderCreateDialog(
            onDismiss = { isCreateDialogOpen = false },
            onCreate = { name, icon, color ->
                viewModel.createFolder(name, icon, color)
                isCreateDialogOpen = false
            },
            hazeState = hazeState
        )
    }

    folderToDelete?.let { folder ->
        DeleteFolderDialog(
            onConfirm = {
                viewModel.deleteFolder(folder.id)
                folderToDelete = null
            },
            onDismiss = { folderToDelete = null },
            folderName = folder.name,
            hazeState = hazeState
        )
    }

    folderToEdit?.let { folder ->
        FolderEditDialog(
            initialName = folder.name,
            initialColor = folder.color ?: "#FFFFFF",
            editMode = folderEditMode,
            onDismiss = { folderToEdit = null },
            onSave = { newName, newColor ->
                viewModel.updateFolder(folder.copy(name = newName, color = newColor))
                folderToEdit = null
            },
            hazeState = hazeState
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick { 
                                activeMenuFolder = null
                                folderEditMode = FolderEditMode.NAME
                                folderToEdit = folder
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_pencil), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.folders_rename), color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick { 
                                activeMenuFolder = null
                                folderEditMode = FolderEditMode.COLOR
                                folderToEdit = folder
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_palette), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.folders_change_color), color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
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
                        Text(stringResource(R.string.folders_delete), color = Color(0xFFFF3B30), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
}

@Composable
fun FolderCard(
    folder: FolderEntity,
    allMovies: List<com.cinetrack.data.model.Movie>,
    onClick: () -> Unit,
    onLongClick: (Rect) -> Unit
) {
    val folderColor = folder.color.toComposeColor()
    val bounds = remember { arrayOf(Rect.Zero) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds[0] = it.boundsInWindow() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .bounceClick(
                    onLongClick = { onLongClick(bounds[0]) },
                    onClick = onClick
                )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
        // Colored dot (circle) with icon
        val topItems = remember(folder.itemIds, allMovies) {
            folder.itemIds.take(3).mapNotNull { id ->
                allMovies.find { "${it.mediaType}_${it.id}" == id }
            }
        }

        if (topItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(folderColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                    contentDescription = null,
                    tint = folderColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(40.dp + ((topItems.size - 1) * 22).dp)
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                topItems.forEachIndexed { index, movie ->
                    val centerIndex = (topItems.size - 1) / 2f
                    val currentRotation = if (topItems.size <= 1) 0f else {
                        val maxRotation = 15f
                        val rotationStep = (maxRotation * 2) / (topItems.size - 1)
                        -maxRotation + (index * rotationStep)
                    }
                    val xOffset = (index - centerIndex) * 14f
                    
                    Box(
                        modifier = Modifier
                            .offset(x = xOffset.dp)
                            .size(width = 40.dp, height = 60.dp)
                            .graphicsLayer {
                                rotationZ = currentRotation
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.1f)
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0xFF13151A), RoundedCornerShape(10.dp))
                            .zIndex((topItems.size - index).toFloat())
                    ) {
                        coil.compose.AsyncImage(
                            model = "https://image.tmdb.org/t/p/w200${movie.posterPath}",
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        if (topItems.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(folderColor)
            )
            Spacer(Modifier.width(8.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp
                ),
                color = Color.White
            )
            
            val formatter = remember { java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.getDefault()) }
            val formattedDate = remember(folder.createdAt) {
                try {
                    val instant = java.time.Instant.parse(folder.createdAt)
                    java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()).format(formatter)
                } catch (e: Exception) {
                    ""
                }
            }
            if (formattedDate.isNotEmpty()) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
        
        Text(
            text = folder.itemIds.size.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.White.copy(alpha = 0.4f)
        )
        
        Spacer(Modifier.width(8.dp))
        
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(20.dp)
        )
        }
    }
}

@Composable
fun NewFolderCard(onClick: () -> Unit, modifier: Modifier = Modifier, alphaProgress: Float = 1f) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (alphaProgress > 0f) {
            Box(modifier = Modifier
                .wrapContentHeight(unbounded = true)
                .graphicsLayer { alpha = alphaProgress }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_plus),
                    null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(Modifier.width(12.dp))
                
                Text(
                    text = stringResource(R.string.folders_new_folder),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    }
}


@Composable
fun FolderCreateDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit,
    hazeState: HazeState? = null
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#6366F1") }
    val focusManager = LocalFocusManager.current
    var isDismissing by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var pendingCreate by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    
    LaunchedEffect(Unit) { isVisible = true }
    LaunchedEffect(isDismissing) {
        if (isDismissing) {
            isVisible = false
            kotlinx.coroutines.delay(250)
            if (pendingCreate != null) {
                onCreate(pendingCreate!!.first, pendingCreate!!.second, pendingCreate!!.third)
            } else {
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = { isDismissing = true },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val scrimAlpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isVisible) HazeStyles.ModalScrimAlpha else 0f,
            animationSpec = androidx.compose.animation.core.tween(250),
            label = "scrimAlpha"
        )
        val blurAlpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = androidx.compose.animation.core.tween(250),
            label = "blurAlpha"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .pointerInput(Unit) {
                    detectTapGestures { 
                        focusManager.clearFocus()
                        isDismissing = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isVisible,
                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(250)),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(250))
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .hazeGlass(state = hazeState, shape = RoundedCornerShape(32.dp), alpha = blurAlpha)
                        .pointerInput(Unit) {
                            detectTapGestures { focusManager.clearFocus() }
                        }
                        .premiumScrollbar(scrollState)
                        .verticalScroll(scrollState)
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.folders_new_folder),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.1f))
                                .bounceClick { isDismissing = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(ImageVector.vectorResource(id = R.drawable.ic_x), null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.folders_name_placeholder), color = Color.White.copy(alpha = 0.3f)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        cursorColor = Color.White
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${name.length}/40",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (name.length >= 35) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                
                Spacer(Modifier.height(24.dp))
                
                FolderColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
                
                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = { 
                        pendingCreate = Triple(name, "folder", selectedColor)
                        isDismissing = true
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White.copy(alpha = 0.2f),
                        disabledContentColor = Color.Black.copy(alpha = 0.5f)
                    )
                ) {
                    Text(stringResource(R.string.folders_create_button), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
                }
            }
        }
    }
}
