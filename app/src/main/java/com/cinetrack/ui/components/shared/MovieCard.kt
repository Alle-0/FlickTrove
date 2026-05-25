package com.cinetrack.ui.components.shared

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Visibility
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

@Composable
fun MovieCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    width: Dp = 110.dp,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val posterUrl = remember(movie.posterPath) {
        if (movie.posterPath != null) "https://image.tmdb.org/t/p/w342${movie.posterPath}" else null
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
                .crossfade(true)
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
                val newEpisodes = movie.newEpisodesFound
                if (movie.isUpcoming == true || (newEpisodes != null && newEpisodes > 0)) {
                    TextBadge(text = "NEW", color = NeonPink)
                }

                if ((movie.voteAverage ?: 0.0) >= 8.5 && (movie.voteCount ?: 0) > 300) {
                    TextBadge(text = "BEST", color = Color(0xFF00E5FF))
                } else if ((movie.voteCount ?: 0) > 3000) {
                    TextBadge(text = "HOT", color = HazeStyles.AccentYellow)
                } else if ((movie.voteAverage ?: 0.0) >= 8.0 && (movie.voteCount ?: 0) > 1000) {
                    TextBadge(text = "WOW", color = NeonTeal)
                }
                
                val isOldAndGood = (movie.releaseDate?.startsWith("19") == true || movie.releaseDate?.startsWith("200") == true) && (movie.voteAverage ?: 0.0) >= 8.0
                if (isOldAndGood) {
                    TextBadge(text = "CULT", color = Color(0xFF9C27B0))
                }

                val genresStr = movie.genreNamesString ?: ""
                if (genresStr.contains("Horror", ignoreCase = true) || movie.genres?.any { it.name?.equals("Horror", ignoreCase = true) == true } == true) {
                    TextBadge(text = "HORROR", color = Color(0xFFE53935))
                } else if (genresStr.contains("Animation", ignoreCase = true) || genresStr.contains("Anime", ignoreCase = true)) {
                    TextBadge(text = "ANIME", color = Color(0xFFFF9800))
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
                        .size(26.dp)
                        .background(
                            HazeStyles.GlassColor.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .border(1.dp, HazeStyles.AccentYellow.copy(alpha = HazeStyles.GlassBorderAlphaActive), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = "Watched",
                        tint = HazeStyles.AccentYellow,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingBadge(rating: Double, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(HazeStyles.GlassColor.copy(alpha = HazeStyles.GlassAlphaFallback), RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = String.format("%.1f", rating),
            color = Color.White,
            fontSize = 10.sp,
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
