package com.cinetrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.zIndex
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.cinetrack.ui.components.card.MovieCard
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.detail.CollectionDetailSkeleton
import com.cinetrack.ui.components.detail.DetailBackdrop
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.MovieActionsState
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.theme.PremiumBackground
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.viewmodel.CollectionDetailViewModel
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlin.math.max

data class CollectionDetailScreen(
    val collectionId: Long,
    val collectionName: String? = null
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel = getViewModel<CollectionDetailViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val rootHazeState = remember { HazeState() }
        val backdropHazeState = remember { HazeState() }

        LaunchedEffect(collectionId) {
            viewModel.initCollection(collectionId, collectionName)
        }

        MovieActionsWrapper(
            hazeState = rootHazeState,
            folders = uiState.folders,
            isItemInFolder = { movie, folderId ->
                uiState.folders.find { it.id == folderId }?.itemIds?.contains("${movie.mediaType}_${movie.id}") ?: false
            },
            onDelete = { /* Collection detail usually doesn't delete */ },
            onUpdateRating = { movie, rating -> viewModel.updateRating(movie, rating) },
            onUpdateNote = { movie, note -> viewModel.updateNote(movie, note) },
            onToggleFolder = { movie, folder -> viewModel.toggleItemInFolder(folder, movie) }
        ) { actionsState ->
            CollectionDetailScreenContent(
                viewModel = viewModel,
                rootHazeState = rootHazeState,
                backdropHazeState = backdropHazeState,
                actionsState = actionsState,
                onBack = { navigator.pop() },
                onMovieClick = { movie ->
                    navigator.push(MovieDetailScreen(movie.id, "movie"))
                }
            )
        }
    }
}

@Composable
fun CollectionDetailScreenContent(
    viewModel: CollectionDetailViewModel,
    rootHazeState: HazeState,
    backdropHazeState: HazeState,
    actionsState: MovieActionsState,
    onBack: () -> Unit,
    onMovieClick: (com.cinetrack.data.model.Movie) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val columns = max(3, (screenWidth / 115.dp).toInt())
    val cardWidth = (screenWidth - 32.dp - (12.dp * (columns - 1))) / columns - 1.5.dp

    val extractedColor by viewModel.extractedColor.collectAsStateWithLifecycle()
    val themePrimaryColor = MaterialTheme.colorScheme.primary
    val fallbackAccentColor = remember(themePrimaryColor) { themePrimaryColor }
    val rawAccentColor = extractedColor ?: fallbackAccentColor
    val globalAccentColor = remember(rawAccentColor) { ColorUtils.ensureVividAccent(rawAccentColor) }

    val baseDarkColor = remember { Color(0xFF161620) }
    val targetBackgroundColor = if (uiState.collection != null) lerp(globalAccentColor, baseDarkColor, 0.68f) else baseDarkColor
    val animatedBgColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 800),
        label = "backgroundColor"
    )

    val currentImageQuality = LocalImageQuality.current
    LaunchedEffect(uiState.collection, currentImageQuality) {
        val collection = uiState.collection
        if (collection != null) {
            val targetPath = collection.backdropPath ?: collection.posterPath
            val imageType = if (collection.backdropPath != null) ImageType.BACKDROP else ImageType.POSTER
            val imageUrl = buildTmdbImageUrl(targetPath, imageType, currentImageQuality)
            if (imageUrl != null) {
                viewModel.fetchAccentColor(imageUrl)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor)
            .haze(rootHazeState, style = HazeStyles.PremiumDark)
    ) {
        if (uiState.isLoading && uiState.collection == null) {
            CollectionDetailSkeleton(
                cardWidth = cardWidth,
                hazeState = backdropHazeState
            )
        } else {
            val collection = uiState.collection
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    DetailBackdrop(
                        backdropPath = collection?.backdropPath ?: collection?.posterPath,
                        posterPath = collection?.posterPath,
                        accentColor = globalAccentColor,
                        backgroundColor = animatedBgColor,
                        modifier = Modifier.haze(backdropHazeState, style = HazeStyles.PremiumDark)
                    )
                }

                Column(
                    modifier = Modifier
                        .offset(y = (-140).dp)
                        .padding(bottom = 60.dp)
                ) {
                    // Hero Header Section
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = uiState.collectionName ?: "Collezione",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                lineHeight = 28.sp
                            ),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Film Count Badge with true real-time Frosted Glass (hazeGlass)
                        val totalMovies = collection?.parts?.size ?: 0
                        Box(
                            modifier = Modifier
                                .hazeGlass(
                                    state = backdropHazeState,
                                    shape = RoundedCornerShape(14.dp),
                                    containerColor = Color.Black.copy(alpha = 0.45f),
                                    borderColor = Color.White.copy(alpha = 0.35f),
                                    borderWidth = 1.dp
                                )
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            val detailBadgeText = when {
                                totalMovies == 1 -> stringResource(R.string.collection_badge_movies_single, 1)
                                totalMovies > 1 -> stringResource(R.string.collection_badge_movies_plural, totalMovies)
                                else -> "SAGA"
                            }
                            Text(
                                text = detailBadgeText,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Expandable Overview
                        val overview = collection?.overview
                        if (!overview.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            var isExpanded by remember { mutableStateOf(false) }
                            var hasOverflow by remember { mutableStateOf(false) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (hasOverflow || isExpanded) Modifier.bounceClick { isExpanded = !isExpanded } else Modifier)
                            ) {
                                AnimatedContent(
                                    targetState = isExpanded,
                                    label = "overview_expand_collection"
                                ) { expanded ->
                                    Text(
                                        text = overview,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 20.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                                        overflow = TextOverflow.Ellipsis,
                                        onTextLayout = { result ->
                                            if (!expanded && hasOverflow != result.hasVisualOverflow) {
                                                hasOverflow = result.hasVisualOverflow
                                            }
                                        }
                                    )
                                }

                                if (hasOverflow || isExpanded) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isExpanded) stringResource(R.string.overview_show_less) else stringResource(R.string.overview_show_more),
                                        color = globalAccentColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Movies inside the collection, exactly 3 columns
                    val parts = collection?.parts ?: emptyList()
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        parts.forEachIndexed { index, movie ->
                            Box(modifier = Modifier.width(cardWidth)) {
                                val folderColors = uiState.movieFolderColors[movie.id.toString()] ?: emptyList()
                                val folderColorObjects = remember(folderColors) {
                                    folderColors.map { Color(android.graphics.Color.parseColor(it)) }
                                }
                                val fav = uiState.favorites.find { it.id == movie.id }
                                val isFavorite = fav?.favorite == true
                                val isWatched = fav?.watched == true
                                val isReminder = fav?.reminder == true

                                MovieCard(
                                    movie = movie,
                                    cardWidth = cardWidth,
                                    isFavorite = isFavorite,
                                    isWatched = isWatched,
                                    isReminder = isReminder,
                                    folderColors = folderColorObjects,
                                    showFolderBookmarks = uiState.preferences.showFolderBookmarks,
                                    showBadges = uiState.preferences.showBadges,
                                    showAdvancedBadges = false,
                                    hazeState = rootHazeState,
                                    staggerIndex = index,
                                    onPress = { onMovieClick(movie) },
                                    onLongPress = { m, pressOffset, cardPos ->
                                        actionsState.onLongPress(m, pressOffset, cardPos)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        val context = LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .hazeGlass(
                            state = backdropHazeState,
                            shape = CircleShape,
                            blurRadius = HazeStyles.SmallGlassBlurRadius,
                            useOffscreenStrategy = true
                        )
                        .bounceClick { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_left),
                        contentDescription = stringResource(R.string.detail_content_desc_back),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .hazeGlass(
                            state = backdropHazeState,
                            shape = CircleShape,
                            blurRadius = HazeStyles.SmallGlassBlurRadius,
                            useOffscreenStrategy = true
                        )
                        .bounceClick {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                val shareText = buildString {
                                    append(uiState.collection?.name ?: "")
                                    val overview = uiState.collection?.overview?.takeIf { it.isNotBlank() }
                                    if (overview != null) {
                                        append("\n\n")
                                        append(if (overview.length > 200) overview.take(197) + "..." else overview)
                                    }
                                    val colId = uiState.collection?.id
                                    if (colId != null) {
                                        append("\n\nhttps://www.themoviedb.org/collection/$colId")
                                    }
                                }
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, context.getString(R.string.detail_content_desc_share)))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_share),
                        contentDescription = stringResource(R.string.detail_content_desc_share),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
