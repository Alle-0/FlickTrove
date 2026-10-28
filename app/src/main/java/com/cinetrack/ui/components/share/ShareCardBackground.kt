package com.cinetrack.ui.components.share

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * Handles the background rendering for the share story card, including
 * full-bleed backdrops, blurred atmospheric posters, and directional gradients.
 */
@Composable
internal fun ShareCardBackground(
    coverStyle: ShareCoverStyle,
    aspectRatio: ShareAspectRatio,
    backdropUrl: String?,
    posterUrl: String?,
    cardHazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AnimatedContent(
        targetState = coverStyle,
        transitionSpec = {
            fadeIn(animationSpec = tween(320)) togetherWith fadeOut(animationSpec = tween(220))
        },
        label = "CardBgTransition",
        modifier = modifier.fillMaxSize()
    ) { currentCoverStyle ->
        when (currentCoverStyle) {
            ShareCoverStyle.BACKDROP_FULL -> {
                // Full-bleed backdrop with dark gradient and Haze capture
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = cardHazeState, style = HazeStyles.PremiumDark)
                ) {
                    if (backdropUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(backdropUrl)
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (aspectRatio == ShareAspectRatio.LANDSCAPE_16_9) {
                                    Brush.horizontalGradient(
                                        0.0f to Color(0xFF09090B).copy(alpha = 0.78f),
                                        0.38f to Color(0xFF09090B).copy(alpha = 0.35f),
                                        0.60f to Color.Transparent,
                                        1.0f to Color.Black.copy(alpha = 0.15f)
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.28f),
                                        0.08f to Color.Transparent,
                                        0.50f to Color.Transparent,
                                        0.70f to Color(0xFF09090B).copy(alpha = 0.45f),
                                        0.88f to Color(0xFF09090B).copy(alpha = 0.78f),
                                        1.0f to Color(0xFF09090B).copy(alpha = 0.92f)
                                    )
                                }
                            )
                    )
                }
            }
            ShareCoverStyle.POSTER_FLOATING -> {
                // Atmospheric backdrop in background with Haze capture
                val isLandscapeRatio = aspectRatio == ShareAspectRatio.LANDSCAPE_16_9
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = cardHazeState, style = HazeStyles.PremiumDark)
                ) {
                    val bgUrl = backdropUrl ?: posterUrl
                    if (bgUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(bgUrl)
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(if (isLandscapeRatio) 6.dp else 28.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isLandscapeRatio) {
                                    Brush.horizontalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.32f),
                                        0.40f to Color(0xFF09090B).copy(alpha = 0.58f),
                                        1.0f to Color(0xFF09090B).copy(alpha = 0.82f)
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.65f),
                                        0.5f to Color(0xFF09090B).copy(alpha = 0.88f),
                                        1.0f to Color(0xFF09090B)
                                    )
                                }
                            )
                    )
                }
            }
        }
    }
}

