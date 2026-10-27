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

// ─── Constants & Formatters (Delegated to com.cinetrack.ui.components.card.EpisodeFormatters) ──

typealias EpisodeInfoStyle = com.cinetrack.ui.components.card.EpisodeInfoStyle
typealias ProgressBarTarget = com.cinetrack.ui.components.card.ProgressBarTarget
typealias PreviewCardMode = com.cinetrack.ui.components.card.PreviewCardMode

fun formatEpisodeInfo(
    style: String,
    nextInfo: NextEpisodeInfo,
    movie: Movie,
    seriesProgress: Float
): String = com.cinetrack.ui.components.card.formatEpisodeInfo(style, nextInfo, movie, seriesProgress)

fun calculateSeasonProgress(nextInfo: NextEpisodeInfo, movie: Movie): Float =
    com.cinetrack.ui.components.card.calculateSeasonProgress(nextInfo, movie)

@Composable
fun formatDrawerEpisodeSubtitle(
    style: String,
    nextInfo: NextEpisodeInfo,
    movie: Movie,
    seriesProgress: Float
): String = com.cinetrack.ui.components.card.formatDrawerEpisodeSubtitle(style, nextInfo, movie, seriesProgress)

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

