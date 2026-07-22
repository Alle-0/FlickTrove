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
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
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

@Composable
fun DiscoverMoreTrendingCard(
    cardWidth: Dp,
    columns: Int,
    onClick: () -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary
    if (columns == 1) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .bounceClick(onClick = onClick)
                .background(
                    color = Color(0xFF191924),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.5f),
                            accentColor.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accentColor.copy(alpha = 0.16f), CircleShape)
                        .border(1.dp, accentColor.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.search_discover_more),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        val cardHeight = cardWidth * 1.5f
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .bounceClick(onClick = onClick)
                .background(
                    color = Color(0xFF191924),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.55f),
                            accentColor.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(accentColor.copy(alpha = 0.18f), CircleShape)
                        .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.search_discover_more),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
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
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit,
    onDiscoverMore: (() -> Unit)? = null
) {
    if (trendingMovies.isNotEmpty() || isLoading) {
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
        if (trendingMovies.isNotEmpty()) {
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
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit,
    onDiscoverMore: (() -> Unit)? = null
) {
    if (trendingTv.isNotEmpty() || isLoading) {
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
        if (trendingTv.isNotEmpty()) {
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
    onCollectionClick: (Long, String?) -> Unit
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
                            onCollectionClick(item.id, item.name)
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

@Composable
fun CollectionCollageCard(
    collection: TMDBSearchResult.CollectionResult,
    favorites: List<Movie> = emptyList(),
    onClick: () -> Unit
) {
    val backdropUrl = buildTmdbImageUrl(collection.backdropPath ?: collection.posterPath, ImageType.BACKDROP, LocalImageQuality.current)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .bounceClick(scaleDown = 0.96f) { onClick() }
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF14141E))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
    ) {
        if (backdropUrl != null) {
            SubcomposeAsyncImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    ImagePlaceholder(
                        title = null,
                        mediaType = "movie",
                        isBackdrop = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        } else {
            ImagePlaceholder(
                title = null,
                mediaType = "movie",
                isBackdrop = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF09090F).copy(alpha = 0.55f),
                            Color(0xFF09090F).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = collection.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 17.sp,
                        lineHeight = 21.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val totalCount = collection.partsCount
            val watchedCount = remember(collection.partsIds, favorites) {
                if (collection.partsIds.isNotEmpty()) {
                    collection.partsIds.count { partId -> favorites.find { it.id == partId }?.watched == true }
                } else 0
            }
            val progress = if (totalCount > 0) (watchedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
            val accentColor = MaterialTheme.colorScheme.primary
            val globalAccentColor = remember(accentColor) { ColorUtils.ensureVividAccent(accentColor) }

            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFF0F0F1A).copy(alpha = 0.82f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                PillProgressBorder(
                    progress = progress,
                    color = globalAccentColor,
                    modifier = Modifier.matchParentSize()
                )
                val badgeText = when {
                    totalCount == 1 -> stringResource(R.string.collection_badge_movies_single, 1)
                    totalCount > 1 -> stringResource(R.string.collection_badge_movies_plural, totalCount)
                    collection.partsPosterPaths.isNotEmpty() -> stringResource(R.string.collection_badge_movies_plus, collection.partsPosterPaths.size)
                    else -> "SAGA"
                }
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
    collectionSpan: Int,
    cardWidth: Dp,
    personCardWidth: Dp,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    keyboardController: SoftwareKeyboardController?,
    onMovieClick: (Movie) -> Unit,
    onPersonClick: (Long) -> Unit,
    onCollectionClick: (Long, String?) -> Unit,
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
            is TMDBSearchResult.CollectionResult -> "${item.id}_collection"
            else -> "${item.id}_other"
        }},
        span = { _, _ ->
            when (category) {
                "person" -> GridItemSpan(personSpan)
                "collection" -> GridItemSpan(collectionSpan)
                else -> GridItemSpan(movieSpan)
            }
        }
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
            is TMDBSearchResult.CollectionResult -> {
                CollectionCollageCard(
                    collection = item,
                    favorites = favorites,
                    onClick = {
                        keyboardController?.hide()
                        onCollectionClick(item.id, item.name)
                    }
                )
            }
        }
    }
}
