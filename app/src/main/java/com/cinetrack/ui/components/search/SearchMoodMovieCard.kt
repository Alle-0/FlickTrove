package com.cinetrack.ui.components.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.cinetrack.R
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.card.MovieCardActionButton
import com.cinetrack.ui.components.card.MovieFolderBookmarks
import com.cinetrack.ui.components.card.rememberCardEntranceAnimation
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClickWithOffset
import com.cinetrack.ui.utils.cardRipple
import com.cinetrack.ui.utils.rememberCardRippleState
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl

/**
 * Layout editoriale dedicato per la modalità Cinema Mood Discovery.
 *
 * Sostituisce la classica griglia a poster con una card orizzontale a tutta larghezza (Span 12),
 * offrendo ampio respiro orizzontale per la motivazione bespoke generata da Gemini
 * senza troncare le spiegazioni o comprimerle in colonne strette.
 */
@Composable
fun SearchMoodMovieCard(
    movie: Movie,
    movieStatus: Movie?,
    folderColors: List<Color>,
    aiReason: String?,
    staggerIndex: Int,
    hasAnimatedSet: MutableSet<String>?,
    keyboardController: SoftwareKeyboardController?,
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val movieActions = LocalMovieActions.current
    val cardPosition = remember { arrayOf(Offset.Zero) }
    val cardSize = remember { arrayOf(Size.Zero) }
    val rippleState = rememberCardRippleState()

    val entranceAnim = rememberCardEntranceAnimation(
        itemId = "${movie.id}_${movie.mediaType}",
        staggerIndex = staggerIndex,
        hasAnimatedSet = hasAnimatedSet,
        isExploding = movieActions.explodingMovie?.id == movie.id
    )

    val posterUrl = buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, LocalImageQuality.current)

    val isFavorite = movieStatus?.favorite ?: movie.favorite
    val isWatched = movieStatus?.watched ?: movie.watched
    val isReminder = movieStatus?.reminder ?: movie.reminder
    val personalRating = movieStatus?.personalRating ?: movie.personalRating

    val isTv = movie.mediaType == "tv"
    val title = movie.title ?: movie.name ?: ""

    val releaseYear = remember(movie.releaseDate, movie.firstAirDate) {
        val date = movie.releaseDate ?: movie.firstAirDate
        if (!date.isNullOrEmpty() && date.length >= 4) date.substring(0, 4) else ""
    }

    val contextLocale = LocalConfiguration.current.locales[0].language
    val genres = remember(movie.genreIds, movie.genreNamesString, contextLocale) {
        if (!movie.genreIds.isNullOrEmpty()) {
            movie.genreIds!!.take(3).mapNotNull { id ->
                val defaultName = GenreConstants.ALL_GENRES.find { it.id == id }?.name ?: ""
                val localized = GenreConstants.getLocalizedName(id, contextLocale, defaultName)
                localized.takeIf { it.isNotEmpty() }
            }.joinToString(", ")
        } else {
            movie.genreNamesString ?: movie.genres?.mapNotNull { it.name }?.take(3)?.joinToString(", ") ?: ""
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entranceAnim.alpha
                scaleX = entranceAnim.scale
                scaleY = entranceAnim.scale
                translationY = entranceAnim.translateY * density.density
            }
            .onGloballyPositioned { coordinates ->
                cardPosition[0] = coordinates.positionInWindow()
                cardSize[0] = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
            }
            .clip(RoundedCornerShape(20.dp))
            .bounceClickWithOffset(
                scaleDown = 0.96f,
                requireUnconsumed = false,
                onLongClick = { offset ->
                    movieActions.updatePopupCardSize(cardSize[0])
                    onLongPress(movie.apply {
                        this.favorite = isFavorite
                        this.watched = isWatched
                        this.reminder = isReminder
                        if (personalRating != null) this.personalRating = personalRating
                    }, offset, cardPosition[0])
                }
            ) { offset ->
                keyboardController?.hide()
                rippleState.trigger(offset)
                onMovieClick(movie)
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131622)),
        border = BorderStroke(
            1.dp,
            if (!aiReason.isNullOrBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
            else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cardRipple(rippleState, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 1. Poster
                Box(
                    modifier = Modifier
                        .width(92.dp)
                        .height(138.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    if (posterUrl != null) {
                        val imageRequest = remember(posterUrl) {
                            ImageRequest.Builder(context)
                                .data(posterUrl)
                                .crossfade(false)
                                .allowHardware(true)
                                .build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isTv) ImageVector.vectorResource(id = R.drawable.ic_tv) else ImageVector.vectorResource(id = R.drawable.ic_ciak),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Folder Bookmarks if any
                    if (folderColors.isNotEmpty()) {
                        MovieFolderBookmarks(folderColors)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 2. Info + AI Reason
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    // Top: Title and Action Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                lineHeight = 19.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Action button
                        MovieCardActionButton(
                            movie = movie,
                            cardWidth = 92.dp,
                            cardHeight = 138.dp,
                            isFavorite = isFavorite,
                            isWatched = isWatched,
                            isReminder = isReminder,
                            computedProgress = 0f,
                            showActionHint = false,
                            isLarge = false,
                            rippleState = rippleState,
                            onAction = onToggleFavorite,
                            onMessage = onEmitMessage
                        )
                    }

                    // Metadata Row: Year · Score · Media Type
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (releaseYear.isNotEmpty()) {
                            Text(
                                text = releaseYear,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if ((movie.voteAverage ?: 0.0) > 0.0) {
                            if (releaseYear.isNotEmpty()) {
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                                    contentDescription = null,
                                    tint = HazeStyles.AccentYellow,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f", movie.voteAverage),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HazeStyles.AccentYellow
                                )
                            }
                        }

                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = if (isTv) stringResource(R.string.folder_detail_tab_tv) else stringResource(R.string.folder_detail_tab_movies),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    // Genres
                    if (genres.isNotEmpty()) {
                        Text(
                            text = genres,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // AI Explanation (or overview)
                    val overview = movie.overview
                    if (!aiReason.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 9.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = aiReason,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 16.sp
                                ),
                                color = Color.White.copy(alpha = 0.94f)
                            )
                        }
                    } else if (!overview.isNullOrBlank()) {
                        Text(
                            text = overview,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoodCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(162.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131622))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(92.dp)
                    .height(138.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}
