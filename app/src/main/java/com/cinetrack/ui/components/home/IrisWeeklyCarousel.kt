package com.cinetrack.ui.components.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.model.IrisWeeklyPicksData
import com.cinetrack.data.model.IrisWeeklyUiState
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.toMovie
import com.cinetrack.ui.components.card.MovieCard
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@Composable
fun IrisWeeklyCarousel(
    state: IrisWeeklyUiState,
    hazeState: HazeState,
    resolveLocalMovie: (Movie) -> Movie,
    isMovieFavorite: (Movie) -> Boolean,
    isMovieWatched: (Movie) -> Boolean,
    isMovieReminder: (Movie) -> Boolean,
    getMovieProgress: (Movie) -> Float,
    getMovieFolderColors: (Movie) -> List<Color>,
    onMovieClick: (Movie) -> Unit,
    onMovieLongPress: (Movie, androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit,
    onAction: (Movie) -> Unit,
    onMessage: (String) -> Unit,
    onRotateFocus: () -> Unit,
    onExploreCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is IrisWeeklyUiState.Loading -> {
            // Skeleton / Shimmer discreto durante il primo caricamento
            IrisWeeklyLoadingSkeleton(modifier = modifier)
        }
        is IrisWeeklyUiState.ColdStartNeeded -> {
            IrisWeeklyColdStartCard(
                hazeState = hazeState,
                onExplore = onExploreCatalog,
                modifier = modifier
            )
        }
        is IrisWeeklyUiState.Ready -> {
            IrisWeeklyReadySection(
                picks = state.picks,
                isRotating = state.isRotating,
                hazeState = hazeState,
                resolveLocalMovie = resolveLocalMovie,
                isMovieFavorite = isMovieFavorite,
                isMovieWatched = isMovieWatched,
                isMovieReminder = isMovieReminder,
                getMovieProgress = getMovieProgress,
                getMovieFolderColors = getMovieFolderColors,
                onMovieClick = onMovieClick,
                onMovieLongPress = onMovieLongPress,
                onAction = onAction,
                onMessage = onMessage,
                onRotateFocus = onRotateFocus,
                modifier = modifier
            )
        }
        is IrisWeeklyUiState.Error -> {
            // Se c'è errore senza fallback, collassa elegantemente a zero-ingombro
        }
    }
}

@Composable
private fun IrisWeeklyReadySection(
    picks: IrisWeeklyPicksData,
    isRotating: Boolean,
    hazeState: HazeState,
    resolveLocalMovie: (Movie) -> Movie,
    isMovieFavorite: (Movie) -> Boolean,
    isMovieWatched: (Movie) -> Boolean,
    isMovieReminder: (Movie) -> Boolean,
    getMovieProgress: (Movie) -> Float,
    getMovieFolderColors: (Movie) -> List<Color>,
    onMovieClick: (Movie) -> Unit,
    onMovieLongPress: (Movie, androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit,
    onAction: (Movie) -> Unit,
    onMessage: (String) -> Unit,
    onRotateFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRotating) 360f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "iris_rotation"
    )

    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // --- HEADER CINEMATOGRAFICO PULITO ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge diaframma ottico a lamelle
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_aperture),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.iris_weekly_section_title).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (picks.focusClusterTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = picks.focusClusterTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tasto "Cambia Fuoco" - visibile SOLO se l'utente ha più di 1 cluster amato
            if (picks.hasMultipleClusters) {
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    modifier = Modifier
                        .bounceClick(onClick = onRotateFocus)
                        .hazeGlass(state = hazeState, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_aperture),
                        contentDescription = stringResource(R.string.iris_weekly_change_focus),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(rotationAngle)
                    )
                    Text(
                        text = stringResource(R.string.iris_weekly_change_focus),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- CAROSELLO PULITO (ZERO NUMERI GIGANTI) ---
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                count = picks.candidateMovies.size,
                key = { index -> "iris_weekly_${picks.candidateMovies[index].id}" }
            ) { index ->
                val searchResult = picks.candidateMovies[index]
                val baseMovie = remember(searchResult) { searchResult.toMovie() }
                val movie = resolveLocalMovie(baseMovie)

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
                        hazeState = hazeState,
                        staggerIndex = index,
                        onPress = onMovieClick,
                        onLongPress = onMovieLongPress,
                        onAction = onAction,
                        onMessage = onMessage
                    )
                }
            }
        }
    }
}

@Composable
private fun IrisWeeklyColdStartCard(
    hazeState: HazeState,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .hazeGlass(state = hazeState, shape = RoundedCornerShape(16.dp))
            .bounceClick(onClick = onExplore)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_aperture),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.iris_weekly_cold_start_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.iris_weekly_cold_start_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun IrisWeeklyLoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.08f))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
            }
        }
    }
}
