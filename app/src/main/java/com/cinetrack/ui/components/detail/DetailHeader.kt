@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
package com.cinetrack.ui.components.detail

import androidx.compose.ui.res.stringResource
import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clipToBounds
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.composed
import coil.request.ImageRequest
import coil.compose.SubcomposeAsyncImageContent
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.viewmodel.ExternalRatings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.LocalAdvancedVisualEffects
import com.cinetrack.data.api.MovieDetailResponse
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.text.style.TextOverflow

/**
 * DetailHeader
 * Fused Container with TMDB rating, dynamic age rating, and technical specifications
 * that smoothly expand into dedicated inspection zones.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailHeader(
    movie: Movie,
    ratings: ExternalRatings,
    accentColor: Color,
    movieDetails: MovieDetailResponse? = null,
    matchPercentage: Int? = null,
    logoPath: String? = null,
    hazeState: HazeState? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onRatingClick: () -> Unit = {},
    hasAlternativeCovers: Boolean = false,
    onCoverSelectClick: (() -> Unit)? = null,
    onTrailerClick: ((Rect?) -> Unit)? = null,
    isOffline: Boolean = false,
    movieStatus: String? = null,
    modifier: Modifier = Modifier
) {
    var expandedSection by remember { mutableStateOf(DetailHeaderExpandedSection.NONE) }

    val hasAwards = !ratings.awards.isNullOrBlank() && ratings.awards != "N/A"
    val hasBoxOffice = (movie.revenue ?: 0L) > 0

    val parentsGuideData = remember(ratings.certification, ratings.certificationCountry) {
        ParentsGuideResolver.resolve(ratings.certification, ratings.certificationCountry)
    }

    val sectionHeights = remember { mutableStateMapOf<DetailHeaderExpandedSection, Int>() }
    var measuredContentHeightPx by remember { mutableIntStateOf(0) }

    var lastNonNoneSection by remember { mutableStateOf(DetailHeaderExpandedSection.RATINGS) }
    LaunchedEffect(expandedSection) {
        if (expandedSection != DetailHeaderExpandedSection.NONE) {
            lastNonNoneSection = expandedSection
        }
    }
    val activeDisplaySection = if (expandedSection != DetailHeaderExpandedSection.NONE) {
        expandedSection
    } else {
        lastNonNoneSection
    }

    val targetHeightPx = if (expandedSection != DetailHeaderExpandedSection.NONE) {
        sectionHeights[expandedSection] ?: measuredContentHeightPx
    } else {
        0
    }

    val animatedHeightPx by animateIntAsState(
        targetValue = targetHeightPx,
        animationSpec = tween(
            durationMillis = 380,
            easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        ),
        label = "HeaderSectionHeight"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (expandedSection != DetailHeaderExpandedSection.NONE) 1f else 0f,
        animationSpec = tween(
            durationMillis = 280,
            easing = LinearEasing
        ),
        label = "HeaderContentAlpha"
    )

    val ratingsExpansion by animateFloatAsState(
        targetValue = if (expandedSection == DetailHeaderExpandedSection.RATINGS) 1f else 0f,
        animationSpec = tween(
            durationMillis = 380,
            easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        ),
        label = "RatingsExpansion"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                
                val heightToSubtract = animatedHeightPx
                val stableHeight = (placeable.height - heightToSubtract).coerceAtLeast(0)
                
                layout(placeable.width, stableHeight) {
                    placeable.place(0, -heightToSubtract)
                }
            }
    ) {
        val isTvSeries = movie.mediaType == "tv"
        val effectiveStatus = movieStatus?.takeIf { it.isNotBlank() } ?: movie.status
        val seriesStatusLabel = if (isTvSeries && movie.isReleased && !effectiveStatus.isNullOrBlank()) {
            when (effectiveStatus) {
                "Returning Series" -> stringResource(R.string.status_returning_series)
                "Planned" -> stringResource(R.string.status_planned)
                "In Production" -> stringResource(R.string.status_in_production)
                "Ended" -> stringResource(R.string.status_ended)
                "Canceled", "Cancelled" -> stringResource(R.string.status_canceled)
                "Pilot" -> stringResource(R.string.status_pilot)
                "Rumored" -> stringResource(R.string.status_rumored)
                "Post Production" -> stringResource(R.string.status_post_production)
                "Released" -> stringResource(R.string.status_released)
                else -> effectiveStatus
            }
        } else null

        // Title & Tagline
        Column(modifier = Modifier.fillMaxWidth()) {
            @Composable
            fun FallbackTitleText() {
                Text(
                    text = movie.displayName.ifEmpty { "-" },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 44.sp,
                        letterSpacing = (-1.5).sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
            }

            if (logoPath != null) {
                val imageUrl = com.cinetrack.util.buildTmdbImageUrl(logoPath, com.cinetrack.util.ImageType.LOGO, com.cinetrack.util.LocalImageQuality.current)
                val context = LocalContext.current
                val request = remember(imageUrl, isOffline) {
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .crossfade(500)
                        .build()
                }
                coil.compose.SubcomposeAsyncImage(
                    model = request,
                    contentDescription = movie.title ?: movie.name ?: "",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.6f) // Takes up to 60% of width
                        .heightIn(max = 100.dp) // Max height to avoid huge logos
                        .padding(bottom = 8.dp),
                    alignment = Alignment.CenterStart
                ) {
                    val state = painter.state
                    if (state is coil.compose.AsyncImagePainter.State.Error) {
                        FallbackTitleText()
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
            } else {
                FallbackTitleText()
            }

            movie.tagline?.let {
                if (it.isNotEmpty()) {
                    Text(
                        text = it.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        ),
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
            }


            val showCoverBtn = movie.customBackdropPath != null && onCoverSelectClick != null
            val showTrailerBtn = onTrailerClick != null

            if (matchPercentage != null || showTrailerBtn || showCoverBtn) {
                val advancedEffectsEnabled = LocalAdvancedVisualEffects.current
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (matchPercentage != null) {
                        if (advancedEffectsEnabled) {
                            // Con blur: sfondo traslucido colorato + testo accentColor (blur fornisce contrasto)
                            Box(
                                modifier = Modifier
                                    .hazeGlass(
                                        state = hazeState,
                                        shape = CircleShape,
                                        containerColor = accentColor.copy(alpha = 0.15f),
                                        blurRadius = 16.dp
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${matchPercentage}% Match",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = ColorUtils.lightenForText(accentColor, 1.35f)
                                )
                            }
                        } else {
                            // Senza blur: sfondo scuro solido + testo accentColor (contrasto garantito)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1A22))
                                    .border(0.5.dp, ColorUtils.lightenForText(accentColor, 1.25f).copy(alpha = 0.7f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${matchPercentage}% Match",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = ColorUtils.lightenForText(accentColor, 1.35f)
                                )
                            }
                        }
                    }

                    if (onTrailerClick != null) {
                        var buttonCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Row(
                            modifier = Modifier
                                .onGloballyPositioned { buttonCoords = it }
                                .bounceClick { onTrailerClick(buttonCoords?.boundsInRoot()) }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_play),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = ColorUtils.lightenForText(accentColor, 1.35f)
                            )
                            Text(
                                text = "Trailer",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = ColorUtils.lightenForText(accentColor, 1.35f)
                            )
                        }
                    }

                    if (showCoverBtn) {
                        Row(
                            modifier = Modifier
                                .bounceClick { onCoverSelectClick() }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_image),
                                contentDescription = stringResource(R.string.detail_select_cover),
                                modifier = Modifier.size(14.dp),
                                tint = ColorUtils.lightenForText(accentColor, 1.35f)
                            )
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = ColorUtils.lightenForText(accentColor, 1.35f)
                            )
                        }
                    }
                }
            }
        }

        // Fused Container
        val containerRadius = 28.dp
        val containerShape = RoundedCornerShape(containerRadius)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .hazeGlass(
                    state = hazeState,
                    shape = containerShape,
                    containerColor = Color(0xFF1A1A1D).copy(alpha = 0.50f),
                    blurRadius = 32.dp
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Primary Row (TMDB Badge, Rated Badge, Technical Specs)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RatingPill(
                            voteAverage = movie.voteAverage ?: 0.0,
                            expansion = ratingsExpansion,
                            accentColor = accentColor,
                            onClick = {
                                expandedSection = if (expandedSection == DetailHeaderExpandedSection.RATINGS) {
                                    DetailHeaderExpandedSection.NONE
                                } else {
                                    DetailHeaderExpandedSection.RATINGS
                                }
                            }
                        )

                        HeaderRatedPill(
                            certification = ratings.certification,
                            certificationCountry = ratings.certificationCountry,
                            isSelected = expandedSection == DetailHeaderExpandedSection.PARENTS_GUIDE,
                            accentColor = accentColor,
                            onClick = {
                                expandedSection = if (expandedSection == DetailHeaderExpandedSection.PARENTS_GUIDE) {
                                    DetailHeaderExpandedSection.NONE
                                } else {
                                    DetailHeaderExpandedSection.PARENTS_GUIDE
                                }
                            }
                        )

                        HeaderTechnicalPill(
                            isSelected = expandedSection == DetailHeaderExpandedSection.TECHNICAL,
                            accentColor = accentColor,
                            onClick = {
                                expandedSection = if (expandedSection == DetailHeaderExpandedSection.TECHNICAL) {
                                    DetailHeaderExpandedSection.NONE
                                } else {
                                    DetailHeaderExpandedSection.TECHNICAL
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        val dateStr = movie.releaseDate ?: movie.firstAirDate
                        val isReleased = movie.isReleased
                        
                        val isFullDate = !isReleased && !dateStr.isNullOrBlank() && dateStr.length >= 10
                        val displayedDate = if (isFullDate) {
                            val parts = dateStr.split("-")
                            if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else dateStr
                        } else {
                            dateStr?.take(4) ?: "-"
                        }
                        
                        val dateColor = if (!isReleased) accentColor else Color.White.copy(alpha = 0.6f)

                        val runtime = movie.numberOfSeasons?.let { 
                            if (it > 0) "$it ${if (it > 1) stringResource(R.string.detail_seasons) else stringResource(R.string.detail_season)}" else null 
                        } ?: movie.runtime?.let {
                            if (it > 0) {
                                val h = it / 60
                                val m = it % 60
                                if (h > 0) "${h}h ${m}m" else "${m}m"
                            } else null
                        } ?: "-"

                        if (isFullDate) {
                            // When full date is shown (unreleased media): stack vertically to prevent horizontal collision with pills
                            Text(
                                text = displayedDate,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                ),
                                color = dateColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = runtime.uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            // Standard released media: compact single line with year + runtime/seasons
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayedDate,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = dateColor,
                                    maxLines = 1
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(3.dp)
                                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                )

                                Text(
                                    text = runtime.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (seriesStatusLabel != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = seriesStatusLabel.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.55f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Expanded Section Content - NOW BELOW the Pill Row
                if (expandedSection != DetailHeaderExpandedSection.NONE || animatedHeightPx > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    constraints.copy(minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity)
                                )
                                if (placeable.height > 0) {
                                    measuredContentHeightPx = placeable.height
                                    if (activeDisplaySection != DetailHeaderExpandedSection.NONE) {
                                        sectionHeights[activeDisplaySection] = placeable.height
                                    }
                                }
                                layout(placeable.width, animatedHeightPx) {
                                    placeable.place(0, 0)
                                }
                            }
                            .alpha(contentAlpha)
                    ) {
                        Crossfade(
                            targetState = activeDisplaySection,
                            animationSpec = tween(220),
                            label = "HeaderSectionCrossfade"
                        ) { section ->
                            when (section) {
                                DetailHeaderExpandedSection.NONE -> {}
                                DetailHeaderExpandedSection.RATINGS -> {
                                    HeaderRatingsSection(
                                        movie = movie,
                                        ratings = ratings,
                                        accentColor = accentColor
                                    )
                                }
                                DetailHeaderExpandedSection.PARENTS_GUIDE -> {
                                    HeaderParentsGuideSection(
                                        certification = ratings.certification,
                                        certificationCountry = ratings.certificationCountry,
                                        accentColor = accentColor,
                                        onCardClick = onRatingClick
                                    )
                                }
                                DetailHeaderExpandedSection.TECHNICAL -> {
                                    HeaderTechnicalSection(
                                        details = movieDetails,
                                        movie = movie,
                                        accentColor = accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

