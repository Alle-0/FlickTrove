package com.cinetrack.ui.components.shared

import androidx.compose.foundation.background

import androidx.compose.ui.res.stringResource
import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.premiumScrollbar
import dev.chrisbanes.haze.HazeState
import androidx.compose.animation.*
import com.cinetrack.ui.utils.bounceClick

enum class FolderEditMode { NAME, COLOR }

@Composable
fun FolderEditDialog(
    initialName: String,
    initialColor: String,
    editMode: FolderEditMode = FolderEditMode.NAME,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    hazeState: HazeState? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    val focusManager = LocalFocusManager.current
    
    var isVisible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { isVisible = true }
    
    LaunchedEffect(isDismissing) {
        if (isDismissing) {
            isVisible = false
            kotlinx.coroutines.delay(250)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { isDismissing = true },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .pointerInput(Unit) {
                    detectTapGestures { 
                        focusManager.clearFocus()
                        isDismissing = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f)
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
                            text = if (editMode == FolderEditMode.NAME) stringResource(R.string.folder_edit_rename) else stringResource(R.string.folder_edit_color),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .bounceClick { isDismissing = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(ImageVector.vectorResource(id = R.drawable.ic_x), null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    
                    if (editMode == FolderEditMode.NAME) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 40) name = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.folder_edit_name_hint), color = Color.White.copy(alpha = 0.3f)) },
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
                    }
                    
                    if (editMode == FolderEditMode.COLOR) {
                        FolderColorPicker(
                            selectedColor = selectedColor,
                            onColorSelected = { selectedColor = it }
                        )
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Button(
                        onClick = { onSave(name, selectedColor) },
                        enabled = if (editMode == FolderEditMode.NAME) name.isNotBlank() && name != initialName else selectedColor != initialColor,
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
                        Text(stringResource(R.string.action_save).uppercase(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
