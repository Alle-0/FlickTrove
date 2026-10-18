package com.cinetrack.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.cinetrack.R
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.common.CollectionProgressPill
import com.cinetrack.ui.components.common.CollectionRosetteBadge
import com.cinetrack.ui.components.shared.ImagePlaceholder
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl

@Composable
fun CollectionCollageCard(
    collection: TMDBSearchResult.CollectionResult,
    favorites: List<Movie> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backdropUrl = buildTmdbImageUrl(collection.backdropPath ?: collection.posterPath, ImageType.BACKDROP, LocalImageQuality.current)

    val totalCount = collection.partsCount
    val watchedCount = remember(collection.partsIds, favorites) {
        if (collection.partsIds.isNotEmpty()) {
            val watchedIds = favorites.filter { it.watched }.map { it.id }.toSet()
            collection.partsIds.count { it in watchedIds }
        } else 0
    }
    val isCompleted = totalCount > 0 && watchedCount >= totalCount
    val accentColor = MaterialTheme.colorScheme.primary
    val globalAccentColor = remember(accentColor) { 
        ColorUtils.ensureVividAccent(accentColor) 
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .bounceClick(scaleDown = 0.96f) { onClick() }
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF14141E))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
    ) {
        if (backdropUrl != null) {
            SubcomposeAsyncImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    ImagePlaceholder(
                        title = null,
                        mediaType = "movie",
                        isBackdrop = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        } else {
            ImagePlaceholder(
                title = null,
                mediaType = "movie",
                isBackdrop = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF09090F).copy(alpha = 0.55f),
                            Color(0xFF09090F).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Gradient for left & right contrast (title on left, pill on right)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color(0xFF09090F).copy(alpha = 0.85f),
                        0.30f to Color(0xFF09090F).copy(alpha = 0.45f),
                        0.52f to Color.Transparent,
                        0.74f to Color.Transparent,
                        0.90f to Color(0xFF09090F).copy(alpha = 0.40f),
                        1.0f to Color(0xFF09090F).copy(alpha = 0.75f)
                    )
                )
        )

        // Completed rosette ribbon badge in top-right
        if (isCompleted) {
            CollectionRosetteBadge(
                accentColor = globalAccentColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp),
                width = 32.dp,
                height = 44.dp,
                starSize = 13.dp,
                contentDescription = stringResource(R.string.action_completed)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = collection.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 17.sp,
                        lineHeight = 21.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val badgeText = when {
                collection.hasTvSeries -> {
                    if (totalCount == 1) stringResource(R.string.collection_badge_items_single, 1)
                    else stringResource(R.string.collection_badge_items_plural, totalCount)
                }
                totalCount == 1 -> stringResource(R.string.collection_badge_movies_single, 1)
                totalCount > 1 -> stringResource(R.string.collection_badge_movies_plural, totalCount)
                collection.partsPosterPaths.isNotEmpty() -> stringResource(R.string.collection_badge_movies_plus, collection.partsPosterPaths.size)
                else -> "SAGA"
            }

            CollectionProgressPill(
                watchedCount = watchedCount,
                totalCount = totalCount,
                badgeText = badgeText,
                accentColor = globalAccentColor,
                containerColor = Color(0xFF0F0F1A).copy(alpha = 0.82f),
                fontSize = 11.sp,
                horizontalPadding = 12.dp,
                verticalPadding = 6.dp
            )
        }
    }
}
