package com.cinetrack.ui.components.card

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
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
import com.cinetrack.data.model.NextEpisodeInfo
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.bounceClickWithOffset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.util.DateTimeUtils
import dev.chrisbanes.haze.HazeState
import kotlin.math.pow

private val UpToDateGreen = Color(0xFF10B981)



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
    precalculatedNextInfo: NextEpisodeInfo? = null,
    modifier: Modifier = Modifier
) {
    val effectiveWidth = cardWidth ?: 110.dp
    val nextInfo = precalculatedNextInfo ?: remember(movie.seasons, movie.watchedEpisodes, movie.numberOfEpisodes, movie.status) {
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
        DateTimeUtils.formatDateOnly(java.util.Date())
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

    val formattedNextDate = remember(nextAirDate, movie.firstAirDate, movie.releaseDate, isReleased) {
        val rawDate = nextAirDate ?: if (!isReleased) (movie.firstAirDate ?: movie.releaseDate)?.take(10) else null
        DateTimeUtils.formatAirDate(rawDate)
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

    val linearBar = com.cinetrack.LocalLinearProgressBar.current
    val showLinear = linearBar == "TOTAL" || linearBar == "SEASON"
    val targetLinearProgress = remember(linearBar, totalSeriesProgress, seasonProgress, isReleased) {
        if (!isReleased) 0f
        else when (linearBar) {
            "TOTAL" -> totalSeriesProgress.coerceIn(0f, 1f)
            "SEASON" -> seasonProgress.coerceIn(0f, 1f)
            else -> 0f
        }
    }
    val animatedLinearProgress by animateFloatAsState(
        targetValue = targetLinearProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "animatedLinearProgress"
    )
    val animatedLinearHeight by animateDpAsState(
        targetValue = if (isReleased && showLinear) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "linearBarHeight"
    )

    val circularBar = com.cinetrack.LocalCircularProgressBar.current
    val targetCircularProgress = remember(circularBar, totalSeriesProgress, seasonProgress, isReleased) {
        if (!isReleased) 0f
        else when (circularBar) {
            "SEASON" -> seasonProgress.coerceIn(0f, 1f)
            else -> totalSeriesProgress.coerceIn(0f, 1f)
        }
    }
    val animatedCircularProgress by animateFloatAsState(
        targetValue = targetCircularProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "animatedCircularProgress"
    )

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }
    val movieActions = LocalMovieActions.current
    val fullCardPosition = remember { arrayOf(Offset.Zero) }
    val fullCardSize = remember { arrayOf(Size.Zero) }

    var cardLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var actionButtonBoundsInCard by remember { mutableStateOf<Rect?>(null) }

    val actionButtonModifier = Modifier.onGloballyPositioned { btnCoords ->
        val parent = cardLayoutCoordinates
        if (parent != null && parent.isAttached && btnCoords.isAttached) {
            val topLeft = parent.localPositionOf(btnCoords, Offset.Zero)
            actionButtonBoundsInCard = Rect(
                topLeft = topLeft,
                bottomRight = Offset(topLeft.x + btnCoords.size.width, topLeft.y + btnCoords.size.height)
            )
        }
    }

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

    val isExploding = movieActions.explodingMovie?.id == movie.id
    val cardAlpha by animateFloatAsState(
        targetValue = if (isExploding) 0f else (if (isVisible) 1f else 0f),
        animationSpec = if (isExploding) snap() else tween(durationMillis = if (isScrollItem) 180 else 250, easing = LinearOutSlowInEasing),
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
                scaleX = cardScale * pressScale.value
                scaleY = cardScale * pressScale.value
                translationY = cardTranslateY * density.density
                clip = false
            }
            .onGloballyPositioned { coordinates ->
                cardLayoutCoordinates = coordinates
                fullCardPosition[0] = coordinates.positionInWindow()
                fullCardSize[0] = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
            }
            .pointerInput(Unit) {
                // If touch is on the action/check button, do NOT scale down the entire card
                var pressJob: Job? = null
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val touchPos = down.position
                    val isActionButtonTouch = actionButtonBoundsInCard?.let { bounds ->
                        bounds.inflate(12.dp.toPx()).contains(touchPos)
                    } ?: (touchPos.x >= (size.width - 56.dp.toPx()) && touchPos.y >= (size.height - 56.dp.toPx()))

                    if (isActionButtonTouch) {
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }

                    pressJob?.cancel()
                    pressJob = coroutineScope.launch {
                        pressScale.animateTo(
                            targetValue = 0.92f,
                            animationSpec = spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
                        )
                    }
                    waitForUpOrCancellation()
                    coroutineScope.launch {
                        pressJob.join()
                        pressScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        )
                    }
                }
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
            bounceEnabled = false, // la scala è gestita dall'outer Column (pressScale)
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
            onLongPress = { m, offset, pos ->
                movieActions.updatePopupCardSize(fullCardSize[0])
                val effectivePos = if (fullCardPosition[0] != Offset.Zero) fullCardPosition[0] else pos
                onLongPress?.invoke(m, offset, effectivePos)
            },
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
                .bounceClickWithOffset(
                    scaleDown = 0.92f,
                    animateOnPress = false,
                    requireUnconsumed = false,
                    onLongClick = { offset ->
                        movieActions.updatePopupCardSize(fullCardSize[0])
                        val posterHeightPx = with(density) { (effectiveWidth * 1.5f).toPx() }
                        val fullCardOffset = Offset(offset.x, posterHeightPx + offset.y)
                        onLongPress?.invoke(movie.apply {
                            this.favorite = isFavorite
                            this.watched = isWatched
                            this.reminder = isReminder
                            if (personalRating != null) this.personalRating = personalRating
                        }, fullCardOffset, fullCardPosition[0])
                    }
                ) { onPress(movie) }
        ) {
            // Pill progress bar tra le due sezioni per l'intera larghezza
            if (isReleased && (showLinear || animatedLinearProgress > 0.005f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(animatedLinearHeight)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    if (animatedLinearProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = animatedLinearProgress.coerceIn(0f, 1f))
                                .clip(CircleShape)
                                .background(if (isUpToDate) UpToDateGreen else appAccent)
                        )
                    }
                }
            }

            ContinueWatchingBottomRow(
                isReleased = isReleased,
                isUpToDate = isUpToDate,
                formattedNextDate = formattedNextDate,
                epText = epText,
                movie = movie,
                nextInfo = nextInfo,
                animatedSeriesProgress = animatedSeriesProgress,
                animatedCircularProgress = animatedCircularProgress,
                isUpdating = isUpdating,
                appAccent = appAccent,
                isReminder = isReminder,
                isFavorite = isFavorite,
                actionButtonModifier = actionButtonModifier,
                onAction = onAction,
                onMessage = onMessage,
                onQuickMarkWatched = onQuickMarkWatched
            )
    }
}
}

