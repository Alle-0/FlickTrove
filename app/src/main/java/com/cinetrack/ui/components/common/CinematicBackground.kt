package com.cinetrack.ui.components.common
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.theme.NeonCyan
import com.cinetrack.ui.theme.NeonBlue
import com.cinetrack.ui.theme.NeonTeal
import com.cinetrack.ui.theme.PrimaryTeal
import java.util.*

@Composable
fun CinematicBackground(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    accentColor: Color? = null
) {
    val baseTint = accentColor ?: MaterialTheme.colorScheme.primary


    Canvas(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        val w = size.width
        val h = size.height

        // Sphere 1: Top Left
        val center1 = Offset(x = w * 0.15f, y = h * 0.2f)
        val radius1 = w * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to baseTint.copy(alpha = 0.22f),
                0.6f to baseTint.copy(alpha = 0.08f),
                1.0f to Color.Transparent,
                center = center1,
                radius = radius1
            ),
            center = center1,
            radius = radius1
        )

        // Sphere 2: Bottom Left
        val center2 = Offset(x = w * 0.2f, y = h * 0.75f)
        val radius2 = w * 0.5f
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to baseTint.copy(alpha = 0.18f),
                0.6f to baseTint.copy(alpha = 0.06f),
                1.0f to Color.Transparent,
                center = center2,
                radius = radius2
            ),
            center = center2,
            radius = radius2
        )

        // Sphere 3: Center Right
        val center3 = Offset(x = w * 0.85f, y = h * 0.5f)
        val radius3 = w * 0.65f
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to baseTint.copy(alpha = 0.25f),
                0.6f to baseTint.copy(alpha = 0.10f),
                1.0f to Color.Transparent,
                center = center3,
                radius = radius3
            ),
            center = center3,
            radius = radius3
        )
    }
}
