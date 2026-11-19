package com.cinetrack.ui.components.account

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.folder.FolderCoverFan
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.mirrorInRtl
import dev.chrisbanes.haze.HazeState

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
                    text = stringResource(R.string.dashboard_my_folders).uppercase(),
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
                    modifier = Modifier
                        .size(20.dp)
                        .mirrorInRtl()
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
                    text = stringResource(R.string.dashboard_no_folders_yet),
                    color = Color.White.copy(alpha = 0.7f),
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
            FolderCoverFan(
                posterPaths = topItems.map { it.posterPath },
                posterWidth = 38.dp,
                posterHeight = 56.dp,
                slotSpacing = 20.dp,
                offsetStepDp = 12f,
                cornerRadius = 8.dp,
                borderWidth = 1.dp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (topItems.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
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
            color = Color.White.copy(alpha = 0.70f)
        )
    }
}
