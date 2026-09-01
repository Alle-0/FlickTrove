package com.cinetrack.ui.components.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.theme.PrimaryTeal
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import kotlinx.coroutines.delay

@Composable
fun HeroSpotlightCarousel(
    movies: List<Movie>,
    pagerState: PagerState,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    // Auto-scroll ogni 4 secondi
    LaunchedEffect(pagerState.pageCount) {
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % pagerState.pageCount
            pagerState.animateScrollToPage(next, animationSpec = tween(600))
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(500.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val movie = movies[page]
                val context = LocalContext.current
                val backdropUrl = buildTmdbImageUrl(
                    movie.backdropPath ?: movie.posterPath,
                    ImageType.BACKDROP,
                    ImageQuality.HIGH
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onMovieClick(movie) }
                ) {
                    // Backdrop Image
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(backdropUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = movie.title ?: movie.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    val bgColor = MaterialTheme.colorScheme.background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.2f),
                                        bgColor.copy(alpha = 0.8f),
                                        bgColor
                                    ),
                                    startY = 0f,
                                    endY = 1400f
                                )
                            )
                    )

                    // Titolo e anno
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                    ) {
                        // Label categoria
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_star),
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.home_hero_featured),
                                color = PrimaryTeal,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }

                        // Titolo
                        Text(
                            text = movie.title ?: movie.name ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 30.sp,
                                lineHeight = 34.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Anno
                        val year = (movie.releaseDate ?: movie.firstAirDate)?.take(4) ?: ""
                        if (year.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = year,
                                color = Color.White.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Pallini indicatori (Symbiont / Worm effect)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val scrollPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, (movies.size - 1).toFloat())

            repeat(movies.size) { index ->
                val distance = Math.abs(scrollPosition - index).coerceIn(0f, 1f)
                
                // Da 6dp (distante) a 20dp (centrato) proporzionale allo scroll
                val width = 6.dp + (20.dp - 6.dp) * (1f - distance)
                
                // Interpolazione colore fluida legata al dito dell'utente
                val color = androidx.compose.ui.graphics.lerp(
                    start = Color.White.copy(alpha = 0.35f),
                    stop = PrimaryTeal,
                    fraction = 1f - distance
                )
                
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}
