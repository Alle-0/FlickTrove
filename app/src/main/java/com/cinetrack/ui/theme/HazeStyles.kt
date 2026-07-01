package com.cinetrack.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle

object HazeStyles {
    // Core Glass Alphas for UI consistency
    const val GlassAlpha = 0.70f
    const val GlassAlphaSPlus = 0.65f
    const val GlassAlphaMedium = 0.45f
    const val GlassAlphaLow = 0.25f
    const val GlassAlphaFallback = 0.90f
    
    // Noise Factors
    const val NoiseFactorHigh = 0.06f
    const val NoiseFactorDefault = 0.04f
    const val NoiseFactorLow = 0.02f
    
    // Overlay Constants
    const val ModalScrimAlpha = 0.35f
    const val GlassBorderAlphaTop = 0.30f
    const val GlassBorderAlphaActive = 0.50f
    val GlassBorderColor = Color(0xFF666666)
    val AccentYellow = Color(0xFFFBBF24)
    const val GlassShimmerAlpha = 0.05f

    val GlassColor = Color(0xFF010103)
    val GlassBlurRadius = 16.dp
    val SmallGlassBlurRadius = 8.dp

    /**
     * Unified haze style for all glassmorphism effects in the app.
     * Uses a deep black tint to ensure readability and high contrast.
     */
    val PremiumDark = HazeStyle(
        blurRadius = GlassBlurRadius,
        tint = GlassColor.copy(alpha = GlassAlpha),
        noiseFactor = NoiseFactorDefault
    )

    /**
     * Style for dialogs to ensure high contrast against background content.
     */
    val glassmorphicDialog = PremiumDark.copy(
        blurRadius = 24.dp,
        tint = Color.Black.copy(alpha = 0.75f)
    )
}
