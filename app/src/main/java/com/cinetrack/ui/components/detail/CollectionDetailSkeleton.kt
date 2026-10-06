package com.cinetrack.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    columns: Int = 3,
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
            // 1. Backdrop + Header in a single Box (mirrors real screen's first LazyColumn item)
            Box(modifier = Modifier.fillMaxWidth()) {
                // Backdrop shimmer (matches DetailBackdrop height = 480.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                        .shimmerEffect()
                )

                // Header content overlapping the backdrop (mirrors padding(top=340.dp) in real screen)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 340.dp)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Title skeleton — left-aligned, wide (mirrors headlineMedium fillMaxWidth title)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Badge skeleton ("4 FILM") — left-aligned pill (mirrors Box + CircleShape badge)
                    Box(
                        modifier = Modifier
                            .width(86.dp)
                            .height(26.dp)
                            .clip(CircleShape)
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Overview skeleton (4 lines) — left-aligned
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().height(15.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        Box(modifier = Modifier.fillMaxWidth().height(15.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        Box(modifier = Modifier.fillMaxWidth(0.82f).height(15.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        Box(modifier = Modifier.fillMaxWidth(0.50f).height(15.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    }
                }
            }

            // 2. Movies grid skeleton (mirrors LazyColumn items with verticalArrangement.spacedBy(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(columns) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                MovieCardSkeleton(
                                    width = Dp.Unspecified,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
