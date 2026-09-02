package com.cinetrack.ui.components.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.cinetrack.ui.components.common.FlickTroveSwitch
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
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
                
                // Tip: sync missing details after import
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.settings_import_tip_sync),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
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
                    Text(stringResource(R.string.settings_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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

                // Tip: sync missing details after import
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.settings_import_tip_sync),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_scaricare), null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_select_file), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))


                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
                    Text(stringResource(R.string.settings_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
    var description by remember { mutableStateOf(TextFieldValue("")) }
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
                        .premiumScrollbar(feedbackScrollState, width = 3f)
                        .padding(end = 12.dp)
                        .verticalFadingEdges(feedbackScrollState, 16.dp, 16.dp)
                        .verticalScroll(feedbackScrollState)
                ) {
                    // Rating Section
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val fraction = offset.x / size.width.toFloat()
                                    val newRating = (fraction * 5).toInt() + 1
                                    val clamped = newRating.coerceIn(1, 5)
                                    if (clamped != rating) {
                                        rating = clamped
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val fraction = change.position.x / size.width.toFloat()
                                    val newRating = (fraction * 5).toInt() + 1
                                    val clamped = newRating.coerceIn(1, 5)
                                    if (clamped != rating) {
                                        rating = clamped
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                    ) {
                        (1..5).forEach { index ->
                            val isSelected = index <= rating
                            val starColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
                            
                            Box(
                                contentAlignment = Alignment.Center,
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
                            Text(
                                text = stringResource(R.string.settings_feedback_desc_label),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = description,
                                onValueChange = { if (it.text.length <= 2000) description = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                placeholder = { Text(stringResource(R.string.settings_feedback_desc_placeholder), color = Color.White.copy(alpha = 0.3f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                visualTransformation = remember { com.cinetrack.ui.screens.MarkdownVisualTransformation() }
                            )
                            // Markdown toolbar and counter
                            val accentColor = MaterialTheme.colorScheme.primary
                            val mdAction = { prefix: String, suffix: String ->
                                val sel = description.selection
                                val text = description.text
                                val newText = if (sel.collapsed) {
                                    text.substring(0, sel.start) + prefix + suffix + text.substring(sel.end)
                                } else {
                                    text.substring(0, sel.start) + prefix + text.substring(sel.start, sel.end) + suffix + text.substring(sel.end)
                                }
                                val newCursor = if (sel.collapsed) sel.start + prefix.length else sel.end + prefix.length + suffix.length
                                description = TextFieldValue(newText, selection = TextRange(newCursor))
                            }
                            @Composable
                            fun MdBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { onClick() }
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) { content() }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LazyRow(verticalAlignment = Alignment.CenterVertically) {
                                    item {
                                        MdBtn(onClick = { mdAction("**", "**") }) { Text("B", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleSmall) }
                                        MdBtn(onClick = { mdAction("*", "*") }) { Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.White, style = MaterialTheme.typography.titleSmall) }
                                        MdBtn(onClick = { mdAction("~~", "~~") }) { Text("S", textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, color = Color.White, style = MaterialTheme.typography.titleSmall) }
                                        MdBtn(onClick = { mdAction("> ", "") }) { Text("\"\"", color = Color.White, style = MaterialTheme.typography.titleSmall) }
                                        MdBtn(onClick = { mdAction("- ", "") }) { Text("•", color = Color.White, style = MaterialTheme.typography.titleSmall) }
                                    }
                                }
                                // Character counter
                                Text(
                                    text = "${description.text.length}/2000",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Roadmap banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_reddit),
                                    contentDescription = "Reddit",
                                    tint = Color(0xFFFF4500),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.feedback_roadmap_note),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Footer (Fixed)
                Spacer(modifier = Modifier.height(20.dp))

                val isEnabled = title.isNotBlank() && description.text.isNotBlank() && !isLoading
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
                            onSubmit(title, description.text, rating, email)
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
                        stringResource(R.string.settings_cancel),
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
                            containerColor = previewAccentColor,
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
                        Text(stringResource(R.string.settings_cancel), color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    accentColor: Color,
    vibrationEnabled: Boolean,
    alpha: Float = 1f
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(24.dp)
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .hazeGlass(state = hazeState, alpha = alpha, shape = RoundedCornerShape(32.dp), style = HazeStyles.glassmorphicDialog)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val options = listOf(
                        "system" to stringResource(R.string.settings_language_system),
                        "en" to stringResource(R.string.settings_language_en),
                        "it" to stringResource(R.string.settings_language_it),
                        "es" to stringResource(R.string.settings_language_es),
                        "fr" to stringResource(R.string.settings_language_fr),
                        "de" to stringResource(R.string.settings_language_de),
                        "pt" to stringResource(R.string.settings_language_pt),
                        "ru" to stringResource(R.string.settings_language_ru),
                        "hi" to stringResource(R.string.settings_language_hi)
                    )
                    
                    options.forEach { (value, label) ->
                        val isSelected = current == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .bounceClick {
                                    if (vibrationEnabled) com.cinetrack.util.VibrationHelper.vibrateTick(context)
                                    onSelect(value)
                                }
                                .background(
                                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) Color(0xFF1E1E1E) else Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.settings_cancel), color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StartScreenSelectionDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    accentColor: Color,
    vibrationEnabled: Boolean,
    alpha: Float = 1f
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(24.dp)
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .hazeGlass(state = hazeState, alpha = alpha, shape = RoundedCornerShape(32.dp), style = HazeStyles.glassmorphicDialog)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.settings_default_start_tab),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val options = listOf(
                        "feed" to stringResource(R.string.settings_default_start_feed),
                        "home" to stringResource(R.string.settings_default_start_home),
                        "visti" to stringResource(R.string.settings_default_start_visti)
                    )
                    
                    options.forEach { (value, label) ->
                        val isSelected = current == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .bounceClick {
                                    if (vibrationEnabled) com.cinetrack.util.VibrationHelper.vibrateTick(context)
                                    onSelect(value)
                                }
                                .background(
                                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) Color(0xFF1E1E1E) else Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.settings_cancel), color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

private data class DashboardSettingItem(
    val iconRes: Int,
    val titleRes: Int,
    val descRes: Int,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

@Composable
fun DashboardSettingsDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    settingsViewModel: com.cinetrack.ui.viewmodel.SettingsViewModel,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val showMyFolders by settingsViewModel.showMyFolders.collectAsStateWithLifecycle()
    val showYourFlow by settingsViewModel.showYourFlow.collectAsStateWithLifecycle()
    val showGeneralStats by settingsViewModel.showGeneralStats.collectAsStateWithLifecycle()
    val dashboardCardOrder by settingsViewModel.dashboardCardOrder.collectAsStateWithLifecycle()
    
    val focusManager = LocalFocusManager.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100000f)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .hazeGlass(state = activeHazeState, alpha = 1f, shape = RoundedCornerShape(32.dp))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.settings_ui_layout),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .bounceClick { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                var localOrder by remember(dashboardCardOrder) { mutableStateOf(dashboardCardOrder) }
                
                var draggedItemKey by remember { mutableStateOf<String?>(null) }
                var dragOffset by remember { mutableStateOf(0f) }
                var dropTrigger by remember { mutableIntStateOf(0) }
                var itemHeightPx by remember { mutableStateOf(0f) }
                val density = androidx.compose.ui.platform.LocalDensity.current
                val draggedIndex = localOrder.indexOf(draggedItemKey)
                val visualTargetIndex = remember(draggedIndex, dragOffset, itemHeightPx) {
                    if (draggedIndex == -1 || itemHeightPx == 0f) -1
                    else {
                        val offsetSlots = (dragOffset / itemHeightPx).roundToInt()
                        (draggedIndex + offsetSlots).coerceIn(0, localOrder.size - 1)
                    }
                }
                
                val currentDraggedIndex by rememberUpdatedState(draggedIndex)
                val currentVisualTargetIndex by rememberUpdatedState(visualTargetIndex)
                val currentLocalOrder by rememberUpdatedState(localOrder)

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    localOrder.forEachIndexed { index, itemKey ->
                        androidx.compose.runtime.key(itemKey) {
                            val isDragging = draggedItemKey == itemKey
                            
                            val translationTarget = when {
                                isDragging -> dragOffset
                                draggedIndex != -1 && visualTargetIndex != -1 -> {
                                    if (draggedIndex < index && index <= visualTargetIndex) {
                                        -itemHeightPx
                                    } else if (draggedIndex > index && index >= visualTargetIndex) {
                                        itemHeightPx
                                    } else {
                                        0f
                                    }
                                }
                                else -> 0f
                            }
                            val translation = remember(dropTrigger) { androidx.compose.animation.core.Animatable(0f) }
                            

                            androidx.compose.runtime.LaunchedEffect(translationTarget) {
                                if (!isDragging) {
                                    translation.animateTo(
                                        targetValue = translationTarget,
                                        animationSpec = androidx.compose.animation.core.spring(
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }

                            val finalTranslation = if (isDragging) dragOffset else translation.value

                        val itemInfo = when (itemKey) {
                            "folders" -> DashboardSettingItem(R.drawable.ic_cartella, R.string.settings_show_my_folders, R.string.settings_show_my_folders_desc, showMyFolders) { settingsViewModel.toggleShowMyFolders(it) }
                            "flow" -> DashboardSettingItem(R.drawable.ic_sparkle, R.string.settings_show_your_flow, R.string.settings_show_your_flow_desc, showYourFlow) { settingsViewModel.toggleShowYourFlow(it) }
                            "stats" -> DashboardSettingItem(R.drawable.ic_stat, R.string.settings_show_general_stats, R.string.settings_show_general_stats_desc, showGeneralStats) { settingsViewModel.toggleShowGeneralStats(it) }
                            else -> DashboardSettingItem(R.drawable.ic_stat, R.string.settings_show_general_stats, R.string.settings_show_general_stats_desc, showGeneralStats) { settingsViewModel.toggleShowGeneralStats(it) }
                        }

                         Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    if (itemHeightPx == 0f) {
                                        itemHeightPx = coordinates.size.height.toFloat() + with(density) { 16.dp.toPx() }
                                    }
                                }
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer { translationY = finalTranslation }
                                .background(if (isDragging) Color.White.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(24.dp)
                                    .pointerInput(itemKey) {
                                        detectDragGestures(
                                            onDragStart = { draggedItemKey = itemKey },
                                            onDragEnd = { 
                                                if (currentDraggedIndex != -1 && currentVisualTargetIndex != -1 && currentDraggedIndex != currentVisualTargetIndex) {
                                                    val newList = currentLocalOrder.toMutableList()
                                                    val item = newList.removeAt(currentDraggedIndex)
                                                    newList.add(currentVisualTargetIndex, item)
                                                    localOrder = newList
                                                    settingsViewModel.updateDashboardCardOrder(newList)
                                                }
                                                draggedItemKey = null
                                                dragOffset = 0f
                                                dropTrigger++
                                            },
                                            onDragCancel = { 
                                                draggedItemKey = null
                                                dragOffset = 0f 
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount.y
                                            }
                                        )
                                    }
                            )

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = itemInfo.iconRes),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(itemInfo.titleRes), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                Text(stringResource(itemInfo.descRes), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                            }
                            FlickTroveSwitch(
                                checked = itemInfo.checked,
                                onCheckedChange = itemInfo.onCheckedChange,
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        } // End key(itemKey)
                    }
                }
            }
        }
    }
}
