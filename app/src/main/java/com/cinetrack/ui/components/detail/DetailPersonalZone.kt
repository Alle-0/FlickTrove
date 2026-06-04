package com.cinetrack.ui.components.detail

import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.data.Movie
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.cinetrack.utils.AudioRecorderHelper
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.scale

@Composable
fun DetailPersonalZone(
    movie: Movie,
    accentColor: Color,
    onRate: (Double?) -> Unit,
    onNoteUpdate: (String) -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    var expandedAction by remember { mutableStateOf<String?>(null) } // "rate" or "note"
    var lastActiveAction by remember { mutableStateOf<String?>(null) }
    
    // Track the rating being selected in real-time
    var previewRating by remember(movie.personalRating) { mutableDoubleStateOf(movie.personalRating ?: 0.0) }
    
    val context = LocalContext.current
    val audioHelper = remember { AudioRecorderHelper(context) }
    var hasAudio by remember { mutableStateOf(audioHelper.hasAudioNote(movie.id, movie.mediaType)) }
    
    LaunchedEffect(movie.id, expandedAction) {
        if (expandedAction == null) {
            hasAudio = audioHelper.hasAudioNote(movie.id, movie.mediaType)
        }
    }
    
    if (expandedAction != null) {
        lastActiveAction = expandedAction
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
    ) {
        Text(
            text = "ZONA PERSONALE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            ),
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PersonalAction(
                label = "VALUTA",
                value = if (expandedAction == "rate" || previewRating > 0) String.format("%.1f", previewRating) else "—",
                icon = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                accentColor = accentColor,
                isActive = expandedAction == "rate",
                onClick = { expandedAction = if (expandedAction == "rate") null else "rate" },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            val hasText = !movie.personalNote.isNullOrBlank()
            val noteValueText = when {
                hasText && hasAudio -> "TESTO/AUDIO"
                hasText -> "TESTO"
                hasAudio -> "AUDIO"
                else -> "VUOTA"
            }
            
            PersonalAction(
                label = "NOTA",
                value = noteValueText,
                icon = ImageVector.vectorResource(id = R.drawable.ic_pencil),
                accentColor = accentColor,
                isActive = expandedAction == "note",
                onClick = { expandedAction = if (expandedAction == "note") null else "note" },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        AnimatedVisibility(
            visible = expandedAction != null,
            enter = scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = scaleOut(targetScale = 0.95f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedContent(
                    targetState = expandedAction ?: lastActiveAction,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(250, delayMillis = 90)) + 
                         scaleIn(initialScale = 0.95f, animationSpec = tween(250, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(150)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "EditorTransition"
                ) { action ->
                    when (action) {
                        "rate" -> RatingPickerBox(
                            currentRating = movie.personalRating,
                            accentColor = accentColor,
                            hazeState = hazeState,
                            onDismiss = { 
                                expandedAction = null
                                previewRating = movie.personalRating ?: 0.0
                            },
                            onSave = { 
                                onRate(it)
                                expandedAction = null
                            },
                            onRatingChange = { previewRating = it }
                        )
                        "note" -> NoteEditorBox(
                            movieId = movie.id,
                            mediaType = movie.mediaType,
                            currentNote = movie.personalNote,
                            accentColor = accentColor,
                            hazeState = hazeState,
                            onDismiss = { expandedAction = null },
                            onSave = { 
                                onNoteUpdate(it)
                                expandedAction = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalAction(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = if (isPressed) spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
                        else spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        label = "ActionScale"
    )

    val hasValue = value != "—" && value != "VUOTA"
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .background(
                if (isActive) accentColor.copy(alpha = 0.15f) 
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = 0.5.dp,
                color = if (isActive) accentColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(50)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (hasValue || isActive) accentColor.copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.1f), 
                        RoundedCornerShape(50)
                    )
                    .border(
                        width = 0.5.dp,
                        color = if (hasValue || isActive) accentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (hasValue || isActive) accentColor else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.4f),
                    lineHeight = 10.sp
                )
                Text(
                    text = value,
                    fontSize = when {
                        hasValue && label == "VALUTA" -> 34.sp
                        hasValue -> 18.sp
                        else -> 14.sp
                    },
                    fontWeight = FontWeight.Black,
                    color = if (hasValue) accentColor else Color.White.copy(alpha = 0.6f),
                    lineHeight = when {
                        hasValue && label == "VALUTA" -> 34.sp
                        hasValue -> 18.sp
                        else -> 14.sp
                    }
                )
            }
        }
    }
}
