package com.cinetrack.ui.components.shared

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import dev.chrisbanes.haze.HazeState

enum class FolderEditMode { ALL, NAME, COLOR }

@Composable
fun FolderEditDialog(
    initialName: String,
    initialColor: String,
    initialDescription: String? = null,
    editMode: FolderEditMode = FolderEditMode.ALL,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
    hazeState: HazeState? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription ?: "") }
    var selectedColor by remember { mutableStateOf(initialColor) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    FlickTroveModal(
        isVisible = true,
        onDismissRequest = onDismiss,
        hazeState = hazeState,
        maxWidth = 360.dp,
        maxWidthFraction = 0.90f,
        contentPadding = PaddingValues(24.dp)
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .premiumScrollbar(scrollState)
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editMode == FolderEditMode.COLOR) stringResource(R.string.folder_edit_color) else stringResource(R.string.folder_edit_info),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                ModalCloseButton(
                    onClose = onDismiss
                )
            }
            Spacer(Modifier.height(24.dp))

            if (editMode == FolderEditMode.ALL || editMode == FolderEditMode.NAME) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.folder_edit_name_hint), color = Color.White.copy(alpha = 0.5f)) },
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
                    label = { Text(stringResource(R.string.folder_edit_description_hint), color = Color.White.copy(alpha = 0.5f)) },
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
            }

            if (editMode == FolderEditMode.ALL || editMode == FolderEditMode.COLOR) {
                if (editMode == FolderEditMode.ALL) {
                    Spacer(Modifier.height(20.dp))
                }
                FolderColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }

            Spacer(Modifier.height(32.dp))

            val isSaveEnabled = when (editMode) {
                FolderEditMode.ALL -> name.isNotBlank() && (name.trim() != initialName.trim() || description.trim() != (initialDescription ?: "").trim() || selectedColor != initialColor)
                FolderEditMode.NAME -> name.isNotBlank() && (name.trim() != initialName.trim() || description.trim() != (initialDescription ?: "").trim())
                FolderEditMode.COLOR -> selectedColor != initialColor
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .bounceClick(enabled = isSaveEnabled) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onSave(name, selectedColor, description.trim().ifEmpty { null })
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSaveEnabled) Color.White else Color.White.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.action_save).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isSaveEnabled) Color.Black else Color.Black.copy(alpha = 0.5f)
                )
            }
        }
    }
}
