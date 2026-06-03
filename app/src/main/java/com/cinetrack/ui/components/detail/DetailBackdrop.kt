package com.cinetrack.ui.components.detail

import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cinetrack.ui.components.shared.ImagePlaceholder

/**
 * DetailBackdrop
 * High-fidelity backdrop with multi-step vertical gradient.
 * Replicates the React Native 'DetailBackdrop.tsx' logic.
 */
@Composable
fun DetailBackdrop(
    backdropPath: String?,
    posterPath: String?,
    accentColor: Color,
    backgroundColor: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    val path = backdropPath ?: posterPath
    // imageBaseUrl is no longer needed

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(520.dp)
            .background(backgroundColor)
    ) {
        if (path != null) {
            AsyncImage(
                model = buildTmdbImageUrl(path, ImageType.BACKDROP, LocalImageQuality.current),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ImagePlaceholder(
                isBackdrop = true,
                mediaType = "movie",
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. GRADIENTE PURO E NATIVO: Multi-step gradient for premium blending
        // Replicating React Native's: [0, 0.1, 0.35, 0.7, 0.9, 1] locations
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.3f to Color.Transparent,
                        0.5f to backgroundColor.copy(alpha = 0.3f),
                        0.7f to backgroundColor.copy(alpha = 0.7f),
                        0.85f to backgroundColor.copy(alpha = 0.9f),
                        1.0f to backgroundColor
                    )
                )
        )
    }
}
