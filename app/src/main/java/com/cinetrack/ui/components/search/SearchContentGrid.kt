package com.cinetrack.ui.components.search

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.shared.MovieActionsState
import com.cinetrack.ui.components.shared.MovieCardSkeleton
import com.cinetrack.ui.viewmodel.SearchUiState

@Composable
fun SearchContentGrid(
    uiState: SearchUiState,
    gridState: LazyGridState,
    paddingValues: PaddingValues,
    headerHeight: Dp,
    columns: Int,
    personColumns: Int,
    cardWidth: Dp,
    personCardWidth: Dp,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    keyboardController: SoftwareKeyboardController?,
    actionsState: MovieActionsState,
    animatedMovieIds: MutableSet<String>?,
    onMovieClick: (Movie) -> Unit,
    onPersonClick: (Long) -> Unit,
    onDiscoverTrendingClick: ((String) -> Unit)?,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shouldLoadMore = remember(gridState) {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisibleItem.index >= gridState.layoutInfo.totalItemsCount - 5
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !uiState.isEndReached && !uiState.isNextPageLoading) {
            onLoadNextPage()
        }
    }

    if (uiState.errorMessage != null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_cloud),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.error_state_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.errorMessage ?: stringResource(R.string.search_error_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_lente),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.detail_retry))
            }
        }
    } else {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(12),
            contentPadding = PaddingValues(
                top = headerHeight + 16.dp,
                bottom = paddingValues.calculateTopPadding() + 80.dp,
                start = 12.dp,
                end = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.fillMaxSize()
        ) {
            val movieSpan = 12 / columns
            val personSpan = 12 / personColumns
            val hasDiscoveryFilters = uiState.sortConfig.selectedGenres.isNotEmpty() ||
                    uiState.sortConfig.selectedKeywords.isNotEmpty() ||
                    uiState.sortConfig.selectedDecades.isNotEmpty() ||
                    uiState.sortConfig.sortType != "popularity"
            val showEmptySearch = uiState.query.isEmpty() && (!hasDiscoveryFilters || uiState.category == "person")

            if (showEmptySearch) {
                if (uiState.category == "movie") {
                    searchTrendingMoviesSection(
                        trendingMovies = uiState.trendingMovies,
                        favorites = uiState.favorites,
                        movieFolderColors = uiState.movieFolderColors,
                        preferences = uiState.preferences,
                        animatedMovieIds = animatedMovieIds,
                        columns = columns,
                        movieSpan = movieSpan,
                        cardWidth = cardWidth,
                        animatedVisibilityScope = animatedVisibilityScope,
                        keyboardController = keyboardController,
                        onMovieClick = onMovieClick,
                        onToggleFavorite = onToggleFavorite,
                        onLongPress = onLongPress,
                        onEmitMessage = onEmitMessage,
                        onDiscoverMore = onDiscoverTrendingClick?.let { cb -> { cb("trending_movies") } }
                    )
                }

                if (uiState.category == "tv") {
                    searchTrendingTvSection(
                        trendingTv = uiState.trendingTv,
                        favorites = uiState.favorites,
                        movieFolderColors = uiState.movieFolderColors,
                        preferences = uiState.preferences,
                        animatedMovieIds = animatedMovieIds,
                        columns = columns,
                        movieSpan = movieSpan,
                        cardWidth = cardWidth,
                        animatedVisibilityScope = animatedVisibilityScope,
                        keyboardController = keyboardController,
                        onMovieClick = onMovieClick,
                        onToggleFavorite = onToggleFavorite,
                        onLongPress = onLongPress,
                        onEmitMessage = onEmitMessage,
                        onDiscoverMore = onDiscoverTrendingClick?.let { cb -> { cb("trending_tv") } }
                    )
                }

                if (uiState.category == "person") {
                    searchTrendingPeopleSection(
                        trendingPeople = uiState.trendingPeople,
                        personSpan = personSpan,
                        personCardWidth = personCardWidth,
                        keyboardController = keyboardController,
                        onPersonClick = onPersonClick
                    )
                }
            } else {
                if (uiState.results.isEmpty() && uiState.isLoading) {
                    items(
                        if (uiState.category == "person") 16 else 12,
                        contentType = { "skeleton" },
                        span = { if (uiState.category == "person") GridItemSpan(personSpan) else GridItemSpan(movieSpan) }
                    ) {
                        MovieCardSkeleton(width = if (uiState.category == "person") personCardWidth else cardWidth)
                    }
                }

                if (uiState.query.isNotEmpty() && !uiState.isLoading && uiState.results.isEmpty()) {
                    item(span = { GridItemSpan(12) }) {
                        SearchNoResults(queryLength = uiState.query.length)
                    }
                }

                searchResultsGridSection(
                    results = uiState.results,
                    category = uiState.category,
                    favorites = uiState.favorites,
                    movieFolderColors = uiState.movieFolderColors,
                    preferences = uiState.preferences,
                    animatedMovieIds = animatedMovieIds,
                    columns = columns,
                    movieSpan = movieSpan,
                    personSpan = personSpan,
                    cardWidth = cardWidth,
                    personCardWidth = personCardWidth,
                    animatedVisibilityScope = animatedVisibilityScope,
                    keyboardController = keyboardController,
                    onMovieClick = onMovieClick,
                    onPersonClick = onPersonClick,
                    onToggleFavorite = onToggleFavorite,
                    onLongPress = onLongPress,
                    onEmitMessage = onEmitMessage
                )

                if (uiState.isNextPageLoading) {
                    items(
                        if (uiState.category == "person") 4 else columns,
                        contentType = { "skeleton" },
                        span = { if (uiState.category == "person") GridItemSpan(personSpan) else GridItemSpan(movieSpan) }
                    ) {
                        MovieCardSkeleton(width = if (uiState.category == "person") personCardWidth else cardWidth)
                    }
                }
            }
        }
    }
}
