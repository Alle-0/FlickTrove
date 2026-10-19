package com.cinetrack.ui.components.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.Episode
import com.cinetrack.data.model.Season
import com.cinetrack.ui.utils.bounceClick

/**
 * Collapsible Season Accordion Card with Spring animation (Pro-Tip #2)
 * and Shimmer Skeleton Loading (Pro-Tip #3).
 */
@Composable
internal fun SeasonAccordionCard(
    season: Season,
    seasonData: Season?,
    isExpanded: Boolean,
    isFullyWatched: Boolean,
    localWatchedEpisodes: Map<String, List<Int>>,
    isDropped: Boolean,
    onHeaderClick: () -> Unit,
    onBatchToggle: () -> Unit,
    onBatchLongClick: () -> Unit,
    onEpisodeToggle: (Int) -> Unit,
    onEpisodeInfoClick: (Episode) -> Unit,
    fallbackBackdropPath: String? = null
) {
    val sNum = season.seasonNumber
    val totalEps = seasonData?.episodeCount?.takeIf { it > 0 }
        ?: season.episodeCount?.takeIf { it > 0 }
        ?: seasonData?.episodes?.size
        ?: 0
    val watchedList = localWatchedEpisodes[sNum.toString()] ?: emptyList()
    val watchedCount = watchedList.size

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "chevron_rotation"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isFullyWatched) Color(0xFF00E676).copy(alpha = 0.05f) else Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isFullyWatched) Color(0xFF00E676).copy(alpha = 0.20f)
            else if (isExpanded) Color.White.copy(alpha = 0.12f)
            else Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(scaleDown = 0.98f) { onHeaderClick() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chevron icon (rotates 0° -> 90° using project's standard ic_right)
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                    contentDescription = null,
                    tint = if (isFullyWatched) Color(0xFF00E676) else if (isExpanded) Color.White else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotationAngle)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Season Title
                Text(
                    text = if (sNum == 0) stringResource(R.string.episodes_specials) else stringResource(R.string.episodes_season_n, sNum),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Watched count pill badge
                Surface(
                    shape = CircleShape,
                    color = if (isFullyWatched) Color(0xFF00E676).copy(alpha = 0.15f)
                    else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(
                        1.dp,
                        if (isFullyWatched) Color(0xFF00E676).copy(alpha = 0.4f)
                        else Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Text(
                        text = "$watchedCount / $totalEps",
                        color = if (isFullyWatched) Color(0xFF00E676) else Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Quick Batch Season Checkmark: 28.dp Circle (matching EpisodeCard)
                Surface(
                    modifier = Modifier
                        .size(28.dp)
                        .bounceClick(
                            onLongClick = { onBatchLongClick() }
                        ) { onBatchToggle() },
                    color = if (isFullyWatched) Color(0xFF00E676) else Color.Transparent,
                    shape = CircleShape,
                    border = BorderStroke(
                        1.5.dp,
                        if (isFullyWatched) Color(0xFF00E676) else Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    if (isFullyWatched) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick),
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Expanded Episode List or Shimmer Skeleton with AnimatedVisibility (smooth spring slide & fade)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeOut(animationSpec = tween(150))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.05f),
                        thickness = 1.dp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (seasonData?.episodes != null) {
                            seasonData.episodes.forEach { episode ->
                                EpisodeCard(
                                    episode = episode,
                                    isWatched = watchedList.contains(episode.episodeNumber),
                                    onToggle = { onEpisodeToggle(episode.episodeNumber) },
                                    onInfoClick = { onEpisodeInfoClick(episode) },
                                    fallbackBackdropPath = fallbackBackdropPath
                                )
                            }
                        } else {
                            // Skeleton shimmer loader while season details are fetching
                            val skeletonCount = minOf(totalEps.coerceAtLeast(3), 4)
                            EpisodeSkeletonList(count = skeletonCount)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shimmer skeleton placeholder for episodes during lazy load (Pro-Tip #3).
 */
@Composable
private fun EpisodeSkeletonList(count: Int) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail placeholder (120x68 matching EpisodeCard)
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = alpha))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Text placeholders
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 60.dp, height = 10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = alpha * 0.7f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = alpha))
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Circle check placeholder (28.dp CircleShape)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color.White.copy(alpha = alpha * 0.5f), CircleShape)
                    )
                }
            }
        }
    }
}
