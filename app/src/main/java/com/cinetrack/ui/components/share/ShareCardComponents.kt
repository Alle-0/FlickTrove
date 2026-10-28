package com.cinetrack.ui.components.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.ui.components.detail.EmotionalVibe
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import java.util.Locale

/**
 * Shared branding pill displaying the FlickTrove icon and brand name with glassmorphism.
 */
@Composable
internal fun FlickTroveBrandPill(
    cardHazeState: HazeState,
    modifier: Modifier = Modifier,
    isSquare: Boolean = false,
    iconSize: Dp = if (isSquare) 13.dp else 14.dp,
    fontSize: TextUnit = if (isSquare) 8.5.sp else 9.sp,
    startPadding: Dp = if (isSquare) 4.5.dp else 5.dp,
    endPadding: Dp = if (isSquare) 6.dp else 7.dp,
    verticalPadding: Dp = if (isSquare) 1.5.dp else 2.dp
) {
    Row(
        modifier = modifier
            .hazeGlass(
                state = cardHazeState,
                shape = RoundedCornerShape(50),
                blurRadius = 18.dp,
                containerColor = Color.Black.copy(alpha = 0.40f),
                borderWidth = 0.6.dp,
                borderColor = Color.White.copy(alpha = 0.22f)
            )
            .padding(
                start = startPadding,
                end = endPadding,
                top = verticalPadding,
                bottom = verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.5.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground_vector),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(iconSize)
        )
        Text(
            text = "FlickTrove",
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.90f),
            letterSpacing = 0.3.sp
        )
    }
}

/**
 * High-emphasis star rating badge with /10 scale indicator.
 */
@Composable
internal fun ShareHeroRating(
    rating: Double,
    accentColor: Color,
    cardHazeState: HazeState,
    modifier: Modifier = Modifier,
    starSize: Dp = 21.dp,
    ratingFontSize: TextUnit = 24.sp,
    scaleFontSize: TextUnit = 14.sp,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 7.dp,
    spacing: Dp = 7.dp
) {
    val effectiveRating = rating.coerceIn(0.0, 10.0)
    Row(
        modifier = modifier
            .hazeGlass(
                state = cardHazeState,
                shape = RoundedCornerShape(50),
                blurRadius = 18.dp,
                containerColor = accentColor.copy(alpha = 0.22f),
                borderWidth = 1.dp,
                borderColor = accentColor.copy(alpha = 0.55f)
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
            contentDescription = null,
            tint = ColorUtils.lightenForText(accentColor, 1.35f),
            modifier = Modifier.size(starSize)
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = String.format(Locale.US, "%.1f", effectiveRating),
                fontSize = ratingFontSize,
                fontWeight = FontWeight.Black,
                color = ColorUtils.lightenForText(accentColor, 1.35f),
                lineHeight = ratingFontSize
            )
            Text(
                text = "/10",
                fontSize = scaleFontSize,
                fontWeight = FontWeight.Bold,
                color = ColorUtils.lightenForText(accentColor, 1.35f).copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

/**
 * Frosted glass pill containing vibe icons.
 */
@Composable
internal fun ShareVibesPill(
    vibes: List<EmotionalVibe>,
    cardHazeState: HazeState,
    modifier: Modifier = Modifier,
    iconSize: Dp = 14.dp,
    horizontalPadding: Dp = 7.dp,
    verticalPadding: Dp = 4.dp,
    spacing: Dp = 3.dp
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(50))
            .border(0.6.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        vibes.forEach { vibe ->
            Icon(
                painter = painterResource(id = vibe.iconRes),
                contentDescription = stringResource(id = vibe.labelRes),
                tint = Color(vibe.colorHex),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Minimalist user profile signature (avatar + @username) for poster floating layouts.
 */
@Composable
internal fun ShareSimpleProfileRow(
    userDisplayName: String,
    userPhotoUrl: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 14.dp,
    fontSize: TextUnit = 10.5.sp
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
    ) {
        if (!userPhotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(userPhotoUrl).crossfade(false).build(),
                contentDescription = null,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userDisplayName.take(1).uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Text(
            text = "@$userDisplayName",
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}

/**
 * Frosted glass pill profile badge for full backdrop layouts.
 */
@Composable
internal fun SharePillProfileRow(
    userDisplayName: String,
    userPhotoUrl: String?,
    accentColor: Color,
    cardHazeState: HazeState,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 16.dp,
    fontSize: TextUnit = 10.5.sp,
    endPadding: Dp = 9.dp
) {
    val context = LocalContext.current
    val profMargin = 3.dp
    val profPillHeight = avatarSize + (profMargin * 2)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .height(profPillHeight)
            .hazeGlass(
                state = cardHazeState,
                shape = RoundedCornerShape(50),
                blurRadius = 18.dp,
                containerColor = Color.Black.copy(alpha = 0.40f),
                borderWidth = 0.6.dp,
                borderColor = Color.White.copy(alpha = 0.22f)
            )
            .padding(start = profMargin, end = endPadding)
    ) {
        if (!userPhotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(userPhotoUrl).crossfade(false).build(),
                contentDescription = null,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userDisplayName.take(1).uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Text(
            text = "@$userDisplayName",
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.80f)
        )
    }
}

/**
 * Frosted horizontal pill badge showing the MVP actor/character.
 */
@Composable
internal fun MvpPill(
    actorName: String,
    characterName: String?,
    profilePath: String?,
    accentColor: Color,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profileUrl = remember(profilePath) {
        buildTmdbImageUrl(profilePath, ImageType.PROFILE, com.cinetrack.util.ImageQuality.HIGH)
    }
    val displayName = if (!characterName.isNullOrBlank()) characterName else actorName

    val avatarSize = 19.dp
    val margin = 3.5.dp
    val pillHeight = avatarSize + (margin * 2)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .height(pillHeight)
            .hazeGlass(
                state = hazeState,
                shape = RoundedCornerShape(50),
                blurRadius = 16.dp,
                containerColor = Color.Black.copy(alpha = 0.40f),
                borderWidth = 0.6.dp,
                borderColor = Color.White.copy(alpha = 0.22f)
            )
            .padding(start = margin, end = 10.dp)
    ) {
        if (profileUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(profileUrl)
                    .crossfade(false)
                    .build(),
                contentDescription = actorName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .border(0.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_persona),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Text(
            text = displayName,
            fontSize = 10.5.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Text(
            text = "•",
            fontSize = 9.sp,
            lineHeight = 9.sp,
            color = Color.White.copy(alpha = 0.4f)
        )

        Text(
            text = "MVP",
            fontSize = 9.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Black,
            color = ColorUtils.lightenForText(accentColor, 1.35f),
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Vertical mini card badge for MVP floating over poster edges.
 * Uses a blurred artwork underlay to seamlessly blur the transition
 * between the poster edge, border, and the background with zero hard seams.
 */
@Composable
internal fun MvpMiniCard(
    actorName: String,
    characterName: String?,
    profilePath: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    posterUrl: String? = null,
    backdropUrl: String? = null,
    hazeState: HazeState? = null,
    isFloatingPoster: Boolean = false
) {
    val context = LocalContext.current
    val profileUrl = remember(profilePath) {
        buildTmdbImageUrl(profilePath, ImageType.PROFILE, com.cinetrack.util.ImageQuality.HIGH)
    }
    val cardShape = RoundedCornerShape(14.dp)
    val bgArtworkUrl = posterUrl ?: backdropUrl

    val baseModifier = if (isFloatingPoster) {
        modifier
            .width(68.dp)
            .shadow(elevation = 16.dp, shape = cardShape)
            .clip(cardShape)
            .border(0.8.dp, Color.White.copy(alpha = 0.28f), cardShape)
    } else {
        modifier
            .width(68.dp)
            .shadow(elevation = 16.dp, shape = cardShape)
            .hazeGlass(
                state = hazeState,
                shape = cardShape,
                blurRadius = 18.dp,
                containerColor = Color(0xFF141418).copy(alpha = 0.44f),
                borderWidth = 0.8.dp,
                borderColor = Color.White.copy(alpha = 0.28f)
            )
    }

    Box(modifier = baseModifier) {
        if (isFloatingPoster) {
            // Base dark glass background (ensures the right side and bottom hanging off the poster remain dark)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFF141418).copy(alpha = if (bgArtworkUrl != null) 0.76f else 0.88f))
            )

            // Continuous blurred artwork underlay feathered towards the right and bottom edges
            if (bgArtworkUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(bgArtworkUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .matchParentSize()
                        .blur(18.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            // Feather towards right edge where the card hangs outside the poster (~70% width)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0.0f to Color.Black,
                                    0.40f to Color.Black,
                                    0.68f to Color.Black.copy(alpha = 0.45f),
                                    0.84f to Color.Transparent,
                                    1.0f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                            // Feather towards bottom edge where the card hangs outside the poster (~85% height)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Black,
                                    0.60f to Color.Black,
                                    0.82f to Color.Black.copy(alpha = 0.45f),
                                    0.96f to Color.Transparent,
                                    1.0f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                )
            }

            // Frosted dark glass overlay tint
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFF141418).copy(alpha = 0.35f))
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 4.dp)
        ) {
            Text(
                text = "MVP",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = ColorUtils.lightenForText(accentColor, 1.35f),
                letterSpacing = 0.8.sp,
                lineHeight = 8.sp,
                style = androidx.compose.ui.text.TextStyle(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                modifier = Modifier.padding(top = 1.dp, bottom = 1.5.dp)
            )

            if (profileUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(profileUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = actorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_persona),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.5.dp))

            val displayName = if (!characterName.isNullOrBlank()) characterName else actorName
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                lineHeight = 9.5.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

