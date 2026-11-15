package com.cinetrack.ui.components.comments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun CommentAttachedMediaRow(
    attachedMedia: List<String>,
    onAttachedMediaChanged: (List<String>) -> Unit,
    onRemoveMedia: ((String) -> Unit)? = null,
    imageLoader: coil3.ImageLoader,
    modifier: Modifier = Modifier
) {
    if (attachedMedia.isEmpty()) return

    val context = LocalContext.current

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(attachedMedia.size) { index ->
            val url = attachedMedia[index]
            val itemHazeState = remember { HazeState() }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(url).build(),
                    imageLoader = imageLoader,
                    contentDescription = "Attachment",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(itemHazeState)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .bounceClick(scaleDown = 0.88f) {
                            onRemoveMedia?.invoke(url)
                            onAttachedMediaChanged(attachedMedia.filter { it != url })
                        }
                        .hazeGlass(
                            state = itemHazeState,
                            shape = CircleShape,
                            containerColor = Color.Black.copy(alpha = 0.50f),
                            blurRadius = 10.dp,
                            borderColor = Color.White.copy(alpha = 0.20f),
                            borderWidth = 1.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_x),
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}
