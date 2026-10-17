package com.cinetrack.ui.components.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.toComposeColor

@Composable
fun FolderCard(
    folder: FolderEntity,
    allMovies: List<com.cinetrack.data.model.Movie>,
    onClick: () -> Unit,
    onLongClick: (Rect) -> Unit
) {
    val folderColor = folder.color.toComposeColor()
    val bounds = remember { arrayOf(Rect.Zero) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds[0] = it.boundsInWindow() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(
                    scaleDown = 0.97f,
                    onLongClick = { onLongClick(bounds[0]) },
                    onClick = onClick
                )
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Colored dot (circle) with icon
        val topItems = remember(folder.itemIds, allMovies) {
            folder.itemIds.take(3).mapNotNull { id ->
                allMovies.find { "${it.mediaType}_${it.id}" == id }
            }
        }

        if (topItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(folderColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                    contentDescription = null,
                    tint = folderColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(40.dp + ((topItems.size - 1) * 22).dp)
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                topItems.forEachIndexed { index, movie ->
                    val centerIndex = (topItems.size - 1) / 2f
                    val currentRotation = if (topItems.size <= 1) 0f else {
                        val maxRotation = 15f
                        val rotationStep = (maxRotation * 2) / (topItems.size - 1)
                        -maxRotation + (index * rotationStep)
                    }
                    val xOffset = (index - centerIndex) * 14f
                    
                    Box(
                        modifier = Modifier
                            .offset(x = xOffset.dp)
                            .size(width = 40.dp, height = 60.dp)
                            .graphicsLayer {
                                rotationZ = currentRotation
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.1f)
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0xFF13151A), RoundedCornerShape(10.dp))
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
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (topItems.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(folderColor)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            val formatter = remember { java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.getDefault()) }
            val formattedDate = remember(folder.createdAt) {
                try {
                    val instant = java.time.Instant.parse(folder.createdAt)
                    java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()).format(formatter)
                } catch (e: Exception) {
                    ""
                }
            }
            if (!folder.description.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = folder.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else if (formattedDate.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val countStr = folder.itemIds.size.toString()
            val fontSize = if (countStr.length >= 3) 10.sp else 12.sp
            Text(
                text = countStr,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize
                ),
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        
        Spacer(Modifier.width(8.dp))
        
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp)
        )
        }
    }
}

@Composable
fun NewFolderCard(onClick: () -> Unit, modifier: Modifier = Modifier, alphaProgress: Float = 1f) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.97f) { onClick() }
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        if (alphaProgress > 0f) {
            Box(modifier = Modifier
                .wrapContentHeight(unbounded = true)
                .graphicsLayer { alpha = alphaProgress }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_plus),
                    null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(Modifier.width(12.dp))
                
                Text(
                    text = stringResource(R.string.folders_new_folder),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    }
}


