package com.cinetrack.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.theme.PrimaryTeal
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import androidx.compose.ui.res.stringResource
import com.cinetrack.R

/**
 * "The Trove's Pick" — una singola card premium che mette in evidenza
 * il titolo top consigliato dall'algoritmo di raccomandazione.
 */
@Composable
fun TrovePickCard(
    movie: Movie,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backdropUrl = buildTmdbImageUrl(
        movie.backdropPath ?: movie.posterPath,
        ImageType.BACKDROP,
        ImageQuality.HIGH
    )
    val posterUrl = buildTmdbImageUrl(
        movie.posterPath,
        ImageType.POSTER,
        ImageQuality.MEDIUM
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Titolo sezione
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_sparkle),
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_section_trove_pick),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Card principale
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .bounceClick { onMovieClick(movie) }
        ) {
            // Backdrop come sfondo della card
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Overlay scuro
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Contenuto card: poster + testo
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Poster
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = movie.title ?: movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(90.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                )

                // Testo
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Badge "Pick" text only
                    Text(
                        text = stringResource(R.string.home_trove_pick_badge).uppercase(),
                        color = PrimaryTeal,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    // Titolo
                    Text(
                        text = movie.title ?: movie.name ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            lineHeight = 26.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Anno
                    val year = (movie.releaseDate ?: movie.firstAirDate)?.take(4) ?: ""
                    if (year.isNotEmpty()) {
                        Text(
                            text = year,
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Voto se disponibile
                    val rating = movie.voteAverage
                    if (rating != null && rating > 0.0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_star_piena),
                                contentDescription = null,
                                tint = com.cinetrack.ui.theme.NeonPink,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%.1f".format(rating),
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // Glow teal nell'angolo in alto a destra
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PrimaryTeal.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
