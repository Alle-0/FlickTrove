package com.cinetrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.data.Movie
import com.cinetrack.data.models.Episode
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.data.models.Season
import com.cinetrack.ui.viewmodel.DetailUiState
import com.cinetrack.ui.viewmodel.DetailEvent
import com.cinetrack.ui.theme.PrimaryTeal
import com.cinetrack.ui.viewmodel.MovieDetailViewModel
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle

import kotlinx.coroutines.launch
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodesBottomSheet(
    movie: Movie,
    viewModel: MovieDetailViewModel,
    hazeState: HazeState? = null,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val successState = state as? DetailUiState.Success
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val seasonDetails = successState?.seasonDetails ?: emptyMap<Int, Season>()
    val loadingSeason = successState?.loadingSeason ?: false
    
    var selectedSeasonNumber by remember { 
        mutableStateOf(movie.watchedEpisodes?.keys?.firstOrNull()?.toInt() ?: 1) 
    }

    LaunchedEffect(selectedSeasonNumber) {
        viewModel.loadSeasonDetails(selectedSeasonNumber)
    }
    val density = LocalDensity.current
    val bottomPadding = WindowInsets.navigationBars.getBottom(density)

    var localWatchedEpisodes by remember(movie.watchedEpisodes) { 
        mutableStateOf(movie.watchedEpisodes ?: emptyMap()) 
    }

    val scope = rememberCoroutineScope()
    val dismissAndSync: () -> Unit = {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                viewModel.onEvent(DetailEvent.SyncWatchedEpisodes(localWatchedEpisodes))
                onDismiss()
            }
        }
    }

    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(view) {
        var parent = view.parent
        while (parent != null && parent !is DialogWindowProvider) {
            parent = parent.parent
        }
        val dialogWindow = (parent as? DialogWindowProvider)?.window
        val activityWindow = (context as? Activity)?.window
        
        val windows = listOfNotNull(dialogWindow, activityWindow)
        windows.forEach { window ->
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            // Do not restore system bars on the Activity window to keep the app immersive
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0f) {
                    return available
                }
                return Offset.Zero
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = dismissAndSync,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .hazeGlass(state = hazeState, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Custom Drag Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.24f))
                    )
                }

            // Header
            Header(movie, dismissAndSync)

            // Season Selector
            val seasons = successState?.details?.seasons?.mapNotNull { it.seasonNumber } ?: (1..(movie.numberOfSeasons ?: 1)).toList()
            val completedSeasons = seasons.filter { seasonNum ->
                val watchedCount = localWatchedEpisodes[seasonNum.toString()]?.size ?: 0
                val totalCount = successState?.details?.seasons?.find { it.seasonNumber == seasonNum }?.episodeCount ?: 0
                totalCount > 0 && watchedCount >= totalCount
            }.toSet()

            SeasonSelector(
                seasons = seasons,
                selectedSeason = selectedSeasonNumber,
                completedSeasons = completedSeasons,
                onSeasonSelected = { selectedSeasonNumber = it }
            )

            // Bulk Action
            val currentSeasonData = seasonDetails[selectedSeasonNumber]
            if (currentSeasonData != null) {
                val allEps = currentSeasonData.episodes?.map { it.episodeNumber } ?: emptyList<Int>()
                BulkAction(
                    isAllWatched = isSeasonFullyWatched(localWatchedEpisodes, currentSeasonNumber = selectedSeasonNumber, seasonData = currentSeasonData),
                    onToggle = { 
                        val currentWatched = localWatchedEpisodes[selectedSeasonNumber.toString()] ?: emptyList()
                        val nextWatched = if (currentWatched.size >= allEps.size) {
                            emptyList()
                        } else {
                            allEps
                        }
                        localWatchedEpisodes = localWatchedEpisodes + (selectedSeasonNumber.toString() to nextWatched)
                    },
                    onLongClick = {
                        localWatchedEpisodes = localWatchedEpisodes + (selectedSeasonNumber.toString() to allEps)
                    }
                )
            }

            // Episode List
            if (loadingSeason) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E676))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = currentSeasonData?.episodes ?: emptyList<Episode>(),
                        key = { it.episodeNumber },
                        contentType = { "episode" }
                    ) { episode ->
                        EpisodeCard(
                            episode = episode,
                            isWatched = (localWatchedEpisodes[selectedSeasonNumber.toString()] ?: emptyList<Int>()).contains(episode.episodeNumber),
                            onToggle = { 
                                val currentWatched = localWatchedEpisodes[selectedSeasonNumber.toString()]?.toMutableList() ?: mutableListOf()
                                if (currentWatched.contains(episode.episodeNumber)) {
                                    currentWatched.remove(episode.episodeNumber)
                                } else {
                                    currentWatched.add(episode.episodeNumber)
                                }
                                localWatchedEpisodes = localWatchedEpisodes + (selectedSeasonNumber.toString() to currentWatched)
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
private fun Header(movie: Movie, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "GESTIONE EPISODI",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                movie.name ?: movie.title ?: "",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            modifier = Modifier
                .size(48.dp)
                .bounceClick { onDismiss() },
            color = Color.White.copy(alpha = 0.06f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun SeasonSelector(
    seasons: List<Int>,
    selectedSeason: Int,
    completedSeasons: Set<Int>,
    onSeasonSelected: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .padding(bottom = 16.dp)
    ) {
        items(seasons, key = { it }, contentType = { "season_chip" }) { seasonNum ->
            val isActive = selectedSeason == seasonNum
            val isCompleted = completedSeasons.contains(seasonNum)
            
            val backgroundColor = when {
                isCompleted && isActive -> Color(0xFF00E676)
                isCompleted -> Color(0xFF00E676).copy(alpha = 0.15f)
                isActive -> Color.White.copy(alpha = 0.48f)
                else -> Color.White.copy(alpha = 0.08f)
            }
            
            val textColor = when {
                isActive -> Color.Black
                isCompleted -> Color(0xFF00E676)
                else -> Color.White.copy(alpha = 0.6f)
            }
            
            val borderColor = when {
                isCompleted -> Color(0xFF00E676).copy(alpha = 0.8f)
                else -> Color.White.copy(alpha = 0.1f)
            }

            Surface(
                modifier = Modifier.bounceClick { onSeasonSelected(seasonNum) },
                color = backgroundColor,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Text(
                    if (seasonNum == 0) "SPECIALI" else "STAGIONE $seasonNum",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = textColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
private fun BulkAction(
    isAllWatched: Boolean, 
    onToggle: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 28.dp, vertical = 8.dp)
            .bounceClick(
                onLongClick = onLongClick,
                onClick = onToggle
            ),
        color = if (isAllWatched) Color(0xFF00E676) else Color.White.copy(alpha = 0.08f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAllWatched) Color(0xFF00E676) else Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAllWatched) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                if (isAllWatched) "STAGIONE COMPLETATA" else "SEGNA STAGIONE COME VISTA",
                color = if (isAllWatched) Color.Black else Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
private fun EpisodeCard(episode: Episode, isWatched: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onToggle() },
        color = if (isWatched) Color(0xFF00E676).copy(alpha = 0.05f) else Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isWatched) Color(0xFF00E676).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w300${episode.stillPath}",
                contentDescription = null,
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentScale = ContentScale.Crop,
                alpha = if (isWatched) 0.6f else 1f
            )
            
            Column(modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)) {
                Text(
                    "EPISODIO ${episode.episodeNumber}",
                    color = if (isWatched) Color(0xFF00E676) else Color.White.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    episode.name,
                    color = if (isWatched) Color.White.copy(alpha = 0.6f) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                modifier = Modifier.size(28.dp),
                color = if (isWatched) Color(0xFF00E676) else Color.Transparent,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isWatched) Color(0xFF00E676) else Color.White.copy(alpha = 0.15f))
            ) {
                if (isWatched) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

private fun isSeasonFullyWatched(localWatchedEpisodes: Map<String, List<Int>>, currentSeasonNumber: Int, seasonData: Season): Boolean {
    val watched = localWatchedEpisodes[currentSeasonNumber.toString()] ?: emptyList()
    val total = seasonData.episodes?.size ?: 0
    return total > 0 && watched.size >= total
}
