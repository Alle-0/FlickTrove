package com.cinetrack.ui.components.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.SymbiontPagerIndicator
import com.cinetrack.ui.viewmodel.WatchState
import dev.chrisbanes.haze.HazeState

/**
 * Side action wheel and button for the Detail actions bar (Rewatch, Drop, Trash).
 */
@Composable
fun DetailSideActionButton(
    currentSideAction: SideAction,
    availableSideActions: List<SideAction>,
    currentSideActionIndex: Int,
    optimisticWatchState: WatchState,
    trashColor: Color,
    displayColor: Color,
    hazeState: HazeState?,
    haptic: HapticFeedback,
    onSideActionIndexChange: (Int) -> Unit,
    onExecuteAction: (SideAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val sideButtonInteractionSource = remember { MutableInteractionSource() }
    val isSideButtonPressed by sideButtonInteractionSource.collectIsPressedAsState()

    val currentActionIndex by rememberUpdatedState(currentSideActionIndex)
    val currentActions by rememberUpdatedState(availableSideActions)
    val onActionIndexChange by rememberUpdatedState(onSideActionIndexChange)

    LaunchedEffect(isSideButtonPressed) {
        if (isSideButtonPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val sideButtonScale by animateFloatAsState(
        targetValue = if (isSideButtonPressed) 0.86f else 1f,
        animationSpec = if (isSideButtonPressed) {
            spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
        } else {
            spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "SideButtonScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = sideButtonScale
                    scaleY = sideButtonScale
                }
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
                            if (dragAccumulator < -40f && currentActionIndex < currentActions.size - 1) {
                                onActionIndexChange(currentActionIndex + 1)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hasActionFired = true
                            } else if (dragAccumulator > 40f && currentActionIndex > 0) {
                                onActionIndexChange(currentActionIndex - 1)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hasActionFired = true
                            }
                        }
                    )
                }
                .clickable(
                    interactionSource = sideButtonInteractionSource,
                    indication = null,
                    onClick = { onExecuteAction(currentSideAction) }
                )
                .hazeGlass(
                    state = hazeState,
                    shape = CircleShape,
                    borderColor = when (currentSideAction) {
                        SideAction.TRASH -> trashColor
                        SideAction.REWATCH -> displayColor
                        SideAction.DROP -> Color.White
                    }.copy(alpha = 0.75f),
                    useRefraction = true
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentSideAction,
                transitionSpec = {
                    val targetIdx = availableSideActions.indexOf(targetState)
                    val initialIdx = availableSideActions.indexOf(initialState)
                    val slideDir = if (targetIdx > initialIdx) 1 else -1
                    slideInVertically { height -> slideDir * height } + fadeIn() togetherWith
                        slideOutVertically { height -> -slideDir * height } + fadeOut()
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
                        DetailSideDropIcon(
                            isDropped = optimisticWatchState == WatchState.DROPPED,
                            scale = sideButtonScale
                        )
                    }
                }
            }
        }

        if (optimisticWatchState != WatchState.NONE && availableSideActions.size > 1) {
            val activeColor by animateColorAsState(
                targetValue = when (currentSideAction) {
                    SideAction.TRASH -> trashColor
                    SideAction.REWATCH -> displayColor
                    SideAction.DROP -> Color.White.copy(alpha = 0.8f)
                },
                label = "activeColor"
            )

            SymbiontPagerIndicator(
                currentPage = currentSideActionIndex,
                pageCount = availableSideActions.size,
                modifier = Modifier
                    .padding(start = 12.dp, end = 6.dp)
                    .rotate(90f),
                accentColor = activeColor,
                dotSize = 4.dp,
                spacing = 4.dp,
                persistPreviousDots = false
            )
        }
    }
}

@Composable
private fun DetailSideDropIcon(
    isDropped: Boolean,
    scale: Float
) {
    // Two-layer approach: static box + animated arrow only
    Box(
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
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
            AnimatedContent(
                targetState = isDropped,
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
            ) { dropped ->
                Icon(
                    imageVector = ImageVector.vectorResource(
                        id = if (dropped) R.drawable.ic_drop_arrow_up else R.drawable.ic_drop_arrow_down
                    ),
                    contentDescription = "Drop",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
