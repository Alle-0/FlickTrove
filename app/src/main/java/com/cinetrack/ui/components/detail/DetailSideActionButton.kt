package com.cinetrack.ui.components.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.SymbiontPagerIndicator
import com.cinetrack.ui.viewmodel.WatchState
import dev.chrisbanes.haze.HazeState

/**
 * Side action wheel and button for the Detail actions bar (Rewatch, Drop, Notifications, Trash).
 * Supports:
 * - Tap to execute current action
 * - Quick flick/swipe to cycle actions
 * - Long-press morphing scrubber: opens a vertical glass capsule with all available actions,
 *   allowing continuous scrubbing with haptic feedback on each item and smooth release-to-select.
 */
@Composable
fun DetailSideActionButton(
    currentSideAction: SideAction,
    availableSideActions: List<SideAction>,
    currentSideActionIndex: Int,
    optimisticWatchState: WatchState,
    isRemindersMuted: Boolean = false,
    trashColor: Color,
    displayColor: Color,
    hazeState: HazeState?,
    haptic: HapticFeedback,
    onSideActionIndexChange: (Int) -> Unit,
    onExecuteAction: (SideAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val currentActionIndex by rememberUpdatedState(currentSideActionIndex)
    val currentActions by rememberUpdatedState(availableSideActions)
    val onActionIndexChange by rememberUpdatedState(onSideActionIndexChange)
    val onExecute by rememberUpdatedState(onExecuteAction)

    var isExpanded by remember { mutableStateOf(false) }
    var hoveredSlotIndex by remember { mutableIntStateOf(0) }
    var isPressedDown by remember { mutableStateOf(false) }

    // All available other actions to present above the base button
    val otherActions = remember(availableSideActions, currentSideAction) {
        availableSideActions.filter { it != currentSideAction }
    }

    val slotSpacing = 60.dp
    val expandedHeight = 56.dp + (slotSpacing * otherActions.size)

    val morphHeight by animateDpAsState(
        targetValue = if (isExpanded) expandedHeight else 56.dp,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SideActionMorphHeight"
    )

    val sideButtonScale by animateFloatAsState(
        targetValue = when {
            isExpanded -> 1f
            isPressedDown -> 0.88f
            else -> 1f
        },
        animationSpec = if (isPressedDown && !isExpanded) {
            spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
        } else {
            spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "SideButtonScale"
    )

    val activeBorderColor = when (currentSideAction) {
        SideAction.TRASH -> trashColor
        SideAction.REWATCH -> displayColor
        SideAction.DROP -> Color.White
        SideAction.NOTIFICATIONS -> if (isRemindersMuted) Color.White.copy(alpha = 0.5f) else displayColor
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier.fillMaxWidth()
    ) {
        // Floating label when scrubbing over an action
        val hoveredAction = when {
            !isExpanded -> null
            hoveredSlotIndex == 0 -> currentSideAction
            hoveredSlotIndex > 0 && hoveredSlotIndex <= otherActions.size -> otherActions[hoveredSlotIndex - 1]
            else -> null
        }

        // Anchor box that defines the bottom alignment anchor (56x56dp)
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = sideButtonScale
                    scaleY = sideButtonScale
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Morphing glass capsule extending upwards from the base button
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .requiredHeight(morphHeight)
                    .pointerInput(currentActions, currentSideAction, currentActionIndex, otherActions) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startY = down.position.y
                            val startTime = System.currentTimeMillis()
                            var isLongPressActive = false
                            var currentHover = 0
                            isPressedDown = true

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                                val elapsed = System.currentTimeMillis() - startTime
                                val deltaY = pointer.position.y - startY

                                if (!pointer.pressed) {
                                    pointer.consume()
                                    isPressedDown = false

                                    if (isLongPressActive) {
                                        // Released after scrubbing!
                                        if (currentHover > 0 && currentHover <= otherActions.size) {
                                            val selectedAction = otherActions[currentHover - 1]
                                            val newIndex = currentActions.indexOf(selectedAction)
                                            if (newIndex >= 0) {
                                                onActionIndexChange(newIndex)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                        isExpanded = false
                                        hoveredSlotIndex = 0
                                    } else {
                                        // Released before long-press threshold
                                        if (Math.abs(deltaY) > 35f) {
                                            // Quick vertical swipe gesture
                                            if (deltaY < 0 && currentActionIndex < currentActions.size - 1) {
                                                onActionIndexChange(currentActionIndex + 1)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } else if (deltaY > 0 && currentActionIndex > 0) {
                                                onActionIndexChange(currentActionIndex - 1)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        } else {
                                            // Quick tap -> execute current action
                                            onExecute(currentSideAction)
                                        }
                                    }
                                    break
                                }

                                // Trigger long-press when holding for > 190ms with minimal movement
                                if (!isLongPressActive && elapsed >= 190 && Math.abs(deltaY) < 25f && otherActions.isNotEmpty()) {
                                    isLongPressActive = true
                                    isExpanded = true
                                    currentHover = 0
                                    hoveredSlotIndex = 0
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pointer.consume()
                                }

                                if (isLongPressActive) {
                                    pointer.consume()
                                    // Upward movement is negative deltaY
                                    val slotHeightPx = with(density) { slotSpacing.toPx() }
                                    val targetHover = if (deltaY >= -slotHeightPx * 0.45f) {
                                        0
                                    } else {
                                        val step = ((-deltaY - (slotHeightPx * 0.45f)) / slotHeightPx).toInt() + 1
                                        step.coerceIn(0, otherActions.size)
                                    }

                                    if (targetHover != currentHover) {
                                        currentHover = targetHover
                                        hoveredSlotIndex = targetHover
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                        }
                    }
                    .hazeGlass(
                        state = hazeState,
                        shape = RoundedCornerShape(28.dp),
                        borderColor = activeBorderColor.copy(alpha = 0.75f),
                        useRefraction = true
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Base action at the bottom of the capsule (Slot 0)
                val isBaseHovered = isExpanded && hoveredSlotIndex == 0
                val baseScale by animateFloatAsState(
                    targetValue = if (isBaseHovered) 1.18f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
                    label = "BaseScale"
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.BottomCenter)
                        .graphicsLayer {
                            scaleX = baseScale
                            scaleY = baseScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isBaseHovered) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(activeBorderColor.copy(alpha = 0.22f))
                        )
                    }
                    SideActionIconContent(
                        action = currentSideAction,
                        optimisticWatchState = optimisticWatchState,
                        isRemindersMuted = isRemindersMuted,
                        trashColor = trashColor,
                        displayColor = displayColor
                    )
                }

                // Other actions stacked vertically above the base button
                otherActions.forEachIndexed { index, action ->
                    val slotIndex = index + 1
                    val yOffset = -(slotSpacing * slotIndex)
                    val isHovered = isExpanded && hoveredSlotIndex == slotIndex

                    val itemAlpha by animateFloatAsState(
                        targetValue = if (isExpanded) 1f else 0f,
                        animationSpec = tween(durationMillis = 180, delayMillis = index * 25),
                        label = "ItemAlpha_$index"
                    )
                    val itemScale by animateFloatAsState(
                        targetValue = if (isExpanded) (if (isHovered) 1.20f else 0.95f) else 0.4f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
                        label = "ItemScale_$index"
                    )

                    if (itemAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .align(Alignment.BottomCenter)
                                .offset(y = yOffset)
                                .graphicsLayer {
                                    alpha = itemAlpha
                                    scaleX = itemScale
                                    scaleY = itemScale
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isHovered) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(getSideActionColor(action, isRemindersMuted, trashColor, displayColor).copy(alpha = 0.25f))
                                )
                            }
                            SideActionIconContent(
                                action = action,
                                optimisticWatchState = optimisticWatchState,
                                isRemindersMuted = isRemindersMuted,
                                trashColor = trashColor,
                                displayColor = displayColor
                            )
                        }
                    }
                }
            }

            // Floating label badge positioned to the left of the currently hovered slot
            if (isExpanded && hoveredAction != null) {
                val labelYOffset = -(slotSpacing * hoveredSlotIndex)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-68).dp, y = labelYOffset - 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xEE16161A),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        shadowElevation = 8.dp
                    ) {
                        Text(
                            text = getSideActionLabel(hoveredAction, optimisticWatchState, isRemindersMuted),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Symbiont pager indicator (smoothly fades out when the morphing scrubber is open)
        if (optimisticWatchState != WatchState.NONE && availableSideActions.size > 1) {
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (isExpanded) 0f else 1f,
                animationSpec = tween(durationMillis = 150),
                label = "IndicatorAlpha"
            )

            if (indicatorAlpha > 0.01f) {
                val activeColor by animateColorAsState(
                    targetValue = when (currentSideAction) {
                        SideAction.TRASH -> trashColor
                        SideAction.REWATCH -> displayColor
                        SideAction.DROP -> Color.White.copy(alpha = 0.8f)
                        SideAction.NOTIFICATIONS -> if (isRemindersMuted) Color.White.copy(alpha = 0.6f) else displayColor
                    },
                    label = "activeColor"
                )

                SymbiontPagerIndicator(
                    currentPage = currentSideActionIndex,
                    pageCount = availableSideActions.size,
                    modifier = Modifier
                        .graphicsLayer { alpha = indicatorAlpha }
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
}

@Composable
private fun SideActionIconContent(
    action: SideAction,
    optimisticWatchState: WatchState,
    isRemindersMuted: Boolean,
    trashColor: Color,
    displayColor: Color,
    modifier: Modifier = Modifier
) {
    when (action) {
        SideAction.TRASH -> {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_trash),
                contentDescription = "Remove",
                tint = trashColor,
                modifier = modifier.size(24.dp)
            )
        }
        SideAction.REWATCH -> {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_ricarica),
                contentDescription = "Rewatch",
                tint = displayColor,
                modifier = modifier.size(24.dp)
            )
        }
        SideAction.DROP -> {
            DetailSideDropIcon(
                isDropped = optimisticWatchState == WatchState.DROPPED,
                scale = 1f
            )
        }
        SideAction.NOTIFICATIONS -> {
            Icon(
                imageVector = ImageVector.vectorResource(
                    id = if (isRemindersMuted) R.drawable.ic_bell_off else R.drawable.ic_bell_piena
                ),
                contentDescription = if (isRemindersMuted) "Enable reminders" else "Mute reminders",
                tint = if (isRemindersMuted) Color.White.copy(alpha = 0.6f) else displayColor,
                modifier = modifier.size(24.dp)
            )
        }
    }
}

private fun getSideActionColor(
    action: SideAction,
    isRemindersMuted: Boolean,
    trashColor: Color,
    displayColor: Color
): Color {
    return when (action) {
        SideAction.TRASH -> trashColor
        SideAction.REWATCH -> displayColor
        SideAction.DROP -> Color.White.copy(alpha = 0.85f)
        SideAction.NOTIFICATIONS -> if (isRemindersMuted) Color.White.copy(alpha = 0.55f) else displayColor
    }
}

@Composable
private fun getSideActionLabel(
    action: SideAction,
    optimisticWatchState: WatchState,
    isRemindersMuted: Boolean
): String {
    return when (action) {
        SideAction.TRASH -> stringResource(R.string.card_remove)
        SideAction.REWATCH -> stringResource(R.string.watch_history_rewatch)
        SideAction.DROP -> stringResource(R.string.home_dropped)
        SideAction.NOTIFICATIONS -> if (isRemindersMuted) stringResource(R.string.action_unmute_reminders) else stringResource(R.string.action_mute_reminders)
    }
}

@Composable
private fun DetailSideDropIcon(
    isDropped: Boolean,
    scale: Float
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_drop_box),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
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
                        slideInVertically(
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f)
                        ) { it } + fadeIn(tween(160)) togetherWith
                            slideOutVertically(
                                animationSpec = tween(160)
                            ) { -it } + fadeOut(tween(120))
                    } else {
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
