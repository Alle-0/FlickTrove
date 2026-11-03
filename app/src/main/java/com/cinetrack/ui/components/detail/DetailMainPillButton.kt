package com.cinetrack.ui.components.detail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.WatchState
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

/**
 * Main pill button in DetailActions representing Watch / Bookmarked / Episode selection status.
 */
@Composable
fun DetailMainPillButton(
    movie: Movie,
    optimisticWatchState: WatchState,
    pillWidth: Dp,
    mainPillAlpha: Float,
    displayColor: Color,
    animatedProgress: Float,
    icon: ImageVector,
    label: String,
    hazeState: HazeState?,
    context: Context,
    permissionLauncher: ActivityResultLauncher<String>,
    onStateChange: (WatchState) -> Unit,
    onEpisodesClick: (() -> Unit)?,
    onManageRewatches: (() -> Unit)?,
    onPreviewWatch: (() -> Unit)?,
    onOptimisticStateUpdate: (WatchState) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(pillWidth)
            .fillMaxHeight()
            .graphicsLayer { 
                alpha = mainPillAlpha
                transformOrigin = TransformOrigin(0f, 0.5f) // Anchor to Left
            }
            .bounceClick(
                scaleDown = 0.95f,
                onLongClick = if (!movie.isReleased && movie.mediaType != "tv" && optimisticWatchState != WatchState.WATCHED) {
                    { onPreviewWatch?.invoke() }
                } else null,
                onClick = {
                    if (!movie.isReleased) {
                        val next = if (optimisticWatchState == WatchState.NONE) WatchState.BOOKMARKED else WatchState.NONE
                        if (next == WatchState.BOOKMARKED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onOptimisticStateUpdate(next)
                        onStateChange(next)
                    } else if (movie.mediaType == "tv") {
                        if (optimisticWatchState == WatchState.NONE) {
                            onOptimisticStateUpdate(WatchState.BOOKMARKED)
                            onStateChange(WatchState.BOOKMARKED)
                        } else {
                            onEpisodesClick?.invoke()
                        }
                    } else {
                        if (optimisticWatchState == WatchState.WATCHED) {
                            onManageRewatches?.invoke()
                        } else {
                            val next = when (optimisticWatchState) {
                                WatchState.NONE -> WatchState.BOOKMARKED
                                WatchState.BOOKMARKED -> WatchState.WATCHED
                                WatchState.WATCHED -> WatchState.NONE
                                WatchState.DROPPED -> WatchState.BOOKMARKED
                            }
                            onOptimisticStateUpdate(next)
                            onStateChange(next)
                        }
                    }
                }
            )
            .hazeGlass(
                state = hazeState,
                shape = RoundedCornerShape(28.dp),
                borderColor = if (optimisticWatchState != WatchState.NONE) displayColor.copy(alpha = 0.75f) else HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
                useRefraction = true
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                if (w < 1f || h < 1f) return@drawBehind
                
                // TV Progress Border
                if (movie.mediaType == "tv" && optimisticWatchState != WatchState.NONE && animatedProgress > 0f) {
                    val strokeWidthPx = 4.dp.toPx()
                    val halfStroke = strokeWidthPx / 2f
                    val path = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                rect = androidx.compose.ui.geometry.Rect(halfStroke, halfStroke, w - halfStroke, h - halfStroke),
                                cornerRadius = CornerRadius(28.dp.toPx() - halfStroke)
                            )
                        )
                    }
                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(path, false)
                    val length = pathMeasure.length
                    
                    this.drawPath(
                        path = path,
                        color = displayColor,
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(length * animatedProgress, length),
                                -length * (1 - animatedProgress)
                            )
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            val bellRotation = remember { Animatable(0f) }
            
            LaunchedEffect(optimisticWatchState) {
                if (!movie.isReleased && optimisticWatchState != WatchState.NONE && optimisticWatchState != WatchState.WATCHED) {
                    delay(150)
                    bellRotation.animateTo(-25f, tween(60, easing = LinearEasing))
                    bellRotation.animateTo(20f, tween(100, easing = LinearEasing))
                    bellRotation.animateTo(-15f, tween(100, easing = LinearEasing))
                    bellRotation.animateTo(10f, tween(100, easing = LinearEasing))
                    bellRotation.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
                }
            }

            Crossfade(targetState = icon, label = "IconCrossfade") { currentIcon ->
                Icon(
                    imageVector = currentIcon,
                    contentDescription = null,
                    tint = displayColor,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            rotationZ = bellRotation.value
                            transformOrigin = TransformOrigin(0.5f, 0.2f) // Swing from the top
                        }
                )
            }

            if (label.isNotEmpty()) {
                Spacer(modifier = Modifier.width(10.dp))
                val isTvSeries = movie.mediaType == "tv"
                val tvMeta = remember(movie.id, movie.numberOfEpisodes, movie.seasons, movie.episodeRunTime, movie.runtime) {
                    if (isTvSeries) {
                        val totalEpisodes = movie.numberOfEpisodes?.takeIf { it > 0 }
                            ?: movie.seasons?.filter { it.seasonNumber > 0 }?.sumOf { it.episodeCount ?: 0 }?.takeIf { it > 0 }
                            ?: movie.seasons?.sumOf { it.episodeCount ?: 0 }?.takeIf { it > 0 }
                        val avgRuntime = movie.episodeRunTime?.filter { it > 0 }?.takeIf { it.isNotEmpty() }?.average()?.toInt()
                            ?: movie.runtime?.takeIf { it > 0 }
                            ?: movie.seasons?.flatMap { it.episodes ?: emptyList() }?.mapNotNull { it.runtime?.takeIf { r -> r > 0 } }?.takeIf { it.isNotEmpty() }?.average()?.toInt()
                        Pair(totalEpisodes, avgRuntime)
                    } else null
                }

                val watchedEpisodesCount = remember(movie.watchedEpisodes) {
                    movie.watchedEpisodes?.filterKeys { it != "0" }?.values?.sumOf { it.size } ?: 0
                }

                AnimatedContent(
                    targetState = optimisticWatchState,
                    transitionSpec = {
                        val slideDirection = if (targetState > initialState) 1 else -1
                        if (!movie.isReleased && targetState != WatchState.WATCHED && initialState != WatchState.WATCHED) {
                            (fadeIn(tween(250)) + slideInVertically(tween(350)) { -slideDirection * it / 2 })
                                .togetherWith(fadeOut(tween(200)) + slideOutVertically(tween(300)) { slideDirection * it / 2 })
                        } else {
                            (fadeIn(tween(250)) + slideInHorizontally(tween(350)) { slideDirection * it / 3 })
                                .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { -slideDirection * it / 3 })
                        }
                    },
                    label = "TextAnim"
                ) { targetState ->
                    val targetLabel = when {
                        targetState == WatchState.WATCHED -> if (movie.mediaType == "tv") stringResource(R.string.action_select_episodes) else stringResource(R.string.action_watched)
                        !movie.isReleased -> {
                            if (targetState != WatchState.NONE) stringResource(R.string.action_reminder_active)
                            else stringResource(R.string.action_remind_me)
                        }
                        targetState == WatchState.NONE -> if (movie.mediaType == "tv") stringResource(R.string.action_to_watch_tv) else stringResource(R.string.action_to_watch_movie)
                        targetState == WatchState.DROPPED -> stringResource(R.string.action_dropped)
                        targetState == WatchState.BOOKMARKED -> if (movie.mediaType == "tv") stringResource(R.string.action_select_episodes) else stringResource(R.string.action_mark_as_watched)
                        else -> ""
                    }

                    val tvSubtitle = if (isTvSeries && targetState == WatchState.NONE && tvMeta != null) {
                        val (totalEpisodes, avgRuntime) = tvMeta
                        when {
                            totalEpisodes != null && avgRuntime != null -> {
                                if (totalEpisodes == 1) {
                                    stringResource(R.string.tv_info_episode_single_and_duration, avgRuntime)
                                } else {
                                    stringResource(R.string.tv_info_episodes_and_duration, totalEpisodes, avgRuntime)
                                }
                            }
                            totalEpisodes != null -> {
                                if (totalEpisodes == 1) {
                                    stringResource(R.string.tv_info_episode_single)
                                } else {
                                    stringResource(R.string.tv_info_episodes_only, totalEpisodes)
                                }
                            }
                            avgRuntime != null -> {
                                stringResource(R.string.tv_info_duration_approx, avgRuntime)
                            }
                            else -> null
                        }
                    } else null

                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = targetLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.1.sp,
                                fontSize = if (isTvSeries && (targetState != WatchState.NONE || tvSubtitle != null)) 12.sp else 14.sp
                            ),
                            color = displayColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (tvSubtitle != null) {
                            Text(
                                text = tvSubtitle.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = displayColor.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (isTvSeries && targetState != WatchState.NONE && movie.isReleased) {
                            val percentage = (animatedProgress * 100).toInt()
                            val totalEpisodes = tvMeta?.first
                            val progressText = when {
                                percentage == 100 -> stringResource(R.string.action_completed)
                                totalEpisodes != null && totalEpisodes > 0 -> {
                                    val safeWatched = minOf(watchedEpisodesCount, totalEpisodes)
                                    "$safeWatched/$totalEpisodes • $percentage%"
                                }
                                else -> stringResource(R.string.action_percent_watched, percentage)
                            }
                            Text(
                                text = progressText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = displayColor.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
