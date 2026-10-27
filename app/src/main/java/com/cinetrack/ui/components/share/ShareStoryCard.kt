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
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.detail.findVibe
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import java.util.Locale

enum class ShareCoverStyle {
    POSTER_FLOATING,
    BACKDROP_FULL
}

data class ShareStoryCardConfig(
    val coverStyle: ShareCoverStyle = ShareCoverStyle.POSTER_FLOATING,
    val showRating: Boolean = true,
    val showProfile: Boolean = true,
    val showVibe: Boolean = true,
    val showWatermark: Boolean = true
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageQuality = LocalImageQuality.current

    val posterUrl = remember(movie.posterPath) {
        val path = movie.posterPath
        if (!path.isNullOrBlank()) buildTmdbImageUrl(path, ImageType.POSTER, imageQuality) else null
    }

    val backdropUrl = remember(movie.backdropPath, movie.customBackdropPath) {
        val path = movie.customBackdropPath ?: movie.backdropPath ?: movie.posterPath
        if (!path.isNullOrBlank()) buildTmdbImageUrl(path, ImageType.BACKDROP, imageQuality) else null
    }

    val effectiveRating = movie.personalRating ?: 0.0
    val hasRating = effectiveRating > 0.0

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

    // Outer 9:16 Card Container
    Box(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF09090B))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
    ) {
        // --- 1. BACKGROUND LAYERS ---
        when (config.coverStyle) {
            ShareCoverStyle.BACKDROP_FULL -> {
                // Full-bleed backdrop with dark gradient
                if (backdropUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(backdropUrl)
                            .crossfade(true)
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
            ShareCoverStyle.POSTER_FLOATING -> {
                // Atmospheric blurred backdrop in background
                val bgUrl = backdropUrl ?: posterUrl
                if (bgUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(bgUrl)
                            .crossfade(true)
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

        // --- 2. FOREGROUND CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR: Centered FlickTrove Branding in dark glass pill (più piccola e più in alto)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 3.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground_vector),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "FlickTrove",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.90f),
                        letterSpacing = 0.2.sp
                    )
                }
            }

            // CENTER: Floating Poster (if POSTER_FLOATING style)
            if (config.coverStyle == ShareCoverStyle.POSTER_FLOATING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .aspectRatio(2f / 3f)
                        .shadow(elevation = 30.dp, shape = RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                ) {
                    if (posterUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(posterUrl)
                                .crossfade(true)
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

                    // Vibe icons floating as a badge on the poster (top-right)
                    if (config.showVibe && vibes.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
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
                // Vibes row for full backdrop mode
                if (config.coverStyle == ShareCoverStyle.BACKDROP_FULL && config.showVibe && vibes.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
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

                // Title: renderizzato SOLO nella modalità Backdrop (nella copertina è già impresso nel poster)
                if (config.coverStyle == ShareCoverStyle.BACKDROP_FULL) {
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

                // HERO RATING (Fisso, punto principale della card)
                if (hasRating) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.30f),
                                        accentColor.copy(alpha = 0.18f)
                                    )
                                )
                            )
                            .border(1.2.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                            contentDescription = null,
                            tint = ColorUtils.lightenForText(accentColor, 1.35f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", effectiveRating),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = ColorUtils.lightenForText(accentColor, 1.35f),
                            lineHeight = 26.sp
                        )
                    }
                }

                // USER PROFILE (Curator Tag sotto al voto, discreto e minimale)
                if (config.showProfile && !userDisplayName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (!userPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(userPhotoUrl).crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                    }
                }
            }
        }
    }
}
