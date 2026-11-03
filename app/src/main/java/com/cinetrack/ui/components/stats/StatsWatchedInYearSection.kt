package com.cinetrack.ui.components.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.card.MovieCard
import com.cinetrack.ui.utils.horizontalFadingEdges
import dev.chrisbanes.haze.HazeState

/**
 * Sezione carosello orizzontale dei titoli visti durante l'anno selezionato.
 */
@Composable
fun StatsWatchedInYearSection(
    year: Int,
    movies: List<Movie>,
    hazeState: HazeState?,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().statsCard()) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                val sectionTitle = stringResource(R.string.stats_watched_in_year, year)
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StatsSectionHeader(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_ciak),
                        title = sectionTitle,
                        count = movies.size
                    )
                }
                val rowState = rememberLazyListState()
                LazyRow(
                    state = rowState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .horizontalFadingEdges(rowState, leftEdgeWidth = 32.dp, rightEdgeWidth = 32.dp)
                ) {
                    items(
                        items = movies,
                        key = { "${it.id}_${it.mediaType}" }
                    ) { movie ->
                        MovieCard(
                            movie = movie,
                            cardWidth = 100.dp,
                            isWatched = movie.watched,
                            isFavorite = movie.favorite,
                            isReminder = movie.reminder,
                            progress = (movie.progress ?: 0.0).toFloat(),
                            showBadges = false,
                            showAdvancedBadges = false,
                            showActionButton = false,
                            bounceEnabled = true,
                            hazeState = hazeState,
                            onPress = { onMovieClick(movie) }
                        )
                    }
                }
            }
        }
    }
}
