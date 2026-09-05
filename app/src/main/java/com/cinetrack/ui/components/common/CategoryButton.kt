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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.Animatable
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
    tabWidth: androidx.compose.ui.unit.Dp = 116.dp
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
        val offsetAnimatable = remember { Animatable(selectedIndex * realTabWidthPx) }

        LaunchedEffect(selectedIndex, realTabWidthPx) {
            offsetAnimatable.animateTo(
                targetValue = selectedIndex * realTabWidthPx,
                animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)
            )
        }

        val maxOffset = realTabWidthPx * (options.size - 1)
        val currentIndicatorOffset = offsetAnimatable.value.coerceIn(0f, maxOffset)

        // Stretch deformation based on velocity
        val velocity = offsetAnimatable.velocity
        // The faster it moves, the more it stretches horizontally
        val stretchFactor = 1f + (kotlin.math.abs(velocity) / realTabWidthPx) * 0.05f
        // Snap to exactly 1f when velocity is near zero to prevent RenderNode float artifacts (1px horizontal line glitch)
        val currentScaleX = if (kotlin.math.abs(velocity) < 10f) 1f else stretchFactor.coerceIn(1f, 1.35f)

        // Sliding Highlighter
        Box(
            modifier = Modifier
                .offset { IntOffset(currentIndicatorOffset.roundToInt(), 0) }
                .graphicsLayer { scaleX = currentScaleX }
                .padding(3.dp)
                .width(realTabWidth - 6.dp)
                .height(tabHeight - 6.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    shape = CircleShape
                )
        )

        // Content
        Row(
            modifier = Modifier
                .width(realTabWidth * options.size)
                .fillMaxHeight()
                .pointerInput(selectedIndex, realTabWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val targetIndex = (offsetAnimatable.value / realTabWidthPx).roundToInt().coerceIn(0, options.size - 1)
                            if (targetIndex != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOptionClick(targetIndex)
                            } else {
                                coroutineScope.launch {
                                    offsetAnimatable.animateTo(
                                        targetValue = selectedIndex * realTabWidthPx,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetAnimatable.animateTo(
                                    targetValue = selectedIndex * realTabWidthPx,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)
                                )
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetAnimatable.snapTo(
                                (offsetAnimatable.value + dragAmount).coerceIn(0f, maxOffset)
                            )
                        }
                    }
                }
        ) {
            options.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                    label = "textColor"
                )

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "tabScale")

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clickable(interactionSource = interactionSource, indication = null) { onOptionClick(index) },
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
                            fontSize = 12.sp,
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
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f),
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
}
