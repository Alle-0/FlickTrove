package com.cinetrack.ui.components.card

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

private val UpToDateGreen = Color(0xFF10B981)

/**
 * Checkmark button where the season progressive border is drawn directly ON the border of the button.
 * Eliminates outer halos or floating rings: the arc and track form the boundary of the button.
 */
@Composable
private fun SeasonProgressCheckButton(
    progress: Float,
    accentColor: Color,
    isUpdating: Boolean = false,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(24.dp)
            .bounceClick(
                scaleDown = 0.85f,
                onClick = {
                    if (!isUpdating) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Progressive border right on the border of the button
        Canvas(modifier = Modifier.fillMaxSize().padding(1.dp)) {
            val sw = 2.dp.toPx()
            // Track
            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = sw)
            )
            // Progress fill
            if (progress > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
            }
        }

        // Inner circular button fill & icon
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(accentColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(11.dp),
                    strokeWidth = 1.5.dp,
                    color = accentColor
                )
            } else {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick_card),
                    contentDescription = contentDescription,
                    tint = accentColor,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

/**
 * ContinueWatchingSeriesCard
 * Features the natural FlickTrove MovieCard on top (whose eye button outline indicates
 * the TOTAL series progress), and an attached bottom protrusion.
 * - Always uses the global app primary accent (not the individual movie's poster accent).
 * - Season circular progress border sits directly on the border of the 24dp check button.
 * - When in progress: shows Sxx Exx • remaining and check button with season progress ring.
 * - When up to date: shows "In pari" / "fino al [data]" in emerald green with completed check ring.
 */
@Composable
fun ContinueWatchingSeriesCard(
    movie: Movie,
    cardWidth: Dp? = null,
    isUpdating: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isFavorite: Boolean = movie.favorite,
    isWatched: Boolean = movie.watched,
    isReminder: Boolean = movie.reminder,
    progress: Float = (movie.progress ?: 0.0).toFloat(),
    personalRating: Double? = movie.personalRating,
    folderColors: List<Color> = emptyList(),
    showFolderBookmarks: Boolean = true,
    showBadges: Boolean = true,
    showAdvancedBadges: Boolean = false,
    hazeState: HazeState? = null,
    staggerIndex: Int = -1,
    hasAnimatedSet: MutableSet<String>? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onPress: (Movie) -> Unit,
    onLongPress: ((Movie, Offset, Offset) -> Unit)? = null,
    onQuickMarkWatched: (Movie) -> Unit,
    onAction: (Movie) -> Unit = {},
    onMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val effectiveWidth = cardWidth ?: 110.dp
    val nextInfo = remember(movie.seasons, movie.watchedEpisodes, movie.numberOfEpisodes, movie.status) {
        movie.calculateNextEpisode()
    }

    // Fallback: If metadata is not present, display the normal MovieCard
    if (nextInfo == null) {
        MovieCard(
            movie = movie,
            cardWidth = effectiveWidth,
            modifier = modifier,
            isFavorite = isFavorite,
            isWatched = isWatched,
            isReminder = isReminder,
            progress = progress,
            personalRating = personalRating,
            folderColors = folderColors,
            showFolderBookmarks = showFolderBookmarks,
            showBadges = showBadges,
            showAdvancedBadges = showAdvancedBadges,
            hazeState = hazeState,
            staggerIndex = staggerIndex,
            hasAnimatedSet = hasAnimatedSet,
            animatedVisibilityScope = animatedVisibilityScope,
            onPress = onPress,
            onLongPress = onLongPress ?: { _, _, _ -> },
            onAction = onAction,
            onMessage = onMessage
        )
        return
    }

    val context = LocalContext.current
    val cornerRadius = 24.dp
    val appAccent = accentColor // App global accent (MaterialTheme.colorScheme.primary), not movie.accentColor
    val isReleased = movie.isReleased

    val isUpToDate = isReleased && (nextInfo.isUpToDateWithAirDate || (nextInfo.remainingTotal == 0 && nextInfo.remainingInSeason == 0))

    // Calculate future air date if up-to-date or unreleased
    val todayIso = remember {
        try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        } catch (_: Exception) {
            "2026-01-01"
        }
    }

    val nextAirDate = remember(movie, todayIso) {
        val nDate = movie.nextEpisodeAirDate?.take(10)
        if (!nDate.isNullOrBlank() && nDate >= todayIso) {
            nDate
        } else {
            val futureDates = mutableListOf<String>()
            val premiere = (movie.firstAirDate ?: movie.releaseDate)?.take(10)
            if (!premiere.isNullOrBlank() && premiere >= todayIso) {
                futureDates.add(premiere)
            }
            movie.seasons?.forEach { s ->
                val sDate = s.airDate?.take(10)
                if (!sDate.isNullOrBlank() && sDate >= todayIso) futureDates.add(sDate)
                s.episodes?.forEach { ep ->
                    val epDate = ep.airDate?.take(10)
                    if (!epDate.isNullOrBlank() && epDate >= todayIso) futureDates.add(epDate)
                }
            }
            futureDates.minOrNull()
        }
    }

    val formattedNextDate = remember(nextAirDate, movie.firstAirDate, movie.releaseDate) {
        val rawDate = nextAirDate ?: (movie.firstAirDate ?: movie.releaseDate)?.take(10)
        if (rawDate.isNullOrBlank()) null
        else {
            try {
                if (rawDate.length >= 10) {
                    val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val date = parser.parse(rawDate)
                    if (date != null) {
                        val calDate = java.util.Calendar.getInstance().apply { time = date }
                        val calNow = java.util.Calendar.getInstance()
                        val pattern = if (calDate.get(java.util.Calendar.YEAR) != calNow.get(java.util.Calendar.YEAR)) {
                            "d MMM yy"
                        } else {
                            "d MMM"
                        }
                        val formatter = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
                        formatter.format(date)
                    } else null
                } else if (rawDate.length == 4) {
                    rawDate
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    val epText = remember(nextInfo.seasonNumber, nextInfo.episodeNumber) {
        String.format("S%02d E%02d", nextInfo.seasonNumber, nextInfo.episodeNumber)
    }

    // Calculate current season progress
    val currentSeason = remember(movie.seasons, nextInfo.seasonNumber) {
        movie.seasons?.find { it.seasonNumber == nextInfo.seasonNumber }
    }
    val watchedInSeason = remember(movie.watchedEpisodes, nextInfo.seasonNumber) {
        movie.watchedEpisodes?.get(nextInfo.seasonNumber.toString())?.size ?: 0
    }
    val totalInSeason = remember(currentSeason, watchedInSeason, nextInfo.remainingInSeason) {
        maxOf(currentSeason?.episodeCount ?: 0, watchedInSeason + nextInfo.remainingInSeason)
    }
    val seasonProgress = remember(watchedInSeason, totalInSeason, isUpToDate) {
        if (isUpToDate) 1f
        else if (totalInSeason > 0) (watchedInSeason.toFloat() / totalInSeason.toFloat()).coerceIn(0f, 1f)
        else 0f
    }
    val animatedSeasonProgress by animateFloatAsState(
        targetValue = seasonProgress,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "seasonProgress"
    )

    // Calculate total series progress for the Netflix-style horizontal progress bar
    val totalSeriesProgress = remember(movie.watchedEpisodes, movie.numberOfEpisodes, progress, nextInfo.progress, isUpToDate) {
        if (isUpToDate) 1f
        else if (progress > 0f) progress
        else if (!movie.watchedEpisodes.isNullOrEmpty() && (movie.numberOfEpisodes ?: 0) > 0) {
            val totalWatched = movie.watchedEpisodes!!.filter { it.key != "0" }.values.sumOf { it.size }
            (totalWatched.toFloat() / movie.numberOfEpisodes!!.toFloat()).coerceIn(0f, 1f)
        } else nextInfo.progress
    }
    val animatedSeriesProgress by animateFloatAsState(
        targetValue = totalSeriesProgress.coerceIn(0f, 1f),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "seriesTotalProgress"
    )

    val density = LocalDensity.current

    // Premium Staggered Entrance Animation States (applies to the entire series card: poster + protrusion)
    val compositeId = "${movie.id}_${movie.mediaType}"
    val hasAnimated = rememberSaveable { mutableStateOf(hasAnimatedSet?.contains(compositeId) == true) }

    LaunchedEffect(movie.id) {
        if (hasAnimated.value) return@LaunchedEffect
        if (staggerIndex in 0..11) delay(staggerIndex * 40L)
        hasAnimated.value = true
        hasAnimatedSet?.add(compositeId)
    }

    val isScrollItem = staggerIndex < 0 || staggerIndex >= 12
    val isVisible = hasAnimated.value || isScrollItem

    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (isScrollItem) 180 else 250, easing = LinearOutSlowInEasing),
        label = "seriesCardAlpha"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = if (isScrollItem) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "seriesCardScale"
    )

    val cardTranslateY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 40f,
        animationSpec = if (isScrollItem) tween(200, easing = FastOutSlowInEasing) else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "seriesCardTranslateY"
    )

    Column(
        modifier = modifier
            .width(effectiveWidth)
            .graphicsLayer {
                alpha = cardAlpha
                scaleX = cardScale
                scaleY = cardScale
                translationY = cardTranslateY * density.density
                clip = false
            }
    ) {
        // --- 1. MovieCard al naturale on top (clean locandina without duplicate eye action button) ---
        MovieCard(
            movie = movie,
            cardWidth = effectiveWidth,
            isFavorite = isFavorite,
            isWatched = isWatched,
            isReminder = isReminder,
            progress = progress,
            personalRating = personalRating,
            showActionButton = false,
            shape = RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = cornerRadius,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            ),
            folderColors = folderColors,
            showFolderBookmarks = showFolderBookmarks,
            showBadges = showBadges,
            showAdvancedBadges = showAdvancedBadges,
            hazeState = hazeState,
            staggerIndex = -1,
            hasAnimatedSet = null,
            animatedVisibilityScope = animatedVisibilityScope,
            onPress = onPress,
            onLongPress = onLongPress ?: { _, _, _ -> },
            onAction = onAction,
            onMessage = onMessage
        )

        // --- 2. Attached Bottom Protrusion with Netflix-style Series Progress Bar ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = cornerRadius,
                        bottomEnd = cornerRadius
                    )
                )
                .background(Color(0xFF1E1E22))
                .bounceClick(scaleDown = 0.98f) { onPress(movie) }
        ) {
            // Netflix-style horizontal series progress bar along the junction
            if (isReleased) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    if (animatedSeriesProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = animatedSeriesProgress)
                                .background(if (isUpToDate) UpToDateGreen else appAccent)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 38.dp)
                    .padding(start = 11.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
            if (!isReleased) {
                // UNRELEASED STATE: Serie non ancora uscita ("In arrivo")
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = stringResource(R.string.lbl_coming_soon),
                        color = appAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    if (!formattedNextDate.isNullOrBlank()) {
                        Text(
                            text = formattedNextDate,
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }
            } else if (isUpToDate) {
                // UP TO DATE STATE
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = stringResource(R.string.lbl_up_to_date),
                        color = UpToDateGreen,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    if (!formattedNextDate.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.lbl_until_date, formattedNextDate),
                            color = UpToDateGreen.copy(alpha = 0.80f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }

                // Completed green check button with 100% progress ring on the border
                SeasonProgressCheckButton(
                    progress = 1f,
                    accentColor = UpToDateGreen,
                    contentDescription = stringResource(R.string.lbl_up_to_date),
                    onClick = {
                        val msg = if (!formattedNextDate.isNullOrBlank()) {
                            context.getString(R.string.lbl_up_to_date) + " • " + context.getString(R.string.lbl_until_date, formattedNextDate)
                        } else {
                            context.getString(R.string.lbl_up_to_date)
                        }
                        onMessage(msg)
                    }
                )
            } else {
                // IN PROGRESS STATE:
                // Line 1: S01 E04
                // Line 2: ancora 71 (lbl_episodes_remaining)
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = epText,
                        color = appAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    if (nextInfo.remainingInSeason > 0) {
                        Text(
                            text = stringResource(R.string.lbl_episodes_remaining, nextInfo.remainingInSeason),
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }

                // 1-tap Checkmark button (✓) with Season Circular Progress directly on the button border
                SeasonProgressCheckButton(
                    progress = animatedSeasonProgress,
                    accentColor = appAccent,
                    isUpdating = isUpdating,
                    contentDescription = stringResource(R.string.cd_mark_episode_watched, epText),
                    onClick = { onQuickMarkWatched(movie) }
                )
            }
        }
    }
}
}

