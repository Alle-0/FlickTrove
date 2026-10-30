package com.cinetrack.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect

object HazeStyles {
    // Core Glass Alphas for UI consistency - tarati per un vetro traslucido, luminoso e non fangoso
    const val GlassAlpha = 0.35f
    const val GlassAlphaSPlus = 0.30f
    const val GlassAlphaMedium = 0.20f
    const val GlassAlphaLow = 0.12f
    const val GlassAlphaFallback = 0.85f
    
    // Noise Factors
    const val NoiseFactorHigh = 0.04f
    const val NoiseFactorDefault = 0.02f
    const val NoiseFactorLow = 0.01f
    
    // Overlay Constants
    const val ModalScrimAlpha = 0.30f
    const val ModalBorderAlpha = 0.06f
    const val ModalBorderAlphaStart = 0.08f
    const val GlassBorderAlphaTop = 0.10f
    const val GlassBorderAlphaActive = 0.18f
    val GlassBorderColor = Color.White.copy(alpha = 0.10f)
    val AccentYellow = Color(0xFFFBBF24)
    const val GlassShimmerAlpha = 0f

    val GlassColor = Color(0xFF0A0E14)
    val GlassBlurRadius = 22.dp
    val SmallGlassBlurRadius = 12.dp

    /**
     * Unified haze style for all glassmorphism effects in the app.
     * Uses transparent background and balanced tint to let backlight and colors shine through.
     */
    val PremiumDark = HazeBlurStyle {
        blurRadius(GlassBlurRadius)
        noiseFactor(NoiseFactorDefault)
        backgroundColor(Color.Transparent)
        colorEffects(
            listOf(HazeColorEffect.tint(GlassColor.copy(alpha = GlassAlpha)))
        )
    }

    /**
     * Style for dialogs to ensure high contrast against background content while preserving translucency.
     */
    val glassmorphicDialog = PremiumDark.then {
        blurRadius(24.dp)
        colorEffects(
            listOf(HazeColorEffect.tint(Color.Black.copy(alpha = 0.38f)))
        )
    }
}

