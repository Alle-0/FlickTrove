package com.cinetrack.ui.components.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.ui.components.card.rememberCardEntranceAnimation
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.utils.bounceClickWithOffset
import com.cinetrack.ui.utils.cardRipple
import com.cinetrack.ui.utils.rememberCardRippleState
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import java.util.Locale

/**
 * Layout editoriale d'autore per Cinema Mood Discovery (Concept 1: Editorial Quote).
 *
 * Card orizzontale a tutta larghezza con locandina 2:3, rating dorato e
 * la spiegazione critica dell'IA formattata come virgolettato d'autore
 * con sottile barra d'accento verticale (senza doppie scatole o fumetti pesanti).
 */
@Composable
fun SearchMoodEditorialCard(
    movie: Movie,
    movieStatus: Movie?,
    folderColors: List<Color>,
    aiReason: String?,
    staggerIndex: Int,
    hasAnimatedSet: MutableSet<String>?,
    keyboardController: SoftwareKeyboardController?,
    onMovieClick: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
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

    val title = movie.title ?: movie.name ?: ""

    val releaseYear = remember(movie.releaseDate, movie.firstAirDate) {
        val date = movie.releaseDate ?: movie.firstAirDate
        if (!date.isNullOrEmpty() && date.length >= 4) date.substring(0, 4) else ""
    }

    val contextLocale = LocalConfiguration.current.locales[0].language
    val genres = remember(movie.genreIds, movie.genreNamesString, contextLocale) {
        if (!movie.genreIds.isNullOrEmpty()) {
            movie.genreIds!!.take(2).mapNotNull { id ->
                val defaultName = GenreConstants.ALL_GENRES.find { it.id == id }?.name ?: ""
                val localized = GenreConstants.getLocalizedName(id, contextLocale, defaultName)
                localized.takeIf { it.isNotEmpty() }
            }.joinToString(", ")
        } else {
            movie.genreNamesString ?: movie.genres?.mapNotNull { it.name }?.take(2)?.joinToString(", ") ?: ""
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    val cardShape = RoundedCornerShape(24.dp)
    val posterShape = RoundedCornerShape(12.dp) // Concentrico: 24.dp (outer) - 12.dp (padding) = 12.dp (inner)

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
            .clip(cardShape)
            .bounceClickWithOffset(
                scaleDown = 0.97f,
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
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = BorderStroke(
            0.5.dp,
            if (!aiReason.isNullOrBlank()) primaryColor.copy(alpha = 0.25f)
            else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cardRipple(rippleState, color = primaryColor.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 1. Locandina 2:3 matematicamente concentrica al box esterno (12dp + 12dp = 24dp)
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(144.dp)
                        .shadow(elevation = 6.dp, shape = posterShape, clip = false)
                        .clip(posterShape)
                        .background(Color(0xFF222222))
                ) {
                    if (!posterUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = posterUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Badge Voto a Pillola autentica con icona ufficiale ic_star_piena
                    val vote = movie.voteAverage
                    if (vote != null && vote > 0.0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 7.dp, bottom = 7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.82f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                                .padding(horizontal = 6.dp, vertical = 2.5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.5.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(10.5.dp)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f", vote),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Badge di Stato (Top-Right: Visto)
                    if (isWatched) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(22.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.75f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_eye),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // 2. Contenuto Editoriale a Destra: flusso naturale verso l'alto (senza buchi vuoti)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 144.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Testata: Titolo
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 19.sp
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Metadati: Anno e Generi
                    val metaList = mutableListOf<String>()
                    if (releaseYear.isNotEmpty()) metaList.add(releaseYear)
                    if (genres.isNotEmpty()) metaList.add(genres)

                    if (metaList.isNotEmpty()) {
                        Text(
                            text = metaList.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = primaryColor.copy(alpha = 0.90f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Virgolettato Critico dell'IA alzato subito sotto i metadati con barra d'accento ad altezza intrinseca
                    if (!aiReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Barra verticale d'accento sfumata perfettamente calibrata sul testo
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                primaryColor.copy(alpha = 0.95f),
                                                primaryColor.copy(alpha = 0.40f)
                                            )
                                        )
                                    )
                            )

                            Spacer(modifier = Modifier.width(9.dp))

                            Text(
                                text = aiReason,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 15.5.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Skeleton di caricamento coerente con il layout editoriale Mood concentrico e neutro.
 */
@Composable
fun SearchMoodEditorialCardSkeleton(
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(24.dp)
    val posterShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color(0xFF161616))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Skeleton Locandina concentrico
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(144.dp)
                    .clip(posterShape)
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Skeleton Testo alzato
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 144.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.50f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .shimmerEffect()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .shimmerEffect()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}
