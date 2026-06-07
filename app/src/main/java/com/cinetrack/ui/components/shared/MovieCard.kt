package com.cinetrack.ui.components.shared

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.data.Movie
import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.ui.theme.NeonTeal
import com.cinetrack.ui.theme.NeonPink
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.data.generateBadges

@Composable
fun MovieCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    width: Dp = 110.dp,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val imageQuality = LocalImageQuality.current
    val posterUrl = remember(movie.posterPath, imageQuality) {
        buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, imageQuality)
    }

    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(28.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    ) {
        // Poster Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(posterUrl)
                .crossfade(false)
                .build(),
            contentDescription = movie.title ?: movie.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Badges Layer
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Dynamic Badges (Top Right)
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                val badges = remember(movie) { movie.generateBadges() }
                badges.forEach { badge ->
                    TextBadge(text = badge.text, color = Color(badge.colorValue))
                }
            }

            // Rating Badge (Bottom Left)
            val voteAverage = movie.voteAverage
            if (voteAverage != null && voteAverage > 0) {
                RatingBadge(
                    rating = voteAverage,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                )
            }

            // Watched Badge (Bottom Right)
            if (movie.watched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 6.dp, end = 2.dp)
                        .size(34.dp)
                        .background(
                            HazeStyles.GlassColor.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .border(1.dp, HazeStyles.AccentYellow.copy(alpha = HazeStyles.GlassBorderAlphaActive), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_eye),
                        contentDescription = "Watched",
                        tint = HazeStyles.AccentYellow,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingBadge(rating: Double, modifier: Modifier = Modifier) {
    val multiplier = com.cinetrack.LocalTitleTextSizeMultiplier.current
    Box(
        modifier = modifier
            .background(HazeStyles.GlassColor.copy(alpha = HazeStyles.GlassAlphaFallback), RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = String.format("%.1f", rating),
            color = Color.White,
            fontSize = (10 * multiplier).sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
private fun TextBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = HazeStyles.GlassAlphaLow), RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
