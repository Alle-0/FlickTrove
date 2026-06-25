package com.cinetrack.ui.components.shared

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.utils.bounceClick
import android.content.Intent
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.cinetrack.R
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.cinetrack.utils.AudioRecorderHelper
import androidx.compose.foundation.border
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Delete

@Composable
fun QuickNoteModal(
    movieId: Long,
    mediaType: String,
    initialNote: String,
    movieTitle: String,
    accentColor: Color,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf(initialNote) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    val audioHelper = remember { AudioRecorderHelper(context) }
    var hasAudio by remember { mutableStateOf(audioHelper.hasAudioNote(movieId, mediaType)) }
    val isRecording by audioHelper.isRecording.collectAsStateWithLifecycle()
    val isPlaying by audioHelper.isPlaying.collectAsStateWithLifecycle()
    var audioDurationMs by remember { mutableStateOf(0L) }
    
    LaunchedEffect(hasAudio) {
        if (hasAudio) {
            audioDurationMs = audioHelper.getAudioDuration(movieId, mediaType)
        }
    }
    
    val formattedDuration = remember(audioDurationMs) {
        if (audioDurationMs <= 0) "" else {
            val seconds = (audioDurationMs / 1000) % 60
            val minutes = (audioDurationMs / (1000 * 60)) % 60
            String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioHelper.release()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            audioHelper.startRecording(movieId, mediaType)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                val prefix = if (note.isNotEmpty()) "$note " else ""
                note = prefix + spokenText
            }
        }
    }

    FlickTroveModal(
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp)
                .imePadding(), // Ensure keyboard doesn't cover content
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_pencil),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        text = movieTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.quick_note_title),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 1000) note = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.quick_note_hint),
                        color = Color.White.copy(alpha = 0.3f)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.15f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    cursorColor = accentColor,
                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                ),
                shape = RoundedCornerShape(20.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    lineHeight = 24.sp
                )
            )

            Text(
                text = "${note.length}/1000",
                style = MaterialTheme.typography.labelSmall,
                color = if (note.length >= 1000) Color.Red else Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )

            // Bottom bar for Dictation and Audio Recording
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speech to Text button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.1f))
                        .bounceClick {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            }
                            speechLauncher.launch(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = stringResource(R.string.quick_note_dictate),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Audio Recording UI
                if (hasAudio && !isRecording) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(accentColor.copy(alpha = 0.2f))
                                .border(1.dp, accentColor, RoundedCornerShape(50))
                                .bounceClick {
                                    if (isPlaying) {
                                        audioHelper.stopPlaying()
                                    } else {
                                        audioHelper.startPlaying(movieId, mediaType)
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) stringResource(R.string.quick_note_stop_audio) else stringResource(R.string.quick_note_play_audio),
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (formattedDuration.isNotEmpty() && !isPlaying) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formattedDuration,
                                        color = accentColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else if (isPlaying) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.quick_note_listening),
                                        color = accentColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        val scopeAudioTrash = rememberCoroutineScope()
                        val hapticAudioTrash = LocalHapticFeedback.current
                        val audioTrashRotation = remember { Animatable(0f) }
                        val audioTrashScale = remember { Animatable(1f) }
                        
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .bounceClick {
                                    hapticAudioTrash.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scopeAudioTrash.launch {
                                        audioTrashScale.animateTo(1.15f, tween(50))
                                        audioTrashRotation.animateTo(-15f, tween(40))
                                        audioTrashRotation.animateTo(15f, tween(40))
                                        audioTrashRotation.animateTo(-15f, tween(40))
                                        audioTrashRotation.animateTo(15f, tween(40))
                                        audioTrashRotation.animateTo(0f, tween(40))
                                        audioTrashScale.animateTo(1f, tween(50))
                                        
                                        kotlinx.coroutines.delay(100)
                                        audioHelper.deleteAudioNote(movieId, mediaType)
                                        hasAudio = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.quick_note_delete_audio),
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer {
                                        rotationZ = audioTrashRotation.value
                                        scaleX = audioTrashScale.value
                                        scaleY = audioTrashScale.value
                                    }
                            )
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Red.copy(alpha = 0.2f))
                                    .border(1.dp, Color.Red, RoundedCornerShape(50))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.quick_note_recording),
                                        color = Color.Red,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isRecording) Color.Red else Color.White.copy(alpha = 0.1f))
                                .bounceClick {
                                    if (isRecording) {
                                        audioHelper.stopRecording()
                                        hasAudio = true
                                        audioDurationMs = audioHelper.getAudioDuration(movieId, mediaType)
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.MicOff,
                                contentDescription = if (isRecording) stringResource(R.string.quick_note_stop_recording) else stringResource(R.string.quick_note_record_audio),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .bounceClick(scaleDown = 0.92f, onClick = onDismiss)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .bounceClick(scaleDown = 0.92f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(note)
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = Color.Black
                    )
                }
            }
        }
    }
}
