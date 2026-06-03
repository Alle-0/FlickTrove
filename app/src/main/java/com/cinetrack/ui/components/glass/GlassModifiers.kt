package com.cinetrack.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinetrack.LocalAdvancedVisualEffects
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.layout
import io.github.fletchmckee.liquid.liquid

import androidx.compose.ui.unit.coerceAtLeast

/**
 * A standardized glassmorphic modifier that integrates with Haze for high-quality background blur.
 * Pairs HazeStyles.PremiumDark with consistent background overlays and borders.
 *
 * @param state The HazeState from the container that should be blurred.
 * @param shape The shape of the glass element.
 * @param style The HazeStyle to apply (defaults to HazeStyles.PremiumDark).
 * @param useOffscreenStrategy Whether to use CompositingStrategy.Offscreen to fix text rendering.
 */
fun Modifier.hazeGlass(
    state: HazeState?,
    shape: Shape,
    style: HazeStyle? = null,
    blurRadius: Dp = HazeStyles.GlassBlurRadius,
    containerColor: Color? = null,
    useOffscreenStrategy: Boolean = false,
    borderColor: Color = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
    borderWidth: Dp = 1.dp,
    alpha: Float = 1f
): Modifier = composed {
    val finalStyle = remember(blurRadius, containerColor, style, alpha) {
        val safeBlurRadius = (blurRadius * alpha).coerceAtLeast(0.1.dp)
        style?.let { 
            // If custom style is provided, attempt to scale it by alpha
            val safeStyleBlurRadius = (it.blurRadius * alpha).coerceAtLeast(0.1.dp)
            it.copy(
                blurRadius = safeStyleBlurRadius,
                tint = it.tint.copy(alpha = it.tint.alpha * alpha)
            )
        } ?: if (containerColor != null) {
            HazeStyles.PremiumDark.copy(
                blurRadius = safeBlurRadius,
                tint = containerColor.copy(alpha = (if (containerColor.alpha != 1f) containerColor.alpha else HazeStyles.GlassAlpha) * alpha)
            )
        } else {
            HazeStyles.PremiumDark.copy(
                blurRadius = safeBlurRadius,
                tint = HazeStyles.PremiumDark.tint.copy(alpha = HazeStyles.PremiumDark.tint.alpha * alpha)
            )
        }
    }
    
    val advancedEffectsEnabled = LocalAdvancedVisualEffects.current
    
    val modifierChain = this
        .clip(shape)
        .then(
            if (state != null && advancedEffectsEnabled) {
                // Apply graphicsLayer with Offscreen strategy if requested, then apply hazeChild.
                // We use the lambda-less version to be extremely explicit and avoid property-vs-parameter confusion.
                if (useOffscreenStrategy) {
                    Modifier
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen, alpha = alpha)
                        .hazeChild(state = state, shape = shape, style = finalStyle)
                } else {
                    Modifier
                        .graphicsLayer(alpha = alpha)
                        .hazeChild(state = state, shape = shape, style = finalStyle)
                }
            } else {
                val fallbackColor = containerColor ?: HazeStyles.GlassColor
                val fallbackAlpha = if (advancedEffectsEnabled) HazeStyles.GlassAlphaFallback * alpha else 0.95f * alpha
                Modifier.background(fallbackColor.copy(alpha = fallbackAlpha), shape)
            }
        )
        .glassOverlay(shape, borderWidth, borderColor.copy(alpha = borderColor.alpha * alpha))
        
    modifierChain
}

fun Modifier.glassmorphic(
    shape: Shape,
    blurRadius: Dp = HazeStyles.GlassBlurRadius,
    containerColor: Color = HazeStyles.GlassColor,
    borderWidth: Dp = 1.dp,
    borderColor: Color = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
    borderBrush: Brush? = null
) = composed {
    val isSPlus = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val advancedEffectsEnabled = LocalAdvancedVisualEffects.current
    val alpha = if (advancedEffectsEnabled) {
        if (isSPlus) HazeStyles.GlassAlphaSPlus else HazeStyles.GlassAlphaFallback
    } else {
        0.95f
    }
    
    this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    containerColor.copy(alpha = alpha),
                    containerColor.copy(alpha = (alpha - 0.1f).coerceAtLeast(0.4f))
                )
            )
        )
        .glassOverlay(shape, borderWidth, borderColor, borderBrush)
}

/**
 * Common overlay for all glass elements (shimmer + border).
 * Uses a solid border by default to ensure visual clarity.
 */
fun Modifier.glassOverlay(
    shape: Shape, 
    borderWidth: Dp = 1.dp,
    borderColor: Color = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
    borderBrush: Brush? = null
): Modifier = this
    .background(Color.White.copy(alpha = HazeStyles.GlassShimmerAlpha), shape) // Subtle shimmer
    .border(
        width = borderWidth,
        brush = borderBrush ?: SolidColor(borderColor),
        shape = shape
    )
