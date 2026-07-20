package com.cinetrack.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.components.shared.MovieCardSkeleton
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun CollectionDetailSkeleton(
    cardWidth: Dp,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (hazeState != null) Modifier.haze(hazeState, style = HazeStyles.PremiumDark) else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Backdrop Skeleton matching DetailBackdrop height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .shimmerEffect()
            )

            // Shift content up by -140.dp to overlap backdrop
            Column(
                modifier = Modifier
                    .offset(y = (-140).dp)
                    .padding(bottom = 60.dp)
            ) {
                // Hero Header Section Skeleton
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Title skeleton
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Badge skeleton ("4 MOVIES")
                    Box(
                        modifier = Modifier
                            .width(86.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Overview skeleton (4 lines)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        Box(modifier = Modifier.fillMaxWidth(0.85f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        Box(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Movies inside collection grid skeleton
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(6) {
                        Box(modifier = Modifier.width(cardWidth)) {
                            MovieCardSkeleton(width = cardWidth)
                        }
                    }
                }
            }
        }
    }
}
