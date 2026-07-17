package com.cinetrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cinetrack.R
import com.cinetrack.data.api.Person
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.detail.PersonBioAndInfoSection
import com.cinetrack.ui.components.detail.PersonDetailSkeleton
import com.cinetrack.ui.components.detail.PersonFilmographyTabs
import com.cinetrack.ui.components.detail.PersonHeroHeader
import com.cinetrack.ui.components.detail.PersonHighlightsRow
import com.cinetrack.ui.components.detail.personFilmographyGrid
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.UiText
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.PersonDetailViewModel
import com.cinetrack.util.toComposeColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

data class PersonDetailScreen(
    val personId: Long,
    val profilePath: String? = null
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    override fun Content() {
        val viewModel = getViewModel<PersonDetailViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        val detailStackDepth = remember(navigator.items) {
            navigator.items.count { it is MovieDetailScreen || it is PersonDetailScreen }
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
    hazeState: HazeState? = null,
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
    val localHazeState = remember { HazeState() }

    var isBackPressed by remember { mutableStateOf(false) }
    val backIconScale by animateFloatAsState(
        targetValue = if (isBackPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = if (isBackPressed) 10000f else Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "BackIconScale"
    )

    val movieActions = LocalMovieActions.current

    BackHandler(enabled = true) {
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
        if (uiState.isLoading) {
            PersonDetailSkeleton(
                hazeState = localHazeState,
                paddingValues = paddingValues,
                cardWidth = cardWidth
            )
        } else {
            val error = uiState.error
            if (error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2E2E48),
                                    Color(0xFF1E1E32),
                                    Color(0xFF0A0A0A)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF6B48FF).copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(2.dp, Color(0xFF6B48FF).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_persona),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = Color(0xFF6B48FF).copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.person_offline_title),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.person_offline_desc),
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = { viewModel.retry() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B48FF)),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
                        ) {
                            Text(
                                stringResource(R.string.detail_retry),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
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
                            p.combinedCredits?.crew?.filter { it.mediaType == "movie" }
                                ?.distinctBy { it.id }
                                ?.sortedByDescending { it.releaseDate ?: "" }
                                .orEmpty()
                        }
                        val crewTV = remember(p.combinedCredits) {
                            p.combinedCredits?.crew?.filter { it.mediaType == "tv" }
                                ?.distinctBy { it.id }
                                ?.sortedByDescending { it.firstAirDate ?: "" }
                                .orEmpty()
                        }

                        val filmo = remember(uiState.activeTab, castMovies, castTV, crewMovies, crewTV) {
                            when (uiState.activeTab) {
                                "cast_movie" -> if (castMovies.isNotEmpty()) castMovies else crewMovies.ifEmpty { castTV.ifEmpty { crewTV } }
                                "cast_tv" -> if (castTV.isNotEmpty()) castTV else castMovies.ifEmpty { crewTV.ifEmpty { crewMovies } }
                                "crew_movie" -> if (crewMovies.isNotEmpty()) crewMovies else castMovies.ifEmpty { crewTV.ifEmpty { castTV } }
                                "crew_tv" -> if (crewTV.isNotEmpty()) crewTV else crewMovies.ifEmpty { castMovies.ifEmpty { castTV } }
                                else -> castMovies.ifEmpty { crewMovies.ifEmpty { castTV.ifEmpty { crewTV } } }
                            }
                        }
                        val filmoRows = remember(filmo, columns) {
                            filmo.chunked(columns)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(localHazeState, style = HazeStyles.PremiumDark),
                            contentPadding = paddingValues
                        ) {
                            item {
                                PersonHeroHeader(
                                    person = p,
                                    paddingValues = paddingValues
                                )
                            }

                            item {
                                Column(modifier = Modifier.offset(y = (-50).dp)) {
                                    PersonBioAndInfoSection(
                                        person = p,
                                        showFullBio = uiState.showFullBio,
                                        onToggleBio = { viewModel.toggleBio() }
                                    )

                                    val knownFor = remember(p.combinedCredits) {
                                        val cast = p.combinedCredits?.cast.orEmpty()
                                        val crew = p.combinedCredits?.crew.orEmpty()
                                        (cast + crew)
                                            .sortedByDescending { it.voteCount ?: 0 }
                                            .distinctBy { it.id.toString() + "_" + it.mediaType }
                                            .take(10)
                                    }

                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        PersonHighlightsRow(
                                            knownFor = knownFor,
                                            favoritesMap = favoritesMap,
                                            folderColorsMap = folderColorsMap,
                                            showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            onMovieClick = onMovieClick,
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            onLongPress = actionsState.onLongPress,
                                            onEmitMessage = { viewModel.emitMessage(UiText.DynamicString(it)) }
                                        )

                                        PersonFilmographyTabs(
                                            activeTab = uiState.activeTab,
                                            castMoviesCount = castMovies.size,
                                            castTVCount = castTV.size,
                                            crewMoviesCount = crewMovies.size,
                                            crewTVCount = crewTV.size,
                                            hazeState = localHazeState,
                                            onTabChanged = { viewModel.onTabChanged(it) }
                                        )
                                    }
                                }
                            }

                            personFilmographyGrid(
                                filmoRows = filmoRows,
                                columns = columns,
                                cardWidth = cardWidth,
                                padding = padding,
                                favoritesMap = favoritesMap,
                                folderColorsMap = folderColorsMap,
                                showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                showBadges = uiState.preferences.showBadges,
                                animatedMovieIds = viewModel.animatedMovieIds,
                                hazeState = localHazeState,
                                onMovieClick = onMovieClick,
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onLongPress = actionsState.onLongPress,
                                onEmitMessage = { viewModel.emitMessage(UiText.DynamicString(it)) }
                            )

                            item {
                                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 16.dp))
                            }
                        }
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
