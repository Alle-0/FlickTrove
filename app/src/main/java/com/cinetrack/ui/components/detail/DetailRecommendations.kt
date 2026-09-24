package com.cinetrack.ui.components.detail

import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.data.model.Movie
import com.cinetrack.data.api.Collection
import com.cinetrack.ui.components.card.MovieCard

/**
 * DetailRecommendations
 * Renders Movie Collections and Recommended Movies using LazyRows.
 * Uses the high-fidelity MovieCard for a premium feel and visual consistency.
 */
@Composable
fun DetailRecommendations(
    collection: Collection?,
    collectionMovies: List<Movie> = emptyList(),
    recommendedMovies: List<Movie>,
    currentId: Long,
    accentColor: Color,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    onMovieClick: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit = { _, _, _ -> },
    onAction: (Movie) -> Unit,
    onMessage: (String) -> Unit,
    onCollectionClick: ((Long, String) -> Unit)? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    scrollState: ScrollState? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // --- SEZIONE COLLEZIONE ---
        if (collection != null && collectionMovies.isNotEmpty()) {
            val backdropUrl = collection.backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }
            val lazyListState = rememberLazyListState()
            val screenHeightPx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
            var collectionStaticY by remember(collection.id) { mutableStateOf<Float?>(null) }
            var collectionHeightPx by remember(collection.id) { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clipToBounds()
                    .onGloballyPositioned { coordinates ->
                        if (collectionStaticY == null && scrollState != null) {
                            collectionStaticY = coordinates.positionInRoot().y + scrollState.value
                            collectionHeightPx = coordinates.size.height.toFloat()
                        }
                    }
            ) {
                // Backdrop image & gradienti
                if (backdropUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(backdropUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                // 1. Scalatura di sicurezza per evitare bordi vuoti durante la traslazione
                                scaleX = 1.25f
                                scaleY = 1.25f

                                // 2. Parallasse orizzontale reattiva allo swipe dei film della collezione
                                val hScroll = lazyListState.firstVisibleItemIndex * 150f + lazyListState.firstVisibleItemScrollOffset
                                translationX = (-hScroll * 0.15f).coerceIn(-100f, 100f)

                                // 3. Parallasse verticale 'a finestra' reattiva allo scroll della pagina
                                if (scrollState != null && collectionStaticY != null) {
                                    val currentCenter = (collectionStaticY!! - scrollState.value) + (collectionHeightPx / 2f)
                                    val viewportCenter = screenHeightPx / 2f
                                    val delta = currentCenter - viewportCenter
                                    translationY = (-delta * 0.18f).coerceIn(-80f, 80f)
                                }
                            }
                    )
                    // Dimming overlay — rende l'immagine più opaca per fondersi elegantemente
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(backgroundColor.copy(alpha = 0.55f))
                    )
                    // Top gradient fade (sfuma la parte superiore verso il colore di sfondo della schermata)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(backgroundColor, Color.Transparent)
                                )
                            )
                    )
                    // Bottom gradient fade (sfuma la parte inferiore verso il colore di sfondo della schermata)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, backgroundColor)
                                )
                            )
                    )
                }

                // Collection content overlaid on top
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)
                            .then(
                                if (onCollectionClick != null) {
                                    Modifier.bounceClick {
                                        onCollectionClick(collection.id.toLong(), collection.name)
                                    }
                                } else Modifier
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.detail_collection),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 3.sp
                                ),
                                color = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = collection.name,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = Color.White
                                )
                                if (onCollectionClick != null) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                                        contentDescription = "View Collection",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(start = 8.dp).size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(
                            items = collectionMovies,
                            key = { _, movie -> movie.id },
                            contentType = { _, _ -> "movie" }
                        ) { index, movie ->
                            val isCurrent = movie.id == currentId
                            MovieCard(
                                movie = movie,
                                cardWidth = 130.dp,
                                isFavorite = movie.favorite,
                                isWatched = movie.watched,
                                isReminder = movie.reminder,
                                progress = movie.progress?.toFloat() ?: 0f,
                                personalRating = movie.personalRating,
                                animatedVisibilityScope = if (isCurrent) null else animatedVisibilityScope,
                                staggerIndex = index,
                                onPress = { if (!isCurrent) onMovieClick(movie) },
                                onLongPress = onLongPress,
                                onAction = onAction,
                                onMessage = onMessage,
                                showActionHint = false,
                                modifier = if (isCurrent) {
                                    Modifier
                                        .graphicsLayer { alpha = 0.6f }
                                        .border(
                                            width = 1.dp,
                                            color = accentColor.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(28.dp)
                                        )
                                } else Modifier
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }

        // --- SEZIONE RACCOMANDATI ---
        if (recommendedMovies.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.detail_recommendations),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    ),
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val collectionIds = collectionMovies.map { it.id }.toSet()
                    val filteredRecommendations = recommendedMovies.filter { it.id != currentId && it.id !in collectionIds }
                    itemsIndexed(
                        items = filteredRecommendations,
                        key = { _, movie -> movie.id.toString() + movie.mediaType + "_rec" },
                        contentType = { _, _ -> "movie" }
                    ) { index, movie ->
                        MovieCard(
                            movie = movie,
                            cardWidth = 130.dp,
                            isFavorite = movie.favorite,
                            isWatched = movie.watched,
                            isReminder = movie.reminder,
                            progress = movie.progress?.toFloat() ?: 0f,
                            personalRating = movie.personalRating,
                            animatedVisibilityScope = animatedVisibilityScope,
                            staggerIndex = index,
                            onPress = { onMovieClick(movie) },
                            onLongPress = onLongPress,
                            onAction = onAction,
                            onMessage = onMessage,
                            showActionHint = false
                        )
                    }
                }
            }
        }
    }
}
