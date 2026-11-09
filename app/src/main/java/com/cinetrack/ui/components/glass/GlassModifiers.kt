package com.cinetrack.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.cinetrack.LocalAdvancedVisualEffects
import com.cinetrack.ui.LocalBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.foundation.shape.CornerBasedShape
import kotlin.math.min
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.OpticalSizeValue
import dev.chrisbanes.haze.glass.hazeGlass as hazeGlassRefract

/**
 * Wraps a Shape to prevent Compose's BorderModifierNode from crashing with
 * `java.lang.IllegalArgumentException: width and height must be > 0`
 * when a generic path has empty/degenerate bounds (e.g. width/height <= 0).
 */
fun safeShape(shape: Shape): Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(Rect.Zero)
        }
        val outline = shape.createOutline(size, layoutDirection, density)
        if (outline is Outline.Generic) {
            val bounds = outline.bounds
            if (bounds.width <= 0.5f || bounds.height <= 0.5f) {
                return Outline.Rectangle(Rect.Zero)
            }
        }
        return outline
    }
}

/**
 * A standardized glassmorphic modifier that integrates with Haze for high-quality background blur.
 * Pairs HazeStyles.PremiumDark with consistent background overlays and borders.
 *
 * @param state The HazeState from the container that should be blurred.
 * @param shape The shape of the glass element.
 * @param style The HazeBlurStyle to apply (defaults to HazeStyles.PremiumDark).
 * @param useOffscreenStrategy Whether to use CompositingStrategy.Offscreen to fix text rendering.
 */
fun Modifier.hazeGlass(
    state: HazeState?,
    shape: Shape,
    style: HazeBlurStyle? = null,
    blurRadius: Dp = HazeStyles.GlassBlurRadius,
    containerColor: Color? = null,
    useOffscreenStrategy: Boolean = false,
    borderColor: Color = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
    borderWidth: Dp = 0.dp,
    alpha: Float = 1f,
    useRefraction: Boolean = false
): Modifier = composed {
    val effectiveShape = remember(shape) { safeShape(shape) }
    val backdrop = LocalBackdrop.current
    val advancedEffectsEnabled = LocalAdvancedVisualEffects.current
    val liquidGlassEnabled = com.cinetrack.LocalLiquidGlassEnabled.current
    val isCornerShape = shape is CornerBasedShape
    val canUseLiquidGlass = backdrop != null && advancedEffectsEnabled && liquidGlassEnabled && useRefraction && isCornerShape

    val modifierChain = this
        .clip(effectiveShape)
        .then(
            if (canUseLiquidGlass) {
                Modifier.drawPlainBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        val targetBlur = (blurRadius * alpha).coerceAtLeast(0.1.dp).toPx()
                        blur(targetBlur)
                        val minDim = min(size.width, size.height)
                        val refHeight = min(24.dp.toPx(), minDim * 0.45f)
                        val refAmount = (refHeight * 1.65f).coerceAtLeast(24.dp.toPx())
                        try {
                            lens(
                                refractionHeight = refHeight,
                                refractionAmount = refAmount,
                                depthEffect = false,
                                chromaticAberration = true
                            )
                        } catch (_: Throwable) {
                            // Defensive guard: lens effect only supports CornerBasedShape
                        }
                    },
                    onDrawSurface = {
                        val darkTint = containerColor ?: Color(0xFF0C0C10)
                        val tintAlpha = if (containerColor != null && containerColor.alpha != 1f) {
                            containerColor.alpha
                        } else {
                            0.52f
                        } * alpha
                        drawRect(color = darkTint.copy(alpha = tintAlpha))
                    }
                )
            } else if (state != null && advancedEffectsEnabled) {
                val finalStyle = remember(blurRadius, containerColor, style, alpha) {
                    val safeBlurRadius = (blurRadius * alpha).coerceAtLeast(0.1.dp)
                    val baseStyle = style ?: HazeStyles.PremiumDark
                    baseStyle.then {
                        blurRadius(safeBlurRadius)
                        if (containerColor != null && containerColor != Color.Unspecified) {
                            val targetAlpha = (if (containerColor.alpha != 1f) containerColor.alpha else HazeStyles.GlassAlpha) * alpha
                            colorEffects(listOf(HazeColorEffect.tint(containerColor.copy(alpha = targetAlpha))))
                        } else if (style == null && alpha != 1f) {
                            colorEffects(listOf(HazeColorEffect.tint(HazeStyles.GlassColor.copy(alpha = HazeStyles.GlassAlpha * alpha))))
                        }
                    }
                }
                if (useOffscreenStrategy) {
                    Modifier
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen, alpha = alpha)
                        .hazeBlur(
                            input = HazeInput.Sources(state),
                            style = finalStyle,
                            performanceMode = HazePerformanceMode.Default,
                            expandLayerBounds = false
                        )
                } else {
                    if (alpha < 1f) {
                        Modifier
                            .graphicsLayer(alpha = alpha)
                            .hazeBlur(
                                input = HazeInput.Sources(state),
                                style = finalStyle,
                                performanceMode = HazePerformanceMode.Default,
                                expandLayerBounds = false
                            )
                    } else {
                        Modifier
                            .hazeBlur(
                                input = HazeInput.Sources(state),
                                style = finalStyle,
                                performanceMode = HazePerformanceMode.Default,
                                expandLayerBounds = false
                            )
                    }
                }
            } else {
                val fallbackBaseColor = if (containerColor != null && containerColor.luminance() > 0.4f) {
                    HazeStyles.GlassColor
                } else {
                    containerColor ?: HazeStyles.GlassColor
                }
                val fallbackAlpha = if (advancedEffectsEnabled) HazeStyles.GlassAlphaFallback * alpha else 0.88f * alpha
                val baseMod = Modifier.background(fallbackBaseColor.copy(alpha = fallbackAlpha), effectiveShape)
                if (containerColor != null && containerColor.luminance() > 0.4f) {
                    baseMod.background(containerColor, effectiveShape)
                } else {
                    baseMod
                }
            }
        )
        .glassOverlay(effectiveShape, borderWidth, borderColor.copy(alpha = borderColor.alpha * alpha))

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
    val effectiveShape = remember(shape) { safeShape(shape) }
    val isSPlus = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val advancedEffectsEnabled = LocalAdvancedVisualEffects.current
    val alpha = if (advancedEffectsEnabled) {
        if (isSPlus) HazeStyles.GlassAlphaSPlus else HazeStyles.GlassAlphaFallback
    } else {
        0.95f
    }
    
    this
        .clip(effectiveShape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    containerColor.copy(alpha = alpha),
                    containerColor.copy(alpha = (alpha - 0.1f).coerceAtLeast(0.4f))
                )
            ),
            shape = effectiveShape
        )
        .glassOverlay(effectiveShape, borderWidth, borderColor, borderBrush)
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
): Modifier {
    val withShimmer = if (HazeStyles.GlassShimmerAlpha > 0f) {
        this.background(Color.White.copy(alpha = HazeStyles.GlassShimmerAlpha), shape)
    } else {
        this
    }
    return if (borderWidth > 0.dp && (borderBrush != null || borderColor.alpha > 0f)) {
        withShimmer.border(
            width = borderWidth,
            brush = borderBrush ?: SolidColor(borderColor),
            shape = shape
        )
    } else {
        withShimmer
    }
}





