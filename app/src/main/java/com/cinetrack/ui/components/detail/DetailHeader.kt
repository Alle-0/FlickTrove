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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.composed
import com.cinetrack.data.Movie
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

/**
 * DetailHeader
 * Fused Container with TMDB rating that expands to show extra ratings.
 * Features 120fps morphing animations via manual float interpolation.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailHeader(
    movie: Movie,
    ratings: ExternalRatings,
    accentColor: Color,
    matchPercentage: Int? = null,
    logoPath: String? = null,
    hazeState: HazeState? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onRatingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Manual float interpolation for "surgical" control over physics (0.0 to 1.0)
    val expansion by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        ),
        label = "HeaderExpansion"
    )

    val targetExtraHeight = 160.dp // Even more compact
    val targetHeight = if (isExpanded) targetExtraHeight else 0.dp
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(600, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
        label = "ExtraHeight"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                
                // If we are in the lookahead pass, we use the stable target height
                // to avoid registering state reads on animating values during lookahead.
                val heightToSubtract = if (isLookingAhead) {
                    targetHeight.roundToPx()
                } else {
                    animatedHeight.roundToPx()
                }
                
                val stableHeight = (placeable.height - heightToSubtract).coerceAtLeast(0)
                
                layout(placeable.width, stableHeight) {
                    placeable.place(0, -heightToSubtract)
                }
            }
    ) {
        // Title & Tagline
        Column(modifier = Modifier.fillMaxWidth()) {
            if (logoPath != null) {
                coil.compose.AsyncImage(
                    model = "https://image.tmdb.org/t/p/w500$logoPath",
                    contentDescription = movie.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.6f) // Takes up to 60% of width
                        .heightIn(max = 100.dp) // Max height to avoid huge logos
                        .padding(bottom = 8.dp),
                    alignment = Alignment.CenterStart
                )
            } else {
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

            if (matchPercentage != null) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .hazeGlass(
                            state = hazeState,
                            shape = androidx.compose.foundation.shape.CircleShape,
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
                        color = accentColor
                    )
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
                // Primary Row (TMDB Badge) - NOW ON TOP of the extra content
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RatingPill(
                        voteAverage = movie.voteAverage ?: 0.0,
                        expansion = expansion,
                        accentColor = accentColor,
                        modifier = Modifier.bounceClick(scaleDown = 0.92f) { isExpanded = !isExpanded }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = movie.releaseDate?.take(4) ?: movie.firstAirDate?.take(4) ?: "-",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .size(4.dp)
                                .background(Color.White.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                        )

                        val runtime = movie.numberOfSeasons?.let { 
                            if (it > 0) "$it ${if (it > 1) stringResource(R.string.detail_seasons) else stringResource(R.string.detail_season)}" else null 
                        } ?: movie.runtime?.let {
                            if (it > 0) {
                                val h = it / 60
                                val m = it % 60
                                if (h > 0) "${h}h ${m}m" else "${m}m"
                            } else null
                        } ?: "-"

                        Text(
                            text = runtime.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Extra Ratings section - NOW BELOW the Pill Row
                if (isExpanded || animatedHeight > 0.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(animatedHeight)
                            .alpha(expansion)
                            .padding(horizontal = 16.dp)
                    ) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val uriHandler = LocalUriHandler.current
                        val imdbId = movie.imdbId ?: ratings.imdb

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModernRatingItem(
                                badge = { Badge("IMDb", Color(0xFFF5C518), Color.Black) },
                                value = ratings.imdb ?: "-",
                                subValue = ratings.imdbVotes,
                                onClick = {
                                    imdbId?.let { uriHandler.openUri("https://www.imdb.com/title/$it") }
                                }
                            )
                            VerticalDivider()
                            
                            val rtScore = ratings.rottenTomatoes?.removeSuffix("%")?.toIntOrNull() ?: 0
                            val rtIcon = if (rtScore >= 60 || ratings.rottenTomatoes == null) "🍅" else "🤢"
                            ModernRatingItem(
                                badge = { Text(rtIcon, fontSize = 14.sp) },
                                value = ratings.rottenTomatoes ?: "-",
                                subValue = null,
                                onClick = {
                                    val title = movie.title ?: movie.name
                                    title?.let { uriHandler.openUri("https://www.rottentomatoes.com/search?search=${it}") }
                                }
                            )
                            VerticalDivider()
                            
                            val mcScore = ratings.metacritic?.split("/")?.firstOrNull()?.toIntOrNull() ?: 0
                            val mcColor = when {
                                mcScore >= 61 -> Color(0xFF66CC33)
                                mcScore >= 40 -> Color(0xFFFFCC33)
                                else -> Color(0xFFFF0000)
                            }
                            ModernRatingItem(
                                badge = { Badge("MC", mcColor, Color.Black) },
                                value = ratings.metacritic?.replace("/100", "") ?: "-",
                                subValue = "Score",
                                onClick = {
                                    val title = movie.title ?: movie.name
                                    title?.let { uriHandler.openUri("https://www.metacritic.com/search/${it}/") }
                                }
                            )
                            VerticalDivider()
                            ModernRatingItem(
                                badge = {
                                    Image(
                                        painter = painterResource(id = com.cinetrack.R.drawable.ic_trakt_logo),
                                        contentDescription = "Trakt.tv Logo",
                                        modifier = Modifier.size(20.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                },
                                value = ratings.trakt?.let { "${(it * 10).toInt()}%" } ?: "-",
                                subValue = ratings.traktVotes,
                                onClick = {
                                    if (imdbId != null) {
                                        uriHandler.openUri("https://trakt.tv/search?query=$imdbId")
                                    }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        ratings.awards?.let { awardText ->
                            if (awardText != "N/A" && awardText.isNotEmpty()) {
                                AwardBox(
                                    awards = awardText,
                                    accentColor = accentColor,
                                    onClick = {
                                        imdbId?.let { uriHandler.openUri("https://www.imdb.com/title/$it/awards") }
                                    }
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            movie.revenue?.let { rev ->
                                if (rev > 0) {
                                    val formattedRevenue = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US).apply {
                                        maximumFractionDigits = 0
                                    }.format(rev)
                                    MetaBadge(label = "BOX OFFICE", value = formattedRevenue)
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                            }
                            ratings.certification?.let { cert ->
                                val certColor = when (cert.uppercase()) {
                                    "G", "TV-G", "U" -> Color(0xFF4CAF50)
                                    "PG", "TV-PG", "12" -> Color(0xFF8BC34A)
                                    "PG-13", "TV-14", "15" -> Color(0xFFFF9800)
                                    "R", "TV-MA", "18", "NC-17" -> Color(0xFFF44336)
                                    else -> Color.White
                                }
                                MetaBadge(
                                    label = "RATED",
                                    value = cert,
                                    valueColor = certColor,
                                    showValueBox = true,
                                    onClick = onRatingClick
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun RatingPill(
    voteAverage: Double,
    expansion: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val vPadding = 6.dp
    val topPadding = vPadding
    val bottomPadding = vPadding
    
    val pillRadius = 48.dp
    
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 80.dp, minHeight = 36.dp)
            .background(accentColor, RoundedCornerShape(pillRadius))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main Rating
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                // Precise centering logic
                translationY = (1f - expansion) * -2.dp.toPx()
                compositingStrategy = CompositingStrategy.Offscreen
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = com.cinetrack.R.drawable.ic_tmdb_logo),
                    contentDescription = "TMDB Logo",
                    modifier = Modifier.height(11.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black.copy(alpha = 0.9f))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.9f),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = String.format("%.1f", voteAverage),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black.copy(alpha = 0.9f)
                )
            }
        }

        // Help Text - Moved further down
        Text(
            text = stringResource(R.string.detail_tap_for_more),
            fontSize = 7.sp,
            lineHeight = 7.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black.copy(alpha = 0.4f),
            modifier = Modifier
                .graphicsLayer {
                    // Slightly faster fade to avoid ghosting during expansion
                    alpha = (1f - expansion * 2.5f).coerceIn(0f, 1f)
                    // Precise offset
                    val startOffset = 11.dp.toPx()
                    translationY = startOffset
                }
        )
    }
}

@Composable
fun ExtraRatingItem(label: String, value: String, subLabel: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.3f),
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(vertical = 1.dp)
        )
        subLabel?.let {
            Text(
                text = it.uppercase(),
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
fun AwardBox(awards: String, accentColor: Color, onClick: () -> Unit = {}) {
    val gold = Color(0xFFFFD700)
    var textWidth by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.97f) { onClick() }
            .background(gold.copy(alpha = 0.05f), CircleShape)
            .border(0.5.dp, gold.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { containerWidth = it.width }
                .then(
                    if (textWidth > containerWidth && containerWidth > 0) {
                        Modifier
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                val fadeWidthPx = 16.dp.toPx()
                                val fadeFraction = if (size.width > 0f) (fadeWidthPx / size.width).coerceAtMost(0.49f) else 0f
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.Transparent,
                                        fadeFraction to Color.Black,
                                        1f - fadeFraction to Color.Black,
                                        1f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = awards,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = gold.copy(alpha = 0.8f),
                maxLines = 1,
                softWrap = false,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1200
                    )
                    .then(
                        if (textWidth > containerWidth && containerWidth > 0) {
                            Modifier.padding(horizontal = 16.dp)
                        } else Modifier
                    )
                    .onSizeChanged { textWidth = it.width }
            )
        }
    }
}

@Composable
fun ModernRatingItem(
    badge: @Composable () -> Unit,
    value: String,
    subValue: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.bounceClick(scaleDown = 0.9f) { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        badge()
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            if (subValue != null && subValue != "N/A" && subValue != "Score") {
                Text(
                    text = subValue.uppercase(),
                    fontSize = 7.sp,
                    lineHeight = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun Badge(text: String, bgColor: Color, textColor: Color, isSquare: Boolean = false) {
    Surface(
        color = bgColor,
        shape = if (isSquare) RoundedCornerShape(4.dp) else CircleShape,
        modifier = (if (isSquare) Modifier.size(20.dp) else Modifier.width(34.dp).height(14.dp))
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = if (isSquare) 12.sp else 8.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@Composable
fun VerticalDivider() {
    Box(modifier = Modifier.width(0.5.dp).height(24.dp).background(Color.White.copy(alpha = 0.12f)))
}

@Composable
fun MetaBadge(
    label: String,
    value: String,
    valueColor: Color = Color.White,
    showValueBox: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .let { if (onClick != null) it.bounceClick(scaleDown = 0.95f) { onClick() } else it }
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(end = 8.dp)
        )
        if (showValueBox) {
            Box(
                modifier = Modifier
                    .background(valueColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .border(0.5.dp, valueColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = valueColor
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = valueColor
            )
        }
    }
}
