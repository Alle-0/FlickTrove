package com.cinetrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.card.MovieListCard
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.shared.ModalBackButton
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick

data class EditorialCollectionScreen(val collectionId: String) : Screen {

    @Composable
    override fun Content() {
        val viewModel = getViewModel<EditorialCollectionViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val hazeState = LocalHazeState.current

        LaunchedEffect(collectionId) {
            viewModel.loadCollection(collectionId)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Sfondo Cinematico basato sulla cover della raccolta o sul primo elemento
            val firstMovie = uiState.movies.firstOrNull()?.first
            CinematicBackground(
                backdropUrl = uiState.collection?.backdropPath ?: uiState.collection?.posterPath ?: firstMovie?.posterPath,
                modifier = Modifier.fillMaxSize()
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Top Bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ModalBackButton(
                        onBack = { navigator.pop() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    
                    Text(
                        text = uiState.collection?.title ?: "Collection",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (uiState.error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Errore: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    // Sorting Toggle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .hazeGlass(
                                    state = hazeState,
                                    style = HazeStyles.PremiumDark,
                                    shape = CircleShape
                                )
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = uiState.isChronologicalOrder,
                                onClick = { if (!uiState.isChronologicalOrder) viewModel.toggleSortOrder() },
                                label = { Text("Cronologico") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = CircleShape,
                                border = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            FilterChip(
                                selected = !uiState.isChronologicalOrder,
                                onClick = { if (uiState.isChronologicalOrder) viewModel.toggleSortOrder() },
                                label = { Text("Data di Uscita") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = CircleShape,
                                border = null
                            )
                        }
                    }

                    // Timeline
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp)
                    ) {
                        itemsIndexed(uiState.movies, key = { _, item -> item.first.id }) { index, (movie, timelineIndex) ->
                            val isLast = index == uiState.movies.lastIndex
                            TimelineNode(
                                movie = movie,
                                indexLabel = if (uiState.isChronologicalOrder) "Fase $timelineIndex" else (movie.releaseDate?.take(4) ?: "N/A"),
                                isLast = isLast,
                                onClick = {
                                    navigator.push(MovieDetailScreen(movie.id, movie.mediaType))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineNode(
    movie: Movie,
    indexLabel: String,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Linea e Nodo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(60.dp)
        ) {
            // Spazio sopra il nodo
            Spacer(modifier = Modifier.height(32.dp))
            
            // Nodo Circolare
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            
            // Linea
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            }
        }
        
        // Contenuto Card
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp, top = 16.dp)
        ) {
            Text(
                text = indexLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            MovieListCard(
                movie = movie,
                onPress = { onClick() }
            )
        }
    }
}
