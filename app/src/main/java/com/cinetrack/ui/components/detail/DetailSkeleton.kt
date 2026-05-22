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
                    .height(520.dp)
                    .shimmerEffect()
            )

            // Shift content up to overlap backdrop
            Column(
                modifier = Modifier
                    .offset(y = (-140).dp)
            ) {
                // 2. Header Skeleton (16dp padding)
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fused Container
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

                Spacer(modifier = Modifier.height(24.dp))

                // 3. MetaRows Skeleton (24dp padding)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Genres FlowRow
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.width(70.dp).height(28.dp).clip(RoundedCornerShape(20.dp)).shimmerEffect())
                        Box(modifier = Modifier.width(85.dp).height(28.dp).clip(RoundedCornerShape(20.dp)).shimmerEffect())
                        Box(modifier = Modifier.width(60.dp).height(28.dp).clip(RoundedCornerShape(20.dp)).shimmerEffect())
                    }

                    // Watch Providers Section
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // "DOVE GUARDARE" Label
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                        
                        // "IN STREAMING" Label
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            repeat(4) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 4. Plot Skeleton (24dp padding)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Box(modifier = Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Box(modifier = Modifier.fillMaxWidth(0.5f).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 5. Personal Zone Skeleton
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(72.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(CircleShape).shimmerEffect())
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(CircleShape).shimmerEffect())
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 6. Cast Skeleton
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.width(100.dp).height(24.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(4) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.size(64.dp).clip(CircleShape).shimmerEffect())
                                Box(modifier = Modifier.width(50.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))

                // 7. Technical Details Skeleton
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.width(150.dp).height(24.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
                                Box(modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 8. Trailers Skeleton
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.width(120.dp).height(24.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .size(width = 220.dp, height = 124.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 9. Recommendations & Collection Skeleton
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.width(140.dp).height(24.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 165.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(140.dp))
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
