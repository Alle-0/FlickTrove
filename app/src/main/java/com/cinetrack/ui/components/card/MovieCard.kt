package com.cinetrack.ui.components.card

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.ui.model.generateBadges
import com.cinetrack.ui.model.getLocalizedText
import com.cinetrack.ui.utils.*
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.toComposeColorOrNull
import dev.chrisbanes.haze.HazeState

// --- Main Component ---

@Composable
fun MovieCard(
    movie: Movie,
    cardWidth: Dp,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    isWatched: Boolean = false,
    isReminder: Boolean = false,
    progress: Float = 0f,
    personalRating: Double? = null,
    personalNote: String? = null,
    showActionHint: Boolean = true,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    folderColors: List<Color> = emptyList(),
    showFolderBookmarks: Boolean = true,
    showBadges: Boolean = true,
    showAdvancedBadges: Boolean = false,
    maxBadges: Int = if (cardWidth > 135.dp) 3 else 1,
    showActionButton: Boolean = true,
    bounceEnabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    hazeState: HazeState? = null,
    staggerIndex: Int = -1,
    hasAnimatedSet: MutableSet<String>? = null,
    onPress: (Movie) -> Unit = {},
    onLongPress: (Movie, Offset, Offset) -> Unit = { _, _, _ -> },
    onAction: (Movie) -> Unit = {},
    onMessage: (String) -> Unit = {}
) {
    val cardHeight = cardWidth * 1.5f
    val isTv = movie.mediaType == "tv"

    val computedProgress = remember(movie.watchedEpisodes, movie.numberOfEpisodes, progress) {
        if (progress > 0f) progress
        else if (isTv && !movie.watchedEpisodes.isNullOrEmpty() && (movie.numberOfEpisodes ?: 0) > 0) {
            val totalWatched = movie.watchedEpisodes!!.filter { it.key != "0" }.values.sumOf { it.size }
            (totalWatched.toFloat() / movie.numberOfEpisodes!!.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }
    val posterUrl = buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, LocalImageQuality.current)
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val movieActions = LocalMovieActions.current
    val cardPosition = remember { arrayOf(Offset.Zero) }
    val cardSize = remember { arrayOf(Size.Zero) }

    val glowColor = remember(movie.accentColor) {
        movie.accentColor.toComposeColorOrNull() ?: Color.Transparent
    }

    val rippleState = rememberCardRippleState()

    val entranceAnim = rememberCardEntranceAnimation(
        itemId = "${movie.id}_${movie.mediaType}",
        staggerIndex = staggerIndex,
        hasAnimatedSet = hasAnimatedSet,
        isExploding = movieActions.explodingMovie?.id == movie.id
    )

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .then(modifier)
            .graphicsLayer {
                alpha = entranceAnim.alpha
                scaleX = entranceAnim.scale
                scaleY = entranceAnim.scale
                translationY = entranceAnim.translateY * density.density
                clip = false
            }
            .onGloballyPositioned { coordinates ->
                cardPosition[0] = coordinates.positionInWindow()
                cardSize[0] = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .cardRipple(rippleState, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                .bounceClickWithOffset(
                    scaleDown = 0.93f,
                    animateOnPress = bounceEnabled,
                    requireUnconsumed = false,
                    onLongClick = { offset ->
                        movieActions.updatePopupCardSize(cardSize[0])
                        onLongPress(movie.apply {
                            this.favorite = isFavorite
                            this.watched = isWatched
                            this.reminder = isReminder
                            if (personalRating != null) this.personalRating = personalRating
                            if (!personalNote.isNullOrBlank()) this.personalNote = personalNote
                        }, offset, cardPosition[0])
                    }
                ) { offset -> 
                    onPress(movie) 
                },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (posterUrl != null) {
                    val imageRequest = remember(posterUrl) {
                        ImageRequest.Builder(context)
                            .data(posterUrl)
                            .crossfade(false)
                            .allowHardware(true)
                            .memoryCacheKey(posterUrl)
                            .diskCacheKey(posterUrl)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = movie.title ?: movie.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isTv) ImageVector.vectorResource(id = R.drawable.ic_tv) else ImageVector.vectorResource(id = R.drawable.ic_ciak),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Subtle color tint at bottom from accent color
            if (glowColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.35f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    glowColor.copy(alpha = 0.20f)
                                )
                            )
                        )
                )
            }

            // Subtle top vignette for badge and bookmark contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.35f),
                            1.0f to Color.Transparent
                        )
                    )
            )

            // Animated Folder Bookmarks (Top Left)
            val hasFolderBookmarks = showFolderBookmarks && folderColors.isNotEmpty()
            var lastFolderColors by remember(movie.id) { mutableStateOf(folderColors) }
            if (folderColors.isNotEmpty()) {
                lastFolderColors = folderColors
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = hasFolderBookmarks,
                enter = fadeIn(tween(250)) + slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut(tween(180)) + slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                MovieFolderBookmarks(lastFolderColors)
            }

            // Dynamic Badges (Top Right)
            val isLarge = cardWidth > 135.dp
            val disabledBadges = com.cinetrack.LocalDisabledBadges.current
            val effectiveShowAdvanced = showAdvancedBadges || isLarge
            val badges = remember(movie, effectiveShowAdvanced) { movie.generateBadges(effectiveShowAdvanced) }
            val visibleBadges = remember(badges, disabledBadges, maxBadges) {
                badges.filter { it.text !in disabledBadges }.take(maxBadges)
            }

            val hasSeasonScope = !movie.seasonScope.isNullOrBlank()
            androidx.compose.animation.AnimatedVisibility(
                visible = showBadges && maxBadges > 0 && visibleBadges.isNotEmpty(),
                enter = fadeIn(tween(250)) + scaleIn(tween(250, easing = FastOutSlowInEasing), initialScale = 0.8f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200, easing = FastOutSlowInEasing), targetScale = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    visibleBadges.forEach { badge ->
                        MovieCardBadge(
                            text = badge.getLocalizedText(), 
                            color = Color(badge.colorValue), 
                            hazeState = hazeState, 
                            isLarge = isLarge
                        )
                    }
                }
            }

            val guiAlpha by if (animatedVisibilityScope != null) {
                animatedVisibilityScope.transition.animateFloat(
                    label = "guiAlpha",
                    transitionSpec = { tween(durationMillis = 300) }
                ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
            } else {
                remember { mutableStateOf(1f) }
            }

            Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = guiAlpha }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Transparent,
                                0.25f to Color.Black.copy(alpha = 0.20f),
                                0.50f to Color.Black.copy(alpha = 0.55f),
                                0.75f to Color.Black.copy(alpha = 0.82f),
                                1.00f to Color.Black.copy(alpha = 0.96f)
                            )
                        )
                )

                val displayRating = personalRating ?: movie.personalRating
                val hasRating = displayRating != null && displayRating > 0.0
                val isLarge = cardWidth > 150.dp
                val multiplier = com.cinetrack.LocalTitleTextSizeMultiplier.current
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(
                            start = if (isLarge) 16.dp else 12.dp, 
                            end = if (isLarge) 16.dp else 12.dp, 
                            bottom = if (hasRating) (if (isLarge) 13.dp else 9.dp) else (if (isLarge) 15.dp else 11.dp)
                        ),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    if (hasSeasonScope) {
                        val formattedScope = remember(movie.seasonScope, context) {
                            val raw = movie.seasonScope!!.trim()
                            val seasonRegex = Regex("(?i)\\b(?:stagion[ie]|seasons?|staffeln?|temporadas?|saisons?|st\\.?|s)(?:\\s*|\\b)([0-9]+(?:\\s*[\u2013\u2014\\-]\\s*[0-9]+)?)")
                            val epRegex = Regex("(?i)\\b(?:episod[io]|episodes?|ep\\.?|e)(?:\\s*|\\b)([0-9]+(?:\\s*[\u2013\u2014\\-]\\s*[0-9]+)?)")

                            val sMatch = seasonRegex.find(raw)
                            val eMatch = epRegex.find(raw)

                            val localized = when {
                                sMatch != null && eMatch != null -> {
                                    val sVal = sMatch.groupValues[1].replace("-", "\u2013").trim()
                                    val eVal = eMatch.groupValues[1].replace("-", "\u2013").trim()
                                    context.getString(R.string.editorial_season_episode_scope, sVal, eVal)
                                }
                                sMatch != null -> {
                                    val sVal = sMatch.groupValues[1].replace("-", "\u2013").trim()
                                    if (sVal.contains("\u2013")) {
                                        context.getString(R.string.editorial_season_scope_range, sVal)
                                    } else {
                                        context.getString(R.string.editorial_season_scope_single, sVal)
                                    }
                                }
                                eMatch != null -> {
                                    val eVal = eMatch.groupValues[1].replace("-", "\u2013").trim()
                                    context.getString(R.string.editorial_episode_scope, eVal)
                                }
                                else -> raw
                            }
                            localized.replace(" ", "\u00A0").uppercase()
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.5.dp),
                            modifier = Modifier
                                .padding(bottom = 3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF0369A1).copy(alpha = 0.40f))
                                .border(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.85f), RoundedCornerShape(50))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_tv),
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(9.dp)
                            )
                            Text(
                                text = formattedScope,
                                color = Color(0xFFE0F2FE),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp,
                                maxLines = 1,
                                softWrap = false,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }

                    val titleEndPadding = if (!showActionButton) 0.dp else (if (isLarge) 36.dp else 24.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = titleEndPadding),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = movie.title ?: movie.name ?: "",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = ((if (isLarge) 14.5f else 13f) * multiplier).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            lineHeight = ((if (isLarge) 17.5f else 16f) * multiplier).sp,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                        if (hasRating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(((if (isLarge) 13.5f else 11.5f) * multiplier).dp)
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f", displayRating),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = ((if (isLarge) 13.5f else 11.5f) * multiplier).sp,
                                    fontWeight = FontWeight.Black,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                        }
                    }
                }

                if (showActionButton) {
                    MovieCardActionButton(
                        movie = movie,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        isFavorite = isFavorite,
                        isWatched = isWatched,
                        isReminder = isReminder,
                        computedProgress = computedProgress,
                        showActionHint = showActionHint,
                        isLarge = isLarge,
                        rippleState = rippleState,
                        onAction = onAction,
                        onMessage = onMessage,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}
}
