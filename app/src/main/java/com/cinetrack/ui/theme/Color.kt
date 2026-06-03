package com.cinetrack.ui.theme

import androidx.compose.ui.graphics.Color

// Core Brand Colors
val DarkGrey = Color(0xFF0F0F1A)
val DarkSurface = Color(0xFF1A1A2E)
val DeepBlack = Color(0xFF000000)
val PremiumBackground = Color(0xFF08080A)

// Light Theme Colors
val LightGrey = Color(0xFFF5F5FA)
val LightSurface = Color(0xFFFFFFFF)
val TextDark = Color(0xFF121212)
val TextDarkMuted = Color(0xFF666666)

// Neon Accents (The "Cyberpunk" palette)
val NeonTeal = Color(0xFF2DD4BF)   // Primary
val NeonPink = Color(0xFFFF4081)   // Secondary
val NeonPurple = Color(0xFFBB86FC) // Tertiary
val NeonCyan = Color(0xFF00E5FF)
val NeonBlue = Color(0xFF00B0FF)
val NeonAmber = Color(0xFFFFB300)
val PrimaryTeal = NeonTeal

// Gradients for Charts & UI
val NeonGradients = listOf(
    listOf(NeonTeal, NeonCyan),
    listOf(NeonPink, NeonPurple),
    listOf(NeonPurple, NeonBlue),
    listOf(NeonCyan, NeonTeal),
    listOf(NeonAmber, NeonPink)
)

// Text & Semantics
val OnSurfaceLight = Color(0xFFE0E0E0)
val OnSurfaceMuted = Color(0xFF808080)
val ErrorRed = Color(0xFFFF5252)

// Glassmorphism System
// Note: We use Color(r, g, b, a) or hex with alpha
val GlassWhite = Color(1f, 1f, 1f, 0.1f)
val GlassWhiteBorder = Color(1f, 1f, 1f, 0.2f)
val GlassBlack = Color(0f, 0f, 0f, 0.3f)
val GlassSurface = HazeStyles.GlassColor.copy(alpha = HazeStyles.GlassAlpha)
