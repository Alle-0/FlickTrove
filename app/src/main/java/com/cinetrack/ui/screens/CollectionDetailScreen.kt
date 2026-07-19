package com.cinetrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.ui.components.card.MovieCard
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.MovieActionsState
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.ui.utils.UiText
import com.cinetrack.ui.viewmodel.CollectionDetailViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlin.math.max

data class CollectionDetailScreen(
    val collectionId: Long,
    val collectionName: String? = null
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel = getViewModel<CollectionDetailViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val hazeState = remember { HazeState() }

        LaunchedEffect(collectionId) {
            viewModel.initCollection(collectionId, collectionName)
        }

        MovieActionsWrapper(
            hazeState = hazeState,
            folders = uiState.folders,
            isItemInFolder = { movie, folderId ->
                uiState.folders.find { it.id == folderId }?.itemIds?.contains("${movie.mediaType}_${movie.id}") ?: false
            },
            onDelete = { /* Collection detail usually doesn't delete */ },
            onUpdateRating = { movie, rating -> viewModel.updateRating(movie, rating) },
            onUpdateNote = { movie, note -> viewModel.updateNote(movie, note) },
            onToggleFolder = { movie, folder -> viewModel.toggleItemInFolder(folder, movie) }
        ) { actionsState ->
            CollectionDetailScreenContent(
                viewModel = viewModel,
                hazeState = hazeState,
                actionsState = actionsState,
                onBack = { navigator.pop() },
                onMovieClick = { movie ->
                    navigator.push(MovieDetailScreen(movie.id, "movie"))
                }
            )
        }
    }
}

@Composable
fun CollectionDetailScreenContent(
    viewModel: CollectionDetailViewModel,
    hazeState: HazeState,
    actionsState: MovieActionsState,
    onBack: () -> Unit,
    onMovieClick: (com.cinetrack.data.model.Movie) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val columns = max(3, (screenWidth / 130.dp).toInt())
    val cardWidth = (screenWidth - 32.dp - (12.dp * (columns - 1))) / columns

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .haze(hazeState)
    ) {
        CinematicBackground(modifier = Modifier.fillMaxSize())

        if (uiState.isLoading && uiState.collection == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val collection = uiState.collection
            val backdropUrl = buildTmdbImageUrl(
                path = collection?.backdropPath ?: collection?.posterPath,
                type = ImageType.BACKDROP,
                quality = LocalImageQuality.current
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Hero Header Section
                item(span = { GridItemSpan(columns) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        AsyncImage(
                            model = backdropUrl,
                            contentDescription = uiState.collectionName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Black.copy(alpha = 0.7f),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        ) {
                            Text(
                                text = uiState.collectionName ?: "Collezione",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 28.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!collection?.overview.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = collection?.overview ?: "",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            val countText = stringResource(
                                R.string.collection_movies_count_format,
                                collection?.parts?.size ?: 0
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = countText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(columns) }) {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Movies inside the collection
                val parts = collection?.parts ?: emptyList()
                itemsIndexed(parts, key = { _, movie -> movie.id }) { index, movie ->
                    Box(modifier = Modifier.padding(horizontal = if (columns == 1) 16.dp else 0.dp)) {
                        val folderColors = uiState.movieFolderColors[movie.id.toString()] ?: emptyList()
                        val folderColorObjects = remember(folderColors) {
                            folderColors.map { Color(android.graphics.Color.parseColor(it)) }
                        }
                        val fav = uiState.favorites.find { it.id == movie.id }
                        val isFavorite = fav?.favorite == true
                        val isWatched = fav?.watched == true
                        val isReminder = fav?.reminder == true

                        MovieCard(
                            movie = movie,
                            cardWidth = cardWidth,
                            isFavorite = isFavorite,
                            isWatched = isWatched,
                            isReminder = isReminder,
                            folderColors = folderColorObjects,
                            showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                            showBadges = uiState.preferences.showBadges,
                            showAdvancedBadges = false,
                            hazeState = hazeState,
                            staggerIndex = index,
                            onPress = { onMovieClick(movie) },
                            onLongPress = { m, pressOffset, cardPos ->
                                actionsState.onLongPress(m, pressOffset, cardPos)
                            }
                        )
                    }
                }
            }
        }

        // Top Back Button
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .hazeGlass(state = hazeState, shape = CircleShape)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Indietro",
                    tint = Color.White
                )
            }
        }
    }
}
