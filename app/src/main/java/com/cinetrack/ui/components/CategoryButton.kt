package com.cinetrack.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
        contentColor = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
        shape = CircleShape,
        modifier = Modifier
            .height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text.uppercase(), 
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier
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
    modifier: Modifier = Modifier
) {
    val tabHeight = 36.dp
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .height(tabHeight)
            .wrapContentWidth()
    ) {
        val calculatedWidth = maxWidth / options.size
        val tabWidth = androidx.compose.ui.unit.min(120.dp, calculatedWidth)
        val tabWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { tabWidth.toPx() }
        
        val coroutineScope = rememberCoroutineScope()
        val offsetAnimatable = remember { androidx.compose.animation.core.Animatable(selectedIndex * tabWidthPx) }

        androidx.compose.runtime.LaunchedEffect(selectedIndex, tabWidthPx) {
            offsetAnimatable.animateTo(
                targetValue = selectedIndex * tabWidthPx,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
            )
        }

        val maxOffset = tabWidthPx * (options.size - 1)
        val currentIndicatorOffset = offsetAnimatable.value.coerceIn(0f, maxOffset)

        // Sliding Highlighter
        Surface(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(currentIndicatorOffset.roundToInt(), 0) }
                .padding(4.dp)
                .width(tabWidth - 8.dp)
                .height(tabHeight - 8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = CircleShape
        ) {}

        // Content
        Row(modifier = Modifier
            .width(tabWidth * options.size)
            .fillMaxHeight()
            .pointerInput(selectedIndex, tabWidthPx) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val currentPos = offsetAnimatable.value
                        val targetIndex = (currentPos / tabWidthPx).roundToInt().coerceIn(0, options.size - 1)
                        if (targetIndex != selectedIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOptionClick(targetIndex)
                        } else {
                            coroutineScope.launch {
                                offsetAnimatable.animateTo(
                                    targetValue = selectedIndex * tabWidthPx,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                                )
                            }
                        }
                    },
                    onDragCancel = { 
                        coroutineScope.launch {
                            offsetAnimatable.animateTo(
                                targetValue = selectedIndex * tabWidthPx,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
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
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onOptionClick(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        if (counts != null && counts.size > index) {
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // Number in a circle/badge
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = counts[index].toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
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
