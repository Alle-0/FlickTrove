package com.cinetrack.ui.components.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.viewmodel.WatchState
import dev.chrisbanes.haze.HazeState

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
    onPreviewWatch: (() -> Unit)? = null,
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
    val currentOnPreviewWatch by rememberUpdatedState(onPreviewWatch)
    
    // Use a single transition for all coordinated animations
    val transition = updateTransition(targetState = optimisticWatchState, label = "DetailActionsTransition")
    
    // Target values for animations
    val displayColor by transition.animateColor(
        transitionSpec = { tween(400) },
        label = "DisplayColor"
    ) { state ->
        when {
            state == WatchState.WATCHED -> accentColor
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
    ) { if (it != WatchState.NONE && (movie.isReleased || it == WatchState.WATCHED)) 68.dp else 0.dp }

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
    val isTrashVisible = optimisticWatchState != WatchState.NONE && (movie.isReleased || optimisticWatchState == WatchState.WATCHED)

    val spacing by transition.animateDp(
        transitionSpec = {
            if (targetState == WatchState.WATCHED) tween(400, delayMillis = 100)
            else spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
        },
        label = "Spacing"
    ) {
        if (isPillVisible && isTrashVisible) 12.dp else 0.dp
    }

    val trashAlpha by transition.animateFloat(
        transitionSpec = { 
            if (targetState != WatchState.NONE) tween(300) 
            else tween(150) 
        },
        label = "TrashAlpha"
    ) { if (it != WatchState.NONE && (movie.isReleased || it == WatchState.WATCHED)) 1f else 0f }

    val trashOffset by transition.animateDp(
        transitionSpec = {
            if (targetState != WatchState.NONE) {
                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            } else {
                spring(stiffness = Spring.StiffnessMedium)
            }
        },
        label = "TrashOffset"
    ) { if (it != WatchState.NONE && (movie.isReleased || it == WatchState.WATCHED)) 0.dp else 16.dp }

    val trashColor by transition.animateColor(
        transitionSpec = { tween(400) },
        label = "TrashColor"
    ) { if (it == WatchState.NONE) accentColor else Color(0xFFFF3D3D) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ProgressBorderAnim"
    )

    val displayWatchState = optimisticWatchState

    val icon = when {
        displayWatchState == WatchState.WATCHED -> {
            if (movie.mediaType == "tv") ImageVector.vectorResource(id = R.drawable.ic_lista)
            else ImageVector.vectorResource(id = R.drawable.ic_ricarica)
        }
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
        else -> ImageVector.vectorResource(id = R.drawable.ic_lista_plus)
    }
    
    val label = when {
        displayWatchState == WatchState.WATCHED -> if (movie.mediaType == "tv") stringResource(R.string.action_select_episodes) else stringResource(R.string.action_watched)
        !movie.isReleased -> {
            if (displayWatchState != WatchState.NONE) stringResource(R.string.action_reminder_active)
            else stringResource(R.string.action_remind_me)
        }
        displayWatchState == WatchState.NONE -> if (movie.mediaType == "tv") stringResource(R.string.action_to_watch_tv) else stringResource(R.string.action_to_watch_movie)
        displayWatchState == WatchState.DROPPED -> stringResource(R.string.action_dropped)
        displayWatchState == WatchState.BOOKMARKED -> if (movie.mediaType == "tv") stringResource(R.string.action_select_episodes) else stringResource(R.string.action_mark_as_watched)
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
        
        val defaultSideActionIndex = remember(availableSideActions, movie.mediaType, optimisticWatchState) {
            if (optimisticWatchState != WatchState.NONE) {
                if (movie.mediaType == "tv") {
                    val dropIndex = availableSideActions.indexOf(SideAction.DROP)
                    val rewatchIndex = availableSideActions.indexOf(SideAction.REWATCH)
                    if (dropIndex >= 0) dropIndex else if (rewatchIndex >= 0) rewatchIndex else 0
                } else {
                    if (optimisticWatchState != WatchState.WATCHED) {
                        availableSideActions.indexOf(SideAction.TRASH).takeIf { it >= 0 } ?: 0
                    } else {
                        0
                    }
                }
            } else 0
        }
        
        var currentSideActionIndex by remember { mutableIntStateOf(defaultSideActionIndex) }
        
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

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Action Pill
            DetailMainPillButton(
                movie = movie,
                optimisticWatchState = optimisticWatchState,
                pillWidth = pillWidth,
                mainPillAlpha = mainPillAlpha,
                displayColor = displayColor,
                animatedProgress = animatedProgress,
                icon = icon,
                label = label,
                hazeState = hazeState,
                context = context,
                permissionLauncher = permissionLauncher,
                onStateChange = currentOnStateChange,
                onEpisodesClick = currentOnEpisodesClick,
                onManageRewatches = currentOnManageRewatches,
                onPreviewWatch = currentOnPreviewWatch,
                onOptimisticStateUpdate = { optimisticWatchState = it }
            )

            // Filler to push TrashBox to the right when Pill shrinks
            Spacer(modifier = Modifier.weight(1f))

            if (spacing > 0.01.dp) {
                Spacer(modifier = Modifier.width(spacing))
            }

            // Side Action Button (Trash / Drop / Rewatch)
            Box(
                modifier = Modifier
                    .width(trashWidth)
                    .fillMaxHeight()
                    .offset(x = trashOffset)
                    .graphicsLayer {
                        alpha = trashAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                if (trashWidth > 20.dp) {
                    DetailSideActionButton(
                        currentSideAction = currentSideAction,
                        availableSideActions = availableSideActions,
                        currentSideActionIndex = currentSideActionIndex,
                        optimisticWatchState = optimisticWatchState,
                        trashColor = trashColor,
                        displayColor = displayColor,
                        hazeState = hazeState,
                        haptic = haptic,
                        onSideActionIndexChange = { currentSideActionIndex = it },
                        onExecuteAction = { action ->
                            when (action) {
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
                }
            }
        }
    }
}

enum class SideAction {
    REWATCH, DROP, TRASH
}
