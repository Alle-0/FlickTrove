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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onMovieClick: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit = { _, _, _ -> },
    onAction: (Movie) -> Unit,
    onMessage: (String) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // --- SEZIONE COLLEZIONE ---
        if (collection != null && collectionMovies.isNotEmpty()) {
            Column(modifier = Modifier.padding(bottom = 72.dp)) {
                Text(
                    text = stringResource(R.string.detail_collection),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    ),
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                )
                
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                )

                LazyRow(
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
                    color = Color.White.copy(alpha = 0.5f),
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
