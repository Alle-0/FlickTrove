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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
    modifier: Modifier = Modifier,
    onResolveLogo: (suspend (Movie) -> String?)? = null
) {
    val context = LocalContext.current
    val imageQuality = com.cinetrack.util.LocalImageQuality.current

    var localLogoPath by remember(movie.id, movie.logoPath) { mutableStateOf(movie.logoPath) }
    androidx.compose.runtime.LaunchedEffect(movie.id, movie.logoPath) {
        if (localLogoPath.isNullOrEmpty() && onResolveLogo != null) {
            val fetched = onResolveLogo.invoke(movie)
            if (!fetched.isNullOrEmpty()) {
                localLogoPath = fetched
            }
        } else {
            localLogoPath = movie.logoPath
        }
    }

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

            // Card content layer (raggio 32.dp concentrico)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(255.dp)
                    .padding(horizontal = 16.dp)
                    .bounceClick {
                        val movieWithLogo = if (!localLogoPath.isNullOrEmpty() && movie.logoPath == null) {
                            movie.copy().apply { this.logoPath = localLogoPath }
                        } else {
                            movie
                        }
                        onMovieClick(movieWithLogo)
                    }
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

                // Overlay gradiente orizzontale: sfumatura più scura a sinistra (0.80f che sfuma verso destra)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.80f),
                                    0.40f to Color.Black.copy(alpha = 0.42f),
                                    0.75f to Color.Transparent,
                                    1.0f to Color.Transparent
                                )
                            )
                        )
                )

                // Overlay gradiente verticale: velatura in alto (0.58f) e nero profondo e solido in basso (fino a 1.0f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.58f),
                                    0.22f to Color.Black.copy(alpha = 0.22f),
                                    0.42f to Color.Transparent,
                                    0.55f to Color.Black.copy(alpha = 0.60f),
                                    0.75f to Color.Black.copy(alpha = 0.92f),
                                    1.0f to Color.Black
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

                    // Colonna Dettagli a destra — badge in alto, contenuto in basso
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Badge "CURATED FOR YOU" ancorato in cima
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                                    CircleShape
                                )
                                .padding(horizontal = 9.dp, vertical = 3.5.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.home_trove_pick_badge).uppercase(),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                maxLines = 1
                            )
                        }

                        // Blocco inferiore: titolo, anno/generi, match + rating
                        val year = (movie.releaseDate ?: movie.firstAirDate)?.take(4) ?: ""
                        val currentLanguage = context.resources.configuration.locales[0].language
                        val genres = movie.genreIds?.mapNotNull { id ->
                            val list = if (movie.mediaType == "tv") com.cinetrack.data.model.GenreConstants.TV_GENRES else com.cinetrack.data.model.GenreConstants.MOVIE_GENRES
                            val defaultName = list.find { it.id == id }?.name ?: ""
                            com.cinetrack.data.model.GenreConstants.getLocalizedName(id, currentLanguage, defaultName).takeIf { it.isNotBlank() }
                        }?.take(3) ?: emptyList()

                        val matchScore = movie.matchScore
                        val rating = movie.voteAverage
                        val titleText = (movie.title ?: movie.name ?: "").uppercase()
                        val activeLogo = localLogoPath ?: movie.logoPath

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. Logo o titolo testuale Serif
                            if (!activeLogo.isNullOrEmpty()) {
                                val logoUrl = buildTmdbImageUrl(activeLogo, ImageType.LOGO, imageQuality)
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(logoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = movie.title ?: movie.name,
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.CenterStart,
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .heightIn(min = 28.dp, max = 46.dp)
                                )
                            } else if (titleText.isNotEmpty()) {
                                Text(
                                    text = titleText,
                                    color = Color.White,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    lineHeight = 25.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 2. Anno • Generi
                            val metaText = remember(year, genres) {
                                buildAnnotatedString {
                                    if (year.isNotEmpty()) {
                                        withStyle(
                                            SpanStyle(
                                                color = Color.White.copy(alpha = 0.95f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            )
                                        ) {
                                            append(year)
                                        }
                                    }
                                    if (year.isNotEmpty() && genres.isNotEmpty()) {
                                        withStyle(
                                            SpanStyle(
                                                color = Color.White.copy(alpha = 0.40f),
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 12.5.sp
                                            )
                                        ) {
                                            append("  •  ")
                                        }
                                    }
                                    if (genres.isNotEmpty()) {
                                        withStyle(
                                            SpanStyle(
                                                color = Color.White.copy(alpha = 0.65f),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                        ) {
                                            append(genres.joinToString(", "))
                                        }
                                    }
                                }
                            }
                            if (metaText.isNotEmpty()) {
                                Text(
                                    text = metaText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // 3. Match Score + Rating
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
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

                                if (rating != null && rating > 0.0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
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
