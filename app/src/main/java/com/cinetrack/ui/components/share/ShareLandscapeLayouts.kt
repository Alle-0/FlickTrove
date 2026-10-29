package com.cinetrack.ui.components.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
 * Landscape (16:9) side-by-side layout with floating poster on the left
 * and metadata/ratings on the right.
 */
@Composable
internal fun ShareLandscapePosterLayout(
    state: ShareCardContentState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 20.dp, top = 20.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(36.dp)
    ) {
        // COLONNA SINISTRA: Poster floating con ombra e badge
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(2f / 3f)
        ) {
            val actorName = state.movie.favoriteActorName
            val profilePath = state.movie.favoriteActorProfilePath ?: state.movie.favoriteActorTmdbPath
            val cornerRadiusDp = 26.dp
            val posterShape = RoundedCornerShape(cornerRadiusDp)
            val showVibesOnPoster = state.config.showVibe && state.vibes.isNotEmpty()
            val showMvpOnPoster = state.config.showMvp && !actorName.isNullOrBlank()
            val posterHazeState = remember { HazeState() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 28.dp, shape = posterShape)
                    .clip(posterShape)
                    .haze(state = posterHazeState, style = HazeStyles.PremiumDark)
                    .border(1.dp, Color.White.copy(alpha = 0.22f), posterShape)
            ) {
                if (state.posterUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(state.posterUrl).crossfade(false).build(),
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
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Vibe icons floating sul poster
            androidx.compose.animation.AnimatedVisibility(
                visible = showVibesOnPoster,
                enter = fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.75f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = fadeOut(animationSpec = tween(160)) + scaleOut(targetScale = 0.75f, animationSpec = tween(160)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                ShareVibesPill(
                    vibes = state.vibes,
                    cardHazeState = posterHazeState,
                    iconSize = 14.dp,
                    horizontalPadding = 7.dp,
                    verticalPadding = 4.dp
                )
            }

            // MVP badge in basso a destra sul poster con real frosted glass blur
            val cardOffsetX = 20.dp
            val cardOffsetY = 14.dp
            androidx.compose.animation.AnimatedVisibility(
                visible = showMvpOnPoster,
                enter = fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.75f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)) + slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 4 },
                exit = fadeOut(animationSpec = tween(160)) + scaleOut(targetScale = 0.75f, animationSpec = tween(160)) + slideOutVertically(animationSpec = tween(160)) { it / 4 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = cardOffsetX, y = cardOffsetY)
            ) {
                MvpMiniCard(
                    actorName = actorName.orEmpty(),
                    characterName = state.movie.favoriteActorCharacter,
                    profilePath = profilePath,
                    accentColor = state.accentColor,
                    hazeState = posterHazeState
                )
            }
        }

        // COLONNA DESTRA: Branding, Titolo, Metadati, Voto Hero e Profilo
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 6.dp)
                .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Top Branding
            FlickTroveBrandPill(
                cardHazeState = state.cardHazeState,
                isSquare = false
            )

            // Middle: Titolo/Logo + Metadati
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if (state.logoUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context).data(state.logoUrl).crossfade(false).build(),
                        contentDescription = state.movie.displayName,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .heightIn(max = 48.dp)
                            .widthIn(max = 240.dp)
                    ) {
                        val imageState = painter.state
                        if (imageState is AsyncImagePainter.State.Loading || imageState is AsyncImagePainter.State.Error) {
                            Text(
                                text = state.movie.displayName,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 23.sp
                            )
                        } else {
                            SubcomposeAsyncImageContent()
                        }
                    }
                } else {
                    Text(
                        text = state.movie.displayName,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 23.sp
                    )
                }
                if (state.metaItems.isNotEmpty()) {
                    Text(
                        text = state.metaItems.joinToString(" • "),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            // Hero Rating /10
            AnimatedVisibility(
                visible = state.hasRating,
                enter = fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.85f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = tween(160)) + scaleOut(targetScale = 0.85f, animationSpec = tween(160)) + shrinkVertically(animationSpec = tween(160))
            ) {
                ShareHeroRating(
                    rating = state.effectiveRating,
                    accentColor = state.accentColor,
                    cardHazeState = state.cardHazeState,
                    starSize = 21.dp,
                    ratingFontSize = 24.sp,
                    scaleFontSize = 14.sp,
                    horizontalPadding = 16.dp,
                    verticalPadding = 7.dp,
                    spacing = 7.dp
                )
            }

            // User profile in basso a destra
            AnimatedVisibility(
                visible = state.config.showProfile && !state.userDisplayName.isNullOrBlank(),
                enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 3 },
                exit = fadeOut(animationSpec = tween(160)) + shrinkVertically(animationSpec = tween(160)) + slideOutVertically(animationSpec = tween(160)) { it / 3 }
            ) {
                ShareSimpleProfileRow(
                    userDisplayName = state.userDisplayName.orEmpty(),
                    userPhotoUrl = state.userPhotoUrl,
                    accentColor = state.accentColor,
                    avatarSize = 15.dp,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

/**
 * Landscape (16:9) full-bleed backdrop layout with top bar and bottom details.
 */
@Composable
internal fun ShareLandscapeBackdropLayout(
    state: ShareCardContentState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Row: Brand pill a sinistra, Vibes a destra
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlickTroveBrandPill(
                cardHazeState = state.cardHazeState,
                isSquare = false
            )

            AnimatedVisibility(
                visible = state.config.showVibe && state.vibes.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.75f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = fadeOut(animationSpec = tween(160)) + scaleOut(targetScale = 0.75f, animationSpec = tween(160))
            ) {
                ShareVibesPill(
                    vibes = state.vibes,
                    cardHazeState = state.cardHazeState,
                    iconSize = 16.dp,
                    horizontalPadding = 9.dp,
                    verticalPadding = 4.dp,
                    spacing = 4.dp
                )
            }
        }

        // Bottom Row: [Titolo + Meta + Voto] a sinistra | [MVP + Profilo] a destra
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Blocco Sinistra
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if (state.logoUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context).data(state.logoUrl).crossfade(false).build(),
                        contentDescription = state.movie.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .heightIn(max = 50.dp)
                            .widthIn(max = 240.dp)
                    ) {
                        val imageState = painter.state
                        if (imageState is AsyncImagePainter.State.Loading || imageState is AsyncImagePainter.State.Error) {
                            Text(
                                text = state.movie.displayName,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 25.sp
                            )
                        } else {
                            SubcomposeAsyncImageContent()
                        }
                    }
                } else {
                    Text(
                        text = state.movie.displayName,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 25.sp
                    )
                }

                if (state.metaItems.isNotEmpty()) {
                    Text(
                        text = state.metaItems.joinToString(" • "),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                }

                AnimatedVisibility(
                    visible = state.hasRating,
                    enter = fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.85f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(animationSpec = tween(160)) + scaleOut(targetScale = 0.85f, animationSpec = tween(160)) + shrinkVertically(animationSpec = tween(160))
                ) {
                    ShareHeroRating(
                        rating = state.effectiveRating,
                        accentColor = state.accentColor,
                        cardHazeState = state.cardHazeState,
                        starSize = 20.dp,
                        ratingFontSize = 24.sp,
                        scaleFontSize = 14.sp,
                        horizontalPadding = 16.dp,
                        verticalPadding = 6.dp,
                        spacing = 7.dp
                    )
                }
            }

            // Blocco Destra (MVP + Profilo)
            Column(
                modifier = Modifier.animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val actorName = state.movie.favoriteActorName
                val profilePath = state.movie.favoriteActorProfilePath ?: state.movie.favoriteActorTmdbPath
                AnimatedVisibility(
                    visible = state.config.showMvp && !actorName.isNullOrBlank(),
                    enter = fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.75f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(animationSpec = tween(160)) + scaleOut(targetScale = 0.75f, animationSpec = tween(160)) + shrinkVertically(animationSpec = tween(160))
                ) {
                    MvpMiniCard(
                        actorName = actorName.orEmpty(),
                        characterName = state.movie.favoriteActorCharacter,
                        profilePath = profilePath,
                        accentColor = state.accentColor,
                        hazeState = state.cardHazeState
                    )
                }

                AnimatedVisibility(
                    visible = state.config.showProfile && !state.userDisplayName.isNullOrBlank(),
                    enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 3 },
                    exit = fadeOut(animationSpec = tween(160)) + shrinkVertically(animationSpec = tween(160)) + slideOutVertically(animationSpec = tween(160)) { it / 3 }
                ) {
                    SharePillProfileRow(
                        userDisplayName = state.userDisplayName.orEmpty(),
                        userPhotoUrl = state.userPhotoUrl,
                        accentColor = state.accentColor,
                        cardHazeState = state.cardHazeState,
                        avatarSize = 16.dp,
                        fontSize = 10.5.sp,
                        endPadding = 9.dp
                    )
                }
            }
        }
    }
}

