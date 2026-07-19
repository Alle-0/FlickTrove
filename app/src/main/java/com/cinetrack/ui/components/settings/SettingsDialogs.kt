package com.cinetrack.ui.components.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.ColorWheel
import com.cinetrack.ui.theme.*
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.util.toComposeColor
import dev.chrisbanes.haze.HazeState

@Composable
fun BackupDialog(
    hazeState: HazeState,
    isBackupLoading: Boolean,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    alpha: Float = 1f
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(0.85f)
                .hazeGlass(
                    state = hazeState, alpha = alpha,
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable(enabled = false) {}
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_cloud),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.settings_backup_restore_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.settings_backup_restore_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                val isExportEnabled = !isBackupLoading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isExportEnabled) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        .bounceClick(enabled = isExportEnabled) {
                            onExport()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_caricare), null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_export_backup), fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .bounceClick {
                            onImport()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_scaricare), null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_restore_backup), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_close), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun ExternalMigrationDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    alpha: Float = 1f
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(0.85f)
                .hazeGlass(
                    state = hazeState, alpha = alpha,
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable(enabled = false) {}
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_ricarica_cloud),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.settings_external_migration_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.settings_external_migration_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Universal import (primary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .bounceClick { onImport() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_scaricare), null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_select_file), fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))


                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_close), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun RewatchMigrationDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onKeepLatest: () -> Unit,
    onKeepFirst: () -> Unit,
    alpha: Float = 1f
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(0.85f)
                .hazeGlass(
                    state = hazeState, alpha = alpha,
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable(enabled = false) {}
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_ricarica_cloud),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.import_rewatch_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.import_rewatch_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Keep Latest (primary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .bounceClick { onKeepLatest() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.import_rewatch_keep_latest),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Keep First (secondary outlined/surface)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .bounceClick { onKeepFirst() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.import_rewatch_keep_first),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_close), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun YamtrackDialog(
    hazeState: HazeState,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    alpha: Float = 1f
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(0.85f)
                .hazeGlass(
                    state = hazeState, alpha = alpha,
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable(enabled = false) {}
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_ricarica_cloud),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Yamtrack",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Import your Yamtrack library into FlickTrove.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Import button (primary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (!isLoading) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        .bounceClick(enabled = !isLoading) { onImport() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_scaricare), null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import from Yamtrack", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_close), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    initialEmail: String = "",
    isLoading: Boolean = false,
    onSubmit: (String, String, Int, String) -> Unit,
    alpha: Float = 1f
) {
    var title by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(initialEmail) }
    var description by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(3) }
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.9f)
                .hazeGlass(
                    state = hazeState, alpha = alpha,
                    shape = RoundedCornerShape(32.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                }
        ) {
            val feedbackScrollState = rememberScrollState()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
                    .heightIn(max = 640.dp)
            ) {
                // Header (Fixed)
                Text(
                    stringResource(R.string.settings_feedback_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.settings_feedback_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Scrollable Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .premiumScrollbar(feedbackScrollState)
                        .padding(end = 12.dp)
                        .verticalFadingEdges(feedbackScrollState, 16.dp, 16.dp)
                        .verticalScroll(feedbackScrollState)
                ) {
                    // Rating Section
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { index ->
                            val isSelected = index <= rating
                            val starColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
                            
                            IconButton(
                                onClick = { 
                                    rating = index 
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = if (isSelected) R.drawable.ic_star_piena else R.drawable.ic_star),
                                    contentDescription = null,
                                    tint = starColor,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .animateContentSize()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        GlassyTextField(
                            value = title,
                            onValueChange = { if (it.length <= 50) title = it },
                            label = stringResource(R.string.settings_feedback_subject_label),
                            placeholder = stringResource(R.string.settings_feedback_subject_placeholder),
                            singleLine = true
                        )

                        GlassyTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = stringResource(R.string.settings_feedback_email_label),
                            placeholder = stringResource(R.string.settings_feedback_email_placeholder),
                            singleLine = true
                        )

                        Column {
                            GlassyTextField(
                                value = description,
                                onValueChange = { if (it.length <= 500) description = it },
                                label = stringResource(R.string.settings_feedback_desc_label),
                                placeholder = stringResource(R.string.settings_feedback_desc_placeholder),
                                minHeight = 120.dp
                            )
                            Text(
                                text = "${description.length}/500",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceMuted.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 4.dp, end = 8.dp)
                            )
                        }
                    }
                }

                // Footer (Fixed)
                Spacer(modifier = Modifier.height(20.dp))

                val isEnabled = title.isNotBlank() && description.isNotBlank() && !isLoading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isEnabled) MaterialTheme.colorScheme.primary 
                            else if (isLoading) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .bounceClick(enabled = isEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSubmit(title, description, rating, email)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.settings_send_message),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) Color.Black else Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.settings_close),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean = false,
    minHeight: Dp = Dp.Unspecified
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.3f)) },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (minHeight != Dp.Unspecified) Modifier.heightIn(min = minHeight) else Modifier),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = singleLine
        )
    }
}

@Composable
fun ColorSelectionDialog(
    hazeState: HazeState,
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String, Offset) -> Unit,
    alpha: Float = 1f
) {
    val focusManager = LocalFocusManager.current
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        var tempSelectedColor by remember { mutableStateOf(current) }
        var isCustomMode by remember { mutableStateOf(current.startsWith("#")) }
        
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(24.dp)
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                }
        ) {
            val previewAccentColor = remember(tempSelectedColor) {
                try {
                    if (tempSelectedColor.startsWith("#")) tempSelectedColor.toComposeColor()
                    else when(tempSelectedColor) {
                        "Pink" -> NeonPink
                        "Purple" -> NeonPurple
                        "Amber" -> NeonAmber
                        "Blue" -> NeonBlue
                        else -> NeonTeal
                    }
                } catch(e: Exception) { NeonTeal }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .hazeGlass(
                        state = hazeState, alpha = alpha,
                        shape = RoundedCornerShape(32.dp),
                        style = HazeStyles.glassmorphicDialog
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(400))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.settings_interface_color),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Unified Presets Row with "+" for Custom
                    val presets = listOf(
                        "Teal" to NeonTeal,
                        "Pink" to NeonPink,
                        "Purple" to NeonPurple,
                        "Amber" to NeonAmber,
                        "Blue" to NeonBlue
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        presets.forEach { (name, color) ->
                            val isSelected = !isCustomMode && tempSelectedColor == name
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable { 
                                            isCustomMode = false
                                            tempSelectedColor = name
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isSelected) color else Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // The "+" Button for Custom mode
                        val customSelected = isCustomMode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (customSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        width = if (customSelected) 2.dp else 0.dp,
                                        color = if (customSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { isCustomMode = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_plus),
                                    contentDescription = stringResource(R.string.settings_custom),
                                    tint = if (customSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = stringResource(R.string.settings_custom),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (customSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp
                                ),
                                color = if (customSelected) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Animated Custom Section
                    AnimatedVisibility(
                        visible = isCustomMode,
                        enter = scaleIn(initialScale = 0.95f, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
                        exit = scaleOut(targetScale = 0.95f, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                    ) {
                        // Custom Color Section (Wheel + Hex)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            var hexInput by remember(current) { 
                                mutableStateOf(if (current.startsWith("#")) current.uppercase() else "#00BCD4") 
                            }
                            var isHexFocused by remember { mutableStateOf(false) }
                            var isDragging by remember { mutableStateOf(false) }

                            // Sync hex ← wheel: only when drag ends and hex field not focused
                            LaunchedEffect(isDragging, tempSelectedColor) {
                                if (!isDragging && !isHexFocused && tempSelectedColor.length == 7) {
                                    hexInput = tempSelectedColor.uppercase()
                                }
                            }
                            
                            ColorWheel(
                                selectedColor = tempSelectedColor,
                                onColorChanged = { 
                                    tempSelectedColor = it
                                },
                                onInteractionStart = { 
                                    isDragging = true
                                    focusManager.clearFocus()
                                },
                                onInteractionEnd = { 
                                    isDragging = false
                                },
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Hex Input
                            Row(
                                modifier = Modifier
                                    .width(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(
                                        width = if (isHexFocused) 2.dp else 1.dp,
                                        color = if (isHexFocused) previewAccentColor else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "#",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color.White.copy(alpha = 0.3f)
                                )
                                BasicTextField(
                                    value = if (hexInput.startsWith("#")) hexInput.substring(1) else hexInput,
                                    onValueChange = { newValue ->
                                        if (newValue.length <= 6) {
                                            val clean = newValue.uppercase().filter { it.isDigit() || it in 'A'..'F' }
                                            hexInput = clean
                                            // Update wheel only when we have a full valid hex
                                            if (clean.length == 6) {
                                                tempSelectedColor = "#$clean"
                                            }
                                        }
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Start,
                                        letterSpacing = 1.sp
                                    ),
                                    cursorBrush = SolidColor(previewAccentColor),
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(80.dp)
                                        .padding(start = 4.dp)
                                        .onFocusChanged { focusState ->
                                            isHexFocused = focusState.isFocused
                                            // When losing focus, sync hex display to current color
                                            if (!focusState.isFocused && tempSelectedColor.length == 7) {
                                                hexInput = tempSelectedColor.uppercase().removePrefix("#")
                                            }
                                        }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Track the screen-space centre of the Conferma button
                    val confirmButtonCenter = remember { arrayOf(Offset.Zero) }

                    Button(
                        onClick = {
                            onSelect(tempSelectedColor, confirmButtonCenter[0])
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInWindow()
                                confirmButtonCenter[0] = Offset(
                                    x = pos.x + coords.size.width / 2f,
                                    y = pos.y + coords.size.height / 2f
                                )
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.settings_confirm),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_close), color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
