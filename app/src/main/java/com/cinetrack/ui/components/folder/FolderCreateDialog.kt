package com.cinetrack.ui.components.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.components.shared.MorphGlassModal
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.verticalFadingEdges
import dev.chrisbanes.haze.HazeState

@Composable
fun FolderCreateDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String?) -> Unit,
    hazeState: HazeState? = null,
    triggerBounds: Rect? = null
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#6366F1") }
    val defaultPresets = remember { listOf("#6366F1", "#EC4899", "#F59E0B", "#10B981", "#8B5CF6", "#EF4444") }
    var isCustomColorMode by remember { mutableStateOf(!defaultPresets.contains(selectedColor.uppercase())) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isVisible by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun requestDismiss(action: (() -> Unit)? = null) {
        focusManager.clearFocus()
        keyboardController?.hide()
        pendingAction = action
        isVisible = false
    }

    MorphGlassModal(
        isVisible = isVisible,
        onDismissRequest = { requestDismiss() },
        onDismissFinished = {
            pendingAction?.invoke()
            onDismiss()
        },
        triggerBounds = triggerBounds,
        hazeState = hazeState,
        targetMaxWidth = 440.dp,
        targetWidthFraction = 0.92f,
        maxModalHeightFraction = 0.85f,
        targetCornerRadius = 32.dp,
        zIndex = 100f
    ) {
        val scrollState = rememberScrollState()
        val maxScrollHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.65f).coerceAtLeast(260.dp)

        LaunchedEffect(isCustomColorMode) {
            if (isCustomColorMode) {
                // Ensure color wheel smoothly scrolls into view when opened
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Fixed Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 16.dp, top = 22.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.folders_new_folder),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                ModalCloseButton(
                    onClose = { requestDismiss() }
                )
            }

            // Scrollable Body with Fading Edges
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxScrollHeight)
                    .verticalFadingEdges(scrollState, topEdgeHeight = 16.dp, bottomEdgeHeight = 16.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 22.dp)
            ) {
                Spacer(Modifier.height(8.dp))

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
                    visualTransformation = remember { com.cinetrack.ui.utils.MarkdownVisualTransformation() },
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
                    onColorSelected = { selectedColor = it },
                    isCustomMode = isCustomColorMode,
                    onCustomModeChange = { isCustomColorMode = it }
                )

                Spacer(Modifier.height(12.dp))
            }

            // Fixed Footer Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
                    .height(56.dp)
                    .bounceClick(enabled = name.isNotBlank()) {
                        val finalName = name
                        val finalDesc = description.trim().ifEmpty { null }
                        val finalColor = selectedColor
                        requestDismiss {
                            onCreate(finalName, "folder", finalColor, finalDesc)
                        }
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
