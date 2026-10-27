package com.cinetrack.ui.components.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.LocalAdvancedVisualEffects
import com.cinetrack.ui.components.card.PreviewCardMode
import com.cinetrack.util.VibrationHelper
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// ─── Preview Mode Tab Items (Layerable for masking) ──────────────────────────

@Composable
fun PreviewModeTabItems(
    modes: List<Triple<PreviewCardMode, String, Int>>,
    contentColor: Color,
    isInteractive: Boolean,
    pressedTabIndex: Int,
    onTabPressed: ((Int, Boolean) -> Unit)? = null,
    onSelect: (PreviewCardMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEachIndexed { index, (mode, label, iconRes) ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            if (isInteractive && onTabPressed != null) {
                DisposableEffect(isPressed) {
                    onTabPressed(index, isPressed)
                    onDispose { onTabPressed(index, false) }
                }
            }

            val isCurrentPressed = (isInteractive && isPressed) || (pressedTabIndex == index)
            val tabBounceScale by animateFloatAsState(
                targetValue = if (isCurrentPressed) 0.96f else 1f,
                animationSpec = spring(
                    stiffness = if (isCurrentPressed) 10000f else Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                label = "previewTabBounceScale_$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isInteractive) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onSelect(mode)
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer {
                        scaleX = tabBounceScale
                        scaleY = tabBounceScale
                    }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = iconRes),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─── Draggable Preview Mode Selector (FlickTrove Tab Physics) ────────────────

@Composable
fun DraggablePreviewModeSelector(
    modes: List<Triple<PreviewCardMode, String, Int>>,
    selectedMode: PreviewCardMode,
    accentColor: Color,
    vibrationEnabled: Boolean,
    context: android.content.Context,
    modifier: Modifier = Modifier,
    onSelectMode: (PreviewCardMode) -> Unit
) {
    val selectedIndex = modes.indexOfFirst { it.first == selectedMode }.coerceAtLeast(0)
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val advancedEffectsEnabled = LocalAdvancedVisualEffects.current
    val haptic = LocalHapticFeedback.current

    val padding = 2.dp

    BoxWithConstraints(
        modifier = modifier
            .height(32.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        val usableWidthPx = constraints.maxWidth.toFloat()
        val segmentCount = modes.size
        val realTabWidthPx = if (segmentCount > 0) usableWidthPx / segmentCount else 1f
        val paddingPx = with(density) { padding.toPx() }
        val normalWidthPx = (realTabWidthPx - 2 * paddingPx).coerceAtLeast(1f)

        val animLeft = remember { Animatable(selectedIndex * realTabWidthPx + paddingPx) }
        val animRight = remember { Animatable((selectedIndex + 1) * realTabWidthPx - paddingPx) }
        val animDragY = remember { Animatable(0f) }
        val flightDynamics = remember { Animatable(0f) }

        var isSelectedTabPressed by remember { mutableStateOf(false) }
        var isDragging by remember { mutableStateOf(false) }
        var previousIndex by remember { mutableStateOf(selectedIndex) }
        var pressedTabIndex by remember { mutableStateOf(-1) }

        LaunchedEffect(selectedIndex, realTabWidthPx, segmentCount, advancedEffectsEnabled) {
            val targetLeft = selectedIndex * realTabWidthPx + paddingPx
            val targetRight = (selectedIndex + 1) * realTabWidthPx - paddingPx

            val isTabChangedByClick = previousIndex != selectedIndex && !isDragging
            val indexDistance = kotlin.math.abs(selectedIndex - previousIndex).coerceAtLeast(1)
            previousIndex = selectedIndex

            if (!advancedEffectsEnabled) {
                launch { animLeft.snapTo(targetLeft) }
                launch { animRight.snapTo(targetRight) }
                return@LaunchedEffect
            }

            if (isTabChangedByClick) {
                val riseDuration = if (indexDistance > 1) 110 else 85
                val peakBoost = (0.24f + (indexDistance - 1) * 0.05f).coerceAtMost(0.34f)
                launch {
                    flightDynamics.snapTo(0f)
                    flightDynamics.animateTo(peakBoost, tween(riseDuration, easing = FastOutSlowInEasing))
                    flightDynamics.animateTo(
                        0f,
                        spring(
                            dampingRatio = 0.50f,
                            stiffness = 300f
                        )
                    )
                }
            }

            if (targetLeft > animLeft.value) {
                val jobRight = launch {
                    animRight.animateTo(
                        targetValue = targetRight,
                        animationSpec = spring(
                            stiffness = 650f,
                            dampingRatio = 0.64f,
                            visibilityThreshold = 0.5f
                        )
                    )
                }
                val jobLeft = launch {
                    animLeft.animateTo(
                        targetValue = targetLeft,
                        animationSpec = spring(
                            stiffness = 480f,
                            dampingRatio = 0.68f,
                            visibilityThreshold = 0.5f
                        )
                    )
                }
                jobRight.join()
                jobLeft.join()
            } else if (targetLeft < animLeft.value) {
                val jobLeft = launch {
                    animLeft.animateTo(
                        targetValue = targetLeft,
                        animationSpec = spring(
                            stiffness = 650f,
                            dampingRatio = 0.64f,
                            visibilityThreshold = 0.5f
                        )
                    )
                }
                val jobRight = launch {
                    animRight.animateTo(
                        targetValue = targetRight,
                        animationSpec = spring(
                            stiffness = 480f,
                            dampingRatio = 0.68f,
                            visibilityThreshold = 0.5f
                        )
                    )
                }
                jobRight.join()
                jobLeft.join()
            }
        }

        val dragScaleY by animateFloatAsState(
            targetValue = if (!advancedEffectsEnabled) 1f
                else if (isDragging) 1.25f
                else if (isSelectedTabPressed) 1.07f
                else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "previewDragScaleY"
        )
        val dragScaleX by animateFloatAsState(
            targetValue = if (!advancedEffectsEnabled) 1f
                else if (isDragging) 1.12f
                else if (isSelectedTabPressed) 1.04f
                else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "previewDragScaleX"
        )

        val pillPath = remember { Path() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedIndex, realTabWidthPx) {
                    var isDragActiveOnPill = false
                    val touchMargin = 8.dp.toPx()
                    var dragTargetLeft = 0f
                    var dragTargetRight = 0f
                    var dragTargetY = 0f

                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val currentLeft = animLeft.value
                            val currentRight = animRight.value
                            if (startOffset.x in (currentLeft - touchMargin)..(currentRight + touchMargin)) {
                                isDragActiveOnPill = true
                                isDragging = true
                                dragTargetLeft = currentLeft
                                dragTargetRight = currentRight
                                dragTargetY = 0f
                                coroutineScope.launch {
                                    launch { animLeft.stop() }
                                    launch { animRight.stop() }
                                    launch { animDragY.stop() }
                                }
                            } else {
                                isDragActiveOnPill = false
                            }
                        },
                        onDragEnd = {
                            if (!isDragActiveOnPill) return@detectDragGestures
                            isDragActiveOnPill = false
                            isDragging = false
                            val currentLeft = animLeft.value - paddingPx
                            val targetIndex = (currentLeft / realTabWidthPx).roundToInt().coerceIn(0, segmentCount - 1)

                            coroutineScope.launch {
                                launch { animDragY.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.6f)) }
                            }

                            if (targetIndex != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                onSelectMode(modes[targetIndex].first)
                            } else {
                                val targetLeft = selectedIndex * realTabWidthPx + paddingPx
                                val targetRight = (selectedIndex + 1) * realTabWidthPx - paddingPx
                                coroutineScope.launch {
                                    launch { animLeft.animateTo(targetLeft, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                    launch { animRight.animateTo(targetRight, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                }
                            }
                        },
                        onDragCancel = {
                            if (!isDragActiveOnPill) return@detectDragGestures
                            isDragActiveOnPill = false
                            isDragging = false
                            val targetLeft = selectedIndex * realTabWidthPx + paddingPx
                            val targetRight = (selectedIndex + 1) * realTabWidthPx - paddingPx
                            coroutineScope.launch {
                                launch { animDragY.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.6f)) }
                                launch { animLeft.animateTo(targetLeft, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                launch { animRight.animateTo(targetRight, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                            }
                        }
                    ) { change, dragAmount ->
                        if (!isDragActiveOnPill) return@detectDragGestures
                        isDragging = true
                        change.consume()

                        val dragX = dragAmount.x
                        val dragYDelta = dragAmount.y

                        dragTargetLeft += dragX
                        dragTargetRight += dragX
                        dragTargetY += dragYDelta

                        coroutineScope.launch {
                            val maxRight = segmentCount * realTabWidthPx - paddingPx
                            val maxLeft = maxRight - normalWidthPx

                            var targetLeft = dragTargetLeft
                            var targetRight = dragTargetRight

                            val maxOverscroll = normalWidthPx * 0.8f
                            if (dragTargetLeft < paddingPx) {
                                val overscroll = (paddingPx - dragTargetLeft).coerceAtMost(maxOverscroll)
                                val maxPenetration = paddingPx * 1.5f
                                val penetration = maxPenetration * (1f - kotlin.math.exp(-overscroll / 100f))
                                targetLeft = paddingPx - penetration
                                targetRight = (paddingPx + normalWidthPx) - overscroll * 0.15f
                            } else if (dragTargetRight > maxRight) {
                                val overscroll = (dragTargetRight - maxRight).coerceAtMost(maxOverscroll)
                                val maxPenetration = paddingPx * 1.5f
                                val penetration = maxPenetration * (1f - kotlin.math.exp(-overscroll / 100f))
                                targetRight = maxRight + penetration
                                targetLeft = maxLeft + overscroll * 0.15f
                            }

                            val leftStiffness = if (dragX > 0) 250f else if (dragX < 0) 800f else 600f
                            val rightStiffness = if (dragX > 0) 800f else if (dragX < 0) 250f else 600f

                            launch { animDragY.animateTo(dragTargetY, spring(stiffness = 1000f, dampingRatio = 0.7f)) }
                            launch { animLeft.animateTo(targetLeft, spring(stiffness = leftStiffness, dampingRatio = 0.65f)) }
                            launch { animRight.animateTo(targetRight, spring(stiffness = rightStiffness, dampingRatio = 0.65f)) }
                        }
                    }
                }
        ) {
            val divHeight = with(density) { 13.dp.toPx() }
            val currentLeft = animLeft.value
            val currentRight = animRight.value

            // 1. Inactive Base Layer (Muted white text, always visible beneath)
            PreviewModeTabItems(
                modes = modes,
                contentColor = Color.White.copy(alpha = 0.65f),
                isInteractive = true,
                pressedTabIndex = pressedTabIndex,
                onTabPressed = { index, pressed ->
                    pressedTabIndex = if (pressed) index else -1
                    if (index == selectedIndex) isSelectedTabPressed = pressed
                },
                onSelect = { mode ->
                    if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                    onSelectMode(mode)
                }
            )

            // 2. Sliding Solid Pill + Active Masked Layer (Black text over solid accentColor, rendered on top)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        // Dividers
                        for (k in 1 until segmentCount) {
                            val boundaryX = k * realTabWidthPx
                            val isPillNearBoundary = boundaryX in (currentLeft - 4.dp.toPx())..(currentRight + 4.dp.toPx())
                            if (!isPillNearBoundary) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.08f),
                                    start = Offset(boundaryX, (size.height - divHeight) / 2f),
                                    end = Offset(boundaryX, (size.height + divHeight) / 2f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }

                        // Sliding Solid Pill
                        val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                        val centerXPx = (currentLeft + currentRight) / 2f

                        val flightOffset = flightDynamics.value
                        val totalScaleY = if (advancedEffectsEnabled) dragScaleY * (1f + flightOffset) else 1f
                        val totalScaleX = if (advancedEffectsEnabled) dragScaleX * (1f + kotlin.math.min(0f, flightOffset * 0.6f)) else 1f

                        val baseHeight = size.height - (paddingPx * 2f)
                        val pillHeight = (baseHeight * totalScaleY).coerceAtLeast(1f)
                        val finalPillWidth = (currentWidth * totalScaleX).coerceAtLeast(1f)
                        val cornerRadius = pillHeight / 2f

                        val rawDragY = animDragY.value
                        val maxVisualDrag = paddingPx * 0.75f
                        val dragYOffset = if (rawDragY > 0) {
                            maxVisualDrag * (1f - kotlin.math.exp(-rawDragY / 80f))
                        } else if (rawDragY < 0) {
                            -maxVisualDrag * (1f - kotlin.math.exp(rawDragY / 80f))
                        } else 0f

                        val pillCenterY = (size.height / 2f) + dragYOffset
                        val pillTop = pillCenterY - (pillHeight / 2f)
                        val pillLeft = centerXPx - (finalPillWidth / 2f)

                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(pillLeft, pillTop),
                            size = Size(finalPillWidth, pillHeight),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )
                    }
                    .drawWithContent {
                        val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                        val centerXPx = (currentLeft + currentRight) / 2f

                        val flightOffset = flightDynamics.value
                        val totalScaleY = if (advancedEffectsEnabled) dragScaleY * (1f + flightOffset) else 1f
                        val totalScaleX = if (advancedEffectsEnabled) dragScaleX * (1f + kotlin.math.min(0f, flightOffset * 0.6f)) else 1f

                        val baseHeight = size.height - (paddingPx * 2f)
                        val pillHeight = (baseHeight * totalScaleY).coerceAtLeast(1f)
                        val finalPillWidth = (currentWidth * totalScaleX).coerceAtLeast(1f)
                        val cornerRadius = pillHeight / 2f

                        val rawDragY = animDragY.value
                        val maxVisualDrag = paddingPx * 0.75f
                        val dragYOffset = if (rawDragY > 0) {
                            maxVisualDrag * (1f - kotlin.math.exp(-rawDragY / 80f))
                        } else if (rawDragY < 0) {
                            -maxVisualDrag * (1f - kotlin.math.exp(rawDragY / 80f))
                        } else 0f

                        val pillCenterY = (size.height / 2f) + dragYOffset
                        val pillTop = pillCenterY - (pillHeight / 2f)
                        val pillLeft = centerXPx - (finalPillWidth / 2f)

                        pillPath.reset()
                        pillPath.addRoundRect(
                            RoundRect(
                                left = pillLeft,
                                top = pillTop,
                                right = pillLeft + finalPillWidth,
                                bottom = pillTop + pillHeight,
                                radiusX = cornerRadius,
                                radiusY = cornerRadius
                            )
                        )

                        clipPath(pillPath) {
                            this@drawWithContent.drawContent()
                        }
                    }
            ) {
                // Active Masked Layer (Black text over solid accentColor)
                PreviewModeTabItems(
                    modes = modes,
                    contentColor = Color.Black.copy(alpha = 0.9f),
                    isInteractive = false,
                    pressedTabIndex = -1,
                    onSelect = {}
                )
            }
        }
    }
}
