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
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.components.shared.MovieCardSkeleton
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * DetailSkeleton
 * Sleek, neutral monochrome loading skeleton that mirrors MovieDetailScreenContent 1:1.
 * Pure shimmer wireframe placeholder for seamless loading without visual frankenstein effects.
 */
@Composable
fun DetailSkeleton(
    hazeState: HazeState? = null,
    paddingValues: PaddingValues = PaddingValues(),
    preloadedTitle: String? = null,
    preloadedPosterPath: String? = null,
    preloadedBackdropPath: String? = null,
    preloadedAccentColor: String? = null,
    isTv: Boolean = false
) {
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
            // 1. Backdrop Skeleton matching DetailBackdrop height (480dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .background(Color.Black)
            ) {
                // Neutral deep slate atmospheric background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color(0xFF1A1C28).copy(alpha = 0.5f),
                                0.30f to Color(0xFF141522).copy(alpha = 0.3f),
                                0.60f to Color(0xFF10111A),
                                1.0f to Color.Black
                            )
                        )
                )

                // Pure shimmer backdrop — neutral monochrome
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.05f))
                        .shimmerEffect()
                )

                // Exact multi-step fading gradient matching DetailBackdrop
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
                    // Title skeleton bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tagline skeleton
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.40f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row with Match Percentage Pill, Trailer Action & Cover Action
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Match Percentage Pill (Neutral monochrome)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.07f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(68.dp)
                                    .height(11.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .shimmerEffect()
                            )
                        }

                        // Trailer Action skeleton
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .shimmerEffect()
                            )
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(11.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .shimmerEffect()
                            )
                        }

                        // Cover action skeleton
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .shimmerEffect()
                            )
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .shimmerEffect()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fused Container matching DetailHeader (TMDB Score + Rated Badge + Tech Specs + Metadata)
                    val containerShape = RoundedCornerShape(28.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(containerShape)
                            .background(Color(0xFF1A1A1D).copy(alpha = 0.50f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), containerShape)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: TMDB Rating Pill + Rated Pill + Technical Specs Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // TMDB Rating Pill
                                Box(
                                    modifier = Modifier
                                        .width(76.dp)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(48.dp))
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .shimmerEffect()
                                )

                                // Rated Certification Pill (e.g. PG / 14+)
                                Box(
                                    modifier = Modifier
                                        .width(52.dp)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(48.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .shimmerEffect()
                                )

                                // Technical Specs Circle (matching HeaderTechnicalPill)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .shimmerEffect()
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            if (isTv) {
                                // TV Series: 2 rows (Year • Seasons, and Series Status)
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    // Line 1: Year • Seasons
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(34.dp)
                                                .height(11.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color.White.copy(alpha = 0.06f))
                                                .shimmerEffect()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .size(3.5.dp)
                                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(52.dp)
                                                .height(11.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color.White.copy(alpha = 0.06f))
                                                .shimmerEffect()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    // Line 2: Status placeholder (e.g. RETURNING SERIES / ENDED)
                                    Box(
                                        modifier = Modifier
                                            .width(68.dp)
                                            .height(9.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color.White.copy(alpha = 0.04f))
                                            .shimmerEffect()
                                    )
                                }
                            } else {
                                // Movies: Single line (Year • Runtime)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color.White.copy(alpha = 0.06f))
                                            .shimmerEffect()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .size(3.5.dp)
                                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(52.dp)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color.White.copy(alpha = 0.06f))
                                            .shimmerEffect()
                                    )
                                }
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
                        listOf(82.dp, 104.dp, 76.dp, 92.dp).forEach { pillWidth ->
                            Box(
                                modifier = Modifier
                                    .width(pillWidth)
                                    .height(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, Color.White.copy(alpha = 0.07f), CircleShape)
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
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
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
                            .background(Color.White.copy(alpha = 0.08f))
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
                                .background(Color.White.copy(alpha = 0.05f))
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
                            .background(Color.White.copy(alpha = 0.08f))
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
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
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
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
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
                                .background(Color.White.copy(alpha = 0.08f))
                                .shimmerEffect()
                        )
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(26.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.07f), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Comment card placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .shimmerEffect()
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .shimmerEffect()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
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
                            .background(Color.White.copy(alpha = 0.08f))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cast Avatars Row matching PersonCard 1:1 (64dp circles with name/character text below)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(5) {
                            Column(
                                modifier = Modifier.width(64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                        .shimmerEffect()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(52.dp)
                                        .height(9.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .shimmerEffect()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                }

                // 8. DetailRecommendations / Similar Titles Skeleton (padding 24.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    // Section Title: "TITOLI SIMILI"
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(4) {
                            MovieCardSkeleton(width = 110.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }

        // 9. Bottom Floating Action Dock Skeleton (Matching DetailActions: Main Pill + Trash/Action Circle)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding() + 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Watch Action Pill (expanded width)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF14151E).copy(alpha = 0.85f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                        .shimmerEffect()
                )

                // Secondary Action Button (56dp circle matching trash/rewatch button)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF14151E).copy(alpha = 0.85f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        .shimmerEffect()
                )
            }
        }
    }
}
