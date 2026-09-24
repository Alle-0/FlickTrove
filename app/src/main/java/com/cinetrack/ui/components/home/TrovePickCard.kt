package com.cinetrack.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
 * "The Trove's Pick" — card asimmetrica premium che mette in evidenza
 * il titolo top consigliato con locandina a sinistra, metadati e colore estratto dalla copertina.
 */
@Composable
fun TrovePickCard(
    movie: Movie,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageQuality = com.cinetrack.util.LocalImageQuality.current

    val backdropUrl = buildTmdbImageUrl(
        movie.backdropPath ?: movie.posterPath,
        ImageType.BACKDROP,
        imageQuality
    )
    val posterUrl = buildTmdbImageUrl(
        movie.posterPath,
        ImageType.POSTER,
        imageQuality
    )

    val hasAnimated = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!hasAnimated.value) {
            hasAnimated.value = true
        }
    }

    val cardAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (hasAnimated.value) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 400,
            easing = androidx.compose.animation.core.LinearOutSlowInEasing
        ),
        label = "alpha"
    )

    val cardTranslateY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (hasAnimated.value) 0f else 60f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "translateY"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = cardAlpha
                this.translationY = cardTranslateY
            }
    ) {
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

        // Card principale con glow custom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .graphicsLayer { clip = false },
            contentAlignment = Alignment.Center
        ) {
            // Ambilight Glow layer (dietro la card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .offset(y = 10.dp)
                    .graphicsLayer {
                        alpha = 0.55f
                        clip = false
                    }
                    .blur(26.dp)
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
                        .height(255.dp)
                        .padding(horizontal = 24.dp)
                        .align(Alignment.Center)
                )
            }

            // Card content layer (raggio 32.dp concentrico)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(255.dp)
                    .padding(horizontal = 16.dp)
                    .bounceClick { onMovieClick(movie) }
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.14f)
                        ),
                        RoundedCornerShape(32.dp)
                    )
            ) {
                // Backdrop di sfondo visibile e nitido
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(backdropUrl)
                        .crossfade(true)
                        .allowHardware(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay gradiente orizzontale da sinistra a destra (stacca la locandina e ammorbidisce la scena)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.85f),
                                    0.45f to Color.Black.copy(alpha = 0.35f),
                                    1.0f to Color.Transparent
                                )
                            )
                        )
                )

                // Overlay gradiente verticale: profondo dal basso verso l'alto, con lieve sfumatura dall'alto verso il basso
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.55f),
                                    0.18f to Color.Transparent,
                                    0.42f to Color.Black.copy(alpha = 0.50f),
                                    0.68f to Color.Black.copy(alpha = 0.88f),
                                    1.0f to Color.Black.copy(alpha = 0.98f)
                                )
                            )
                        )
                )

                // Contenuto: Poster a sinistra + Colonna Dettagli a destra
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Locandina / Poster a sinistra con raggio concentrico (32dp - 16dp = 16dp)
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(posterUrl)
                            .allowHardware(false)
                            .crossfade(true)
                            .build(),
                        contentDescription = movie.title ?: movie.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(136.dp)
                            .fillMaxHeight()
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                0.5.dp,
                                Color.White.copy(alpha = 0.16f),
                                RoundedCornerShape(16.dp)
                            )
                    )

                    // Colonna Dettagli a destra
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Badge "CURATED FOR YOU" in alto
                        Text(
                            text = stringResource(R.string.home_trove_pick_badge).uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp,
                            maxLines = 1
                        )

                        // Parte inferiore: Metadati sistemati e compatti senza overflow
                        val year = (movie.releaseDate ?: movie.firstAirDate)?.take(4) ?: ""
                        val currentLanguage = context.resources.configuration.locales[0].language
                        val primaryGenre = movie.genreIds?.firstNotNullOfOrNull { id ->
                            val list = if (movie.mediaType == "tv") com.cinetrack.data.model.GenreConstants.TV_GENRES else com.cinetrack.data.model.GenreConstants.MOVIE_GENRES
                            val defaultName = list.find { it.id == id }?.name ?: ""
                            com.cinetrack.data.model.GenreConstants.getLocalizedName(id, currentLanguage, defaultName).takeIf { it.isNotBlank() }
                        }
                        val matchScore = movie.matchScore
                        val rating = movie.voteAverage

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Riga 1: Anno e Genere (testo fluido e minimale, senza pillola)
                            val metaText = listOfNotNull(
                                year.takeIf { it.isNotEmpty() },
                                primaryGenre?.takeIf { it.isNotBlank() }
                            ).joinToString(" • ")

                            if (metaText.isNotEmpty()) {
                                Text(
                                    text = metaText,
                                    color = Color.White.copy(alpha = 0.70f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Riga 2: Match Score e/o Rating (con spaziatura ariosa)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Match Score
                                if (matchScore != null && matchScore > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f), CircleShape)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = "${matchScore}%",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = stringResource(R.string.match_score).uppercase(),
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.4.sp
                                            )
                                        }
                                    }
                                }

                                // Rating con stella
                                if (rating != null && rating > 0.0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(
                                                0.5.dp,
                                                Color.White.copy(alpha = 0.14f),
                                                CircleShape
                                            )
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_star_piena),
                                            contentDescription = null,
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "%.1f".format(java.util.Locale.US, rating),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
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
