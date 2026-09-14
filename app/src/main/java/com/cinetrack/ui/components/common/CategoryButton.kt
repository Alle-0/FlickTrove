package com.cinetrack.ui.components.common
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun CategoryButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.6f),
        shape = CircleShape,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun CategoryTabSelector(
    options: List<String>,
    counts: List<Int>? = null,
    selectedIndex: Int,
    onOptionClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabWidth: androidx.compose.ui.unit.Dp = 116.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    val tabHeight = 34.dp
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .height(tabHeight)
            .wrapContentWidth()
    ) {
        val requestedTabWidthPx = with(LocalDensity.current) { tabWidth.toPx() }
        val maxAvailableWidthPx = constraints.maxWidth.toFloat()
        
        // If the total requested width exceeds available width, the layout will squish the tabs.
        // We calculate the real tab width to ensure the indicator matches the actual rendered tab size.
        val realTabWidthPx = if (maxAvailableWidthPx > 0 && maxAvailableWidthPx < requestedTabWidthPx * options.size) {
            maxAvailableWidthPx / options.size
        } else {
            requestedTabWidthPx
        }
        val realTabWidth = with(LocalDensity.current) { realTabWidthPx.toDp() }

        val coroutineScope = rememberCoroutineScope()
        val paddingPx = with(LocalDensity.current) { 3.dp.toPx() }
        val normalWidthPx = (realTabWidthPx - 2 * paddingPx).coerceAtLeast(1f)

        val animLeft = remember { Animatable(selectedIndex * realTabWidthPx + paddingPx) }
        val animRight = remember { Animatable((selectedIndex + 1) * realTabWidthPx - paddingPx) }

        var isSelectedTabPressed by remember { mutableStateOf(false) }
        var isDragging by remember { mutableStateOf(false) }
        var previousIndex by remember { mutableStateOf(selectedIndex) }
        var flightStartCenter by remember { mutableStateOf(0f) }
        var flightTargetCenter by remember { mutableStateOf(0f) }
        var isFlightActive by remember { mutableStateOf(false) }
        var peakFlightBoost by remember { mutableStateOf(0.30f) }

        // Apple-style Leading & Trailing edge spring physics
        val advancedEffectsEnabled = com.cinetrack.LocalAdvancedVisualEffects.current

        LaunchedEffect(selectedIndex, realTabWidthPx, options.size, advancedEffectsEnabled) {
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
                flightStartCenter = (animLeft.value + animRight.value) / 2f
                flightTargetCenter = (targetLeft + targetRight) / 2f
                peakFlightBoost = (0.30f + (indexDistance - 1) * 0.04f).coerceAtMost(0.40f)
                isFlightActive = true
            }

            if (targetLeft > animLeft.value) {
                // Moving right: Right edge leads (fast spring), Left edge trails (softer spring) -> stretches horizontally
                val jobRight = launch {
                    animRight.animateTo(
                        targetValue = targetRight,
                        animationSpec = spring(stiffness = 700f, dampingRatio = 0.72f)
                    )
                }
                val jobLeft = launch {
                    animLeft.animateTo(
                        targetValue = targetLeft,
                        animationSpec = spring(stiffness = 460f, dampingRatio = 0.78f)
                    )
                }
                jobRight.join()
                jobLeft.join()
                isFlightActive = false
            } else {
                // Moving left: Left edge leads (fast spring), Right edge trails (softer spring) -> stretches horizontally
                val jobLeft = launch {
                    animLeft.animateTo(
                        targetValue = targetLeft,
                        animationSpec = spring(stiffness = 700f, dampingRatio = 0.72f)
                    )
                }
                val jobRight = launch {
                    animRight.animateTo(
                        targetValue = targetRight,
                        animationSpec = spring(stiffness = 460f, dampingRatio = 0.78f)
                    )
                }
                jobLeft.join()
                jobRight.join()
                isFlightActive = false
            }
        }

        // Spring animation for scaling up the indicator when held/pressed or dragged
        val dragScaleY by animateFloatAsState(
            targetValue = if (!advancedEffectsEnabled) 1f
                else if (isDragging) 1.30f
                else if (isSelectedTabPressed) 1.10f
                else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "dragScaleY"
        )
        val dragScaleX by animateFloatAsState(
            targetValue = if (!advancedEffectsEnabled) 1f
                else if (isDragging) 1.06f
                else if (isSelectedTabPressed) 1.04f
                else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "dragScaleX"
        )

        val primaryColor = MaterialTheme.colorScheme.primary
        val pillPath = remember { Path() }

        Box(
            modifier = Modifier
                .width(realTabWidth * options.size)
                .fillMaxHeight()
                .pointerInput(selectedIndex, realTabWidthPx) {
                    var isDragActiveOnPill = false
                    val touchMargin = 8.dp.toPx()
                    detectHorizontalDragGestures(
                        onDragStart = { startOffset ->
                            val currentLeft = animLeft.value
                            val currentRight = animRight.value
                            if (startOffset.x in (currentLeft - touchMargin)..(currentRight + touchMargin)) {
                                isDragActiveOnPill = true
                                isDragging = true
                                coroutineScope.launch {
                                    launch { animLeft.stop() }
                                    launch { animRight.stop() }
                                }
                            } else {
                                isDragActiveOnPill = false
                            }
                        },
                        onDragEnd = {
                            if (!isDragActiveOnPill) return@detectHorizontalDragGestures
                            isDragActiveOnPill = false
                            isDragging = false
                            val currentLeft = animLeft.value - paddingPx
                            val targetIndex = (currentLeft / realTabWidthPx).roundToInt().coerceIn(0, options.size - 1)
                            if (targetIndex != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOptionClick(targetIndex)
                            } else {
                                val targetLeft = selectedIndex * realTabWidthPx + paddingPx
                                val targetRight = (selectedIndex + 1) * realTabWidthPx - paddingPx
                                coroutineScope.launch {
                                    launch {
                                        animLeft.animateTo(
                                            targetLeft,
                                            spring(stiffness = 500f, dampingRatio = 0.75f)
                                        )
                                    }
                                    launch {
                                        animRight.animateTo(
                                            targetRight,
                                            spring(stiffness = 500f, dampingRatio = 0.75f)
                                        )
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            if (!isDragActiveOnPill) return@detectHorizontalDragGestures
                            isDragActiveOnPill = false
                            isDragging = false
                            val targetLeft = selectedIndex * realTabWidthPx + paddingPx
                            val targetRight = (selectedIndex + 1) * realTabWidthPx - paddingPx
                            coroutineScope.launch {
                                launch {
                                    animLeft.animateTo(
                                        targetLeft,
                                        spring(stiffness = 500f, dampingRatio = 0.75f)
                                    )
                                }
                                launch {
                                    animRight.animateTo(
                                        targetRight,
                                        spring(stiffness = 500f, dampingRatio = 0.75f)
                                    )
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        if (!isDragActiveOnPill) return@detectHorizontalDragGestures
                        isDragging = true
                        change.consume()
                        coroutineScope.launch {
                            val maxRight = options.size * realTabWidthPx - paddingPx
                            val newLeft = (animLeft.value + dragAmount).coerceIn(paddingPx, maxRight - normalWidthPx)
                            val newRight = newLeft + normalWidthPx
                            launch { animLeft.snapTo(newLeft) }
                            launch { animRight.snapTo(newRight) }
                        }
                    }
                }
        ) {
            // 1. Base Layer: Inactive muted grey labels
            CategoryTabItems(
                options = options,
                counts = counts,
                textColor = Color.White.copy(alpha = 0.5f),
                badgeBgColor = Color.White.copy(alpha = 0.1f),
                realTabWidth = realTabWidth,
                selectedIndex = selectedIndex,
                isInteractive = true,
                onOptionClick = onOptionClick,
                onTabPressedChange = { isSelectedTabPressed = it },
                fontSize = fontSize
            )

            // 2. Sliding Highlighter + 3. Active Masked Layer (Only what is under the selector gets colored!)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val currentLeft = animLeft.value
                        val currentRight = animRight.value
                        val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                        val centerXPx = (currentLeft + currentRight) / 2f

                        val stretch = if (advancedEffectsEnabled && normalWidthPx > 0f) {
                            ((currentWidth / normalWidthPx) - 1f).coerceAtLeast(0f)
                        } else 0f
                        val flightEnlargeX = stretch * 0.35f

                        val flightEnlargeY = if (advancedEffectsEnabled && isFlightActive) {
                            val totalDist = flightTargetCenter - flightStartCenter
                            if (kotlin.math.abs(totalDist) > 1f) {
                                val fraction = ((centerXPx - flightStartCenter) / totalDist).coerceIn(0f, 1f)
                                kotlin.math.sin(fraction * Math.PI.toFloat()) * peakFlightBoost
                            } else 0f
                        } else 0f

                        val totalScaleX = if (advancedEffectsEnabled) dragScaleX * (1f + flightEnlargeX) else 1f
                        val totalScaleY = if (advancedEffectsEnabled) maxOf(dragScaleY, 1f + flightEnlargeY) else 1f

                        val baseHeight = size.height - (paddingPx * 2f)
                        val maxPillHeight = size.height * 1.45f
                        val pillHeight = (baseHeight * totalScaleY).coerceAtLeast(1f).coerceAtMost(maxPillHeight)
                        val pillWidth = (currentWidth * totalScaleX).coerceAtLeast(1f)

                        val pillTop = (size.height - pillHeight) / 2f
                        val pillLeft = centerXPx - (pillWidth / 2f)

                        drawRoundRect(
                            color = primaryColor.copy(alpha = 0.25f),
                            topLeft = Offset(pillLeft, pillTop),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f)
                        )
                    }
                    .drawWithContent {
                        val currentLeft = animLeft.value
                        val currentRight = animRight.value
                        val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                        val centerXPx = (currentLeft + currentRight) / 2f

                        val stretch = if (advancedEffectsEnabled && normalWidthPx > 0f) {
                            ((currentWidth / normalWidthPx) - 1f).coerceAtLeast(0f)
                        } else 0f
                        val flightEnlargeX = stretch * 0.35f

                        val flightEnlargeY = if (advancedEffectsEnabled && isFlightActive) {
                            val totalDist = flightTargetCenter - flightStartCenter
                            if (kotlin.math.abs(totalDist) > 1f) {
                                val fraction = ((centerXPx - flightStartCenter) / totalDist).coerceIn(0f, 1f)
                                kotlin.math.sin(fraction * Math.PI.toFloat()) * peakFlightBoost
                            } else 0f
                        } else 0f

                        val totalScaleX = if (advancedEffectsEnabled) dragScaleX * (1f + flightEnlargeX) else 1f
                        val totalScaleY = if (advancedEffectsEnabled) maxOf(dragScaleY, 1f + flightEnlargeY) else 1f

                        val baseHeight = size.height - (paddingPx * 2f)
                        val maxPillHeight = size.height * 1.45f
                        val pillHeight = (baseHeight * totalScaleY).coerceAtLeast(1f).coerceAtMost(maxPillHeight)
                        val pillWidth = (currentWidth * totalScaleX).coerceAtLeast(1f)

                        val pillTop = (size.height - pillHeight) / 2f
                        val pillLeft = centerXPx - (pillWidth / 2f)

                        pillPath.reset()
                        pillPath.addRoundRect(
                            RoundRect(
                                left = pillLeft,
                                top = pillTop,
                                right = pillLeft + pillWidth,
                                bottom = pillTop + pillHeight,
                                radiusX = pillHeight / 2f,
                                radiusY = pillHeight / 2f
                            )
                        )

                        // Clip the active pink layer strictly to what is under the selector
                        clipPath(pillPath) {
                            this@drawWithContent.drawContent()
                        }
                    }
            ) {
                // Active Layer: Rendered only within the pill capsule bounds
                CategoryTabItems(
                    options = options,
                    counts = counts,
                    textColor = primaryColor,
                    badgeBgColor = primaryColor.copy(alpha = 0.15f),
                    realTabWidth = realTabWidth,
                    selectedIndex = selectedIndex,
                    isInteractive = false,
                    onOptionClick = {},
                    fontSize = fontSize
                )
            }
        }
    }
}

@Composable
private fun CategoryTabItems(
    options: List<String>,
    counts: List<Int>?,
    textColor: Color,
    badgeBgColor: Color,
    realTabWidth: androidx.compose.ui.unit.Dp,
    selectedIndex: Int,
    isInteractive: Boolean,
    onOptionClick: (Int) -> Unit,
    onTabPressedChange: ((Boolean) -> Unit)? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    Row(
        modifier = Modifier
            .width(realTabWidth * options.size)
            .fillMaxHeight()
    ) {
        options.forEachIndexed { index, title ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            if (isInteractive && index == selectedIndex && onTabPressedChange != null) {
                DisposableEffect(isPressed) {
                    onTabPressedChange(isPressed)
                    onDispose { onTabPressedChange(false) }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isInteractive) {
                            Modifier.clickable(interactionSource = interactionSource, indication = null) {
                                onOptionClick(index)
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (counts != null && counts.size > index) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .background(
                                    color = badgeBgColor,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = counts[index].toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}
