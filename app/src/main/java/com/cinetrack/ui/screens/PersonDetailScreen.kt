package com.cinetrack.ui.screens

import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import com.cinetrack.ui.components.CinematicBackground
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.components.glass.hazeGlass

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class PersonDetailScreen(
    val personId: Long,
    val profilePath: String?
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    override fun Content() {
        val viewModel = getViewModel<PersonDetailViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        val detailStackDepth = androidx.compose.runtime.remember(navigator.items) {
            navigator.items.count { it is com.cinetrack.ui.screens.MovieDetailScreen || it is PersonDetailScreen }
        }

        LaunchedEffect(personId) {
            viewModel.initPerson(personId, profilePath)
        }

        PersonDetailScreenContent(
            viewModel = viewModel,
            paddingValues = PaddingValues(0.dp),
            onBackClick = { navigator.pop() },
            onMovieClick = { movie ->
                navigator.push(MovieDetailScreen(movie.id, movie.mediaType))
            },
            detailStackDepth = detailStackDepth,
            onHomeClick = {
                navigator.popUntilRoot()
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PersonDetailScreenContent(
    viewModel: PersonDetailViewModel,
    paddingValues: PaddingValues,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    onBackClick: () -> Unit = {},
    onMovieClick: (Movie) -> Unit,
    detailStackDepth: Int = 1,
    onHomeClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val padding = 16.dp
    val gap = 12.dp
    val columns = if (uiState.preferences.gridColumns in 1..4) uiState.preferences.gridColumns else 3
    val cardWidth = if (columns > 1) {
        (screenWidth - (padding * 2) - (gap * (columns - 1))) / columns
    } else {
        screenWidth - (padding * 2)
    }
    val localHazeState = remember { dev.chrisbanes.haze.HazeState() }

    
    // Press state for back button to scale only icon
    var isBackPressed by remember { mutableStateOf(false) }
    val backIconScale by animateFloatAsState(
        targetValue = if (isBackPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = if (isBackPressed) 10000f else Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "BackIconScale"
    )

    val movieActions = com.cinetrack.ui.components.shared.LocalMovieActions.current

    androidx.activity.compose.BackHandler(enabled = true) {
        if (movieActions.isAnyModalOpen) {
            movieActions.closeAll()
        } else {
            onBackClick()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                            Text(stringResource(R.string.detail_retry), color = Color.Black, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            } else {
                uiState.person?.let { p ->
                val favoritesMap = remember(uiState.favorites) {
                    uiState.favorites.associateBy { "${it.mediaType}_${it.id}" }
                }
                val folderColorsMap = remember(uiState.movieFolderColors) {
                    uiState.movieFolderColors.mapValues { entry ->
                        entry.value.map { it.toComposeColor() }
                    }
                }
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

                    val filmo = remember(uiState.activeTab, castMovies, castTV, crewMovies, crewTV) {
                        when (uiState.activeTab) {
                            "cast_movie" -> castMovies
                            "cast_tv" -> castTV
                            "crew_movie" -> crewMovies
                            "crew_tv" -> crewTV
                            else -> castMovies
                        }
                    }
                    val filmoRows = remember(filmo, columns) {
                        filmo.chunked(columns)
                    }

                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .haze(localHazeState, style = HazeStyles.PremiumDark),
                        contentPadding = paddingValues
                    ) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                                AsyncImage(
                                    model = buildTmdbImageUrl(p.profilePath, ImageType.PROFILE, LocalImageQuality.current),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

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

                                Box(
                                    modifier = Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color(0xFF0A0A0A).copy(alpha = 0.55f),
                                                Color(0xFF0A0A0A)
                                            )
                                        )
                                    )
                                )
                            }
                        }

                        item {
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
                                        Column(
                                            modifier = Modifier
                                                .bounceClick(scaleDown = 0.99f) { viewModel.toggleBio() }
                                        ) {
                                            androidx.compose.animation.AnimatedContent(
                                                targetState = uiState.showFullBio,
                                                transitionSpec = {
                                                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(350)) togetherWith
                                                            androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(350)) using
                                                            androidx.compose.animation.SizeTransform(clip = true) { _, _ ->
                                                                androidx.compose.animation.core.spring(
                                                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                                                )
                                                            }
                                                },
                                                label = "BioExpansion"
                                            ) { targetExpanded ->
                                                Text(
                                                    text = bio,
                                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                                        lineHeight = 20.sp,
                                                        letterSpacing = 0.2.sp,
                                                        fontSize = 14.sp
                                                    ),
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    maxLines = if (targetExpanded) Int.MAX_VALUE else 4,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                            if (bio.length > 200) {
                                                Text(
                                                    text = if (uiState.showFullBio) stringResource(R.string.person_read_less) else stringResource(R.string.person_read_more),
                                                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color.White,
                                                    modifier = Modifier.padding(top = 12.dp)
                                                )
                                            }
                                        }
                                    } ?: Text(
                                        text = stringResource(R.string.person_no_bio),
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 20.sp,
                                            letterSpacing = 0.2.sp,
                                            fontSize = 14.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(30.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            PersonInfoItem(ImageVector.vectorResource(id = R.drawable.ic_star), stringResource(R.string.person_birthday), p.birthday ?: stringResource(R.string.person_nd))
                                            PersonInfoItem(ImageVector.vectorResource(id = R.drawable.ic_partenone), stringResource(R.string.person_birth_place), p.placeOfBirth ?: stringResource(R.string.person_nd))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            PersonInfoItem(ImageVector.vectorResource(id = R.drawable.ic_documento), stringResource(R.string.person_known_for), p.knownForDepartment ?: stringResource(R.string.person_nd))
                                            PersonInfoItem(ImageVector.vectorResource(id = R.drawable.ic_star), stringResource(R.string.person_popularity), p.popularity?.let { "%.1f".format(it) } ?: stringResource(R.string.person_nd))
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
                                        Text(stringResource(R.string.person_highlights), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            itemsIndexed(
                                                items = knownFor,
                                                key = { _, it -> it.id.toString() + "_" + it.mediaType }
                                            ) { index, m ->
                                                val movieStatus = favoritesMap["${m.mediaType}_${m.id}"]
                                                val movie = movieStatus ?: m
                                                val folderColors = folderColorsMap["${movie.mediaType}_${movie.id}"] ?: emptyList()
                                                MovieCard(
                                                    movie = movie,
                                                    cardWidth = 130.dp,
                                                    isFavorite = movieStatus?.favorite ?: false,
                                                    isWatched = movieStatus?.watched ?: false,
                                                    isReminder = movieStatus?.reminder ?: false,
                                                    progress = movieStatus?.progress?.toFloat() ?: 0f,
                                                    personalRating = movieStatus?.personalRating,
                                                    folderColors = folderColors,
                                                    showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                                    animatedVisibilityScope = animatedVisibilityScope,
                                                    staggerIndex = index,
                                                    onPress = { onMovieClick(m) },
                                                    onAction = { viewModel.toggleFavorite(m) },
                                                    onLongPress = actionsState.onLongPress,
                                                            onMessage = { viewModel.emitMessage(com.cinetrack.ui.utils.UiText.DynamicString(it)) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(40.dp))
                                    }

                                    Text(stringResource(R.string.person_filmography), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)

                                    Row(
                                        modifier = Modifier
                                            .padding(vertical = 16.dp)
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (castMovies.isNotEmpty()) CategoryPill(stringResource(R.string.person_cast_movie_format, castMovies.size), uiState.activeTab == "cast_movie", hazeState = localHazeState) { viewModel.onTabChanged("cast_movie") }
                                        if (castTV.isNotEmpty()) CategoryPill(stringResource(R.string.person_cast_tv_format, castTV.size), uiState.activeTab == "cast_tv", hazeState = localHazeState) { viewModel.onTabChanged("cast_tv") }
                                        if (crewMovies.isNotEmpty()) CategoryPill(stringResource(R.string.person_crew_movie_format, crewMovies.size), uiState.activeTab == "crew_movie", hazeState = localHazeState) { viewModel.onTabChanged("crew_movie") }
                                        if (crewTV.isNotEmpty()) CategoryPill(stringResource(R.string.person_crew_tv_format, crewTV.size), uiState.activeTab == "crew_tv", hazeState = localHazeState) { viewModel.onTabChanged("crew_tv") }
                                    }
                                }
                            }
                        }

                        items(filmoRows) { rowMovies ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = padding, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowMovies.forEachIndexed { colIndex, movie ->
                                val movieStatus = favoritesMap["${movie.mediaType}_${movie.id}"]
                                val m = movieStatus ?: movie
                                val staggerIdx = colIndex
                                val folderColors = folderColorsMap["${m.mediaType}_${m.id}"] ?: emptyList()
                                Box(modifier = Modifier.weight(1f)) {
                                    if (columns == 1) {
                                        com.cinetrack.ui.components.MovieListCard(
                                            movie = m,
                                            modifier = Modifier.fillMaxWidth(),
                                            isFavorite = movieStatus?.favorite ?: false,
                                            isWatched = movieStatus?.watched ?: false,
                                            isReminder = movieStatus?.reminder ?: false,
                                            progress = movieStatus?.progress?.toFloat() ?: 0f,
                                            personalRating = movieStatus?.personalRating,
                                            folderColors = folderColors,
                                            showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                            showBadges = uiState.preferences.showBadges,
                                            hasAnimatedSet = viewModel.animatedMovieIds,
                                            hazeState = localHazeState,
                                            staggerIndex = staggerIdx,
                                            onPress = { onMovieClick(movie) },
                                            onAction = { viewModel.toggleFavorite(movie) },
                                            onLongPress = actionsState.onLongPress,
                                                    onMessage = { viewModel.emitMessage(com.cinetrack.ui.utils.UiText.DynamicString(it)) }
                                        )
                                    } else {
                                        MovieCard(
                                            movie = m,
                                            cardWidth = cardWidth,
                                            isFavorite = movieStatus?.favorite ?: false,
                                            isWatched = movieStatus?.watched ?: false,
                                            isReminder = movieStatus?.reminder ?: false,
                                            progress = movieStatus?.progress?.toFloat() ?: 0f,
                                            personalRating = movieStatus?.personalRating,
                                            folderColors = folderColors,
                                            showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                            hasAnimatedSet = viewModel.animatedMovieIds,
                                            animatedVisibilityScope = null,
                                            staggerIndex = staggerIdx,
                                            onPress = { onMovieClick(movie) },
                                            onAction = { viewModel.toggleFavorite(movie) },
                                            onLongPress = actionsState.onLongPress,
                                                    onMessage = { viewModel.emitMessage(com.cinetrack.ui.utils.UiText.DynamicString(it)) }
                                        )
                                    }
                                }
                            }
                            repeat(columns - rowMovies.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 16.dp))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = 8.dp, start = 16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        .bounceClick { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_left),
                        contentDescription = stringResource(R.string.detail_content_desc_back),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                val homeButtonVisible = detailStackDepth >= 3
                val homeButtonAlpha by animateFloatAsState(
                    targetValue = if (homeButtonVisible) 1f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "HomeButtonAlpha"
                )
                val homeButtonScale by animateFloatAsState(
                    targetValue = if (homeButtonVisible) 1f else 0.6f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "HomeButtonScale"
                )
                if (homeButtonAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                alpha = homeButtonAlpha
                                scaleX = homeButtonScale
                                scaleY = homeButtonScale
                            }
                            .hazeGlass(
                                state = localHazeState,
                                shape = CircleShape,
                                blurRadius = HazeStyles.SmallGlassBlurRadius,
                                useOffscreenStrategy = true
                            )
                            .bounceClick { onHomeClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_home),
                            contentDescription = stringResource(R.string.detail_content_desc_home),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
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
