package com.cinetrack.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.components.shared.shimmerEffect

/**
 * Skeleton Loader premium per il Diario di FlickTrove.
 * Riflette fedelmente la struttura di HistoryTimelineView con la pillola mensile e le card dei film/serie.
 */
@Composable
fun HistoryTimelineSkeleton(
    topBarClearance: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Continuous lateral timeline track line spanning the entire screen height
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .width(22.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.08f))
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            userScrollEnabled = false
        ) {
            // Sticky Month Header Skeleton
            item {
                MonthTimelineHeaderSkeleton()
            }

            // List of Card Skeletons with lateral timeline track
            items(7) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lateral Timeline Node Skeleton
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .shimmerEffect()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 5.dp)
                ) {
                    HistoryCardSkeleton(
                        titleWidthFraction = when (index % 3) {
                            0 -> 0.65f
                            1 -> 0.45f
                            else -> 0.55f
                        },
                        subtitleWidthFraction = when (index % 2) {
                            0 -> 0.40f
                            else -> 0.32f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
}

@Composable
private fun MonthTimelineHeaderSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lateral Timeline Node Skeleton
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .shimmerEffect()
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Month Pill Skeleton
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .clip(CircleShape)
                .background(Color(0xFF14171E).copy(alpha = 0.8f))
                .border(0.8.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(115.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .shimmerEffect()
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
fun HistoryCardSkeleton(
    modifier: Modifier = Modifier,
    titleWidthFraction: Float = 0.55f,
    subtitleWidthFraction: Float = 0.35f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail placeholder (48.dp x 72.dp)
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF121216))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .shimmerEffect()
            )

            // Info Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Title placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(titleWidthFraction)
                        .height(15.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle placeholder: "Serie (2016) - 1 ep" / "Film (2008)"
                Box(
                    modifier = Modifier
                        .fillMaxWidth(subtitleWidthFraction)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(7.dp))

                // Badges placeholder row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Date badge pill (e.g. "20 ott")
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .shimmerEffect()
                    )

                    // Optional second badge pill (e.g. rating or rewatch)
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.06f))
                            .shimmerEffect()
                    )
                }
            }

            // More Options Icon placeholder
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .shimmerEffect()
            )
        }
    }
}
