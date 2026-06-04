package com.cinetrack.ui.components.detail

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeStyle
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.data.Movie
import com.cinetrack.ui.viewmodel.WatchState
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.theme.HazeStyles

/**
 * DetailActions
 * Premium pill-shaped actions with width transitions and custom progress border.
 * Replicates the "FlickTrove" dock logic with "Zero-Friction" animations.
 */
@Composable
fun DetailActions(
    movie: Movie,
    watchState: WatchState,
    progress: Float, // 0.0 to 1.0
    accentColor: Color,
    hazeState: HazeState? = null,
    onStateChange: (WatchState) -> Unit,
    onRemove: () -> Unit,
    onEpisodesClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) {}
    
    // Stato ottimistico per scatenare l'animazione a 0 ms di latenza, 
    // ancor prima che il ViewModel / Database confermino il salvataggio
    var optimisticWatchState by remember(watchState) { mutableStateOf(watchState) }
    
    // Use a single transition for all coordinated animations
    val transition = updateTransition(targetState = optimisticWatchState, label = "DetailActionsTransition")
    
    // Target values for animations
    val displayColor by transition.animateColor(
        transitionSpec = { tween(400) },
        label = "DisplayColor"
    ) { state ->
        when {
            !movie.isReleased && state != WatchState.NONE -> accentColor
            state == WatchState.NONE -> accentColor.copy(alpha = 0.9f)
            else -> accentColor
        }
    }

    // Coordinated split animation parameters
    
    val trashWidth by transition.animateDp(
        transitionSpec = {
            if (targetState != WatchState.NONE) {
                spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
            } else {
                spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
            }
        },
        label = "TrashWidth"
    ) { if (it != WatchState.NONE && movie.isReleased) 56.dp else 0.dp }

    val mainPillWeight by transition.animateFloat(
        transitionSpec = {
            if (targetState == WatchState.WATCHED) {
                tween(800, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f))
            } else {
                // Expand smoothly so the user can see it grow from left to right
                spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
            }
        },
        label = "MainPillWeight"
    ) { state ->
        if (state == WatchState.WATCHED && movie.mediaType != "tv") 0f else 1f
    }

    val mainPillAlpha by transition.animateFloat(
        transitionSpec = { 
            if (targetState == WatchState.WATCHED) {
                tween(300, delayMillis = 300) // Fade later, after width starts shrinking
            } else if (initialState == WatchState.WATCHED) {
                // Immediate fade in, allowing the left-to-right expansion to be visible from the start
                tween(400) 
            } else {
                tween(300, delayMillis = 100)
            }
        },
        label = "MainPillAlpha"
    ) { state ->
        if (state == WatchState.WATCHED && movie.mediaType != "tv") 0f else 1f
    }

    val isPillVisible = !(optimisticWatchState == WatchState.WATCHED && movie.mediaType != "tv")
    val isTrashVisible = optimisticWatchState != WatchState.NONE && movie.isReleased

    val spacing by transition.animateDp(
        transitionSpec = {
            if (targetState == WatchState.WATCHED) tween(400, delayMillis = 100)
            else spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
        },
        label = "Spacing"
    ) { state ->
        if (isPillVisible && isTrashVisible) 12.dp else 0.dp
    }

    val trashAlpha by transition.animateFloat(
        transitionSpec = { 
            if (targetState != WatchState.NONE) tween(300) 
            else tween(150) 
        },
        label = "TrashAlpha"
    ) { if (it != WatchState.NONE && movie.isReleased) 1f else 0f }

    // Offset animation for trash button to avoid graphicsLayer lag with Haze
    val trashOffset by transition.animateDp(
        transitionSpec = {
            if (targetState != WatchState.NONE) {
                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            } else {
                spring(stiffness = Spring.StiffnessMedium)
            }
        },
        label = "TrashOffset"
    ) { if (it != WatchState.NONE && movie.isReleased) 0.dp else 16.dp }

    val trashColor by transition.animateColor(
        transitionSpec = { tween(400) },
        label = "TrashColor"
    ) { if (it == WatchState.NONE) accentColor else Color(0xFFFF3D3D) } // More vibrant red

    // trashScale removed to ensure Haze synchronization with trashWidth layout bounds.

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ProgressBorderAnim"
    )

    // Interaction state for main pill content scale
    var isPillPressed by remember { mutableStateOf(false) }
    val pillContentScale by animateFloatAsState(
        targetValue = if (isPillPressed) 0.96f else 1f,
        animationSpec = if (isPillPressed) spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
                        else spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        label = "PillContentScale"
    )

    // For movies, when they become WATCHED they disappear, so we don't want to show the "VISTO" label/icon transition
    val displayWatchState = if (optimisticWatchState == WatchState.WATCHED && movie.mediaType != "tv") WatchState.BOOKMARKED else optimisticWatchState

    val icon = when {
        !movie.isReleased -> {
            if (displayWatchState != WatchState.NONE) ImageVector.vectorResource(id = R.drawable.ic_bell_piena)
            else ImageVector.vectorResource(id = R.drawable.ic_bell)
        }
        displayWatchState == WatchState.NONE -> ImageVector.vectorResource(id = R.drawable.ic_lista_plus)
        displayWatchState == WatchState.BOOKMARKED -> {
            if (movie.mediaType == "tv") ImageVector.vectorResource(id = R.drawable.ic_lista)
            else ImageVector.vectorResource(id = R.drawable.ic_eye)
        }
        displayWatchState == WatchState.WATCHED -> {
            if (movie.mediaType == "tv") ImageVector.vectorResource(id = R.drawable.ic_lista)
            else ImageVector.vectorResource(id = R.drawable.ic_tick)
        }
        else -> ImageVector.vectorResource(id = R.drawable.ic_lista_plus)
    }
    
    val label = when {
        !movie.isReleased -> {
            if (displayWatchState != WatchState.NONE) "RICORDA ATTIVO"
            else "RICORDAMI"
        }
        displayWatchState == WatchState.NONE -> if (movie.mediaType == "tv") "DA VEDERE" else "DA GUARDARE"
        displayWatchState == WatchState.BOOKMARKED -> if (movie.mediaType == "tv") "SELEZIONA PUNTATE" else "SEGNA COME VISTO"
        displayWatchState == WatchState.WATCHED -> if (movie.mediaType == "tv") "SELEZIONA PUNTATE" else "VISTO"
        else -> ""
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(56.dp),
        horizontalArrangement = Arrangement.Start, // Changed to Start to facilitate leftward shrink
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Action Pill
        if (mainPillWeight > 0.005f) {
            Box(
                modifier = Modifier
                    .weight(mainPillWeight)
                    .fillMaxHeight()
                    .graphicsLayer { 
                        alpha = mainPillAlpha
                        // Remove scaleX and scaleY to prevent Haze misalignment (ritardo)
                        // Use Offscreen strategy when fading to ensure Haze respects alpha (rimane)
                        compositingStrategy = if (mainPillAlpha < 1f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                        transformOrigin = TransformOrigin(0f, 0.5f) // Anchor to Left
                    }
                    .hazeGlass(
                        state = hazeState,
                        shape = RoundedCornerShape(28.dp),
                        borderColor = if (optimisticWatchState != WatchState.NONE) displayColor.copy(alpha = 0.75f) else HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop)
                    )
                .pointerInput(optimisticWatchState, movie.mediaType) {
                    detectTapGestures(
                        onPress = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            isPillPressed = true
                            try {
                                awaitRelease()
                            } finally {
                                isPillPressed = false
                            }
                        },
                        onTap = {
                            if (!movie.isReleased) {
                                // Simple toggle for unreleased movies and tv series
                                val next = if (optimisticWatchState == WatchState.NONE) WatchState.BOOKMARKED else WatchState.NONE
                                if (next == WatchState.BOOKMARKED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                optimisticWatchState = next
                                onStateChange(next)
                            } else if (movie.mediaType == "tv") {
                                if (optimisticWatchState == WatchState.NONE) {
                                    optimisticWatchState = WatchState.BOOKMARKED
                                    onStateChange(WatchState.BOOKMARKED)
                                } else {
                                    onEpisodesClick?.invoke()
                                }
                            } else {
                                val next = when (optimisticWatchState) {
                                    WatchState.NONE -> WatchState.BOOKMARKED
                                    WatchState.BOOKMARKED -> WatchState.WATCHED
                                    WatchState.WATCHED -> WatchState.NONE
                                }
                                optimisticWatchState = next
                                onStateChange(next)
                            }
                        }
                    )
                }
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    if (w < 1f || h < 1f) return@drawBehind
                    
                    // TV Progress Border
                    if (movie.mediaType == "tv" && optimisticWatchState != WatchState.NONE && animatedProgress > 0f) {
                        val path = Path().apply {
                            addRoundRect(
                                androidx.compose.ui.geometry.RoundRect(
                                    rect = androidx.compose.ui.geometry.Rect(0f, 0f, w, h),
                                    cornerRadius = CornerRadius(28.dp.toPx())
                                )
                            )
                        }
                        val pathMeasure = PathMeasure()
                        pathMeasure.setPath(path, false)
                        val length = pathMeasure.length
                        
                        this.drawPath(
                            path = path,
                            color = displayColor,
                            style = Stroke(
                                width = 6.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(length * animatedProgress, length),
                                    -length * (1 - animatedProgress)
                                )
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        scaleX = pillContentScale
                        scaleY = pillContentScale
                    }
            ) {
                val bellRotation = remember { Animatable(0f) }
                
                LaunchedEffect(displayWatchState) {
                    if (!movie.isReleased && displayWatchState != WatchState.NONE) {
                        delay(150)
                        bellRotation.animateTo(-25f, tween(60, easing = LinearEasing))
                        bellRotation.animateTo(20f, tween(100, easing = LinearEasing))
                        bellRotation.animateTo(-15f, tween(100, easing = LinearEasing))
                        bellRotation.animateTo(10f, tween(100, easing = LinearEasing))
                        bellRotation.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
                    }
                }

                Crossfade(targetState = icon, label = "IconCrossfade") { currentIcon ->
                    Icon(
                        imageVector = currentIcon,
                        contentDescription = null,
                        tint = displayColor,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                rotationZ = bellRotation.value
                                transformOrigin = TransformOrigin(0.5f, 0.2f) // Swing from the top
                            }
                    )
                }

                if (label.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    AnimatedContent(
                        targetState = displayWatchState,
                        transitionSpec = {
                            val slideDirection = if (targetState > initialState) 1 else -1
                            if (!movie.isReleased) {
                                (fadeIn(tween(250)) + slideInVertically(tween(350)) { -slideDirection * it / 2 })
                                    .togetherWith(fadeOut(tween(200)) + slideOutVertically(tween(300)) { slideDirection * it / 2 })
                            } else {
                                (fadeIn(tween(250)) + slideInHorizontally(tween(350)) { slideDirection * it / 3 })
                                    .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { -slideDirection * it / 3 })
                            }
                        },
                        label = "TextAnim"
                    ) { targetState ->
                        val targetLabel = when {
                            !movie.isReleased -> {
                                if (targetState != WatchState.NONE) "RICORDA ATTIVO"
                                else "RICORDAMI"
                            }
                            targetState == WatchState.NONE -> if (movie.mediaType == "tv") "DA VEDERE" else "DA GUARDARE"
                            targetState == WatchState.BOOKMARKED -> if (movie.mediaType == "tv") "SELEZIONA PUNTATE" else "SEGNA COME VISTO"
                            targetState == WatchState.WATCHED -> if (movie.mediaType == "tv") "SELEZIONA PUNTATE" else "VISTO"
                            else -> ""
                        }

                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = targetLabel,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.1.sp,
                                    fontSize = if (movie.mediaType == "tv" && targetState != WatchState.NONE) 12.sp else 14.sp
                                ),
                                color = displayColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            if (movie.mediaType == "tv" && targetState != WatchState.NONE && movie.isReleased) {
                                val percentage = (animatedProgress * 100).toInt()
                                Text(
                                    text = if (percentage == 100) "COMPLETATA" else "$percentage% VISTO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = displayColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Filler to push pill to the left when it shrinks
        val fillerWeight = 1f - mainPillWeight
        if (fillerWeight > 0.005f) {
            Spacer(modifier = Modifier.weight(fillerWeight))
        }

        if (spacing > 0.01.dp) {
            Spacer(modifier = Modifier.width(spacing))
        }

        // Trash Button (The Split-off component)
        Box(
            modifier = Modifier
                .width(trashWidth)
                .fillMaxHeight()
                .offset(x = trashOffset)
                .graphicsLayer {
                    alpha = trashAlpha
                    // Scale removed for Haze sync. Alpha handles fade.
                    compositingStrategy = if (trashAlpha < 1f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                },
            contentAlignment = Alignment.Center
        ) {
            if (trashWidth > 20.dp) {
                var isTrashPressed by remember { mutableStateOf(false) }
                val trashIconScale by animateFloatAsState(
                    targetValue = if (isTrashPressed) 0.86f else 1f,
                    animationSpec = if (isTrashPressed) spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
                                    else spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
                    label = "TrashIconScale"
                )

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .pointerInput(onRemove) {
                            detectTapGestures(
                                onPress = {
                                    isTrashPressed = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    try { awaitRelease() } finally { isTrashPressed = false }
                                },
                                onTap = { onRemove() }
                            )
                        }
                        .hazeGlass(
                            state = hazeState,
                            shape = CircleShape,
                            borderColor = trashColor.copy(alpha = 0.75f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_trash),
                        contentDescription = "Remove",
                        tint = trashColor,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = trashIconScale
                                scaleY = trashIconScale
                            }
                    )
                }
            }
        }
    }
}
