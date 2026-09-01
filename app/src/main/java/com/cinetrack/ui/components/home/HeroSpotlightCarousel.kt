package com.cinetrack.ui.components.home

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
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
                .height(500.dp)
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

                    // Gradient overlay - blend verso basso
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.4f),
                                        Color(0xFF0F0F1A).copy(alpha = 0.95f),
                                        Color(0xFF0F0F1A)
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
                        Text(
                            text = "★ IN EVIDENZA",
                            color = PrimaryTeal,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

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

        // Pallini indicatori
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(movies.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) PrimaryTeal else Color.White.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}
