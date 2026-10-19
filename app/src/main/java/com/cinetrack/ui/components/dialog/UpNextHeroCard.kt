package com.cinetrack.ui.components.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.Season
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl

/**
 * Data class representing the next unwatched episode in the sequence.
 */
data class UpNextEpisodeData(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episode: Episode?
)

/**
 * Information about a future upcoming season/episode that hasn't aired yet.
 */
data class UpcomingAiringInfo(
    val seasonNumber: Int,
    val episodeNumber: Int?,
    val airDate: String?
)

/**
 * Identifies the next upcoming unreleased season or episode if available.
 */
internal fun findUpcomingAiring(
    seasons: List<Season>,
    seasonDetails: Map<Int, Season>,
    todayIso: String
): UpcomingAiringInfo? {
    for (season in seasons) {
        val sNum = season.seasonNumber
        if (sNum <= 0) continue

        val detailedSeason = seasonDetails[sNum]
        if (detailedSeason?.episodes != null) {
            val upcomingEp = detailedSeason.episodes.firstOrNull { ep ->
                val epDate = ep.airDate
                if (!epDate.isNullOrBlank()) epDate.take(10) > todayIso
                else {
                    val sDate = detailedSeason.airDate ?: season.airDate
                    !sDate.isNullOrBlank() && sDate.take(10) > todayIso
                }
            }
            if (upcomingEp != null) {
                return UpcomingAiringInfo(
                    seasonNumber = sNum,
                    episodeNumber = upcomingEp.episodeNumber,
                    airDate = upcomingEp.airDate ?: detailedSeason.airDate ?: season.airDate
                )
            }
        } else {
            val sDate = season.airDate
            if (!sDate.isNullOrBlank() && sDate.take(10) > todayIso) {
                return UpcomingAiringInfo(
                    seasonNumber = sNum,
                    episodeNumber = 1,
                    airDate = sDate
                )
            }
        }
    }
    return null
}

/**
 * Calculates the next episode to watch strictly excluding Specials (Season 0)
 * and unreleased future episodes/seasons.
 */
internal fun findUpNextEpisode(
    seasons: List<Season>,
    localWatchedEpisodes: Map<String, List<Int>>,
    seasonDetails: Map<Int, Season>,
    todayIso: String
): UpNextEpisodeData? {
    for (season in seasons) {
        val sNum = season.seasonNumber
        if (sNum <= 0) continue // Exclude specials (Pro-Tip #1)

        // If the entire season air date is known and in the future, skip it
        val seasonAirDate = season.airDate
        if (!seasonAirDate.isNullOrBlank() && seasonAirDate.take(10) > todayIso) {
            continue
        }

        val watched = (localWatchedEpisodes[sNum.toString()] ?: emptyList()).toSet()
        val detailedSeason = seasonDetails[sNum]

        if (detailedSeason?.episodes != null) {
            val detailedSeasonAirDate = detailedSeason.airDate
            if (!detailedSeasonAirDate.isNullOrBlank() && detailedSeasonAirDate.take(10) > todayIso) {
                continue
            }

            val releasedEpisodes = detailedSeason.episodes.filter { ep ->
                val epDate = ep.airDate
                if (!epDate.isNullOrBlank()) epDate.take(10) <= todayIso
                else if (!detailedSeasonAirDate.isNullOrBlank()) detailedSeasonAirDate.take(10) <= todayIso
                else true
            }

            // Strictly pick from released episodes: do not fallback to unreleased episodes
            val nextEp = releasedEpisodes.firstOrNull { it.episodeNumber !in watched }
            if (nextEp != null) {
                return UpNextEpisodeData(sNum, nextEp.episodeNumber, nextEp)
            }
        } else {
            val totalCount = season.episodeCount ?: 0
            if (watched.size < totalCount) {
                val nextNum = (1..totalCount).firstOrNull { it !in watched } ?: 1
                return UpNextEpisodeData(sNum, nextNum, null)
            }
        }
    }
    return null
}

/**
 * Up Next Hero Card styled with FlickTrove's dark glassmorphism system.
 * Streamlined to focus directly on the episode (thumbnail, title, Sxx•Exx pill, date, and checkmark)
 * without distracting header badges.
 */
@Composable
internal fun UpNextHeroCard(
    movie: Movie,
    upNext: UpNextEpisodeData?,
    upcomingAiring: UpcomingAiringInfo? = null,
    isDropped: Boolean,
    onToggle: (Int, Int) -> Unit,
    onInfoClick: (Episode) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .bounceClick(
                onLongClick = { upNext?.episode?.let { onInfoClick(it) } }
            ) {
                if (upNext != null && !isDropped) {
                    onToggle(upNext.seasonNumber, upNext.episodeNumber)
                }
            },
        color = Color(0xFF191B20),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00E676).copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        )
    ) {
        if (upNext != null) {
            // Clean, streamlined horizontal hero card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val thumbModel = upNext.episode?.stillPath?.let {
                    buildTmdbImageUrl(it, ImageType.BACKDROP, LocalImageQuality.current)
                } ?: movie.backdropPath?.let {
                    buildTmdbImageUrl(it, ImageType.BACKDROP, LocalImageQuality.current)
                } ?: buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, LocalImageQuality.current)

                // Clean Thumbnail without play button
                Box(
                    modifier = Modifier
                        .size(width = 118.dp, height = 72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = thumbModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Metadata Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    val epTitle = upNext.episode?.name?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.episodes_episode_n, upNext.episodeNumber)

                    Text(
                        text = epTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val seasonLabel = "S%02d • E%02d".format(upNext.seasonNumber, upNext.episodeNumber)
                    val airDate = upNext.episode?.airDate
                    val context = LocalContext.current
                    val airDateInfo = remember(airDate) {
                        com.cinetrack.ui.components.updates.formatEpisodeAirDate(airDate, context)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Season/Episode pill
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.08f),
                                    CircleShape
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = seasonLabel,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1
                            )
                        }

                        if (airDateInfo != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        airDateInfo.second.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        airDateInfo.second.copy(alpha = 0.30f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = airDateInfo.first,
                                    color = airDateInfo.second,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Checkmark Action Button
                Surface(
                    modifier = Modifier.size(34.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // All Caught Up Celebration State with upcoming season info if available
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    color = Color(0xFF00E676),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.episodes_all_caught_up),
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (upcomingAiring != null) {
                        val context = LocalContext.current
                        val airDateInfo = remember(upcomingAiring.airDate) {
                            com.cinetrack.ui.components.updates.formatEpisodeAirDate(upcomingAiring.airDate, context)
                        }
                        val subtitle = if (airDateInfo != null) {
                            stringResource(R.string.episodes_season_upcoming_date, upcomingAiring.seasonNumber, airDateInfo.first)
                        } else {
                            stringResource(R.string.episodes_season_upcoming, upcomingAiring.seasonNumber)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
