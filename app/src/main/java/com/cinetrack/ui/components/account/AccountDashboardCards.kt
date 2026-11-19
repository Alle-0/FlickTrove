package com.cinetrack.ui.components.account

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.mirrorInRtl
import com.cinetrack.ui.viewmodel.CalculatedStats
import dev.chrisbanes.haze.HazeState

@Composable
fun GeneralStatsCard(
    stats: CalculatedStats?,
    hazeState: HazeState,
    backgroundLuminance: Float = 0f,
    onClick: () -> Unit
) {
    val cardOverlay = if (backgroundLuminance > 0.38f) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .hazeGlass(
                    state = hazeState,
                    shape = RoundedCornerShape(32.dp),
                    containerColor = cardOverlay,
                    borderColor = Color.White.copy(alpha = 0.1f),
                    borderWidth = 1.dp,
                    useOffscreenStrategy = false
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { onClick() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stat),
                    contentDescription = "General Stats",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.dashboard_general_stats).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    painter = painterResource(id = R.drawable.ic_right),
                    contentDescription = "View Stats",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .mirrorInRtl()
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats Content
            if (stats != null) {
                // Calculate average rating
                var totalRatingScore = 0.0
                var totalRatingsCount = 0
                stats.ratingDistribution.forEachIndexed { index, count ->
                    val bucketScore = (index + 1) * 0.5
                    totalRatingScore += bucketScore * count
                    totalRatingsCount += count
                }
                val avgRating = if (totalRatingsCount > 0) {
                    String.format(java.util.Locale.US, "%.1f/10", totalRatingScore / totalRatingsCount)
                } else "N/A"
                val moviesCount = stats.moviesWatched
                val seriesCount = stats.tvWatched
                val ratioText = if (moviesCount == 0 && seriesCount == 0) "0% / 0%" else {
                    val total = moviesCount + seriesCount
                    val moviePct = (moviesCount * 100) / total
                    val seriesPct = 100 - moviePct
                    "$moviePct% / $seriesPct%"
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatItem(
                            label = stringResource(R.string.stat_hours_watched),
                            value = stats.totalTimeFormatted
                        )
                        StatItem(
                            label = stringResource(R.string.stat_completed_titles),
                            value = (moviesCount + seriesCount).toString()
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatItem(
                            label = stringResource(R.string.stat_avg_rating),
                            value = avgRating
                        )
                        StatItem(
                            label = stringResource(R.string.stat_movies_vs_series),
                            value = ratioText
                        )
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SkeletonStatItem()
                        SkeletonStatItem()
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SkeletonStatItem()
                        SkeletonStatItem()
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.72f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

@Composable
private fun SkeletonStatItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.width(90.dp).height(14.dp).background(Color.White.copy(alpha = alpha), RoundedCornerShape(4.dp)))
        Box(modifier = Modifier.width(50.dp).height(20.dp).background(Color.White.copy(alpha = alpha), RoundedCornerShape(4.dp)))
    }
}

@Composable
fun YourFlowCard(
    hazeState: HazeState,
    backgroundLuminance: Float = 0f,
    onFlowClick: () -> Unit,
    onFlowStatsClick: () -> Unit
) {
    val cardOverlay = if (backgroundLuminance > 0.38f) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .hazeGlass(
                    state = hazeState,
                    shape = RoundedCornerShape(32.dp),
                    containerColor = cardOverlay,
                    borderColor = Color.White.copy(alpha = 0.1f),
                    borderWidth = 1.dp,
                    useOffscreenStrategy = false
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { onFlowClick() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_film),
                    contentDescription = "Your Flow",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.dashboard_your_flow).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    painter = painterResource(id = R.drawable.ic_right),
                    contentDescription = "Play Flow",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .mirrorInRtl()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dashboard_your_flow_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f)
                )
                
                // Action Pill
                Row(
                    modifier = Modifier
                        .bounceClick(scaleDown = 0.90f) { onFlowStatsClick() }
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stats Button ONLY
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_stat),
                            contentDescription = "Flow Stats",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
