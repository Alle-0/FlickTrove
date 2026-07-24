package com.cinetrack.ui.components.dialog
import com.cinetrack.R

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.Episode
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.data.model.Season
import com.cinetrack.ui.viewmodel.DetailUiState
import com.cinetrack.ui.viewmodel.DetailEvent
import com.cinetrack.ui.theme.PrimaryTeal
import com.cinetrack.ui.viewmodel.MovieDetailViewModel
import com.cinetrack.ui.viewmodel.WatchState
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle

import kotlinx.coroutines.launch
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.unit.Velocity

import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.utils.verticalFadingEdges
import androidx.compose.foundation.lazy.rememberLazyListState

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
    val isDropped = successState?.watchState == WatchState.DROPPED
    
    var selectedSeasonNumber by remember { 
        mutableStateOf(
            movie.seasons
                ?.sortedBy { it.seasonNumber }
                ?.firstOrNull { season ->
                    val seasonNum = season.seasonNumber
                    if (seasonNum == 0) return@firstOrNull false
                    val watchedCount = movie.watchedEpisodes?.get(seasonNum.toString())?.size ?: 0
                    val totalCount = season.episodeCount ?: 0
                    totalCount > 0 && watchedCount < totalCount
                }?.seasonNumber
                ?: movie.seasons?.sortedBy { it.seasonNumber }?.firstOrNull { it.seasonNumber > 0 }?.seasonNumber
                ?: 1
        ) 
    }

    LaunchedEffect(selectedSeasonNumber) {
        viewModel.loadSeasonDetails(selectedSeasonNumber)
    }
    val density = LocalDensity.current
    val bottomPadding = WindowInsets.navigationBars.getBottom(density)

    var localWatchedEpisodes by remember(movie.watchedEpisodes) { 
        mutableStateOf(movie.watchedEpisodes ?: emptyMap()) 
    }
    
    var selectedEpisodeForInfo by remember { mutableStateOf<Episode?>(null) }

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

    val contextForWindow = LocalContext.current
    val view = LocalView.current
    DisposableEffect(view) {
        var parent = view.parent
        while (parent != null && parent !is DialogWindowProvider) {
            parent = parent.parent
        }
        val dialogWindow = (parent as? DialogWindowProvider)?.window
        val activityWindow = (contextForWindow as? Activity)?.window
        
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
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return Velocity(0f, available.y)
            }
        }
    }

    val context = LocalContext.current
    val config = LocalConfiguration.current

    if (selectedEpisodeForInfo != null) {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides config
        ) {
            EpisodeInfoModal(
                episode = selectedEpisodeForInfo!!,
                onDismiss = { selectedEpisodeForInfo = null }
            )
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
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides config
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

            val currentSeasonData = seasonDetails[selectedSeasonNumber]

            // Non-draggable content area
            Column(
                modifier = Modifier.blockBottomSheetVerticalDrag()
            ) {
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
                if (currentSeasonData != null) {
                    val todayIso = remember {
                        try { java.time.LocalDate.now().toString() } catch (e: Exception) { "2026-01-01" }
                    }
                    val releasedEps = currentSeasonData.episodes?.filter { ep ->
                        val epDate = ep.airDate
                        if (!epDate.isNullOrBlank()) epDate.take(10) <= todayIso
                        else if (!currentSeasonData.airDate.isNullOrBlank()) currentSeasonData.airDate.take(10) <= todayIso
                        else true
                    }?.map { it.episodeNumber } ?: emptyList<Int>()
                    val allEps = currentSeasonData.episodes?.map { it.episodeNumber } ?: emptyList<Int>()
                    val targetEps = if (releasedEps.isNotEmpty()) releasedEps else allEps

                    BulkAction(
                        isAllWatched = isSeasonFullyWatched(localWatchedEpisodes, currentSeasonNumber = selectedSeasonNumber, seasonData = currentSeasonData),
                        onToggle = { 
                            if (isDropped) return@BulkAction
                            val currentWatched = localWatchedEpisodes[selectedSeasonNumber.toString()] ?: emptyList()
                            val nextWatched = if (currentWatched.size >= targetEps.size) {
                                emptyList()
                            } else {
                                targetEps
                            }
                            localWatchedEpisodes = localWatchedEpisodes + (selectedSeasonNumber.toString() to nextWatched)
                        },
                        onLongClick = {
                            if (isDropped) return@BulkAction
                            localWatchedEpisodes = localWatchedEpisodes + (selectedSeasonNumber.toString() to targetEps)
                        }
                    )
                }

                // Instruction text
                Text(
                    text = stringResource(R.string.episodes_long_press_info),
                    modifier = Modifier.padding(horizontal = 32.dp).padding(bottom = 12.dp),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Episode List
            if (loadingSeason) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blockBottomSheetVerticalDrag(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00E676))
                }
            } else {
                val listState = rememberLazyListState()

                LaunchedEffect(currentSeasonData, selectedSeasonNumber) {
                    val episodes = currentSeasonData?.episodes
                    if (!episodes.isNullOrEmpty()) {
                        val watched = localWatchedEpisodes[selectedSeasonNumber.toString()] ?: emptyList()
                        val targetIndex = episodes.indexOfFirst { it.episodeNumber !in watched }
                        if (targetIndex >= 0) {
                            listState.animateScrollToItem(targetIndex)
                        } else {
                            listState.animateScrollToItem(0)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .nestedScroll(nestedScrollConnection)
                        .premiumScrollbar(listState)
                        .verticalFadingEdges(listState, 16.dp, 16.dp),
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
                                if (isDropped) return@EpisodeCard
                                val currentWatched = localWatchedEpisodes[selectedSeasonNumber.toString()]?.toMutableList() ?: mutableListOf()
                                if (currentWatched.contains(episode.episodeNumber)) {
                                    currentWatched.remove(episode.episodeNumber)
                                } else {
                                    currentWatched.add(episode.episodeNumber)
                                }
                                localWatchedEpisodes = localWatchedEpisodes + (selectedSeasonNumber.toString() to currentWatched)
                            },
                            onInfoClick = { selectedEpisodeForInfo = episode }
                        )
                    }
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
                stringResource(R.string.episodes_sheet_title),
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
                Icon(ImageVector.vectorResource(id = R.drawable.ic_x), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
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
    val lazyListState = rememberLazyListState()

    LaunchedEffect(selectedSeason, seasons) {
        val index = seasons.indexOf(selectedSeason)
        if (index >= 0) {
            lazyListState.animateScrollToItem(index)
        }
    }

    LazyRow(
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
            .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val fadePx = 16.dp.toPx()
                val width = size.width
                val ratio = if (width > 0) fadePx / width else 0f
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        ratio to Color.Black,
                        1f - ratio to Color.Black,
                        1f to Color.Transparent,
                        startX = 0f,
                        endX = width
                    ),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                )
            }
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
                    if (seasonNum == 0) stringResource(R.string.episodes_specials) else stringResource(R.string.episodes_season_n, seasonNum),
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
                Icon(ImageVector.vectorResource(id = R.drawable.ic_tick), contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                if (isAllWatched) stringResource(R.string.episodes_season_completed) else stringResource(R.string.episodes_mark_season_watched),
                color = if (isAllWatched) Color.Black else Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
private fun EpisodeCard(episode: Episode, isWatched: Boolean, onToggle: () -> Unit, onInfoClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(
                onLongClick = { onInfoClick() }
            ) { onToggle() },
        color = if (isWatched) Color(0xFF00E676).copy(alpha = 0.05f) else Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isWatched) Color(0xFF00E676).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = buildTmdbImageUrl(episode.stillPath, ImageType.BACKDROP, LocalImageQuality.current),
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
                    stringResource(R.string.episodes_episode_n, episode.episodeNumber),
                    color = if (isWatched) Color(0xFF00E676) else Color.White.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
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
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_tick), contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

private fun isSeasonFullyWatched(localWatchedEpisodes: Map<String, List<Int>>, currentSeasonNumber: Int, seasonData: Season): Boolean {
    val watched = localWatchedEpisodes[currentSeasonNumber.toString()] ?: emptyList()
    val todayIso = try { java.time.LocalDate.now().toString() } catch (e: Exception) { "2026-01-01" }
    val releasedCount = seasonData.episodes?.count { ep ->
        val epDate = ep.airDate
        if (!epDate.isNullOrBlank()) epDate.take(10) <= todayIso
        else if (!seasonData.airDate.isNullOrBlank()) seasonData.airDate.take(10) <= todayIso
        else true
    } ?: 0
    val total = if (releasedCount > 0) releasedCount else (seasonData.episodes?.size ?: 0)
    return total > 0 && watched.size >= total
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeInfoModal(episode: Episode, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A1C), // Sfondo scuro e monocolore
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Titolo e X
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${episode.seasonNumber}x${String.format("%02d", episode.episodeNumber)} - ${episode.name}",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f).padding(end = 16.dp),
                        lineHeight = 24.sp
                    )
                    
                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .bounceClick { onDismiss() },
                        color = Color.White.copy(alpha = 0.06f),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(ImageVector.vectorResource(id = R.drawable.ic_x), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Immagine (se presente)
                if (!episode.stillPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = buildTmdbImageUrl(episode.stillPath, ImageType.BACKDROP, LocalImageQuality.current),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Pillole (Data e Voto)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!episode.airDate.isNullOrEmpty()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DateRange,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = episode.airDate,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    if (episode.voteAverage != null && episode.voteAverage > 0.0) {
                        Surface(
                            color = Color(0xFF00E676).copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f", episode.voteAverage),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    }
                }

                // Trama
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .drawWithContent {
                            drawContent()
                            val showTop = scrollState.value > 0
                            val showBottom = scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue
                            
                            if (showTop) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF1A1A1C), Color.Transparent),
                                        startY = 0f,
                                        endY = 24.dp.toPx()
                                    )
                                )
                            }
                            if (showBottom) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xFF1A1A1C)),
                                        startY = size.height - 24.dp.toPx(),
                                        endY = size.height
                                    )
                                )
                            }
                        }
                ) {
                    if (!episode.overview.isNullOrEmpty()) {
                        Text(
                            text = episode.overview,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .verticalScroll(scrollState)
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.detail_no_overview),
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.blockBottomSheetVerticalDrag(): Modifier {
    val touchSlop = androidx.compose.ui.platform.LocalViewConfiguration.current.touchSlop
    return this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
            var verticalDragged = false
            var totalDy = 0f
            var totalDx = 0f
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed || !change.pressed) break
                val dy = change.position.y - change.previousPosition.y
                val dx = change.position.x - change.previousPosition.x
                totalDy += dy
                totalDx += dx
                if (!verticalDragged) {
                    if (kotlin.math.abs(totalDy) > touchSlop && kotlin.math.abs(totalDy) > kotlin.math.abs(totalDx)) {
                        verticalDragged = true
                    }
                }
                if (verticalDragged) {
                    change.consume()
                }
            }
        }
    }
}
