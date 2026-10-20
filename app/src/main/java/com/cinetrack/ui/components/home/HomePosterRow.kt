package com.cinetrack.ui.components.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.card.MovieCard
import com.cinetrack.ui.components.card.ShowMoreCard
import dev.chrisbanes.haze.HazeState

@Composable
fun HomePosterRow(
    title: String,
    movies: List<Movie>,
    onTitleClick: (() -> Unit)? = null,
    onShowMoreClick: (() -> Unit)? = null,
    maxDisplayCount: Int? = null,
    resolveLocalMovie: (Movie) -> Movie,
    isMovieFavorite: (Movie) -> Boolean,
    isMovieWatched: (Movie) -> Boolean,
    isMovieReminder: (Movie) -> Boolean,
    getMovieProgress: (Movie) -> Float,
    getMovieFolderColors: (Movie) -> List<Color>,
    activeHazeState: HazeState?,
    onMovieClick: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onAction: (Movie) -> Unit,
    onMessage: (String) -> Unit,
    keyPrefix: String = "movie"
) {
    if (movies.isEmpty()) return

    AnimatedHomeSection {
        Column {
            HomeSectionTitle(title = title, onClick = onTitleClick)

            val displayList = if (maxDisplayCount != null) movies.take(maxDisplayCount) else movies
            val rowState = rememberSaveable(keyPrefix, saver = LazyListState.Saver) {
                LazyListState()
            }
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    count = displayList.size,
                    key = { index -> "${keyPrefix}_${displayList[index].mediaType}_${displayList[index].id}" }
                ) { index ->
                    val rawMovie = displayList[index]
                    val movie = resolveLocalMovie(rawMovie)
                    Box(modifier = Modifier.width(110.dp)) {
                        MovieCard(
                            movie = movie,
                            cardWidth = 110.dp,
                            isFavorite = isMovieFavorite(movie),
                            isWatched = isMovieWatched(movie),
                            isReminder = isMovieReminder(movie),
                            progress = getMovieProgress(movie),
                            personalRating = movie.personalRating,
                            folderColors = getMovieFolderColors(movie),
                            hazeState = activeHazeState,
                            staggerIndex = index,
                            onPress = onMovieClick,
                            onLongPress = onLongPress,
                            onAction = onAction,
                            onMessage = onMessage
                        )
                    }
                }
                if (onShowMoreClick != null && (maxDisplayCount == null || movies.size > maxDisplayCount)) {
                    item {
                        ShowMoreCard(onClick = onShowMoreClick)
                    }
                }
            }
        }
    }
}
