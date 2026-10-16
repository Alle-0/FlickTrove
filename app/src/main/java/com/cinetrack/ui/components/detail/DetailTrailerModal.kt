package com.cinetrack.ui.components.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import coil.compose.AsyncImage
import com.cinetrack.ui.components.shared.MorphGlassModal
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.horizontalFadingEdges
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import dev.chrisbanes.haze.haze
import com.cinetrack.ui.theme.HazeStyles
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.data.api.Video
import dev.chrisbanes.haze.HazeState

/**
 * Overlay modal that plays the trailer in-place without navigating away
 * or cluttering the vertical scroll of the detail screen.
 * Wrapped in MorphGlassModal for bouncy expanding morph animation with real-time haze blur,
 * custom video cover with dark gradient & duration, category selector, and 16:9 preview cards with bounceClick feedback.
 */
@Composable
fun DetailTrailerModal(
    isVisible: Boolean = true,
    videos: List<Video>,
    accentColor: Color,
    hazeState: HazeState? = null,
    triggerBounds: Rect? = null,
    onDismiss: () -> Unit
) {
    if (videos.isEmpty()) return

    val categoryOrder = remember {
        listOf("Trailer", "Teaser", "Clip", "Featurette", "Behind the Scenes", "Bloopers", "Opening Credits", "Recap")
    }
    val availableCategories = remember(videos) {
        videos.map { it.type }.distinct().filter { it.isNotBlank() }
            .sortedBy { categoryOrder.indexOf(it).takeIf { idx -> idx != -1 } ?: 99 }
    }

    var selectedCategory by remember(availableCategories) {
        mutableStateOf(
            availableCategories.firstOrNull { it.equals("Trailer", ignoreCase = true) }
                ?: availableCategories.firstOrNull()
                ?: "Trailer"
        )
    }

    val currentCategoryVideos = remember(videos, selectedCategory) {
        val filtered = videos.filter { it.type.equals(selectedCategory, ignoreCase = true) }
        if (filtered.isNotEmpty()) filtered else videos
    }

    // Default to the first video of the active category
    val defaultVideo = remember(currentCategoryVideos) {
        currentCategoryVideos.firstOrNull() ?: videos.first()
    }
    var currentVideo by remember(videos) { mutableStateOf(defaultVideo) }
    var isPlaying by remember(currentVideo.key) { mutableStateOf(false) }

    // When category changes, switch current video to the first of that category if not currently in it
    LaunchedEffect(selectedCategory) {
        if (currentCategoryVideos.none { it.key == currentVideo.key }) {
            currentCategoryVideos.firstOrNull()?.let { currentVideo = it }
        }
    }

    var categoryMenuData by remember { mutableStateOf<TrailersMenuModalData?>(null) }
    var pillButtonCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val modalHazeState = remember { HazeState() }

    BackHandler(enabled = categoryMenuData != null) {
        categoryMenuData = null
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            isPlaying = false
            categoryMenuData = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100000f)
    ) {
        MorphGlassModal(
            isVisible = isVisible,
            onDismissRequest = onDismiss,
            triggerBounds = triggerBounds,
            hazeState = hazeState,
            targetMaxWidth = 560.dp,
            targetWidthFraction = 0.94f,
            maxModalHeightFraction = 0.88f,
            targetCornerRadius = 28.dp,
            zIndex = 1000f
        ) { contentAlpha ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .haze(state = modalHazeState, style = HazeStyles.PremiumDark)
                    .padding(18.dp)
            ) {
                // Header: Title and Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_play),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = currentVideo.name.ifBlank { stringResource(R.string.detail_official_trailer) },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    ModalCloseButton(onClick = onDismiss)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Player 16:9 with custom cover (gradient, title, duration) before play
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .bounceClick(scaleDown = if (!isPlaying) 0.98f else 1f) {
                            if (!isPlaying) isPlaying = true
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    if (isPlaying) {
                        key(currentVideo.key) {
                            YouTubePlayer(
                                videoId = currentVideo.key,
                                autoPlay = true
                            )
                        }
                    } else {
                        TrailerThumbnail(
                            video = currentVideo,
                            accentColor = accentColor
                        )
                    }
                }

                // Category Dropdown Button (if more than 1 category exists)
                if (availableCategories.size > 1) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                pillButtonCoords = coords
                            }
                            .bounceClick(scaleDown = 0.94f) {
                                val liveBounds = pillButtonCoords?.boundsInRoot()
                                if (liveBounds != null) {
                                    val counts = availableCategories.associateWith { cat ->
                                        videos.count { it.type.equals(cat, ignoreCase = true) }
                                    }
                                    categoryMenuData = TrailersMenuModalData(
                                        triggerBounds = liveBounds,
                                        availableCategories = availableCategories,
                                        categoryCounts = counts,
                                        selectedCategory = selectedCategory,
                                        onSelectCategory = { newCat ->
                                            selectedCategory = newCat
                                            videos.firstOrNull { it.type.equals(newCat, ignoreCase = true) }?.let {
                                                currentVideo = it
                                                isPlaying = true
                                            }
                                        }
                                    )
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val count = videos.count { it.type.equals(selectedCategory, ignoreCase = true) }
                            val catLabel = when (selectedCategory) {
                                "Trailer" -> stringResource(R.string.detail_video_pill_trailer)
                                "Teaser" -> stringResource(R.string.detail_video_pill_teaser)
                                "Clip" -> stringResource(R.string.detail_video_pill_clip)
                                "Featurette" -> stringResource(R.string.detail_video_pill_featurette)
                                "Behind the Scenes" -> stringResource(R.string.detail_video_pill_bts)
                                "Bloopers" -> stringResource(R.string.detail_video_pill_bloopers)
                                "Opening Credits" -> stringResource(R.string.detail_video_pill_credits)
                                "Recap" -> stringResource(R.string.detail_video_pill_recap)
                                else -> selectedCategory.uppercase()
                            }
                            Text(
                                text = "$catLabel ($count)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.2.sp
                                ),
                                color = accentColor
                            )
                            val arrowRotation by animateFloatAsState(
                                targetValue = if (categoryMenuData != null) -90f else 90f,
                                label = "arrowRotation"
                            )
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier
                                    .size(12.dp)
                                    .graphicsLayer { rotationZ = arrowRotation }
                            )
                        }
                    }
                }

                // Carousel of 16:9 video preview cards with thumbnails, gradients, titles and durations
                if (currentCategoryVideos.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val videoRowState = rememberLazyListState()
                    LazyRow(
                        state = videoRowState,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalFadingEdges(
                                lazyListState = videoRowState,
                                leftEdgeWidth = 20.dp,
                                rightEdgeWidth = 20.dp
                            )
                    ) {
                        items(currentCategoryVideos, key = { it.key }) { video ->
                            val isSelected = video.key == currentVideo.key

                            Box(
                                modifier = Modifier
                                    .width(130.dp)
                                    .aspectRatio(16f / 9f)
                                    .bounceClick(scaleDown = 0.94f) {
                                        currentVideo = video
                                        isPlaying = true
                                    }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = "https://img.youtube.com/vi/${video.key}/mqdefault.jpg",
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Bottom black gradient for readability
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.45f),
                                                    Color.Black.copy(alpha = 0.90f)
                                                )
                                            )
                                        )
                                )

                                // Title text
                                Text(
                                    text = video.name.ifBlank { video.type },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 9.sp,
                                        lineHeight = 11.sp
                                    ),
                                    color = if (isSelected) ColorUtils.lightenForText(accentColor, 1.35f) else Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )

                                // Play indicator for selected video using canonical ic_play
                                if (isSelected) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_play),
                                        contentDescription = null,
                                        tint = ColorUtils.lightenForText(accentColor, 1.35f),
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(26.dp)
                                            .offset(x = (-1.5).dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dedicated Glassmorphic Dropdown Menu matching DetailTrailers with hazeGlass blur and spring physics
        DetailTrailersMenuModal(
            data = categoryMenuData,
            accentColor = accentColor,
            hazeState = modalHazeState,
            rootHazeState = hazeState,
            onDismiss = { categoryMenuData = null }
        )
    }
}
