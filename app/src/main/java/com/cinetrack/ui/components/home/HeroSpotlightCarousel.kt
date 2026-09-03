package com.cinetrack.ui.components.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import kotlin.math.absoluteValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.toBitmap
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.util.toComposeColor

@Composable
fun HeroSpotlightCarousel(
    movies: List<Movie>,
    pagerState: PagerState,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return
    
    val primaryColor = MaterialTheme.colorScheme.primary

    // Auto-scroll ogni 4 secondi
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            if (pagerState.pageCount > 0) {
                val next = pagerState.currentPage + 1
                pagerState.animateScrollToPage(next, animationSpec = tween(600))
            }
        }
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp,
            modifier = Modifier
                .height(500.dp)
        ) { virtualPage ->
            val page = if (movies.isNotEmpty()) virtualPage % movies.size else 0
            val movie = if (movies.isNotEmpty()) movies[page] else return@HorizontalPager

            val context = LocalContext.current
            val backdropUrl = buildTmdbImageUrl(
                movie.backdropPath ?: movie.posterPath,
                ImageType.BACKDROP,
                ImageQuality.HIGH
            )

            val rawPageOffset = (
                (pagerState.currentPage - virtualPage) + pagerState.currentPageOffsetFraction
            )
            val pageOffset = rawPageOffset.absoluteValue
            
            val scale = 1f - (pageOffset * 0.15f).coerceIn(0f, 0.15f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = rawPageOffset * 28.dp.toPx()
                    }
                    .bounceClick { onMovieClick(movie) }
                    .clip(RoundedCornerShape(24.dp))
            ) {

                var dominantColor by remember { mutableStateOf<Color?>(null) }

                // Backdrop Image
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(backdropUrl)
                        .crossfade(true)
                        .allowHardware(false)
                        .build(),
                    contentDescription = movie.title ?: movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { result ->
                        val drawable = result.result.drawable
                        val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        bitmap?.let { b ->
                            androidx.palette.graphics.Palette.from(b).generate { palette ->
                                palette?.darkVibrantSwatch?.rgb?.let { colorInt ->
                                    dominantColor = Color(colorInt)
                                } ?: palette?.vibrantSwatch?.rgb?.let { colorInt ->
                                    dominantColor = Color(colorInt)
                                } ?: palette?.dominantSwatch?.rgb?.let { colorInt ->
                                    dominantColor = Color(colorInt)
                                } ?: palette?.mutedSwatch?.rgb?.let { colorInt ->
                                    dominantColor = Color(colorInt)
                                }
                            }
                        }
                    }
                )

                val bgColor = MaterialTheme.colorScheme.background
                val targetColor = movie.accentColor?.toComposeColor() ?: dominantColor ?: bgColor
                val animatedColor by animateColorAsState(targetValue = targetColor, label = "backdropColor")
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        animatedColor.copy(alpha = 0.3f),
                                        animatedColor.copy(alpha = 0.85f),
                                        animatedColor
                                    )
                                )
                        )
                )

                    // Titolo e anno
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Logo o Titolo
                        if (!movie.logoPath.isNullOrEmpty()) {
                            val logoUrl = com.cinetrack.util.buildTmdbImageUrl(movie.logoPath, com.cinetrack.util.ImageType.LOGO, com.cinetrack.util.LocalImageQuality.current)
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(logoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = movie.title ?: movie.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .heightIn(max = 100.dp)
                                    .fillMaxWidth(0.8f)
                            )
                        } else {
                            Text(
                                text = movie.title ?: movie.name ?: "",
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp,
                                    lineHeight = 34.sp,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.8f),
                                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                        blurRadius = 16f
                                    )
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Anno e generi
                        val year = (movie.releaseDate ?: movie.firstAirDate)?.take(4) ?: ""
                        val genres = movie.genreIds?.mapNotNull { id ->
                            val list = if (movie.mediaType == "tv") com.cinetrack.data.model.GenreConstants.TV_GENRES else com.cinetrack.data.model.GenreConstants.MOVIE_GENRES
                            list.find { it.id == id }?.name
                        }?.take(3) ?: emptyList()
                        
                        if (year.isNotEmpty() || genres.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (year.isNotEmpty()) {
                                    Text(
                                        text = year,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            shadow = androidx.compose.ui.graphics.Shadow(
                                                color = Color.Black.copy(alpha = 0.8f),
                                                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                                blurRadius = 4f
                                            )
                                        )
                                    )
                                }
                                genres.forEach { genreName ->
                                    Box(
                                        modifier = Modifier
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = genreName,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } // End Page Box
        } // End HorizontalPager

        // Pallini indicatori (Symbiont / Worm effect)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val pageCount = movies.size
            val dotSize = 6.dp
            val spacing = 8.dp
            
            val distance = dotSize + spacing
            val canvasWidth = dotSize + (distance * (pageCount - 1).coerceAtLeast(0))
            
            Canvas(
                modifier = Modifier
                    .width(canvasWidth)
                    .height(dotSize)
            ) {
                // Pallini inattivi (base)
                for (i in 0 until pageCount) {
                    val cx = (i * distance.toPx()) + (dotSize.toPx() / 2f)
                    val cy = dotSize.toPx() / 2f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = dotSize.toPx() / 2f,
                        center = Offset(cx, cy)
                    )
                }

                // Symbiont (worm animato)
                if (pageCount > 0) {
                    val virtualScrollPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction).toFloat()
                    var scrollPosition = ((virtualScrollPosition % pageCount) + pageCount) % pageCount
                    // Handle edge cases of floating point precision
                    if (scrollPosition >= pageCount) scrollPosition = 0f

                    val floor = kotlin.math.floor(scrollPosition.toDouble()).toFloat()
                    val fraction = scrollPosition - floor
                    
                    // Logica "worm": 
                    // Se andiamo oltre l'ultimo indice, il lato destro sfora e il sinistro lo segue.
                    val leftNode = floor + Math.max(0f, (fraction - 0.5f) * 2f)
                    val rightNode = floor + Math.min(1f, fraction * 2f)
                    
                    val leftX = leftNode * distance.toPx()
                    val rightX = (rightNode * distance.toPx()) + dotSize.toPx()
                    
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(leftX, 0f),
                        size = Size(rightX - leftX, dotSize.toPx()),
                        cornerRadius = CornerRadius(dotSize.toPx() / 2f, dotSize.toPx() / 2f)
                    )
                    
                    // Se stiamo completando il loop dall'ultimo al primo elemento, disegniamo anche la parte
                    // che spunta dal primo pallino.
                    if (rightNode > pageCount - 1) {
                        val overlapRightNode = rightNode - pageCount
                        val overlapLeftNode = Math.max(0f, leftNode - pageCount)
                        val ovLeftX = overlapLeftNode * distance.toPx()
                        val ovRightX = (overlapRightNode * distance.toPx()) + dotSize.toPx()
                        if (ovRightX > ovLeftX) {
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(ovLeftX, 0f),
                                size = Size(ovRightX - ovLeftX, dotSize.toPx()),
                                cornerRadius = CornerRadius(dotSize.toPx() / 2f, dotSize.toPx() / 2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
