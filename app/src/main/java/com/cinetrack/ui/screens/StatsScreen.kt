package com.cinetrack.ui.screens

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
import com.cinetrack.data.Movie
import com.cinetrack.ui.utils.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.LayoutCoordinates
import com.cinetrack.ui.components.*
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

// ──────────────────────────────────────────────────────────────────────
// Custom Stats Card Modifier (No Blur)
// ──────────────────────────────────────────────────────────────────────
private fun Modifier.statsCard(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp)
): Modifier = this.then(
    Modifier
        .background(Color.White.copy(alpha = 0.06f), shape)
        .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
        .clip(shape)
)

class TicketShape(
    private val cutoutRadius: androidx.compose.ui.unit.Dp,
    private val cornerRadius: androidx.compose.ui.unit.Dp = 32.dp
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            val rPx = with(density) { cutoutRadius.toPx() }.coerceAtMost(size.height / 3.5f)
            val crPx = with(density) { cornerRadius.toPx() }.coerceAtMost(size.height / 2.2f)
            
            // Further safety: ensure corner + cutout don't overlap with a small gap
            val safeRPx = if (crPx + rPx > size.height / 2f) {
                (size.height / 2f - crPx).coerceAtLeast(size.height * 0.05f)
            } else {
                rPx
            }
            
            // Start top-left
            moveTo(crPx, 0f)
            lineTo(size.width - crPx, 0f)
            // Top-right corner
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(size.width - 2 * crPx, 0f, size.width, 2 * crPx),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            
            // Right edge cutout
            lineTo(size.width, size.height / 2f - safeRPx)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(size.width - safeRPx, size.height / 2f - safeRPx, size.width + safeRPx, size.height / 2f + safeRPx),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            
            lineTo(size.width, size.height - crPx)
            // Bottom-right corner
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(size.width - 2 * crPx, size.height - 2 * crPx, size.width, size.height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            
            lineTo(crPx, size.height)
            // Bottom-left corner
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(0f, size.height - 2 * crPx, 2 * crPx, size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            
            // Left edge cutout
            lineTo(0f, size.height / 2f + safeRPx)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(-safeRPx, size.height / 2f - safeRPx, safeRPx, size.height / 2f + safeRPx),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            
            lineTo(0f, crPx)
            // Top-left corner
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(0f, 0f, 2 * crPx, 2 * crPx),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    paddingValues: PaddingValues,
    onToggleYearPicker: (Boolean, androidx.compose.ui.geometry.Rect?) -> Unit = { _, _ -> },
    onPersonClick: (Long) -> Unit = {},
    onMovieClick: (Movie) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    val context = LocalContext.current
    
    // Local haze state to isolate background blur from foreground content
    val localHazeState = remember { HazeState() }
    var isSharingStats by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Background & Content Layer (Captured by Haze for Modals) ──────
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Contenuto da catturare (solo lo sfondo e la colonna principale)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(localHazeState, style = HazeStyles.PremiumDark)
            ) {
                // Darker background to make bars look "scure" (dark)
                Box(Modifier.fillMaxSize().background(Color(0xFF050507)))
                
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
                                    Text("Nessuna statistica disponibile", color = Color.White.copy(alpha = 0.5f))
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
                                    Spacer(Modifier.height(paddingValues.calculateTopPadding()))

                                    // ── Year Selection Button (Top Bar Style) ─────────────────
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 20.dp)
                                    ) {
                                        Text(
                                            text = when (val range = uiState.timeRange) {
                                                is TimeRange.AllTime -> "Tutte le Statistiche"
                                                is TimeRange.Year -> "Statistiche ${range.year}"
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

                                    val isYearRange = uiState.timeRange is TimeRange.Year
                                    val selectedYear = if (isYearRange) (uiState.timeRange as TimeRange.Year).year else currentYear
                                    val safeStats = uiState.stats
                                    val shouldShowWrapped = safeStats != null && (safeStats.moviesWatched > 0 || safeStats.tvWatched > 0)

                                    if (shouldShowWrapped) {
                                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                            WrappedBannerPill(
                                                stats = safeStats,
                                                year = selectedYear,
                                                graphicsLayer = graphicsLayer,
                                                isSharing = isSharingStats,
                                                onShare = {
                                                    val yearTitle = "$selectedYear WRAPPED"
                                                    isSharingStats = true
                                                    scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                                        try {
                                                            kotlinx.coroutines.delay(150)
                                                            withFrameMillis {}
                                                            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                                            if (bitmap.width > 0 && bitmap.height > 0) {
                                                                shareBitmap(context, bitmap, yearTitle)
                                                            } else {
                                                                shareStats(context, safeStats, yearTitle)
                                                            }
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                            shareStats(context, safeStats, yearTitle)
                                                        } finally {
                                                            isSharingStats = false
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        Spacer(Modifier.height(36.dp))
                                    }

                                    // ── VISTI QUEST'ANNO ────────────────────────────────────
                                    if (isYearRange && uiState.moviesInSelectedRange.isNotEmpty()) {
                                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                            Box(modifier = Modifier.fillMaxWidth().statsCard()) {
                                                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                                    val sectionTitle = "VISTI NEL $selectedYear"
                                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                                        StatsSectionHeader(
                                                            icon = Icons.Rounded.Movie,
                                                            title = sectionTitle,
                                                            count = uiState.moviesInSelectedRange.size
                                                        )
                                                    }
                                                    androidx.compose.foundation.lazy.LazyRow(
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                                    ) {
                                                        items(uiState.moviesInSelectedRange.size) { index ->
                                                            val movie = uiState.moviesInSelectedRange[index]
                                                            com.cinetrack.ui.components.MovieCard(
                                                                movie = movie,
                                                                cardWidth = 100.dp,
                                                                isWatched = movie.watched,
                                                                isFavorite = movie.favorite,
                                                                isReminder = movie.reminder,
                                                                progress = (movie.progress ?: 0.0).toFloat(),
                                                                hazeState = localHazeState,
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
                                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                        TotalTimeHeroCard(
                                            totalMinutes = stats.totalMinutes
                                        )
                                    }
                                    Spacer(Modifier.height(36.dp))
                                    // ... the rest of the column will be handled by the replacement end range

                            // ── I TUOI FILM ───────────────────────────────────────────
                            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                StatsSectionHeader(
                                    icon = Icons.Rounded.Movie,
                                    title = "I TUOI FILM",
                                    count = null
                                )
                                // Single pill: DA VEDERE | VISTI
                                DualStatPill(
                                    leftLabel = "DA VEDERE",
                                    leftValue = stats.moviesToWatch,
                                    leftIcon = Icons.Rounded.Adjust,
                                    rightLabel = "VISTI",
                                    rightValue = stats.moviesWatched,
                                    rightIcon = Icons.Rounded.EmojiEvents,
                                    accentColor = MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.height(10.dp))

                                // Combined card: tempo totale + film più lungo
                                MediaTimeCard(
                                    timeLabel = "Tempo speso:",
                                    time = stats.movieTimeFormatted,
                                    longestLabel = "FILM PIÙ LUNGO",
                                    longestTitle = stats.longestMovie?.title ?: stats.longestMovie?.name,
                                    longestDurationMinutes = stats.longestMovieMinutes,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    sectionIcon = Icons.Rounded.Movie,
                                    longestPosterPath = stats.longestMovie?.posterPath
                                )
                            }
                            Spacer(Modifier.height(36.dp))

                            // ── SERIE TV ──────────────────────────────────────────────
                            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                StatsSectionHeader(
                                    icon = Icons.Rounded.Tv,
                                    title = "SERIE TV",
                                    count = null
                                )
                                // Single pill: DA VEDERE | COMPLETATE
                                DualStatPill(
                                    leftLabel = "DA VEDERE",
                                    leftValue = stats.tvToWatch,
                                    leftIcon = Icons.Rounded.Adjust,
                                    rightLabel = "COMPLETATE",
                                    rightValue = stats.tvWatched,
                                    rightIcon = Icons.Rounded.EmojiEvents,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    rightSuffix = if (stats.totalEpisodes > 0) "${stats.totalEpisodes} ep" else null
                                )

                                Spacer(Modifier.height(10.dp))

                                MediaTimeCard(
                                    timeLabel = "Tempo speso:",
                                    time = stats.tvTimeFormatted,
                                    longestLabel = "SERIE PIÙ LUNGA",
                                    longestTitle = stats.longestTV?.title ?: stats.longestTV?.name,
                                    longestDurationMinutes = stats.longestTVMinutes,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    sectionIcon = Icons.Rounded.Tv,
                                    longestSuffix = stats.longestTV?.let {
                                        val eps = it.watchedEpisodes?.values?.sumOf { s -> s.size } ?: 0
                                        if (eps > 0) "• $eps episodi" else null
                                    },
                                    longestPosterPath = stats.longestTV?.posterPath
                                )
                            }
                            Spacer(Modifier.height(36.dp))

                            if (stats.topCast.isNotEmpty()) {
                                var castExpanded by rememberSaveable { mutableStateOf(false) }
                                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    StatsSectionHeader(
                                        icon = Icons.Rounded.EmojiEvents,
                                        title = "I TUOI MITI (CAST)",
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
                                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    StatsSectionHeader(
                                        icon = Icons.Rounded.BarChart,
                                        title = "TOP REGISTI",
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

                            Column(modifier = Modifier.padding(horizontal = 24.dp)) {

                                // ── Distribuzione Generi ──────────────────────────────────
                                if (stats.genreCounts.isNotEmpty()) {
                                    var genreExpanded by rememberSaveable { mutableStateOf(false) }
                                    StatsSectionHeader(
                                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                                        title = "DISTRIBUZIONE GENERI",
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
                                        icon = Icons.Rounded.DateRange,
                                        title = "LE TUE DECADI D'ORO",
                                        count = null
                                    )
                                    DecadesSection(decadeCounts = stats.decadeCounts)
                                    Spacer(Modifier.height(36.dp))
                                }

                                // ── Distribuzione Voti ────────────────────────────────────
                                StatsSectionHeader(
                                    icon = Icons.Rounded.Star,
                                    title = "DISTRIBUZIONE VOTI",
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
                            text = "Generazione Card...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Stiamo creando la tua immagine da condividere",
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

// ════════════════════════════════════════════════════════════════════
// Total Time Hero
// ════════════════════════════════════════════════════════════════════

@Composable
fun TotalTimeHeroCard(totalMinutes: Int) {
    var currentMinutes by remember { mutableStateOf(0) }
    LaunchedEffect(totalMinutes) {
        currentMinutes = totalMinutes
    }
    
    val animatedMinutes by animateIntAsState(
        targetValue = currentMinutes,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "totalMinutes"
    )

    // Format minutes on the fly for the animation effect
    val d = animatedMinutes / 1440
    val h = (animatedMinutes % 1440) / 60
    val m = animatedMinutes % 60
    val timeFormatted = when {
        d > 0 -> "${d}g ${h}h ${m}m"
        h > 0 -> "${h}h ${m}m"
        else -> "${m}m"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statsCard(RoundedCornerShape(24.dp))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AccessTime, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "TEMPO TOTALE TRASCORSO",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp
                    )
                }
                
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(
                    onClick = {
                        val days = totalMinutes / 1440
                        val hours = (totalMinutes % 1440) / 60
                        val minutes = totalMinutes % 60
                        val time = when {
                            days > 0 -> "${days}g ${hours}h ${minutes}m"
                            hours > 0 -> "${hours}h ${minutes}m"
                            else -> "${minutes}m"
                        }
                        val text = "🏆 Il mio tempo totale su FlickTrove: $time di cinema e serie TV!\nScarica l'app per tracciare i tuoi progressi."
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Rounded.Share, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                Text(
                    timeFormatted,
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "DI VISIONE COMBINATA",
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Section Header
// ════════════════════════════════════════════════════════════════════

@Composable
fun StatsSectionHeader(
    icon: ImageVector,
    title: String,
    count: Int?,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        // Icon inside a rounded pill box
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f)
        )
        
        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier = Modifier
                    .bounceClick { onActionClick() }
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (actionLabel.contains("MENO", ignoreCase = true)) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else if (count != null) {
            CountingText(target = count, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Mini stat card
// ════════════════════════════════════════════════════════════════════

@Composable
fun MiniStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .statsCard(RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(label, color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Animated counting number
// ════════════════════════════════════════════════════════════════════

@Composable
fun CountingText(
    target: Int,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    suffixFontSize: androidx.compose.ui.unit.TextUnit = fontSize,
    textAlign: TextAlign? = null
) {
    var currentValue by remember { mutableStateOf(0) }
    LaunchedEffect(target) {
        currentValue = target
    }

    val animatedValue by animateIntAsState(
        targetValue = currentValue,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "count"
    )
    if (suffix != null) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = color, fontSize = fontSize, fontWeight = fontWeight, letterSpacing = letterSpacing)) {
                    append(animatedValue.toString())
                }
                withStyle(SpanStyle(color = color.copy(alpha = 0.5f), fontSize = suffixFontSize, fontWeight = FontWeight.Medium)) {
                    append(" $suffix")
                }
            },
            modifier = modifier.wrapContentSize(unbounded = true),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            textAlign = textAlign,
            lineHeight = fontSize
        )
    } else {
        Text(
            text = animatedValue.toString(),
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            modifier = modifier,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            textAlign = textAlign,
            lineHeight = fontSize
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// Dual stat pill  (single surface, divider, two halves)
// ════════════════════════════════════════════════════════════════════

@Composable
fun DualStatPill(
    leftLabel: String,
    leftValue: Int,
    leftIcon: ImageVector,
    rightLabel: String,
    rightValue: Int,
    rightIcon: ImageVector,
    accentColor: Color,
    rightSuffix: String? = null
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .statsCard(RoundedCornerShape(20.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left half
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(leftIcon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
                Column {
                    CountingText(
                        target = leftValue,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(leftLabel, color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
            // Central vertical divider
            Box(
                Modifier
                    .width(1.dp)
                    .height(34.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            // Right half
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(rightIcon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
                Column {
                    CountingText(
                        target = rightValue,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        suffix = rightSuffix,
                        suffixFontSize = 11.sp
                    )
                    Text(rightLabel, color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Combined media time + longest card (like the reference image)
// ════════════════════════════════════════════════════════════════════

@Composable
fun MediaTimeCard(
    timeLabel: String,
    time: String,
    longestLabel: String,
    longestTitle: String?,
    longestDurationMinutes: Int,
    accentColor: Color,
    sectionIcon: ImageVector,
    longestSuffix: String? = null,
    longestPosterPath: String? = null
) {
    val h = longestDurationMinutes / 60
    val m = longestDurationMinutes % 60
    val durText = buildString {
        if (h > 0) append("${h}h ${m}m") else append("${m}m")
        if (longestSuffix != null) append(" $longestSuffix")
    }

    Box(
        modifier = Modifier.fillMaxWidth()
            .statsCard(RoundedCornerShape(28.dp))
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

            // === Top row: total time ===
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AccessTime, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(timeLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(time, color = accentColor, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.4).sp)
                }
            }

            // === Longest row (below the time) ===
            if (!longestTitle.isNullOrBlank() && longestDurationMinutes > 0) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (longestPosterPath != null) {
                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .width(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            coil.compose.AsyncImage(
                                model = buildTmdbImageUrl(longestPosterPath, ImageType.POSTER, LocalImageQuality.current),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    
                    Column(Modifier.weight(1f)) {
                        Text(
                            longestLabel,
                            color = accentColor.copy(alpha = 0.55f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            longestTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(durText, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}


// ════════════════════════════════════════════════════════════════════
// Person horizontal list (cast / directors)
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonSection(
    people: List<PersonStat>,
    accentColor: Color,
    isExpanded: Boolean,
    onPersonClick: (Long) -> Unit = {}
) {
    if (isExpanded) {
        // Full grid when expanded
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            maxItemsInEachRow = 4
        ) {
            people.forEach { person ->
                PersonAvatar(person = person, accentColor = accentColor, onPersonClick = onPersonClick)
            }
        }
    } else {
        // Horizontal list when collapsed
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(people.take(10), key = { it.id }, contentType = { "person" }) { person ->
                PersonAvatar(person = person, accentColor = accentColor, onPersonClick = onPersonClick)
            }
        }
    }
}

@Composable
fun PersonAvatar(
    person: PersonStat,
    accentColor: Color,
    onPersonClick: (Long) -> Unit = {}
) {
    val avatarSize = 66.dp
    val badgeSize  = 22.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .bounceClick { onPersonClick(person.id) }
    ) {
        Box(modifier = Modifier.size(avatarSize)) {
            // Outer teal ring
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                if (!person.profilePath.isNullOrBlank()) {
                    AsyncImage(
                        model = buildTmdbImageUrl(person.profilePath, ImageType.PROFILE, LocalImageQuality.current),
                        contentDescription = person.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = person.name.split(" ")
                                .mapNotNull { it.firstOrNull()?.toString() }
                                .take(2)
                                .joinToString(""),
                            color = accentColor.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            // Badge: teal pill with count, bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .defaultMinSize(minWidth = badgeSize)
                    .height(badgeSize)
                    .background(accentColor, RoundedCornerShape(50))
                    .border(2.dp, Color(0xFF0D1117), RoundedCornerShape(50))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                CountingText(
                    target = person.count,
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = person.name,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// Genre Distribution – Treemap
// ════════════════════════════════════════════════════════════════════

@Composable
private fun LegendItem(
    genre: String,
    count: Int,
    percentage: Int,
    color: Color,
    isSelected: Boolean,
    isDimmed: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.12f else 0.02f,
        label = "bgAlpha"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        
        Spacer(Modifier.width(14.dp))
        
        Text(
            text = genre,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) color else Color.White.copy(alpha = if (isDimmed) 0.35f else 0.9f)
            ),
            modifier = Modifier.weight(1f)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) color else Color.White.copy(alpha = if (isDimmed) 0.35f else 0.8f)
                )
            )
            Spacer(Modifier.width(8.dp))
            val percentageText = if (percentage == 0 && count > 0) "<1%" else "$percentage%"
            Text(
                text = percentageText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = if (isSelected) 0.5f else 0.25f)
                )
            )
        }
    }
}

@Composable
fun GenreDistributionSection(
    genreCounts: List<Pair<String, Int>>, 
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    if (genreCounts.isEmpty()) return
    
    val textMeasurer = rememberTextMeasurer()
    
    val totalCount = genreCounts.sumOf { it.second }
    
    val processedGenres = remember(genreCounts) {
        if (genreCounts.size <= 8) {
            genreCounts
        } else {
            val mainGenres = genreCounts.take(7)
            val othersCount = genreCounts.drop(7).sumOf { it.second }
            mainGenres + Pair("ALTRO", othersCount)
        }
    }
    
    var selectedGenreName by remember { mutableStateOf<String?>(null) }
    
    val genreColors = listOf(
        Color(0xFF00F2FE), // Cyan Neon
        Color(0xFF4FACFE), // Blue Neon
        Color(0xFFF355DA), // Magenta Neon
        Color(0xFFFFB300), // Gold/Amber Neon
        Color(0xFF00FF87), // Green Neon
        Color(0xFFFF4F4F), // Red Neon
        Color(0xFF9D50BB), // Purple
        Color(0xFFFF8C00), // Orange
        Color(0xFF00E676), // Bright Green
        Color(0xFFD500F9), // Deep Purple
        Color(0xFFE040FB), // Light Purple
        Color(0xFF18FFFF)  // Cyan 2
    )
    
    val chartSelectedIndex = remember(selectedGenreName, processedGenres) {
        if (selectedGenreName == null) {
            -1
        } else {
            val idx = processedGenres.indexOfFirst { it.first == selectedGenreName }
            if (idx != -1) {
                idx
            } else {
                processedGenres.lastIndex
            }
        }
    }
    
    val animOffsets = processedGenres.mapIndexed { idx, _ ->
        animateFloatAsState(
            targetValue = if (chartSelectedIndex == idx) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "offset_$idx"
        )
    }
    
    val initialAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "initialAnim"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    val animatedHaloColor by animateColorAsState(
        targetValue = if (selectedGenreName != null && chartSelectedIndex != -1) {
            genreColors[chartSelectedIndex % genreColors.size]
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(500),
        label = "haloColor"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statsCard(RoundedCornerShape(24.dp))
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The internal title "DISTRIBUZIONE GENERI" was removed here to avoid redundancy
            
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val strokeWidthPx = with(density) { 28.dp.toPx() }
                val donutRadius = with(density) { 118.dp.toPx() }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(processedGenres, initialAnim) {
                            val canvasSize = size
                            detectTapGestures { offset ->
                                val canvasWidth = canvasSize.width
                                val canvasHeight = canvasSize.height
                                val centerX = canvasWidth / 2f
                                val centerY = canvasHeight / 2f
                                val dx = offset.x - centerX
                                val dy = offset.y - centerY
                                val distance = sqrt(dx * dx + dy * dy)
                                
                                val outerRadius = donutRadius
                                val innerRadius = outerRadius - strokeWidthPx
                                
                                if (distance in innerRadius..outerRadius) {
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f
                                    val normalizedAngle = (angle + 90f) % 360f
                                    
                                    var currentAngle = 0f
                                    var foundIndex = -1
                                    for (i in processedGenres.indices) {
                                        val sweep = (processedGenres[i].second.toFloat() / totalCount) * 360f
                                        if (normalizedAngle >= currentAngle && normalizedAngle < currentAngle + sweep) {
                                            foundIndex = i
                                            break
                                        }
                                        currentAngle += sweep
                                    }
                                    
                                    if (foundIndex != -1) {
                                        val clickedGenre = processedGenres[foundIndex].first
                                        selectedGenreName = if (selectedGenreName == clickedGenre) null else clickedGenre
                                    }
                                } else {
                                    selectedGenreName = null
                                }
                            }
                        }
                ) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    var currentStartAngle = -90f
                    
                    processedGenres.forEachIndexed { index, (genre, count) ->
                        val sweepAngle = (count.toFloat() / totalCount) * 360f * initialAnim
                        val isSelected = (chartSelectedIndex == index)
                        val selectionOffset = animOffsets[index].value * 8.dp.toPx()
                        
                        val midAngle = currentStartAngle + sweepAngle / 2f
                        val rad = Math.toRadians(midAngle.toDouble())
                        val cosVal = cos(rad).toFloat()
                        val sinVal = sin(rad).toFloat()
                        val offsetX = cosVal * selectionOffset
                        val offsetY = sinVal * selectionOffset
                        
                        val currentStrokeWidth = if (isSelected) strokeWidthPx + 4.dp.toPx() else strokeWidthPx
                        val color = genreColors[index % genreColors.size]
                        val alpha = if (chartSelectedIndex == -1 || isSelected) 1f else 0.25f
                        
                        val pathRadius = donutRadius - strokeWidthPx / 2f
                        val arcSize = androidx.compose.ui.geometry.Size(pathRadius * 2f, pathRadius * 2f)
                        val arcTopLeft = Offset(centerX - pathRadius + offsetX, centerY - pathRadius + offsetY)
                        
                        if (isSelected) {
                            drawArc(
                                color = color.copy(alpha = 0.25f),
                                startAngle = currentStartAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = currentStrokeWidth + 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        
                        drawArc(
                            color = color.copy(alpha = alpha),
                            startAngle = currentStartAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = currentStrokeWidth, cap = if (isSelected) StrokeCap.Round else StrokeCap.Butt)
                        )
                        
                        // Draw genre text label directly next to donut slice
                        val percentage = if (totalCount > 0) (count.toFloat() / totalCount * 100).roundToInt() else 0
                        val percentageText = if (percentage == 0 && count > 0) "<1%" else "$percentage%"
                        if (sweepAngle > 8f && count > 0) {
                            val textRadius = donutRadius + 12.dp.toPx() + selectionOffset
                            val textX = centerX + cosVal * textRadius
                            val textY = centerY + sinVal * textRadius
                            
                            val titleCasedGenre = genre.lowercase().replaceFirstChar { it.uppercase() }
                            val labelText = "$titleCasedGenre $percentageText"
                            
                            val textLayoutResult = textMeasurer.measure(
                                text = labelText,
                                style = androidx.compose.ui.text.TextStyle(
                                    color = color.copy(alpha = alpha),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            val textWidth = textLayoutResult.size.width
                            val textHeight = textLayoutResult.size.height
                            
                            val x = if (cosVal > 0.2f) {
                                textX
                            } else if (cosVal < -0.2f) {
                                textX - textWidth
                            } else {
                                textX - textWidth / 2f
                            }
                            
                            val y = if (sinVal > 0.2f) {
                                textY
                            } else if (sinVal < -0.2f) {
                                textY - textHeight
                            } else {
                                textY - textHeight / 2f
                            }
                            
                            // Leader line connecting the arc outer boundary to the label
                            val lineStartX = centerX + cosVal * (donutRadius + 2.dp.toPx() + selectionOffset)
                            val lineStartY = centerY + sinVal * (donutRadius + 2.dp.toPx() + selectionOffset)
                            val lineEndX = centerX + cosVal * (donutRadius + 8.dp.toPx() + selectionOffset)
                            val lineEndY = centerY + sinVal * (donutRadius + 8.dp.toPx() + selectionOffset)
                            
                            drawLine(
                                color = color.copy(alpha = alpha * 0.4f),
                                start = Offset(lineStartX, lineStartY),
                                end = Offset(lineEndX, lineEndY),
                                strokeWidth = 1.dp.toPx()
                            )
                            
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(x, y)
                            )
                        }
                        
                        currentStartAngle += sweepAngle
                    }
                }
                
                // 1. Organic Breathing Inner Neon Halo
                Box(
                    modifier = Modifier
                        .size(165.dp)
                        .graphicsLayer {
                            scaleX = breathingScale
                            scaleY = breathingScale
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    animatedHaloColor.copy(alpha = breathingAlpha),
                                    animatedHaloColor.copy(alpha = 0f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // 2. Stunning Glowing Frosted Glass Circle Card
                Box(
                    modifier = Modifier
                        .size(158.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F0F1A).copy(alpha = 0.75f))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    animatedHaloColor.copy(alpha = 0.5f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        val genreName = selectedGenreName
                        if (genreName != null) {
                            val count = if (genreName == "ALTRO") {
                                processedGenres.firstOrNull { it.first == "ALTRO" }?.second ?: 0
                            } else {
                                genreCounts.firstOrNull { it.first == genreName }?.second ?: 0
                            }
                            val percentage = ((count.toFloat() / totalCount) * 100).roundToInt()
                            val percentageText = if (percentage == 0 && count > 0) "<1%" else "$percentage%"
                            val countColor = if (chartSelectedIndex != -1) {
                                genreColors[chartSelectedIndex % genreColors.size]
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                            
                            Text(
                                text = genreName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.5.sp,
                                    fontSize = 9.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            CountingText(
                                target = count,
                                color = countColor,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (count == 1) "TITOLO" else "TITOLI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = countColor.copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "$percentageText DEL TOTALE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 9.sp
                                )
                            )
                        } else {
                            Text(
                                text = "GENERI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.5.sp,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            CountingText(
                                target = genreCounts.size,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "VISTI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "TOCCA UNA FETTA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    fontSize = 8.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (expanded) 320.dp else 200.dp)
            ) {
                val scrollState = rememberScrollState()
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            if (scrollState.value > 0) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.15f to Color.Black,
                                        startY = 0f,
                                        endY = size.height * 0.15f
                                    ),
                                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                                )
                            }
                            if (scrollState.value < scrollState.maxValue) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.85f to Color.Black,
                                        1f to Color.Transparent,
                                        startY = size.height * 0.75f,
                                        endY = size.height
                                    ),
                                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                                )
                            }
                        }
                        .verticalScroll(scrollState)
                        .padding(vertical = 12.dp)
                ) {
                    val displayList = if (expanded) genreCounts else genreCounts.take(7)
                    
                    displayList.forEachIndexed { index, (genre, count) ->
                        val percentage = ((count.toFloat() / totalCount) * 100).roundToInt()
                        
                        val color = if (index < 7) genreColors[index] else genreColors[7]
                        
                        val isItemSelected = (selectedGenreName == genre)
                        val isDimmed = selectedGenreName != null && !isItemSelected
                        
                        LegendItem(
                            genre = genre,
                            count = count,
                            percentage = percentage,
                            color = color,
                            isSelected = isItemSelected,
                            isDimmed = isDimmed,
                            onClick = {
                                selectedGenreName = if (isItemSelected) null else genre
                            }
                        )
                        if (index < displayList.lastIndex) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                
                if (expanded) {
                    val scrollFraction by remember {
                        derivedStateOf { if (scrollState.maxValue > 0) scrollState.value.toFloat() / scrollState.maxValue else 0f }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, bottom = 10.dp, end = 4.dp)
                            .fillMaxHeight()
                            .width(3.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.2f)
                                .offset(y = (240 * scrollFraction).dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
            }
            
            if (genreCounts.size > 5) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .bounceClick(onClick = { onToggleExpanded() })
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (expanded) "VEDI MENO" else "VEDI TUTTI",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Decades bar chart
// ════════════════════════════════════════════════════════════════════

@Composable
fun DecadesSection(decadeCounts: List<Pair<String, Int>>) {
    val max = decadeCounts.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val totalMovies = remember(decadeCounts) { decadeCounts.sumOf { it.second } }
    var selectedDecadeIdx by remember { mutableStateOf<Int?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statsCard(RoundedCornerShape(20.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            decadeCounts.forEachIndexed { idx, (decade, count) ->
                val fraction = count.toFloat() / max
                val isMax = count.toFloat() == max
                val animFrac by animateFloatAsState(fraction, tween(1000, idx * 80, FastOutSlowInEasing), label = "dec$idx")
                
                val isSelected = selectedDecadeIdx == idx
                val glowColor = if (isMax) MaterialTheme.colorScheme.primary else Color(0xFF00F2FE)
                
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "barScale_$idx"
                )
                
                val percentage = if (totalMovies > 0) (count.toFloat() / totalMovies * 100).roundToInt() else 0
                val percentageText = if (percentage == 0 && count > 0) "<1%" else "$percentage%"
                
                Column(
                    Modifier
                        .zIndex(if (isSelected) 1f else 0f)
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedDecadeIdx = if (isSelected) null else idx
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(0.5f, 1f)),
                            exit = fadeOut() + scaleOut(transformOrigin = TransformOrigin(0.5f, 1f)),
                            modifier = Modifier.zIndex(10f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(unbounded = true)
                                    .offset(y = (-4).dp)
                                    .background(
                                        Color(0xFF0F0F1A).copy(alpha = 0.9f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        1.dp,
                                        glowColor.copy(alpha = 0.4f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = percentageText,
                                        color = glowColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = if (count == 1) "1 film" else "$count film",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isSelected,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            CountingText(
                                target = count,
                                color = if (isMax) MaterialTheme.colorScheme.primary else Color.White.copy(0.7f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(3.dp))
                    
                    Box(
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(90.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0.5f, 1.0f)
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val minHeight = if (count > 0) 0.06f else 0.0f
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animFrac.coerceAtLeast(minHeight))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            if (isSelected) glowColor else if (isMax) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(0.45f),
                                            if (isSelected) glowColor.copy(0.3f) else if (isMax) MaterialTheme.colorScheme.primary.copy(0.3f) else MaterialTheme.colorScheme.primary.copy(0.1f)
                                        )
                                    ),
                                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) glowColor else Color.Transparent,
                                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                                )
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        decade,
                        color = if (isSelected) glowColor else Color.White.copy(0.55f),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Wrapped Banner Pill Component
// ════════════════════════════════════════════════════════════════════

@Composable
fun WrappedBannerPill(
    stats: CalculatedStats,
    year: Int,
    graphicsLayer: androidx.compose.ui.graphics.layer.GraphicsLayer,
    isSharing: Boolean = false,
    onShare: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    // Get theme colors in composable context
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                // Draw the content normally on screen
                drawContent()
                
                // Record content for sharing with 3x scale for high quality
                val scaleFactor = 3f
                graphicsLayer.record(
                    size = androidx.compose.ui.unit.IntSize(
                        (size.width * scaleFactor).toInt(), 
                        (size.height * scaleFactor).toInt()
                    )
                ) {
                    scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
                        this@drawWithContent.drawContent()
                    }
                }
            }
            .graphicsLayer {
                // Use block version for compatibility
                clip = true
                shape = TicketShape(
                    cutoutRadius = if (expanded) 16.dp else 10.dp,
                    cornerRadius = if (expanded) 32.dp else 18.dp
                )
            }
            .background(Color(0xFF0A0A0C))
            .animateContentSize(
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .drawBehind {
                // Use drawBehind for better compatibility and performance
                drawRect(color = Color(0xFF0A0A0C))
                
                // Nebula Mesh Effect
                drawRect(
                    Brush.radialGradient(
                        0.0f to primary.copy(alpha = 0.7f),
                        1.0f to Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(this.size.width * 0.15f, this.size.height * 0.2f),
                        radius = this.size.maxDimension * 0.85f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        0.0f to secondary.copy(alpha = 0.7f),
                        1.0f to Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(this.size.width * 0.85f, this.size.height * 0.45f),
                        radius = this.size.maxDimension * 0.75f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        0.0f to tertiary.copy(alpha = 0.75f),
                        1.0f to Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(this.size.width * 0.45f, this.size.height * 0.85f),
                        radius = this.size.maxDimension * 0.95f
                    )
                )
            }
            .border(
                width = 1.2.dp, 
                color = Color.White.copy(alpha = 0.2f), 
                shape = TicketShape(
                    cutoutRadius = if (expanded) 16.dp else 10.dp,
                    cornerRadius = if (expanded) 32.dp else 18.dp
                )
            )
    ) {
        // Subtle animated background particles
        Canvas(modifier = Modifier.matchParentSize()) {
            val count = 20
            repeat(count) { i ->
                val x = (i * 15345.67f) % size.width
                val y = (i * 88765.43f) % size.height
                val alpha = 0.1f + (i % 5) * 0.02f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = 2f + (i % 4),
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = if (expanded) 32.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {

                    Text(
                        text = "$year WRAPPED",
                        style = (if (expanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall).copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                if (!isSharing) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }



            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.95f, animationSpec = tween(400)),
                exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.95f, animationSpec = tween(300))
            ) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    
                    // Main Hero Stat - Total Movies
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (stats.moviesWatched + stats.tvWatched).toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 72.sp,
                                color = Color.White,
                                letterSpacing = (-2).sp
                            )
                        )
                        Text(
                            text = "TITOLI VISTI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 2.sp,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(28.dp))
                    
                    // Two Main Stats Row - Genere + Tempo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WrappedMainStat("GENERE TOP", stats.topGenre ?: "---", Icons.Rounded.AutoAwesome, Modifier.weight(1f))
                        WrappedMainStat("TEMPO TOTALE", stats.totalTimeFormatted, Icons.Rounded.AccessTime, Modifier.weight(1f))
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    // Highlight Box - Top Actor
                    val topActor = stats.topCast.firstOrNull()
                    if (topActor != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TicketShape(cutoutRadius = 8.dp, cornerRadius = 24.dp))
                                .background(
                                    Brush.verticalGradient(
                                        0f to primary.copy(alpha = 0.3f),
                                        1f to secondary.copy(alpha = 0.1f)
                                    )
                                )
                                .border(1.2.dp, Color.White.copy(alpha = 0.15f), TicketShape(cutoutRadius = 8.dp, cornerRadius = 24.dp))
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Actor Image
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!topActor.profilePath.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = buildTmdbImageUrl(topActor.profilePath, ImageType.PROFILE, LocalImageQuality.current),
                                            contentDescription = topActor.name,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(50)),
                                            contentScale = ContentScale.Crop,
                                            onError = { /* fallback to icon */ }
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "2026 STAR",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White.copy(alpha = 0.5f),
                                            letterSpacing = 1.5.sp,
                                            fontSize = 8.sp
                                        )
                                    )
                                    Text(
                                        text = topActor.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    // Secondary Stats - Film, Serie, Star Preferita, Regista Top
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WrappedSmallStat("FILM", stats.moviesWatched.toString(), Icons.Rounded.Movie, Modifier.weight(1f))
                            WrappedSmallStat("SERIE", stats.tvWatched.toString(), Icons.Rounded.Tv, Modifier.weight(1f))
                            WrappedSmallStat("REGISTA TOP", stats.topDirectors.firstOrNull()?.name ?: "---", Icons.Rounded.Videocam, Modifier.weight(2f))
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    
                    // Branding
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.cinetrack.R.drawable.ic_launcher_foreground_vector),
                                contentDescription = "Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp).offset(y = (-1).dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "FLICKTROVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 3.5.sp,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Text(
                            text = "THE CINEPHILE'S LEGACY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Light,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 2.sp,
                                fontSize = 8.sp
                            )
                        )
                    }

                    if (!isSharing) {
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onShare() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.25f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.AutoFixHigh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("CONDIVIDI LA TUA CARD", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shareBitmap(context: android.content.Context, bitmap: android.graphics.Bitmap, title: String) {
    try {
        val cachePath = java.io.File(context.cacheDir, "images")
        if (!cachePath.exists()) cachePath.mkdirs()
        
        val imageFile = java.io.File(cachePath, "wrapped_stats_${System.currentTimeMillis()}.png")
        val stream = java.io.FileOutputStream(imageFile)
        
        // Convert hardware bitmap to software before compressing
        val softwareBitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
            bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        
        softwareBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        val contentUri = androidx.core.content.FileProvider.getUriForFile(
            context, 
            "${context.packageName}.fileprovider", 
            imageFile
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, title)
            putExtra(android.content.Intent.EXTRA_TEXT, "Guarda il mio $title su FlickTrove! 🍿")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = android.content.Intent.createChooser(intent, "Condividi il tuo Wrapped")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        throw e // Rethrow to allow fallback to text sharing in the caller
    }
}

@Composable
private fun WrappedMainStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.5.sp,
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 16.sp,
                    letterSpacing = (-0.5).sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WrappedSmallStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        fontSize = 8.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = when {
                        value.length > 25 -> 9.sp
                        value.length > 18 -> 10.sp
                        value.length > 12 -> 11.sp
                        else -> 13.sp
                    },
                    lineHeight = 12.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WrappedMiniStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .drawBehind {
                // Glass shine effect
                drawRect(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.03f),
                        1f to Color.Transparent,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                    )
                )
            }
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.2.sp,
                        fontSize = 9.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = if (value.length > 12) 14.sp else 18.sp,
                    letterSpacing = (-0.5).sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}




@Composable
fun TimeRangePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
        border = if (isSelected) null else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.animateContentSize()
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// Rating Histogram
// Buckets: index 0 = 0.5★, index 1 = 1.0★, ..., index 19 = 10.0★
// Labels below: 0.5, 1, 1.5, 2 ... 10  (every half point)
// Count above each bar
// ═════════════════════════════════════════════════════════════@Composable
@Composable
fun RatingHistogram(distribution: ImmutableList<Int>) {
    val maxCount = distribution.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val totalRatedMovies = remember(distribution) { distribution.sum() }
    var selectedRatingIdx by remember { mutableStateOf<Int?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statsCard(RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (i in 1..20) {
                    val idx = i - 1
                    val count = if (idx < distribution.size) distribution[idx] else 0
                    val fraction = count / maxCount
                    val isMax = count.toFloat() == maxCount
                    
                    val animFrac by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(durationMillis = 1000, delayMillis = i * 30),
                        label = "ratingBar_$i"
                    )

                    val isSelected = selectedRatingIdx == idx
                    val glowColor = if (isMax) MaterialTheme.colorScheme.primary else Color(0xFF00F2FE)
                    
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.25f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "ratingScale_$i"
                    )

                    val percentage = if (totalRatedMovies > 0) (count.toFloat() / totalRatedMovies * 100).roundToInt() else 0

                    Column(
                        modifier = Modifier
                            .zIndex(if (isSelected) 1f else 0f)
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                selectedRatingIdx = if (isSelected) null else idx
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // The Bar
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .align(Alignment.BottomCenter)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = 1.0f
                                        transformOrigin = TransformOrigin(0.5f, 1.0f)
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                val minHeight = if (count > 0) 0.08f else 0.0f
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animFrac.coerceAtLeast(minHeight))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    if (isSelected) glowColor else if (isMax) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                                    if (isSelected) glowColor.copy(0.3f) else if (isMax) MaterialTheme.colorScheme.primary.copy(0.3f) else MaterialTheme.colorScheme.primary.copy(0.1f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) glowColor else Color.Transparent,
                                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                            }

                            // Peak count (when not selected)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !isSelected,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = - (100.dp * animFrac.coerceAtLeast(if (count > 0) 0.08f else 0.0f) + 6.dp))
                            ) {
                                if (count > 0 && isMax) {
                                    CountingText(
                                        target = count,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            // Tooltip (when selected)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(0.5f, 1f)),
                                exit = fadeOut() + scaleOut(transformOrigin = TransformOrigin(0.5f, 1f)),
                                modifier = Modifier
                                    .zIndex(10f)
                                    .align(Alignment.BottomCenter)
                                    .offset(y = - (100.dp * animFrac.coerceAtLeast(if (count > 0) 0.08f else 0.0f) + 6.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize(unbounded = true)
                                        .background(
                                            Color(0xFF0F0F1A).copy(alpha = 0.95f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            1.dp,
                                            glowColor.copy(alpha = 0.4f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val percentageText = if (percentage == 0 && count > 0) "<1%" else "$percentage%"
                                        Text(
                                            text = percentageText,
                                            color = glowColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = if (count == 1) "1 film" else "$count film",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(10.dp))
            
            Row(Modifier.fillMaxWidth()) {
                for (i in 1..20) {
                    val label = if (i % 2 == 0) {
                        "${i / 2}"
                    } else {
                        ""
                    }
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        color = if (i == 20) MaterialTheme.colorScheme.primary.copy(0.8f) else Color.White.copy(0.45f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Improved Loading Skeleton
// ════════════════════════════════════════════════════════════════════

@Composable
fun StatsSkeleton(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top safe area spacer
        Spacer(Modifier.height(paddingValues.calculateTopPadding()))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))

            // Year selection placeholder (Title + Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(180.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
                Box(Modifier.width(100.dp).height(36.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
            }

            Spacer(Modifier.height(20.dp))

            // Wrapped Banner Pill Placeholder (Ticket Shape)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(TicketShape(cutoutRadius = 10.dp, cornerRadius = 18.dp))
                    .shimmerEffect()
            )

            Spacer(Modifier.height(24.dp))

            // Hero Card Skeleton (Total Time)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shimmerEffect()
            )

            Spacer(Modifier.height(36.dp))

            // Sections: Film and Series TV
            repeat(2) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
                Spacer(Modifier.height(14.dp))

                // Dual Pill
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(10.dp))

                // Media Time Card (The taller one)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(36.dp))
            }

            // Cast / Director Circles Skeleton
            repeat(2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).shimmerEffect())
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.width(140.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(4) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(72.dp).clip(CircleShape).shimmerEffect())
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.width(50.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        }
                    }
                }
            }

            // Genre Distribution Placeholder
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).shimmerEffect())
                Spacer(Modifier.width(14.dp))
                Box(Modifier.width(160.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shimmerEffect()
            )
            Spacer(Modifier.height(36.dp))

            // Decades Placeholder
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).shimmerEffect())
                Spacer(Modifier.width(14.dp))
                Box(Modifier.width(150.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .shimmerEffect()
            )
            Spacer(Modifier.height(36.dp))

            // Ratings Histogram Placeholder
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).shimmerEffect())
                Spacer(Modifier.width(14.dp))
                Box(Modifier.width(140.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shimmerEffect()
            )

            Spacer(Modifier.height(80.dp))
            Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
        }
    }
}

@Composable
private fun YearSelectionButton(
    currentRange: com.cinetrack.ui.viewmodel.TimeRange,
    onToggle: (Boolean, androidx.compose.ui.geometry.Rect?) -> Unit,
    modifier: Modifier = Modifier
) {
    var buttonCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .height(40.dp)
            .onGloballyPositioned { buttonCoords = it }
            .bounceClick {
                val rect = buttonCoords?.let {
                    val pos = it.positionInWindow()
                    androidx.compose.ui.geometry.Rect(
                        pos.x,
                        pos.y,
                        pos.x + it.size.width,
                        pos.y + it.size.height
                    )
                }
                onToggle(true, rect)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
        )
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (currentRange) {
                    is com.cinetrack.ui.viewmodel.TimeRange.AllTime -> "Tutto"
                    is com.cinetrack.ui.viewmodel.TimeRange.Year -> currentRange.year.toString()
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Classic Centered Modal for Year Selection.
 * Standardized to match HomeFilterModal's glassmorphic design.
 */
@Composable
fun YearSelectionModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    currentRange: TimeRange,
    availableYears: List<Int>,
    hazeState: HazeState,
    triggerBounds: androidx.compose.ui.geometry.Rect? = null,
    onYearSelected: (Int) -> Unit,
    onAllTimeSelected: () -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val targetWidth = (screenWidth * 0.85f).coerceAtMost(with(density) { 320.dp.toPx() })
    
    var contentHeightPx by remember { mutableStateOf(0f) }
    val topSafetyPx = with(density) { 96.dp.toPx() }
    val bottomSafetyPx = with(density) { 56.dp.toPx() }
    val maxAllowedHeight = screenHeight - topSafetyPx - bottomSafetyPx
    
    val targetHeightPx by animateFloatAsState(
        targetValue = if (contentHeightPx > 0) contentHeightPx.coerceIn(with(density) { 300.dp.toPx() }, maxAllowedHeight) 
                      else screenHeight * 0.4f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dynamicHeight"
    )

    val targetRect = androidx.compose.ui.geometry.Rect(
        left = (screenWidth - targetWidth) / 2f,
        top = (screenHeight - targetHeightPx) / 2f,
        right = (screenWidth + targetWidth) / 2f,
        bottom = (screenHeight + targetHeightPx) / 2f
    )

    val transition = updateTransition(targetState = isVisible, label = "YearModalTransition")

    androidx.activity.compose.BackHandler(enabled = isVisible) {
        onDismiss()
    }

    val progress by transition.animateFloat(
        transitionSpec = {
            if (initialState == false && targetState == true) {
                spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
            } else {
                spring(stiffness = Spring.StiffnessMedium)
            }
        },
        label = "expansionProgress"
    ) { state -> if (state) 1f else 0f }

    val alpha by transition.animateFloat(label = "scrimAlpha") { state -> if (state) 1f else 0f }

    if (transition.currentState || transition.targetState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(200f)
                .graphicsLayer(alpha = alpha)
        ) {
            // --- GHOST MEASUREMENT LAYER ---
            Box(
                modifier = Modifier
                    .width(with(density) { targetWidth.toDp() })
                    .alpha(0f)
                    .onSizeChanged { size ->
                        if (size.height > 0) contentHeightPx = size.height.toFloat()
                    }
                    .align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text("PERIODO", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(24.dp))
                    ModalItem("Tutte le statistiche", false, {})
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Column {
                        availableYears.forEach { year ->
                            ModalItem(year.toString(), false, {})
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(48.dp))
                }
            }

            val startRect = triggerBounds ?: targetRect.copy(
                left = targetRect.center.x - 20f,
                top = targetRect.center.y - 20f,
                right = targetRect.center.x + 20f,
                bottom = targetRect.center.y + 20f
            )

            val currentRect = androidx.compose.ui.geometry.lerp(startRect, targetRect, progress)
            val currentCornerRadius = androidx.compose.ui.util.lerp(
                if (triggerBounds != null) startRect.width / 2f else with(density) { 32.dp.toPx() },
                with(density) { 32.dp.toPx() },
                progress
            )

            Box(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(currentRect.left.roundToInt(), currentRect.top.roundToInt()) }
                    .size(
                        width = with(density) { currentRect.width.toDp() },
                        height = with(density) { currentRect.height.toDp() }
                    )
                    .bounceClick(scaleDown = 1f) { /* Prevent dismissal */ }
            ) {
                // Background Layer (Blurred glass)
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeGlass(
                            state = hazeState,
                            shape = RoundedCornerShape(with(density) { currentCornerRadius.toDp() }),
                            containerColor = DarkSurface.copy(alpha = 0.45f),
                            useOffscreenStrategy = false
                        )
                )

                // Foreground Content
                if (progress > 0.4f) {
                    val contentAlpha = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f)
                            .graphicsLayer(
                                alpha = contentAlpha,
                                compositingStrategy = CompositingStrategy.Offscreen
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PERIODO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp,
                                color = Color.White
                            )
                        }

                        // Scrollable Content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                        ) {
                            val isAllTime = currentRange is TimeRange.AllTime
                            
                            ModalItem(
                                label = "Tutte le statistiche",
                                isSelected = isAllTime,
                                onClick = { 
                                    onAllTimeSelected()
                                    onDismiss()
                                }
                            )
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                thickness = 0.5.dp,
                                color = Color.White.copy(alpha = 0.1f)
                            )
                            
                            availableYears.forEach { year ->
                                val isSelected = currentRange is TimeRange.Year && 
                                                currentRange.year == year
                                ModalItem(
                                    label = year.toString(),
                                    isSelected = isSelected,
                                    onClick = { 
                                        onYearSelected(year)
                                        onDismiss()
                                    }
                                )
                            }
                            
                            Spacer(Modifier.height(12.dp))
                        }

                        // Close Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .height(48.dp)
                                .bounceClick { onDismiss() }
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "CHIUDI",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModalItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
            )
        }
    }
}
// ── Helper Share Function ───────────────────────────────────────────
fun shareStats(context: android.content.Context, stats: CalculatedStats, title: String) {
    val message = buildString {
        append("🎬 $title su FlickTrove!\n\n")
        append("📊 Ho guardato ${stats.moviesWatched} film e ${stats.tvWatched} serie TV.\n")
        append("⏳ Tempo totale: ${stats.totalTimeFormatted}\n")
        
        val topGenre = stats.genreCounts.firstOrNull()?.first
        if (topGenre != null) {
            append("🎭 Genere preferito: $topGenre\n")
        }
        
        val topActor = stats.topCast.firstOrNull()?.name
        if (topActor != null) {
            append("⭐ Star preferita: $topActor\n")
        }

        val topDirector = stats.topDirectors.firstOrNull()?.name
        if (topDirector != null) {
            append("🎥 Regista preferito: $topDirector\n")
        }

        val topDecade = stats.decadeCounts.maxByOrNull { it.second }?.first
        if (topDecade != null) {
            append("📅 Decade d'oro: $topDecade\n")
        }

        append("\nTraccia il tuo viaggio cinematografico su FlickTrove! 🍿")
    }

    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }
    val chooser = android.content.Intent.createChooser(sendIntent, "Condividi le tue statistiche")
    chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
