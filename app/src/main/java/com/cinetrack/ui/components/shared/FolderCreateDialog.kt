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
                    text = stringResource(R.string.folders_new_folder),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                ModalCloseButton(
                    onClose = onDismiss
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
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onCreate(name, "folder", selectedColor, description.trim().ifEmpty { null })
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
