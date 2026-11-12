package com.cinetrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.utils.ColorUtils

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
fun FlickTroveTheme(
    themeSetting: String = "AMOLED",
    accentColor: Color = NeonTeal,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    
    val baseColorScheme = if (themeSetting == "AMOLED") AMOLEDColorScheme else DarkColorScheme

    val colorScheme = baseColorScheme.copy(
        primary = accentColor,
        onPrimary = ColorUtils.contentColorForAccent(accentColor),
        secondary = ColorUtils.calculateHarmonicSecondary(accentColor)
    )

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val isArabic = LocalConfiguration.current.locales.let { !it.isEmpty && it[0]?.language == "ar" }
    val resolvedTypography = if (isRtl || isArabic) {
        Typography.copy(
            bodyLarge = Typography.bodyLarge.copy(letterSpacing = 0.sp),
            labelSmall = Typography.labelSmall.copy(letterSpacing = 0.sp)
        )
    } else {
        Typography
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = resolvedTypography,
        content = content
    )
}
