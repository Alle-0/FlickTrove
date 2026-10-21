package com.cinetrack.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import com.cinetrack.ui.utils.parseMarkdown
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.shared.LocalMovieActions
import com.cinetrack.ui.history.model.WatchTimelineEntry
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.bounceClickWithOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import java.util.Locale

@Composable
fun HistoryCard(
    entry: WatchTimelineEntry,
    onClick: () -> Unit,
    onEditDate: () -> Unit,
    onDeleteSession: () -> Unit,
    modifier: Modifier = Modifier,
    isCalendarDayMode: Boolean = false,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val movieActions = LocalMovieActions.current

    val cardPosition = remember { arrayOf(Offset.Zero) }
    val cardSize = remember { arrayOf(Size.Zero) }
    val buttonPosition = remember { arrayOf(Offset.Zero) }

    val posterUrl = remember(entry.posterPath) {
        if (!entry.posterPath.isNullOrBlank()) {
            "https://image.tmdb.org/t/p/w342${entry.posterPath}"
        } else null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                cardPosition[0] = coordinates.positionInWindow()
                cardSize[0] = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .bounceClickWithOffset(
                scaleDown = 0.98f,
                requireUnconsumed = false,
                onLongClick = { offset ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val movie = Movie(
                        id = entry.movieId,
                        mediaType = entry.mediaType,
                        title = entry.title,
                        name = if (entry.isTvShow) entry.title else null,
                        posterPath = entry.posterPath,
                        watched = true,
                        personalRating = entry.personalRating,
                        personalNote = entry.personalNote
                    )
                    movieActions.openActionsPopup(
                        movie = movie,
                        pressOffset = offset,
                        cardPosition = cardPosition[0],
                        cardSize = cardSize[0],
                        onEditWatchDate = onEditDate,
                        onDeleteWatchSession = onDeleteSession
                    )
                }
            ) { _ ->
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail (matching UpdateCard 48.dp x 72.dp)
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF121216))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            ) {
                if (posterUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = entry.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.title.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }

            // Info Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = entry.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle: "Serie (2016)" oppure "Film (2008)"
                val mediaTypeLabel = if (entry.isTvShow) {
                    stringResource(id = R.string.history_media_series)
                } else {
                    stringResource(id = R.string.history_media_movie)
                }
                val subtitleText = remember(entry.isTvShow, entry.releaseYear, mediaTypeLabel) {
                    buildString {
                        append(mediaTypeLabel)
                        if (!entry.releaseYear.isNullOrBlank()) {
                            append(" (")
                            append(entry.releaseYear)
                            append(")")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitleText,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Badges Row: Unified Session Capsule + Rating Badge (Max 2 clean badges)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Timing / Date formatted
                    val shortDate = remember(entry.parsedDate) {
                        try {
                            entry.parsedDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.getDefault()))
                        } catch (e: Exception) {
                            entry.formattedDisplayDate.take(6)
                        }
                    }

                    val rewatchLabel = if (entry.isRewatch) {
                        if (entry.rewatchNumber > 1) {
                            stringResource(id = R.string.history_rewatch_badge, entry.rewatchNumber)
                        } else {
                            stringResource(id = R.string.watch_history_rewatch)
                        }
                    } else null

                    // Theme color for the session capsule:
                    // TV Series uses active accent (primary), Movies use complementary harmonic (secondary)
                    val sessionColor = if (entry.isTvShow) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }

                    // Unified Session Capsule: [ Date • Ep • 🔁 Rewatch ]
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(sessionColor.copy(alpha = 0.14f))
                            .border(1.dp, sessionColor.copy(alpha = 0.35f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = shortDate,
                                color = sessionColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp,
                                maxLines = 1
                            )

                            if (!entry.episodeInfo.isNullOrBlank()) {
                                Text(
                                    text = "•",
                                    color = sessionColor.copy(alpha = 0.55f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = entry.episodeInfo,
                                    color = sessionColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp,
                                    maxLines = 1
                                )
                            }

                            if (rewatchLabel != null) {
                                Text(
                                    text = "•",
                                    color = sessionColor.copy(alpha = 0.55f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_ricarica),
                                    contentDescription = null,
                                    tint = sessionColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = rewatchLabel,
                                    color = sessionColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Personal Rating badge (with official ic_star_piena)
                    if (entry.personalRating != null && entry.personalRating > 0.0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFBBF24).copy(alpha = 0.14f))
                                .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.35f), RoundedCornerShape(50))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f", entry.personalRating),
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Note Callout if present (with Markdown formatting & at least 2 lines)
                if (!entry.personalNote.isNullOrBlank()) {
                    val parsedNote = remember(entry.personalNote) {
                        val md = entry.personalNote.parseMarkdown()
                        buildAnnotatedString {
                            append("\"")
                            append(md)
                            append("\"")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parsedNote,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Options Button (triggers canonical MovieActionsPopup)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .onGloballyPositioned { coordinates ->
                        buttonPosition[0] = coordinates.positionInWindow()
                    }
                    .bounceClick(scaleDown = 0.88f) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val movie = Movie(
                            id = entry.movieId,
                            mediaType = entry.mediaType,
                            title = entry.title,
                            name = if (entry.isTvShow) entry.title else null,
                            posterPath = entry.posterPath,
                            watched = true,
                            personalRating = entry.personalRating,
                            personalNote = entry.personalNote
                        )
                        movieActions.openActionsPopup(
                            movie = movie,
                            pressOffset = Offset.Zero,
                            cardPosition = buttonPosition[0],
                            onEditWatchDate = onEditDate,
                            onDeleteWatchSession = onDeleteSession
                        )
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Opzioni",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
