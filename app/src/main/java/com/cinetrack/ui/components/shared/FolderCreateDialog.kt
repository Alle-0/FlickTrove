package com.cinetrack.ui.components.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.util.toComposeColor
import dev.chrisbanes.haze.HazeState

@Composable
fun FolderCreateDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String?) -> Unit,
    hazeState: HazeState? = null
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#6366F1") }
    val focusManager = LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var isDismissing by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var pendingCreate by remember { mutableStateOf<Triple<String, String, Pair<String, String?>>?>(null) }
    
    LaunchedEffect(Unit) { isVisible = true }
    LaunchedEffect(isDismissing) {
        if (isDismissing) {
            isVisible = false
            kotlinx.coroutines.delay(250)
            if (pendingCreate != null) {
                onCreate(pendingCreate!!.first, pendingCreate!!.second, pendingCreate!!.third.first, pendingCreate!!.third.second)
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
                    detectTapGestures(onTap = { 
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        isDismissing = true
                    })
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
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
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
                        com.cinetrack.ui.components.shared.ModalCloseButton(
                            onClose = { isDismissing = true }
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.folders_name_placeholder), color = Color.White.copy(alpha = 0.5f)) },
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

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 250) description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.folders_description_placeholder), color = Color.White.copy(alpha = 0.5f)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        cursorColor = Color.White
                    ),
                    minLines = 2,
                    maxLines = 4
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${description.length}/250",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (description.length >= 230) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                
                Spacer(Modifier.height(24.dp))
                
                FolderColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
                
                Spacer(Modifier.height(32.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .bounceClick(enabled = name.isNotBlank()) {
                            pendingCreate = Triple(name, "folder", Pair(selectedColor, description.trim().ifEmpty { null }))
                            isDismissing = true
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (name.isNotBlank()) Color.White else Color.White.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.folders_create_button),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (name.isNotBlank()) Color.Black else Color.Black.copy(alpha = 0.5f)
                    )
                }
                }
            }
        }
    }
}
