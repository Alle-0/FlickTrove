package com.cinetrack.ui.screens

import com.cinetrack.R

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import java.util.Calendar
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.cinetrack.ui.LocalAppPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import com.cinetrack.ui.viewmodel.CalculatedStats
import com.cinetrack.ui.viewmodel.StatsUiState
import com.cinetrack.ui.viewmodel.TimeRange
import com.cinetrack.ui.viewmodel.StatsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.ui.theme.DarkSurface
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.LayoutCoordinates
import com.cinetrack.ui.components.*
import com.cinetrack.ui.components.stats.*
import com.cinetrack.ui.theme.*
import com.cinetrack.ui.viewmodel.PersonStat
import com.cinetrack.ui.components.shared.shimmerEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.*
import kotlin.math.roundToInt

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.card.MovieCard

object StatsTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(id = R.string.stats_tab_title)
            return remember(title) {
                TabOptions(
                    index = 4u,
                    title = title,
                    icon = null
                )
            }
        }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val activity = context as? androidx.activity.ComponentActivity
        val viewModel = if (activity != null) {
            androidx.hilt.navigation.compose.hiltViewModel<StatsViewModel>(activity)
        } else {
            getViewModel<StatsViewModel>()
        }
        val paddingValues = LocalAppPadding.current
        val hazeState = com.cinetrack.ui.LocalHazeState.current
        val navigator = LocalNavigator.currentOrThrow.parent ?: LocalNavigator.currentOrThrow

        var isYearPickerVisible by remember { mutableStateOf(false) }
        val yearPickerButtonBounds = remember { arrayOf<androidx.compose.ui.geometry.Rect?>(null) }
        val statsUiState by viewModel.uiState.collectAsStateWithLifecycle()
        val activeHazeState = hazeState ?: remember { HazeState() }

        StatsScreenContent(
            viewModel = viewModel,
            paddingValues = paddingValues,
            hazeState = activeHazeState,
            onToggleYearPicker = { visible, bounds ->
                isYearPickerVisible = visible
                yearPickerButtonBounds[0] = bounds
            },
            onPersonClick = { personId, profilePath ->
                navigator.push(PersonDetailScreen(personId, profilePath))
            },
            onMovieClick = { movie ->
                navigator.push(MovieDetailScreen(movie.id, movie.mediaType))
            }
        )

        YearSelectionModal(
            isVisible = isYearPickerVisible,
            onDismiss = { isYearPickerVisible = false },
            currentRange = statsUiState.timeRange,
            availableYears = statsUiState.availableYears,
            hazeState = activeHazeState,
            triggerBounds = yearPickerButtonBounds[0],
            onYearSelected = { year ->
                viewModel.setTimeRange(TimeRange.Year(year))
                isYearPickerVisible = false
            },
            onAllTimeSelected = {
                viewModel.setTimeRange(TimeRange.AllTime)
                isYearPickerVisible = false
            }
        )
    }
}

@Composable
fun StatsScreenContent(
    viewModel: StatsViewModel,
    paddingValues: PaddingValues,
    hazeState: HazeState? = null,
    onToggleYearPicker: (Boolean, androidx.compose.ui.geometry.Rect?) -> Unit = { _, _ -> },
    onPersonClick: (Long, String?) -> Unit = { _, _ -> },
    onMovieClick: (Movie) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    val context = LocalContext.current
    
    // Local haze state to isolate background blur from foreground content
    val activeHazeState = hazeState ?: remember { HazeState() }
    var isSharingStats by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Background & Content Layer (Captured by Haze for Modals) ──────
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Contenuto da catturare (solo lo sfondo e la colonna principale)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(activeHazeState, style = HazeStyles.PremiumDark)
            ) {
                CinematicBackground(
                    modifier = Modifier.fillMaxSize()
                )

                AnimatedContent(
                    targetState = uiState.isLoading,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(600)) togetherWith 
                        fadeOut(animationSpec = tween(600))
                    },
                    label = "StatsLoadingTransition"
                ) { loading ->
                    if (loading) {
                        StatsSkeleton(paddingValues = paddingValues)
                    } else {
                        val statsData = uiState.stats
                        when {
                            statsData == null -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.stats_no_data), color = Color.White.copy(alpha = 0.5f))
                                }
                            }
                            else -> {
                                val stats = statsData
                                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                val currentYearStats = uiState.currentYearStats

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    // Top safe area spacer
                                    Spacer(Modifier.height(paddingValues.calculateTopPadding() + androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 46.dp + 60.dp))

                                    // ── Year Selection Button (Top Bar Style) ─────────────────
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 20.dp)
                                    ) {
                                        Text(
                                            text = when (val range = uiState.timeRange) {
                                                is TimeRange.AllTime -> stringResource(R.string.stats_all_time)
                                                is TimeRange.Year -> stringResource(R.string.stats_year_prefix, range.year)
                                            },
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.align(Alignment.CenterStart)
                                        )

                                        // YearSelectionButton (Glassy Filter button style)
                                        YearSelectionButton(
                                            currentRange = uiState.timeRange,
                                            onToggle = onToggleYearPicker,
                                            modifier = Modifier.align(Alignment.CenterEnd)
                                        )
                                    }

                                    val selectedYear = when (val range = uiState.timeRange) {
                                        is TimeRange.Year -> range.year
                                        is TimeRange.AllTime -> currentYear
                                    }
                                    val wrappedStats = uiState.currentYearStats
                                    val shouldShowWrapped = wrappedStats != null && (wrappedStats.moviesWatched > 0 || wrappedStats.tvWatched > 0)

                                    if (shouldShowWrapped) {
                                        val yearTitle = stringResource(R.string.stats_wrapped, selectedYear)
                                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            WrappedBannerPill(
                                                stats = wrappedStats,
                                                year = selectedYear,
                                                graphicsLayer = graphicsLayer,
                                                isSharing = isSharingStats,
                                                onShare = {
                                                    isSharingStats = true
                                                    scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                                        try {
                                                            kotlinx.coroutines.delay(150)
                                                            withFrameMillis {}
                                                            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                                            if (bitmap.width > 0 && bitmap.height > 0) {
                                                                shareBitmap(context, bitmap, yearTitle)
                                                            } else {
                                                                shareStats(context, wrappedStats, yearTitle)
                                                            }
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                            shareStats(context, wrappedStats, yearTitle)
                                                        } finally {
                                                            isSharingStats = false
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        Spacer(Modifier.height(36.dp))
                                    }

                                    // ── VISTI QUEST'ANNO (solo se filtro per anno specifico) ────
                                    if (uiState.timeRange is TimeRange.Year && uiState.moviesInSelectedRange.isNotEmpty()) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            Box(modifier = Modifier.fillMaxWidth().statsCard()) {
                                                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                                    val sectionTitle = stringResource(R.string.stats_watched_in_year, selectedYear)
                                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                                        StatsSectionHeader(
                                                            icon = ImageVector.vectorResource(id = R.drawable.ic_ciack),
                                                            title = sectionTitle,
                                                            count = uiState.moviesInSelectedRange.size
                                                        )
                                                    }
                                                    androidx.compose.foundation.lazy.LazyRow(
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                                    ) {
                                                        items(
                                                            items = uiState.moviesInSelectedRange,
                                                            key = { "${it.id}_${it.mediaType}" }
                                                        ) { movie ->
                                                            com.cinetrack.ui.components.card.MovieCard(
                                                                movie = movie,
                                                                cardWidth = 100.dp,
                                                                isWatched = movie.watched,
                                                                isFavorite = movie.favorite,
                                                                isReminder = movie.reminder,
                                                                progress = (movie.progress ?: 0.0).toFloat(),
                                                                hazeState = activeHazeState,
                                                                onPress = { onMovieClick(movie) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(36.dp))
                                    }

                                    // ── Hero: Tempo totale ────────────────────────────────────
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        TotalTimeHeroCard(
                                            totalMinutes = stats.totalMinutes
                                        )
                                    }
                                    Spacer(Modifier.height(36.dp))

                            // ── I TUOI FILM ───────────────────────────────────────────
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                StatsSectionHeader(
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_ciack),
                                    title = stringResource(R.string.stats_your_movies),
                                    count = null
                                )
                                // Single pill: DA VEDERE | VISTI
                                DualStatPill(
                                    leftLabel = stringResource(R.string.stats_to_watch),
                                    leftValue = stats.moviesToWatch,
                                    leftIcon = ImageVector.vectorResource(id = R.drawable.ic_punto_cerchiato),
                                    rightLabel = stringResource(R.string.stats_watched),
                                    rightValue = stats.moviesWatched,
                                    rightIcon = ImageVector.vectorResource(id = R.drawable.ic_trophy),
                                    accentColor = MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.height(10.dp))

                                // Combined card: tempo totale + film più lungo
                                MediaTimeCard(
                                    timeLabel = stringResource(R.string.stats_time_spent),
                                    time = stats.movieTimeFormatted,
                                    longestLabel = stringResource(R.string.stats_longest_movie),
                                    longestTitle = stats.longestMovie?.title ?: stats.longestMovie?.name,
                                    longestDurationMinutes = stats.longestMovieMinutes,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    sectionIcon = ImageVector.vectorResource(id = R.drawable.ic_ciack),
                                    longestPosterPath = stats.longestMovie?.posterPath,
                                    onLongestItemClick = {
                                        stats.longestMovie?.let { onMovieClick(it) }
                                    }
                                )
                            }
                            Spacer(Modifier.height(36.dp))

                            // ── SERIE TV ──────────────────────────────────────────────
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                StatsSectionHeader(
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_tv),
                                    title = stringResource(R.string.stats_tv_series),
                                    count = null
                                )
                                // Single pill: DA VEDERE | COMPLETATE
                                DualStatPill(
                                    leftLabel = stringResource(R.string.stats_to_watch),
                                    leftValue = stats.tvToWatch,
                                    leftIcon = ImageVector.vectorResource(id = R.drawable.ic_punto_cerchiato),
                                    rightLabel = stringResource(R.string.stats_completed),
                                    rightValue = stats.tvWatched,
                                    rightIcon = ImageVector.vectorResource(id = R.drawable.ic_trophy),
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    rightSuffix = if (stats.totalEpisodes > 0) stringResource(R.string.stats_episodes_short, stats.totalEpisodes) else null
                                )

                                Spacer(Modifier.height(10.dp))

                                MediaTimeCard(
                                    timeLabel = stringResource(R.string.stats_time_spent),
                                    time = stats.tvTimeFormatted,
                                    longestLabel = stringResource(R.string.stats_longest_tv),
                                    longestTitle = stats.longestTV?.title ?: stats.longestTV?.name,
                                    longestDurationMinutes = stats.longestTVMinutes,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    sectionIcon = ImageVector.vectorResource(id = R.drawable.ic_tv),
                                    longestSuffix = stats.longestTV?.let {
                                        val eps = it.watchedEpisodes?.values?.sumOf { s -> s.size } ?: 0
                                        if (eps > 0) stringResource(R.string.stats_episodes_long, eps) else null
                                    },
                                    longestPosterPath = stats.longestTV?.posterPath,
                                    onLongestItemClick = {
                                        stats.longestTV?.let { onMovieClick(it) }
                                    }
                                )
                            }
                            Spacer(Modifier.height(36.dp))

                            if (stats.topCast.isNotEmpty()) {
                                var castExpanded by rememberSaveable { mutableStateOf(false) }
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    StatsSectionHeader(
                                        icon = ImageVector.vectorResource(id = R.drawable.ic_trophy),
                                        title = stringResource(R.string.stats_top_cast),
                                        count = null
                                    )
                                }
                                PersonSection(
                                    people = stats.topCast,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    isExpanded = castExpanded,
                                    onPersonClick = onPersonClick
                                )
                                Spacer(Modifier.height(36.dp))
                            }

                            if (stats.topDirectors.isNotEmpty()) {
                                var dirExpanded by rememberSaveable { mutableStateOf(false) }
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    StatsSectionHeader(
                                        icon = ImageVector.vectorResource(id = R.drawable.ic_megafono),
                                        title = stringResource(R.string.stats_top_directors),
                                        count = null
                                    )
                                }
                                PersonSection(
                                    people = stats.topDirectors,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    isExpanded = dirExpanded,
                                    onPersonClick = onPersonClick
                                )
                                Spacer(Modifier.height(36.dp))
                            }

                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                                // ── Distribuzione Generi ──────────────────────────────────
                                if (stats.genreCounts.isNotEmpty()) {
                                    var genreExpanded by rememberSaveable { mutableStateOf(false) }
                                    StatsSectionHeader(
                                        icon = ImageVector.vectorResource(id = R.drawable.ic_temi),
                                        title = stringResource(R.string.stats_genre_distribution),
                                        count = null
                                    )
                                    GenreDistributionSection(
                                        genreCounts = stats.genreCounts, 
                                        expanded = genreExpanded,
                                        onToggleExpanded = { genreExpanded = !genreExpanded }
                                    )
                                    Spacer(Modifier.height(36.dp))
                                }

                                // ── Le Tue Decadi d'Oro ───────────────────────────────────
                                if (stats.decadeCounts.isNotEmpty()) {
                                    StatsSectionHeader(
                                        icon = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                                        title = stringResource(R.string.stats_top_decades),
                                        count = null
                                    )
                                    DecadesSection(decadeCounts = stats.decadeCounts)
                                    Spacer(Modifier.height(36.dp))
                                }

                                // ── Distribuzione Voti ────────────────────────────────────
                                StatsSectionHeader(
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_star),
                                    title = stringResource(R.string.stats_rating_distribution),
                                    count = null
                                )
                                RatingHistogram(distribution = stats.ratingDistribution)
                                Spacer(Modifier.height(56.dp))
                            }
                            Spacer(Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
                        }
                    }
                }
            }
        }

        // Premium loading overlay for sharing
        val sharingAlpha by animateFloatAsState(
            targetValue = if (isSharingStats) 1f else 0f,
            animationSpec = tween(300),
            label = "sharingAlpha"
        )

        if (sharingAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                // Scrim
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f * sharingAlpha))
                        .pointerInput(Unit) {
                            detectTapGestures { /* consume clicks */ }
                        }
                )

                Card(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth(0.85f)
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .hazeGlass(
                                    state = null, // Disabilita haze per risolvere il bug del testo sfocato
                                    shape = RoundedCornerShape(24.dp),
                                    containerColor = DarkSurface.copy(alpha = 0.45f),
                                    alpha = sharingAlpha,
                                    useOffscreenStrategy = true
                                )
                        )

                        // Sibling 2: The crisp content card (defines the parent size, completely untouched by blur!)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = sharingAlpha
                                }
                                .padding(horizontal = 28.dp, vertical = 32.dp)
                        ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.stats_generating_card),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.stats_generating_card_desc),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    }
                }
            }
        }
    }
}
}
}
