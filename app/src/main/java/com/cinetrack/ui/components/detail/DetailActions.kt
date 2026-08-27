package com.cinetrack.ui.components.detail

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import com.cinetrack.data.model.Movie
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
    movieStatus: String? = null,
    watchState: WatchState,
    progress: Float, // 0.0 to 1.0
    accentColor: Color,
    hazeState: HazeState? = null,
    onStateChange: (WatchState) -> Unit,
    onRemove: () -> Unit,
    onEpisodesClick: (() -> Unit)? = null,
    onManageRewatches: (() -> Unit)? = null,
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
    
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    val currentOnRemove by rememberUpdatedState(onRemove)
    val currentOnEpisodesClick by rememberUpdatedState(onEpisodesClick)
    val currentOnManageRewatches by rememberUpdatedState(onManageRewatches)
    
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
    ) { if (it != WatchState.NONE && movie.isReleased) 68.dp else 0.dp }

    val mainPillWeight by transition.animateFloat(
        transitionSpec = {
            if (targetState == WatchState.WATCHED) {
                tween(800, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f))
            } else {
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
                tween(300, delayMillis = 300)
            } else if (initialState == WatchState.WATCHED) {
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
    val pillInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPillPressed by pillInteractionSource.collectIsPressedAsState()
    LaunchedEffect(isPillPressed) {
        if (isPillPressed) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }

    val pillContentScale by animateFloatAsState(
        targetValue = if (isPillPressed) 0.96f else 1f,
        animationSpec = if (isPillPressed) spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
                        else spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        label = "PillContentScale"
    )

    // For movies, when they become WATCHED they disappear, so we don't want to show the "VISTO" label/icon transition
    val displayWatchState = optimisticWatchState

    val icon = when {
        !movie.isReleased -> {
            if (displayWatchState != WatchState.NONE) ImageVector.vectorResource(id = R.drawable.ic_bell_piena)
            else ImageVector.vectorResource(id = R.drawable.ic_bell)
        }
        displayWatchState == WatchState.NONE -> ImageVector.vectorResource(id = R.drawable.ic_lista_plus)
        displayWatchState == WatchState.DROPPED -> ImageVector.vectorResource(id = R.drawable.ic_x)
        displayWatchState == WatchState.BOOKMARKED -> {
            if (movie.mediaType == "tv") ImageVector.vectorResource(id = R.drawable.ic_lista)
            else ImageVector.vectorResource(id = R.drawable.ic_eye)
        }
        displayWatchState == WatchState.WATCHED -> {
            if (movie.mediaType == "tv") ImageVector.vectorResource(id = R.drawable.ic_lista)
            else ImageVector.vectorResource(id = R.drawable.ic_ricarica)
        }
        else -> ImageVector.vectorResource(id = R.drawable.ic_lista_plus)
    }
    
    val label = when {
        !movie.isReleased -> {
            if (displayWatchState != WatchState.NONE) stringResource(R.string.action_reminder_active)
            else stringResource(R.string.action_remind_me)
        }
        displayWatchState == WatchState.NONE -> if (movie.mediaType == "tv") stringResource(R.string.action_to_watch_tv) else stringResource(R.string.action_to_watch_movie)
        displayWatchState == WatchState.DROPPED -> stringResource(R.string.action_dropped)
        displayWatchState == WatchState.BOOKMARKED -> if (movie.mediaType == "tv") stringResource(R.string.action_select_episodes) else stringResource(R.string.action_mark_as_watched)
        displayWatchState == WatchState.WATCHED -> if (movie.mediaType == "tv") stringResource(R.string.action_select_episodes) else stringResource(R.string.action_watched)
        else -> ""
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(56.dp)
    ) {
        val maxAvailableWidth = maxWidth
        
        val targetPillWidth = if (optimisticWatchState == WatchState.WATCHED && movie.mediaType == "movie") {
            0.dp
        } else {
            maxAvailableWidth - spacing - trashWidth
        }
        
        val pillWidth by animateDpAsState(
            targetValue = targetPillWidth,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "PillWidth"
        )

                val isTvCompletedAndEnded = movie.mediaType == "tv" && 
                                          optimisticWatchState == WatchState.WATCHED && 
                                          (movieStatus ?: movie.status)?.lowercase() in listOf("ended", "canceled", "cancelled")
                
                val availableSideActions = remember(movie.mediaType, optimisticWatchState, movie.status, movieStatus) {
                    if (movie.mediaType == "movie") {
                        if (optimisticWatchState == WatchState.WATCHED) listOf(SideAction.REWATCH, SideAction.TRASH)
                        else listOf(SideAction.TRASH)
                    } else {
                        if (optimisticWatchState == WatchState.WATCHED) {
                            if (isTvCompletedAndEnded) listOf(SideAction.REWATCH, SideAction.TRASH)
                            else listOf(SideAction.REWATCH, SideAction.DROP, SideAction.TRASH)
                        } else {
                            listOf(SideAction.REWATCH, SideAction.DROP, SideAction.TRASH)
                        }
                    }
                }
                
                var currentSideActionIndex by remember { mutableIntStateOf(0) }
                
                LaunchedEffect(availableSideActions) {
                    if (currentSideActionIndex >= availableSideActions.size) {
                        currentSideActionIndex = availableSideActions.size - 1
                    }
                    if (optimisticWatchState != WatchState.NONE) {
                        if (movie.mediaType == "tv") {
                            val dropIndex = availableSideActions.indexOf(SideAction.DROP)
                            val rewatchIndex = availableSideActions.indexOf(SideAction.REWATCH)
                            currentSideActionIndex = if (dropIndex >= 0) {
                                dropIndex
                            } else if (rewatchIndex >= 0) {
                                rewatchIndex
                            } else {
                                0
                            }
                        } else {
                            currentSideActionIndex = if (optimisticWatchState != WatchState.WATCHED) {
                                availableSideActions.indexOf(SideAction.TRASH).takeIf { it >= 0 } ?: 0
                            } else {
                                0
                            }
                        }
                    }
                }
                
                val currentSideAction = availableSideActions.getOrElse(currentSideActionIndex) { SideAction.TRASH }
                val isTrashMode = currentSideAction == SideAction.TRASH
                val sideButtonInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isSideButtonPressed by sideButtonInteractionSource.collectIsPressedAsState()
                LaunchedEffect(isSideButtonPressed) {
                    if (isSideButtonPressed) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Action Pill morphs into a circle for movies when WATCHED
            Box(
                modifier = Modifier
                    .width(pillWidth)
                    .fillMaxHeight()
                    .graphicsLayer { 
                        alpha = mainPillAlpha
                        transformOrigin = TransformOrigin(0f, 0.5f) // Anchor to Left
                    }
                    .hazeGlass(
                        state = hazeState,
                        shape = RoundedCornerShape(28.dp),
                        borderColor = if (optimisticWatchState != WatchState.NONE) displayColor.copy(alpha = 0.75f) else HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop)
                    )
                .clickable(
                    interactionSource = pillInteractionSource,
                    indication = null,
                    onClick = {
                        if (!movie.isReleased) {
                            // Simple toggle for unreleased movies and tv series
                            val next = if (optimisticWatchState == WatchState.NONE) WatchState.BOOKMARKED else WatchState.NONE
                            if (next == WatchState.BOOKMARKED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            optimisticWatchState = next
                            currentOnStateChange(next)
                        } else if (movie.mediaType == "tv") {
                            if (optimisticWatchState == WatchState.NONE) {
                                optimisticWatchState = WatchState.BOOKMARKED
                                currentOnStateChange(WatchState.BOOKMARKED)
                            } else {
                                currentOnEpisodesClick?.invoke()
                            }
                        } else {
                            if (optimisticWatchState == WatchState.WATCHED) {
                                currentOnManageRewatches?.invoke()
                            } else {
                                val next = when (optimisticWatchState) {
                                    WatchState.NONE -> WatchState.BOOKMARKED
                                    WatchState.BOOKMARKED -> WatchState.WATCHED
                                    WatchState.WATCHED -> WatchState.NONE
                                    WatchState.DROPPED -> WatchState.BOOKMARKED
                                }
                                optimisticWatchState = next
                                currentOnStateChange(next)
                            }
                        }
                    }
                )
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    if (w < 1f || h < 1f) return@drawBehind
                    
                    // TV Progress Border
                    if (movie.mediaType == "tv" && optimisticWatchState != WatchState.NONE && animatedProgress > 0f) {
                        val strokeWidthPx = 4.dp.toPx()
                        val halfStroke = strokeWidthPx / 2f
                        val path = Path().apply {
                            addRoundRect(
                                androidx.compose.ui.geometry.RoundRect(
                                    rect = androidx.compose.ui.geometry.Rect(halfStroke, halfStroke, w - halfStroke, h - halfStroke),
                                    cornerRadius = CornerRadius(28.dp.toPx() - halfStroke)
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
                                width = strokeWidthPx,
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
                                if (targetState != WatchState.NONE) stringResource(R.string.action_reminder_active)
                                else stringResource(R.string.action_remind_me)
                            }
                            targetState == WatchState.NONE -> if (movie.mediaType == "tv") stringResource(R.string.action_to_watch_tv) else stringResource(R.string.action_to_watch_movie)
                            targetState == WatchState.DROPPED -> stringResource(R.string.action_dropped)
                            targetState == WatchState.BOOKMARKED -> if (movie.mediaType == "tv") stringResource(R.string.action_select_episodes) else stringResource(R.string.action_mark_as_watched)
                            targetState == WatchState.WATCHED -> if (movie.mediaType == "tv") stringResource(R.string.action_select_episodes) else stringResource(R.string.action_watched)
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
                                    text = if (percentage == 100) stringResource(R.string.action_completed) else stringResource(R.string.action_percent_watched, percentage),
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

            // Filler to push TrashBox to the right when Pill shrinks
            Spacer(modifier = Modifier.weight(1f))

            if (spacing > 0.01.dp) {
                Spacer(modifier = Modifier.width(spacing))
            }

        Box(
            modifier = Modifier
                .width(trashWidth)
                .fillMaxHeight()
                .offset(x = trashOffset)
                .graphicsLayer {
                    alpha = trashAlpha
                    // Scale removed for Haze sync. Alpha handles fade.
                },
            contentAlignment = Alignment.Center
        ) {
            if (trashWidth > 20.dp) {



                val sideButtonScale by animateFloatAsState(
                    targetValue = if (isSideButtonPressed) 0.86f else 1f,
                    animationSpec = if (isSideButtonPressed) spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
                                    else spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
                    label = "SideButtonScale"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .pointerInput(Unit) {
                                var dragAccumulator = 0f
                                var hasActionFired = false
                                detectVerticalDragGestures(
                                    onDragStart = { 
                                        dragAccumulator = 0f 
                                        hasActionFired = false
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        if (hasActionFired) return@detectVerticalDragGestures
                                        
                                        dragAccumulator += dragAmount
                                        if (dragAccumulator < -40f && currentSideActionIndex < availableSideActions.size - 1) {
                                            currentSideActionIndex++
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            hasActionFired = true
                                        } else if (dragAccumulator > 40f && currentSideActionIndex > 0) {
                                            currentSideActionIndex--
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            hasActionFired = true
                                        }
                                    }
                                )
                            }
                            .clickable(
                                interactionSource = sideButtonInteractionSource,
                                indication = null,
                                onClick = { 
                                    when (currentSideAction) {
                                        SideAction.TRASH -> currentOnRemove()
                                        SideAction.DROP -> {
                                            val next = if (optimisticWatchState == WatchState.DROPPED) WatchState.BOOKMARKED else WatchState.DROPPED
                                            optimisticWatchState = next
                                            currentOnStateChange(next)
                                        }
                                        SideAction.REWATCH -> currentOnManageRewatches?.invoke()
                                    }
                                }
                            )
                            .hazeGlass(
                                state = hazeState,
                                shape = CircleShape,
                                borderColor = when(currentSideAction) {
                                    SideAction.TRASH -> trashColor
                                    SideAction.REWATCH -> displayColor
                                    SideAction.DROP -> Color.White
                                }.copy(alpha = 0.75f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = currentSideAction,
                            transitionSpec = {
                                val targetIdx = availableSideActions.indexOf(targetState)
                                val initialIdx = availableSideActions.indexOf(initialState)
                                val slideDir = if (targetIdx > initialIdx) 1 else -1
                                slideInVertically { height -> slideDir * height } + fadeIn() togetherWith slideOutVertically { height -> -slideDir * height } + fadeOut()
                            },
                            label = "SideButtonAnim"
                        ) { action ->
                            when (action) {
                                SideAction.TRASH -> {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_trash),
                                        contentDescription = "Remove",
                                        tint = trashColor,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer {
                                                scaleX = sideButtonScale
                                                scaleY = sideButtonScale
                                            }
                                    )
                                }
                                SideAction.REWATCH -> {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_ricarica),
                                        contentDescription = "Rewatch",
                                        tint = displayColor,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer {
                                                scaleX = sideButtonScale
                                                scaleY = sideButtonScale
                                            }
                                    )
                                }
                                SideAction.DROP -> {
                                    // Two-layer approach: static box + animated arrow only
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer {
                                                scaleX = sideButtonScale
                                                scaleY = sideButtonScale
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Layer 1: static box/container - never animates
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_drop_box),
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        // Layer 2: only the arrow animates, clipped to the button circle
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.animation.AnimatedContent(
                                                targetState = optimisticWatchState == WatchState.DROPPED,
                                                transitionSpec = {
                                                    if (targetState) {
                                                        // Going DROPPED: up-arrow enters from below, down-arrow exits upward
                                                        slideInVertically(
                                                            animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f)
                                                        ) { it } + fadeIn(tween(160)) togetherWith
                                                        slideOutVertically(
                                                            animationSpec = tween(160)
                                                        ) { -it } + fadeOut(tween(120))
                                                    } else {
                                                        // Leaving DROPPED: down-arrow enters from above, up-arrow exits downward
                                                        slideInVertically(
                                                            animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f)
                                                        ) { -it } + fadeIn(tween(160)) togetherWith
                                                        slideOutVertically(
                                                            animationSpec = tween(160)
                                                        ) { it } + fadeOut(tween(120))
                                                    }
                                                },
                                                label = "DropArrowAnim"
                                            ) { isDropped ->
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(
                                                        id = if (isDropped) R.drawable.ic_drop_arrow_up else R.drawable.ic_drop_arrow_down
                                                    ),
                                                    contentDescription = "Drop",
                                                    tint = Color.White.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } // closes when
                        } // closes AnimatedContent
                    } // closes side button Box
                    
                    if (optimisticWatchState != WatchState.NONE && availableSideActions.size > 1) {
                        var previousSideActionIndex by remember { mutableIntStateOf(currentSideActionIndex) }
                        LaunchedEffect(currentSideActionIndex) {
                            // Update previous after a slight delay to allow the animation to read it first
                            kotlinx.coroutines.delay(50)
                            previousSideActionIndex = currentSideActionIndex
                        }
                        
                        val isMovingDown = currentSideActionIndex > previousSideActionIndex
                        val isMovingUp = currentSideActionIndex < previousSideActionIndex
                        
                        val targetTop = (currentSideActionIndex * 8).dp
                        val targetBottom = (currentSideActionIndex * 8 + 4).dp
                        
                        val topOffset by animateDpAsState(
                            targetValue = targetTop,
                            animationSpec = spring(
                                dampingRatio = 0.65f, 
                                stiffness = if (isMovingUp) 1500f else if (isMovingDown) 200f else 500f
                            ),
                            label = "topOffset"
                        )
                        val bottomOffset by animateDpAsState(
                            targetValue = targetBottom,
                            animationSpec = spring(
                                dampingRatio = 0.65f, 
                                stiffness = if (isMovingDown) 1500f else if (isMovingUp) 200f else 500f
                            ),
                            label = "bottomOffset"
                        )
                        val activeColor by animateColorAsState(
                            targetValue = when (currentSideAction) {
                                SideAction.TRASH -> trashColor
                                SideAction.REWATCH -> displayColor
                                SideAction.DROP -> Color.White.copy(alpha = 0.8f)
                            },
                            label = "activeColor"
                        )
                        
                        val canvasHeight = ((availableSideActions.size * 8) - 4).dp
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .padding(start = 12.dp, end = 6.dp)
                                .width(4.dp)
                                .height(canvasHeight)
                        ) {
                            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            val faintColor = Color.White.copy(alpha = 0.2f)
                            
                            for (i in 0 until availableSideActions.size) {
                                drawRoundRect(
                                    color = faintColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, (i * 8).dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 4.dp.toPx()),
                                    cornerRadius = cornerRadius
                                )
                            }
                            
                            val tY = topOffset.toPx()
                            val bY = bottomOffset.toPx()
                            val height = kotlin.math.max(0.1f, bY - tY)
                            val startY = kotlin.math.min(tY, bY)
                            
                            drawRoundRect(
                                color = activeColor,
                                topLeft = androidx.compose.ui.geometry.Offset(0f, startY),
                                size = androidx.compose.ui.geometry.Size(4.dp.toPx(), height),
                                cornerRadius = cornerRadius
                            )
                        } // closes Canvas
                    } // closes if
                } // closes Row
            } // closes if (trashWidth > 20.dp)
        } // closes Box (Side Container)
    } // closes Box (fillMaxSize)
} // closes BoxWithConstraints
} // closes fun DetailActions

enum class SideAction {
    REWATCH, DROP, TRASH
}
