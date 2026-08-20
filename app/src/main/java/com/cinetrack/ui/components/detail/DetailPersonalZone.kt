package com.cinetrack.ui.components.detail

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background

import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
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
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.cinetrack.util.AudioRecorderHelper
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
    onCheckInClick: () -> Unit = {},
    hazeState: HazeState? = null,
    globalStats: com.cinetrack.data.model.GlobalMovieStats? = null,
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

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
    ) {
        Text(
            text = stringResource(R.string.detail_personal_zone),
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
            val hasVibe = !movie.emotionalVibes.isNullOrBlank() || movie.favoriteActorId != null
            val vibeValueText = if (hasVibe) stringResource(R.string.personal_zone_saved) else stringResource(R.string.personal_zone_empty)
            val canInteract = movie.watched || movie.mediaType == "tv"
            
            PersonalAction(
                label = stringResource(R.string.personal_zone_vibe),
                value = vibeValueText,
                hasValue = hasVibe,
                isRateAction = false,
                icon = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                accentColor = accentColor,
                isActive = false,
                enabled = canInteract,
                onClick = { if (canInteract) onCheckInClick() },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            val hasText = !movie.personalNote.isNullOrBlank()
            val noteValueText = when {
                hasText && hasAudio -> stringResource(R.string.personal_zone_text_audio)
                hasText -> stringResource(R.string.personal_zone_text)
                hasAudio -> stringResource(R.string.personal_zone_audio)
                else -> stringResource(R.string.personal_zone_empty)
            }
            
            PersonalAction(
                label = stringResource(R.string.personal_zone_note),
                value = noteValueText,
                hasValue = hasText || hasAudio,
                isRateAction = false,
                icon = ImageVector.vectorResource(id = R.drawable.ic_pencil),
                accentColor = accentColor,
                isActive = expandedAction == "note",
                onClick = { expandedAction = if (expandedAction == "note") null else "note" },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
        ) {
            val globalRating = if (globalStats != null && globalStats.ratingCount > 0) {
                globalStats.totalRating / globalStats.ratingCount
            } else {
                movie.voteAverage
            }
            val globalRatingText = if (globalRating != null && globalRating > 0) String.format(java.util.Locale.US, "%.1f", globalRating) else "—"
            PersonalAction(
                label = stringResource(R.string.personal_zone_rate),
                value = if (expandedAction == "rate" || previewRating > 0) String.format("%.1f", previewRating) else "—",
                hasValue = (expandedAction == "rate" || previewRating > 0),
                isRateAction = true,
                icon = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                accentColor = accentColor,
                isActive = expandedAction == "rate",
                enabled = canInteract,
                onClick = { if (canInteract) expandedAction = if (expandedAction == "rate") null else "rate" },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                trailingContent = {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.personal_zone_global_rating),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.4f),
                            lineHeight = 10.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_people),
                                contentDescription = null,
                                tint = if (globalRating != null && globalRating > 0) accentColor else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = globalRatingText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (globalRating != null && globalRating > 0) accentColor else Color.White.copy(alpha = 0.6f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
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
    hasValue: Boolean,
    isRateAction: Boolean,
    icon: ImageVector,
    accentColor: Color,
    isActive: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = if (isPressed) spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
                        else spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        label = "ActionScale"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
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
            .pointerInput(enabled) {
                if (enabled) {
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
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp).fillMaxWidth(),
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
                        hasValue && isRateAction -> 34.sp
                        hasValue -> 18.sp
                        else -> 14.sp
                    },
                    fontWeight = FontWeight.Black,
                    color = if (hasValue) accentColor else Color.White.copy(alpha = 0.6f),
                    lineHeight = when {
                        hasValue && isRateAction -> 34.sp
                        hasValue -> 18.sp
                        else -> 14.sp
                    }
                )
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.weight(1f))
                trailingContent()
            } else {
                Spacer(modifier = Modifier.width(16.dp)) // Maintain padding for non-trailing actions
            }
        }
    }
}
