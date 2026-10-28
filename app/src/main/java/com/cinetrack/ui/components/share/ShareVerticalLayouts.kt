package com.cinetrack.ui.components.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * Vertical poster floating layout calibrated for Story 9:16, Post 4:5, and Square 1:1.
 */
@Composable
internal fun ShareVerticalPosterLayout(
    state: ShareCardContentState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSquare = state.config.aspectRatio == ShareAspectRatio.SQUARE_1_1
    val isPost = state.config.aspectRatio == ShareAspectRatio.POST_4_5
    val posterFraction = when (state.config.aspectRatio) {
        ShareAspectRatio.STORY_9_16 -> 0.76f
        ShareAspectRatio.POST_4_5 -> 0.52f
        ShareAspectRatio.SQUARE_1_1 -> 0.44f
        ShareAspectRatio.LANDSCAPE_16_9 -> 0.30f
    }

    val showProfile = state.config.showProfile && !state.userDisplayName.isNullOrBlank()
    val floatSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val animatedTopWeight by animateFloatAsState(
        targetValue = if (isSquare) 0.3f else if (showProfile) 1f else 1.15f,
        animationSpec = floatSpring,
        label = "TopSpacerWeight"
    )
    val animatedBottomWeight by animateFloatAsState(
        targetValue = if (isSquare) 0.3f else if (showProfile) 1f else 1.3f,
        animationSpec = floatSpring,
        label = "BottomSpacerWeight"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .animateContentSize()
            .padding(
                start = if (isSquare) 8.dp else 14.dp,
                end = if (isSquare) 8.dp else 14.dp,
                top = if (isSquare) 4.dp else 8.dp,
                bottom = if (isSquare) 4.dp else 10.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP BAR: Centered FlickTrove Branding in frosted glass pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isSquare) 0.dp else 2.dp),
            contentAlignment = Alignment.Center
        ) {
            FlickTroveBrandPill(
                cardHazeState = state.cardHazeState,
                isSquare = isSquare
            )
        }

        Spacer(
            modifier = Modifier
                .heightIn(min = if (isSquare) 4.dp else 14.dp)
                .weight(animatedTopWeight)
        )

        // CENTER: Floating Poster
        Box(
            modifier = Modifier
                .fillMaxWidth(posterFraction)
                .aspectRatio(2f / 3f)
        ) {
            val actorName = state.movie.favoriteActorName
            val profilePath = state.movie.favoriteActorProfilePath ?: state.movie.favoriteActorTmdbPath
            val posterShape = RoundedCornerShape(if (isSquare) 24.dp else 32.dp)
            val showVibesOnPoster = state.config.showVibe && state.vibes.isNotEmpty()
            val posterHazeState = remember(showVibesOnPoster) { HazeState() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 28.dp, shape = posterShape)
                    .clip(posterShape)
                    .then(
                        if (showVibesOnPoster) {
                            Modifier.haze(state = posterHazeState, style = HazeStyles.PremiumDark)
                        } else {
                            Modifier
                        }
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.22f), posterShape)
            ) {
                if (state.posterUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(state.posterUrl)
                            .crossfade(false)
                            .build(),
                        contentDescription = state.movie.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E24)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ciak),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Vibe Icons Floating
            if (showVibesOnPoster) {
                ShareVibesPill(
                    vibes = state.vibes,
                    cardHazeState = posterHazeState,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (isSquare) 7.dp else 10.dp),
                    iconSize = if (isSquare) 11.dp else 14.dp,
                    horizontalPadding = if (isSquare) 5.dp else 7.dp,
                    verticalPadding = if (isSquare) 2.5.dp else 4.dp
                )
            }

            // MVP Badge: floating on the bottom edge
            if (state.config.showMvp && !actorName.isNullOrBlank()) {
                MvpMiniCard(
                    actorName = actorName,
                    characterName = state.movie.favoriteActorCharacter,
                    profilePath = profilePath,
                    accentColor = state.accentColor,
                    posterUrl = state.posterUrl,
                    backdropUrl = state.backdropUrl,
                    hazeState = state.cardHazeState,
                    isFloatingPoster = true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 20.dp, y = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isSquare) 4.dp else if (isPost) 8.dp else 12.dp))

        // Year & Metadata
        if (state.metaItems.isNotEmpty()) {
            Text(
                text = state.metaItems.joinToString(" • "),
                fontSize = if (isSquare) 8.5.sp else 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.80f),
                style = MaterialTheme.typography.bodySmall.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.80f),
                        blurRadius = 8f
                    )
                ),
                modifier = Modifier.padding(bottom = if (isSquare || isPost) 3.dp else 5.dp)
            )
        }

        // HERO RATING
        if (state.hasRating) {
            ShareHeroRating(
                rating = state.effectiveRating,
                accentColor = state.accentColor,
                cardHazeState = state.cardHazeState,
                starSize = if (isSquare) 16.dp else 20.dp,
                ratingFontSize = if (isSquare) 19.sp else 23.sp,
                scaleFontSize = if (isSquare) 11.sp else 13.5.sp,
                horizontalPadding = if (isSquare) 11.dp else 16.dp,
                verticalPadding = if (isSquare) 3.dp else 6.5.dp,
                spacing = if (isSquare) 4.dp else 7.dp
            )
        }

        Spacer(modifier = Modifier.weight(animatedBottomWeight))

        // USER PROFILE
        AnimatedVisibility(
            visible = showProfile,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + expandVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(150)) + shrinkVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        ) {
            ShareSimpleProfileRow(
                userDisplayName = state.userDisplayName.orEmpty(),
                userPhotoUrl = state.userPhotoUrl,
                accentColor = state.accentColor,
                avatarSize = if (isSquare) 12.dp else 14.dp,
                fontSize = if (isSquare) 9.sp else 10.5.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

/**
 * Vertical full-bleed backdrop layout calibrated for Story 9:16, Post 4:5, and Square 1:1.
 */
@Composable
internal fun ShareVerticalBackdropLayout(
    state: ShareCardContentState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSquare = state.config.aspectRatio == ShareAspectRatio.SQUARE_1_1
    val isPost = state.config.aspectRatio == ShareAspectRatio.POST_4_5

    Column(
        modifier = modifier
            .fillMaxSize()
            .animateContentSize()
            .padding(
                start = if (isSquare) 10.dp else 14.dp,
                end = if (isSquare) 10.dp else 14.dp,
                top = if (isSquare) 6.dp else 8.dp,
                bottom = if (isSquare) 8.dp else 12.dp
            ),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP BAR: Centered FlickTrove Branding in frosted glass pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isSquare) 0.dp else 2.dp),
            contentAlignment = Alignment.Center
        ) {
            FlickTroveBrandPill(
                cardHazeState = state.cardHazeState,
                isSquare = isSquare
            )
        }

        // BOTTOM BLOCK: Vibes, Logo/Titolo, Meta, Hero Rating, MVP Pill, Profilo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(bottom = if (isSquare) 2.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Vibes row
            if (state.config.showVibe && state.vibes.isNotEmpty()) {
                ShareVibesPill(
                    vibes = state.vibes,
                    cardHazeState = state.cardHazeState,
                    modifier = Modifier.padding(bottom = if (isSquare) 6.dp else 10.dp),
                    iconSize = 18.dp,
                    horizontalPadding = 10.dp,
                    verticalPadding = 5.dp,
                    spacing = 5.dp
                )
            }

            // Logo o Titolo
            if (state.logoUrl != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(state.logoUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = state.movie.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(if (isSquare) 0.60f else 0.72f)
                        .heightIn(max = if (isSquare) 44.dp else if (isPost) 56.dp else 68.dp)
                        .padding(horizontal = 8.dp),
                    alignment = Alignment.Center
                ) {
                    val painterState = painter.state
                    if (painterState is AsyncImagePainter.State.Error) {
                        Text(
                            text = state.movie.displayName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = if (isSquare) 18.sp else 22.sp,
                                color = Color.White,
                                lineHeight = if (isSquare) 20.sp else 26.sp,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.85f),
                                    blurRadius = 10f
                                )
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
            } else {
                Text(
                    text = state.movie.displayName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = if (isSquare) 18.sp else 22.sp,
                        color = Color.White,
                        lineHeight = if (isSquare) 20.sp else 26.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.85f),
                            blurRadius = 10f
                        )
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Year & Metadata
            if (state.metaItems.isNotEmpty()) {
                Text(
                    text = state.metaItems.joinToString(" • "),
                    fontSize = if (isSquare) 9.5.sp else 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.80f),
                    style = MaterialTheme.typography.bodySmall.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.80f),
                            blurRadius = 8f
                        )
                    ),
                    modifier = Modifier.padding(top = if (isSquare) 2.dp else 4.dp)
                )
            }

            // HERO RATING
            if (state.hasRating) {
                Spacer(modifier = Modifier.height(if (isSquare) 5.dp else if (isPost) 6.dp else 8.dp))
                ShareHeroRating(
                    rating = state.effectiveRating,
                    accentColor = state.accentColor,
                    cardHazeState = state.cardHazeState,
                    starSize = if (isSquare) 18.dp else 23.dp,
                    ratingFontSize = if (isSquare) 21.sp else if (isPost) 24.sp else 26.sp,
                    scaleFontSize = if (isSquare) 12.5.sp else 15.sp,
                    horizontalPadding = if (isSquare) 14.dp else 18.dp,
                    verticalPadding = if (isSquare) 5.dp else 8.dp,
                    spacing = if (isSquare) 6.dp else 8.dp
                )
            }

            // MVP PILL
            val actorName = state.movie.favoriteActorName
            val profilePath = state.movie.favoriteActorProfilePath ?: state.movie.favoriteActorTmdbPath
            if (state.config.showMvp && !actorName.isNullOrBlank()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(if (isSquare) 5.dp else 7.dp))
                    MvpPill(
                        actorName = actorName,
                        characterName = state.movie.favoriteActorCharacter,
                        profilePath = profilePath,
                        accentColor = state.accentColor,
                        hazeState = state.cardHazeState
                    )
                }
            }

            // USER PROFILE
            AnimatedVisibility(
                visible = state.config.showProfile && !state.userDisplayName.isNullOrBlank(),
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = fadeOut(tween(150)) + shrinkVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(if (isSquare) 5.dp else if (isPost) 7.dp else 10.dp))
                    ShareSimpleProfileRow(
                        userDisplayName = state.userDisplayName.orEmpty(),
                        userPhotoUrl = state.userPhotoUrl,
                        accentColor = state.accentColor,
                        avatarSize = if (isSquare) 12.dp else 14.dp,
                        fontSize = if (isSquare) 9.sp else 10.5.sp,
                        modifier = Modifier.padding(bottom = if (isSquare) 2.dp else 4.dp)
                    )
                }
            }
        }
    }
}

