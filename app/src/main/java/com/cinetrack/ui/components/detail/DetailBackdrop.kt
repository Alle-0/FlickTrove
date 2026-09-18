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
import coil.request.ImageRequest
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cinetrack.R

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer

/**
 * DetailBackdrop
 * High-fidelity backdrop with multi-step vertical gradient, atmospheric offline fallback,
 * and zero-cost 120 FPS hardware-accelerated parallax.
 */
@Composable
fun DetailBackdrop(
    scrollState: ScrollState? = null,
    backdropPath: String?,
    posterPath: String?,
    accentColor: Color,
    backgroundColor: Color = Color.Black,
    isOffline: Boolean = false,
    modifier: Modifier = Modifier
) {
    val path = backdropPath ?: posterPath

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(480.dp)
            .clipToBounds()
            .background(backgroundColor)
    ) {
        // Piano di profondità con Parallax GPU (Fallback + Immagine)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .let { m ->
                    if (scrollState != null) {
                        m.graphicsLayer {
                            val scroll = scrollState.value
                            if (scroll > 0) {
                                // L'immagine sale a metà velocità (0.5f) rispetto allo scroll del contenuto
                                translationY = scroll * 0.5f
                            } else if (scroll < 0) {
                                // Stretch elastico gommoso in overscroll pull-down
                                val stretch = 1f + (-scroll * 0.001f).coerceIn(0f, 0.25f)
                                scaleX = stretch
                                scaleY = stretch
                            }
                        }
                    } else {
                        m
                    }
                }
        ) {
            // 1. ATMOSPHERIC PREMIUM FALLBACK (Visibile durante il caricamento, offline o se l'immagine è assente)
            AtmosphericBackdropFallback(
                accentColor = accentColor,
                backgroundColor = backgroundColor,
                showCiack = (path == null)
            )

            // 2. BACKDROP IMAGE (Crossfade sopra il fallback quando connesso)
            if (path != null) {
                val imageUrl = buildTmdbImageUrl(path, ImageType.BACKDROP, LocalImageQuality.current)
                val context = LocalContext.current
                val request = remember(imageUrl, isOffline) {
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .crossfade(700)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 3. GRADIENTE PURO E NATIVO: Multi-step gradient for premium blending
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

@Composable
private fun AtmosphericBackdropFallback(
    accentColor: Color,
    backgroundColor: Color,
    showCiack: Boolean = false,
    modifier: Modifier = Modifier
) {
    val gradient = remember(accentColor, backgroundColor) {
        Brush.verticalGradient(
            0.0f to accentColor.copy(alpha = 0.45f),
            0.35f to accentColor.copy(alpha = 0.20f),
            0.65f to Color(0xFF161622),
            1.0f to backgroundColor
        )
    }
    val radialGlow = remember(accentColor) {
        Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.35f),
                Color.Transparent
            ),
            radius = 600f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
            .background(radialGlow),
        contentAlignment = Alignment.Center
    ) {
        if (showCiack) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape)
                    .border(1.5.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_ciack),
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}
