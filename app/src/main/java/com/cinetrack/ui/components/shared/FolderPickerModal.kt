package com.cinetrack.ui.components.shared

import androidx.compose.ui.res.stringResource
import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.util.toComposeColor

@Composable
fun FolderPickerModal(
    folders: List<FolderEntity>,
    isItemInFolder: (String) -> Boolean,
    onToggleItem: (FolderEntity) -> Unit,
    onCreateFolder: (String, String) -> Unit,
    onClose: () -> Unit,
    hazeState: HazeState
) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeGlass(state = hazeState, shape = RoundedCornerShape(0.dp))
            .pointerInput(Unit) {
                detectTapGestures { 
                    focusManager.clearFocus()
                    onClose() 
                }
            }
    ) {
        FolderPickerModalContent(
            folders = folders,
            isItemInFolder = isItemInFolder,
            onToggleItem = onToggleItem,
            onCreateFolder = onCreateFolder,
            onClose = onClose,
            modifier = Modifier
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = 16.dp, end = 16.dp, start = 16.dp)
                .width(320.dp)
                .shadow(24.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .hazeGlass(state = hazeState, shape = RoundedCornerShape(28.dp))
                .align(Alignment.TopEnd)
        )
    }
}

@Composable
fun FolderPickerModalContent(
    folders: List<FolderEntity>,
    isItemInFolder: (String) -> Boolean,
    onToggleItem: (FolderEntity) -> Unit,
    onCreateFolder: (String, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCreating by remember { mutableStateOf(false) }

    val sortedFolders = remember(folders, isItemInFolder) {
        folders.sortedWith(
            compareByDescending<FolderEntity> { isItemInFolder(it.id) }
                .thenBy { it.name.lowercase() }
        )
    }

    val focusManager = LocalFocusManager.current
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() }
            }
    ) {
        AnimatedContent(
            targetState = isCreating,
            transitionSpec = {
                val springSpec = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                fadeIn(animationSpec = springSpec) togetherWith fadeOut(animationSpec = springSpec)
            },
            modifier = Modifier.fillMaxSize(),
            label = "FolderPickerTransition"
        ) { creating ->
            if (creating) {
                FolderCreateForm(
                    onCancel = { isCreating = false },
                    onCreate = { name, color ->
                        onCreateFolder(name, color)
                        isCreating = false
                    }
                )
            } else {
                FolderListContent(
                    folders = sortedFolders,
                    isItemInFolder = isItemInFolder,
                    onToggleItem = onToggleItem,
                    onNewFolder = { isCreating = true },
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
private fun FolderListContent(
    folders: List<FolderEntity>,
    isItemInFolder: (String) -> Boolean,
    onToggleItem: (FolderEntity) -> Unit,
    onNewFolder: () -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.folder_picker_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.folder_picker_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .bounceClick { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                    contentDescription = "Chiudi",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Spacer(Modifier.height(20.dp))
        
        if (folders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.folder_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                        .padding(end = 12.dp) // Space for scrollbar
                        .verticalFadingEdges(listState, 16.dp, 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(folders, key = { it.id }, contentType = { "folder_picker_item" }) { folder ->
                        Box(modifier = Modifier.animateItem()) {
                            FolderItem(
                                folder = folder,
                                isSelected = isItemInFolder(folder.id),
                                onClick = { onToggleItem(folder) }
                            )
                        }
                    }
                }
                
                // Premium Scrollbar
                val totalItems = folders.size
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo.size
                
                // Show scrollbar only if items exceed visible capacity
                val isScrollable = remember(totalItems, visibleItems) {
                    totalItems > visibleItems && visibleItems > 0
                }
                
                if (isScrollable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(vertical = 16.dp)
                            .padding(end = 6.dp)
                            .width(5.dp)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        val thumbHeightPercent = remember(totalItems, visibleItems) {
                            (visibleItems.toFloat() / totalItems).coerceIn(0.1f, 0.9f)
                        }
                        
                        val thumbOffsetPercent by remember {
                            derivedStateOf {
                                val firstVisible = listState.firstVisibleItemIndex
                                val firstVisibleOffset = listState.firstVisibleItemScrollOffset.toFloat()
                                val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
                                
                                val currentScroll = firstVisible + (firstVisibleOffset / itemSize)
                                val maxScroll = (totalItems - visibleItems).coerceAtLeast(1)
                                (currentScroll / maxScroll).coerceIn(0f, 1f)
                            }
                        }
                        
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val trackHeight = maxHeight
                            val thumbHeight = trackHeight * thumbHeightPercent
                            val thumbOffset = (trackHeight - thumbHeight) * thumbOffsetPercent
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(thumbHeight)
                                    .offset(y = thumbOffset)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.7f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(20.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .bounceClick { onNewFolder() }
                .background(Color.White, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(ImageVector.vectorResource(id = R.drawable.ic_plus), null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.folder_picker_new), color = Color.Black, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun FolderItem(
    folder: FolderEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)
    val borderColor = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .bounceClick { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(folder.color.toComposeColor().copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                contentDescription = null,
                tint = folder.color.toComposeColor(),
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1
                )
            Text(
                text = stringResource(R.string.folder_item_count, folder.itemIds.size),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
        
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick),
                    contentDescription = "Selected",
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FolderCreateForm(
    onCancel: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#6366F1") }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalFadingEdges(scrollState, 24.dp, 24.dp)
                .premiumScrollbar(scrollState)
                .verticalScroll(scrollState)
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                }
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.folder_new_name),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .bounceClick { onCancel() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                        contentDescription = "Chiudi",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 40) name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.folder_edit_name_hint), color = Color.White.copy(alpha = 0.4f)) },
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
            
            Spacer(Modifier.height(20.dp))
            
            FolderColorPicker(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )
            
            Spacer(Modifier.height(24.dp))
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .height(52.dp)
                    .bounceClick { onCancel() }
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.action_cancel).uppercase(), color = Color.White.copy(alpha = 0.5f), maxLines = 1)
            }
            
            val isEnabled = name.isNotBlank()
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(52.dp)
                    .then(
                        if (isEnabled) Modifier.bounceClick { onCreate(name, selectedColor) }
                        else Modifier
                    )
                    .background(
                        if (isEnabled) Color.White else Color.White.copy(alpha = 0.3f), 
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.folder_picker_new).uppercase(),
                    color = if (isEnabled) Color.Black else Color.Black.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
