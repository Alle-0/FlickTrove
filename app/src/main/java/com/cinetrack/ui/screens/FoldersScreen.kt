package com.cinetrack.ui.screens

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
import dev.chrisbanes.haze.HazeStyle

@Composable
fun FoldersScreen(
    viewModel: FoldersViewModel,
    paddingValues: PaddingValues,
    hazeState: HazeState? = null,
    onFolderClick: (FolderEntity) -> Unit = {}
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var isCreateDialogOpen by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // --- FOREGROUND LAYER (SHARP CONTENT) ---
        if (folders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
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
                    Button(
                        onClick = { isCreateDialogOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("CREA LA PRIMA")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 32.dp
                ),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    NewFolderCard(onClick = { isCreateDialogOpen = true })
                }

                val sortedFolders = folders.sortedBy { it.name.lowercase() }
                items(sortedFolders, key = { it.id }) { folder ->
                    Box(modifier = Modifier.animateItem()) {
                        FolderCard(
                            folder = folder,
                            onClick = { onFolderClick(folder) }
                        )
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
}

@Composable
fun FolderCard(
    folder: FolderEntity,
    onClick: () -> Unit
) {
    val folderColor = folder.color.toComposeColor()
    
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
                imageVector = Icons.Rounded.Folder,
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
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(20.dp)
        )
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
                Icons.Rounded.Add,
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
                Text(
                    text = "Nuova Cartella",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
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
