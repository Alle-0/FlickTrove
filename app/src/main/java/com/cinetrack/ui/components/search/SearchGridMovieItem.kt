package com.cinetrack.ui.components.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.card.MovieCard
import com.cinetrack.ui.components.card.MovieListCard

/**
 * Grid/List item rendering a movie in search results, with status badges
 * and optional AI recommendation reasoning.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SearchGridMovieItem(
    movie: Movie,
    movieStatus: Movie?,
    folderColors: List<Color>,
    columns: Int,
    cardWidth: Dp,
    showFolderBookmarks: Boolean,
    hasAnimatedSet: MutableSet<String>?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    staggerIndex: Int,
    keyboardController: SoftwareKeyboardController?,
    aiReason: String? = null,
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onLongPress: (Movie, Offset, Offset) -> Unit,
    onEmitMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = columns == 1,
            transitionSpec = {
                (fadeIn(animationSpec = tween(200, delayMillis = 40)) + scaleIn(initialScale = 0.94f, animationSpec = tween(200, delayMillis = 40)))
                    .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.94f, animationSpec = tween(120)))
            },
            label = "SearchGridCardLayoutTransition"
        ) { isList ->
            if (isList) {
                MovieListCard(
                    movie = movie,
                    modifier = Modifier.fillMaxWidth(),
                    isFavorite = movieStatus?.favorite ?: false,
                    isWatched = movieStatus?.watched ?: false,
                    isReminder = movieStatus?.reminder ?: false,
                    progress = movieStatus?.progress?.toFloat() ?: 0f,
                    folderColors = folderColors,
                    showFolderBookmarks = showFolderBookmarks,
                    hasAnimatedSet = hasAnimatedSet,
                    staggerIndex = staggerIndex,
                    aiReason = aiReason,
                    onPress = { 
                        keyboardController?.hide()
                        onMovieClick(movie) 
                    },
                    onAction = { onToggleFavorite(movie) },
                    onLongPress = { m, pressOffset, cardPos ->
                        onLongPress(m, pressOffset, cardPos)
                    },
                    onMessage = { onEmitMessage(it) }
                )
            } else {
                if (!aiReason.isNullOrBlank()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(cardWidth)
                    ) {
                        MovieCard(
                            movie = movie,
                            cardWidth = cardWidth,
                            isFavorite = movieStatus?.favorite ?: false,
                            isWatched = movieStatus?.watched ?: false,
                            isReminder = movieStatus?.reminder ?: false,
                            progress = movieStatus?.progress?.toFloat() ?: 0f,
                            personalRating = movieStatus?.personalRating,
                            folderColors = folderColors,
                            showFolderBookmarks = showFolderBookmarks,
                            hasAnimatedSet = hasAnimatedSet,
                            animatedVisibilityScope = animatedVisibilityScope,
                            staggerIndex = staggerIndex,
                            onPress = { 
                                keyboardController?.hide()
                                onMovieClick(movie) 
                            },
                            onAction = { onToggleFavorite(movie) },
                            onLongPress = { m, pressOffset, cardPos ->
                                onLongPress(m, pressOffset, cardPos)
                            },
                            onMessage = { onEmitMessage(it) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = aiReason,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 13.5.sp
                                ),
                                color = Color.White.copy(alpha = 0.90f),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    MovieCard(
                        movie = movie,
                        cardWidth = cardWidth,
                        isFavorite = movieStatus?.favorite ?: false,
                        isWatched = movieStatus?.watched ?: false,
                        isReminder = movieStatus?.reminder ?: false,
                        progress = movieStatus?.progress?.toFloat() ?: 0f,
                        personalRating = movieStatus?.personalRating,
                        folderColors = folderColors,
                        showFolderBookmarks = showFolderBookmarks,
                        hasAnimatedSet = hasAnimatedSet,
                        animatedVisibilityScope = animatedVisibilityScope,
                        staggerIndex = staggerIndex,
                        onPress = { 
                            keyboardController?.hide()
                            onMovieClick(movie) 
                        },
                        onAction = { onToggleFavorite(movie) },
                        onLongPress = { m, pressOffset, cardPos ->
                            onLongPress(m, pressOffset, cardPos)
                        },
                        onMessage = { onEmitMessage(it) }
                    )
                }
            }
        }
    }
}
