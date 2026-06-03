package com.cinetrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

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

private val AMOLEDColorScheme = darkColorScheme(
    primary = NeonTeal,
    secondary = NeonPink,
    tertiary = NeonPurple,
    background = DeepBlack,
    surface = DeepBlack,
    error = ErrorRed,
    onPrimary = DeepBlack,
    onSecondary = OnSurfaceLight,
    onTertiary = OnSurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = OnSurfaceLight
)

private val LightColorScheme = lightColorScheme(
    primary = NeonTeal,
    secondary = NeonPink,
    tertiary = NeonPurple,
    background = LightGrey,
    surface = LightSurface,
    error = ErrorRed,
    onPrimary = LightSurface,
    onSecondary = TextDark,
    onTertiary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = TextDarkMuted
)

@Composable
fun FlickTrove_KotlinTheme(
    themeSetting: String = "System",
    accentColor: Color = NeonTeal,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    
    val baseColorScheme = when (themeSetting) {
        "Light" -> LightColorScheme
        "Dark" -> DarkColorScheme
        "AMOLED" -> AMOLEDColorScheme
        else -> if (isSystemDark) DarkColorScheme else LightColorScheme
    }

    val colorScheme = baseColorScheme.copy(
        primary = accentColor
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
