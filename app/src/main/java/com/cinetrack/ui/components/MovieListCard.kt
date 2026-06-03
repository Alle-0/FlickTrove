package com.cinetrack.ui.components

import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.data.Movie
import com.cinetrack.ui.utils.bounceClickWithOffset
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.cardRipple
import com.cinetrack.ui.utils.rememberCardRippleState
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MovieListCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    isWatched: Boolean = false,
    isReminder: Boolean = false,
    personalRating: Double? = null,
    progress: Float = 0f,
    folderColors: List<Color> = emptyList(),
    showFolderBookmarks: Boolean = true,
    showBadges: Boolean = true,
    hazeState: HazeState? = null,
    staggerIndex: Int = -1,
    onPress: (Movie) -> Unit = {},
    onLongPress: (Movie, Offset, Offset) -> Unit = { _, _, _ -> },
    onAction: (Movie) -> Unit = {},
    onMessage: (String) -> Unit = {}
) {
    val isTv = movie.mediaType == "tv"
    val posterUrl = buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, LocalImageQuality.current)
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var cardPosition by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    val rippleState = rememberCardRippleState()

    // Premium Staggered Entrance Animation States
    val hasAnimated = rememberSaveable { mutableStateOf(false) }

    val cardAlpha = remember { Animatable(if (hasAnimated.value) 1f else 0f) }
    val cardScale = remember { Animatable(if (hasAnimated.value) 1f else 0.85f) }
    val cardTranslateY = remember { Animatable(if (hasAnimated.value) 0f else 40f) }

    LaunchedEffect(movie.id) {
        if (hasAnimated.value) return@LaunchedEffect

        val delayMillis = if (staggerIndex in 0..11) staggerIndex * 40L else 0L
        if (delayMillis > 0) delay(delayMillis)

        val isScrollItem = staggerIndex < 0 || staggerIndex >= 12

        if (isScrollItem) {
            // Snapping immediately to visible state without coroutine animations to prevent scroll jank
            cardAlpha.snapTo(1f)
            cardTranslateY.snapTo(0f)
            cardScale.snapTo(1f)
        } else {
            val jobAlpha = launch { cardAlpha.animateTo(1f, tween(250, easing = LinearOutSlowInEasing)) }
            val jobScale = launch { cardScale.animateTo(1f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)) }
            val jobTranslate = launch { cardTranslateY.animateTo(0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)) }
            jobAlpha.join(); jobScale.join(); jobTranslate.join()
        }
        hasAnimated.value = true
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .graphicsLayer {
                alpha = cardAlpha.value
                scaleX = cardScale.value
                scaleY = cardScale.value
                translationY = cardTranslateY.value * density.density
            }
            .onGloballyPositioned { coordinates -> 
                cardPosition = coordinates.positionInWindow()
                cardSize = coordinates.size
            }
            .clip(RoundedCornerShape(24.dp))
            .bounceClickWithOffset(
                scaleDown = 0.95f, 
                requireUnconsumed = false,
                onLongClick = { offset -> 
                    onLongPress(movie, offset, cardPosition) 
                }
            ) { offset -> 
                rippleState.trigger(offset)
                onPress(movie) 
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .cardRipple(rippleState, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            val bgUrl = buildTmdbImageUrl(movie.backdropPath, ImageType.BACKDROP, LocalImageQuality.current) ?: posterUrl
            if (bgUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(bgUrl)
                        .crossfade(false)
                        .allowHardware(true)
                        .memoryCacheKey(bgUrl)
                        .diskCacheKey(bgUrl)
                        .build(),
                    contentDescription = movie.title ?: movie.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = if (isTv) Icons.Rounded.Tv else Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(24.dp)
                            .bounceClick(scaleDown = 0.85f, onLongClick = {}) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onAction(movie)
                            },)
                }
            }

            // Gradient for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            0.0f to Color.Black.copy(alpha = 0.95f),
                            0.3f to Color.Black.copy(alpha = 0.8f),
                            0.6f to Color.Black.copy(alpha = 0.4f),
                            0.75f to Color.Transparent,
                            0.85f to Color.Transparent,
                            0.95f to Color.Black.copy(alpha = 0.5f),
                            1.0f to Color.Black.copy(alpha = 0.8f)
                        )
                    )
            )

            if (showFolderBookmarks) {
                Box(modifier = Modifier.align(Alignment.TopStart)) {
                    MovieFolderBookmarks(folderColors)
                }
            }

            // Dynamic Badges (Top Right)
            if (showBadges) {
                val disabledBadges = com.cinetrack.LocalDisabledBadges.current
                val addBadge = @Composable { text: String, color: Color ->
                    if (!disabledBadges.contains(text)) {
                        MovieCardBadge(text = text, color = color, hazeState = hazeState, isLarge = false)
                    }
                }
                
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val newEpisodes = movie.newEpisodesFound
                    if (newEpisodes != null && newEpisodes > 0) {
                        addBadge("NEW", com.cinetrack.ui.theme.NeonPink)
                    }
    
                    if ((movie.voteAverage ?: 0.0) >= 8.8 && (movie.voteCount ?: 0) > 2000) {
                        addBadge("MASTERPIECE", Color(0xFFFFD700))
                    } else if ((movie.voteAverage ?: 0.0) >= 8.5 && (movie.voteCount ?: 0) > 300) {
                        addBadge("BEST", Color(0xFF00E5FF))
                    } else if ((movie.voteCount ?: 0) > 3000) {
                        addBadge("HOT", com.cinetrack.ui.theme.HazeStyles.AccentYellow)
                    } else if ((movie.voteAverage ?: 0.0) >= 8.0 && (movie.voteCount ?: 0) > 1000) {
                        addBadge("WOW", com.cinetrack.ui.theme.NeonTeal)
                    } else if ((movie.voteAverage ?: 0.0) >= 7.5 && (movie.voteCount ?: 0) in 50..500) {
                        addBadge("HIDDEN GEM", Color(0xFF00E676))
                    }
                    
                    if ((movie.voteCount ?: 0) > 1000 && (movie.voteAverage ?: 0.0) >= 5.0 && (movie.voteAverage ?: 0.0) <= 6.5) {
                        addBadge("DIVISIVE", Color(0xFFFF9800))
                    }

                    if ((movie.revenue ?: 0L) > 500_000_000L) {
                        addBadge("BLOCKBUSTER", Color(0xFF6200EA))
                    } else if ((movie.budget ?: 0L) in 1L..5_000_000L && (movie.voteAverage ?: 0.0) >= 7.0) {
                        addBadge("INDIE", Color(0xFFAED581))
                    }
                    
                    val releaseYearNum = movie.releaseYear?.toIntOrNull() ?: 
                                      movie.releaseDate?.take(4)?.toIntOrNull() ?: 
                                      movie.firstAirDate?.take(4)?.toIntOrNull() ?: 9999
                    
                    if (releaseYearNum < 1970) {
                        addBadge("VINTAGE", Color(0xFFBCAAA4))
                    } else if (releaseYearNum < 1990 && (movie.voteAverage ?: 0.0) >= 7.0) {
                        addBadge("CLASSIC", Color(0xFF8D6E63))
                    } else if (releaseYearNum in 1990..2010 && (movie.voteAverage ?: 0.0) >= 8.0) {
                        addBadge("CULT", Color(0xFF9C27B0))
                    }
                    
                    if ((movie.runtime ?: 0) > 160) {
                        addBadge("EPIC", Color(0xFFFF5722))
                    } else if (movie.mediaType != "tv" && (movie.runtime ?: 0) in 1..89) {
                        addBadge("QUICK", Color(0xFFC6FF00))
                    }
                    
                    if ((movie.numberOfSeasons ?: 0) >= 5 || (movie.numberOfEpisodes ?: 0) > 50) {
                        addBadge("BINGE", Color(0xFF00BCD4))
                    } else if (movie.mediaType == "tv" && (movie.episodeRunTime?.firstOrNull() ?: 0) in 1..25) {
                        addBadge("SNACK", Color(0xFFC6FF00))
                    }
    
                    val genresStr = movie.genreNamesString ?: ""
                    val hasGenre = { name: String -> 
                        genresStr.contains(name, ignoreCase = true) || 
                        movie.genres?.any { it.name?.equals(name, ignoreCase = true) == true } == true
                    }
                    
                    var genreBadgeAdded = false
                    if (hasGenre("Horror")) {
                        addBadge("HORROR", Color(0xFFE53935))
                        genreBadgeAdded = true
                    }
                    if (hasGenre("Thriller")) {
                        addBadge("THRILLER", Color(0xFF651FFF))
                        genreBadgeAdded = true
                    }
                    
                    if (!genreBadgeAdded) {
                        if (hasGenre("Animation") || hasGenre("Anime") || hasGenre("Animazione")) {
                            addBadge("ANIME", Color(0xFFFF9800))
                        } else if (hasGenre("Science Fiction") || hasGenre("Sci-Fi") || hasGenre("Fantascienza")) {
                            addBadge("SCI-FI", Color(0xFF2962FF))
                        } else if (hasGenre("Comedy") || hasGenre("Commedia")) {
                            addBadge("COMEDY", Color(0xFFFFEA00))
                        } else if (hasGenre("Documentary") || hasGenre("Documentario")) {
                            addBadge("DOCU", Color(0xFF9E9E9E))
                        } else if (hasGenre("Family") || hasGenre("Famiglia")) {
                            addBadge("FAMILY", Color(0xFF81D4FA))
                        }
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 56.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                val multiplier = com.cinetrack.LocalTitleTextSizeMultiplier.current
                Text(
                    text = movie.title ?: movie.name ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = (17 * multiplier).sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val releaseYear = movie.releaseYear?.toIntOrNull()?.toString() ?: movie.releaseDate?.take(4) ?: movie.firstAirDate?.take(4) ?: ""
                
                val displayRating = personalRating ?: movie.personalRating
                val hasRating = displayRating != null && displayRating > 0.0
                val vote = if (hasRating) String.format("%.1f", displayRating) else ""
                
                val genres = movie.genreNamesString ?: movie.genres?.mapNotNull { it.name }?.joinToString(", ")
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (releaseYear.isNotEmpty()) {
                        Text(
                            text = releaseYear,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (hasRating) {
                        Text(
                            text = "★ $vote",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, fontSize = (13 * multiplier).sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (!genres.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = genres,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Action Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 12.dp, end = 12.dp)
                    .size(34.dp)
                    .bounceClick(
                        scaleDown = 0.85f,
                        onLongClick = {}, // Block long press menu from opening
                        vibrateOnLongClick = false,
                        onPress = null
                    ) {
                        // Trigger ripple on the parent card from the button center on release
                        val rippleX = if (cardSize.width > 0) {
                            cardSize.width.toFloat() - with(density) { 29.dp.toPx() } // button size 34/2 + end padding 12 = 29
                        } else {
                            with(density) { 300.dp.toPx() - 29.dp.toPx() }
                        }
                        
                        val rippleY = if (cardSize.height > 0) {
                            cardSize.height.toFloat() - with(density) { 29.dp.toPx() } // bottom padding 12 + 17 = 29
                        } else {
                            with(density) { 110.dp.toPx() - 29.dp.toPx() }
                        }
                        rippleState.trigger(Offset(rippleX, rippleY))

                        val isReleased = movie.isReleased
                        if (!isReleased) {
                            if (!isWatched && !isFavorite && !isReminder) onAction(movie)
                            else onMessage("Gestisci il promemoria nella pagina dei dettagli")
                        } else if (isTv) {
                            if (!isWatched && !isFavorite && !isReminder) onAction(movie)
                            else onMessage("Gestisci gli episodi nella pagina dei dettagli")
                        } else {
                            // Normal behavior for released movies
                            onAction(movie)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isTv && !isWatched && progress > 0f) {
                    MovieCircularProgress(progress = progress)
                }

                val appAccent = MaterialTheme.colorScheme.primary
                val isReleased = movie.isReleased
                val isPromemoriaAttiva = !isReleased && isReminder
                val isOcchio = isReleased && (isReminder || isFavorite)

                val actionBg = when {
                    isWatched -> Color(0xFF10B981).copy(alpha = 0.1f) // Green for Watched
                    isPromemoriaAttiva -> appAccent.copy(alpha = 0.1f) // Theme color for Reminder
                    isOcchio -> com.cinetrack.ui.theme.HazeStyles.AccentYellow.copy(alpha = 0.1f) // Yellow for Eye/Favorite
                    else -> com.cinetrack.ui.theme.HazeStyles.GlassColor.copy(alpha = 0.60f)
                }

                val actionBorder = when {
                    isWatched -> Color(0xFF10B981)
                    isPromemoriaAttiva -> appAccent
                    isOcchio -> com.cinetrack.ui.theme.HazeStyles.AccentYellow
                    else -> com.cinetrack.ui.theme.HazeStyles.GlassBorderColor
                }.copy(alpha = 0.3f)

                val actionTint = when {
                    isWatched -> Color(0xFF10B981)
                    isPromemoriaAttiva -> appAccent
                    isOcchio -> com.cinetrack.ui.theme.HazeStyles.AccentYellow
                    else -> Color.White
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(color = actionBg, shape = CircleShape)
                        .border(width = 1.dp, color = actionBorder, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val isBellOutlined = !isReleased && !isReminder
                    
                    val bellRotation = remember { androidx.compose.animation.core.Animatable(0f) }
                    var bellHasInitialized by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(isReminder) {
                        if (bellHasInitialized && !isReleased && isReminder) {
                            bellRotation.animateTo(-25f, tween(60, easing = LinearEasing))
                            bellRotation.animateTo(20f, tween(100, easing = LinearEasing))
                            bellRotation.animateTo(-15f, tween(100, easing = LinearEasing))
                            bellRotation.animateTo(10f, tween(100, easing = LinearEasing))
                            bellRotation.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
                        }
                        bellHasInitialized = true
                    }
                    
                    Icon(
                        imageVector = when {
                            isWatched -> com.cinetrack.ui.assets.CustomIcons.PremiumCheck
                            !isReleased -> if (isReminder) com.cinetrack.ui.assets.CustomIcons.PremiumBellFilled else Icons.Rounded.NotificationsNone
                            isOcchio -> androidx.compose.material.icons.Icons.Rounded.Visibility
                            else -> com.cinetrack.ui.assets.CustomIcons.PremiumAdd
                        },
                        contentDescription = "Action",
                        tint = actionTint,
                        modifier = Modifier
                            .size(if (isBellOutlined) 20.dp else 18.dp)
                            .graphicsLayer {
                                if (!isReleased && isReminder) {
                                    rotationZ = bellRotation.value
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.2f)
                                }
                            }
                    )
                }
            }
        }
    }
}
