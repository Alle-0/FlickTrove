package com.cinetrack.ui.screens

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.BorderStroke

import com.cinetrack.ui.components.HomeFilterModal
import com.cinetrack.ui.components.search.*
import com.cinetrack.ui.theme.PremiumBackground
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.layoutToggleIcon
import com.cinetrack.ui.components.shared.MovieCardSkeleton
import com.cinetrack.ui.components.shared.nextGridColumns

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.data.Movie
import com.cinetrack.data.api.PersonSearchResult
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.ui.components.CinematicBackground
import com.cinetrack.ui.components.MovieCard
import com.cinetrack.ui.components.PersonCard
import com.cinetrack.util.toComposeColor
import com.cinetrack.ui.viewmodel.SearchViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.cinetrack.ui.theme.HazeStyles

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.core.screen.Screen
import kotlin.math.pow
import kotlin.math.sqrt
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class SearchScreen(
    val startX: Float? = null,
    val startY: Float? = null,
    val initialGenreId: Long? = null,
    val initialGenreName: String? = null,
    val initialKeywordId: Long? = null,
    val initialKeywordName: String? = null
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel = getViewModel<SearchViewModel>()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(initialGenreId, initialGenreName, initialKeywordName) {
            if (initialGenreId != null) {
                viewModel.updateSortConfig(viewModel.uiState.value.sortConfig.copy(selectedGenres = listOf(initialGenreId), selectedKeywords = emptyList()))
                // We want to force a blank search so the genre filter kicks in immediately
                viewModel.onQueryChanged("")
            } else if (initialGenreName != null) {
                viewModel.onQueryChanged("") // Or handle initial search
            }
        }

        val deepLinkIntent = com.cinetrack.ui.LocalDeepLinkIntent.current
        SearchScreenContent(
            viewModel = viewModel,
            paddingValues = PaddingValues(0.dp),
            startX = startX,
            startY = startY,
            initialGenreName = initialGenreName,
            initialKeywordName = initialKeywordName,
            onBack = { navigator.pop() },
            onClosing = { },
            onMovieClick = { movie ->
                navigator.push(MovieDetailScreen(movie.id, movie.mediaType))
            },
            onPersonClick = { personId ->
                navigator.push(PersonDetailScreen(personId, null))
            },
            onDiscoverTrendingClick = { reqType ->
                navigator.pop()
                deepLinkIntent.value = android.content.Intent("com.cinetrack.OPEN_DISCOVER_TAB").apply {
                    putExtra("requestedType", reqType)
                }
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreenContent(
    viewModel: SearchViewModel,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    startX: Float? = null,
    startY: Float? = null,
    initialGenreName: String? = null,
    initialKeywordName: String? = null,
    onBack: () -> Unit,
    onClosing: () -> Unit = {},
    onMovieClick: (Movie) -> Unit,
    onPersonClick: (Long) -> Unit,
    onDiscoverTrendingClick: ((String) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    
    var isFilterVisible by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    var textFieldValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(uiState.query)) }
    
    LaunchedEffect(uiState.query) {
        if (uiState.query != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = uiState.query,
                selection = androidx.compose.ui.text.TextRange(uiState.query.length)
            )
        }
    }

    val internalHazeState = hazeState ?: remember { HazeState() }
    val globalSearchHazeState = remember { HazeState() }

    // This will be set by animatedTriggerExit defined inside BoxWithConstraints
    val onExitRequest = remember { androidx.compose.runtime.mutableStateOf<(() -> Unit)?>(null) }

    val movieActions = com.cinetrack.ui.components.shared.LocalMovieActions.current

    androidx.activity.compose.BackHandler {
        if (movieActions.isAnyModalOpen) {
            movieActions.closeAll()
        } else if (isFilterVisible) {
            isFilterVisible = false
        } else {
            onExitRequest.value?.invoke()
        }
    }

    val focusRequester = remember { FocusRequester() }
    var hasRequestedFocus by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val filterBounds = remember { arrayOf<androidx.compose.ui.geometry.Rect?>(null) }
    
    val density = androidx.compose.ui.platform.LocalDensity.current
    var headerHeight by remember { mutableStateOf(250.dp) }
    
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
    val personColumns = 4
    val personCardWidth = (screenWidth - (padding * 2) - (gap * (personColumns - 1))) / personColumns


    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        var isMeasured by remember { mutableStateOf(false) }
        var hasRevealed by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
        val revealAmount = remember(hasRevealed) { Animatable(if (hasRevealed) 1f else 0f) }
        var isClosing by remember { mutableStateOf(false) }

        if (width > 0 && !isMeasured) {
            LaunchedEffect(Unit) { isMeasured = true }
        }

        LaunchedEffect(isMeasured) {
            if (isMeasured) {
                if (!hasRevealed) {
                    revealAmount.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 800, 
                            easing = CubicBezierEasing(0.7f, 0f, 0.2f, 1f)
                        )
                    )
                    hasRevealed = true
                }
                
                // Request keyboard focus after reveal completes
                if (!hasRequestedFocus) {
                    hasRequestedFocus = true
                    if (initialGenreName == null && initialKeywordName == null) {
                        try { focusRequester.requestFocus() } catch (e: IllegalStateException) {}
                    } else {
                        keyboardController?.hide()
                    }
                }
            }
        }

        val localDensity = LocalDensity.current
        val statusBarsTopPx = androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(localDensity).toFloat()
        val revealCenter = remember(width, height, statusBarsTopPx, startX, startY) {
            if (startX != null && startY != null) {
                androidx.compose.ui.geometry.Offset(startX, startY)
            } else {
                with(localDensity) {
                    androidx.compose.ui.geometry.Offset(width - 36.dp.toPx(), height - 120.dp.toPx())
                }
            }
        }
        val maxRevealRadius = remember(revealCenter, width, height) {
            val distTopLeft = sqrt(revealCenter.x.pow(2) + revealCenter.y.pow(2))
            val distTopRight = sqrt((width - revealCenter.x).pow(2) + revealCenter.y.pow(2))
            val distBottomLeft = sqrt(revealCenter.x.pow(2) + (height - revealCenter.y).pow(2))
            val distBottomRight = sqrt((width - revealCenter.x).pow(2) + (height - revealCenter.y).pow(2))
            max(max(distTopLeft, distTopRight), max(distBottomLeft, distBottomRight)) * 1.1f
        }

        // Override triggerExit to animate close before popping
        val animatedTriggerExit = {
            if (!isClosing) {
                isClosing = true
                keyboardController?.hide()
                focusManager.clearFocus()
                onClosing()
                scope.launch {
                    revealAmount.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = 800, 
                            easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
                        )
                    )
                    onBack()
                }
            }
        }
        // Wire into the BackHandler callback defined above BoxWithConstraints
        onExitRequest.value = animatedTriggerExit

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    val radius = revealAmount.value * maxRevealRadius
                    clip = true
                    shape = object : Shape {
                        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                            val path = Path().apply {
                                addOval(androidx.compose.ui.geometry.Rect(center = revealCenter, radius = radius))
                            }
                            return Outline.Generic(path)
                        }
                    }
                }
        ) {

        LaunchedEffect(Unit) { /* focus handled after reveal */ }

        Box(modifier = Modifier.fillMaxSize()) {

                com.cinetrack.ui.components.shared.MovieActionsWrapper(
                    hazeState = internalHazeState,
                    folders = uiState.folders,
                    isItemInFolder = { movie, folderId ->
                        uiState.folders.find { it.id == folderId }?.itemIds?.contains("${movie.mediaType}_${movie.id}") ?: false
                    },
                    onDelete = { viewModel.deleteMovie(it) },
                    onUpdateRating = { movie, rating -> viewModel.updateRating(movie, rating) },
                    onUpdateNote = { movie, note -> viewModel.updateNote(movie, note) },
                    onToggleFolder = { movie, folder -> viewModel.toggleItemInFolder(folder, movie) }
                ) { actionsState ->
                Box(modifier = Modifier.fillMaxSize().haze(globalSearchHazeState).background(MaterialTheme.colorScheme.background)) {
                // 1. Background Source Layer (Only captures what's behind the header/glass)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(
                            state = internalHazeState,
                            style = HazeStyles.PremiumDark
                        )
                        
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { focusManager.clearFocus() })
                            }
                    ) {

                    val gridState = viewModel.lazyGridState
                    
                    var previousSortConfig by remember { mutableStateOf<com.cinetrack.data.models.SortConfig?>(null) }
                    LaunchedEffect(uiState.sortConfig) {
                        if (previousSortConfig != null && previousSortConfig != uiState.sortConfig) {
                            if (uiState.results.isNotEmpty()) {
                                gridState.scrollToItem(0)
                            }
                        }
                        previousSortConfig = uiState.sortConfig
                    }
                    
                    val shouldLoadMore = remember {
                        derivedStateOf {
                            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
                            lastVisibleItem.index >= gridState.layoutInfo.totalItemsCount - 5
                        }
                    }

                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value && !uiState.isEndReached && !uiState.isNextPageLoading) {
                            viewModel.loadNextPage()
                        }
                    }

                    if (uiState.errorMessage != null) {
                        // PREMIUM ERROR STATE
                        Column(
                            modifier = Modifier
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
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { viewModel.retry() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.onSurface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(ImageVector.vectorResource(id = R.drawable.ic_lente), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.detail_retry))
                            }
                        }
                    } else {                        LazyVerticalGrid(
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
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val movieSpan = 12 / columns
                            val personSpan = 12 / personColumns
                            val hasDiscoveryFilters = uiState.sortConfig.selectedGenres.isNotEmpty() || uiState.sortConfig.selectedKeywords.isNotEmpty() || uiState.sortConfig.selectedDecades.isNotEmpty() || uiState.sortConfig.sortType != "popularity"
                            val showEmptySearch = uiState.query.isEmpty() && (!hasDiscoveryFilters || uiState.category == "person")
                            
                            if (showEmptySearch) {

                                if (uiState.category == "movie") {
                                    searchTrendingMoviesSection(
                                        trendingMovies = uiState.trendingMovies,
                                        favorites = uiState.favorites,
                                        movieFolderColors = uiState.movieFolderColors,
                                        preferences = uiState.preferences,
                                        animatedMovieIds = viewModel.animatedMovieIds,
                                        columns = columns,
                                        movieSpan = movieSpan,
                                        cardWidth = cardWidth,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        keyboardController = keyboardController,
                                        onMovieClick = onMovieClick,
                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                        onLongPress = { m, pressOffset, cardPos -> actionsState.onLongPress(m, pressOffset, cardPos) },
                                        onEmitMessage = { viewModel.emitMessage(com.cinetrack.ui.utils.UiText.DynamicString(it)) },
                                        onDiscoverMore = onDiscoverTrendingClick?.let { cb -> { cb("trending_movies") } }
                                    )
                                }

                                if (uiState.category == "tv") {
                                    searchTrendingTvSection(
                                        trendingTv = uiState.trendingTv,
                                        favorites = uiState.favorites,
                                        movieFolderColors = uiState.movieFolderColors,
                                        preferences = uiState.preferences,
                                        animatedMovieIds = viewModel.animatedMovieIds,
                                        columns = columns,
                                        movieSpan = movieSpan,
                                        cardWidth = cardWidth,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        keyboardController = keyboardController,
                                        onMovieClick = onMovieClick,
                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                        onLongPress = { m, pressOffset, cardPos -> actionsState.onLongPress(m, pressOffset, cardPos) },
                                        onEmitMessage = { viewModel.emitMessage(com.cinetrack.ui.utils.UiText.DynamicString(it)) },
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
                                    items(if (uiState.category == "person") 16 else 12, contentType = { "skeleton" }, span = { if (uiState.category == "person") GridItemSpan(personSpan) else GridItemSpan(movieSpan) }) { 
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
                                    animatedMovieIds = viewModel.animatedMovieIds,
                                    columns = columns,
                                    movieSpan = movieSpan,
                                    personSpan = personSpan,
                                    cardWidth = cardWidth,
                                    personCardWidth = personCardWidth,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    keyboardController = keyboardController,
                                    onMovieClick = onMovieClick,
                                    onPersonClick = onPersonClick,
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onLongPress = { m, pressOffset, cardPos -> actionsState.onLongPress(m, pressOffset, cardPos) },
                                    onEmitMessage = { viewModel.emitMessage(com.cinetrack.ui.utils.UiText.DynamicString(it)) }
                                )

                                if (uiState.isNextPageLoading) {
                                    items(if (uiState.category == "person") 4 else columns, contentType = { "skeleton" }, span = { if (uiState.category == "person") GridItemSpan(personSpan) else GridItemSpan(movieSpan) }) { 
                                        MovieCardSkeleton(width = if (uiState.category == "person") personCardWidth else cardWidth) 
                                    }
                                }
                                }
                            }
                        }
                    }
                } // End Background Source Layer
                }

                // 2. Header (Moved outside blurring boxes to stay sharp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.TopCenter)
                        .zIndex(10f)
                        .onGloballyPositioned { layoutCoordinates ->
                            headerHeight = with(density) { layoutCoordinates.size.height.toDp() }
                        }
                ) {
                    // Header Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .hazeGlass(
                                state = internalHazeState,
                                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                                borderWidth = 0.dp
                            )
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.displayCutout)
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    ) {

                        // Row 1: Back, Search, Filter
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(44.dp)) {
                                Box(modifier = Modifier.fillMaxSize()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape))
                                Box(modifier = Modifier.fillMaxSize().bounceClick { onExitRequest.value?.invoke() }, contentAlignment = Alignment.Center) {
                                    Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_left), contentDescription = stringResource(R.string.detail_content_desc_back), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                             Box(modifier = Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.CenterStart) {
                                 Box(modifier = Modifier.fillMaxSize()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape))
                                     Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp)) {
                                        Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_lente), contentDescription = stringResource(R.string.search_content_desc), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    CompositionLocalProvider(LocalTextSelectionColors provides TextSelectionColors(handleColor = primaryColor, backgroundColor = primaryColor.copy(alpha = 0.4f))) {
                                        BasicTextField(
                                            value = textFieldValue,
                                            onValueChange = { 
                                                textFieldValue = it
                                                viewModel.onQueryChanged(it.text) 
                                            },
                                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = { viewModel.onQueryChanged(uiState.query) }),
                                            decorationBox = { innerTextField ->
                                                Box(contentAlignment = Alignment.CenterStart) {
                                                    if (uiState.query.isEmpty()) {
                                                        val hasLayoutButton = uiState.preferences.showLayoutToggle && uiState.category != "person"
                                                        val placeholderText = when {
                                                            uiState.sortConfig.selectedGenres.isNotEmpty() -> {
                                                                val gid = uiState.sortConfig.selectedGenres.first()
                                                                val name = uiState.suggestedFilters.find { it.id == gid && !it.isKeyword }?.name ?: initialGenreName ?: stringResource(R.string.search_fallback_genre)
                                                                stringResource(R.string.search_active_genre_format, name)
                                                            }
                                                            uiState.sortConfig.selectedKeywords.isNotEmpty() -> {
                                                                val kid = uiState.sortConfig.selectedKeywords.first()
                                                                val dictName = com.cinetrack.data.KeywordDictionary.getLocalizedKeywordName(kid, uiState.preferences.contentLanguage)?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                                                                val name = uiState.suggestedFilters.find { it.id == kid }?.name ?: dictName ?: initialKeywordName ?: stringResource(R.string.search_fallback_keyword)
                                                                stringResource(R.string.search_active_keyword_format, name)
                                                            }
                                                            else -> stringResource(R.string.search_placeholder)
                                                        }
                                                        val hasActiveFilter = uiState.sortConfig.selectedGenres.isNotEmpty() || uiState.sortConfig.selectedKeywords.isNotEmpty()
                                                        Text(
                                                            text = placeholderText,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasActiveFilter) 0.6f else 0.3f),
                                                            fontSize = if (hasLayoutButton) 12.sp else 14.sp,
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )
                                    }
                                    if (uiState.query.isNotEmpty()) {
                                        Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_x), contentDescription = stringResource(R.string.search_content_desc_clear), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(14.dp).bounceClick(scaleDown = 0.8f) { viewModel.onQueryChanged("") })
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            val hasActiveFilters = uiState.sortConfig.selectedGenres.isNotEmpty() || uiState.sortConfig.selectedKeywords.isNotEmpty() || uiState.sortConfig.selectedProviders.isNotEmpty() || uiState.sortConfig.selectedDecades.isNotEmpty() || uiState.sortConfig.sortType != "popularity"
                            
                            if (uiState.preferences.showLayoutToggle && uiState.category != "person") {
                                Box(modifier = Modifier.size(44.dp)) {
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier.fillMaxSize().bounceClick(scaleDown = 0.92f) {
                                            val nextColumns = nextGridColumns(uiState.preferences.gridColumns)
                                            viewModel.updatePreferences(uiState.preferences.copy(gridColumns = nextColumns))
                                        },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = layoutToggleIcon(uiState.preferences.gridColumns),
                                            contentDescription = stringResource(R.string.search_content_desc_layout),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            
                            Box(modifier = Modifier.size(44.dp).onGloballyPositioned { layoutCoordinates ->
                                val position = layoutCoordinates.positionInWindow()
                                filterBounds[0] = Rect(position.x, position.y, position.x + layoutCoordinates.size.width, position.y + layoutCoordinates.size.height)
                            }) {
                                Box(modifier = Modifier.fillMaxSize()
                                     .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                                    .then(if (hasActiveFilters) Modifier.border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), CircleShape) else Modifier))
                                Box(modifier = Modifier.fillMaxSize().bounceClick { keyboardController?.hide(); focusManager.clearFocus(); isFilterVisible = true }, contentAlignment = Alignment.Center) {
                                    Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_filtri), contentDescription = stringResource(R.string.folder_detail_filters), tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        
                        if (uiState.sortConfig.selectedKeywords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.search_keyword_filter_warning),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 24.dp),
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )
                        }
                        

                        
                        Spacer(modifier = Modifier.height(16.dp))

                        SearchRecentSearchesRow(
                            recentSearches = uiState.recentSearches,
                            query = uiState.query,
                            onSearchClick = { viewModel.onQueryChanged(it) },
                            onClearAll = { viewModel.clearRecentSearches() },
                            onDeleteSearch = { viewModel.deleteRecentSearch(it) }
                        )

                        SearchSuggestedFiltersRow(
                            suggestedFilters = uiState.suggestedFilters,
                            query = uiState.query,
                            isExpanded = uiState.preferences.isSearchSuggestionsExpanded,
                            onToggleExpanded = { viewModel.toggleSuggestionsExpanded() },
                            onFilterClick = { filter ->
                                if (filter.isKeyword) {
                                    val currentKw = uiState.sortConfig.selectedKeywords
                                    val newKw = if (currentKw.contains(filter.id)) emptyList() else listOf(filter.id)
                                    viewModel.updateSortConfig(uiState.sortConfig.copy(selectedKeywords = newKw))
                                } else {
                                    val currentGenres = uiState.sortConfig.selectedGenres
                                    val newGenres = if (currentGenres.contains(filter.id)) emptyList() else listOf(filter.id)
                                    viewModel.updateSortConfig(uiState.sortConfig.copy(selectedGenres = newGenres))
                                }
                                viewModel.onQueryChanged("")
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        )

                        // Category Selector (Pillow)
                        SearchCategorySelector(
                            category = uiState.category,
                            onCategoryChanged = { viewModel.onCategoryChanged(it) }
                        )

                    }
                }
                }
                }
                }
            }

        // --- Modal (Top Level to avoid clipping/zIndex issues) ---
        Box(modifier = Modifier.zIndex(5000f)) {
            val dimAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isFilterVisible) 0.65f else 0f,
                animationSpec = tween(400),
                label = "searchDimAlpha"
            )
            
            if (dimAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = dimAlpha))
                        .pointerInput(Unit) { detectTapGestures(onTap = { isFilterVisible = false }) }
                )
            }
            
            HomeFilterModal(
                isVisible = isFilterVisible,
                sortConfig = uiState.sortConfig,
                hazeState = globalSearchHazeState,
                triggerBounds = filterBounds[0],
                category = uiState.category,
                suggestedFilters = uiState.suggestedFilters,
                initialKeywordName = initialKeywordName,
                onSortConfigChanged = { viewModel.updateSortConfig(it) },
                onDismissRequest = { isFilterVisible = false }
            )
        }
    }




