package com.cinetrack.ui.components.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.LocalEpisodeInfoStyle
import com.cinetrack.LocalLinearProgressBar
import com.cinetrack.LocalCircularProgressBar
import com.cinetrack.LocalProgressBarTarget
import com.cinetrack.LocalTitleTextSizeMultiplier
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.NextEpisodeInfo
import com.cinetrack.data.model.Season
import com.cinetrack.ui.components.card.ContinueWatchingSeriesCard
import com.cinetrack.ui.components.card.MovieCard
import com.cinetrack.ui.components.card.MovieListCard
import com.cinetrack.ui.components.common.FlickTroveSwitch
import com.cinetrack.ui.components.glass.GlassmorphicModal
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.ui.viewmodel.SettingsViewModel
import com.cinetrack.util.VibrationHelper
import dev.chrisbanes.haze.HazeState

// ─── Constants ─────────────────────────────────────────────────────────────

/** Values for episodeInfoStyle preference */
object EpisodeInfoStyle {
    // 1. Remaining
    const val REMAINING_SEASON = "REMAINING_SEASON"
    const val REMAINING_TOTAL = "REMAINING_TOTAL"
    const val REMAINING_SEASON_AND_TOTAL = "REMAINING_SEASON_AND_TOTAL"
    const val REMAINING = "REMAINING" // default / alias for REMAINING_SEASON_AND_TOTAL

    // 2. Watched / Total
    const val SEASON_ONLY = "SEASON_ONLY"
    const val WATCHED_OVER_TOTAL = "WATCHED_OVER_TOTAL"
    const val SEASON_AND_TOTAL = "SEASON_AND_TOTAL"

    // 3. Percentage
    const val PERCENTAGE_SEASON = "PERCENTAGE_SEASON"
    const val PERCENTAGE_TOTAL = "PERCENTAGE_TOTAL"
    const val PERCENTAGE = "PERCENTAGE" // alias for PERCENTAGE_TOTAL
    const val PERCENTAGE_SEASON_AND_TOTAL = "PERCENTAGE_SEASON_AND_TOTAL"
}

/** Values for progressBarTarget preference */
object ProgressBarTarget {
    const val CIRCULAR_TOTAL = "CIRCULAR_TOTAL"
    const val CIRCULAR_SEASON = "CIRCULAR_SEASON"
    const val LINEAR_TOTAL = "LINEAR_TOTAL"
    const val LINEAR_SEASON = "LINEAR_SEASON"
    const val NONE = "NONE"
}

enum class PreviewCardMode {
    GRID_3_COL,
    GRID_2_COL,
    LIST
}

// ─── Shared Formatters ──────────────────────────────────────────────────────

fun formatEpisodeInfo(
    style: String,
    nextInfo: NextEpisodeInfo,
    movie: Movie,
    seriesProgress: Float
): String {
    val totalInSeason = movie.seasons?.find { it.seasonNumber == nextInfo.seasonNumber }?.episodeCount ?: 0
    val watchedInSeason = movie.watchedEpisodes?.get(nextInfo.seasonNumber.toString())?.size 
        ?: if (totalInSeason > 0) (totalInSeason - nextInfo.remainingInSeason).coerceAtLeast(0) else 0
    val totalWatched = movie.watchedEpisodes?.filter { it.key != "0" }?.values?.sumOf { it.size } ?: 0
    val totalEpisodes = movie.numberOfEpisodes ?: 0
    val seasonProgress = if (totalInSeason > 0) (watchedInSeason.toFloat() / totalInSeason.toFloat()).coerceIn(0f, 1f) else 0f

    return when (style) {
        // Remaining
        EpisodeInfoStyle.REMAINING_SEASON -> {
            if (nextInfo.remainingInSeason > 0) " • ${nextInfo.remainingInSeason}"
            else ""
        }
        EpisodeInfoStyle.REMAINING_TOTAL -> {
            if (nextInfo.remainingTotal > 0) " • ${nextInfo.remainingTotal} tot"
            else ""
        }
        EpisodeInfoStyle.REMAINING, EpisodeInfoStyle.REMAINING_SEASON_AND_TOTAL, "REMAINING_AND_TOTAL" -> {
            if (nextInfo.remainingTotal > nextInfo.remainingInSeason) {
                " • ${nextInfo.remainingInSeason} (${nextInfo.remainingTotal} tot)"
            } else if (nextInfo.remainingInSeason > 0) {
                " • ${nextInfo.remainingInSeason}"
            } else {
                ""
            }
        }

        // Watched / Total
        EpisodeInfoStyle.SEASON_ONLY -> {
            if (totalInSeason > 0) " • $watchedInSeason/$totalInSeason ep"
            else ""
        }
        EpisodeInfoStyle.WATCHED_OVER_TOTAL -> {
            if (totalEpisodes > 0) " • $totalWatched/$totalEpisodes ep"
            else if (totalWatched > 0) " • $totalWatched ep"
            else ""
        }
        EpisodeInfoStyle.SEASON_AND_TOTAL -> {
            if (totalInSeason > 0 && totalEpisodes > 0) " • $watchedInSeason/$totalInSeason • $totalWatched/$totalEpisodes tot"
            else if (totalInSeason > 0) " • $watchedInSeason/$totalInSeason ep"
            else if (totalEpisodes > 0) " • $totalWatched/$totalEpisodes tot"
            else ""
        }

        // Percentage
        EpisodeInfoStyle.PERCENTAGE_SEASON -> {
            " • ${(seasonProgress * 100).toInt()}%"
        }
        EpisodeInfoStyle.PERCENTAGE, EpisodeInfoStyle.PERCENTAGE_TOTAL -> {
            " • ${(seriesProgress * 100).toInt()}%"
        }
        EpisodeInfoStyle.PERCENTAGE_SEASON_AND_TOTAL -> {
            " • ${(seasonProgress * 100).toInt()}% • ${(seriesProgress * 100).toInt()}%"
        }

        else -> {
            if (nextInfo.remainingInSeason > 0) " • ${nextInfo.remainingInSeason}"
            else ""
        }
    }
}

fun calculateSeasonProgress(nextInfo: NextEpisodeInfo, movie: Movie): Float {
    val totalInSeason = movie.seasons?.find { it.seasonNumber == nextInfo.seasonNumber }?.episodeCount ?: 0
    if (totalInSeason <= 0) return 0f
    val watchedInSeason = movie.watchedEpisodes?.get(nextInfo.seasonNumber.toString())?.size 
        ?: (totalInSeason - nextInfo.remainingInSeason).coerceAtLeast(0)
    return (watchedInSeason.toFloat() / totalInSeason.toFloat()).coerceIn(0f, 1f)
}

@Composable
fun formatDrawerEpisodeSubtitle(
    style: String,
    nextInfo: NextEpisodeInfo,
    movie: Movie,
    seriesProgress: Float
): String {
    val totalInSeason = movie.seasons?.find { it.seasonNumber == nextInfo.seasonNumber }?.episodeCount ?: 0
    val watchedInSeason = movie.watchedEpisodes?.get(nextInfo.seasonNumber.toString())?.size 
        ?: if (totalInSeason > 0) (totalInSeason - nextInfo.remainingInSeason).coerceAtLeast(0) else 0
    val totalWatched = movie.watchedEpisodes?.filter { it.key != "0" }?.values?.sumOf { it.size } ?: 0
    val totalEpisodes = movie.numberOfEpisodes ?: 0
    val seasonProgress = if (totalInSeason > 0) (watchedInSeason.toFloat() / totalInSeason.toFloat()).coerceIn(0f, 1f) else 0f

    return when (style) {
        // Remaining
        EpisodeInfoStyle.REMAINING_SEASON -> {
            val remainingSeason = nextInfo.remainingInSeason
            if (remainingSeason > 0) stringResource(R.string.lbl_episodes_remaining, remainingSeason)
            else ""
        }
        EpisodeInfoStyle.REMAINING_TOTAL -> {
            val remainingTotal = nextInfo.remainingTotal
            if (remainingTotal > 0) "$remainingTotal tot"
            else ""
        }
        EpisodeInfoStyle.REMAINING, EpisodeInfoStyle.REMAINING_SEASON_AND_TOTAL, "REMAINING_AND_TOTAL" -> {
            val remainingSeason = nextInfo.remainingInSeason
            val remainingTotal = nextInfo.remainingTotal
            if (remainingTotal > remainingSeason) {
                "${stringResource(R.string.lbl_episodes_remaining, remainingSeason)} ($remainingTotal tot)"
            } else if (remainingSeason > 0) {
                stringResource(R.string.lbl_episodes_remaining, remainingSeason)
            } else {
                ""
            }
        }

        // Watched / Total
        EpisodeInfoStyle.SEASON_ONLY -> {
            if (totalInSeason > 0) "$watchedInSeason / $totalInSeason ep"
            else ""
        }
        EpisodeInfoStyle.WATCHED_OVER_TOTAL -> {
            if (totalEpisodes > 0) "$totalWatched / $totalEpisodes ep"
            else if (totalWatched > 0) "$totalWatched ep"
            else ""
        }
        EpisodeInfoStyle.SEASON_AND_TOTAL -> {
            if (totalInSeason > 0 && totalEpisodes > 0) "$watchedInSeason/$totalInSeason • $totalWatched/$totalEpisodes tot"
            else if (totalInSeason > 0) "$watchedInSeason / $totalInSeason ep"
            else if (totalEpisodes > 0) "$totalWatched / $totalEpisodes tot"
            else ""
        }

        // Percentage
        EpisodeInfoStyle.PERCENTAGE_SEASON -> {
            "${(seasonProgress * 100).toInt()}%"
        }
        EpisodeInfoStyle.PERCENTAGE, EpisodeInfoStyle.PERCENTAGE_TOTAL -> {
            "${(seriesProgress * 100).toInt()}%"
        }
        EpisodeInfoStyle.PERCENTAGE_SEASON_AND_TOTAL -> {
            "${(seasonProgress * 100).toInt()}% • ${(seriesProgress * 100).toInt()}%"
        }

        else -> {
            val remainingSeason = nextInfo.remainingInSeason
            if (remainingSeason > 0) stringResource(R.string.lbl_episodes_remaining, remainingSeason)
            else ""
        }
    }
}

// ─── Main Dialog ───────────────────────────────────────────────────────────

@Composable
fun CardAppearanceDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    settingsViewModel: SettingsViewModel,
    onShowBadgesInfo: () -> Unit = {},
    onDismiss: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsStateWithLifecycle()

    // Collect persisted values as initial state
    val persistedEpisodeInfoStyle by settingsViewModel.episodeInfoStyle.collectAsStateWithLifecycle()
    val persistedLinearProgressBar by settingsViewModel.linearProgressBar.collectAsStateWithLifecycle()
    val persistedCircularProgressBar by settingsViewModel.circularProgressBar.collectAsStateWithLifecycle()
    val persistedTextSizeMultiplier by settingsViewModel.titleTextSizeMultiplier.collectAsStateWithLifecycle()
    val persistedShowBadges by settingsViewModel.showBadges.collectAsStateWithLifecycle()
    val persistedShowFolderBookmarks by settingsViewModel.showFolderBookmarks.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary

    // Local state for live preview (not yet persisted)
    var localEpisodeStyle by remember(persistedEpisodeInfoStyle) { mutableStateOf(persistedEpisodeInfoStyle) }
    var localLinearBar by remember(persistedLinearProgressBar) { mutableStateOf(persistedLinearProgressBar) }
    var localCircularBar by remember(persistedCircularProgressBar) {
        mutableStateOf(if (persistedCircularProgressBar == "SEASON") "SEASON" else "TOTAL")
    }
    var localTextMultiplier by remember(persistedTextSizeMultiplier) { mutableStateOf(persistedTextSizeMultiplier) }
    var localShowBadges by remember(persistedShowBadges) { mutableStateOf(persistedShowBadges) }
    var localShowFolderBookmarks by remember(persistedShowFolderBookmarks) { mutableStateOf(persistedShowFolderBookmarks) }

    // Mode selector for preview (3 columns, 2 columns, list)
    var previewMode by remember { mutableStateOf(PreviewCardMode.GRID_3_COL) }

    // Mock folder colors for bookmark ribbon preview
    val mockFolderColors = remember { listOf(Color(0xFFE50914), Color(0xFF3B82F6)) }

    // Sample Normal Movie (Film in watchlist/preferiti)
    val mockNormalMovie = remember {
        Movie(
            id = 27205L,
            title = "Inception",
            name = "Inception",
            mediaType = "movie",
            posterPath = "/ljsZTbVsrQSqZgWeep2B1QiDKuh.jpg",
            backdropPath = "/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg",
            releaseDate = "2010-07-15",
            releaseYear = "2010",
            voteAverage = 8.8,
            voteCount = 35000,
            personalRating = 9.0,
            genreNamesString = "Fantascienza, Azione"
        ).apply {
            favorite = true
            watched = false
        }
    }

    // Sample In-Progress TV Series (Serie "Da vedere" con prossimo episodio)
    val mockWatchingTv = remember {
        Movie(
            id = 1396L,
            title = "Breaking Bad",
            name = "Breaking Bad",
            mediaType = "tv",
            posterPath = "/ggFHVNu6YYI5L9pCfOacjizRGt.jpg",
            backdropPath = "/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
            releaseDate = "2008-01-20",
            firstAirDate = "2008-01-20",
            releaseYear = "2008",
            voteAverage = 9.5,
            voteCount = 14500,
            personalRating = 9.0,
            numberOfEpisodes = 62,
            numberOfSeasons = 5,
            status = "Ended",
            genreIds = listOf(18, 80),
            genreNamesString = "Dramma, Crime",
            watchedEpisodes = mapOf(
                "1" to (1..7).toList(),
                "2" to (1..6).toList()
            ),
            seasons = listOf(
                Season(seasonNumber = 1, episodeCount = 7, airDate = "2008-01-20"),
                Season(seasonNumber = 2, episodeCount = 13, airDate = "2009-03-08"),
                Season(seasonNumber = 3, episodeCount = 13, airDate = "2010-03-21"),
                Season(seasonNumber = 4, episodeCount = 13, airDate = "2011-07-17"),
                Season(seasonNumber = 5, episodeCount = 16, airDate = "2012-07-15")
            )
        ).apply {
            favorite = true
            watched = false
        }
    }

    val mockNextEpisode = remember(mockWatchingTv) {
        mockWatchingTv.calculateNextEpisode() ?: NextEpisodeInfo(
            seasonNumber = 2,
            episodeNumber = 7,
            remainingInSeason = 7,
            remainingTotal = 49,
            progress = 0.21f,
            isLastEpisodeOfSeries = false,
            isUpToDateWithAirDate = false
        )
    }

    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            // ── Top drag handle ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )

            // ── Header ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_temi),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_card_appearance_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .bounceClick(scaleDown = 0.92f, onLongClick = {}) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.settings_cancel),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── Preview Mode Selector Tabs (Drag & Physics) ───────────
            val previewModes = listOf(
                Triple(PreviewCardMode.GRID_3_COL, stringResource(R.string.settings_preview_3_columns), R.drawable.ic_grid),
                Triple(PreviewCardMode.GRID_2_COL, stringResource(R.string.settings_preview_2_columns), R.drawable.ic_grid_a_4),
                Triple(PreviewCardMode.LIST, stringResource(R.string.settings_preview_list), R.drawable.ic_strisce)
            )

            DraggablePreviewModeSelector(
                modes = previewModes,
                selectedMode = previewMode,
                accentColor = accentColor,
                vibrationEnabled = vibrationEnabled,
                context = context,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 20.dp, vertical = 2.dp)
                    .widthIn(max = 295.dp)
                    .fillMaxWidth(),
                onSelectMode = { mode -> previewMode = mode }
            )

            // ── Live Authentic Preview Area (Side by side comparison) ──
            val previewHeight = when (previewMode) {
                PreviewCardMode.LIST -> 248.dp
                PreviewCardMode.GRID_3_COL -> 230.dp
                PreviewCardMode.GRID_2_COL -> 278.dp
            }
            val animPreviewHeight by animateDpAsState(
                targetValue = previewHeight,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "previewHeight"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .height(animPreviewHeight)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(
                    LocalEpisodeInfoStyle provides localEpisodeStyle,
                    LocalProgressBarTarget provides localLinearBar,
                    LocalLinearProgressBar provides localLinearBar,
                    LocalCircularProgressBar provides localCircularBar,
                    LocalTitleTextSizeMultiplier provides localTextMultiplier
                ) {
                    AnimatedContent(
                        targetState = previewMode,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                        label = "previewModeAnim"
                    ) { mode ->
                        when (mode) {
                            PreviewCardMode.GRID_3_COL -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(R.string.settings_card_preview_normal),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = Color.White.copy(alpha = 0.45f),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        MovieCard(
                                            movie = mockNormalMovie,
                                            cardWidth = 118.dp,
                                            showBadges = localShowBadges,
                                            showFolderBookmarks = localShowFolderBookmarks,
                                            folderColors = mockFolderColors,
                                            isFavorite = true,
                                            bounceEnabled = false
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(R.string.settings_card_preview_to_watch),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = accentColor.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        ContinueWatchingSeriesCard(
                                            movie = mockWatchingTv,
                                            cardWidth = 118.dp,
                                            showBadges = localShowBadges,
                                            showFolderBookmarks = localShowFolderBookmarks,
                                            folderColors = mockFolderColors,
                                            precalculatedNextInfo = mockNextEpisode,
                                            onQuickMarkWatched = {},
                                            onPress = {}
                                        )
                                    }
                                }
                            }
                            PreviewCardMode.GRID_2_COL -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(R.string.settings_card_preview_normal),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = Color.White.copy(alpha = 0.45f),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        MovieCard(
                                            movie = mockNormalMovie,
                                            cardWidth = 138.dp,
                                            showBadges = localShowBadges,
                                            showFolderBookmarks = localShowFolderBookmarks,
                                            folderColors = mockFolderColors,
                                            isFavorite = true,
                                            bounceEnabled = false
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(R.string.settings_card_preview_to_watch),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = accentColor.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        ContinueWatchingSeriesCard(
                                            movie = mockWatchingTv,
                                            cardWidth = 138.dp,
                                            showBadges = localShowBadges,
                                            showFolderBookmarks = localShowFolderBookmarks,
                                            folderColors = mockFolderColors,
                                            precalculatedNextInfo = mockNextEpisode,
                                            onQuickMarkWatched = {},
                                            onPress = {}
                                        )
                                    }
                                }
                            }
                            PreviewCardMode.LIST -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MovieListCard(
                                        movie = mockNormalMovie,
                                        showBadges = localShowBadges,
                                        showFolderBookmarks = localShowFolderBookmarks,
                                        folderColors = mockFolderColors,
                                        isFavorite = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    MovieListCard(
                                        movie = mockWatchingTv,
                                        showBadges = localShowBadges,
                                        showFolderBookmarks = localShowFolderBookmarks,
                                        folderColors = mockFolderColors,
                                        isFavorite = true,
                                        onQuickMarkWatched = {},
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Options ───────────────────────────────────────────────
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalFadingEdges(scrollState)
                    .premiumScrollbar(state = scrollState)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Info Episodi
                AppearanceSection(title = stringResource(R.string.settings_card_episode_info)) {
                    EpisodeStyleCompactOptions(
                        selected = localEpisodeStyle,
                        accentColor = accentColor,
                        vibrationEnabled = vibrationEnabled,
                        context = context,
                        onSelect = { localEpisodeStyle = it }
                    )
                }

                // 2. Progress Bar
                AppearanceSection(title = stringResource(R.string.settings_card_progress_bar)) {
                    ProgressBarCompactOptions(
                        selectedLinear = localLinearBar,
                        selectedCircular = localCircularBar,
                        accentColor = accentColor,
                        vibrationEnabled = vibrationEnabled,
                        context = context,
                        onSelectLinear = { localLinearBar = it },
                        onSelectCircular = { localCircularBar = it }
                    )
                }

                // 3. Grandezza Testo
                AppearanceSection(title = stringResource(R.string.settings_card_text_size)) {
                    TextSizeOptions(
                        selected = localTextMultiplier,
                        accentColor = accentColor,
                        vibrationEnabled = vibrationEnabled,
                        context = context,
                        onSelect = { localTextMultiplier = it }
                    )
                }

                // 4. Segnalibro Cartelle (Folder Bookmarks)
                AppearanceSection(title = stringResource(R.string.settings_folder_bookmarks)) {
                    FolderBookmarksToggleOption(
                        showFolderBookmarks = localShowFolderBookmarks,
                        accentColor = accentColor,
                        onToggle = {
                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                            localShowFolderBookmarks = it
                        }
                    )
                }

                // 5. Badge Informativi
                AppearanceSection(title = stringResource(R.string.settings_badges)) {
                    BadgeToggleOption(
                        showBadges = localShowBadges,
                        accentColor = accentColor,
                        onShowBadgesInfo = onShowBadgesInfo,
                        onToggle = {
                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                            localShowBadges = it
                        }
                    )
                }
            }

            // ── Footer Buttons ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bounceClick(scaleDown = 0.96f, onLongClick = {}) { onDismiss() }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.settings_cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                // Apply
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bounceClick(scaleDown = 0.96f, onLongClick = {}) {
                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                            settingsViewModel.updateEpisodeInfoStyle(localEpisodeStyle)
                            settingsViewModel.updateLinearProgressBar(localLinearBar)
                            settingsViewModel.updateCircularProgressBar(localCircularBar)
                            settingsViewModel.updateTitleTextSizeMultiplier(localTextMultiplier)
                            settingsViewModel.toggleBadges(localShowBadges)
                            settingsViewModel.toggleFolderBookmarks(localShowFolderBookmarks)
                            onDismiss()
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.settings_apply),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = Color.Black.copy(alpha = 0.88f)
                    )
                }
            }
        }
    }
}

// ─── Appearance Section Wrapper ─────────────────────────────────────────────

@Composable
private fun AppearanceSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 11.5.sp
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            modifier = Modifier.padding(start = 2.dp)
        )
        content()
    }
}

// ─── Draggable Preview Mode Selector (Drag & Physics) ───────────────────────

@Composable
private fun DraggablePreviewModeSelector(
    modes: List<Triple<PreviewCardMode, String, Int>>,
    selectedMode: PreviewCardMode,
    accentColor: Color,
    vibrationEnabled: Boolean,
    context: android.content.Context,
    modifier: Modifier = Modifier,
    onSelectMode: (PreviewCardMode) -> Unit
) {
    val selectedIndex = modes.indexOfFirst { it.first == selectedMode }.coerceAtLeast(0)
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    val padding = 2.dp

    BoxWithConstraints(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(padding)
    ) {
        val usableWidthPx = constraints.maxWidth.toFloat()
        val segmentCount = modes.size
        val segmentWidthPx = usableWidthPx / segmentCount
        val segmentWidthDp = maxWidth / segmentCount
        val usableHeightDp = maxHeight

        val offsetAnim = remember { Animatable(selectedIndex * segmentWidthPx) }
        var isDragging by remember { mutableStateOf(false) }
        var hasInitialized by remember { mutableStateOf(false) }

        LaunchedEffect(selectedIndex, segmentWidthPx) {
            if (segmentWidthPx > 0f) {
                val target = selectedIndex * segmentWidthPx
                if (!hasInitialized) {
                    offsetAnim.snapTo(target)
                    hasInitialized = true
                } else if (!isDragging && offsetAnim.value != target) {
                    offsetAnim.animateTo(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(segmentWidthPx, segmentCount) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragAccumulatedX = 0f
                        var dragAccumulatedY = 0f
                        var isDrag = false
                        val pointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                if (isDrag) {
                                    val targetIndex = if (segmentWidthPx > 0f) {
                                        (offsetAnim.value / segmentWidthPx).roundToInt().coerceIn(0, segmentCount - 1)
                                    } else selectedIndex

                                    coroutineScope.launch {
                                        offsetAnim.animateTo(
                                            targetValue = targetIndex * segmentWidthPx,
                                            animationSpec = spring(
                                                dampingRatio = 0.72f,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    if (targetIndex != selectedIndex) {
                                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                        onSelectMode(modes[targetIndex].first)
                                    }
                                } else {
                                    val tapX = down.position.x.coerceIn(0f, usableWidthPx)
                                    val tappedIndex = if (segmentWidthPx > 0f) {
                                        (tapX / segmentWidthPx).toInt().coerceIn(0, segmentCount - 1)
                                    } else selectedIndex

                                    if (tappedIndex != selectedIndex) {
                                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                        onSelectMode(modes[tappedIndex].first)
                                    }
                                    coroutineScope.launch {
                                        offsetAnim.animateTo(
                                            targetValue = tappedIndex * segmentWidthPx,
                                            animationSpec = spring(
                                                dampingRatio = 0.72f,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                                isDragging = false
                                break
                            }

                            val deltaX = change.position.x - change.previousPosition.x
                            val deltaY = change.position.y - change.previousPosition.y
                            dragAccumulatedX += deltaX
                            dragAccumulatedY += deltaY

                            if (!isDrag) {
                                if (abs(dragAccumulatedX) > viewConfiguration.touchSlop &&
                                    abs(dragAccumulatedX) > abs(dragAccumulatedY)
                                ) {
                                    isDrag = true
                                    isDragging = true
                                } else if (abs(dragAccumulatedY) > viewConfiguration.touchSlop) {
                                    break
                                }
                            }

                            if (isDrag) {
                                change.consume()
                                val maxOffset = (segmentCount - 1) * segmentWidthPx
                                val current = offsetAnim.value + deltaX
                                val rubberBanded = when {
                                    current < 0f -> current * 0.35f
                                    current > maxOffset -> maxOffset + (current - maxOffset) * 0.35f
                                    else -> current
                                }
                                coroutineScope.launch {
                                    offsetAnim.snapTo(rubberBanded)
                                }
                            }
                        }
                    }
                }
        ) {
            // Sliding Indicator (Solid accentColor with spring deformation)
            val indicatorOffsetDp = with(density) { offsetAnim.value.toDp() }
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffsetDp)
                    .width(segmentWidthDp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = if (isDragging) 1.03f else 1f
                        scaleY = if (isDragging) 0.98f else 1f
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor)
            )

            // Dividers
            val currentFraction = if (segmentWidthPx > 0f) offsetAnim.value / segmentWidthPx else selectedIndex.toFloat()
            for (k in 1 until segmentCount) {
                val distToBoundary = abs(currentFraction - (k - 0.5f))
                val dividerAlpha = ((distToBoundary - 0.35f) / 0.15f).coerceIn(0f, 1f)
                if (dividerAlpha > 0.02f) {
                    val dividerXDp = segmentWidthDp * k
                    Box(
                        modifier = Modifier
                            .offset(x = dividerXDp - 0.5.dp, y = (usableHeightDp - 13.dp) / 2)
                            .width(1.dp)
                            .height(13.dp)
                            .background(Color.White.copy(alpha = 0.12f * dividerAlpha))
                    )
                }
            }

            // Labels
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                modes.forEachIndexed { index, (_, label, iconRes) ->
                    val weight = (1f - abs(currentFraction - index)).coerceIn(0f, 1f)
                    val contentColor = lerp(Color.White.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.9f), weight)

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = iconRes),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (weight > 0.5f) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ─── Segmented Option Model & Control (Drag & Physics) ───────────────────────

data class SegmentedOption<T>(
    val value: T,
    val title: String,
    val subtitle: String? = null
)

@Composable
private fun <T> SegmentedControl(
    options: List<SegmentedOption<T>>,
    selected: T,
    accentColor: Color,
    vibrationEnabled: Boolean,
    context: android.content.Context,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 54.dp,
    onSelect: (T) -> Unit
) {
    val selectedIndex = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    val padding = 3.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(padding)
    ) {
        val usableWidthPx = constraints.maxWidth.toFloat()
        val segmentCount = options.size
        val segmentWidthPx = usableWidthPx / segmentCount
        val segmentWidthDp = maxWidth / segmentCount
        val usableHeightDp = maxHeight

        val offsetAnim = remember { Animatable(selectedIndex * segmentWidthPx) }
        var isDragging by remember { mutableStateOf(false) }
        var hasInitialized by remember { mutableStateOf(false) }

        LaunchedEffect(selectedIndex, segmentWidthPx) {
            if (segmentWidthPx > 0f) {
                val target = selectedIndex * segmentWidthPx
                if (!hasInitialized) {
                    offsetAnim.snapTo(target)
                    hasInitialized = true
                } else if (!isDragging && offsetAnim.value != target) {
                    offsetAnim.animateTo(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(segmentWidthPx, segmentCount) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragAccumulatedX = 0f
                        var dragAccumulatedY = 0f
                        var isDrag = false
                        val pointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                if (isDrag) {
                                    val targetIndex = if (segmentWidthPx > 0f) {
                                        (offsetAnim.value / segmentWidthPx).roundToInt().coerceIn(0, segmentCount - 1)
                                    } else selectedIndex

                                    coroutineScope.launch {
                                        offsetAnim.animateTo(
                                            targetValue = targetIndex * segmentWidthPx,
                                            animationSpec = spring(
                                                dampingRatio = 0.72f,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    if (targetIndex != selectedIndex) {
                                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                        onSelect(options[targetIndex].value)
                                    }
                                } else {
                                    val tapX = down.position.x.coerceIn(0f, usableWidthPx)
                                    val tappedIndex = if (segmentWidthPx > 0f) {
                                        (tapX / segmentWidthPx).toInt().coerceIn(0, segmentCount - 1)
                                    } else selectedIndex

                                    if (tappedIndex != selectedIndex) {
                                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                        onSelect(options[tappedIndex].value)
                                    }
                                    coroutineScope.launch {
                                        offsetAnim.animateTo(
                                            targetValue = tappedIndex * segmentWidthPx,
                                            animationSpec = spring(
                                                dampingRatio = 0.72f,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                                isDragging = false
                                break
                            }

                            val deltaX = change.position.x - change.previousPosition.x
                            val deltaY = change.position.y - change.previousPosition.y
                            dragAccumulatedX += deltaX
                            dragAccumulatedY += deltaY

                            if (!isDrag) {
                                if (abs(dragAccumulatedX) > viewConfiguration.touchSlop &&
                                    abs(dragAccumulatedX) > abs(dragAccumulatedY)
                                ) {
                                    isDrag = true
                                    isDragging = true
                                } else if (abs(dragAccumulatedY) > viewConfiguration.touchSlop) {
                                    break
                                }
                            }

                            if (isDrag) {
                                change.consume()
                                val maxOffset = (segmentCount - 1) * segmentWidthPx
                                val current = offsetAnim.value + deltaX
                                val rubberBanded = when {
                                    current < 0f -> current * 0.35f
                                    current > maxOffset -> maxOffset + (current - maxOffset) * 0.35f
                                    else -> current
                                }
                                coroutineScope.launch {
                                    offsetAnim.snapTo(rubberBanded)
                                }
                            }
                        }
                    }
                }
        ) {
            // Sliding Indicator
            val indicatorOffsetDp = with(density) { offsetAnim.value.toDp() }
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffsetDp)
                    .width(segmentWidthDp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = if (isDragging) 1.03f else 1f
                        scaleY = if (isDragging) 0.98f else 1f
                    }
                    .clip(RoundedCornerShape(13.dp))
                    .background(accentColor.copy(alpha = 0.14f))
                    .border(1.5.dp, accentColor, RoundedCornerShape(13.dp))
            )

            // Dividers
            val currentFraction = if (segmentWidthPx > 0f) offsetAnim.value / segmentWidthPx else selectedIndex.toFloat()
            val hasSubtitle = options.any { !it.subtitle.isNullOrBlank() }
            val divHeight = if (hasSubtitle) 22.dp else 16.dp

            for (k in 1 until segmentCount) {
                val distToBoundary = abs(currentFraction - (k - 0.5f))
                val dividerAlpha = ((distToBoundary - 0.35f) / 0.15f).coerceIn(0f, 1f)
                if (dividerAlpha > 0.02f) {
                    val dividerXDp = segmentWidthDp * k
                    Box(
                        modifier = Modifier
                            .offset(x = dividerXDp - 0.5.dp, y = (usableHeightDp - divHeight) / 2)
                            .width(1.dp)
                            .height(divHeight)
                            .background(Color.White.copy(alpha = 0.08f * dividerAlpha))
                    )
                }
            }

            // Labels
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, option ->
                    val weight = (1f - abs(currentFraction - index)).coerceIn(0f, 1f)
                    val titleColor = lerp(Color.White.copy(alpha = 0.85f), accentColor, weight)
                    val subtitleColor = lerp(Color.White.copy(alpha = 0.45f), accentColor.copy(alpha = 0.85f), weight)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (weight > 0.5f) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 12.5.sp
                                ),
                                color = titleColor,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            if (!option.subtitle.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = option.subtitle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = subtitleColor,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Compact Episode Style Options ──────────────────────────────────────────

@Composable
private fun EpisodeStyleCompactOptions(
    selected: String,
    accentColor: Color,
    vibrationEnabled: Boolean,
    context: android.content.Context,
    onSelect: (String) -> Unit
) {
    val normRemaining = if (selected == EpisodeInfoStyle.REMAINING || selected == "REMAINING_AND_TOTAL") {
        EpisodeInfoStyle.REMAINING_SEASON_AND_TOTAL
    } else selected

    val normPercentage = if (selected == EpisodeInfoStyle.PERCENTAGE) {
        EpisodeInfoStyle.PERCENTAGE_TOTAL
    } else selected

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ── 1. Rimanenti (Left) ──
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(R.string.settings_cat_remaining).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.45f)
            )
            SegmentedControl(
                options = listOf(
                    SegmentedOption(
                        value = EpisodeInfoStyle.REMAINING_SEASON,
                        title = stringResource(R.string.settings_chip_col_season),
                        subtitle = stringResource(R.string.settings_sub_rem_season)
                    ),
                    SegmentedOption(
                        value = EpisodeInfoStyle.REMAINING_TOTAL,
                        title = stringResource(R.string.settings_chip_col_series),
                        subtitle = stringResource(R.string.settings_sub_rem_series)
                    ),
                    SegmentedOption(
                        value = EpisodeInfoStyle.REMAINING_SEASON_AND_TOTAL,
                        title = stringResource(R.string.settings_chip_col_both),
                        subtitle = stringResource(R.string.settings_sub_rem_both)
                    )
                ),
                selected = normRemaining,
                accentColor = accentColor,
                vibrationEnabled = vibrationEnabled,
                context = context,
                onSelect = onSelect
            )
        }

        // ── 2. Visti / Totale ( / ) ──
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(R.string.settings_cat_watched).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.45f)
            )
            SegmentedControl(
                options = listOf(
                    SegmentedOption(
                        value = EpisodeInfoStyle.SEASON_ONLY,
                        title = stringResource(R.string.settings_chip_col_season),
                        subtitle = stringResource(R.string.settings_sub_watched_season)
                    ),
                    SegmentedOption(
                        value = EpisodeInfoStyle.WATCHED_OVER_TOTAL,
                        title = stringResource(R.string.settings_chip_col_series),
                        subtitle = stringResource(R.string.settings_sub_watched_series)
                    ),
                    SegmentedOption(
                        value = EpisodeInfoStyle.SEASON_AND_TOTAL,
                        title = stringResource(R.string.settings_chip_col_both),
                        subtitle = stringResource(R.string.settings_sub_watched_both)
                    )
                ),
                selected = selected,
                accentColor = accentColor,
                vibrationEnabled = vibrationEnabled,
                context = context,
                onSelect = onSelect
            )
        }

        // ── 3. Percentuale (%) ──
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(R.string.settings_cat_percentage).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.45f)
            )
            SegmentedControl(
                options = listOf(
                    SegmentedOption(
                        value = EpisodeInfoStyle.PERCENTAGE_SEASON,
                        title = stringResource(R.string.settings_chip_col_season),
                        subtitle = stringResource(R.string.settings_sub_pct_season)
                    ),
                    SegmentedOption(
                        value = EpisodeInfoStyle.PERCENTAGE_TOTAL,
                        title = stringResource(R.string.settings_chip_col_series),
                        subtitle = stringResource(R.string.settings_sub_pct_series)
                    ),
                    SegmentedOption(
                        value = EpisodeInfoStyle.PERCENTAGE_SEASON_AND_TOTAL,
                        title = stringResource(R.string.settings_chip_col_both),
                        subtitle = stringResource(R.string.settings_sub_pct_both)
                    )
                ),
                selected = normPercentage,
                accentColor = accentColor,
                vibrationEnabled = vibrationEnabled,
                context = context,
                onSelect = onSelect
            )
        }
    }
}

// ─── Compact Progress Bar Options ───────────────────────────────────────────

@Composable
private fun ProgressBarCompactOptions(
    selectedLinear: String,
    selectedCircular: String,
    accentColor: Color,
    vibrationEnabled: Boolean,
    context: android.content.Context,
    onSelectLinear: (String) -> Unit,
    onSelectCircular: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ── 1. Barra Lineare ──
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(R.string.settings_cat_linear_bar).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.45f)
            )
            SegmentedControl(
                options = listOf(
                    SegmentedOption(
                        value = "NONE",
                        title = stringResource(R.string.settings_chip_disabled),
                        subtitle = stringResource(R.string.settings_chip_progress_none)
                    ),
                    SegmentedOption(
                        value = "TOTAL",
                        title = stringResource(R.string.settings_chip_col_series),
                        subtitle = stringResource(R.string.settings_chip_whole_series)
                    ),
                    SegmentedOption(
                        value = "SEASON",
                        title = stringResource(R.string.settings_chip_col_season),
                        subtitle = stringResource(R.string.settings_chip_current_season)
                    )
                ),
                selected = selectedLinear,
                accentColor = accentColor,
                vibrationEnabled = vibrationEnabled,
                context = context,
                onSelect = onSelectLinear
            )
        }

        // ── 2. Barra Circolare ──
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(R.string.settings_cat_circular_bar).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.45f)
            )
            SegmentedControl(
                options = listOf(
                    SegmentedOption(
                        value = "TOTAL",
                        title = stringResource(R.string.settings_chip_col_series),
                        subtitle = stringResource(R.string.settings_chip_whole_series)
                    ),
                    SegmentedOption(
                        value = "SEASON",
                        title = stringResource(R.string.settings_chip_col_season),
                        subtitle = stringResource(R.string.settings_chip_current_season)
                    )
                ),
                selected = selectedCircular,
                accentColor = accentColor,
                vibrationEnabled = vibrationEnabled,
                context = context,
                onSelect = onSelectCircular
            )
        }
    }
}

// ─── Text Size Options ────────────────────────────────────────────────────

@Composable
private fun TextSizeOptions(
    selected: Float,
    accentColor: Color,
    vibrationEnabled: Boolean,
    context: android.content.Context,
    onSelect: (Float) -> Unit
) {
    SegmentedControl(
        options = listOf(
            SegmentedOption(value = 0.8f, title = stringResource(R.string.settings_size_small)),
            SegmentedOption(value = 1.0f, title = stringResource(R.string.settings_size_medium)),
            SegmentedOption(value = 1.2f, title = stringResource(R.string.settings_size_large))
        ),
        selected = selected,
        accentColor = accentColor,
        vibrationEnabled = vibrationEnabled,
        context = context,
        height = 44.dp,
        onSelect = onSelect
    )
}

// ─── Folder Bookmarks Toggle Option ─────────────────────────────────────────

@Composable
private fun FolderBookmarksToggleOption(
    showFolderBookmarks: Boolean,
    accentColor: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onLongClick = {}) { onToggle(!showFolderBookmarks) }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_folder_bookmarks),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = stringResource(R.string.settings_folder_bookmarks_desc),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        FlickTroveSwitch(
            checked = showFolderBookmarks,
            onCheckedChange = onToggle,
            accentColor = accentColor
        )
    }
}

// ─── Badge Toggle Option ────────────────────────────────────────────────────

@Composable
private fun BadgeToggleOption(
    showBadges: Boolean,
    accentColor: Color,
    onShowBadgesInfo: () -> Unit = {},
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onLongClick = {}) { onToggle(!showBadges) }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_badges),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = stringResource(R.string.settings_badges_desc),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .bounceClick { onShowBadgesInfo() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Info Badge",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            FlickTroveSwitch(
                checked = showBadges,
                onCheckedChange = onToggle,
                accentColor = accentColor
            )
        }
    }
}
