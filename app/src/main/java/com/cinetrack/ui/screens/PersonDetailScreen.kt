package com.cinetrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cinetrack.data.Movie
import com.cinetrack.ui.viewmodel.PersonDetailViewModel
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.components.shared.MovieActionsState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.theme.PrimaryTeal
import com.cinetrack.ui.components.MovieCard
import com.cinetrack.util.toComposeColor
import com.cinetrack.ui.components.CategoryPill
import com.cinetrack.ui.components.detail.PersonDetailSkeleton
import com.cinetrack.ui.viewmodel.PersonDetailUiState
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.ui.theme.PremiumBackground
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.components.glass.hazeGlass

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PersonDetailScreen(
    viewModel: PersonDetailViewModel,
    paddingValues: PaddingValues,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    onBackClick: () -> Unit = {},
    onMovieClick: (Movie) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val padding = 16.dp
    val gap = 12.dp
    val cardWidth = (screenWidth - (padding * 2) - (gap * 2)) / 3
    val localHazeState = remember { dev.chrisbanes.haze.HazeState() }

    
    // Press state for back button to scale only icon
    var isBackPressed by remember { mutableStateOf(false) }
    val backIconScale by animateFloatAsState(
        targetValue = if (isBackPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = if (isBackPressed) 10000f else Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "BackIconScale"
    )

    androidx.activity.compose.BackHandler(enabled = true) {
        onBackClick()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumBackground)
            .background(Color(0xFF0A0A0A))

    ) {
        // Removed CinematicBackground

        if (uiState.isLoading) {
            PersonDetailSkeleton(
                hazeState = localHazeState,
                paddingValues = paddingValues,
                cardWidth = cardWidth
            )
        } else {
            val error = uiState.error
            if (error != null) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.retry() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("RIPROVA", color = Color.Black, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            } else {
                uiState.person?.let { p ->
                MovieActionsWrapper(
                    hazeState = localHazeState,
                    folders = uiState.folders,
                    isItemInFolder = { movie, folderId ->
                        uiState.folders.find { it.id == folderId }?.itemIds?.contains("${movie.mediaType}_${movie.id}") ?: false
                    },
                    onDelete = { /* Person detail usually doesn't delete */ },
                    onUpdateRating = { movie, rating -> viewModel.updateRating(movie, rating) },
                    onUpdateNote = { movie, note -> viewModel.updateNote(movie, note) },
                    onToggleFolder = { movie, folder -> viewModel.toggleItemInFolder(folder, movie) }
                ) { actionsState ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .haze(localHazeState, style = HazeStyles.PremiumDark)
                    ) {
                    // Hero Profile Image
                    Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                        AsyncImage(
                            model = "https://image.tmdb.org/t/p/original/${p.profilePath}",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Top Gradient - Unified with MovieDetail
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp + paddingValues.calculateTopPadding())
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                                    )
                                )
                        )

                        Box(modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(
                                Color.Transparent, 
                                Color.Transparent, 
                                Color(0xFF0A0A0A).copy(alpha = 0.55f),
                                Color(0xFF0A0A0A)
                            ))
                        ))
                    }

                    Column(modifier = Modifier.offset(y = (-50).dp)) {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Text(
                                text = p.name,
                                color = Color.White,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 46.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = p.knownForDepartment ?: "",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            p.biography?.let { bio ->
                                Text(
                                    text = bio,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp,
                                    lineHeight = 26.sp,
                                    maxLines = if (uiState.showFullBio) Int.MAX_VALUE else 4
                                )
                                if (bio.length > 200) {
                                    Text(
                                        text = if (uiState.showFullBio) "LEGGI MENO" else "LEGGI TUTTO",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(top = 16.dp).clickable { viewModel.toggleBio() }
                                    )
                                }
                            } ?: Text(
                                text = "Nessuna biografia disponibile.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                lineHeight = 26.sp
                            )

                            Spacer(modifier = Modifier.height(30.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    PersonInfoItem(Icons.Rounded.Star, "DATA DI NASCITA", p.birthday ?: "N/D")
                                    PersonInfoItem(Icons.Rounded.Place, "LUOGO DI NASCITA", p.placeOfBirth ?: "N/D")
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    PersonInfoItem(Icons.Rounded.Info, "CONOSCIUTO PER", p.knownForDepartment ?: "N/D")
                                    PersonInfoItem(Icons.Rounded.Star, "POPOLARITÀ", p.popularity?.let { "%.1f".format(it) } ?: "N/D")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            val knownFor = remember(p.combinedCredits) {
                                val cast = p.combinedCredits?.cast.orEmpty()
                                val crew = p.combinedCredits?.crew.orEmpty()
                                (cast + crew)
                                    .sortedByDescending { it.voteCount ?: 0 }
                                    .distinctBy { it.id.toString() + "_" + it.mediaType }
                                    .take(10)
                            }
                            if (knownFor.isNotEmpty()) {
                                Text("Punti Forti", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(16.dp))
                                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    itemsIndexed(
                                        items = knownFor,
                                        key = { _, it -> it.id.toString() + "_" + it.mediaType }
                                    ) { index, m ->
                                        val movieStatus = uiState.favorites.find { it.id == m.id && it.mediaType == m.mediaType }
                                        MovieCard(
                                            movie = m,
                                            cardWidth = cardWidth,
                                            isFavorite = movieStatus?.favorite ?: false,
                                            isWatched = movieStatus?.watched ?: false,
                                            isReminder = movieStatus?.reminder ?: false,
                                            progress = movieStatus?.progress?.toFloat() ?: 0f,
                                            folderColors = uiState.movieFolderColors["${m.mediaType}_${m.id}"]?.map {
                                                it.toComposeColor()
                                            } ?: emptyList(),
                                            showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            staggerIndex = index,
                                            onPress = { onMovieClick(m) },
                                            onAction = { viewModel.toggleFavorite(m) },
                                            onLongPress = actionsState.onLongPress,
                                            onMessage = { viewModel.emitMessage(it) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(40.dp))
                            }

                            Text("Filmografia", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)

                            val castMovies = remember(p.combinedCredits) {
                                p.combinedCredits?.cast?.filter { it.mediaType == "movie" }
                                    ?.distinctBy { it.id }
                                    ?.sortedByDescending { it.releaseDate ?: "" }
                                    .orEmpty()
                            }
                            val castTV = remember(p.combinedCredits) {
                                p.combinedCredits?.cast?.filter { it.mediaType == "tv" }
                                    ?.distinctBy { it.id }
                                    ?.sortedByDescending { it.firstAirDate ?: "" }
                                    .orEmpty()
                            }
                            val crewMovies = remember(p.combinedCredits) {
                                p.combinedCredits?.crew?.filter { it.job == "Director" && it.mediaType == "movie" }
                                    ?.distinctBy { it.id }
                                    ?.sortedByDescending { it.releaseDate ?: "" }
                                    .orEmpty()
                            }
                            val crewTV = remember(p.combinedCredits) {
                                p.combinedCredits?.crew?.filter { it.job == "Director" && it.mediaType == "tv" }
                                    ?.distinctBy { it.id }
                                    ?.sortedByDescending { it.firstAirDate ?: "" }
                                    .orEmpty()
                            }

                            Row(
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (castMovies.isNotEmpty()) CategoryPill("Cast - Film (${castMovies.size})", uiState.activeTab == "cast_movie", hazeState = localHazeState) { viewModel.onTabChanged("cast_movie") }
                                if (castTV.isNotEmpty()) CategoryPill("Cast - Serie (${castTV.size})", uiState.activeTab == "cast_tv", hazeState = localHazeState) { viewModel.onTabChanged("cast_tv") }
                                if (crewMovies.isNotEmpty()) CategoryPill("Regia - Film (${crewMovies.size})", uiState.activeTab == "crew_movie", hazeState = localHazeState) { viewModel.onTabChanged("crew_movie") }
                                if (crewTV.isNotEmpty()) CategoryPill("Regia - Serie (${crewTV.size})", uiState.activeTab == "crew_tv", hazeState = localHazeState) { viewModel.onTabChanged("crew_tv") }
                            }

                            val filmo = when (uiState.activeTab) {
                                "cast_movie" -> castMovies
                                "cast_tv" -> castTV
                                "crew_movie" -> crewMovies
                                "crew_tv" -> crewTV
                                else -> castMovies
                            }

                            filmo.chunked(3).forEachIndexed { rowIndex, rowMovies ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowMovies.forEachIndexed { colIndex, movie ->
                                        val movieStatus = uiState.favorites.find { it.id == movie.id && it.mediaType == movie.mediaType }
                                        val staggerIdx = rowIndex * 3 + colIndex
                                        Box(modifier = Modifier.weight(1f)) {
                                            MovieCard(
                                                movie = movie,
                                                cardWidth = cardWidth,
                                                isFavorite = movieStatus?.favorite ?: false,
                                                isWatched = movieStatus?.watched ?: false,
                                                isReminder = movieStatus?.reminder ?: false,
                                                progress = movieStatus?.progress?.toFloat() ?: 0f,
                                                folderColors = uiState.movieFolderColors["${movie.mediaType}_${movie.id}"]?.map {
                                                    it.toComposeColor()
                                                } ?: emptyList(),
                                                showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                                animatedVisibilityScope = null,
                                                staggerIndex = staggerIdx,
                                                onPress = { onMovieClick(movie) },
                                                onAction = { viewModel.toggleFavorite(movie) },
                                                onLongPress = actionsState.onLongPress,
                                                onMessage = { viewModel.emitMessage(it) }
                                            )
                                        }
                                    }
                                    repeat(3 - rowMovies.size) { Spacer(modifier = Modifier.weight(1f)) }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 16.dp))
                    }
                }
            }
            }
        }
    }

    // Floating Back Button (Top Left)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = 8.dp, start = 16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .hazeGlass(
                        state = localHazeState,
                        shape = CircleShape,
                        blurRadius = HazeStyles.SmallGlassBlurRadius,
                        useOffscreenStrategy = true
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isBackPressed = true
                                try { awaitRelease() } finally { isBackPressed = false }
                            },
                            onTap = { onBackClick() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = backIconScale
                            scaleY = backIconScale
                        }
                )
            }
        }
    }
}

@Composable
fun PersonInfoItem(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp)) }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
