package com.cinetrack.ui.components.shared

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinetrack.LocalAdvancedVisualEffects
import com.cinetrack.R
import com.cinetrack.ui.utils.ColorUtils
import kotlinx.coroutines.launch

/**
 * Reusable sliding capsule switcher between List/Timeline and Calendar views.
 *
 * Implements the EXACT physics of FlickTrove's CategoryTabSelector (Movies / TV):
 * - Apple-style asymmetric spring physics (jelly stretch on leading/trailing edges).
 * - Unified inertial flight oscillator (jump & bounce landing on tap).
 * - Separate dynamic scaling along X and Y axes (squash & stretch on drag & press).
 * - 2D elastic drag with vertical spring resistance.
 * - Non-clipping edge cushion with exponential penetration bounding.
 * - Dual-layer clipPath icon masking (smooth morphing from muted white to contrast accent).
 */
@Composable
fun ListCalendarSwitcher(
    isCalendar: Boolean,
    onSelectMode: (isCalendar: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    listIconRes: Int = R.drawable.ic_lista,
    calendarIconRes: Int = R.drawable.ic_calendario,
    tabWidthDp: Dp = 32.dp,
    tabHeightDp: Dp = 32.dp,
    paddingDp: Dp = 3.dp,
    iconSizeDp: Dp = 16.dp,
    trackColor: Color = Color.White.copy(alpha = 0.08f),
    borderColor: Color? = Color.White.copy(alpha = 0.15f),
    inactiveIconTint: Color = Color.White.copy(alpha = 0.55f)
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

    val normalWidthPx = (tabWidthPx - 2 * paddingPx).coerceAtLeast(1f)

    val animLeft = remember { Animatable(selectedIndex * tabWidthPx + paddingPx) }
    val animRight = remember { Animatable((selectedIndex + 1) * tabWidthPx - paddingPx) }
    val animDragY = remember { Animatable(0f) }
    val flightDynamics = remember { Animatable(0f) }

    var isSelectedTabPressed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }

    val advancedEffectsEnabled = LocalAdvancedVisualEffects.current

    // Tab change animation: Asymmetric spring physics (Leading edge stretches, trailing edge catches up)
    LaunchedEffect(selectedIndex, tabWidthPx, advancedEffectsEnabled) {
        val targetLeft = selectedIndex * tabWidthPx + paddingPx
        val targetRight = (selectedIndex + 1) * tabWidthPx - paddingPx

        val isTabChangedByClick = previousIndex != selectedIndex && !isDragging
        previousIndex = selectedIndex

        if (!advancedEffectsEnabled) {
            launch { animLeft.snapTo(targetLeft) }
            launch { animRight.snapTo(targetRight) }
            return@LaunchedEffect
        }

        // Unified inertial flight oscillator: jumps up in scale, reaches target, and bounces softly into place
        if (isTabChangedByClick) {
            val riseDuration = 85
            val peakBoost = 0.24f
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
            // Moving right: Right edge leads (fast spring), Left edge trails (softer spring) -> stretches horizontally
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
            // Moving left: Left edge leads (fast spring), Right edge trails (softer spring) -> stretches horizontally
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

    // Spring animation for scaling the indicator when held/pressed or dragged (Identical to CategoryTabSelector)
    val dragScaleY by animateFloatAsState(
        targetValue = if (!advancedEffectsEnabled) 1f
            else if (isDragging) 1.25f
            else if (isSelectedTabPressed) 1.07f
            else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dragScaleY"
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
        label = "dragScaleX"
    )

    val pillPath = remember { Path() }

    Surface(
        shape = CircleShape,
        color = trackColor,
        border = if (borderColor != null) androidx.compose.foundation.BorderStroke(
            1.dp,
            borderColor
        ) else null,
        modifier = modifier
            .size(width = tabWidthDp * 2, height = tabHeightDp)
    ) {
        Box(
            modifier = Modifier
                .width(tabWidthDp * 2)
                .height(tabHeightDp)
                .pointerInput(selectedIndex, tabWidthPx) {
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
                            val targetIndex = (currentLeft / tabWidthPx).toInt().coerceIn(0, 1)

                            coroutineScope.launch {
                                launch { animDragY.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.6f)) }
                            }

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
                            val maxRight = 2f * tabWidthPx - paddingPx
                            val maxLeft = maxRight - normalWidthPx

                            var targetLeft = dragTargetLeft
                            var targetRight = dragTargetRight

                            // Controlled squish against container edges (identical to CategoryTabSelector)
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

                            // Jelly + inertia stiffness
                            val leftStiffness = if (dragX > 0) 250f else if (dragX < 0) 800f else 600f
                            val rightStiffness = if (dragX > 0) 800f else if (dragX < 0) 250f else 600f

                            launch { animDragY.animateTo(dragTargetY, spring(stiffness = 1000f, dampingRatio = 0.7f)) }
                            launch { animLeft.animateTo(targetLeft, spring(stiffness = leftStiffness, dampingRatio = 0.65f)) }
                            launch { animRight.animateTo(targetRight, spring(stiffness = rightStiffness, dampingRatio = 0.65f)) }
                        }
                    }
                }
        ) {
            // 1. Base Inactive Layer: Icons in soft muted white with press feedback
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: List / Timeline
                Box(
                    modifier = Modifier
                        .width(tabWidthDp)
                        .height(tabHeightDp)
                        .pointerInput(selectedIndex) {
                            detectTapGestures(
                                onPress = {
                                    if (selectedIndex == 0) isSelectedTabPressed = true
                                    val released = tryAwaitRelease()
                                    isSelectedTabPressed = false
                                    if (released && selectedIndex != 0) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectMode(false)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = listIconRes),
                        contentDescription = null,
                        tint = inactiveIconTint,
                        modifier = Modifier.size(iconSizeDp)
                    )
                }

                // Tab 1: Calendar
                Box(
                    modifier = Modifier
                        .width(tabWidthDp)
                        .height(tabHeightDp)
                        .pointerInput(selectedIndex) {
                            detectTapGestures(
                                onPress = {
                                    if (selectedIndex == 1) isSelectedTabPressed = true
                                    val released = tryAwaitRelease()
                                    isSelectedTabPressed = false
                                    if (released && selectedIndex != 1) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectMode(true)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = calendarIconRes),
                        contentDescription = null,
                        tint = inactiveIconTint,
                        modifier = Modifier.size(iconSizeDp)
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
                            color = primaryColor,
                            topLeft = Offset(pillLeft, pillTop),
                            size = Size(finalPillWidth, pillHeight),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )
                    }
                    .drawWithContent {
                        val currentLeft = animLeft.value
                        val currentRight = animRight.value
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
                            modifier = Modifier.size(iconSizeDp)
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
                            modifier = Modifier.size(iconSizeDp)
                        )
                    }
                }
            }
        }
    }
}
