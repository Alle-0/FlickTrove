package com.cinetrack.ui.components.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.LocalAdvancedVisualEffects
import com.cinetrack.util.VibrationHelper
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// ─── Segmented Option Model & Items (Layerable for masking) ───────────────────

data class SegmentedOption<T>(
    val value: T,
    val title: String,
    val subtitle: String? = null
)

@Composable
internal fun <T> SegmentedTabItems(
    options: List<SegmentedOption<T>>,
    titleColor: Color,
    subtitleColor: Color,
    isInteractive: Boolean,
    pressedTabIndex: Int,
    onTabPressed: ((Int, Boolean) -> Unit)? = null,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
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
                label = "segTabBounceScale_$index"
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
                                onSelect(option.value)
                            }
                        } else Modifier
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer {
                        scaleX = tabBounceScale
                        scaleY = tabBounceScale
                    }
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = titleColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    if (!option.subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = subtitleColor,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ─── Segmented Control (FlickTrove Tab Physics + Vanishing unselected indicator)

@Composable
fun <T> SegmentedControl(
    options: List<SegmentedOption<T>>,
    selected: T,
    accentColor: Color,
    vibrationEnabled: Boolean,
    context: android.content.Context,
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
    enabled: Boolean = true,
    isPill: Boolean = true,
    onSelect: (T) -> Unit
) {
    val selectedIndex = options.indexOfFirst { it.value == selected }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val advancedEffectsEnabled = LocalAdvancedVisualEffects.current
    val haptic = LocalHapticFeedback.current
    val isCompact = height <= 38.dp
    val padding = 2.5.dp
    val containerCornerRadius = if (isPill) height / 2 else if (isCompact) 12.dp else 14.dp

    val controlAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.40f,
        animationSpec = tween(200),
        label = "segControlAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { alpha = controlAlpha }
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(containerCornerRadius))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(containerCornerRadius))
    ) {
        val usableWidthPx = constraints.maxWidth.toFloat()
        val segmentCount = options.size
        val realTabWidthPx = if (segmentCount > 0) usableWidthPx / segmentCount else 1f
        val paddingPx = with(density) { padding.toPx() }
        val normalWidthPx = (realTabWidthPx - 2 * paddingPx).coerceAtLeast(1f)

        val initialLeft = if (selectedIndex >= 0) selectedIndex * realTabWidthPx + paddingPx else -realTabWidthPx
        val initialRight = if (selectedIndex >= 0) (selectedIndex + 1) * realTabWidthPx - paddingPx else 0f

        val animLeft = remember { Animatable(initialLeft) }
        val animRight = remember { Animatable(initialRight) }

        var isSelectedTabPressed by remember { mutableStateOf(false) }
        var isDragging by remember { mutableStateOf(false) }
        var previousIndex by remember { mutableStateOf(selectedIndex) }
        var pressedTabIndex by remember { mutableStateOf(-1) }

        val indicatorAlpha by animateFloatAsState(
            targetValue = if (selectedIndex >= 0) 1f else 0f,
            animationSpec = tween(150),
            label = "segIndicatorAlpha"
        )

        LaunchedEffect(selectedIndex, realTabWidthPx, segmentCount, advancedEffectsEnabled) {
            if (selectedIndex < 0) {
                previousIndex = selectedIndex
                return@LaunchedEffect
            }
            val targetLeft = selectedIndex * realTabWidthPx + paddingPx
            val targetRight = (selectedIndex + 1) * realTabWidthPx - paddingPx

            val wasPreviouslyUnselected = previousIndex < 0
            val isTabChangedByClick = previousIndex != selectedIndex && !isDragging && !wasPreviouslyUnselected
            val indexDistance = if (wasPreviouslyUnselected) 1 else kotlin.math.abs(selectedIndex - previousIndex).coerceAtLeast(1)
            previousIndex = selectedIndex

            if (wasPreviouslyUnselected) {
                animLeft.snapTo(targetLeft)
                animRight.snapTo(targetRight)
                return@LaunchedEffect
            }

            if (!advancedEffectsEnabled) {
                launch { animLeft.snapTo(targetLeft) }
                launch { animRight.snapTo(targetRight) }
                return@LaunchedEffect
            }

            if (targetLeft > animLeft.value) {
                val jobRight = launch {
                    animRight.animateTo(
                        targetValue = targetRight,
                        animationSpec = spring(stiffness = 650f, dampingRatio = 0.64f, visibilityThreshold = 0.5f)
                    )
                }
                val jobLeft = launch {
                    animLeft.animateTo(
                        targetValue = targetLeft,
                        animationSpec = spring(stiffness = 480f, dampingRatio = 0.68f, visibilityThreshold = 0.5f)
                    )
                }
                jobRight.join()
                jobLeft.join()
            } else if (targetLeft < animLeft.value) {
                val jobLeft = launch {
                    animLeft.animateTo(
                        targetValue = targetLeft,
                        animationSpec = spring(stiffness = 650f, dampingRatio = 0.64f, visibilityThreshold = 0.5f)
                    )
                }
                val jobRight = launch {
                    animRight.animateTo(
                        targetValue = targetRight,
                        animationSpec = spring(stiffness = 480f, dampingRatio = 0.68f, visibilityThreshold = 0.5f)
                    )
                }
                jobLeft.join()
                jobRight.join()
            }
        }

        val dragScale by animateFloatAsState(
            targetValue = if (!advancedEffectsEnabled) 1f
                else if (isDragging) 1.15f
                else if (isSelectedTabPressed) 1.06f
                else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "segDragScale"
        )

        val pillPath = remember { Path() }
        val cornerRadiusPx = with(density) {
            if (isPill) (height / 2f - padding).coerceAtLeast(0.dp).toPx()
            else (if (isCompact) 9.5.dp else 11.5.dp).toPx()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedIndex, realTabWidthPx, enabled) {
                    if (!enabled) return@pointerInput
                    var isDragActiveOnPill = false
                    val touchMargin = 8.dp.toPx()
                    var dragTargetLeft = 0f
                    var dragTargetRight = 0f

                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (selectedIndex < 0) return@detectDragGestures
                            val currentLeft = animLeft.value
                            val currentRight = animRight.value
                            if (startOffset.x in (currentLeft - touchMargin)..(currentRight + touchMargin)) {
                                isDragActiveOnPill = true
                                isDragging = true
                                dragTargetLeft = currentLeft
                                dragTargetRight = currentRight
                                coroutineScope.launch {
                                    launch { animLeft.stop() }
                                    launch { animRight.stop() }
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

                            if (targetIndex != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                onSelect(options[targetIndex].value)
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
                                launch { animLeft.animateTo(targetLeft, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                launch { animRight.animateTo(targetRight, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                            }
                        }
                    ) { change, dragAmount ->
                        if (!isDragActiveOnPill) return@detectDragGestures
                        isDragging = true
                        change.consume()

                        val dragX = dragAmount.x
                        dragTargetLeft += dragX
                        dragTargetRight += dragX

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

                            launch { animLeft.animateTo(targetLeft, spring(stiffness = leftStiffness, dampingRatio = 0.65f)) }
                            launch { animRight.animateTo(targetRight, spring(stiffness = rightStiffness, dampingRatio = 0.65f)) }
                        }
                    }
                }
        ) {
            val divHeight = with(density) { 
                if (options.any { !it.subtitle.isNullOrBlank() }) 19.dp.toPx() 
                else if (isCompact) 13.dp.toPx() 
                else 16.dp.toPx() 
            }
            val currentLeft = animLeft.value
            val currentRight = animRight.value

            val updatePillPath: (Path, Float) -> Unit = { path, h ->
                val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                val centerXPx = (currentLeft + currentRight) / 2f
                val centerYPx = h / 2f

                val baseHeight = (h - (paddingPx * 2f)).coerceAtLeast(1f)
                val finalPillWidth = (currentWidth * dragScale).coerceAtLeast(1f)
                val finalPillHeight = (baseHeight * dragScale).coerceAtLeast(1f)
                val pillLeft = centerXPx - (finalPillWidth / 2f)
                val pillTop = centerYPx - (finalPillHeight / 2f)
                val pillRadius = if (isPill) finalPillHeight / 2f else cornerRadiusPx * dragScale

                path.reset()
                path.addRoundRect(
                    RoundRect(
                        left = pillLeft,
                        top = pillTop,
                        right = pillLeft + finalPillWidth,
                        bottom = pillTop + finalPillHeight,
                        radiusX = pillRadius,
                        radiusY = pillRadius
                    )
                )
            }

            // Inactive Base Layer (Muted greyish text, strictly clipped OUTSIDE the active pill via ClipOp.Difference)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        if (indicatorAlpha > 0.01f) {
                            updatePillPath(pillPath, size.height)
                            clipPath(pillPath, clipOp = ClipOp.Difference) {
                                this@drawWithContent.drawContent()
                            }
                        } else {
                            this@drawWithContent.drawContent()
                        }
                    }
            ) {
                SegmentedTabItems(
                    options = options,
                    titleColor = Color.White.copy(alpha = 0.75f),
                    subtitleColor = Color.White.copy(alpha = 0.40f),
                    isInteractive = enabled,
                    pressedTabIndex = pressedTabIndex,
                    onTabPressed = { index, pressed ->
                        if (enabled) {
                            pressedTabIndex = if (pressed) index else -1
                            if (index == selectedIndex) isSelectedTabPressed = pressed
                        }
                    },
                    onSelect = { value ->
                        if (enabled) {
                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                            onSelect(value)
                        }
                    }
                )
            }

            // Active Masked Layer + Pill Background & Inset Border (Rendered on top, strictly INSIDE the pill via ClipOp.Intersect)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        // Dividers
                        for (k in 1 until segmentCount) {
                            val boundaryX = k * realTabWidthPx
                            val isPillNearBoundary = if (indicatorAlpha > 0.01f) {
                                boundaryX in (currentLeft - 4.dp.toPx())..(currentRight + 4.dp.toPx())
                            } else false

                            if (!isPillNearBoundary) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.08f),
                                    start = Offset(boundaryX, (size.height - divHeight) / 2f),
                                    end = Offset(boundaryX, (size.height + divHeight) / 2f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }

                        // Sliding Translucent Pill + Inset Border
                        if (indicatorAlpha > 0.01f) {
                            updatePillPath(pillPath, size.height)

                            // Background fill
                            drawPath(
                                path = pillPath,
                                color = accentColor.copy(alpha = 0.14f * indicatorAlpha)
                            )

                            // Inset border stroke so it never clips at edges
                            val strokeWidthPx = 1.5.dp.toPx()
                            drawPath(
                                path = pillPath,
                                color = accentColor.copy(alpha = indicatorAlpha),
                                style = Stroke(width = strokeWidthPx)
                            )
                        }
                    }
                    .drawWithContent {
                        if (indicatorAlpha > 0.01f) {
                            updatePillPath(pillPath, size.height)
                            clipPath(pillPath, clipOp = ClipOp.Intersect) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    }
            ) {
                // Active Masked Layer (Accent color, clipped strictly under the pill capsule)
                if (indicatorAlpha > 0.01f) {
                    SegmentedTabItems(
                        options = options,
                        titleColor = accentColor,
                        subtitleColor = accentColor.copy(alpha = 0.85f),
                        isInteractive = false,
                        pressedTabIndex = pressedTabIndex,
                        onSelect = {}
                    )
                }
            }
        }
    }
}
