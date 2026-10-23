package com.cinetrack.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.Episode
import com.cinetrack.data.model.Season
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl

import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.components.shared.ModalCloseButton
import dev.chrisbanes.haze.HazeState

@Composable
internal fun EpisodeCard(
    episode: Episode,
    isWatched: Boolean,
    onToggle: () -> Unit,
    onInfoClick: () -> Unit,
    fallbackBackdropPath: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(
                onLongClick = { onInfoClick() }
            ) { onToggle() },
        color = if (isWatched) Color(0xFF00E676).copy(alpha = 0.05f) else Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isWatched) Color(0xFF00E676).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val thumbModel = episode.stillPath?.let {
                buildTmdbImageUrl(it, ImageType.BACKDROP, LocalImageQuality.current)
            } ?: fallbackBackdropPath?.let {
                buildTmdbImageUrl(it, ImageType.BACKDROP, LocalImageQuality.current)
            }

            AsyncImage(
                model = thumbModel,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentScale = ContentScale.Crop,
                alpha = if (isWatched) 0.6f else 1f
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp)
            ) {
                val context = LocalContext.current
                val airDateInfo = remember(episode.airDate) {
                    com.cinetrack.ui.components.updates.formatEpisodeAirDate(episode.airDate, context)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.episodes_episode_n, episode.episodeNumber),
                        color = if (isWatched) Color(0xFF00E676) else Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        maxLines = 1
                    )

                    if (airDateInfo != null && !isWatched) {
                        Text(
                            text = " • " + airDateInfo.first,
                            color = airDateInfo.second,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = episode.name,
                    color = if (isWatched) Color.White.copy(alpha = 0.6f) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                modifier = Modifier.size(28.dp),
                color = if (isWatched) Color(0xFF00E676) else Color.Transparent,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isWatched) Color(0xFF00E676) else Color.White.copy(alpha = 0.15f))
            ) {
                if (isWatched) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_tick), contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

internal fun isSeasonFullyWatched(localWatchedEpisodes: Map<String, List<Int>>, currentSeasonNumber: Int, seasonData: Season): Boolean {
    val watched = localWatchedEpisodes[currentSeasonNumber.toString()]?.toSet() ?: emptySet()
    if (watched.isEmpty()) return false
    val todayIso = try { java.time.LocalDate.now().toString() } catch (e: Exception) { "2026-01-01" }
    val releasedEps = seasonData.episodes?.filter { ep ->
        val epDate = ep.airDate
        if (!epDate.isNullOrBlank()) epDate.take(10) <= todayIso
        else if (!seasonData.airDate.isNullOrBlank()) seasonData.airDate.take(10) <= todayIso
        else true
    }?.map { it.episodeNumber }
    
    if (releasedEps != null) {
        return releasedEps.isNotEmpty() && releasedEps.all { it in watched }
    } else {
        val total = seasonData.episodeCount ?: 0
        return total > 0 && watched.size >= total
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EpisodeInfoModal(
    episode: Episode,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null
) {
    FlickTroveModal(
        onDismissRequest = onDismiss,
        hazeState = hazeState,
        contentPadding = PaddingValues(24.dp)
    ) {
        // Titolo e X
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "${episode.seasonNumber}x${String.format("%02d", episode.episodeNumber)} - ${episode.name}",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                lineHeight = 24.sp
            )

            ModalCloseButton(onClick = onDismiss)
        }

        // Immagine (se presente)
        if (!episode.stillPath.isNullOrEmpty()) {
            AsyncImage(
                model = buildTmdbImageUrl(episode.stillPath, ImageType.BACKDROP, LocalImageQuality.current),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentScale = ContentScale.Crop
            )
        }

        // Pillole (Data e Voto)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!episode.airDate.isNullOrEmpty()) {
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = episode.airDate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            if (episode.voteAverage != null && episode.voteAverage > 0.0) {
                Surface(
                    color = Color(0xFF00E676).copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", episode.voteAverage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    }
                }
            }

            val runtimeText = episode.runtime?.takeIf { it > 0 }?.let { m ->
                if (m < 60) "${m}m" else "${m / 60}h ${if (m % 60 > 0) "${m % 60}m" else ""}".trim()
            }
            if (runtimeText != null) {
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_clock),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = runtimeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Trama
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
        ) {
            if (!episode.overview.isNullOrEmpty()) {
                Text(
                    text = episode.overview,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(vertical = 12.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.detail_no_overview),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

