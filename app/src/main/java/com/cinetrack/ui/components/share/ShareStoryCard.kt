package com.cinetrack.ui.components.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.detail.findVibe
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import java.util.Locale

enum class ShareCoverStyle {
    POSTER_FLOATING,
    BACKDROP_FULL
}

enum class ShareAspectRatio(
    val ratioName: String,
    val subtitle: String,
    val socialHint: String,
    val widthRatio: Float,
    val heightRatio: Float,
    val targetWidthPx: Int,
    val targetHeightPx: Int,
    val baseWidthDp: androidx.compose.ui.unit.Dp,
    val baseHeightDp: androidx.compose.ui.unit.Dp,
    val previewWidthDp: androidx.compose.ui.unit.Dp
) {
    STORY_9_16(
        ratioName = "9:16",
        subtitle = "Storia",
        socialHint = "Instagram • TikTok",
        widthRatio = 9f,
        heightRatio = 16f,
        targetWidthPx = 1080,
        targetHeightPx = 1920,
        baseWidthDp = 360.dp,
        baseHeightDp = 640.dp,
        previewWidthDp = 135.dp
    ),
    POST_4_5(
        ratioName = "4:5",
        subtitle = "Post",
        socialHint = "Instagram Feed",
        widthRatio = 4f,
        heightRatio = 5f,
        targetWidthPx = 1080,
        targetHeightPx = 1350,
        baseWidthDp = 360.dp,
        baseHeightDp = 450.dp,
        previewWidthDp = 175.dp
    ),
    SQUARE_1_1(
        ratioName = "1:1",
        subtitle = "Quadrato",
        socialHint = "Post universale",
        widthRatio = 1f,
        heightRatio = 1f,
        targetWidthPx = 1080,
        targetHeightPx = 1080,
        baseWidthDp = 360.dp,
        baseHeightDp = 360.dp,
        previewWidthDp = 180.dp
    ),
    LANDSCAPE_16_9(
        ratioName = "16:9",
        subtitle = "Wide",
        socialHint = "X • Reddit",
        widthRatio = 16f,
        heightRatio = 9f,
        targetWidthPx = 1920,
        targetHeightPx = 1080,
        baseWidthDp = 540.dp,
        baseHeightDp = 303.75.dp,
        previewWidthDp = 240.dp
    );

    val ratio: Float get() = widthRatio / heightRatio
}

data class ShareStoryCardConfig(
    val coverStyle: ShareCoverStyle = ShareCoverStyle.POSTER_FLOATING,
    val aspectRatio: ShareAspectRatio = ShareAspectRatio.STORY_9_16,
    val showRating: Boolean = true,
    val showProfile: Boolean = true,
    val showVibe: Boolean = true,
    val showMvp: Boolean = true,
    val showWatermark: Boolean = true,
    val customPosterPath: String? = null,
    val customBackdropPath: String? = null
)

/**
 * 9:16 Story Card for sharing ratings, reviews and cinephile recommendations.
 * Designed with a premium cinematic dark aesthetic, soft glassmorphism,
 * deep dynamic gradients and bold typography.
 */
@Composable
fun ShareStoryCard(
    movie: Movie,
    config: ShareStoryCardConfig,
    accentColor: Color,
    userDisplayName: String?,
    userPhotoUrl: String?,
    logoPath: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageQuality = LocalImageQuality.current

    val posterUrl = remember(config.customPosterPath, movie.posterPath) {
        val path = config.customPosterPath ?: movie.posterPath
        if (!path.isNullOrBlank()) buildTmdbImageUrl(path, ImageType.POSTER, com.cinetrack.util.ImageQuality.HIGH) else null
    }

    val backdropUrl = remember(config.customBackdropPath, movie.customBackdropPath, movie.backdropPath, movie.posterPath) {
        val path = config.customBackdropPath ?: movie.customBackdropPath ?: movie.backdropPath ?: movie.posterPath
        if (!path.isNullOrBlank()) buildTmdbImageUrl(path, ImageType.BACKDROP, com.cinetrack.util.ImageQuality.HIGH) else null
    }

    val logoUrl = remember(logoPath) {
        if (!logoPath.isNullOrBlank()) {
            buildTmdbImageUrl(logoPath, ImageType.LOGO, com.cinetrack.util.ImageQuality.HIGH)
        } else null
    }

    val effectiveRating = movie.personalRating ?: 0.0
    val hasRating = effectiveRating > 0.0

    val posterHazeState = remember { HazeState() }
    val backdropHazeState = remember { HazeState() }

    // Extract all emotional vibes and resolve their visual models/icons
    val vibes = remember(movie.emotionalVibes) {
        val raw = movie.emotionalVibes?.trim()
        if (!raw.isNullOrBlank() && raw != "[]") {
            raw.removePrefix("[").removeSuffix("]")
                .split(",")
                .map { it.replace("\"", "").trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { findVibe(it) }
        } else emptyList()
    }

    // Outer Card Container (responsive to config.aspectRatio)
    Box(
        modifier = modifier
            .aspectRatio(config.aspectRatio.ratio)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF09090B))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
    ) {
        // --- 1. BACKGROUND LAYERS ---
        when (config.coverStyle) {
            ShareCoverStyle.BACKDROP_FULL -> {
                // Full-bleed backdrop with dark gradient and Haze capture
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = backdropHazeState, style = HazeStyles.PremiumDark)
                ) {
                    if (backdropUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(backdropUrl)
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.45f),
                                    0.4f to Color.Black.copy(alpha = 0.70f),
                                    0.75f to Color(0xFF09090B).copy(alpha = 0.95f),
                                    1.0f to Color(0xFF09090B)
                                )
                            )
                    )
                }
            }
            ShareCoverStyle.POSTER_FLOATING -> {
                // Atmospheric blurred backdrop in background with Haze capture
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = backdropHazeState, style = HazeStyles.PremiumDark)
                ) {
                    val bgUrl = backdropUrl ?: posterUrl
                    if (bgUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(bgUrl)
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(32.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.65f),
                                    0.5f to Color(0xFF09090B).copy(alpha = 0.88f),
                                    1.0f to Color(0xFF09090B)
                                )
                            )
                    )
                }
            }
        }

        // --- 2. FOREGROUND CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR: Centered FlickTrove Branding in frosted glass pill with enlarged logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .hazeGlass(
                            state = backdropHazeState,
                            shape = RoundedCornerShape(50),
                            blurRadius = 18.dp,
                            containerColor = Color.Black.copy(alpha = 0.40f),
                            borderWidth = 0.6.dp,
                            borderColor = Color.White.copy(alpha = 0.22f)
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.5.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground_vector),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(25.dp)
                    )
                    Text(
                        text = "FlickTrove",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.90f),
                        letterSpacing = 0.3.sp
                    )
                }
            }

            // CENTER: Floating Poster (if POSTER_FLOATING style)
            if (config.coverStyle == ShareCoverStyle.POSTER_FLOATING) {
                val posterFraction = when (config.aspectRatio) {
                    ShareAspectRatio.STORY_9_16 -> 0.72f
                    ShareAspectRatio.POST_4_5 -> 0.58f
                    ShareAspectRatio.SQUARE_1_1 -> 0.44f
                    ShareAspectRatio.LANDSCAPE_16_9 -> 0.30f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(posterFraction)
                        .aspectRatio(2f / 3f)
                        .shadow(elevation = 32.dp, shape = RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(28.dp))
                ) {
                    // Solo l'immagine del poster viene catturata da Haze per il blur sottostante
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .haze(state = posterHazeState, style = HazeStyles.PremiumDark)
                    ) {
                        if (posterUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(posterUrl)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = movie.displayName,
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
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    // Vibe icons floating as a badge on the poster (top-right) with authentic frosted glass blur
                    if (config.showVibe && vibes.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .hazeGlass(
                                    state = posterHazeState,
                                    shape = RoundedCornerShape(50),
                                    blurRadius = 18.dp,
                                    containerColor = Color.Black.copy(alpha = 0.35f),
                                    borderWidth = 0.6.dp,
                                    borderColor = Color.White.copy(alpha = 0.25f)
                                )
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            vibes.forEach { vibe ->
                                Icon(
                                    painter = painterResource(id = vibe.iconRes),
                                    contentDescription = stringResource(id = vibe.labelRes),
                                    tint = Color(vibe.colorHex),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // MVP Badge overlapping bottom-right of poster
                    val actorName = movie.favoriteActorName
                    val profilePath = movie.favoriteActorProfilePath ?: movie.favoriteActorTmdbPath
                    if (config.showMvp && !actorName.isNullOrBlank()) {
                        MvpMiniCard(
                            actorName = actorName,
                            characterName = movie.favoriteActorCharacter,
                            profilePath = profilePath,
                            accentColor = accentColor,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 10.dp, y = 12.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // BOTTOM BLOCK: Title (only on backdrop), Meta, Hero Rating & Profile
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Vibes row for full backdrop mode with authentic blur
                if (config.coverStyle == ShareCoverStyle.BACKDROP_FULL && config.showVibe && vibes.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .hazeGlass(
                                state = backdropHazeState,
                                shape = RoundedCornerShape(50),
                                blurRadius = 18.dp,
                                containerColor = Color.Black.copy(alpha = 0.35f),
                                borderWidth = 0.6.dp,
                                borderColor = Color.White.copy(alpha = 0.22f)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        vibes.forEach { vibe ->
                            Icon(
                                painter = painterResource(id = vibe.iconRes),
                                contentDescription = stringResource(id = vibe.labelRes),
                                tint = Color(vibe.colorHex),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Title / Logo: renderizzato SOLO nella modalità Backdrop (mostra il logo TMDB se disponibile, altrimenti il titolo testuale)
                if (config.coverStyle == ShareCoverStyle.BACKDROP_FULL) {
                    if (logoUrl != null) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(logoUrl)
                                .crossfade(false)
                                .build(),
                            contentDescription = movie.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .heightIn(max = 68.dp)
                                .padding(horizontal = 8.dp),
                            alignment = Alignment.Center
                        ) {
                            val painterState = painter.state
                            if (painterState is AsyncImagePainter.State.Error) {
                                Text(
                                    text = movie.displayName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        color = Color.White,
                                        lineHeight = 26.sp
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
                            text = movie.displayName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color.White,
                                lineHeight = 26.sp
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Year & Metadata
                val metaItems = mutableListOf<String>()
                val year = (movie.releaseDate ?: movie.firstAirDate)?.take(4) ?: movie.releaseYear
                if (!year.isNullOrBlank()) metaItems.add(year)
                if (movie.mediaType == "tv") {
                    val seasons = movie.numberOfSeasons ?: 1
                    metaItems.add(if (seasons == 1) "1 Stagione" else "$seasons Stagioni")
                } else if ((movie.runtime ?: 0) > 0) {
                    val hours = movie.runtime!! / 60
                    val mins = movie.runtime!! % 60
                    metaItems.add(if (hours > 0) "${hours}h ${mins}m" else "${mins}m")
                }

                if (metaItems.isNotEmpty()) {
                    Text(
                        text = metaItems.joinToString(" • "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // HERO RATING (Prominente ed elegante con scala /10 esplicita)
                if (hasRating) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.28f),
                                        accentColor.copy(alpha = 0.16f)
                                    )
                                )
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.50f), RoundedCornerShape(50))
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                            contentDescription = null,
                            tint = ColorUtils.lightenForText(accentColor, 1.35f),
                            modifier = Modifier.size(23.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f", effectiveRating),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = ColorUtils.lightenForText(accentColor, 1.35f),
                                lineHeight = 26.sp
                            )
                            Text(
                                text = "/10",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorUtils.lightenForText(accentColor, 1.35f).copy(alpha = 0.75f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }

                // USER PROFILE (Spostato più in basso rispetto al voto)
                if (config.showProfile && !userDisplayName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        if (!userPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(userPhotoUrl).crossfade(false).build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userDisplayName.take(1).uppercase(),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = "@$userDisplayName",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }

        // Floating MVP Badge for Full Backdrop style
        if (config.coverStyle == ShareCoverStyle.BACKDROP_FULL && config.showMvp) {
            val actorName = movie.favoriteActorName
            val profilePath = movie.favoriteActorProfilePath ?: movie.favoriteActorTmdbPath
            if (!actorName.isNullOrBlank()) {
                MvpMiniCard(
                    actorName = actorName,
                    characterName = movie.favoriteActorCharacter,
                    profilePath = profilePath,
                    accentColor = accentColor,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 44.dp)
                )
            }
        }
    }
}

@Composable
private fun MvpMiniCard(
    actorName: String,
    characterName: String?,
    profilePath: String?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val profileUrl = remember(profilePath) {
        buildTmdbImageUrl(profilePath, ImageType.PROFILE, com.cinetrack.util.ImageQuality.HIGH)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(66.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141418).copy(alpha = 0.92f))
            .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = "👑", fontSize = 8.5.sp)
            Text(
                text = "MVP",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = ColorUtils.lightenForText(accentColor, 1.35f),
                letterSpacing = 0.4.sp
            )
        }

        if (profileUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
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

        if (!characterName.isNullOrBlank()) {
            Text(
                text = characterName,
                color = Color.White,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                lineHeight = 9.5.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = actorName,
            color = if (!characterName.isNullOrBlank()) Color.White.copy(alpha = 0.6f) else Color.White,
            fontSize = if (!characterName.isNullOrBlank()) 7.5.sp else 8.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            lineHeight = 8.5.sp,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}
