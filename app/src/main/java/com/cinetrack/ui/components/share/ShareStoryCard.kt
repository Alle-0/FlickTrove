package com.cinetrack.ui.components.share

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.detail.EmotionalVibe
import com.cinetrack.ui.components.detail.findVibe
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState

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
 * Encapsulates the resolved display state and resources required across
 * card background and adaptive foreground layout variants.
 */
internal data class ShareCardContentState(
    val movie: Movie,
    val config: ShareStoryCardConfig,
    val accentColor: Color,
    val userDisplayName: String?,
    val userPhotoUrl: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val logoUrl: String?,
    val cardHazeState: HazeState,
    val effectiveRating: Double,
    val hasRating: Boolean,
    val vibes: List<EmotionalVibe>,
    val metaItems: List<String>
)

/**
 * Story and Social Share Card for ratings, reviews, and cinephile recommendations.
 * Designed with a premium cinematic dark aesthetic, soft glassmorphism,
 * deep dynamic gradients, and responsive layout architectures for 9:16, 4:5, 1:1, and 16:9.
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

    val posterUrl = remember(config.customPosterPath, movie.posterPath) {
        val path = config.customPosterPath ?: movie.posterPath
        if (!path.isNullOrBlank()) buildTmdbImageUrl(path, ImageType.POSTER, ImageQuality.HIGH) else null
    }

    val backdropUrl = remember(config.customBackdropPath, movie.customBackdropPath, movie.backdropPath, movie.posterPath) {
        val path = config.customBackdropPath ?: movie.customBackdropPath ?: movie.backdropPath ?: movie.posterPath
        if (!path.isNullOrBlank()) buildTmdbImageUrl(path, ImageType.BACKDROP, ImageQuality.HIGH) else null
    }

    val logoUrl = remember(logoPath) {
        if (!logoPath.isNullOrBlank()) {
            buildTmdbImageUrl(logoPath, ImageType.LOGO, ImageQuality.HIGH)
        } else null
    }

    val effectiveRating = movie.personalRating ?: 0.0
    val hasRating = config.showRating && effectiveRating > 0.0

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

    val contentState = ShareCardContentState(
        movie = movie,
        config = config,
        accentColor = accentColor,
        userDisplayName = userDisplayName,
        userPhotoUrl = userPhotoUrl,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        logoUrl = logoUrl,
        cardHazeState = cardHazeState,
        effectiveRating = effectiveRating,
        hasRating = hasRating,
        vibes = vibes,
        metaItems = metaItems
    )

    // Outer Card Container (responsive to config.aspectRatio)
    Box(
        modifier = modifier
            .aspectRatio(config.aspectRatio.ratio)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF09090B))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
    ) {
        // --- 1. BACKGROUND LAYERS ---
        ShareCardBackground(
            coverStyle = config.coverStyle,
            aspectRatio = config.aspectRatio,
            backdropUrl = backdropUrl,
            posterUrl = posterUrl,
            cardHazeState = cardHazeState
        )

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
            if (isLandscape) {
                if (config.coverStyle == ShareCoverStyle.POSTER_FLOATING) {
                    ShareLandscapePosterLayout(state = contentState)
                } else {
                    ShareLandscapeBackdropLayout(state = contentState)
                }
            } else {
                if (config.coverStyle == ShareCoverStyle.POSTER_FLOATING) {
                    ShareVerticalPosterLayout(state = contentState)
                } else {
                    ShareVerticalBackdropLayout(state = contentState)
                }
            }
        }
    }
}
