package com.cinetrack.ui.components.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.LocalEpisodeInfoStyle
import com.cinetrack.LocalProgressBarTarget
import com.cinetrack.LocalTitleTextSizeMultiplier
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.NextEpisodeInfo
import com.cinetrack.data.model.Season
import com.cinetrack.ui.components.card.MovieCard
import com.cinetrack.ui.components.card.MovieListCard
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
    const val REMAINING = "REMAINING"
    const val WATCHED_OVER_TOTAL = "WATCHED_OVER_TOTAL"
    const val SEASON_ONLY = "SEASON_ONLY"
    const val SEASON_AND_TOTAL = "SEASON_AND_TOTAL"
    const val PERCENTAGE = "PERCENTAGE"
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

    return when (style) {
        EpisodeInfoStyle.REMAINING -> {
            if (nextInfo.remainingTotal > nextInfo.remainingInSeason) {
                " • ${nextInfo.remainingInSeason} (${nextInfo.remainingTotal} tot)"
            } else {
                " • ${nextInfo.remainingInSeason}"
            }
        }
        EpisodeInfoStyle.WATCHED_OVER_TOTAL -> {
            if (totalEpisodes > 0) " • $totalWatched / $totalEpisodes ep"
            else " • $totalWatched ep"
        }
        EpisodeInfoStyle.SEASON_ONLY -> {
            if (totalInSeason > 0) " • $watchedInSeason/$totalInSeason ep S${nextInfo.seasonNumber}"
            else " • S${nextInfo.seasonNumber}"
        }
        EpisodeInfoStyle.SEASON_AND_TOTAL -> {
            if (totalInSeason > 0 && totalEpisodes > 0) " • $watchedInSeason/$totalInSeason S${nextInfo.seasonNumber} • $totalWatched/$totalEpisodes tot"
            else if (totalInSeason > 0) " • $watchedInSeason/$totalInSeason S${nextInfo.seasonNumber}"
            else " • $totalWatched/$totalEpisodes tot"
        }
        EpisodeInfoStyle.PERCENTAGE -> {
            " • ${(seriesProgress * 100).toInt()}%"
        }
        else -> {
            if (nextInfo.remainingTotal > nextInfo.remainingInSeason) {
                " • ${nextInfo.remainingInSeason} (${nextInfo.remainingTotal} tot)"
            } else {
                " • ${nextInfo.remainingInSeason}"
            }
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
    val persistedProgressBarTarget by settingsViewModel.progressBarTarget.collectAsStateWithLifecycle()
    val persistedTextSizeMultiplier by settingsViewModel.titleTextSizeMultiplier.collectAsStateWithLifecycle()
    val persistedShowBadges by settingsViewModel.showBadges.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary

    // Local state for live preview (not yet persisted)
    var localEpisodeStyle by remember(persistedEpisodeInfoStyle) { mutableStateOf(persistedEpisodeInfoStyle) }
    var localProgressTarget by remember(persistedProgressBarTarget) { mutableStateOf(persistedProgressBarTarget) }
    var localTextMultiplier by remember(persistedTextSizeMultiplier) { mutableStateOf(persistedTextSizeMultiplier) }
    var localShowBadges by remember(persistedShowBadges) { mutableStateOf(persistedShowBadges) }

    // Mode selector for preview (3 columns, 2 columns, list)
    var previewMode by remember { mutableStateOf(PreviewCardMode.GRID_2_COL) }

    // Authentic sample Movie for preview with realistic posters and season metadata
    val mockMovie = remember {
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
            genreNamesString = "Drama, Crime",
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
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_grid),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings_card_appearance_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // ── Preview Mode Selector Tabs ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    Triple(PreviewCardMode.GRID_3_COL, stringResource(R.string.settings_card_preview_3_cols), R.drawable.ic_grid),
                    Triple(PreviewCardMode.GRID_2_COL, stringResource(R.string.settings_card_preview_2_cols), R.drawable.ic_grid),
                    Triple(PreviewCardMode.LIST, stringResource(R.string.settings_card_preview_list), R.drawable.ic_strisce)
                )
                modes.forEach { (mode, label, iconRes) ->
                    val isSelected = previewMode == mode
                    val tabBg by animateColorAsState(
                        targetValue = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        label = "tabBg"
                    )
                    val tabBorder by animateColorAsState(
                        targetValue = if (isSelected) accentColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f),
                        label = "tabBorder"
                    )
                    val contentColor = if (isSelected) accentColor else Color.White.copy(alpha = 0.6f)

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tabBg)
                            .border(0.5.dp, tabBorder, RoundedCornerShape(10.dp))
                            .bounceClick(scaleDown = 0.95f, onLongClick = {}) {
                                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                previewMode = mode
                            },
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = contentColor
                        )
                    }
                }
            }

            // ── Live Authentic Preview Area ───────────────────────────
            val previewHeight = when (previewMode) {
                PreviewCardMode.LIST -> 124.dp
                PreviewCardMode.GRID_3_COL -> 200.dp
                PreviewCardMode.GRID_2_COL -> 265.dp
            }
            val animPreviewHeight by animateDpAsState(
                targetValue = previewHeight,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "previewHeight"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(animPreviewHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(
                    LocalEpisodeInfoStyle provides localEpisodeStyle,
                    LocalProgressBarTarget provides localProgressTarget,
                    LocalTitleTextSizeMultiplier provides localTextMultiplier
                ) {
                    AnimatedContent(
                        targetState = previewMode,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                        label = "previewModeAnim"
                    ) { mode ->
                        when (mode) {
                            PreviewCardMode.GRID_3_COL -> {
                                MovieCard(
                                    movie = mockMovie,
                                    cardWidth = 118.dp,
                                    showBadges = localShowBadges,
                                    isFavorite = true,
                                    bounceEnabled = false
                                )
                            }
                            PreviewCardMode.GRID_2_COL -> {
                                MovieCard(
                                    movie = mockMovie,
                                    cardWidth = 160.dp,
                                    showBadges = localShowBadges,
                                    isFavorite = true,
                                    bounceEnabled = false
                                )
                            }
                            PreviewCardMode.LIST -> {
                                MovieListCard(
                                    movie = mockMovie,
                                    showBadges = localShowBadges,
                                    isFavorite = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

            // ── Options ───────────────────────────────────────────────
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalFadingEdges(scrollState)
                    .premiumScrollbar(state = scrollState, color = accentColor)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Testo Episodi
                AppearanceSection(title = stringResource(R.string.settings_card_episode_info)) {
                    EpisodeStyleOptions(
                        selected = localEpisodeStyle,
                        accentColor = accentColor,
                        onSelect = {
                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                            localEpisodeStyle = it
                        }
                    )
                }

                // 2. Progress Bar
                AppearanceSection(title = stringResource(R.string.settings_card_progress_bar)) {
                    ProgressBarOptions(
                        selected = localProgressTarget,
                        accentColor = accentColor,
                        onSelect = {
                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                            localProgressTarget = it
                        }
                    )
                }

                // 3. Grandezza Testo
                AppearanceSection(title = stringResource(R.string.settings_card_text_size)) {
                    TextSizeOptions(
                        selected = localTextMultiplier,
                        accentColor = accentColor,
                        onSelect = {
                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                            localTextMultiplier = it
                        }
                    )
                }

                // 4. Badge
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

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

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
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .bounceClick(scaleDown = 0.96f, onLongClick = {}) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.settings_cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                // Apply
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor)
                        .bounceClick(scaleDown = 0.96f, onLongClick = {}) {
                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                            settingsViewModel.updateEpisodeInfoStyle(localEpisodeStyle)
                            settingsViewModel.updateProgressBarTarget(localProgressTarget)
                            settingsViewModel.updateTitleTextSizeMultiplier(localTextMultiplier)
                            settingsViewModel.toggleBadges(localShowBadges)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.settings_apply),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black.copy(alpha = 0.87f)
                    )
                }
            }
        }
    }
}

// ─── Section Wrapper ──────────────────────────────────────────────────────

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
                letterSpacing = 1.1.sp,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        content()
    }
}

// ─── Episode Info Style Options ────────────────────────────────────────────

@Composable
private fun EpisodeStyleOptions(
    selected: String,
    accentColor: Color,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        EpisodeInfoStyle.REMAINING to R.string.settings_episode_style_remaining,
        EpisodeInfoStyle.WATCHED_OVER_TOTAL to R.string.settings_episode_style_watched_total,
        EpisodeInfoStyle.SEASON_ONLY to R.string.settings_episode_style_season_only,
        EpisodeInfoStyle.SEASON_AND_TOTAL to R.string.settings_episode_style_season_and_total,
        EpisodeInfoStyle.PERCENTAGE to R.string.settings_episode_style_percentage,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, labelRes) ->
            AppearanceOptionRow(
                label = stringResource(labelRes),
                selected = selected == key,
                accentColor = accentColor,
                onClick = { onSelect(key) }
            )
        }
    }
}

// ─── Progress Bar Options ─────────────────────────────────────────────────

@Composable
private fun ProgressBarOptions(
    selected: String,
    accentColor: Color,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        ProgressBarTarget.CIRCULAR_TOTAL to R.string.settings_progress_circular_total,
        ProgressBarTarget.CIRCULAR_SEASON to R.string.settings_progress_circular_season,
        ProgressBarTarget.LINEAR_TOTAL to R.string.settings_progress_linear_total,
        ProgressBarTarget.LINEAR_SEASON to R.string.settings_progress_linear_season,
        ProgressBarTarget.NONE to R.string.settings_progress_none,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, labelRes) ->
            AppearanceOptionRow(
                label = stringResource(labelRes),
                selected = selected == key,
                accentColor = accentColor,
                onClick = { onSelect(key) }
            )
        }
    }
}

// ─── Text Size Options ────────────────────────────────────────────────────

@Composable
private fun TextSizeOptions(
    selected: Float,
    accentColor: Color,
    onSelect: (Float) -> Unit
) {
    val options = listOf(
        0.8f to R.string.settings_size_small,
        1.0f to R.string.settings_size_medium,
        1.2f to R.string.settings_size_large,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (value, labelRes) ->
            val isSelected = selected == value
            val bg by animateColorAsState(
                targetValue = if (isSelected) accentColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                label = "textSizeBg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) accentColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f),
                label = "textSizeBorder"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .bounceClick(scaleDown = 0.95f, onLongClick = {}) { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = (12f * value).sp
                    ),
                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

// ─── Badge Toggle ─────────────────────────────────────────────────────────

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
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .bounceClick(scaleDown = 0.98f, onLongClick = {}) { onToggle(!showBadges) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
            com.cinetrack.ui.components.common.FlickTroveSwitch(
                checked = showBadges,
                onCheckedChange = onToggle,
                accentColor = accentColor
            )
        }
    }
}

// ─── Generic Option Row ───────────────────────────────────────────────────

@Composable
private fun AppearanceOptionRow(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.04f),
        label = "optBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f),
        label = "optBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
            .bounceClick(scaleDown = 0.97f, onLongClick = {}) { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) accentColor else Color.White.copy(alpha = 0.1f))
                .border(1.5.dp, if (selected) accentColor else Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                )
            }
        }
    }
}
