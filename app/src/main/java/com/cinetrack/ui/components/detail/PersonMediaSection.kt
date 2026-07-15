package com.cinetrack.ui.components.detail

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.card.MovieCard
import com.cinetrack.ui.components.card.MovieListCard
import com.cinetrack.ui.components.common.CategoryPill
import dev.chrisbanes.haze.HazeState

@Composable
fun PersonHighlightsRow(
    knownFor: List<Movie>,
    favoritesMap: Map<String, Movie>,
    folderColorsMap: Map<String, List<Color>>,
    showFolderBookmarks: Boolean,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (knownFor.isNotEmpty()) {
        Text(
            text = stringResource(R.string.person_highlights),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = knownFor,
                key = { _, it -> it.id.toString() + "_" + it.mediaType }
            ) { index, m ->
                val movieStatus = favoritesMap["${m.mediaType}_${m.id}"]
                val movie = movieStatus ?: m
                val folderColors = folderColorsMap["${movie.mediaType}_${movie.id}"] ?: emptyList()
                MovieCard(
                    movie = movie,
                    cardWidth = 130.dp,
                    isFavorite = movieStatus?.favorite ?: false,
                    isWatched = movieStatus?.watched ?: false,
                    isReminder = movieStatus?.reminder ?: false,
                    progress = movieStatus?.progress?.toFloat() ?: 0f,
                    personalRating = movieStatus?.personalRating,
                    folderColors = folderColors,
                    showFolderBookmarks = showFolderBookmarks,
                    animatedVisibilityScope = animatedVisibilityScope,
                    staggerIndex = index,
                    onPress = { onMovieClick(m) },
                    onAction = { onToggleFavorite(m) },
                    onLongPress = onLongPress,
                    onMessage = onEmitMessage
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun PersonFilmographyTabs(
    activeTab: String,
    castMoviesCount: Int,
    castTVCount: Int,
    crewMoviesCount: Int,
    crewTVCount: Int,
    hazeState: HazeState,
    onTabChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.person_filmography),
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black
    )

    Row(
        modifier = modifier
            .padding(vertical = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (castMoviesCount > 0) {
            CategoryPill(
                text = stringResource(R.string.person_cast_movie_format, castMoviesCount),
                isSelected = activeTab == "cast_movie",
                hazeState = hazeState,
                onClick = { onTabChanged("cast_movie") }
            )
        }
        if (castTVCount > 0) {
            CategoryPill(
                text = stringResource(R.string.person_cast_tv_format, castTVCount),
                isSelected = activeTab == "cast_tv",
                hazeState = hazeState,
                onClick = { onTabChanged("cast_tv") }
            )
        }
        if (crewMoviesCount > 0) {
            CategoryPill(
                text = stringResource(R.string.person_crew_movie_format, crewMoviesCount),
                isSelected = activeTab == "crew_movie",
                hazeState = hazeState,
                onClick = { onTabChanged("crew_movie") }
            )
        }
        if (crewTVCount > 0) {
            CategoryPill(
                text = stringResource(R.string.person_crew_tv_format, crewTVCount),
                isSelected = activeTab == "crew_tv",
                hazeState = hazeState,
                onClick = { onTabChanged("crew_tv") }
            )
        }
    }
}

fun LazyListScope.personFilmographyGrid(
    filmoRows: List<List<Movie>>,
    columns: Int,
    cardWidth: Dp,
    padding: Dp,
    favoritesMap: Map<String, Movie>,
    folderColorsMap: Map<String, List<Color>>,
    showFolderBookmarks: Boolean,
    showBadges: Boolean,
    animatedMovieIds: MutableSet<String>?,
    hazeState: HazeState,
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit
) {
    items(
        items = filmoRows,
        key = { row -> row.joinToString("_") { "${it.id}_${it.mediaType}" } }
    ) { rowMovies ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = padding, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rowMovies.forEachIndexed { colIndex, movie ->
                val movieStatus = favoritesMap["${movie.mediaType}_${movie.id}"]
                val m = movieStatus ?: movie
                val staggerIdx = colIndex
                val folderColors = folderColorsMap["${m.mediaType}_${m.id}"] ?: emptyList()
                Box(modifier = Modifier.weight(1f)) {
                    if (columns == 1) {
                        MovieListCard(
                            movie = m,
                            modifier = Modifier.fillMaxWidth(),
                            isFavorite = movieStatus?.favorite ?: false,
                            isWatched = movieStatus?.watched ?: false,
                            isReminder = movieStatus?.reminder ?: false,
                            progress = movieStatus?.progress?.toFloat() ?: 0f,
                            personalRating = movieStatus?.personalRating,
                            folderColors = folderColors,
                            showFolderBookmarks = showFolderBookmarks,
                            showBadges = showBadges,
                            hasAnimatedSet = animatedMovieIds,
                            hazeState = hazeState,
                            staggerIndex = staggerIdx,
                            onPress = { onMovieClick(movie) },
                            onAction = { onToggleFavorite(movie) },
                            onLongPress = onLongPress,
                            onMessage = onEmitMessage
                        )
                    } else {
                        MovieCard(
                            movie = m,
                            cardWidth = cardWidth,
                            isFavorite = movieStatus?.favorite ?: false,
                            isWatched = movieStatus?.watched ?: false,
                            isReminder = movieStatus?.reminder ?: false,
                            progress = movieStatus?.progress?.toFloat() ?: 0f,
                            personalRating = movieStatus?.personalRating,
                            folderColors = folderColors,
                            showFolderBookmarks = showFolderBookmarks,
                            hasAnimatedSet = animatedMovieIds,
                            animatedVisibilityScope = null,
                            staggerIndex = staggerIdx,
                            onPress = { onMovieClick(movie) },
                            onAction = { onToggleFavorite(movie) },
                            onLongPress = onLongPress,
                            onMessage = onEmitMessage
                        )
                    }
                }
            }
            repeat(columns - rowMovies.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}
