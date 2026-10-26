package com.cinetrack.ui.components.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import com.cinetrack.LocalAdvancedVisualEffects
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
import com.cinetrack.ui.components.shared.ModalCloseButton

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
        mutableStateOf(if (persistedCircularProgressBar == "TOTAL") "TOTAL" else "SEASON")
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
            genreIds = listOf(878L, 28L)
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
            genreIds = listOf(18L, 80L),
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
            // ── Header ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
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

                ModalCloseButton(
                    onClick = onDismiss
                )
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
                PreviewCardMode.GRID_3_COL -> 264.dp
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
                        isLinearEnabled = previewMode != PreviewCardMode.LIST,
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
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bounceClick(scaleDown = 0.96f, onLongClick = {}) { onDismiss() }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
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
                        .background(accentColor, RoundedCornerShape(16.dp)),
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

// ─── Preview Mode Tab Items (Layerable for masking) ──────────────────────────

@Composable
private fun PreviewModeTabItems(
    modes: List<Triple<PreviewCardMode, String, Int>>,
    contentColor: Color,
    isInteractive: Boolean,
    pressedTabIndex: Int,
    onTabPressed: ((Int, Boolean) -> Unit)? = null,
    onSelect: (PreviewCardMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEachIndexed { index, (mode, label, iconRes) ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            if (isInteractive && onTabPressed != null) {
                DisposableEffect(isPressed) {
                    onTabPressed(index, isPressed)
                    onDispose { onTabPressed(index, false) }
                }
            }

            val isCurrentPressed = (isInteractive && isPressed) || (pressedTabIndex == index)
            val tabBounceScale by animateFloatAsState(
                targetValue = if (isCurrentPressed) 0.96f else 1f,
                animationSpec = spring(
                    stiffness = if (isCurrentPressed) 10000f else Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                label = "previewTabBounceScale_$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isInteractive) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onSelect(mode)
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer {
                        scaleX = tabBounceScale
                        scaleY = tabBounceScale
                    }
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
                            fontWeight = FontWeight.Bold,
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

// ─── Draggable Preview Mode Selector (FlickTrove Tab Physics) ────────────────

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
    val coroutineScope = rememberCoroutineScope()
    val advancedEffectsEnabled = LocalAdvancedVisualEffects.current
    val haptic = LocalHapticFeedback.current

    val padding = 2.dp

    BoxWithConstraints(
        modifier = modifier
            .height(32.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        val usableWidthPx = constraints.maxWidth.toFloat()
        val segmentCount = modes.size
        val realTabWidthPx = if (segmentCount > 0) usableWidthPx / segmentCount else 1f
        val paddingPx = with(density) { padding.toPx() }
        val normalWidthPx = (realTabWidthPx - 2 * paddingPx).coerceAtLeast(1f)

        val animLeft = remember { Animatable(selectedIndex * realTabWidthPx + paddingPx) }
        val animRight = remember { Animatable((selectedIndex + 1) * realTabWidthPx - paddingPx) }
        val animDragY = remember { Animatable(0f) }
        val flightDynamics = remember { Animatable(0f) }

        var isSelectedTabPressed by remember { mutableStateOf(false) }
        var isDragging by remember { mutableStateOf(false) }
        var previousIndex by remember { mutableStateOf(selectedIndex) }
        var pressedTabIndex by remember { mutableStateOf(-1) }

        LaunchedEffect(selectedIndex, realTabWidthPx, segmentCount, advancedEffectsEnabled) {
            val targetLeft = selectedIndex * realTabWidthPx + paddingPx
            val targetRight = (selectedIndex + 1) * realTabWidthPx - paddingPx

            val isTabChangedByClick = previousIndex != selectedIndex && !isDragging
            val indexDistance = kotlin.math.abs(selectedIndex - previousIndex).coerceAtLeast(1)
            previousIndex = selectedIndex

            if (!advancedEffectsEnabled) {
                launch { animLeft.snapTo(targetLeft) }
                launch { animRight.snapTo(targetRight) }
                return@LaunchedEffect
            }

            if (isTabChangedByClick) {
                val riseDuration = if (indexDistance > 1) 110 else 85
                val peakBoost = (0.24f + (indexDistance - 1) * 0.05f).coerceAtMost(0.34f)
                launch {
                    flightDynamics.snapTo(0f)
                    flightDynamics.animateTo(peakBoost, tween(riseDuration, easing = FastOutSlowInEasing))
                    flightDynamics.animateTo(
                        0f,
                        spring(
                            dampingRatio = 0.50f,
                            stiffness = 300f
                        )
                    )
                }
            }

            if (targetLeft > animLeft.value) {
                val jobRight = launch {
                    animRight.animateTo(
                        targetValue = targetRight,
                        animationSpec = spring(
                            stiffness = 650f,
                            dampingRatio = 0.64f,
                            visibilityThreshold = 0.5f
                        )
                    )
                }
                val jobLeft = launch {
                    animLeft.animateTo(
                        targetValue = targetLeft,
                        animationSpec = spring(
                            stiffness = 480f,
                            dampingRatio = 0.68f,
                            visibilityThreshold = 0.5f
                        )
                    )
                }
                jobRight.join()
                jobLeft.join()
            } else if (targetLeft < animLeft.value) {
                val jobLeft = launch {
                    animLeft.animateTo(
                        targetValue = targetLeft,
                        animationSpec = spring(
                            stiffness = 650f,
                            dampingRatio = 0.64f,
                            visibilityThreshold = 0.5f
                        )
                    )
                }
                val jobRight = launch {
                    animRight.animateTo(
                        targetValue = targetRight,
                        animationSpec = spring(
                            stiffness = 480f,
                            dampingRatio = 0.68f,
                            visibilityThreshold = 0.5f
                        )
                    )
                }
                jobRight.join()
                jobLeft.join()
            }
        }

        val dragScaleY by animateFloatAsState(
            targetValue = if (!advancedEffectsEnabled) 1f
                else if (isDragging) 1.25f
                else if (isSelectedTabPressed) 1.07f
                else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "previewDragScaleY"
        )
        val dragScaleX by animateFloatAsState(
            targetValue = if (!advancedEffectsEnabled) 1f
                else if (isDragging) 1.12f
                else if (isSelectedTabPressed) 1.04f
                else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "previewDragScaleX"
        )

        val pillPath = remember { Path() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedIndex, realTabWidthPx) {
                    var isDragActiveOnPill = false
                    val touchMargin = 8.dp.toPx()
                    var dragTargetLeft = 0f
                    var dragTargetRight = 0f
                    var dragTargetY = 0f

                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val currentLeft = animLeft.value
                            val currentRight = animRight.value
                            if (startOffset.x in (currentLeft - touchMargin)..(currentRight + touchMargin)) {
                                isDragActiveOnPill = true
                                isDragging = true
                                dragTargetLeft = currentLeft
                                dragTargetRight = currentRight
                                dragTargetY = 0f
                                coroutineScope.launch {
                                    launch { animLeft.stop() }
                                    launch { animRight.stop() }
                                    launch { animDragY.stop() }
                                }
                            } else {
                                isDragActiveOnPill = false
                            }
                        },
                        onDragEnd = {
                            if (!isDragActiveOnPill) return@detectDragGestures
                            isDragActiveOnPill = false
                            isDragging = false
                            val currentLeft = animLeft.value - paddingPx
                            val targetIndex = (currentLeft / realTabWidthPx).roundToInt().coerceIn(0, segmentCount - 1)

                            coroutineScope.launch {
                                launch { animDragY.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.6f)) }
                            }

                            if (targetIndex != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                onSelectMode(modes[targetIndex].first)
                            } else {
                                val targetLeft = selectedIndex * realTabWidthPx + paddingPx
                                val targetRight = (selectedIndex + 1) * realTabWidthPx - paddingPx
                                coroutineScope.launch {
                                    launch { animLeft.animateTo(targetLeft, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                    launch { animRight.animateTo(targetRight, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                }
                            }
                        },
                        onDragCancel = {
                            if (!isDragActiveOnPill) return@detectDragGestures
                            isDragActiveOnPill = false
                            isDragging = false
                            val targetLeft = selectedIndex * realTabWidthPx + paddingPx
                            val targetRight = (selectedIndex + 1) * realTabWidthPx - paddingPx
                            coroutineScope.launch {
                                launch { animDragY.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.6f)) }
                                launch { animLeft.animateTo(targetLeft, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                launch { animRight.animateTo(targetRight, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                            }
                        }
                    ) { change, dragAmount ->
                        if (!isDragActiveOnPill) return@detectDragGestures
                        isDragging = true
                        change.consume()

                        val dragX = dragAmount.x
                        val dragYDelta = dragAmount.y

                        dragTargetLeft += dragX
                        dragTargetRight += dragX
                        dragTargetY += dragYDelta

                        coroutineScope.launch {
                            val maxRight = segmentCount * realTabWidthPx - paddingPx
                            val maxLeft = maxRight - normalWidthPx

                            var targetLeft = dragTargetLeft
                            var targetRight = dragTargetRight

                            val maxOverscroll = normalWidthPx * 0.8f
                            if (dragTargetLeft < paddingPx) {
                                val overscroll = (paddingPx - dragTargetLeft).coerceAtMost(maxOverscroll)
                                val maxPenetration = paddingPx * 1.5f
                                val penetration = maxPenetration * (1f - kotlin.math.exp(-overscroll / 100f))
                                targetLeft = paddingPx - penetration
                                targetRight = (paddingPx + normalWidthPx) - overscroll * 0.15f
                            } else if (dragTargetRight > maxRight) {
                                val overscroll = (dragTargetRight - maxRight).coerceAtMost(maxOverscroll)
                                val maxPenetration = paddingPx * 1.5f
                                val penetration = maxPenetration * (1f - kotlin.math.exp(-overscroll / 100f))
                                targetRight = maxRight + penetration
                                targetLeft = maxLeft + overscroll * 0.15f
                            }

                            val leftStiffness = if (dragX > 0) 250f else if (dragX < 0) 800f else 600f
                            val rightStiffness = if (dragX > 0) 800f else if (dragX < 0) 250f else 600f

                            launch { animDragY.animateTo(dragTargetY, spring(stiffness = 1000f, dampingRatio = 0.7f)) }
                            launch { animLeft.animateTo(targetLeft, spring(stiffness = leftStiffness, dampingRatio = 0.65f)) }
                            launch { animRight.animateTo(targetRight, spring(stiffness = rightStiffness, dampingRatio = 0.65f)) }
                        }
                    }
                }
        ) {
            val divHeight = with(density) { 13.dp.toPx() }
            val currentLeft = animLeft.value
            val currentRight = animRight.value

            // 1. Inactive Base Layer (Muted white text, always visible beneath)
            PreviewModeTabItems(
                modes = modes,
                contentColor = Color.White.copy(alpha = 0.65f),
                isInteractive = true,
                pressedTabIndex = pressedTabIndex,
                onTabPressed = { index, pressed ->
                    pressedTabIndex = if (pressed) index else -1
                    if (index == selectedIndex) isSelectedTabPressed = pressed
                },
                onSelect = { mode ->
                    if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                    onSelectMode(mode)
                }
            )

            // 2. Sliding Solid Pill + Active Masked Layer (Black text over solid accentColor, rendered on top)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        // Dividers
                        for (k in 1 until segmentCount) {
                            val boundaryX = k * realTabWidthPx
                            val isPillNearBoundary = boundaryX in (currentLeft - 4.dp.toPx())..(currentRight + 4.dp.toPx())
                            if (!isPillNearBoundary) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.08f),
                                    start = Offset(boundaryX, (size.height - divHeight) / 2f),
                                    end = Offset(boundaryX, (size.height + divHeight) / 2f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }

                        // Sliding Solid Pill
                        val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                        val centerXPx = (currentLeft + currentRight) / 2f

                        val flightOffset = flightDynamics.value
                        val totalScaleY = if (advancedEffectsEnabled) dragScaleY * (1f + flightOffset) else 1f
                        val totalScaleX = if (advancedEffectsEnabled) dragScaleX * (1f + kotlin.math.min(0f, flightOffset * 0.6f)) else 1f

                        val baseHeight = size.height - (paddingPx * 2f)
                        val pillHeight = (baseHeight * totalScaleY).coerceAtLeast(1f)
                        val finalPillWidth = (currentWidth * totalScaleX).coerceAtLeast(1f)
                        val cornerRadius = pillHeight / 2f

                        val rawDragY = animDragY.value
                        val maxVisualDrag = paddingPx * 0.75f
                        val dragYOffset = if (rawDragY > 0) {
                            maxVisualDrag * (1f - kotlin.math.exp(-rawDragY / 80f))
                        } else if (rawDragY < 0) {
                            -maxVisualDrag * (1f - kotlin.math.exp(rawDragY / 80f))
                        } else 0f

                        val pillCenterY = (size.height / 2f) + dragYOffset
                        val pillTop = pillCenterY - (pillHeight / 2f)
                        val pillLeft = centerXPx - (finalPillWidth / 2f)

                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(pillLeft, pillTop),
                            size = Size(finalPillWidth, pillHeight),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )
                    }
                    .drawWithContent {
                        val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                        val centerXPx = (currentLeft + currentRight) / 2f

                        val flightOffset = flightDynamics.value
                        val totalScaleY = if (advancedEffectsEnabled) dragScaleY * (1f + flightOffset) else 1f
                        val totalScaleX = if (advancedEffectsEnabled) dragScaleX * (1f + kotlin.math.min(0f, flightOffset * 0.6f)) else 1f

                        val baseHeight = size.height - (paddingPx * 2f)
                        val pillHeight = (baseHeight * totalScaleY).coerceAtLeast(1f)
                        val finalPillWidth = (currentWidth * totalScaleX).coerceAtLeast(1f)
                        val cornerRadius = pillHeight / 2f

                        val rawDragY = animDragY.value
                        val maxVisualDrag = paddingPx * 0.75f
                        val dragYOffset = if (rawDragY > 0) {
                            maxVisualDrag * (1f - kotlin.math.exp(-rawDragY / 80f))
                        } else if (rawDragY < 0) {
                            -maxVisualDrag * (1f - kotlin.math.exp(rawDragY / 80f))
                        } else 0f

                        val pillCenterY = (size.height / 2f) + dragYOffset
                        val pillTop = pillCenterY - (pillHeight / 2f)
                        val pillLeft = centerXPx - (finalPillWidth / 2f)

                        pillPath.reset()
                        pillPath.addRoundRect(
                            RoundRect(
                                left = pillLeft,
                                top = pillTop,
                                right = pillLeft + finalPillWidth,
                                bottom = pillTop + pillHeight,
                                radiusX = cornerRadius,
                                radiusY = cornerRadius
                            )
                        )

                        clipPath(pillPath) {
                            this@drawWithContent.drawContent()
                        }
                    }
            ) {
                // Active Masked Layer (Black text over solid accentColor)
                PreviewModeTabItems(
                    modes = modes,
                    contentColor = Color.Black.copy(alpha = 0.9f),
                    isInteractive = false,
                    pressedTabIndex = -1,
                    onSelect = {}
                )
            }
        }
    }
}

// ─── Segmented Option Model & Items (Layerable for masking) ───────────────────

data class SegmentedOption<T>(
    val value: T,
    val title: String,
    val subtitle: String? = null
)

@Composable
internal fun <T> SegmentedTabItems(
    options: List<SegmentedOption<T>>,
    titleColor: Color,
    subtitleColor: Color,
    isInteractive: Boolean,
    pressedTabIndex: Int,
    onTabPressed: ((Int, Boolean) -> Unit)? = null,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            if (isInteractive && onTabPressed != null) {
                DisposableEffect(isPressed) {
                    onTabPressed(index, isPressed)
                    onDispose { onTabPressed(index, false) }
                }
            }

            val isCurrentPressed = (isInteractive && isPressed) || (pressedTabIndex == index)
            val tabBounceScale by animateFloatAsState(
                targetValue = if (isCurrentPressed) 0.96f else 1f,
                animationSpec = spring(
                    stiffness = if (isCurrentPressed) 10000f else Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                label = "segTabBounceScale_$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isInteractive) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onSelect(option.value)
                            }
                        } else Modifier
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer {
                        scaleX = tabBounceScale
                        scaleY = tabBounceScale
                    }
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = titleColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    if (!option.subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
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

// ─── Segmented Control (FlickTrove Tab Physics + Vanishing unselected indicator)

@Composable
internal fun <T> SegmentedControl(
    options: List<SegmentedOption<T>>,
    selected: T,
    accentColor: Color,
    vibrationEnabled: Boolean,
    context: android.content.Context,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 46.dp,
    enabled: Boolean = true,
    onSelect: (T) -> Unit
) {
    val selectedIndex = options.indexOfFirst { it.value == selected }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val advancedEffectsEnabled = LocalAdvancedVisualEffects.current
    val haptic = LocalHapticFeedback.current
    val isCompact = height <= 38.dp
    val padding = 2.5.dp
    val containerCornerRadius = if (isCompact) 12.dp else 14.dp

    val controlAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.40f,
        animationSpec = tween(200),
        label = "segControlAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { alpha = controlAlpha }
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(containerCornerRadius))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(containerCornerRadius))
    ) {
        val usableWidthPx = constraints.maxWidth.toFloat()
        val segmentCount = options.size
        val realTabWidthPx = if (segmentCount > 0) usableWidthPx / segmentCount else 1f
        val paddingPx = with(density) { padding.toPx() }
        val normalWidthPx = (realTabWidthPx - 2 * paddingPx).coerceAtLeast(1f)

        val initialLeft = if (selectedIndex >= 0) selectedIndex * realTabWidthPx + paddingPx else -realTabWidthPx
        val initialRight = if (selectedIndex >= 0) (selectedIndex + 1) * realTabWidthPx - paddingPx else 0f

        val animLeft = remember { Animatable(initialLeft) }
        val animRight = remember { Animatable(initialRight) }
        val animDragY = remember { Animatable(0f) }
        val flightDynamics = remember { Animatable(0f) }

        var isSelectedTabPressed by remember { mutableStateOf(false) }
        var isDragging by remember { mutableStateOf(false) }
        var previousIndex by remember { mutableStateOf(selectedIndex) }
        var pressedTabIndex by remember { mutableStateOf(-1) }

        val indicatorAlpha by animateFloatAsState(
            targetValue = if (selectedIndex >= 0) 1f else 0f,
            animationSpec = tween(150),
            label = "segIndicatorAlpha"
        )

        LaunchedEffect(selectedIndex, realTabWidthPx, segmentCount, advancedEffectsEnabled) {
            if (selectedIndex < 0) {
                previousIndex = selectedIndex
                return@LaunchedEffect
            }
            val targetLeft = selectedIndex * realTabWidthPx + paddingPx
            val targetRight = (selectedIndex + 1) * realTabWidthPx - paddingPx

            val wasPreviouslyUnselected = previousIndex < 0
            val isTabChangedByClick = previousIndex != selectedIndex && !isDragging && !wasPreviouslyUnselected
            val indexDistance = if (wasPreviouslyUnselected) 1 else kotlin.math.abs(selectedIndex - previousIndex).coerceAtLeast(1)
            previousIndex = selectedIndex

            if (wasPreviouslyUnselected) {
                animLeft.snapTo(targetLeft)
                animRight.snapTo(targetRight)
                return@LaunchedEffect
            }

            if (!advancedEffectsEnabled) {
                launch { animLeft.snapTo(targetLeft) }
                launch { animRight.snapTo(targetRight) }
                return@LaunchedEffect
            }

            if (isTabChangedByClick) {
                val riseDuration = if (indexDistance > 1) 110 else 85
                val peakBoost = (0.24f + (indexDistance - 1) * 0.05f).coerceAtMost(0.34f)
                launch {
                    flightDynamics.snapTo(0f)
                    flightDynamics.animateTo(peakBoost, tween(riseDuration, easing = FastOutSlowInEasing))
                    flightDynamics.animateTo(
                        0f,
                        spring(dampingRatio = 0.50f, stiffness = 300f)
                    )
                }
            }

            if (targetLeft > animLeft.value) {
                val jobRight = launch {
                    animRight.animateTo(
                        targetValue = targetRight,
                        animationSpec = spring(stiffness = 650f, dampingRatio = 0.64f, visibilityThreshold = 0.5f)
                    )
                }
                val jobLeft = launch {
                    animLeft.animateTo(
                        targetValue = targetLeft,
                        animationSpec = spring(stiffness = 480f, dampingRatio = 0.68f, visibilityThreshold = 0.5f)
                    )
                }
                jobRight.join()
                jobLeft.join()
            } else if (targetLeft < animLeft.value) {
                val jobLeft = launch {
                    animLeft.animateTo(
                        targetValue = targetLeft,
                        animationSpec = spring(stiffness = 650f, dampingRatio = 0.64f, visibilityThreshold = 0.5f)
                    )
                }
                val jobRight = launch {
                    animRight.animateTo(
                        targetValue = targetRight,
                        animationSpec = spring(stiffness = 480f, dampingRatio = 0.68f, visibilityThreshold = 0.5f)
                    )
                }
                jobRight.join()
                jobLeft.join()
            }
        }

        val dragScaleY by animateFloatAsState(
            targetValue = if (!advancedEffectsEnabled) 1f
                else if (isDragging) 1.25f
                else if (isSelectedTabPressed) 1.07f
                else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "segDragScaleY"
        )
        val dragScaleX by animateFloatAsState(
            targetValue = if (!advancedEffectsEnabled) 1f
                else if (isDragging) 1.12f
                else if (isSelectedTabPressed) 1.04f
                else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "segDragScaleX"
        )

        val pillPath = remember { Path() }
        val cornerRadiusPx = with(density) { (if (isCompact) 9.5.dp else 11.5.dp).toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedIndex, realTabWidthPx, enabled) {
                    if (!enabled) return@pointerInput
                    var isDragActiveOnPill = false
                    val touchMargin = 8.dp.toPx()
                    var dragTargetLeft = 0f
                    var dragTargetRight = 0f
                    var dragTargetY = 0f

                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (selectedIndex < 0) return@detectDragGestures
                            val currentLeft = animLeft.value
                            val currentRight = animRight.value
                            if (startOffset.x in (currentLeft - touchMargin)..(currentRight + touchMargin)) {
                                isDragActiveOnPill = true
                                isDragging = true
                                dragTargetLeft = currentLeft
                                dragTargetRight = currentRight
                                dragTargetY = 0f
                                coroutineScope.launch {
                                    launch { animLeft.stop() }
                                    launch { animRight.stop() }
                                    launch { animDragY.stop() }
                                }
                            } else {
                                isDragActiveOnPill = false
                            }
                        },
                        onDragEnd = {
                            if (!isDragActiveOnPill) return@detectDragGestures
                            isDragActiveOnPill = false
                            isDragging = false
                            val currentLeft = animLeft.value - paddingPx
                            val targetIndex = (currentLeft / realTabWidthPx).roundToInt().coerceIn(0, segmentCount - 1)

                            coroutineScope.launch {
                                launch { animDragY.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.6f)) }
                            }

                            if (targetIndex != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                onSelect(options[targetIndex].value)
                            } else {
                                val targetLeft = selectedIndex * realTabWidthPx + paddingPx
                                val targetRight = (selectedIndex + 1) * realTabWidthPx - paddingPx
                                coroutineScope.launch {
                                    launch { animLeft.animateTo(targetLeft, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                    launch { animRight.animateTo(targetRight, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                }
                            }
                        },
                        onDragCancel = {
                            if (!isDragActiveOnPill) return@detectDragGestures
                            isDragActiveOnPill = false
                            isDragging = false
                            val targetLeft = selectedIndex * realTabWidthPx + paddingPx
                            val targetRight = (selectedIndex + 1) * realTabWidthPx - paddingPx
                            coroutineScope.launch {
                                launch { animDragY.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.6f)) }
                                launch { animLeft.animateTo(targetLeft, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                                launch { animRight.animateTo(targetRight, spring(stiffness = 500f, dampingRatio = 0.70f)) }
                            }
                        }
                    ) { change, dragAmount ->
                        if (!isDragActiveOnPill) return@detectDragGestures
                        isDragging = true
                        change.consume()

                        val dragX = dragAmount.x
                        val dragYDelta = dragAmount.y

                        dragTargetLeft += dragX
                        dragTargetRight += dragX
                        dragTargetY += dragYDelta

                        coroutineScope.launch {
                            val maxRight = segmentCount * realTabWidthPx - paddingPx
                            val maxLeft = maxRight - normalWidthPx

                            var targetLeft = dragTargetLeft
                            var targetRight = dragTargetRight

                            val maxOverscroll = normalWidthPx * 0.8f
                            if (dragTargetLeft < paddingPx) {
                                val overscroll = (paddingPx - dragTargetLeft).coerceAtMost(maxOverscroll)
                                val maxPenetration = paddingPx * 1.5f
                                val penetration = maxPenetration * (1f - kotlin.math.exp(-overscroll / 100f))
                                targetLeft = paddingPx - penetration
                                targetRight = (paddingPx + normalWidthPx) - overscroll * 0.15f
                            } else if (dragTargetRight > maxRight) {
                                val overscroll = (dragTargetRight - maxRight).coerceAtMost(maxOverscroll)
                                val maxPenetration = paddingPx * 1.5f
                                val penetration = maxPenetration * (1f - kotlin.math.exp(-overscroll / 100f))
                                targetRight = maxRight + penetration
                                targetLeft = maxLeft + overscroll * 0.15f
                            }

                            val leftStiffness = if (dragX > 0) 250f else if (dragX < 0) 800f else 600f
                            val rightStiffness = if (dragX > 0) 800f else if (dragX < 0) 250f else 600f

                            launch { animDragY.animateTo(dragTargetY, spring(stiffness = 1000f, dampingRatio = 0.7f)) }
                            launch { animLeft.animateTo(targetLeft, spring(stiffness = leftStiffness, dampingRatio = 0.65f)) }
                            launch { animRight.animateTo(targetRight, spring(stiffness = rightStiffness, dampingRatio = 0.65f)) }
                        }
                    }
                }
        ) {
            val divHeight = with(density) { 
                if (options.any { !it.subtitle.isNullOrBlank() }) 19.dp.toPx() 
                else if (isCompact) 13.dp.toPx() 
                else 16.dp.toPx() 
            }
            val currentLeft = animLeft.value
            val currentRight = animRight.value

            // Inactive Base Layer (Muted greyish text, always visible beneath)
            SegmentedTabItems(
                options = options,
                titleColor = Color.White.copy(alpha = 0.75f),
                subtitleColor = Color.White.copy(alpha = 0.40f),
                isInteractive = enabled,
                pressedTabIndex = pressedTabIndex,
                onTabPressed = { index, pressed ->
                    if (enabled) {
                        pressedTabIndex = if (pressed) index else -1
                        if (index == selectedIndex) isSelectedTabPressed = pressed
                    }
                },
                onSelect = { value ->
                    if (enabled) {
                        if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                        onSelect(value)
                    }
                }
            )

            // Active Masked Layer + Pill Background & Inset Border (Rendered on top)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        // Dividers
                        for (k in 1 until segmentCount) {
                            val boundaryX = k * realTabWidthPx
                            val isPillNearBoundary = if (indicatorAlpha > 0.01f) {
                                boundaryX in (currentLeft - 4.dp.toPx())..(currentRight + 4.dp.toPx())
                            } else false

                            if (!isPillNearBoundary) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.08f),
                                    start = Offset(boundaryX, (size.height - divHeight) / 2f),
                                    end = Offset(boundaryX, (size.height + divHeight) / 2f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }

                        // Sliding Translucent Pill + Inset Border
                        if (indicatorAlpha > 0.01f) {
                            val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                            val centerXPx = (currentLeft + currentRight) / 2f

                            val flightOffset = flightDynamics.value
                            val totalScaleY = if (advancedEffectsEnabled) dragScaleY * (1f + flightOffset) else 1f
                            val totalScaleX = if (advancedEffectsEnabled) dragScaleX * (1f + kotlin.math.min(0f, flightOffset * 0.6f)) else 1f

                            val baseHeight = size.height - (paddingPx * 2f)
                            val pillHeight = (baseHeight * totalScaleY).coerceAtLeast(1f)
                            val finalPillWidth = (currentWidth * totalScaleX).coerceAtLeast(1f)

                            val rawDragY = animDragY.value
                            val maxVisualDrag = paddingPx * 0.75f
                            val dragYOffset = if (rawDragY > 0) {
                                maxVisualDrag * (1f - kotlin.math.exp(-rawDragY / 80f))
                            } else if (rawDragY < 0) {
                                -maxVisualDrag * (1f - kotlin.math.exp(rawDragY / 80f))
                            } else 0f

                            val pillCenterY = (size.height / 2f) + dragYOffset
                            val pillTop = pillCenterY - (pillHeight / 2f)
                            val pillLeft = centerXPx - (finalPillWidth / 2f)

                            // Background fill
                            drawRoundRect(
                                color = accentColor.copy(alpha = 0.14f * indicatorAlpha),
                                topLeft = Offset(pillLeft, pillTop),
                                size = Size(finalPillWidth, pillHeight),
                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            )

                            // Inset border stroke so it never clips at edges
                            val strokeWidthPx = 1.5.dp.toPx()
                            val halfStroke = strokeWidthPx / 2f
                            drawRoundRect(
                                color = accentColor.copy(alpha = indicatorAlpha),
                                topLeft = Offset(pillLeft + halfStroke, pillTop + halfStroke),
                                size = Size((finalPillWidth - strokeWidthPx).coerceAtLeast(1f), (pillHeight - strokeWidthPx).coerceAtLeast(1f)),
                                cornerRadius = CornerRadius((cornerRadiusPx - halfStroke).coerceAtLeast(0f), (cornerRadiusPx - halfStroke).coerceAtLeast(0f)),
                                style = Stroke(width = strokeWidthPx)
                            )
                        }
                    }
                    .drawWithContent {
                        if (indicatorAlpha > 0.01f) {
                            val currentWidth = (currentRight - currentLeft).coerceAtLeast(1f)
                            val centerXPx = (currentLeft + currentRight) / 2f

                            val flightOffset = flightDynamics.value
                            val totalScaleY = if (advancedEffectsEnabled) dragScaleY * (1f + flightOffset) else 1f
                            val totalScaleX = if (advancedEffectsEnabled) dragScaleX * (1f + kotlin.math.min(0f, flightOffset * 0.6f)) else 1f

                            val baseHeight = size.height - (paddingPx * 2f)
                            val pillHeight = (baseHeight * totalScaleY).coerceAtLeast(1f)
                            val finalPillWidth = (currentWidth * totalScaleX).coerceAtLeast(1f)

                            val rawDragY = animDragY.value
                            val maxVisualDrag = paddingPx * 0.75f
                            val dragYOffset = if (rawDragY > 0) {
                                maxVisualDrag * (1f - kotlin.math.exp(-rawDragY / 80f))
                            } else if (rawDragY < 0) {
                                -maxVisualDrag * (1f - kotlin.math.exp(rawDragY / 80f))
                            } else 0f

                            val pillCenterY = (size.height / 2f) + dragYOffset
                            val pillTop = pillCenterY - (pillHeight / 2f)
                            val pillLeft = centerXPx - (finalPillWidth / 2f)

                            pillPath.reset()
                            pillPath.addRoundRect(
                                RoundRect(
                                    left = pillLeft,
                                    top = pillTop,
                                    right = pillLeft + finalPillWidth,
                                    bottom = pillTop + pillHeight,
                                    radiusX = cornerRadiusPx,
                                    radiusY = cornerRadiusPx
                                )
                            )

                            clipPath(pillPath) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    }
            ) {
                // Active Masked Layer (Accent color, clipped strictly under the pill capsule)
                if (indicatorAlpha > 0.01f) {
                    SegmentedTabItems(
                        options = options,
                        titleColor = accentColor,
                        subtitleColor = accentColor.copy(alpha = 0.85f),
                        isInteractive = false,
                        pressedTabIndex = -1,
                        onSelect = {}
                    )
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
    isLinearEnabled: Boolean = true,
    onSelectLinear: (String) -> Unit,
    onSelectCircular: (String) -> Unit
) {
    val linearLabelAlpha by animateFloatAsState(
        targetValue = if (isLinearEnabled) 0.45f else 0.20f,
        animationSpec = tween(200),
        label = "linearLabelAlpha"
    )

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
                color = Color.White.copy(alpha = linearLabelAlpha)
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
                enabled = isLinearEnabled,
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
        height = 36.dp,
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
