package com.cinetrack.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.components.shared.MovieCardSkeleton
import com.cinetrack.ui.components.shared.shimmerEffect

fun LazyListScope.homeFeedSkeletons() {
    item {
        // Hero Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(500.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0xFF1A1A2E))
                .shimmerEffect()
        )
    }

    item {
        // Standard Row Skeleton
        Column {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .width(160.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A2E))
                    .shimmerEffect()
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(4) {
                    MovieCardSkeleton(width = 110.dp)
                }
            }
        }
    }

    item {
        // Trove Pick Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 24.dp)
                .height(260.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0xFF1A1A2E))
                .shimmerEffect()
        )
    }

    items(2) {
        Column {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .width(140.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A2E))
                    .shimmerEffect()
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(4) {
                    MovieCardSkeleton(width = 110.dp)
                }
            }
        }
    }
}
