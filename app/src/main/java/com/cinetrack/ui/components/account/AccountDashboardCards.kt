package com.cinetrack.ui.components.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import com.cinetrack.R
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.CalculatedStats
import dev.chrisbanes.haze.HazeState
import androidx.compose.foundation.border
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.*

@Composable
fun GeneralStatsCard(
    stats: CalculatedStats?,
    hazeState: HazeState,
    backgroundLuminance: Float = 0f,
    onClick: () -> Unit
) {
    val cardOverlay = if (backgroundLuminance > 0.35f) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f)
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
                    text = stringResource(id = R.string.dashboard_general_stats),
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
                    modifier = Modifier.size(20.dp)
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
                val avgRating = if (totalRatingsCount > 0) String.format(java.util.Locale.US, "%.1f/10", totalRatingScore / totalRatingsCount) else "N/A"
                val moviesCount = stats.moviesWatched
                val seriesCount = stats.tvWatched
                val ratioText = if (moviesCount == 0 && seriesCount == 0) "0/0" else {
                    val total = moviesCount + seriesCount
                    val moviePct = (moviesCount * 100) / total
                    val seriesPct = 100 - moviePct
                    "$moviePct/$seriesPct"
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatItem(label = stringResource(R.string.stat_hours_watched), value = stats.totalTimeFormatted)
                        StatItem(label = stringResource(R.string.stat_completed_titles), value = (moviesCount + seriesCount).toString())
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatItem(label = stringResource(R.string.stat_avg_rating), value = avgRating)
                        StatItem(label = stringResource(R.string.stat_movies_vs_series), value = ratioText)
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
            color = Color.White.copy(alpha = 0.6f)
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
private fun SkeletonFolderItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_folder")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color.White.copy(alpha = alpha), RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(12.dp)
                .background(Color.White.copy(alpha = alpha), RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun MyFoldersCard(
    folders: List<FolderEntity>?,
    allMovies: List<Movie>,
    hazeState: HazeState,
    backgroundLuminance: Float = 0f,
    onViewAllClick: () -> Unit,
    onFolderClick: (FolderEntity) -> Unit
) {
    val cardOverlay = if (backgroundLuminance > 0.35f) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f)
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { onViewAllClick() }
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cartella),
                    contentDescription = "My Folders",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.dashboard_my_folders),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    painter = painterResource(id = R.drawable.ic_right),
                    contentDescription = "View All Folders",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Horizontal list of folders
            if (folders == null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    SkeletonFolderItem()
                    SkeletonFolderItem()
                    SkeletonFolderItem()
                    SkeletonFolderItem()
                }
            } else if (folders.isEmpty()) {
                Text(
                    "You haven't created any folders yet.",
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    0.05f to Color.Black,
                                    0.95f to Color.Black,
                                    1f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    items(folders) { folder ->
                        FolderPreviewItem(
                            folder = folder,
                            allMovies = allMovies,
                            onClick = { onFolderClick(folder) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FolderPreviewItem(
    folder: FolderEntity,
    allMovies: List<Movie>,
    onClick: () -> Unit
) {
    val folderColorInt = try {
        if (!folder.color.isNullOrBlank()) android.graphics.Color.parseColor(folder.color)
        else android.graphics.Color.WHITE
    } catch (e: Exception) {
        android.graphics.Color.WHITE
    }
    val fColor = Color(folderColorInt)

    val topItems = remember(folder.itemIds, allMovies) {
        folder.itemIds.take(3).mapNotNull { id ->
            allMovies.find { "${it.mediaType}_${it.id}" == id }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .bounceClick { onClick() }
            .width(82.dp)
    ) {
        if (topItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(fColor.copy(alpha = 0.15f))
                    .border(1.dp, fColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cartella),
                    contentDescription = folder.name,
                    tint = fColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(38.dp + ((topItems.size - 1) * 20).dp)
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                topItems.forEachIndexed { index, movie ->
                    val centerIndex = (topItems.size - 1) / 2f
                    val currentRotation = if (topItems.size <= 1) 0f else {
                        val maxRotation = 15f
                        val rotationStep = (maxRotation * 2) / (topItems.size - 1)
                        -maxRotation + (index * rotationStep)
                    }
                    val xOffset = (index - centerIndex) * 12f

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset.dp)
                            .size(width = 38.dp, height = 56.dp)
                            .graphicsLayer {
                                rotationZ = currentRotation
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.1f)
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF13151A), RoundedCornerShape(8.dp))
                            .zIndex((topItems.size - index).toFloat())
                    ) {
                        coil.compose.AsyncImage(
                            model = "https://image.tmdb.org/t/p/w200${movie.posterPath}",
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (topItems.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(fColor)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Text(
            text = "(${folder.itemIds.size})",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun YourFlowCard(
    hazeState: HazeState,
    backgroundLuminance: Float = 0f,
    onFlowClick: () -> Unit,
    onFlowStatsClick: () -> Unit
) {
    val cardOverlay = if (backgroundLuminance > 0.35f) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f)
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
                    painter = painterResource(id = R.drawable.ic_sparkle),
                    contentDescription = "Your Flow",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.dashboard_your_flow),
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
                    modifier = Modifier.size(20.dp)
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
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
                
                // Action Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .bounceClick { onFlowStatsClick() },
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
