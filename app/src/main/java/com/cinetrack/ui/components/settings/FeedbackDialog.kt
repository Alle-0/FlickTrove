package com.cinetrack.ui.components.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import com.cinetrack.util.VibrationHelper
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
import com.cinetrack.ui.viewmodel.SettingsViewModel

@Composable
fun FeedbackDialog(
    initialEmail: String = "",
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(initialEmail) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var rating by remember { mutableStateOf(3) }
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

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
                                visualTransformation = remember { com.cinetrack.ui.utils.MarkdownVisualTransformation() }
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
                                        .bounceClick { onClick() }
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
                Spacer(modifier = Modifier.height(24.dp))

                val isEnabled = title.isNotBlank() && description.text.isNotBlank() && !isLoading
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsDialogCancelButton(
                        text = stringResource(R.string.settings_cancel),
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .bounceClick(enabled = isEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSubmit(title, description.text, rating, email)
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isEnabled) MaterialTheme.colorScheme.primary 
                                else if (isLoading) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else Color.White.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.Black,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.settings_send_message),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isEnabled) Color.Black else Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            }
}

