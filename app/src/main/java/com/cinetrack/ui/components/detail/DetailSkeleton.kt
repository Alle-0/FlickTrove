package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.ui.components.shared.PersonCardSkeleton
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.toComposeColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * DetailSkeleton
 * High-fidelity loading skeleton that mirrors MovieDetailScreenContent 1:1.
 * Supports optional preloaded metadata for instantaneous, seamless visual transition.
 */
@Composable
fun DetailSkeleton(
    hazeState: HazeState? = null,
    paddingValues: PaddingValues = PaddingValues(),
    preloadedTitle: String? = null,
    preloadedPosterPath: String? = null,
    preloadedBackdropPath: String? = null,
    preloadedAccentColor: String? = null
) {
    val themePrimary = MaterialTheme.colorScheme.primary
    val accentColor = remember(preloadedAccentColor, themePrimary) {
        preloadedAccentColor?.toComposeColor() ?: themePrimary
    }
    val backdropPath = preloadedBackdropPath ?: preloadedPosterPath

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (hazeState != null) Modifier.haze(hazeState, style = HazeStyles.PremiumDark) else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Backdrop Skeleton matching DetailBackdrop height (480dp) with atmospheric glow & multi-step fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .background(Color.Black)
            ) {
                // Atmospheric gradient fallback with vivid accent glow and subtle cinematic aura
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to accentColor.copy(alpha = 0.38f),
                                0.30f to accentColor.copy(alpha = 0.18f),
                                0.60f to Color(0xFF141520),
                                1.0f to Color.Black
                            )
                        )
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.28f),
                                    Color.Transparent
                                ),
                                radius = 700f
                            )
                        )
                )

                // Backdrop image (rendered if preloaded artwork exists)
                if (backdropPath != null) {
                    val imageUrl = buildTmdbImageUrl(backdropPath, ImageType.BACKDROP, LocalImageQuality.current)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .crossfade(400)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Shimmer over atmospheric fallback when no preloaded image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.05f))
                            .shimmerEffect()
                    )
                }

                // Exact multi-step fading gradient matching DetailBackdrop so text sits on deep dark fade
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.30f to Color.Transparent,
                                0.50f to Color.Black.copy(alpha = 0.30f),
                                0.70f to Color.Black.copy(alpha = 0.70f),
                                0.85f to Color.Black.copy(alpha = 0.90f),
                                1.0f to Color.Black
                            )
                        )
                )
            }

            // Shift content up by -140.dp to overlap backdrop (matches MovieDetailScreen layout)
            Column(
                modifier = Modifier
                    .offset(y = (-140).dp)
                    .padding(bottom = paddingValues.calculateBottomPadding() + 60.dp)
            ) {
                // 2. DetailHeader Skeleton (padding 16.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Title (Preloaded text or high-contrast skeleton)
                    if (!preloadedTitle.isNullOrBlank()) {
                        Text(
                            text = preloadedTitle,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 44.sp,
                                letterSpacing = (-1.5).sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.22f))
                                .shimmerEffect()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tagline skeleton
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.40f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Match Percentage Pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f))
                            .border(0.5.dp, accentColor.copy(alpha = 0.35f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(62.dp)
                                .height(11.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(accentColor.copy(alpha = 0.70f))
                                .shimmerEffect()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fused Container (TMDB rating pill + dot + year + runtime)
                    val containerShape = RoundedCornerShape(28.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(containerShape)
                            .background(Color(0xFF1A1A1D).copy(alpha = 0.55f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), containerShape)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // TMDB Rating Pill (matches RatingPill)
                            Box(
                                modifier = Modifier
                                    .width(84.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(48.dp))
                                    .background(accentColor.copy(alpha = 0.90f))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.Black.copy(alpha = 0.35f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.35f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.Black.copy(alpha = 0.45f))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Year • Runtime
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .shimmerEffect()
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp)
                                        .size(4.dp)
                                        .background(Color.White.copy(alpha = 0.35f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(52.dp)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                }

                // 3. DetailMetaRows Skeleton (Genres Flow & Watch Providers) (padding 24.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Genres Pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(82.dp, 104.dp, 76.dp).forEach { pillWidth ->
                            Box(
                                modifier = Modifier
                                    .width(pillWidth)
                                    .height(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                    .shimmerEffect()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Streaming / Watch Providers icons row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                }

                // 4. DetailInfo Skeleton (Plot / Overview) (padding 24.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(28.dp))

                    // Section Title: "TRAMA"
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.35f))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4 Overview text lines
                    listOf(1.0f, 0.95f, 0.88f, 0.55f).forEach { fraction ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(13.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // 5. DetailPersonalZone Skeleton (Area Personale) (padding 24.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    // Section Title: "AREA PERSONALE"
                    Box(
                        modifier = Modifier
                            .width(116.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.35f))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 1: Vibe + Note (2 cards)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .shimmerEffect()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Rate (1 wide card)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .shimmerEffect()
                    )
                }

                // 6. DetailComments Skeleton (padding 24.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    // Header: "COMMENTI" on left + "SCRIVI" pill on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.35f))
                                .shimmerEffect()
                        )
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(26.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(0.5.dp, accentColor.copy(alpha = 0.30f), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Comment card placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                            .shimmerEffect()
                    )
                }

                // 7. DetailCast Skeleton (padding 24.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    // Section Title: "CAST"
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.35f))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cast Avatars Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(4) {
                            PersonCardSkeleton(width = 76.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }

        // 8. Bottom Floating Action Dock Skeleton (matches DetailActions dual-button dock)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding() + 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Action Pill (Watchlist / Visto)
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF14151E).copy(alpha = 0.90f))
                        .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.65f))
                        )
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(13.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .shimmerEffect()
                        )
                    }
                }

                // Side Circular Action Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF14151E).copy(alpha = 0.90f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.30f))
                    )
                }
            }
        }
    }
}
