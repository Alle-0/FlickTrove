package com.cinetrack.ui.screens

import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.cinetrack.ui.components.stats.YearSelectionModal
import com.cinetrack.ui.viewmodel.TimeRange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.BoxOfficeMovie
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.common.CategoryTabSelector
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.boxoffice.boxOfficeSkeletons
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.theme.NeonTeal
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.BoxOfficeCategory
import com.cinetrack.ui.viewmodel.BoxOfficeUiState
import com.cinetrack.ui.viewmodel.BoxOfficeViewModel
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

object BoxOfficeTab : Tab {
    var requestedCategory: BoxOfficeCategory? by mutableStateOf(null)

    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(id = R.string.box_office_title)
            return remember(title) {
                TabOptions(
                    index = 11u,
                    title = title,
                    icon = null
                )
            }
        }

    @Composable
    override fun Content() {
        val rootNavigator = LocalNavigator.currentOrThrow.parent ?: LocalNavigator.currentOrThrow
        val context = LocalContext.current
        var currentContext = context
        while (currentContext is ContextWrapper && currentContext !is ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? ComponentActivity

        val viewModel = if (activity != null) {
            androidx.hilt.navigation.compose.hiltViewModel<BoxOfficeViewModel>(activity)
        } else {
            getViewModel<BoxOfficeViewModel>()
        }

        LaunchedEffect(requestedCategory) {
            val target = requestedCategory
            if (target != null) {
                if (viewModel.uiState.value.activeTab != target) {
                    viewModel.switchTab(target)
                }
                requestedCategory = null
            }
        }

        val paddingValues = LocalAppPadding.current
        val hazeState = LocalHazeState.current ?: remember { HazeState() }

        BoxOfficeScreenContent(
            viewModel = viewModel,
            paddingValues = paddingValues,
            hazeState = hazeState,
            onMovieClick = { movie ->
                rootNavigator.push(
                    MovieDetailScreen(
                        movieId = movie.id,
                        mediaType = "movie",
                        preloadedTitle = movie.title ?: movie.name,
                        preloadedPosterPath = movie.posterPath,
                        preloadedBackdropPath = movie.backdropPath,
                        preloadedLogoPath = movie.logoPath,
                        preloadedAccentColor = movie.accentColor
                    )
                )
            }
        )
    }
}

@Composable
fun BoxOfficeScreenContent(
    viewModel: BoxOfficeViewModel,
    paddingValues: PaddingValues,
    hazeState: HazeState,
    onMovieClick: (Movie) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackground(modifier = Modifier.fillMaxSize())

        val statusBarsTop = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val cutoutTop = androidx.compose.foundation.layout.WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
        val safeDrawingTop = androidx.compose.foundation.layout.WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
        val insetsTop = maxOf(safeDrawingTop, maxOf(statusBarsTop, cutoutTop))

        val topBarHeight = 46.dp
        val topPadding = paddingValues.calculateTopPadding() + insetsTop + topBarHeight + 10.dp
        val stickyHeaderHeight = 44.dp
        val listTopInset = topPadding + stickyHeaderHeight + 14.dp
        val bottomInset = paddingValues.calculateBottomPadding() + 100.dp

        val tabTitles = listOf(
            stringResource(R.string.box_office_weekend_tab),
            if (uiState.selectedYear != null) uiState.selectedYear.toString() else stringResource(R.string.box_office_all_time_tab)
        )
        val selectedIndex = if (uiState.activeTab == BoxOfficeCategory.WEEKEND) 0 else 1

        var isYearFilterModalVisible by remember { mutableStateOf(false) }
        var filterButtonBounds by remember { mutableStateOf<Rect?>(null) }
        val hasActiveFilter = uiState.selectedYear != null
        val currentYear = remember { java.time.LocalDate.now().year }
        val availableYears = remember(currentYear) { (currentYear downTo 2010).toList() }

        val isCurrentLoading = when (uiState.activeTab) {
            BoxOfficeCategory.WEEKEND -> uiState.isLoadingWeekend
            BoxOfficeCategory.ALL_TIME -> uiState.isLoadingAllTime
        }
        val currentList = when (uiState.activeTab) {
            BoxOfficeCategory.WEEKEND -> uiState.weekendList
            BoxOfficeCategory.ALL_TIME -> uiState.allTimeList
        }

        val weekendListState = rememberLazyListState()
        val allTimeListState = rememberLazyListState()
        val listState = if (uiState.activeTab == BoxOfficeCategory.WEEKEND) weekendListState else allTimeListState

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState),
            contentPadding = PaddingValues(top = listTopInset, bottom = bottomInset, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Content
            when {
                isCurrentLoading && currentList.isEmpty() -> {
                    boxOfficeSkeletons(activeTab = uiState.activeTab)
                }
                uiState.isError && currentList.isEmpty() -> {
                    item(key = "error_state") {
                        BoxOfficeErrorState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }
                currentList.isEmpty() -> {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.box_office_empty),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                else -> {
                    // In Weekend mode, highlight #1 with a Hero Card!
                    if (uiState.activeTab == BoxOfficeCategory.WEEKEND && currentList.isNotEmpty()) {
                        val winner = currentList.first()
                        item(key = "hero_winner_${winner.movie.id}") {
                            BoxOfficeHeroWinnerCard(
                                item = winner,
                                onClick = { onMovieClick(winner.movie) }
                            )
                        }

                        // Remaining #2 to #10
                        itemsIndexed(currentList.drop(1), key = { _, item -> "weekend_${item.movie.id}" }) { _, item ->
                            BoxOfficeMovieRow(
                                item = item,
                                onClick = { onMovieClick(item.movie) }
                            )
                        }
                    } else {
                        // All-Time List
                        itemsIndexed(currentList, key = { _, item -> "all_time_${item.movie.id}_${item.rank}" }) { _, item ->
                            BoxOfficeMovieRow(
                                item = item,
                                onClick = { onMovieClick(item.movie) }
                            )
                        }
                    }
                }
            }
        }

        // Fixed Floating Selector Bar Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .align(Alignment.TopCenter)
                .zIndex(10f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stickyHeaderHeight)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Category Tab Selector Island (offset with right padding for visual balance with filter button)
                val rightControlsInset = 44.dp
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(end = rightControlsInset),
                    contentAlignment = Alignment.Center
                ) {
                    Spacer(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeGlass(
                                state = hazeState,
                                shape = CircleShape,
                                blurRadius = HazeStyles.SmallGlassBlurRadius,
                                useOffscreenStrategy = false
                            )
                    )

                    CategoryTabSelector(
                        options = tabTitles,
                        selectedIndex = selectedIndex,
                        onOptionClick = { index ->
                            viewModel.switchTab(if (index == 0) BoxOfficeCategory.WEEKEND else BoxOfficeCategory.ALL_TIME)
                        },
                        tabWidth = 118.dp
                    )
                }

                // Classic Circular Filter Button (Right Aligned, matching HomeScreen)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInWindow()
                            filterButtonBounds = Rect(pos.x, pos.y, pos.x + coords.size.width, pos.y + coords.size.height)
                        }
                ) {
                    // Glass Background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeGlass(
                                state = hazeState,
                                shape = CircleShape,
                                blurRadius = HazeStyles.SmallGlassBlurRadius,
                                useOffscreenStrategy = false,
                                borderWidth = if (hasActiveFilter) 1.5.dp else 1.dp,
                                borderColor = if (hasActiveFilter) MaterialTheme.colorScheme.primary else HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop)
                            )
                    )

                    // Interactive Content Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .bounceClick(scaleDown = 0.92f) {
                                isYearFilterModalVisible = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_filtri),
                            contentDescription = stringResource(R.string.home_cd_filters),
                            tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Year Selection Modal
        YearSelectionModal(
            isVisible = isYearFilterModalVisible,
            onDismiss = { isYearFilterModalVisible = false },
            currentRange = if (uiState.selectedYear == null) TimeRange.AllTime else TimeRange.Year(uiState.selectedYear!!),
            availableYears = availableYears,
            hazeState = hazeState,
            triggerBounds = filterButtonBounds,
            titleRes = R.string.stats_period,
            allTimeLabelRes = R.string.box_office_filter_all_years,
            onYearSelected = { year ->
                viewModel.selectYear(year)
                viewModel.switchTab(BoxOfficeCategory.ALL_TIME)
                isYearFilterModalVisible = false
            },
            onAllTimeSelected = {
                viewModel.selectYear(null)
                viewModel.switchTab(BoxOfficeCategory.ALL_TIME)
                isYearFilterModalVisible = false
            }
        )
    }
}


@androidx.compose.runtime.Composable
fun BoxOfficeHeroWinnerCard(
    item: BoxOfficeMovie,
    onClick: () -> Unit
) {
    val movie = item.movie
    val imageQuality = LocalImageQuality.current

    val backdropUrl = remember(movie.backdropPath, movie.posterPath, imageQuality) {
        val path = movie.backdropPath ?: movie.posterPath
        if (path != null) buildTmdbImageUrl(path, ImageType.BACKDROP, imageQuality) else null
    }

    val heroCardShape = RoundedCornerShape(26.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .bounceClick(scaleDown = 0.96f) {
                onClick()
            }
            .clip(heroCardShape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), heroCardShape)
    ) {
        // Backdrop Image
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1F1F24))
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Top Badge: Champion / Winner Pill
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_crown),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "#1 ${stringResource(R.string.box_office_weekend_badge).uppercase()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }

        // Bottom Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = movie.title ?: "",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Year & Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val year = movie.releaseDate?.take(4) ?: ""
                    if (year.isNotBlank()) {
                        Text(
                            text = year,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    val rating = movie.voteAverage ?: 0.0
                    if (rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_star_piena),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", rating),
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Revenue Pill
                if (item.formattedRevenue.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = item.formattedRevenue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BoxOfficeMovieRow(
    item: BoxOfficeMovie,
    onClick: () -> Unit
) {
    val movie = item.movie
    val imageQuality = LocalImageQuality.current

    val posterUrl = remember(movie.posterPath, imageQuality) {
        if (movie.posterPath != null) buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, imageQuality) else null
    }

    // Rank styling: #2 silver, #3 bronze, others crystal glass
    val rankBadgeColors = remember(item.rank) {
        when (item.rank) {
            1 -> Color(0xFFFFD700) to Color(0xFFFFD700).copy(alpha = 0.2f) // Gold
            2 -> Color(0xFFE0E0E0) to Color(0xFFC0C0C0).copy(alpha = 0.2f) // Silver
            3 -> Color(0xFFCD7F32) to Color(0xFFCD7F32).copy(alpha = 0.2f) // Bronze
            else -> Color.White.copy(alpha = 0.6f) to Color.White.copy(alpha = 0.08f)
        }
    }

    val rowShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f) { onClick() }
            .clip(rowShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = rowShape
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(rankBadgeColors.second)
                    .border(1.dp, rankBadgeColors.first, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${item.rank}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = rankBadgeColors.first
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Poster Thumbnail
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                if (posterUrl != null) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title, Year, Rating
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = movie.title ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val year = movie.releaseDate?.take(4) ?: ""
                    if (year.isNotBlank()) {
                        Text(
                            text = year,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    val rating = movie.voteAverage ?: 0.0
                    if (rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_star_piena),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", rating),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Revenue Pill (if revenue > 0)
            if (item.formattedRevenue.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.formattedRevenue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                }
            }
        }
    }
}



@Composable
fun BoxOfficeErrorState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_error),
            contentDescription = null,
            tint = Color(0xFFFFB74D),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.box_office_error),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Riprova", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
