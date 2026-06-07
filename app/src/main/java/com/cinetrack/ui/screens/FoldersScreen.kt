package com.cinetrack.ui.screens

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.HazeStyle
import com.cinetrack.ui.components.shared.DeleteFolderDialog
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

object FoldersTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            return remember {
                TabOptions(
                    index = 3u,
                    title = "Folders",
                    icon = null
                )
            }
        }

    @Composable
    override fun Content() {
        val viewModel = getViewModel<FoldersViewModel>()
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
    var isCreateDialogOpen by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToEdit by remember { mutableStateOf<FolderEntity?>(null) }
    var folderEditMode by remember { mutableStateOf(FolderEditMode.NAME) }
    
    var activeMenuFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var activeMenuBounds by remember { mutableStateOf(Rect.Zero) }
    
    val activeHazeState = hazeState ?: remember { HazeState() }
    
    androidx.activity.compose.BackHandler(enabled = activeMenuFolder != null) {
        activeMenuFolder = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = activeHazeState)
        ) {
            // --- FOREGROUND LAYER (SHARP CONTENT) ---
            if (folders.isEmpty()) {
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
                            text = "Non hai ancora creato cartelle",
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
                                "CREA LA PRIMA", 
                                color = Color.White, 
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = paddingValues.calculateTopPadding() + androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 46.dp + 60.dp + 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 32.dp
                    ),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        NewFolderCard(onClick = { isCreateDialogOpen = true })
                    }
    
                    val sortedFolders = folders.sortedBy { it.name.lowercase() }
                    items(sortedFolders, key = { it.id }, contentType = { "folder" }) { folder ->
                        Box(modifier = Modifier.animateItem()) {
                            FolderCard(
                                folder = folder,
                                onClick = { onFolderClick(folder) },
                                onLongClick = { bounds ->
                                    activeMenuBounds = bounds
                                    activeMenuFolder = folder
                                }
                            )
                        }
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

    activeMenuFolder?.let { folder ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2000f)
                .pointerInput(Unit) { detectTapGestures { activeMenuFolder = null } }
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val offsetX = with(density) { activeMenuBounds.left.toDp() + 32.dp }
            val offsetY = with(density) { activeMenuBounds.top.toDp() + 48.dp }
            
            var isMenuVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { isMenuVisible = true }
            
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
                        .then(Modifier.hazeGlass(state = activeHazeState, shape = RoundedCornerShape(24.dp)))
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
                        Text("Rinomina Cartella", color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
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
                        Text("Cambia Colore", color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
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
                        Text("Elimina Cartella", color = Color(0xFFFF3B30), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
}

@Composable
fun FolderCard(
    folder: FolderEntity,
    onClick: () -> Unit,
    onLongClick: (Rect) -> Unit
) {
    val folderColor = folder.color.toComposeColor()
    var bounds by remember { mutableStateOf(Rect.Zero) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds = it.boundsInWindow() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .bounceClick(
                    onLongClick = { onLongClick(bounds) },
                    onClick = onClick
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Colored dot (circle) with icon
        // Icon container with subtle background
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .background(folderColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                contentDescription = null,
                tint = folderColor,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Text(
            text = folder.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            ),
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        
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
fun NewFolderCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .bounceClick { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_plus),
                null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Text(
            "Nuova Cartella",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            ),
            color = Color.White.copy(alpha = 0.6f)
        )
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .pointerInput(Unit) {
                    detectTapGestures { 
                        focusManager.clearFocus()
                        onDismiss()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { isVisible = true }
            
            androidx.compose.animation.AnimatedVisibility(
                visible = isVisible,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.9f),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.9f)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .hazeGlass(state = hazeState, shape = RoundedCornerShape(32.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { focusManager.clearFocus() }
                        }
                        .clickable(enabled = false) { }
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
                            text = "Nuova Cartella",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.1f))
                                .bounceClick { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(ImageVector.vectorResource(id = R.drawable.ic_x), null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 25) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nome cartella", color = Color.White.copy(alpha = 0.3f)) },
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
                
                Spacer(Modifier.height(24.dp))
                
                FolderColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
                
                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = { onCreate(name, "folder", selectedColor) },
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
                    Text("CREA", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
                }
            }
        }
    }
}
