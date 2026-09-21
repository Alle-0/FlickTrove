package com.cinetrack.ui.components.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp

@Composable
fun FlickTroveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val trackWidth = 52.dp
    val trackHeight = 28.dp
    val baseThumbSize = 24.dp
    val padding = 2.dp

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    // Elastic stretch on press (increases width from 24.dp to 28.dp like iOS / Material You)
    val animatedThumbWidth by animateDpAsState(
        targetValue = if (isPressed) 28.dp else baseThumbSize,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "thumbWidth"
    )

    val targetOffset = if (checked) trackWidth - animatedThumbWidth - padding else padding
    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "switchOffset"
    )

    val targetScale = when {
        isPressed -> 0.95f
        isHovered -> if (checked) 1.05f else 0.95f
        checked -> 1f
        else -> 0.85f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "thumbScale"
    )

    val targetTrackAlpha = when {
        checked -> if (isHovered) 0.75f else 0.6f
        isHovered -> 0.25f
        else -> 0.15f
    }
    val animatedTrackAlpha by animateFloatAsState(
        targetValue = targetTrackAlpha,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "trackAlpha"
    )

    val animatedHoverHaloAlpha by animateFloatAsState(
        targetValue = if (isHovered) 0.18f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "hoverHaloAlpha"
    )

    val borderColor = when {
        checked -> if (isHovered) accentColor else accentColor.copy(alpha = 0.85f)
        isHovered -> Color.White.copy(alpha = 0.35f)
        else -> Color.White.copy(alpha = 0.15f)
    }

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .pointerHoverIcon(PointerIcon.Hand)
            .clip(CircleShape)
            .background(if (checked) accentColor.copy(alpha = animatedTrackAlpha) else Color.White.copy(alpha = animatedTrackAlpha))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Glow effect behind the thumb when checked
        if (checked) {
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .size(width = animatedThumbWidth, height = baseThumbSize)
                    .graphicsLayer {
                        scaleX = if (isHovered) 1.45f else 1.3f
                        scaleY = if (isHovered) 1.45f else 1.3f
                        alpha = if (isHovered) 0.55f else 0.4f
                    }
                    .background(accentColor, CircleShape)
            )
        }

        // Concentric Hover Halo around thumb
        if (animatedHoverHaloAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset - 4.dp)
                    .size(width = animatedThumbWidth + 8.dp, height = baseThumbSize + 8.dp)
                    .graphicsLayer {
                        alpha = animatedHoverHaloAlpha
                    }
                    .background(if (checked) accentColor else Color.White, CircleShape)
            )
        }

        // Thumb
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .size(width = animatedThumbWidth, height = baseThumbSize)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
                .shadow(
                    elevation = if (checked) (if (isHovered) 8.dp else 6.dp) else 2.dp,
                    shape = CircleShape,
                    ambientColor = if (checked) accentColor else Color.Black,
                    spotColor = if (checked) accentColor else Color.Black
                )
                .background(Color.White, CircleShape)
        )
    }
}
