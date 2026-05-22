package com.cinetrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonTeal,
    secondary = NeonPink,
    tertiary = NeonPurple,
    background = DarkGrey,
    surface = DarkSurface,
    error = ErrorRed,
    onPrimary = DeepBlack,
    onSecondary = OnSurfaceLight,
    onTertiary = OnSurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = OnSurfaceLight
)

@Composable
fun FlickTrove_KotlinTheme(
    darkTheme: Boolean = true,
    accentColor: Color = NeonTeal,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme.copy(
        primary = accentColor
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
