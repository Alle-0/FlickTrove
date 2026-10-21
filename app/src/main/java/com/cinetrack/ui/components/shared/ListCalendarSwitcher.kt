package com.cinetrack.ui.components.shared

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.ui.utils.ColorUtils
import kotlinx.coroutines.launch

/**
 * Reusable sliding capsule switcher between List/Timeline and Calendar views.
 *
 * Features:
 * - Fluid asymmetric spring physics (jelly stretch on leading/trailing edges).
 * - Interactive gesture drag with elastic edge squish and safe bounding clamp.
 * - Dual-layer clipPath icon masking (smooth morphing from muted white to contrast accent).
 */
@Composable
fun ListCalendarSwitcher(
    isCalendar: Boolean,
    onSelectMode: (isCalendar: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    listIconRes: Int = R.drawable.ic_lista,
    calendarIconRes: Int = R.drawable.ic_calendario,
    tabWidthDp: Dp = 34.dp,
    tabHeightDp: Dp = 30.dp,
    paddingDp: Dp = 3.dp
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val selectedIndex = if (!isCalendar) 0 else 1
    val primaryColor = MaterialTheme.colorScheme.primary
    val activeContentColor = ColorUtils.contentColorForAccent(primaryColor)

    val tabWidthPx = with(density) { tabWidthDp.toPx() }
    val tabHeightPx = with(density) { tabHeightDp.toPx() }
    val paddingPx = with(density) { paddingDp.toPx() }

    val normalWidthPx = tabWidthPx - (paddingPx * 2f)
    val pillHeightPx = tabHeightPx - (paddingPx * 2f)

    val initialLeft = selectedIndex * tabWidthPx + paddingPx
    val initialRight = (selectedIndex + 1) * tabWidthPx - paddingPx

    val animLeft = remember { Animatable(initialLeft) }
    val animRight = remember { Animatable(initialRight) }

    var isDragging by remember { mutableStateOf(false) }
    var previousIndex by remember { mutableStateOf(selectedIndex) }

    // Tab change animation: Asymmetric spring physics (Leading edge stretches, trailing edge catches up)
    LaunchedEffect(selectedIndex, tabWidthPx) {
        val targetLeft = selectedIndex * tabWidthPx + paddingPx
        val targetRight = (selectedIndex + 1) * tabWidthPx - paddingPx

        previousIndex = selectedIndex

        if (targetLeft > animLeft.value) {
            // Moving right: Right edge leads (fast spring), Left edge trails (softer spring) -> jelly stretch
            val jobRight = launch {
                animRight.animateTo(
                    targetValue = targetRight,
                    animationSpec = spring(stiffness = 650f, dampingRatio = 0.65f)
                )
            }
            val jobLeft = launch {
                animLeft.animateTo(
                    targetValue = targetLeft,
                    animationSpec = spring(stiffness = 450f, dampingRatio = 0.68f)
                )
            }
            jobRight.join()
            jobLeft.join()
        } else if (targetLeft < animLeft.value) {
            // Moving left: Left edge leads (fast spring), Right edge trails (softer spring) -> jelly stretch
            val jobLeft = launch {
                animLeft.animateTo(
                    targetValue = targetLeft,
                    animationSpec = spring(stiffness = 650f, dampingRatio = 0.65f)
                )
            }
            val jobRight = launch {
                animRight.animateTo(
                    targetValue = targetRight,
                    animationSpec = spring(stiffness = 450f, dampingRatio = 0.68f)
                )
            }
            jobLeft.join()
            jobRight.join()
        }
    }

    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "dragScale"
    )

    val pillPath = remember { Path() }

    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.15f)
        ),
        modifier = modifier.clip(CircleShape)
    ) {
        Box(
            modifier = Modifier
                .width(tabWidthDp * 2)
                .height(tabHeightDp)
                .clip(CircleShape)
                .pointerInput(selectedIndex, tabWidthPx) {
                    var isDragActiveOnPill = false
                    val touchMargin = 6.dp.toPx()
                    var dragTargetLeft = 0f
                    var dragTargetRight = 0f

                    detectDragGestures(
                        onDragStart = { startOffset ->
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

                            val currentCenter = (animLeft.value + animRight.value) / 2f
                            val targetIndex = (currentCenter / tabWidthPx).toInt().coerceIn(0, 1)

                            if (targetIndex != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectMode(targetIndex == 1)
                            } else {
                                val targetLeft = selectedIndex * tabWidthPx + paddingPx
                                val targetRight = (selectedIndex + 1) * tabWidthPx - paddingPx
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
                            val targetLeft = selectedIndex * tabWidthPx + paddingPx
                            val targetRight = (selectedIndex + 1) * tabWidthPx - paddingPx
                            coroutineScope.launch {
                                launch { animLeft.animateTo(targetLeft, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                launch { animRight.animateTo(targetRight, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                            }
                        }
                    ) { change, dragAmount ->
                        if (!isDragActiveOnPill) return@detectDragGestures
                        change.consume()

                        val dragX = dragAmount.x
                        dragTargetLeft += dragX
                        dragTargetRight += dragX

                        coroutineScope.launch {
                            val maxRight = 2f * tabWidthPx - paddingPx
                            val maxLeft = maxRight - normalWidthPx

                            var targetLeft = dragTargetLeft
                            var targetRight = dragTargetRight

                            // Soft elastic edge squish within safe bounds
                            if (dragTargetLeft < paddingPx) {
                                val overscroll = (paddingPx - dragTargetLeft).coerceAtMost(normalWidthPx * 0.4f)
                                targetLeft = paddingPx - (overscroll * 0.15f)
                                targetRight = (paddingPx + normalWidthPx) - overscroll * 0.1f
                            } else if (dragTargetRight > maxRight) {
                                val overscroll = (dragTargetRight - maxRight).coerceAtMost(normalWidthPx * 0.4f)
                                targetRight = maxRight + (overscroll * 0.15f)
                                targetLeft = maxLeft + overscroll * 0.1f
                            }

                            val leftStiffness = if (dragX > 0) 300f else 750f
                            val rightStiffness = if (dragX > 0) 750f else 300f

                            launch { animLeft.animateTo(targetLeft, spring(stiffness = leftStiffness, dampingRatio = 0.65f)) }
                            launch { animRight.animateTo(targetRight, spring(stiffness = rightStiffness, dampingRatio = 0.65f)) }
                        }
                    }
                }
        ) {
            // 1. Base Inactive Layer: Icons in soft muted white
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: List / Timeline
                Box(
                    modifier = Modifier
                        .width(tabWidthDp)
                        .height(tabHeightDp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (selectedIndex != 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectMode(false)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = listIconRes),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Tab 1: Calendar
                Box(
                    modifier = Modifier
                        .width(tabWidthDp)
                        .height(tabHeightDp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (selectedIndex != 1) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectMode(true)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = calendarIconRes),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 2. Sliding Indicator Pill + 3. Active Masked Layer (Dual-layer clipPath morphing)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val currentLeft = animLeft.value
                        val currentRight = animRight.value
                        val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                        val centerXPx = (currentLeft + currentRight) / 2f

                        val scaledWidth = (currentWidth * dragScale).coerceAtLeast(1f)
                        val maxAllowedWidth = (size.width - paddingPx * 2f).coerceAtLeast(1f)
                        val clampedWidth = scaledWidth.coerceAtMost(maxAllowedWidth)

                        val pillLeft = (centerXPx - (clampedWidth / 2f)).coerceIn(paddingPx, size.width - paddingPx - clampedWidth)
                        val pillTop = paddingPx
                        val cornerRadius = pillHeightPx / 2f

                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(pillLeft, pillTop),
                            size = Size(clampedWidth, pillHeightPx),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )
                    }
                    .drawWithContent {
                        val currentLeft = animLeft.value
                        val currentRight = animRight.value
                        val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                        val centerXPx = (currentLeft + currentRight) / 2f

                        val scaledWidth = (currentWidth * dragScale).coerceAtLeast(1f)
                        val maxAllowedWidth = (size.width - paddingPx * 2f).coerceAtLeast(1f)
                        val clampedWidth = scaledWidth.coerceAtMost(maxAllowedWidth)

                        val pillLeft = (centerXPx - (clampedWidth / 2f)).coerceIn(paddingPx, size.width - paddingPx - clampedWidth)
                        val pillTop = paddingPx
                        val cornerRadius = pillHeightPx / 2f

                        pillPath.reset()
                        pillPath.addRoundRect(
                            RoundRect(
                                left = pillLeft,
                                top = pillTop,
                                right = pillLeft + clampedWidth,
                                bottom = pillTop + pillHeightPx,
                                radiusX = cornerRadius,
                                radiusY = cornerRadius
                            )
                        )

                        // Mask: Only the portion of the icon under the indicator pill is rendered in active color!
                        clipPath(pillPath) {
                            this@drawWithContent.drawContent()
                        }
                    }
            ) {
                // Active Icons Layer: Rendered in active high-contrast color (masked by clipPath)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(tabWidthDp)
                            .height(tabHeightDp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = listIconRes),
                            contentDescription = null,
                            tint = activeContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(tabWidthDp)
                            .height(tabHeightDp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = calendarIconRes),
                            contentDescription = null,
                            tint = activeContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
