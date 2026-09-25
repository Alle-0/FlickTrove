package com.cinetrack.ui.components.boxoffice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.viewmodel.BoxOfficeCategory
import com.cinetrack.ui.components.shared.shimmerEffect

/**
 * Skeleton estensione per il LazyColumn del BoxOfficeScreen:
 * - In modalità WEEKEND: Hero card #1 skeleton + 6 righe skeleton (#2 - #7)
 * - In modalità ALL_TIME: 10 righe ranked skeleton (#1 - #10)
 */
fun LazyListScope.boxOfficeSkeletons(activeTab: BoxOfficeCategory) {
    if (activeTab == BoxOfficeCategory.WEEKEND) {
        item(key = "skeleton_hero") {
            BoxOfficeHeroSkeleton()
        }
        items(6, key = { "skeleton_row_$it" }) {
            BoxOfficeRowSkeleton()
        }
    } else {
        items(10, key = { "skeleton_at_$it" }) {
            BoxOfficeRowSkeleton()
        }
    }
}

/**
 * Skeleton fedele a [BoxOfficeHeroWinnerCard]:
 * card arrotondata a 26dp con shimmer e gradient scuro
 */
@Composable
fun BoxOfficeHeroSkeleton(
    modifier: Modifier = Modifier
) {
    val heroCardShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(heroCardShape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), heroCardShape)
    ) {
        // Backdrop shimmer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Crown pill placeholder top-left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(CircleShape)
                .width(110.dp)
                .height(24.dp)
                .shimmerEffect()
        )

        // Bottom content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.60f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .width(72.dp)
                        .height(24.dp)
                        .shimmerEffect()
                )
            }
        }
    }
}

/**
 * Skeleton fedele a [BoxOfficeMovieRow]:
 * cerchio rank | thumbnail 48×70dp | colonna titolo+anno+rating | pillola revenue
 */
@Composable
fun BoxOfficeRowSkeleton(
    modifier: Modifier = Modifier
) {
    val rowShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), rowShape)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge circle (neutro monocromatico)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .shimmerEffect()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Poster thumbnail
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title + year + rating column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(15.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                // Year + rating line
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .shimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .shimmerEffect()
                    )
                }
            }

            // Revenue pill placeholder
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .width(64.dp)
                    .height(26.dp)
                    .shimmerEffect()
            )
        }
    }
}
