package com.cinetrack.ui.components.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.detail.DetailActions
import com.cinetrack.ui.viewmodel.WatchState
import dev.chrisbanes.haze.HazeState

/**
 * Floating bottom action bar containing [DetailActions].
 * Anchors actions over the scrolling detail content with bottom padding offset.
 */
@Composable
fun DetailFloatingActionBar(
    visible: Boolean,
    movie: Movie,
    movieStatus: String?,
    watchState: WatchState,
    progress: Float,
    accentColor: Color,
    hazeState: HazeState?,
    bottomPadding: Dp,
    onStateChange: (WatchState) -> Unit,
    onRemove: () -> Unit,
    onEpisodesClick: () -> Unit,
    onManageRewatches: () -> Unit,
    onPreviewWatch: () -> Unit,
    onToggleRemindersMuted: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) { it } + fadeIn(tween(300)),
        exit = slideOutVertically(
            animationSpec = tween(250)
        ) { it } + fadeOut(tween(200)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding + 32.dp),
            contentAlignment = Alignment.Center
        ) {
            DetailActions(
                movie = movie,
                movieStatus = movieStatus,
                watchState = watchState,
                progress = progress,
                accentColor = accentColor,
                hazeState = hazeState,
                onStateChange = onStateChange,
                onRemove = onRemove,
                onEpisodesClick = onEpisodesClick,
                onManageRewatches = onManageRewatches,
                onPreviewWatch = onPreviewWatch,
                onToggleRemindersMuted = onToggleRemindersMuted
            )
        }
    }
}
