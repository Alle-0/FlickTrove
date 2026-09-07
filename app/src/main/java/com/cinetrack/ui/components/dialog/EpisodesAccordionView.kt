package com.cinetrack.ui.components.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.Episode
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.Season
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.ui.viewmodel.DetailUiState
import com.cinetrack.ui.viewmodel.MovieDetailViewModel
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl

@Composable
fun EpisodesAccordionView(
    movie: Movie,
    viewModel: MovieDetailViewModel,
    localWatchedEpisodes: Map<String, List<Int>>,
    onUpdateWatchedEpisodes: (Map<String, List<Int>>) -> Unit,
    onEpisodeInfoClick: (Episode) -> Unit,
    isDropped: Boolean,
    nestedScrollConnection: NestedScrollConnection,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val successState = state as? DetailUiState.Success
    val seasonDetails = successState?.seasonDetails ?: emptyMap()

    // Valid canonical seasons (strictly excluding Season 0 / Specials as per pro-tip)
    val canonicalSeasons = remember(movie.seasons, successState?.details?.seasons) {
        val seasonsSource = successState?.details?.seasons?.takeIf { it.isNotEmpty() }
            ?: movie.seasons
            ?: (1..(movie.numberOfSeasons ?: 1)).map { Season(seasonNumber = it, episodeCount = 0) }
        seasonsSource
            .filter { (it.seasonNumber ?: 0) > 0 }
            .sortedBy { it.seasonNumber }
    }

    // Identify "Up Next" episode across regular seasons
    val upNextInfo = remember(canonicalSeasons, localWatchedEpisodes, seasonDetails) {
        findUpNextEpisode(canonicalSeasons, localWatchedEpisodes, seasonDetails)
    }

    // Preload season details for Up Next if not already present
    LaunchedEffect(upNextInfo?.seasonNumber) {
        upNextInfo?.seasonNumber?.let { sNum ->
            if (!seasonDetails.containsKey(sNum)) {
                viewModel.loadSeasonDetails(sNum)
            }
        }
    }

    // Determine the initially expanded season (first season that has unwatched episodes)
    val initialExpandedSeason = remember(canonicalSeasons) {
        upNextInfo?.seasonNumber ?: canonicalSeasons.firstOrNull()?.seasonNumber ?: 1
    }

    // Track expanded state for each season
    var expandedSeasons by rememberSaveable {
        mutableStateOf(setOf(initialExpandedSeason))
    }

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var hasInitialScrolled by remember { mutableStateOf(false) }

    // Auto-scroll on launch to bring the active season and unwatched episode into view
    LaunchedEffect(canonicalSeasons, upNextInfo?.seasonNumber, seasonDetails[upNextInfo?.seasonNumber]) {
        if (hasInitialScrolled || upNextInfo == null || canonicalSeasons.isEmpty()) return@LaunchedEffect

        val targetSeasonNum = upNextInfo.seasonNumber
        val seasonIdx = canonicalSeasons.indexOfFirst { it.seasonNumber == targetSeasonNum }
        if (seasonIdx < 0) return@LaunchedEffect

        val targetSeasonData = seasonDetails[targetSeasonNum]
        val targetEpNum = upNextInfo.episodeNumber
        val targetEpIndex = targetSeasonData?.episodes?.indexOfFirst { it.episodeNumber == targetEpNum }
            ?.takeIf { it >= 0 }
            ?: (targetEpNum - 1).coerceAtLeast(0)

        val itemIndex = 3 + seasonIdx
        val targetOffsetDp = if (targetEpIndex <= 1) {
            0.dp
        } else {
            // Header ~56dp + divider 1dp + top padding 10dp = 67dp.
            // Each episode is ~100dp. Offset by 80dp so target episode sits comfortably near the top.
            (67 + (targetEpIndex * 100) - 80).coerceAtLeast(0).dp
        }
        val offsetPx = with(density) { targetOffsetDp.roundToPx() }

        if (targetSeasonData != null) {
            delay(100)
            listState.animateScrollToItem(itemIndex, offsetPx)
            hasInitialScrolled = true
        } else {
            delay(300)
            if (!hasInitialScrolled) {
                listState.animateScrollToItem(itemIndex, offsetPx)
                hasInitialScrolled = true
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .premiumScrollbar(listState)
            .verticalFadingEdges(listState, 16.dp, 16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Up Next Hero Card
        item(key = "up_next_hero") {
            UpNextHeroCard(
                movie = movie,
                upNext = upNextInfo,
                isDropped = isDropped,
                onToggle = { sNum, epNum ->
                    if (isDropped) return@UpNextHeroCard
                    val currentWatched = localWatchedEpisodes[sNum.toString()]?.toMutableList() ?: mutableListOf()
                    if (!currentWatched.contains(epNum)) {
                        currentWatched.add(epNum)
                    } else {
                        currentWatched.remove(epNum)
                    }
                    onUpdateWatchedEpisodes(localWatchedEpisodes + (sNum.toString() to currentWatched))
                },
                onInfoClick = { ep -> onEpisodeInfoClick(ep) }
            )
        }

        // 2. Helpful Info Hint
        item(key = "info_hint") {
            Text(
                text = stringResource(R.string.episodes_long_press_info),
                modifier = Modifier.padding(horizontal = 4.dp),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 3. Section Header: All Seasons
        item(key = "seasons_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.episodes_all_episodes),
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )

                val totalEpisodesAcrossAll = canonicalSeasons.sumOf { it.episodeCount ?: 0 }
                val totalWatchedAcrossAll = canonicalSeasons.sumOf { s ->
                    (localWatchedEpisodes[s.seasonNumber.toString()]?.size ?: 0)
                }
                val isAllCompleted = totalEpisodesAcrossAll > 0 && totalWatchedAcrossAll >= totalEpisodesAcrossAll

                Text(
                    text = "$totalWatchedAcrossAll / $totalEpisodesAcrossAll",
                    color = if (isAllCompleted) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        // 4. Stacked Season Accordion Items
        items(
            count = canonicalSeasons.size,
            key = { index -> "season_${canonicalSeasons[index].seasonNumber}" },
            contentType = { "season_accordion" }
        ) { index ->
            val season = canonicalSeasons[index]
            val sNum = season.seasonNumber ?: (index + 1)
            val isExpanded = expandedSeasons.contains(sNum)
            val seasonData = seasonDetails[sNum]
            val isFullyWatched = seasonData?.let {
                isSeasonFullyWatched(localWatchedEpisodes, sNum, it)
            } ?: run {
                val watchedCount = localWatchedEpisodes[sNum.toString()]?.size ?: 0
                val totalCount = season.episodeCount ?: 0
                totalCount > 0 && watchedCount >= totalCount
            }

            SeasonAccordionCard(
                season = season,
                seasonData = seasonData,
                isExpanded = isExpanded,
                isFullyWatched = isFullyWatched,
                localWatchedEpisodes = localWatchedEpisodes,
                isDropped = isDropped,
                onHeaderClick = {
                    val willExpand = !isExpanded
                    val nextSet = if (isExpanded) {
                        expandedSeasons - sNum
                    } else {
                        viewModel.loadSeasonDetails(sNum)
                        expandedSeasons + sNum
                    }
                    expandedSeasons = nextSet
                    if (willExpand) {
                        coroutineScope.launch {
                            delay(80)
                            listState.animateScrollToItem(3 + index, 0)
                        }
                    }
                },
                onBatchToggle = {
                    if (isDropped) return@SeasonAccordionCard
                    val todayIso = try { java.time.LocalDate.now().toString() } catch (e: Exception) { "2026-01-01" }
                    val releasedEps = seasonData?.episodes?.filter { ep ->
                        val epDate = ep.airDate
                        if (!epDate.isNullOrBlank()) epDate.take(10) <= todayIso
                        else if (!seasonData.airDate.isNullOrBlank()) seasonData.airDate.take(10) <= todayIso
                        else true
                    }?.map { it.episodeNumber } ?: emptyList()

                    val allEps = seasonData?.episodes?.map { it.episodeNumber }
                        ?: (1..(season.episodeCount ?: 0)).toList()

                    val targetEps = if (releasedEps.isNotEmpty()) releasedEps else allEps
                    val currentWatched = (localWatchedEpisodes[sNum.toString()] ?: emptyList()).toSet()

                    val isAllTargetWatched = targetEps.isNotEmpty() && targetEps.all { it in currentWatched }
                    val nextWatched = if (isAllTargetWatched) {
                        (currentWatched - targetEps.toSet()).toList()
                    } else {
                        (currentWatched + targetEps.toSet()).toList().sorted()
                    }
                    onUpdateWatchedEpisodes(localWatchedEpisodes + (sNum.toString() to nextWatched))
                },
                onBatchLongClick = {
                    if (isDropped) return@SeasonAccordionCard
                    val allEps = seasonData?.episodes?.map { it.episodeNumber }
                        ?: (1..(season.episodeCount ?: 0)).toList()
                    onUpdateWatchedEpisodes(localWatchedEpisodes + (sNum.toString() to allEps))
                },
                onEpisodeToggle = { epNum ->
                    if (isDropped) return@SeasonAccordionCard
                    val currentWatched = localWatchedEpisodes[sNum.toString()]?.toMutableList() ?: mutableListOf()
                    if (currentWatched.contains(epNum)) {
                        currentWatched.remove(epNum)
                    } else {
                        currentWatched.add(epNum)
                    }
                    onUpdateWatchedEpisodes(localWatchedEpisodes + (sNum.toString() to currentWatched))
                },
                onEpisodeInfoClick = onEpisodeInfoClick
            )
        }
    }
}

/**
 * Data class representing the next unwatched episode in the sequence.
 */
data class UpNextEpisodeData(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episode: Episode?
)

/**
 * Calculates the next episode to watch strictly excluding Specials (Season 0).
 */
private fun findUpNextEpisode(
    seasons: List<Season>,
    localWatchedEpisodes: Map<String, List<Int>>,
    seasonDetails: Map<Int, Season>
): UpNextEpisodeData? {
    val todayIso = try { java.time.LocalDate.now().toString() } catch (e: Exception) { "2026-01-01" }

    for (season in seasons) {
        val sNum = season.seasonNumber ?: continue
        if (sNum <= 0) continue // Exclude specials (Pro-Tip #1)

        val watched = (localWatchedEpisodes[sNum.toString()] ?: emptyList()).toSet()
        val detailedSeason = seasonDetails[sNum]

        if (detailedSeason?.episodes != null) {
            val releasedEpisodes = detailedSeason.episodes.filter { ep ->
                val epDate = ep.airDate
                if (!epDate.isNullOrBlank()) epDate.take(10) <= todayIso
                else if (!detailedSeason.airDate.isNullOrBlank()) detailedSeason.airDate.take(10) <= todayIso
                else true
            }

            val nextEp = releasedEpisodes.firstOrNull { it.episodeNumber !in watched }
                ?: detailedSeason.episodes.firstOrNull { it.episodeNumber !in watched }

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
 */
@Composable
private fun UpNextHeroCard(
    movie: Movie,
    upNext: UpNextEpisodeData?,
    isDropped: Boolean,
    onToggle: (Int, Int) -> Unit,
    onInfoClick: (Episode) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .bounceClick(
                onLongClick = { upNext?.episode?.let { onInfoClick(it) } }
            ) {
                if (upNext != null && !isDropped) {
                    onToggle(upNext.seasonNumber, upNext.episodeNumber)
                }
            },
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        if (upNext != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header label row: UP NEXT on left, S01 • E03 on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.episodes_up_next),
                        color = Color(0xFF00E676),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )

                    val seasonLabel = "S%02d • E%02d".format(upNext.seasonNumber, upNext.episodeNumber)
                    Text(
                        text = seasonLabel,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content Row: Thumbnail, Title & Air Date, Checkmark Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val thumbModel = upNext.episode?.stillPath?.let {
                        buildTmdbImageUrl(it, ImageType.BACKDROP, LocalImageQuality.current)
                    } ?: movie.backdropPath?.let {
                        buildTmdbImageUrl(it, ImageType.BACKDROP, LocalImageQuality.current)
                    } ?: buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, LocalImageQuality.current)

                    AsyncImage(
                        model = thumbModel,
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 120.dp, height = 68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentScale = ContentScale.Crop
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
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

                        val airDate = upNext.episode?.airDate
                        val todayIso = try { java.time.LocalDate.now().toString() } catch (e: Exception) { "2026-01-01" }
                        val isUnreleased = !airDate.isNullOrBlank() && airDate > todayIso
                        if (!airDate.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = com.cinetrack.ui.components.updates.formatReleaseDate(airDate),
                                color = if (isUnreleased) Color(0xFFF9A825) else Color.White.copy(alpha = 0.4f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    // Checkmark Action Button (identical to EpisodeCard unwatched circle)
                    Surface(
                        modifier = Modifier.size(28.dp),
                        color = Color.Transparent,
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        // Empty inside because it's unwatched
                    }
                }
            }
        } else {
            // All Caught Up Celebration State
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFF00E676),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = stringResource(R.string.episodes_all_caught_up),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Collapsible Season Accordion Card with Spring animation (Pro-Tip #2)
 * and Shimmer Skeleton Loading (Pro-Tip #3).
 */
@Composable
private fun SeasonAccordionCard(
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
    onEpisodeInfoClick: (Episode) -> Unit
) {
    val sNum = season.seasonNumber ?: 1
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
                                    onInfoClick = { onEpisodeInfoClick(episode) }
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
