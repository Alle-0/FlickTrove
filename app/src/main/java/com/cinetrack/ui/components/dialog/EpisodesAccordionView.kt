package com.cinetrack.ui.components.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.R
import com.cinetrack.data.model.Episode
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.Season
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.ui.viewmodel.DetailUiState
import com.cinetrack.ui.viewmodel.MovieDetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



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
            .filter { it.seasonNumber > 0 }
            .sortedBy { it.seasonNumber }
    }

    val todayIso = remember {
        try { java.time.LocalDate.now().toString() } catch (e: Exception) { "2026-01-01" }
    }

    // Identify "Up Next" episode across regular seasons (excluding future unreleased episodes)
    val upNextInfo = remember(canonicalSeasons, localWatchedEpisodes, seasonDetails, todayIso) {
        findUpNextEpisode(canonicalSeasons, localWatchedEpisodes, seasonDetails, todayIso)
    }

    // Identify upcoming unreleased season/episode if all released episodes are watched
    val upcomingAiringInfo = remember(canonicalSeasons, seasonDetails, todayIso) {
        findUpcomingAiring(canonicalSeasons, seasonDetails, todayIso)
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
                upcomingAiring = upcomingAiringInfo,
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
            val sNum = season.seasonNumber
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
                fallbackBackdropPath = movie.backdropPath ?: movie.posterPath,
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

