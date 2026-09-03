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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.toComposeColor
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
    val coroutineScope = rememberCoroutineScope()
    var extractedColor by remember { mutableStateOf<Color?>(null) }
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_section_trove_pick),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Extracted accent color or fallback to primary
        val glowColor = extractedColor ?: movie.accentColor?.toComposeColor() ?: MaterialTheme.colorScheme.primary

        // Card principale con glow custom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp) // Aumentato lo spazio naturale per il glow
                .graphicsLayer { clip = false },
            contentAlignment = Alignment.Center
        ) {
            // Ambilight Glow layer (dietro la card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp) // Più alto fisicamente (260 card + 80 padding) per far sfumare il blur senza tagli
                    .offset(y = 12.dp) 
                    .graphicsLayer {
                        alpha = 0.55f 
                        clip = false
                    }
                    .blur(24.dp) // Blur aumentato per essere più diffuso e morbido
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(backdropUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(horizontal = 24.dp) // Più stretto della card vera (16dp) così sfuma bene ai lati
                        .align(Alignment.Center)
                        .graphicsLayer {
                            // Zoom normale
                            scaleX = 1.0f
                            scaleY = 1.0f
                        }
                )
            }

            // Card content layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 16.dp)
                    .bounceClick { onMovieClick(movie) }
                    .clip(RoundedCornerShape(20.dp))
            ) {
                // Backdrop come sfondo della card
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .crossfade(true)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { result ->
                    coroutineScope.launch {
                        val bitmap = result.result.drawable.toBitmap()
                        val extracted = ColorUtils.extractAverageColor(bitmap)
                        if (extracted != Color.Unspecified) {
                            val ambientColor = ColorUtils.darkenForAmbient(extracted)
                            extractedColor = ColorUtils.ensureMinimumLuminance(ambientColor, 0.25f)
                        }
                    }
                }
            )

            // Overlay scuro (orizzontale - per leggibilità testo)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
            // (Rimosso Overlay colorato - per preservare i colori originali della copertina)
            
            // Overlay scuro (verticale in basso) per testo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )


            // Contenuto card: poster + testo
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Poster
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(posterUrl)
                        .allowHardware(false)
                        .crossfade(true)
                        .build(),
                    contentDescription = movie.title ?: movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(110.dp)
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
                        color = MaterialTheme.colorScheme.primary,
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
                            fontSize = 26.sp,
                            lineHeight = 30.sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Anno e Generi
                    val year = (movie.releaseDate ?: movie.firstAirDate)?.take(4) ?: ""
                    val genres = movie.genreIds?.mapNotNull { id ->
                        val list = if (movie.mediaType == "tv") com.cinetrack.data.model.GenreConstants.TV_GENRES else com.cinetrack.data.model.GenreConstants.MOVIE_GENRES
                        list.find { it.id == id }?.name
                    }?.take(2) ?: emptyList()
                    
                    if (year.isNotEmpty() || genres.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (year.isNotEmpty()) {
                                Text(
                                    text = year,
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            genres.forEach { genreName ->
                                Box(
                                    modifier = Modifier
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = genreName,
                                        color = Color.White.copy(alpha = 0.9f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    // Match Score (se disponibile)
                    val matchScore = movie.matchScore
                    if (matchScore != null && matchScore > 0) {
                        Box(
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${matchScore}%",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.match_score).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    } else {
                        // Fallback Voto
                        val rating = movie.voteAverage
                        if (rating != null && rating > 0.0) {
                            Box(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_star_piena),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "%.1f".format(rating),
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
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
