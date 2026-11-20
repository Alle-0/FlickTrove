package com.cinetrack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cinetrack.ui.LocalSearchOverlay
import com.cinetrack.ui.components.detail.MovieDetailScreenContent
import com.cinetrack.ui.viewmodel.MovieDetailViewModel
import com.cinetrack.ui.viewmodel.SettingsViewModel

data class MovieDetailScreen(
    val movieId: Long,
    val mediaType: String,
    val openComments: Boolean = false,
    val targetCommentId: String? = null,
    val targetSeasonNumber: Int? = null,
    val targetEpisodeNumber: Int? = null,
    val preloadedTitle: String? = null,
    val preloadedPosterPath: String? = null,
    val preloadedBackdropPath: String? = null,
    val preloadedLogoPath: String? = null,
    val preloadedAccentColor: String? = null
) : Screen {
    override val key: ScreenKey = uniqueScreenKey
    @Transient var hasHandledCommentsNavigation: Boolean = false

    @Composable
    override fun Content() {
        val viewModel = getViewModel<MovieDetailViewModel>()
        
        val context = LocalContext.current
        var currentContext = context
        while (currentContext is android.content.ContextWrapper && currentContext !is androidx.activity.ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? androidx.activity.ComponentActivity
        val settingsViewModel = if (activity != null) {
            androidx.hilt.navigation.compose.hiltViewModel<SettingsViewModel>(activity)
        } else {
            getViewModel<SettingsViewModel>()
        }
        
        val navigator = LocalNavigator.currentOrThrow
        val detailStackDepth = remember(navigator.items) {
            navigator.items.count { it is MovieDetailScreen || it is PersonDetailScreen }
        }

        LaunchedEffect(movieId, mediaType) {
            viewModel.initMovie(movieId, mediaType)
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshComments()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        var wasInBackground by remember { mutableStateOf(false) }
        LaunchedEffect(navigator.lastItem) {
            if (navigator.lastItem.key == this@MovieDetailScreen.key) {
                if (wasInBackground) {
                    viewModel.refreshComments()
                }
            } else {
                wasInBackground = true
            }
        }

        val searchOverlay = LocalSearchOverlay.current

        MovieDetailScreenContent(
            viewModel = viewModel,
            movieId = movieId,
            mediaType = mediaType,
            openComments = openComments,
            targetCommentId = targetCommentId,
            targetSeasonNumber = targetSeasonNumber,
            targetEpisodeNumber = targetEpisodeNumber,
            preloadedTitle = preloadedTitle,
            preloadedPosterPath = preloadedPosterPath,
            preloadedBackdropPath = preloadedBackdropPath,
            preloadedLogoPath = preloadedLogoPath,
            preloadedAccentColor = preloadedAccentColor,
            settingsViewModel = settingsViewModel,
            paddingValues = PaddingValues(0.dp),
            onBackClick = {
                if (navigator.canPop) {
                    navigator.pop()
                }
            },
            onMovieClick = { movie ->
                navigator.push(MovieDetailScreen(movie.id, movie.mediaType))
            },
            detailStackDepth = detailStackDepth,
            onPersonClick = { personId, profilePath ->
                navigator.push(PersonDetailScreen(personId, profilePath))
            },
            onGenreClick = { genreId, genreName, offset ->
                searchOverlay?.invoke(offset, genreId, genreName, null, null)
            },
            onKeywordClick = { keywordId, keywordName, offset ->
                searchOverlay?.invoke(offset, null, null, keywordId, keywordName)
            },
            onHomeClick = {
                navigator.popUntilRoot()
            },
            initialHasHandledComments = hasHandledCommentsNavigation,
            onCommentsHandled = { hasHandledCommentsNavigation = true }
        )
    }
}
