package com.cinetrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.SortConfig
import com.cinetrack.ui.components.dialog.HomeFilterModal
import com.cinetrack.ui.components.search.SearchContentGrid
import com.cinetrack.ui.components.search.SearchHeader
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.UiText
import com.cinetrack.ui.viewmodel.FilterPill
import com.cinetrack.ui.viewmodel.SearchViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

data class SearchScreen(
    val startX: Float? = null,
    val startY: Float? = null,
    val initialGenreId: Long? = null,
    val initialGenreName: String? = null,
    val initialKeywordId: Long? = null,
    val initialKeywordName: String? = null
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    override fun Content() {
        val viewModel = getViewModel<SearchViewModel>()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(initialGenreId, initialGenreName, initialKeywordName) {
            if (initialGenreId != null) {
                viewModel.updateSortConfig(
                    viewModel.uiState.value.sortConfig.copy(
                        selectedGenres = listOf(initialGenreId),
                        selectedKeywords = emptyList()
                    )
                )
                viewModel.onQueryChanged("")
            } else if (initialKeywordId != null) {
                viewModel.updateSortConfig(
                    viewModel.uiState.value.sortConfig.copy(
                        selectedKeywords = listOf(initialKeywordId),
                        selectedGenres = emptyList()
                    )
                )
                viewModel.onQueryChanged("")
            }
        }

        val paddingValues = PaddingValues(0.dp)

        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                SearchScreenContent(
                    viewModel = viewModel,
                    paddingValues = paddingValues,
                    animatedVisibilityScope = this@AnimatedVisibility,
                    startX = startX,
                    startY = startY,
                    initialGenreName = initialGenreName,
                    initialKeywordName = initialKeywordName,
                    onBack = { navigator.pop() },
                    onMovieClick = { movie ->
                        navigator.push(MovieDetailScreen(movie.id, movie.mediaType))
                    },
                    onPersonClick = { personId ->
                        navigator.push(PersonDetailScreen(personId, null))
                    },
                    onCollectionClick = { collectionId, collectionName ->
                        navigator.push(CollectionDetailScreen(collectionId, collectionName))
                    },
                    onDiscoverTrendingClick = { requestedType ->
                        DiscoverTab.requestedType = requestedType
                        navigator.popUntilRoot()
                    }
                )
            }
        }
    }
}

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
    onPersonClick: (Long) -> Unit = {},
    onCollectionClick: (Long, String?) -> Unit = { _, _ -> },
    onDiscoverTrendingClick: ((String) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var isFilterVisible by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    var textFieldValue by remember { mutableStateOf(TextFieldValue(uiState.query)) }

    LaunchedEffect(uiState.query) {
        if (uiState.query != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = uiState.query,
                selection = TextRange(uiState.query.length)
            )
        }
    }

    val internalHazeState = hazeState ?: remember { HazeState() }
    val globalSearchHazeState = remember { HazeState() }
    val onExitRequest = remember { mutableStateOf<(() -> Unit)?>(null) }
    val movieActions = LocalMovieActions.current

    BackHandler {
        if (movieActions.isAnyModalOpen) {
            movieActions.closeAll()
        } else if (isFilterVisible) {
            isFilterVisible = false
        } else {
            onExitRequest.value?.invoke()
        }
    }

    val focusRequester = remember { FocusRequester() }
    var hasRequestedFocus by rememberSaveable { mutableStateOf(false) }
    val filterBounds = remember { arrayOf<Rect?>(null) }

    val density = LocalDensity.current
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
        var hasRevealed by rememberSaveable { mutableStateOf(false) }
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
        val statusBarsTopPx = WindowInsets.statusBars.getTop(localDensity).toFloat()
        val revealCenter = remember(width, height, statusBarsTopPx, startX, startY) {
            if (startX != null && startY != null) {
                Offset(startX, startY)
            } else {
                with(localDensity) {
                    Offset(width - 36.dp.toPx(), height - 120.dp.toPx())
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
                                addOval(Rect(center = revealCenter, radius = radius))
                            }
                            return Outline.Generic(path)
                        }
                    }
                }
        ) {
            MovieActionsWrapper(
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(globalSearchHazeState)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // 1. Background Source Layer
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
                            var previousSortConfig by remember { mutableStateOf<SortConfig?>(null) }
                            LaunchedEffect(uiState.sortConfig) {
                                if (previousSortConfig != null && previousSortConfig != uiState.sortConfig) {
                                    if (uiState.results.isNotEmpty()) {
                                        gridState.scrollToItem(0)
                                    }
                                }
                                previousSortConfig = uiState.sortConfig
                            }

                            SearchContentGrid(
                                uiState = uiState,
                                gridState = gridState,
                                paddingValues = paddingValues,
                                headerHeight = headerHeight,
                                columns = columns,
                                personColumns = personColumns,
                                cardWidth = cardWidth,
                                personCardWidth = personCardWidth,
                                animatedVisibilityScope = animatedVisibilityScope,
                                keyboardController = keyboardController,
                                actionsState = actionsState,
                                animatedMovieIds = viewModel.animatedMovieIds,
                                onMovieClick = onMovieClick,
                                onPersonClick = onPersonClick,
                                onCollectionClick = onCollectionClick,
                                onDiscoverTrendingClick = onDiscoverTrendingClick,
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onLongPress = { m, pressOffset, cardPos -> actionsState.onLongPress(m, pressOffset, cardPos) },
                                onEmitMessage = { viewModel.emitMessage(UiText.DynamicString(it)) },
                                onRetry = { viewModel.retry() },
                                onLoadNextPage = { viewModel.loadNextPage() }
                            )
                        }
                    }

                    // 2. Header Layer
                    SearchHeader(
                        query = uiState.query,
                        category = uiState.category,
                        sortConfig = uiState.sortConfig,
                        preferences = uiState.preferences,
                        recentSearches = uiState.recentSearches,
                        suggestedFilters = uiState.suggestedFilters,
                        initialGenreName = initialGenreName,
                        initialKeywordName = initialKeywordName,
                        textFieldValue = textFieldValue,
                        onTextFieldValueChange = { textFieldValue = it },
                        onQueryChanged = { viewModel.onQueryChanged(it) },
                        onClearQuery = { viewModel.onQueryChanged("") },
                        onBackClick = { onExitRequest.value?.invoke() },
                        onLayoutToggleClick = { nextCols ->
                            viewModel.updatePreferences(uiState.preferences.copy(gridColumns = nextCols))
                        },
                        onFilterClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            isFilterVisible = true
                        },
                        onCategoryChanged = { viewModel.onCategoryChanged(it) },
                        onClearRecentSearches = { viewModel.clearRecentSearches() },
                        onDeleteRecentSearch = { viewModel.deleteRecentSearch(it) },
                        onToggleSuggestionsExpanded = { viewModel.toggleSuggestionsExpanded() },
                        onSuggestedFilterClick = { filter ->
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
                        },
                        focusRequester = focusRequester,
                        keyboardController = keyboardController,
                        hazeState = internalHazeState,
                        onHeaderHeightMeasured = { measured -> headerHeight = measured },
                        onFilterBoundsMeasured = { bounds -> filterBounds[0] = bounds },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }

        // 3. Filter Modal Layer
        Box(modifier = Modifier.zIndex(5000f)) {
            val dimAlpha by animateFloatAsState(
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
}
