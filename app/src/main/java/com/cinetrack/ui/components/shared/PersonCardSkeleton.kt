package com.cinetrack.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PersonCardSkeleton(
    width: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(width),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular Avatar Shimmer matching PersonCard
        Box(
            modifier = Modifier
                .size(width)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Name Shimmer (2 lines max in PersonCard, height matching ~28dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(11.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Department/Role Shimmer (1 line max in PersonCard)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(9.dp)
                .clip(RoundedCornerShape(3.dp))
                .shimmerEffect()
        )
    }
}
