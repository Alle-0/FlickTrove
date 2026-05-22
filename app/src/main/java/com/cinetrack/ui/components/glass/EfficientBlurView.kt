package com.cinetrack.ui.components.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.cinetrack.ui.theme.HazeStyles

/**
 * A performance-optimized BlurView container for Jetpack Compose.
 * Uses the custom [glassmorphic] modifier to achieve the "FlickTrove" look.
 */
@Composable
fun EfficientBlurView(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    blurRadius: Dp = HazeStyles.SmallGlassBlurRadius,
    contentPadding: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        // Background layer (blurred)
        Box(
            modifier = Modifier
                .matchParentSize()
                .glassmorphic(shape = shape, blurRadius = blurRadius)
        )
        
        // Content layer (sharp) - Ensure it stays sharp by being outside the blur layer
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
