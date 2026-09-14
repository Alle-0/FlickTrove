package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.cinetrack.ui.components.shared.PersonCardSkeleton
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.theme.HazeStyles

@Composable
fun DetailSkeleton(
    hazeState: HazeState? = null,
    paddingValues: PaddingValues = PaddingValues()
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
            // 1. Backdrop Skeleton matching DetailBackdrop height with atmospheric gradient fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect()
                )
                // Multi-step gradient matching DetailBackdrop so text sits on dark fade
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.35f to Color.Transparent,
                                0.55f to Color.Black.copy(alpha = 0.4f),
                                0.75f to Color.Black.copy(alpha = 0.8f),
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
                    // Title
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tagline
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Match Percentage Pill
                    Box(
                        modifier = Modifier
                            .padding(bottom = 14.dp)
                            .width(86.dp)
                            .height(20.dp)
                            .clip(CircleShape)
                            .shimmerEffect()
                    )

                    // Fused Container (TMDB rating pill + dot + year + runtime)
                    val containerShape = RoundedCornerShape(28.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(containerShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), containerShape)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // TMDB Rating Pill
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(48.dp))
                                    .shimmerEffect()
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Year, Dot, Runtime
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .shimmerEffect()
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp)
                                        .size(4.dp)
                                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(56.dp)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(4.dp))
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
                    Spacer(modifier = Modifier.height(20.dp))

                    // Genres Pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(82.dp)
                                .height(32.dp)
                                .clip(CircleShape)
                                .shimmerEffect()
                        )
                        Box(
                            modifier = Modifier
                                .width(104.dp)
                                .height(32.dp)
                                .clip(CircleShape)
                                .shimmerEffect()
                        )
                        Box(
                            modifier = Modifier
                                .width(74.dp)
                                .height(32.dp)
                                .clip(CircleShape)
                                .shimmerEffect()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4 Overview text lines
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }

                // 5. DetailPersonalZone Skeleton (Vibe, Note, Rate action cards) (padding 24.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(36.dp))

                    // Section Title: "AREA PERSONALE"
                    Box(
                        modifier = Modifier
                            .width(112.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Personal Action cards row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                }

                // 6. DetailCast Skeleton (padding 24.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(36.dp))

                    // Section Title: "CAST"
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
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

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        // 7. Bottom Floating Action Dock Skeleton (matches DetailActions)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding() + 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .shimmerEffect()
            )
        }
    }
}
