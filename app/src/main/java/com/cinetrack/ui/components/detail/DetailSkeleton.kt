package com.cinetrack.ui.components.detail

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
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
            // 1. Backdrop Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .shimmerEffect()
            )

            // Shift content up to overlap backdrop
            Column(
                modifier = Modifier.offset(y = (-140).dp)
            ) {
                // 2. Header Skeleton
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Title
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tagline
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Match Percentage Pill
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .width(72.dp)
                            .height(18.dp)
                            .clip(CircleShape)
                            .shimmerEffect()
                    )

                    // Fused Container (rating + year + runtime)
                    val containerShape = RoundedCornerShape(28.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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

                            // Year and Runtime
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .shimmerEffect()
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .size(4.dp)
                                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                }

                DetailSkeletonBottomPart()
            }
        }

        // Bottom Actions Skeleton
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

@Composable
fun DetailSkeletonBottomPart() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Overview lines
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (i == 3) 0.6f else 1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Genres row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(28.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Cast section title
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Cast row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}
