package com.cinetrack.ui.components.search

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.Movie
import com.cinetrack.data.api.PersonSearchResult
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.models.UserPreferences
import com.cinetrack.ui.components.MovieCard
import com.cinetrack.ui.components.MovieListCard
import com.cinetrack.ui.components.PersonCard
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.toComposeColor

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SearchGridMovieItem(
    movie: Movie,
    movieStatus: Movie?,
    folderColors: List<Color>,
    columns: Int,
    cardWidth: Dp,
    showFolderBookmarks: Boolean,
    hasAnimatedSet: MutableSet<String>?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    staggerIndex: Int,
    keyboardController: SoftwareKeyboardController?,
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit
) {
    if (columns == 1) {
        MovieListCard(
            movie = movie,
            modifier = Modifier.fillMaxWidth(),
            isFavorite = movieStatus?.favorite ?: false,
            isWatched = movieStatus?.watched ?: false,
            isReminder = movieStatus?.reminder ?: false,
            progress = movieStatus?.progress?.toFloat() ?: 0f,
            folderColors = folderColors,
            showFolderBookmarks = showFolderBookmarks,
            hasAnimatedSet = hasAnimatedSet,
            staggerIndex = staggerIndex,
            onPress = { 
                keyboardController?.hide()
                onMovieClick(movie) 
            },
            onAction = { onToggleFavorite(movie) },
            onLongPress = { m, pressOffset, cardPos ->
                onLongPress(m, pressOffset, cardPos)
            },
            onMessage = { onEmitMessage(it) }
        )
    } else {
        MovieCard(
            movie = movie,
            cardWidth = cardWidth,
            isFavorite = movieStatus?.favorite ?: false,
            isWatched = movieStatus?.watched ?: false,
            isReminder = movieStatus?.reminder ?: false,
            progress = movieStatus?.progress?.toFloat() ?: 0f,
            personalRating = movieStatus?.personalRating,
            folderColors = folderColors,
            showFolderBookmarks = showFolderBookmarks,
            hasAnimatedSet = hasAnimatedSet,
            animatedVisibilityScope = animatedVisibilityScope,
            staggerIndex = staggerIndex,
            onPress = { 
                keyboardController?.hide()
                onMovieClick(movie) 
            },
            onAction = { onToggleFavorite(movie) },
            onLongPress = { m, pressOffset, cardPos ->
                onLongPress(m, pressOffset, cardPos)
            },
            onMessage = { onEmitMessage(it) }
        )
    }
}

@Composable
fun DiscoverMoreTrendingButton(
    onClick: () -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .bounceClick(onClick = onClick)
                .background(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.35f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.search_discover_more),
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun LazyGridScope.searchTrendingMoviesSection(
    trendingMovies: List<TMDBSearchResult>,
    favorites: List<Movie>,
    movieFolderColors: Map<String, List<String>>,
    preferences: UserPreferences,
    animatedMovieIds: MutableSet<String>?,
    columns: Int,
    movieSpan: Int,
    cardWidth: Dp,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    keyboardController: SoftwareKeyboardController?,
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit,
    onDiscoverMore: (() -> Unit)? = null
) {
    if (trendingMovies.isNotEmpty()) {
        item(span = { GridItemSpan(12) }) {
            Column {
                Text(
                    text = stringResource(R.string.search_trending_now),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
        }
        itemsIndexed(
            items = trendingMovies.take(6),
            key = { _, item -> "trending_movie_${item.id}" },
            span = { _, _ -> GridItemSpan(movieSpan) }
        ) { index, item ->
            if (item is TMDBSearchResult.MovieResult) {
                val baseMovie = Movie(
                    id = item.id,
                    mediaType = "movie",
                    title = item.title,
                    posterPath = item.posterPath,
                    backdropPath = item.backdropPath,
                    voteAverage = item.voteAverage,
                    releaseDate = item.releaseDate,
                    overview = item.overview,
                    genreIds = item.genreIds
                )
                val movieStatus = favorites.find { it.id == baseMovie.id && it.mediaType == "movie" }
                val movie = movieStatus ?: baseMovie
                val folderColors = remember(movie.id, movieFolderColors) {
                    movieFolderColors["${movie.mediaType}_${movie.id}"]?.map { it.toComposeColor() } ?: emptyList()
                }
                SearchGridMovieItem(
                    movie = movie,
                    movieStatus = movieStatus,
                    folderColors = folderColors,
                    columns = columns,
                    cardWidth = cardWidth,
                    showFolderBookmarks = preferences.showFolderBookmarks,
                    hasAnimatedSet = animatedMovieIds,
                    animatedVisibilityScope = animatedVisibilityScope,
                    staggerIndex = index,
                    keyboardController = keyboardController,
                    onMovieClick = onMovieClick,
                    onToggleFavorite = onToggleFavorite,
                    onLongPress = onLongPress,
                    onEmitMessage = onEmitMessage
                )
            }
        }
        if (onDiscoverMore != null) {
            item(span = { GridItemSpan(12) }) {
                DiscoverMoreTrendingButton(onClick = onDiscoverMore)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun LazyGridScope.searchTrendingTvSection(
    trendingTv: List<TMDBSearchResult>,
    favorites: List<Movie>,
    movieFolderColors: Map<String, List<String>>,
    preferences: UserPreferences,
    animatedMovieIds: MutableSet<String>?,
    columns: Int,
    movieSpan: Int,
    cardWidth: Dp,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    keyboardController: SoftwareKeyboardController?,
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit,
    onDiscoverMore: (() -> Unit)? = null
) {
    if (trendingTv.isNotEmpty()) {
        item(span = { GridItemSpan(12) }) {
            Column {
                Text(
                    text = stringResource(R.string.search_trending_now),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
        }
        itemsIndexed(
            items = trendingTv.take(6),
            key = { _, item -> "trending_tv_${item.id}" },
            span = { _, _ -> GridItemSpan(movieSpan) }
        ) { index, item ->
            if (item is TMDBSearchResult.TvResult) {
                val baseMovie = Movie(
                    id = item.id,
                    mediaType = "tv",
                    name = item.name,
                    posterPath = item.posterPath,
                    backdropPath = item.backdropPath,
                    voteAverage = item.voteAverage,
                    firstAirDate = item.firstAirDate,
                    overview = item.overview,
                    genreIds = item.genreIds
                )
                val movieStatus = favorites.find { it.id == baseMovie.id && it.mediaType == "tv" }
                val movie = movieStatus ?: baseMovie
                val folderColors = remember(movie.id, movieFolderColors) {
                    movieFolderColors["${movie.mediaType}_${movie.id}"]?.map { it.toComposeColor() } ?: emptyList()
                }
                SearchGridMovieItem(
                    movie = movie,
                    movieStatus = movieStatus,
                    folderColors = folderColors,
                    columns = columns,
                    cardWidth = cardWidth,
                    showFolderBookmarks = preferences.showFolderBookmarks,
                    hasAnimatedSet = animatedMovieIds,
                    animatedVisibilityScope = animatedVisibilityScope,
                    staggerIndex = index,
                    keyboardController = keyboardController,
                    onMovieClick = onMovieClick,
                    onToggleFavorite = onToggleFavorite,
                    onLongPress = onLongPress,
                    onEmitMessage = onEmitMessage
                )
            }
        }
        if (onDiscoverMore != null) {
            item(span = { GridItemSpan(12) }) {
                DiscoverMoreTrendingButton(onClick = onDiscoverMore)
            }
        }
    }
}

fun LazyGridScope.searchTrendingPeopleSection(
    trendingPeople: List<TMDBSearchResult>,
    personSpan: Int,
    personCardWidth: Dp,
    keyboardController: SoftwareKeyboardController?,
    onPersonClick: (Long) -> Unit
) {
    if (trendingPeople.isNotEmpty()) {
        item(span = { GridItemSpan(12) }) {
            Column {
                Text(
                    text = stringResource(R.string.search_trending_now),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
        }
        items(
            items = trendingPeople.take(8),
            key = { "trending_person_${it.id}" },
            contentType = { "person_result" },
            span = { GridItemSpan(personSpan) }
        ) { item ->
            if (item is TMDBSearchResult.PersonResult) {
                PersonCard(
                    person = PersonSearchResult(
                        id = item.id,
                        name = item.name,
                        profilePath = item.profilePath,
                        knownForDepartment = item.knownForDepartment
                    ),
                    width = personCardWidth,
                    onClick = { 
                        keyboardController?.hide()
                        onPersonClick(item.id) 
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun LazyGridScope.searchResultsGridSection(
    results: List<TMDBSearchResult>,
    category: String,
    favorites: List<Movie>,
    movieFolderColors: Map<String, List<String>>,
    preferences: UserPreferences,
    animatedMovieIds: MutableSet<String>?,
    columns: Int,
    movieSpan: Int,
    personSpan: Int,
    cardWidth: Dp,
    personCardWidth: Dp,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    keyboardController: SoftwareKeyboardController?,
    onMovieClick: (Movie) -> Unit,
    onPersonClick: (Long) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit
) {
    itemsIndexed(
        items = results,
        key = { _, item -> when (item) {
            is TMDBSearchResult.MovieResult -> "${item.id}_movie"
            is TMDBSearchResult.TvResult -> "${item.id}_tv"
            is TMDBSearchResult.PersonResult -> "${item.id}_person"
        }},
        span = { _, _ -> if (category == "person") GridItemSpan(personSpan) else GridItemSpan(movieSpan) }
    ) { index, item ->
        when (item) {
            is TMDBSearchResult.MovieResult -> {
                val baseMovie = Movie(
                    id = item.id,
                    mediaType = "movie",
                    title = item.title,
                    posterPath = item.posterPath,
                    backdropPath = item.backdropPath,
                    voteAverage = item.voteAverage,
                    releaseDate = item.releaseDate,
                    overview = item.overview,
                    genreIds = item.genreIds
                )
                val movieStatus = favorites.find { it.id == baseMovie.id && it.mediaType == "movie" }
                val movie = movieStatus ?: baseMovie
                val folderColors = remember(movie.id, movieFolderColors) {
                    movieFolderColors["${movie.mediaType}_${movie.id}"]?.map { it.toComposeColor() } ?: emptyList()
                }
                SearchGridMovieItem(
                    movie = movie,
                    movieStatus = movieStatus,
                    folderColors = folderColors,
                    columns = columns,
                    cardWidth = cardWidth,
                    showFolderBookmarks = preferences.showFolderBookmarks,
                    hasAnimatedSet = animatedMovieIds,
                    animatedVisibilityScope = animatedVisibilityScope,
                    staggerIndex = index,
                    keyboardController = keyboardController,
                    onMovieClick = onMovieClick,
                    onToggleFavorite = onToggleFavorite,
                    onLongPress = onLongPress,
                    onEmitMessage = onEmitMessage
                )
            }
            is TMDBSearchResult.TvResult -> {
                val baseMovie = Movie(
                    id = item.id,
                    mediaType = "tv",
                    name = item.name,
                    posterPath = item.posterPath,
                    backdropPath = item.backdropPath,
                    voteAverage = item.voteAverage,
                    firstAirDate = item.firstAirDate,
                    overview = item.overview,
                    genreIds = item.genreIds
                )
                val movieStatus = favorites.find { it.id == baseMovie.id && it.mediaType == "tv" }
                val movie = movieStatus ?: baseMovie
                val folderColors = remember(movie.id, movieFolderColors) {
                    movieFolderColors["${movie.mediaType}_${movie.id}"]?.map { it.toComposeColor() } ?: emptyList()
                }
                SearchGridMovieItem(
                    movie = movie,
                    movieStatus = movieStatus,
                    folderColors = folderColors,
                    columns = columns,
                    cardWidth = cardWidth,
                    showFolderBookmarks = preferences.showFolderBookmarks,
                    hasAnimatedSet = animatedMovieIds,
                    animatedVisibilityScope = animatedVisibilityScope,
                    staggerIndex = index,
                    keyboardController = keyboardController,
                    onMovieClick = onMovieClick,
                    onToggleFavorite = onToggleFavorite,
                    onLongPress = onLongPress,
                    onEmitMessage = onEmitMessage
                )
            }
            is TMDBSearchResult.PersonResult -> {
                PersonCard(
                    person = PersonSearchResult(
                        id = item.id,
                        name = item.name,
                        profilePath = item.profilePath,
                        knownForDepartment = item.knownForDepartment
                    ),
                    width = personCardWidth,
                    onClick = { 
                        keyboardController?.hide()
                        onPersonClick(item.id) 
                    }
                )
            }
        }
    }
}
