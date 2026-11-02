package com.cinetrack.ui.components.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.Episode
import com.cinetrack.data.model.Season
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import com.cinetrack.ui.components.shared.MorphGlassModal
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.horizontalFadingEdges
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState

@Composable
fun EpisodeDiscussionPickerModal(
    isVisible: Boolean = true,
    showSeasons: List<Season>,
    seasonEpisodes: Map<Int, List<Episode>>,
    isLoadingEpisodes: Boolean,
    currentSeason: Int?,
    currentEpisode: Int?,
    fallbackBackdropPath: String?,
    onLoadSeasonEpisodes: (Int) -> Unit,
    onSelectEpisode: (seasonNumber: Int, episode: Episode) -> Unit,
    onSelectMainDiscussion: () -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    hazeState: HazeState? = null,
    triggerBounds: Rect? = null
) {
    val validSeasons = remember(showSeasons) {
        showSeasons.filter { it.seasonNumber > 0 }.sortedBy { it.seasonNumber }
    }

    var selectedSeasonNumber by rememberSaveable(validSeasons) {
        mutableStateOf(currentSeason ?: validSeasons.firstOrNull()?.seasonNumber ?: 1)
    }

    LaunchedEffect(selectedSeasonNumber) {
        onLoadSeasonEpisodes(selectedSeasonNumber)
    }

    MorphGlassModal(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        triggerBounds = triggerBounds,
        hazeState = hazeState,
        targetMaxWidth = 440.dp,
        targetWidthFraction = 0.92f,
        maxModalHeightFraction = 0.85f,
        targetCornerRadius = 32.dp
    ) { contentAlpha ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = contentAlpha }
                .padding(20.dp)
        ) {
            // Modal Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tv),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.episodes_discussions_title),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ModalCloseButton(onClick = onDismiss)
        }

        // Jump to Main Series Discussion (when inside an episode)
        if (currentSeason != null && currentEpisode != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick {
                        onDismiss()
                        onSelectMainDiscussion()
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_comment),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = stringResource(R.string.episodes_discussions_all_series),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Season Chips Row
        if (validSeasons.isNotEmpty()) {
            val seasonsListState = rememberLazyListState()
            LazyRow(
                state = seasonsListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalFadingEdges(seasonsListState, leftEdgeWidth = 16.dp, rightEdgeWidth = 16.dp)
                    .padding(bottom = 12.dp)
            ) {
                items(validSeasons, key = { it.seasonNumber }) { season ->
                    val isSelected = season.seasonNumber == selectedSeasonNumber
                    Box(
                        modifier = Modifier
                            .bounceClick { selectedSeasonNumber = season.seasonNumber }
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.06f))
                            .border(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.12f), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.episodes_season_n, season.seasonNumber),
                            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Episodes List
        val episodes = seasonEpisodes[selectedSeasonNumber] ?: emptyList()
        val context = LocalContext.current

        if (isLoadingEpisodes && episodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White.copy(alpha = 0.7f),
                    strokeWidth = 2.dp
                )
            }
        } else if (episodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.comment_empty_state),
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 13.sp
                )
            }
        } else {
            val episodesListState = rememberLazyListState()
            LazyColumn(
                state = episodesListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalFadingEdges(episodesListState, topEdgeHeight = 20.dp, bottomEdgeHeight = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(episodes, key = { it.id }) { episode ->
                    val isCurrent = selectedSeasonNumber == currentSeason && episode.episodeNumber == currentEpisode
                    val thumbModel = episode.stillPath?.let {
                        buildTmdbImageUrl(it, ImageType.BACKDROP, LocalImageQuality.current)
                    } ?: fallbackBackdropPath?.let {
                        buildTmdbImageUrl(it, ImageType.BACKDROP, LocalImageQuality.current)
                    }

                    val airDateInfo = remember(episode.airDate) {
                        com.cinetrack.ui.components.updates.formatEpisodeAirDate(episode.airDate, context)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick {
                                onDismiss()
                                onSelectEpisode(selectedSeasonNumber, episode)
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCurrent) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                            .border(
                                1.dp,
                                if (isCurrent) accentColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = thumbModel,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(width = 86.dp, height = 50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentScale = ContentScale.Crop
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp, end = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.episodes_episode_n, episode.episodeNumber),
                                        color = if (isCurrent) accentColor else Color.White.copy(alpha = 0.5f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1
                                    )
                                    if (airDateInfo != null) {
                                        Text(
                                            text = " • " + airDateInfo.first,
                                            color = airDateInfo.second,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = episode.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_comment),
                                contentDescription = null,
                                tint = if (isCurrent) accentColor else Color.White.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}

