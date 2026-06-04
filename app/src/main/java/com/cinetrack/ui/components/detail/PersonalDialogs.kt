package com.cinetrack.ui.components.detail

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import dev.chrisbanes.haze.HazeState
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.components.shared.FluidRatingBar
import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.cinetrack.utils.AudioRecorderHelper
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RatingPickerBox(
    currentRating: Double?,
    accentColor: Color,
    hazeState: HazeState?,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit,
    onRatingChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var rating by remember(currentRating) { mutableDoubleStateOf(currentRating ?: 0.0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(0.5.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    "SPOSTA PER VALUTARE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black
                )
                
                if (currentRating != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .bounceClick {
                                rating = currentRating
                                onRatingChange(currentRating)
                            }
                    ) {
                        Text(
                            String.format("PREC. %.1f", currentRating),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = 8.dp, y = (-4).dp)
                    .bounceClick(scaleDown = 0.85f, onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Chiudi",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        FluidRatingBar(
            rating = rating,
            onRatingChange = { 
                rating = it
                onRatingChange(it)
            },
            accentColor = accentColor,
            starSize = 56.dp,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val scope = rememberCoroutineScope()
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            val trashRotation = remember { Animatable(0f) }
            val trashScale = remember { Animatable(1f) }

            if (currentRating != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .bounceClick { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            scope.launch {
                                trashScale.animateTo(1.15f, tween(50))
                                trashRotation.animateTo(-15f, tween(40))
                                trashRotation.animateTo(15f, tween(40))
                                trashRotation.animateTo(-15f, tween(40))
                                trashRotation.animateTo(15f, tween(40))
                                trashRotation.animateTo(0f, tween(40))
                                trashScale.animateTo(1f, tween(50))
                                
                                kotlinx.coroutines.delay(100)
                                onSave(null)
                            }
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Rimuovi",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = trashRotation.value
                                scaleX = trashScale.value
                                scaleY = trashScale.value
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.8f)
                            }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .bounceClick { onSave(if (rating > 0) rating else null) }
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (rating > 0) accentColor else Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (rating > 0) "SALVA VALUTAZIONE" else "RIMUOVI VOTO",
                    color = if (rating > 0) Color.Black else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun NoteEditorBox(
    movieId: Long,
    mediaType: String,
    currentNote: String?,
    accentColor: Color,
    hazeState: HazeState?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var note by remember(currentNote) { mutableStateOf(currentNote ?: "") }
    
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
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                note = if (note.isEmpty()) spokenText else "$note $spokenText"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(0.5.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "NOTA PERSONALE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Black
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .bounceClick(scaleDown = 0.85f, onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Chiudi",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(accentColor),
                modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(bottom = 8.dp),
                decorationBox = { innerTextField ->
                    if (note.isEmpty()) {
                        Text(
                            "Scrivi o detta una nota...",
                            color = Color.White.copy(alpha = 0.2f),
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                }
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
                        contentDescription = "Detta nota",
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
                                    contentDescription = if (isPlaying) "Ferma audio" else "Riproduci audio",
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
                                        text = "In ascolto",
                                        color = accentColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        val scopeAudioTrash = rememberCoroutineScope()
                        val hapticAudioTrash = androidx.compose.ui.platform.LocalHapticFeedback.current
                        val audioTrashRotation = remember { Animatable(0f) }
                        val audioTrashScale = remember { Animatable(1f) }
                        
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .bounceClick {
                                    hapticAudioTrash.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
                                contentDescription = "Elimina audio",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer {
                                        rotationZ = audioTrashRotation.value
                                        scaleX = audioTrashScale.value
                                        scaleY = audioTrashScale.value
                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.8f)
                                    }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isRecording) Color(0xFFFF3B30).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                            .border(1.dp, if (isRecording) Color(0xFFFF3B30) else Color.Transparent, RoundedCornerShape(50))
                            .bounceClick {
                                if (isRecording) {
                                    audioHelper.stopRecording()
                                    hasAudio = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isRecording) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.5f))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isRecording) "REGISTRANDO..." else "REGISTRA VOCE",
                                color = if (isRecording) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Warning text for audio
            if (hasAudio) {
                Text(
                    text = "L'audio è salvato solo in locale e non verrà sincronizzato.",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 9.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!currentNote.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .bounceClick { onSave("") }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Rimuovi",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .bounceClick { onSave(note) }
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text("SALVA NOTA", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
    }
}

private fun calculateRatingFromOffset(x: Float, totalWidth: Float): Double {
    val rawRating = (x / totalWidth) * 10.0
    // Round to nearest 0.5 and coerce
    return (Math.round(rawRating * 2.0) / 2.0).coerceIn(0.0, 10.0)
}
