package com.cinetrack.ui.screens

import coil3.toBitmap

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.cinetrack.ui.components.detail.PersonMorphingTopBar
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.imageLoader
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cinetrack.R
import com.cinetrack.ui.components.home.HomeFilterModal
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.model.SortConfig
import androidx.compose.ui.geometry.Rect
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
import coil3.imageLoader
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

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
    val scope = rememberCoroutineScope()
    val currentImageQuality = LocalImageQuality.current

    var filterButtonBounds by remember { mutableStateOf<Rect?>(null) }
    val filterModalScrimAlpha by animateFloatAsState(
        targetValue = if (uiState.isFilterModalOpen) 0.6f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "topBarScrimAlpha"
    )

    val scrollState = rememberLazyListState()
    val density = LocalDensity.current
    val scrollThreshold = with(density) { 470.dp.toPx() }
    val scrollProgress by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 0) 1f
            else (scrollState.firstVisibleItemScrollOffset.toFloat() / scrollThreshold).coerceIn(0f, 1f)
        }
    }
    
    val isScrolling = scrollState.isScrollInProgress
    val isTransitioning = animatedVisibilityScope?.transition?.let { it.currentState != it.targetState } ?: false
    val isMerged = scrollProgress >= 0.75f && !isTransitioning
    
    val targetSymbioteProgress = when {
        isTransitioning -> 0f
        isMerged -> 1f
        isScrolling -> scrollProgress
        else -> 0f
    }
    
    val symbioteProgress by animateFloatAsState(
        targetValue = targetSymbioteProgress,
        animationSpec = if (isScrolling || isMerged) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "symbioteProgress"
    )

    val movieActions = LocalMovieActions.current

    BackHandler(enabled = true) {
        if (uiState.isFilterModalOpen) {
            viewModel.dismissFilterModal()
        } else if (movieActions.isAnyModalOpen) {
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
                        onDelete = { viewModel.deleteMovie(it) },
                        onUpdateRating = { movie, rating -> viewModel.updateRating(movie, rating) },
                        onUpdateNote = { movie, note -> viewModel.updateNote(movie, note) },
                        onToggleFolder = { movie, folder -> viewModel.toggleItemInFolder(folder, movie) }
                    ) { actionsState ->
                        val castMovies = remember(p.combinedCredits) {
                            p.combinedCredits?.cast?.filter { it.mediaType == "movie" }
                                ?.distinctBy { it.id }
                                .orEmpty()
                        }
                        val castTV = remember(p.combinedCredits) {
                            p.combinedCredits?.cast?.filter { it.mediaType == "tv" }
                                ?.distinctBy { it.id }
                                .orEmpty()
                        }
                        val allCrew = remember(p.combinedCredits) { p.combinedCredits?.crew.orEmpty() }
                        val mainDept = remember(allCrew, p.knownForDepartment) {
                            p.knownForDepartment?.takeIf { it != "Acting" }
                                ?: if (allCrew.any { it.department == "Directing" || it.job == "Director" }) "Directing"
                                else allCrew.groupingBy { it.department }.eachCount().maxByOrNull { it.value }?.key
                        }

                        val deptMovies = remember(allCrew, mainDept) {
                            if (mainDept == null) emptyList()
                            else allCrew.filter { it.mediaType == "movie" && (it.department == mainDept || (mainDept == "Directing" && it.job == "Director")) }
                                .distinctBy { it.id }
                        }
                        val deptTV = remember(allCrew, mainDept) {
                            if (mainDept == null) emptyList()
                            else allCrew.filter { it.mediaType == "tv" && (it.department == mainDept || (mainDept == "Directing" && it.job == "Director")) }
                                .distinctBy { it.id }
                        }
                        val crewMovies = remember(allCrew, mainDept) {
                            allCrew.filter { it.mediaType == "movie" && (mainDept == null || !(it.department == mainDept || (mainDept == "Directing" && it.job == "Director"))) }
                                .distinctBy { it.id }
                        }
                        val crewTV = remember(allCrew, mainDept) {
                            allCrew.filter { it.mediaType == "tv" && (mainDept == null || !(it.department == mainDept || (mainDept == "Directing" && it.job == "Director"))) }
                                .distinctBy { it.id }
                        }

                        fun filterAndSortCredits(list: List<Movie>, sort: SortConfig): List<Movie> {
                            val filtered = list.filter { movie ->
                                val isTv = movie.mediaType == "tv"
                                val genreMatch = sort.selectedGenres.isEmpty() || sort.selectedGenres.any { g ->
                                    val targets = GenreConstants.getEffectiveGenreIds(g, isTv)
                                    movie.genreIds?.any { it in targets } == true
                                }
                                val countryMatch = sort.selectedCountries.isEmpty() ||
                                    (movie.originCountry?.any { it.uppercase() in sort.selectedCountries } == true)
                                val decadeMatch = sort.selectedDecades.isEmpty() || sort.selectedDecades.any { decade ->
                                    val prefix = decade.take(3)
                                    (movie.releaseDate?.startsWith(prefix) == true) || (movie.firstAirDate?.startsWith(prefix) == true)
                                }
                                val providerMatch = sort.selectedProviders.isEmpty() ||
                                    movie.streamingProviderIds?.any { p -> p in sort.selectedProviders } == true
                                genreMatch && countryMatch && decadeMatch && providerMatch
                            }
                            val isDesc = sort.sortDirection == "desc"
                            return when (sort.sortType) {
                                "release_date" -> {
                                    if (isDesc) {
                                        filtered.sortedWith(compareByDescending<Movie> { it.releaseDate ?: it.firstAirDate ?: "" }.thenBy { it.title ?: it.name ?: "" })
                                    } else {
                                        filtered.sortedWith(compareBy<Movie> { (it.releaseDate ?: it.firstAirDate)?.takeIf { d -> d.isNotBlank() } ?: "9999-99-99" }.thenBy { it.title ?: it.name ?: "" })
                                    }
                                }
                                "vote_average" -> {
                                    if (isDesc) {
                                        filtered.sortedWith(compareByDescending<Movie> { it.voteAverage ?: 0.0 }.thenByDescending { it.voteCount ?: 0 })
                                    } else {
                                        filtered.sortedWith(compareBy<Movie> { it.voteAverage ?: 0.0 }.thenBy { it.voteCount ?: 0 })
                                    }
                                }
                                "title" -> {
                                    if (isDesc) {
                                        filtered.sortedWith(compareByDescending<Movie> { it.title ?: it.name ?: "" })
                                    } else {
                                        filtered.sortedWith(compareBy<Movie> { it.title ?: it.name ?: "" })
                                    }
                                }
                                else -> {
                                    if (isDesc) {
                                        filtered.sortedWith(compareByDescending<Movie> { it.releaseDate ?: it.firstAirDate ?: "" })
                                    } else {
                                        filtered.sortedWith(compareBy<Movie> { (it.releaseDate ?: it.firstAirDate)?.takeIf { d -> d.isNotBlank() } ?: "9999-99-99" })
                                    }
                                }
                            }
                        }

                        val filteredCastMovies = remember(castMovies, uiState.sortConfig) { filterAndSortCredits(castMovies, uiState.sortConfig) }
                        val filteredCastTV = remember(castTV, uiState.sortConfig) { filterAndSortCredits(castTV, uiState.sortConfig) }
                        val filteredDeptMovies = remember(deptMovies, uiState.sortConfig) { filterAndSortCredits(deptMovies, uiState.sortConfig) }
                        val filteredDeptTV = remember(deptTV, uiState.sortConfig) { filterAndSortCredits(deptTV, uiState.sortConfig) }
                        val filteredCrewMovies = remember(crewMovies, uiState.sortConfig) { filterAndSortCredits(crewMovies, uiState.sortConfig) }
                        val filteredCrewTV = remember(crewTV, uiState.sortConfig) { filterAndSortCredits(crewTV, uiState.sortConfig) }

                        val filmo = remember(uiState.activeTab, filteredCastMovies, filteredCastTV, filteredDeptMovies, filteredDeptTV, filteredCrewMovies, filteredCrewTV) {
                            when (uiState.activeTab) {
                                "cast_movie" -> filteredCastMovies
                                "cast_tv" -> filteredCastTV
                                "dept_movie" -> filteredDeptMovies
                                "dept_tv" -> filteredDeptTV
                                "crew_movie" -> filteredCrewMovies
                                "crew_tv" -> filteredCrewTV
                                else -> filteredDeptMovies.ifEmpty { filteredCastMovies.ifEmpty { filteredCrewMovies.ifEmpty { filteredDeptTV.ifEmpty { filteredCastTV.ifEmpty { filteredCrewTV } } } } }
                            }
                        }
                        val filmoRows = remember(filmo, columns) {
                            filmo.chunked(columns)
                        }

                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(localHazeState, style = HazeStyles.PremiumDark)
                                .background(Color(0xFF0A0A0A)),
                            contentPadding = paddingValues
                        ) {
                            item {
                                PersonHeroHeader(
                                    person = p,
                                    paddingValues = paddingValues,
                                    scrollState = scrollState
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

                                    Column {
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
                                            castMoviesCount = filteredCastMovies.size,
                                            castTVCount = filteredCastTV.size,
                                            deptMoviesCount = filteredDeptMovies.size,
                                            deptTVCount = filteredDeptTV.size,
                                            crewMoviesCount = filteredCrewMovies.size,
                                            crewTVCount = filteredCrewTV.size,
                                            mainDepartment = mainDept,
                                            hazeState = localHazeState,
                                            onTabChanged = { viewModel.onTabChanged(it) },
                                            hasActiveFilters = uiState.hasActiveFilters,
                                            onFilterClick = { bounds ->
                                                filterButtonBounds = bounds
                                                viewModel.openFilterModal()
                                            }
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

        val context = LocalContext.current
        PersonMorphingTopBar(
            title = uiState.person?.name ?: "",
            profilePath = uiState.person?.profilePath ?: uiState.profilePath,
            localHazeState = localHazeState,
            symbioteProgress = symbioteProgress,
            detailStackDepth = detailStackDepth,
            onBackClick = onBackClick,
            onHomeClick = onHomeClick,
            scrimAlpha = filterModalScrimAlpha,
            onShareClick = {
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val imageUrl = com.cinetrack.util.buildTmdbImageUrl(
                        uiState.person?.profilePath,
                        com.cinetrack.util.ImageType.PROFILE,
                        currentImageQuality
                    )
                    val fileUri = if (imageUrl != null) {
                        val request = coil3.request.ImageRequest.Builder(context)
                            .data(imageUrl).build()
                        val result = context.imageLoader.execute(request)
                        if (result is coil3.request.SuccessResult) {
                            val bitmap = result.image.toBitmap()
                            val imagesDir = java.io.File(context.cacheDir, "images")
                            imagesDir.mkdirs()
                            val file = java.io.File(imagesDir, "share_person.jpg")
                            val fos = java.io.FileOutputStream(file)
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos)
                            fos.close()
                            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        } else null
                    } else null

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            val personName = uiState.person?.name ?: ""
                            val dept = uiState.person?.knownForDepartment?.takeIf { it.isNotBlank() }
                            val displayName = if (dept != null) "$personName ($dept)" else personName
                            val bioSnippet = uiState.person?.biography?.takeIf { it.isNotBlank() }?.let { bio ->
                                if (bio.length > 200) bio.take(197) + "..." else bio
                            }
                            val titleParam = android.net.Uri.encode(personName)
                            val posterParam = uiState.person?.profilePath?.let { "&p=${android.net.Uri.encode(it)}" } ?: ""
                            val link = "https://alle-0.github.io/FlickTrove/open.html?type=person&id=${uiState.personId}&t=$titleParam$posterParam"
                            val body = if (bioSnippet != null) "$bioSnippet\n\n$link" else link
                            val shareText = context.getString(R.string.detail_share_text, displayName, body)
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            if (fileUri != null) {
                                putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                type = "image/jpeg"
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } else {
                                type = "text/plain"
                            }
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, context.getString(R.string.detail_content_desc_share)))
                    }
                }
            }
        )

        HomeFilterModal(
            isVisible = uiState.isFilterModalOpen,
            isCollectionFilter = false,
            isCommentsFilter = false,
            hasTvSeries = true,
            category = if (uiState.activeTab.contains("tv")) "tv" else "movie",
            triggerBounds = filterButtonBounds,
            sortConfig = uiState.sortConfig,
            hazeState = localHazeState,
            onSortConfigChanged = { newConfig ->
                viewModel.updateSortConfig(newConfig)
            },
            onDismissRequest = { viewModel.dismissFilterModal() }
        )
    }
}
