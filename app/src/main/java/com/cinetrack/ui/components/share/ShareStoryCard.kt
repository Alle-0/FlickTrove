package com.cinetrack.ui.components.share

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
    @StringRes val subtitleResId: Int,
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
        subtitleResId = R.string.share_ratio_story,
        socialHint = "Instagram • TikTok",
        widthRatio = 9f,
        heightRatio = 16f,
        targetWidthPx = 1080,
        targetHeightPx = 1920,
        baseWidthDp = 360.dp,
        baseHeightDp = 640.dp,
        previewWidthDp = 235.dp
    ),
    POST_4_5(
        ratioName = "4:5",
        subtitleResId = R.string.share_ratio_post,
        socialHint = "Instagram Feed",
        widthRatio = 4f,
        heightRatio = 5f,
        targetWidthPx = 1080,
        targetHeightPx = 1350,
        baseWidthDp = 360.dp,
        baseHeightDp = 450.dp,
        previewWidthDp = 280.dp
    ),
    SQUARE_1_1(
        ratioName = "1:1",
        subtitleResId = R.string.share_ratio_square,
        socialHint = "Post universale",
        widthRatio = 1f,
        heightRatio = 1f,
        targetWidthPx = 1080,
        targetHeightPx = 1080,
        baseWidthDp = 360.dp,
        baseHeightDp = 360.dp,
        previewWidthDp = 300.dp
    ),
    LANDSCAPE_16_9(
        ratioName = "16:9",
        subtitleResId = R.string.share_ratio_wide,
        socialHint = "X • Reddit",
        widthRatio = 16f,
        heightRatio = 9f,
        targetWidthPx = 1920,
        targetHeightPx = 1080,
        baseWidthDp = 540.dp,
        baseHeightDp = 303.75.dp,
        previewWidthDp = 340.dp
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

    val cardHazeState = remember { HazeState() }

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

    // Common formatted metadata items (Year, Seasons, Runtime)
    val metaItems = remember(movie.releaseDate, movie.firstAirDate, movie.releaseYear, movie.mediaType, movie.numberOfSeasons, movie.runtime) {
        val list = mutableListOf<String>()
        val year = (movie.releaseDate ?: movie.firstAirDate)?.take(4) ?: movie.releaseYear
        if (!year.isNullOrBlank()) list.add(year)
        if (movie.mediaType == "tv") {
            val seasons = movie.numberOfSeasons ?: 1
            list.add(
                if (seasons == 1) context.getString(R.string.share_meta_season_single)
                else context.getString(R.string.share_meta_seasons_multiple, seasons)
            )
        } else if ((movie.runtime ?: 0) > 0) {
            val hours = movie.runtime!! / 60
            val mins = movie.runtime!! % 60
            list.add(if (hours > 0) "${hours}h ${mins}m" else "${mins}m")
        }
        list
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
        AnimatedContent(
            targetState = config.coverStyle,
            transitionSpec = {
                fadeIn(animationSpec = tween(320)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "CardBgTransition",
            modifier = Modifier.fillMaxSize()
        ) { currentCoverStyle ->
            when (currentCoverStyle) {
            ShareCoverStyle.BACKDROP_FULL -> {
                // Full-bleed backdrop with dark gradient and Haze capture
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = cardHazeState, style = HazeStyles.PremiumDark)
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
                                if (config.aspectRatio == ShareAspectRatio.LANDSCAPE_16_9) {
                                    Brush.horizontalGradient(
                                        0.0f to Color(0xFF09090B).copy(alpha = 0.78f),
                                        0.38f to Color(0xFF09090B).copy(alpha = 0.35f),
                                        0.60f to Color.Transparent,
                                        1.0f to Color.Black.copy(alpha = 0.15f)
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.28f),
                                        0.08f to Color.Transparent,
                                        0.50f to Color.Transparent,
                                        0.70f to Color(0xFF09090B).copy(alpha = 0.45f),
                                        0.88f to Color(0xFF09090B).copy(alpha = 0.78f),
                                        1.0f to Color(0xFF09090B).copy(alpha = 0.92f)
                                    )
                                }
                            )
                    )
                }
            }
            ShareCoverStyle.POSTER_FLOATING -> {
                // Atmospheric backdrop in background with Haze capture
                val isLandscapeRatio = config.aspectRatio == ShareAspectRatio.LANDSCAPE_16_9
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = cardHazeState, style = HazeStyles.PremiumDark)
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
                                .blur(if (isLandscapeRatio) 6.dp else 28.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isLandscapeRatio) {
                                    Brush.horizontalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.32f),
                                        0.40f to Color(0xFF09090B).copy(alpha = 0.58f),
                                        1.0f to Color(0xFF09090B).copy(alpha = 0.82f)
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.65f),
                                        0.5f to Color(0xFF09090B).copy(alpha = 0.88f),
                                        1.0f to Color(0xFF09090B)
                                    )
                                }
                            )
                    )
                }
            }
        }
        }

        // --- 2. FOREGROUND CONTENT ADAPTIVE BY RATIO ---
        val isLandscape = config.aspectRatio == ShareAspectRatio.LANDSCAPE_16_9
        val foregroundKey = "${isLandscape}_${config.coverStyle}"
        AnimatedContent(
            targetState = foregroundKey,
            transitionSpec = {
                (fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.98f, animationSpec = tween(280)))
                    .togetherWith(fadeOut(animationSpec = tween(180)))
            },
            label = "CardForegroundTransition",
            modifier = Modifier.fillMaxSize()
        ) { _ ->
            if (config.aspectRatio == ShareAspectRatio.LANDSCAPE_16_9) {
            // ========================================================
            // === DEDICATED 16:9 LANDSCAPE HORIZONTAL ARCHITECTURE ===
            // ========================================================
            if (config.coverStyle == ShareCoverStyle.POSTER_FLOATING) {
                // 16:9 AFFIANCATO: Locandina a sinistra, Info/Voto a destra
                Row(
                    modifier = Modifier
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
                        val posterShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(elevation = 28.dp, shape = posterShape)
                                .clip(posterShape)
                                .haze(state = cardHazeState, style = HazeStyles.PremiumDark)
                                .border(1.dp, Color.White.copy(alpha = 0.22f), posterShape)
                        ) {
                            if (posterUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(posterUrl).crossfade(false).build(),
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
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        // Vibe icons floating sul poster (fuori dal source haze per non sfocare le proprie icone)
                        if (config.showVibe && vibes.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .hazeGlass(
                                        state = cardHazeState,
                                        shape = RoundedCornerShape(50),
                                        blurRadius = 16.dp,
                                        containerColor = Color.Black.copy(alpha = 0.35f),
                                        borderWidth = 0.6.dp,
                                        borderColor = Color.White.copy(alpha = 0.25f)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                vibes.forEach { vibe ->
                                    Icon(
                                        painter = painterResource(id = vibe.iconRes),
                                        contentDescription = stringResource(id = vibe.labelRes),
                                        tint = Color(vibe.colorHex),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // MVP badge in basso a destra sul poster con frosted glass: mezza dentro e mezza fuori
                        val actorName = movie.favoriteActorName
                        val profilePath = movie.favoriteActorProfilePath ?: movie.favoriteActorTmdbPath
                        if (config.showMvp && !actorName.isNullOrBlank()) {
                            MvpMiniCard(
                                actorName = actorName,
                                characterName = movie.favoriteActorCharacter,
                                profilePath = profilePath,
                                accentColor = accentColor,
                                hazeState = cardHazeState,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 20.dp, y = 14.dp)
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
                        Row(
                            modifier = Modifier
                                .hazeGlass(
                                    state = cardHazeState,
                                    shape = RoundedCornerShape(50),
                                    blurRadius = 18.dp,
                                    containerColor = Color.Black.copy(alpha = 0.40f),
                                    borderWidth = 0.6.dp,
                                    borderColor = Color.White.copy(alpha = 0.22f)
                                )
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground_vector),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "FlickTrove",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.90f),
                                letterSpacing = 0.3.sp
                            )
                        }

                        // Middle: Titolo/Logo + Metadati
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (logoUrl != null) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context).data(logoUrl).crossfade(false).build(),
                                    contentDescription = movie.displayName,
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.CenterStart,
                                    modifier = Modifier
                                        .heightIn(max = 48.dp)
                                        .widthIn(max = 240.dp)
                                ) {
                                    val state = painter.state
                                    if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                                        Text(
                                            text = movie.displayName,
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
                                    text = movie.displayName,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 23.sp
                                )
                            }
                            if (metaItems.isNotEmpty()) {
                                Text(
                                    text = metaItems.joinToString(" • "),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }

                        // Hero Rating /10
                        if (hasRating) {
                            Row(
                                modifier = Modifier
                                    .hazeGlass(
                                        state = cardHazeState,
                                        shape = RoundedCornerShape(50),
                                        blurRadius = 18.dp,
                                        containerColor = accentColor.copy(alpha = 0.22f),
                                        borderWidth = 1.dp,
                                        borderColor = accentColor.copy(alpha = 0.55f)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                                    contentDescription = null,
                                    tint = ColorUtils.lightenForText(accentColor, 1.35f),
                                    modifier = Modifier.size(21.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", effectiveRating),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ColorUtils.lightenForText(accentColor, 1.35f),
                                        lineHeight = 24.sp
                                    )
                                    Text(
                                        text = "/10",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorUtils.lightenForText(accentColor, 1.35f).copy(alpha = 0.75f),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }

                        // User profile in basso a destra
                        androidx.compose.animation.AnimatedVisibility(
                            visible = config.showProfile && !userDisplayName.isNullOrBlank(),
                            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + expandVertically(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                            ),
                            exit = fadeOut(tween(150)) + shrinkVertically(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (!userPhotoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(userPhotoUrl).crossfade(false).build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = userDisplayName.orEmpty().take(1).uppercase(),
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
                                    color = Color.White.copy(alpha = 0.70f)
                                )
                            }
                        }
                    }
                }
            } else {
                // 16:9 FULL BACKDROP: Dettagli e voto a sinistra, MVP e profilo a destra
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize()
                        .padding(horizontal = 26.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Row: Brand pill a sinistra, Vibes a destra
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .hazeGlass(
                                    state = cardHazeState,
                                    shape = RoundedCornerShape(50),
                                    blurRadius = 18.dp,
                                    containerColor = Color.Black.copy(alpha = 0.40f),
                                    borderWidth = 0.6.dp,
                                    borderColor = Color.White.copy(alpha = 0.22f)
                                )
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground_vector),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "FlickTrove",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.90f),
                                letterSpacing = 0.3.sp
                            )
                        }

                        if (config.showVibe && vibes.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .hazeGlass(
                                        state = cardHazeState,
                                        shape = RoundedCornerShape(50),
                                        blurRadius = 18.dp,
                                        containerColor = Color.Black.copy(alpha = 0.35f),
                                        borderWidth = 0.6.dp,
                                        borderColor = Color.White.copy(alpha = 0.22f)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
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
                    }

                    // Bottom Row: [Titolo + Meta + Voto] a sinistra | [MVP + Profilo] a destra
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Blocco Sinistra
                        Column(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (logoUrl != null) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context).data(logoUrl).crossfade(false).build(),
                                    contentDescription = movie.displayName,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .heightIn(max = 50.dp)
                                        .widthIn(max = 240.dp)
                                )
                            } else {
                                Text(
                                    text = movie.displayName,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 25.sp
                                )
                            }

                            if (metaItems.isNotEmpty()) {
                                Text(
                                    text = metaItems.joinToString(" • "),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.70f)
                                )
                            }

                            if (hasRating) {
                                Row(
                                    modifier = Modifier
                                        .hazeGlass(
                                            state = cardHazeState,
                                            shape = RoundedCornerShape(50),
                                            blurRadius = 18.dp,
                                            containerColor = accentColor.copy(alpha = 0.22f),
                                            borderWidth = 1.dp,
                                            borderColor = accentColor.copy(alpha = 0.55f)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                                        contentDescription = null,
                                        tint = ColorUtils.lightenForText(accentColor, 1.35f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f", effectiveRating),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ColorUtils.lightenForText(accentColor, 1.35f),
                                            lineHeight = 24.sp
                                        )
                                        Text(
                                            text = "/10",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorUtils.lightenForText(accentColor, 1.35f).copy(alpha = 0.75f),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Blocco Destra (MVP + Profilo)
                        Column(
                            modifier = Modifier.animateContentSize(),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val actorName = movie.favoriteActorName
                            val profilePath = movie.favoriteActorProfilePath ?: movie.favoriteActorTmdbPath
                            if (config.showMvp && !actorName.isNullOrBlank()) {
                                MvpMiniCard(
                                    actorName = actorName.orEmpty(),
                                    characterName = movie.favoriteActorCharacter,
                                    profilePath = profilePath,
                                    accentColor = accentColor,
                                    hazeState = cardHazeState
                                )
                            }

                            if (config.showProfile && !userDisplayName.isNullOrBlank()) {
                                val profAvatarSize = 16.dp
                                val profMargin = 3.dp
                                val profPillHeight = profAvatarSize + (profMargin * 2) // 22.dp
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier
                                        .height(profPillHeight)
                                        .hazeGlass(
                                            state = cardHazeState,
                                            shape = RoundedCornerShape(50),
                                            blurRadius = 14.dp,
                                            containerColor = Color.Black.copy(alpha = 0.45f),
                                            borderWidth = 0.6.dp,
                                            borderColor = Color.White.copy(alpha = 0.20f)
                                        )
                                        .padding(start = profMargin, end = 9.dp)
                                ) {
                                    if (!userPhotoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(userPhotoUrl).crossfade(false).build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(profAvatarSize)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(profAvatarSize)
                                                .clip(CircleShape)
                                                .background(accentColor.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = userDisplayName.orEmpty().take(1).uppercase(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Text(
                                        text = "@$userDisplayName",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.80f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ===============================================================
            // === VERTICAL LAYOUT CALIBRATED FOR STORY 9:16, POST 4:5, 1:1 ===
            // ===============================================================
            val isSquare = config.aspectRatio == ShareAspectRatio.SQUARE_1_1
            val isPost = config.aspectRatio == ShareAspectRatio.POST_4_5
            val posterFraction = when (config.aspectRatio) {
                ShareAspectRatio.STORY_9_16 -> 0.76f
                ShareAspectRatio.POST_4_5 -> 0.60f
                ShareAspectRatio.SQUARE_1_1 -> 0.38f
                ShareAspectRatio.LANDSCAPE_16_9 -> 0.30f
            }

            if (config.coverStyle == ShareCoverStyle.POSTER_FLOATING) {
                // =======================================================
                // 1) VERTICAL POSTER FLOATING (Poster hero + voto alto)
                // =======================================================
                val showProfile = config.showProfile && !userDisplayName.isNullOrBlank()
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
                    modifier = Modifier
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
                        Row(
                            modifier = Modifier
                                .hazeGlass(
                                    state = cardHazeState,
                                    shape = RoundedCornerShape(50),
                                    blurRadius = 18.dp,
                                    containerColor = Color.Black.copy(alpha = 0.40f),
                                    borderWidth = 0.6.dp,
                                    borderColor = Color.White.copy(alpha = 0.22f)
                                )
                                .padding(horizontal = if (isSquare) 8.dp else 10.dp, vertical = if (isSquare) 2.5.dp else 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground_vector),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(if (isSquare) 20.dp else 24.dp)
                            )
                            Text(
                                text = "FlickTrove",
                                fontSize = if (isSquare) 8.5.sp else 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.90f),
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(animatedTopWeight))

                    // CENTER: Floating Poster ingrandito
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(posterFraction)
                            .aspectRatio(2f / 3f)
                    ) {
                        // Poster effettivo con ombra, clip e bordo
                        val posterShape = RoundedCornerShape(if (isSquare) 18.dp else 24.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(elevation = 28.dp, shape = posterShape)
                                .clip(posterShape)
                                .haze(state = cardHazeState, style = HazeStyles.PremiumDark)
                                .border(1.dp, Color.White.copy(alpha = 0.22f), posterShape)
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
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                        }

                        // Vibe icons floating on poster (fuori dal source haze per non sfocare le proprie icone)
                        if (config.showVibe && vibes.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(if (isSquare) 6.dp else 9.dp)
                                    .hazeGlass(
                                        state = cardHazeState,
                                        shape = RoundedCornerShape(50),
                                        blurRadius = 18.dp,
                                        containerColor = Color.Black.copy(alpha = 0.35f),
                                        borderWidth = 0.6.dp,
                                        borderColor = Color.White.copy(alpha = 0.25f)
                                    )
                                    .padding(horizontal = if (isSquare) 7.dp else 9.dp, vertical = if (isSquare) 3.5.dp else 4.5.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                vibes.forEach { vibe ->
                                    Icon(
                                        painter = painterResource(id = vibe.iconRes),
                                        contentDescription = stringResource(id = vibe.labelRes),
                                        tint = Color(vibe.colorHex),
                                        modifier = Modifier.size(if (isSquare) 13.dp else 15.dp)
                                    )
                                }
                            }
                        }

                        // MVP Badge: mezza dentro e mezza fuori dal poster
                        val actorName = movie.favoriteActorName
                        val profilePath = movie.favoriteActorProfilePath ?: movie.favoriteActorTmdbPath
                        if (config.showMvp && !actorName.isNullOrBlank()) {
                            MvpMiniCard(
                                actorName = actorName.orEmpty(),
                                characterName = movie.favoriteActorCharacter,
                                profilePath = profilePath,
                                accentColor = accentColor,
                                hazeState = cardHazeState,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 20.dp, y = 16.dp)
                            )
                        }
                    }

                    // Spacing compatto: il blocco metadati e voto sale subito sotto il poster
                    Spacer(modifier = Modifier.height(if (isSquare) 4.dp else 12.dp))

                    // Year & Metadata (direttamente sotto il poster)
                    if (metaItems.isNotEmpty()) {
                        Text(
                            text = metaItems.joinToString(" • "),
                            fontSize = if (isSquare) 8.5.sp else 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.80f),
                            style = MaterialTheme.typography.bodySmall.copy(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.80f),
                                    blurRadius = 8f
                                )
                            ),
                            modifier = Modifier.padding(bottom = if (isSquare) 2.dp else 5.dp)
                        )
                    }

                    // HERO RATING (Alto, subito sotto i metadati)
                    if (hasRating) {
                        Row(
                            modifier = Modifier
                                .hazeGlass(
                                    state = cardHazeState,
                                    shape = RoundedCornerShape(50),
                                    blurRadius = 18.dp,
                                    containerColor = accentColor.copy(alpha = 0.22f),
                                    borderWidth = 1.dp,
                                    borderColor = accentColor.copy(alpha = 0.55f)
                                )
                                .padding(
                                    horizontal = if (isSquare) 11.dp else 16.dp,
                                    vertical = if (isSquare) 3.dp else 6.5.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isSquare) 4.dp else 7.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                                contentDescription = null,
                                tint = ColorUtils.lightenForText(accentColor, 1.35f),
                                modifier = Modifier.size(if (isSquare) 15.dp else 21.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", effectiveRating),
                                    fontSize = if (isSquare) 17.sp else if (isPost) 22.sp else 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ColorUtils.lightenForText(accentColor, 1.35f),
                                    lineHeight = if (isSquare) 17.sp else if (isPost) 22.sp else 24.sp
                                )
                                Text(
                                    text = "/10",
                                    fontSize = if (isSquare) 10.5.sp else 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorUtils.lightenForText(accentColor, 1.35f).copy(alpha = 0.75f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(animatedBottomWeight))

                    // USER PROFILE (in basso)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showProfile,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + expandVertically(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = fadeOut(tween(150)) + shrinkVertically(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(bottom = if (isSquare) 4.dp else 4.dp)
                        ) {
                            if (!userPhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(userPhotoUrl).crossfade(false).build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(if (isSquare) 12.dp else 14.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(if (isSquare) 12.dp else 14.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userDisplayName.orEmpty().take(1).uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = "@$userDisplayName",
                                fontSize = if (isSquare) 9.sp else 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            } else {
                // =======================================================
                // 2) VERTICAL FULL BACKDROP (Titolo/Logo, Voto, MVP pill)
                // =======================================================
                Column(
                    modifier = Modifier
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
                        Row(
                            modifier = Modifier
                                .hazeGlass(
                                    state = cardHazeState,
                                    shape = RoundedCornerShape(50),
                                    blurRadius = 18.dp,
                                    containerColor = Color.Black.copy(alpha = 0.40f),
                                    borderWidth = 0.6.dp,
                                    borderColor = Color.White.copy(alpha = 0.22f)
                                )
                                .padding(horizontal = if (isSquare) 8.dp else 10.dp, vertical = if (isSquare) 2.5.dp else 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground_vector),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(if (isSquare) 20.dp else 25.dp)
                            )
                            Text(
                                text = "FlickTrove",
                                fontSize = if (isSquare) 8.5.sp else 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.90f),
                                letterSpacing = 0.3.sp
                            )
                        }
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
                        if (config.showVibe && vibes.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .padding(bottom = if (isSquare) 6.dp else 10.dp)
                                    .hazeGlass(
                                        state = cardHazeState,
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

                        // Logo o Titolo
                        if (logoUrl != null) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(logoUrl)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = movie.displayName,
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
                                        text = movie.displayName,
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
                                text = movie.displayName,
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
                        if (metaItems.isNotEmpty()) {
                            Text(
                                text = metaItems.joinToString(" • "),
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
                        if (hasRating) {
                            Spacer(modifier = Modifier.height(if (isSquare) 5.dp else if (isPost) 6.dp else 8.dp))
                            Row(
                                modifier = Modifier
                                    .hazeGlass(
                                        state = cardHazeState,
                                        shape = RoundedCornerShape(50),
                                        blurRadius = 18.dp,
                                        containerColor = accentColor.copy(alpha = 0.22f),
                                        borderWidth = 1.dp,
                                        borderColor = accentColor.copy(alpha = 0.55f)
                                    )
                                    .padding(
                                        horizontal = if (isSquare) 14.dp else 18.dp,
                                        vertical = if (isSquare) 5.dp else 8.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (isSquare) 6.dp else 8.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                                    contentDescription = null,
                                    tint = ColorUtils.lightenForText(accentColor, 1.35f),
                                    modifier = Modifier.size(if (isSquare) 18.dp else 23.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", effectiveRating),
                                        fontSize = if (isSquare) 21.sp else if (isPost) 24.sp else 26.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ColorUtils.lightenForText(accentColor, 1.35f),
                                        lineHeight = if (isSquare) 21.sp else if (isPost) 24.sp else 26.sp
                                    )
                                    Text(
                                        text = "/10",
                                        fontSize = if (isSquare) 12.5.sp else 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorUtils.lightenForText(accentColor, 1.35f).copy(alpha = 0.75f),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }

                        // MVP PILL (Per Full Backdrop in verticale: compatto e centrato)
                        val actorName = movie.favoriteActorName
                        val profilePath = movie.favoriteActorProfilePath ?: movie.favoriteActorTmdbPath
                        if (config.showMvp && !actorName.isNullOrBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.height(if (isSquare) 5.dp else 7.dp))
                                MvpPill(
                                    actorName = actorName.orEmpty(),
                                    characterName = movie.favoriteActorCharacter,
                                    profilePath = profilePath,
                                    accentColor = accentColor,
                                    hazeState = cardHazeState
                                )
                            }
                        }

                        // USER PROFILE
                        androidx.compose.animation.AnimatedVisibility(
                            visible = config.showProfile && !userDisplayName.isNullOrBlank(),
                            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + expandVertically(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                            ),
                            exit = fadeOut(tween(150)) + shrinkVertically(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.height(if (isSquare) 5.dp else if (isPost) 7.dp else 10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(bottom = if (isSquare) 2.dp else 4.dp)
                                ) {
                                    if (!userPhotoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(userPhotoUrl).crossfade(false).build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(if (isSquare) 12.dp else 14.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(if (isSquare) 12.dp else 14.dp)
                                                .clip(CircleShape)
                                                .background(accentColor.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = userDisplayName.orEmpty().take(1).uppercase(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Text(
                                        text = "@$userDisplayName",
                                        fontSize = if (isSquare) 9.sp else 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun MvpPill(
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
    val pillHeight = avatarSize + (margin * 2) // 26.dp (perfettamente concentrico con il raggio di curvatura 13.dp)

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

@Composable
private fun MvpMiniCard(
    actorName: String,
    characterName: String?,
    profilePath: String?,
    accentColor: Color,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val profileUrl = remember(profilePath) {
        buildTmdbImageUrl(profilePath, ImageType.PROFILE, com.cinetrack.util.ImageQuality.HIGH)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(68.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(14.dp))
            .hazeGlass(
                state = hazeState,
                shape = RoundedCornerShape(14.dp),
                blurRadius = 18.dp,
                containerColor = Color(0xFF141418).copy(alpha = 0.44f),
                borderWidth = 0.8.dp,
                borderColor = Color.White.copy(alpha = 0.28f)
            )
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
