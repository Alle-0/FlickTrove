package com.cinetrack.ui.components.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.viewmodel.WatchState
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Side action wheel and button for the Detail actions bar (Rewatch, Drop, Notifications, Trash).
 * Supports:
 * - Tap to execute current action
 * - Quick flick/swipe with animated tumbler roll between actions
 * - Long-press morphing scrubber: opens a vertical glass capsule with all available actions,
 *   allowing continuous scrubbing with haptic feedback on each item and smooth release-to-select.
 * - Perfectly placed vertical dots indicator nestled directly beside the circular button.
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

    val slotSpacing = 56.dp
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
        horizontalArrangement = Arrangement.Start,
        modifier = modifier.wrapContentSize()
    ) {
        // Floating label when scrubbing over an action (ordered naturally from top to bottom)
        val hoveredAction = when {
            !isExpanded -> null
            hoveredSlotIndex == 0 -> currentSideAction
            hoveredSlotIndex > 0 && hoveredSlotIndex <= otherActions.size -> {
                val actionIndex = otherActions.size - hoveredSlotIndex
                otherActions.getOrNull(actionIndex)
            }
            else -> null
        }

        // Anchor box that defines the fixed bottom alignment anchor (56x56dp).
        // Uses custom layout to strictly enforce a 56x56dp measurement to the parent layout,
        // preventing the expanding capsule from enlarging the parent and causing vertical centering drift.
        Box(
            modifier = Modifier
                .layout { measurable, _ ->
                    val sizePx = 56.dp.roundToPx()
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = sizePx,
                            maxWidth = sizePx,
                            minHeight = sizePx,
                            maxHeight = Constraints.Infinity
                        )
                    )
                    layout(sizePx, sizePx) {
                        placeable.place(0, 0)
                    }
                }
                .pointerInput(currentActions, currentSideAction, currentActionIndex, otherActions) {
                    coroutineScope {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startY = down.position.y
                            var isLongPressActive = false
                            var currentHover = 0
                            var hasSwipedActionFired = false
                            isPressedDown = true

                            // Standalone timer: triggers long-press even if the finger remains completely motionless!
                            val longPressJob = launch {
                                delay(200L)
                                if (!hasSwipedActionFired && isPressedDown && otherActions.isNotEmpty()) {
                                    isLongPressActive = true
                                    isExpanded = true
                                    currentHover = 0
                                    hoveredSlotIndex = 0
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }

                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                    val deltaY = pointer.position.y - startY

                                    if (!pointer.pressed) {
                                        pointer.consume()
                                        isPressedDown = false
                                        longPressJob.cancel()

                                        if (isLongPressActive) {
                                            // Released after scrubbing
                                            if (currentHover > 0 && currentHover <= otherActions.size) {
                                                val actionIndex = otherActions.size - currentHover
                                                val selectedAction = otherActions.getOrNull(actionIndex)
                                                if (selectedAction != null) {
                                                    val newIndex = currentActions.indexOf(selectedAction)
                                                    if (newIndex >= 0) {
                                                        onActionIndexChange(newIndex)
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                }
                                            }
                                            isExpanded = false
                                            hoveredSlotIndex = 0
                                        } else if (!hasSwipedActionFired) {
                                            // Quick tap with no swipe movement -> execute current action
                                            if (Math.abs(deltaY) < 25f) {
                                                onExecute(currentSideAction)
                                            }
                                        }
                                        break
                                    }

                                    // Immediate vertical swipe: fire action switch as soon as dragged > 35px before long-press!
                                    if (!isLongPressActive && !hasSwipedActionFired && Math.abs(deltaY) > 35f) {
                                        longPressJob.cancel()
                                        hasSwipedActionFired = true
                                        pointer.consume()
                                        if (deltaY < 0 && currentActionIndex < currentActions.size - 1) {
                                            onActionIndexChange(currentActionIndex + 1)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } else if (deltaY > 0 && currentActionIndex > 0) {
                                            onActionIndexChange(currentActionIndex - 1)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }

                                    if (isLongPressActive) {
                                        pointer.consume()
                                        val slotHeightPx = with(density) { slotSpacing.toPx() }
                                        val upwardDistance = -deltaY
                                        val targetHover = if (upwardDistance < slotHeightPx * 0.45f) {
                                            0
                                        } else {
                                            val step = ((upwardDistance - (slotHeightPx * 0.45f)) / slotHeightPx).toInt() + 1
                                            step.coerceIn(0, otherActions.size)
                                        }

                                        if (targetHover != currentHover) {
                                            currentHover = targetHover
                                            hoveredSlotIndex = targetHover
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }
                            } finally {
                                longPressJob.cancel()
                            }
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = sideButtonScale
                    scaleY = sideButtonScale
                },
            contentAlignment = Alignment.TopStart
        ) {
            // Morphing glass capsule strictly anchored at the resting button's bottom (y = 56.dp) and growing UPWARDS
            Box(
                modifier = Modifier
                    .offset(y = 56.dp - morphHeight)
                    .requiredSize(width = 56.dp, height = morphHeight)
                    .hazeGlass(
                        state = hazeState,
                        shape = RoundedCornerShape(28.dp),
                        borderColor = activeBorderColor.copy(alpha = 0.75f),
                        useRefraction = true
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Fluid gliding background circle that slides smoothly with spring and dynamically interpolates color
                val targetHighlightY = -(slotSpacing * hoveredSlotIndex) - 6.dp
                val animatedHighlightY by animateDpAsState(
                    targetValue = targetHighlightY,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "GlidingHighlightY"
                )

                val targetHighlightColor = remember(hoveredAction, isRemindersMuted, trashColor, displayColor) {
                    if (hoveredAction != null) {
                        getSideActionColor(hoveredAction, isRemindersMuted, trashColor, displayColor)
                    } else {
                        activeBorderColor
                    }
                }

                val animatedHighlightColor by animateColorAsState(
                    targetValue = targetHighlightColor,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "GlidingHighlightColor"
                )

                val highlightAlpha by animateFloatAsState(
                    targetValue = if (isExpanded) 0.28f else 0f,
                    animationSpec = tween(durationMillis = 160),
                    label = "GlidingHighlightAlpha"
                )

                if (highlightAlpha > 0.005f) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.BottomCenter)
                            .offset(y = animatedHighlightY)
                            .graphicsLayer { alpha = highlightAlpha }
                            .clip(CircleShape)
                            .background(animatedHighlightColor)
                    )
                }

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
                    // Fluid tumbler roll animation when switching between actions
                    AnimatedContent(
                        targetState = currentSideAction,
                        transitionSpec = {
                            val targetIdx = currentActions.indexOf(targetState)
                            val initialIdx = currentActions.indexOf(initialState)
                            if (targetIdx > initialIdx) {
                                slideInVertically { height -> height } + fadeIn(tween(180)) togetherWith
                                    slideOutVertically { height -> -height } + fadeOut(tween(140))
                            } else {
                                slideInVertically { height -> -height } + fadeIn(tween(180)) togetherWith
                                    slideOutVertically { height -> height } + fadeOut(tween(140))
                            }.using(SizeTransform(clip = false))
                        },
                        label = "SideActionAnimation"
                    ) { action ->
                        SideActionIconContent(
                            action = action,
                            optimisticWatchState = optimisticWatchState,
                            isRemindersMuted = isRemindersMuted,
                            trashColor = trashColor,
                            displayColor = displayColor
                        )
                    }
                }

                // Other actions stacked vertically above the base button in natural top-to-bottom order:
                // otherActions[0] at the top (highest slotIndex), flowing down towards the base button
                otherActions.forEachIndexed { index, action ->
                    val slotIndex = otherActions.size - index
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

        // Floating label badge positioned to the left of the currently hovered slot with glass blur
        if (isExpanded && hoveredAction != null) {
            val slotCenterY = 28.dp - (slotSpacing * hoveredSlotIndex)
            val animatedBadgeY by animateDpAsState(
                targetValue = slotCenterY - 18.dp,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "BadgeVerticalGlide"
            )

            val hoveredColor = remember(hoveredAction, isRemindersMuted, trashColor, displayColor) {
                getSideActionColor(hoveredAction, isRemindersMuted, trashColor, displayColor)
            }

            val animatedBadgeBorderColor by animateColorAsState(
                targetValue = hoveredColor.copy(alpha = 0.35f),
                animationSpec = tween(durationMillis = 200),
                label = "BadgeBorderColor"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-66).dp, y = animatedBadgeY)
                    .wrapContentSize(align = Alignment.CenterEnd, unbounded = true)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    .hazeGlass(
                        state = hazeState,
                        shape = RoundedCornerShape(14.dp),
                        containerColor = Color(0xD0121216),
                        borderColor = animatedBadgeBorderColor,
                        useRefraction = true
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                AnimatedContent(
                    targetState = getSideActionLabel(hoveredAction, optimisticWatchState, isRemindersMuted),
                    transitionSpec = {
                        (fadeIn(tween(160, easing = LinearOutSlowInEasing)) + scaleIn(initialScale = 0.94f))
                            .togetherWith(fadeOut(tween(100, easing = FastOutLinearInEasing)))
                    },
                    label = "BadgeTextTransition"
                ) { labelText ->
                    Text(
                        text = labelText,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }

    // Dedicated slot for vertical dots (always 11.dp wide so the button NEVER shifts horizontally)
        Box(
            modifier = Modifier.width(11.dp),
            contentAlignment = Alignment.Center
        ) {
            val showDots = optimisticWatchState != WatchState.NONE && availableSideActions.size > 1
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (showDots && !isExpanded) 1f else 0f,
                animationSpec = tween(durationMillis = 150),
                label = "IndicatorAlpha"
            )

            if (showDots) {
                val activeColor by animateColorAsState(
                    targetValue = when (currentSideAction) {
                        SideAction.TRASH -> trashColor
                        SideAction.REWATCH -> displayColor
                        SideAction.DROP -> Color.White.copy(alpha = 0.8f)
                        SideAction.NOTIFICATIONS -> if (isRemindersMuted) Color.White.copy(alpha = 0.6f) else displayColor
                    },
                    label = "activeColor"
                )

                SideActionVerticalDots(
                    currentPage = currentSideActionIndex,
                    pageCount = availableSideActions.size,
                    accentColor = activeColor,
                    modifier = Modifier
                        .graphicsLayer { alpha = indicatorAlpha }
                        .padding(start = 5.dp, end = 2.dp)
                )
            }
        }
    }
}

/**
 * Native vertical pager dots indicator nestled directly beside the circular button.
 * Eliminates the excessive ~26dp gap caused by rotating a horizontal layout canvas.
 */
@Composable
private fun SideActionVerticalDots(
    currentPage: Int,
    pageCount: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 4.dp,
    spacing: Dp = 4.dp
) {
    if (pageCount <= 1) return

    val totalHeight = dotSize * pageCount + spacing * (pageCount - 1)

    val animatedProgress by animateFloatAsState(
        targetValue = currentPage.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SideActionDotsProgress"
    )

    Box(
        modifier = modifier
            .width(dotSize)
            .height(totalHeight),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotRadius = dotSize.toPx() / 2f
            val distance = (dotSize + spacing).toPx()
            val centerX = size.width / 2f

            // Inactive background dots
            for (i in 0 until pageCount) {
                val cy = (i * distance) + dotRadius
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = dotRadius,
                    center = Offset(centerX, cy)
                )
            }

            // Active sliding dot with spring bounce
            val activeCy = (animatedProgress * distance) + dotRadius
            drawCircle(
                color = accentColor,
                radius = dotRadius,
                center = Offset(centerX, activeCy)
            )
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
