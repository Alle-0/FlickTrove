package com.cinetrack.ui.components.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.cinetrack.ui.utils.mirrorInRtl
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.data.api.PersonSearchResult
import com.cinetrack.data.api.TMDBSearchResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.cinetrack.data.model.UserPreferences
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.cinetrack.ui.components.shared.ImagePlaceholder
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.ui.components.card.MovieCard
import com.cinetrack.ui.components.card.MovieListCard
import com.cinetrack.ui.components.shared.CollectionCardSkeleton
import com.cinetrack.ui.components.shared.MovieCardSkeleton
import com.cinetrack.ui.components.shared.PersonCardSkeleton
import com.cinetrack.ui.components.card.PersonCard
import com.cinetrack.ui.components.card.CollectionCollageCard
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.toComposeColor
import com.cinetrack.ui.components.common.PillProgressBorder
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.horizontalFadingEdges
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState



@OptIn(ExperimentalAnimationApi::class)
fun LazyGridScope.searchTrendingMediaSection(
    trendingItems: List<TMDBSearchResult>,
    isLoading: Boolean,
    favorites: List<Movie>,
    movieFolderColors: Map<String, List<String>>,
    preferences: UserPreferences,
    animatedMovieIds: MutableSet<String>?,
    columns: Int,
    movieSpan: Int,
    cardWidth: Dp,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    keyboardController: SoftwareKeyboardController?,
    favoritesMap: Map<String, Movie>? = null,
    precomputedFolderColors: Map<String, List<Color>>? = null,
    keyPrefix: String = "trending",
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit,
    onDiscoverMore: (() -> Unit)? = null
) {
    val favMap = favoritesMap ?: favorites.associateBy { "${it.mediaType}_${it.id}" }
    val colorsMap = precomputedFolderColors ?: movieFolderColors.mapValues { entry -> entry.value.map { it.toComposeColor() } }

    if (trendingItems.isNotEmpty() || isLoading) {
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
        if (trendingItems.isNotEmpty()) {
            itemsIndexed(
                items = trendingItems.take(6),
                key = { _, item -> "${keyPrefix}_${item.id}" },
                span = { _, _ -> GridItemSpan(movieSpan) }
            ) { index, item ->
                val baseMovie = when (item) {
                    is TMDBSearchResult.MovieResult -> Movie(
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
                    is TMDBSearchResult.TvResult -> Movie(
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
                    else -> null
                }
                if (baseMovie != null) {
                    val movieStatus = favMap["${baseMovie.mediaType}_${baseMovie.id}"]
                    val movie = movieStatus ?: baseMovie
                    val folderColors = colorsMap["${movie.mediaType}_${movie.id}"] ?: emptyList()
                    SearchGridMovieItem(
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(280),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            fadeOutSpec = tween(200)
                        ),
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
        } else if (isLoading) {
            items(
                count = 6,
                contentType = { "skeleton" },
                span = { GridItemSpan(movieSpan) }
            ) {
                MovieCardSkeleton(width = cardWidth)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun LazyGridScope.searchTrendingMoviesSection(
    trendingMovies: List<TMDBSearchResult>,
    isLoading: Boolean,
    favorites: List<Movie>,
    movieFolderColors: Map<String, List<String>>,
    preferences: UserPreferences,
    animatedMovieIds: MutableSet<String>?,
    columns: Int,
    movieSpan: Int,
    cardWidth: Dp,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    keyboardController: SoftwareKeyboardController?,
    favoritesMap: Map<String, Movie>? = null,
    precomputedFolderColors: Map<String, List<Color>>? = null,
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit,
    onDiscoverMore: (() -> Unit)? = null
) = searchTrendingMediaSection(
    trendingItems = trendingMovies,
    isLoading = isLoading,
    favorites = favorites,
    movieFolderColors = movieFolderColors,
    preferences = preferences,
    animatedMovieIds = animatedMovieIds,
    columns = columns,
    movieSpan = movieSpan,
    cardWidth = cardWidth,
    animatedVisibilityScope = animatedVisibilityScope,
    keyboardController = keyboardController,
    favoritesMap = favoritesMap,
    precomputedFolderColors = precomputedFolderColors,
    keyPrefix = "trending_movie",
    onMovieClick = onMovieClick,
    onToggleFavorite = onToggleFavorite,
    onLongPress = onLongPress,
    onEmitMessage = onEmitMessage,
    onDiscoverMore = onDiscoverMore
)

@OptIn(ExperimentalAnimationApi::class)
fun LazyGridScope.searchTrendingTvSection(
    trendingTv: List<TMDBSearchResult>,
    isLoading: Boolean,
    favorites: List<Movie>,
    movieFolderColors: Map<String, List<String>>,
    preferences: UserPreferences,
    animatedMovieIds: MutableSet<String>?,
    columns: Int,
    movieSpan: Int,
    cardWidth: Dp,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    keyboardController: SoftwareKeyboardController?,
    favoritesMap: Map<String, Movie>? = null,
    precomputedFolderColors: Map<String, List<Color>>? = null,
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit,
    onDiscoverMore: (() -> Unit)? = null
) = searchTrendingMediaSection(
    trendingItems = trendingTv,
    isLoading = isLoading,
    favorites = favorites,
    movieFolderColors = movieFolderColors,
    preferences = preferences,
    animatedMovieIds = animatedMovieIds,
    columns = columns,
    movieSpan = movieSpan,
    cardWidth = cardWidth,
    animatedVisibilityScope = animatedVisibilityScope,
    keyboardController = keyboardController,
    favoritesMap = favoritesMap,
    precomputedFolderColors = precomputedFolderColors,
    keyPrefix = "trending_tv",
    onMovieClick = onMovieClick,
    onToggleFavorite = onToggleFavorite,
    onLongPress = onLongPress,
    onEmitMessage = onEmitMessage,
    onDiscoverMore = onDiscoverMore
)

fun LazyGridScope.searchTrendingPeopleSection(
    trendingPeople: List<TMDBSearchResult>,
    isLoading: Boolean,
    personSpan: Int,
    personCardWidth: Dp,
    keyboardController: SoftwareKeyboardController?,
    onPersonClick: (Long) -> Unit
) {
    if (trendingPeople.isNotEmpty() || isLoading) {
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
        if (trendingPeople.isNotEmpty()) {
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
        } else if (isLoading) {
            items(
                count = 8,
                contentType = { "skeleton" },
                span = { GridItemSpan(personSpan) }
            ) {
                PersonCardSkeleton(width = personCardWidth)
            }
        }
    }
}

fun LazyGridScope.searchTrendingCollectionsSection(
    trendingCollections: List<TMDBSearchResult>,
    isLoading: Boolean,
    collectionSpan: Int,
    keyboardController: SoftwareKeyboardController?,
    favorites: List<Movie> = emptyList(),
    onCollectionClick: (String, String?) -> Unit
) {
    if (trendingCollections.isNotEmpty() || isLoading) {
        item(span = { GridItemSpan(12) }) {
            Column {
                Text(
                    text = stringResource(R.string.collection_trending_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
        }
        if (trendingCollections.isNotEmpty()) {
            items(
                items = trendingCollections.take(6),
                key = { "trending_collection_${it.id}" },
                contentType = { "collection_result" },
                span = { GridItemSpan(collectionSpan) }
            ) { item ->
                if (item is TMDBSearchResult.CollectionResult) {
                    CollectionCollageCard(
                        collection = item,
                        favorites = favorites,
                        onClick = {
                            keyboardController?.hide()
                            onCollectionClick(item.editorialId ?: item.id.toString(), item.name)
                        }
                    )
                }
            }
        } else if (isLoading) {
            items(
                count = 6,
                contentType = { "skeleton" },
                span = { GridItemSpan(collectionSpan) }
            ) {
                CollectionCardSkeleton()
            }
        }
    }
}

fun LazyGridScope.searchEditorialCollectionsVetrina(
    editorialCollections: List<com.cinetrack.data.model.EditorialCollection>,
    keyboardController: SoftwareKeyboardController?,
    favorites: List<Movie> = emptyList(),
    onCollectionClick: (String, String?) -> Unit
) {
    if (editorialCollections.isNotEmpty()) {
        item(span = { GridItemSpan(12) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.search_editorial_collections),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                
                val rowState = rememberLazyListState()

                androidx.compose.foundation.lazy.LazyRow(
                    state = rowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalFadingEdges(
                            lazyListState = rowState,
                            leftEdgeWidth = 24.dp,
                            rightEdgeWidth = 28.dp
                        ),
                    contentPadding = PaddingValues(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        count = editorialCollections.size,
                        key = { index -> "editorial_${editorialCollections[index].id}" }
                    ) { index ->
                        val ed = editorialCollections[index]
                        val result = TMDBSearchResult.CollectionResult(
                            id = kotlin.math.abs(ed.id.hashCode()).toLong(),
                            name = ed.title,
                            posterPath = ed.posterPath,
                            backdropPath = ed.backdropPath,
                            overview = ed.description,
                            partsCount = ed.items.size,
                            partsIds = ed.items.map { it.tmdbId },
                            editorialId = ed.id,
                            hasTvSeries = ed.items.any { it.mediaType == "tv" }
                        )
                        CollectionCollageCard(
                            collection = result,
                            favorites = favorites,
                            modifier = Modifier.width(300.dp),
                            onClick = {
                                keyboardController?.hide()
                                onCollectionClick(ed.id, ed.title)
                            }
                        )
                    }
                }
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
    collectionSpan: Int,
    cardWidth: Dp,
    personCardWidth: Dp,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    keyboardController: SoftwareKeyboardController?,
    favoritesMap: Map<String, Movie>? = null,
    precomputedFolderColors: Map<String, List<Color>>? = null,
    onMovieClick: (Movie) -> Unit,
    onPersonClick: (Long) -> Unit,
    onCollectionClick: (String, String?) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit
) {
    val favMap = favoritesMap ?: favorites.associateBy { "${it.mediaType}_${it.id}" }
    val colorsMap = precomputedFolderColors ?: movieFolderColors.mapValues { entry -> entry.value.map { it.toComposeColor() } }

    itemsIndexed(
        items = results,
        key = { _, item -> when (item) {
            is TMDBSearchResult.MovieResult -> "${item.id}_movie"
            is TMDBSearchResult.TvResult -> "${item.id}_tv"
            is TMDBSearchResult.PersonResult -> "${item.id}_person"
            is TMDBSearchResult.CollectionResult -> "${item.id}_collection"
        }},
        span = { _, _ ->
            when (category) {
                "person" -> GridItemSpan(personSpan)
                "collection" -> GridItemSpan(collectionSpan)
                "mood" -> GridItemSpan(12)
                else -> GridItemSpan(movieSpan)
            }
        }
    ) { index, item ->
        when (item) {
            is TMDBSearchResult.MovieResult -> {
                val effectiveOverview = item.aiReason ?: item.overview
                val baseMovie = Movie(
                    id = item.id,
                    mediaType = "movie",
                    title = item.title,
                    posterPath = item.posterPath,
                    backdropPath = item.backdropPath,
                    voteAverage = item.voteAverage,
                    releaseDate = item.releaseDate,
                    overview = effectiveOverview,
                    genreIds = item.genreIds
                )
                val movieStatus = favMap["${baseMovie.mediaType}_${baseMovie.id}"]
                val movie = if (item.aiReason != null && movieStatus != null) {
                    movieStatus.copy(overview = item.aiReason)
                } else {
                    movieStatus ?: baseMovie
                }
                val folderColors = colorsMap["${movie.mediaType}_${movie.id}"] ?: emptyList()

                if (category == "mood") {
                    SearchMoodEditorialCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .animateItem(
                                fadeInSpec = tween(280),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                fadeOutSpec = tween(200)
                            ),
                        movie = movie,
                        movieStatus = movieStatus,
                        folderColors = folderColors,
                        aiReason = item.aiReason,
                        staggerIndex = index,
                        hasAnimatedSet = animatedMovieIds,
                        keyboardController = keyboardController,
                        onMovieClick = onMovieClick,
                        onLongPress = onLongPress
                    )
                } else {
                    SearchGridMovieItem(
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(280),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            fadeOutSpec = tween(200)
                        ),
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
                        aiReason = item.aiReason,
                        onMovieClick = onMovieClick,
                        onToggleFavorite = onToggleFavorite,
                        onLongPress = onLongPress,
                        onEmitMessage = onEmitMessage
                    )
                }
            }
            is TMDBSearchResult.TvResult -> {
                val effectiveOverview = item.aiReason ?: item.overview
                val baseMovie = Movie(
                    id = item.id,
                    mediaType = "tv",
                    name = item.name,
                    posterPath = item.posterPath,
                    backdropPath = item.backdropPath,
                    voteAverage = item.voteAverage,
                    firstAirDate = item.firstAirDate,
                    overview = effectiveOverview,
                    genreIds = item.genreIds
                )
                val movieStatus = favMap["${baseMovie.mediaType}_${baseMovie.id}"]
                val movie = if (item.aiReason != null && movieStatus != null) {
                    movieStatus.copy(overview = item.aiReason)
                } else {
                    movieStatus ?: baseMovie
                }
                val folderColors = colorsMap["${movie.mediaType}_${movie.id}"] ?: emptyList()

                if (category == "mood") {
                    SearchMoodEditorialCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .animateItem(
                                fadeInSpec = tween(280),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                fadeOutSpec = tween(200)
                            ),
                        movie = movie,
                        movieStatus = movieStatus,
                        folderColors = folderColors,
                        aiReason = item.aiReason,
                        staggerIndex = index,
                        hasAnimatedSet = animatedMovieIds,
                        keyboardController = keyboardController,
                        onMovieClick = onMovieClick,
                        onLongPress = onLongPress
                    )
                } else {
                    SearchGridMovieItem(
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(280),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            fadeOutSpec = tween(200)
                        ),
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
                        aiReason = item.aiReason,
                        onMovieClick = onMovieClick,
                        onToggleFavorite = onToggleFavorite,
                        onLongPress = onLongPress,
                        onEmitMessage = onEmitMessage
                    )
                }
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
            is TMDBSearchResult.CollectionResult -> {
                CollectionCollageCard(
                    collection = item,
                    favorites = favorites,
                    onClick = {
                        keyboardController?.hide()
                        onCollectionClick(item.editorialId ?: item.id.toString(), item.name)
                    }
                )
            }
        }
    }
}
