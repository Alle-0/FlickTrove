package com.cinetrack.ui.components.common
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
    val thumbSize = 24.dp
    val padding = 2.dp

    val animatedOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - padding else padding,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "switchOffset"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "thumbScale"
    )

    val animatedTrackAlpha by animateFloatAsState(
        targetValue = if (checked) 0.6f else 0.15f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "trackAlpha"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(CircleShape)
            .background(if (checked) accentColor.copy(alpha = animatedTrackAlpha) else Color.White.copy(alpha = animatedTrackAlpha))
            .border(
                width = 1.dp,
                color = if (checked) accentColor else Color.White.copy(alpha = 0.15f),
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
                    .size(thumbSize)
                    .graphicsLayer {
                        scaleX = 1.3f
                        scaleY = 1.3f
                        alpha = 0.4f
                    }
                    .background(accentColor, CircleShape)
            )
        }
        
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .size(thumbSize)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
                .shadow(
                    elevation = if (checked) 6.dp else 2.dp,
                    shape = CircleShape,
                    ambientColor = if (checked) accentColor else Color.Black,
                    spotColor = if (checked) accentColor else Color.Black
                )
                .background(Color.White, CircleShape)
        )
    }
}
