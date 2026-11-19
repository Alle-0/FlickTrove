package com.cinetrack.ui.screens

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cinetrack.ui.components.comments.CommsUniCommentsContent
import com.cinetrack.ui.viewmodel.CommsUniViewModel

class CommsUniCommentsScreen(
    private val tvdbId: Int? = null,
    private val mediaId: String = "",
    private val mediaType: String = "movie",
    private val seasonNumber: Int? = null,
    private val episodeNumber: Int? = null,
    private val accentColorValue: Long = 0L,
    private val mediaTitle: String = "",
    private val mediaImage: String? = null,
    private val backdropUrl: String? = null,
    private val releaseYear: String? = null,
    private val focusInputOnLaunch: Boolean = false,
    private val targetCommentId: String? = null,
    private val imdbId: String? = null,
    private val initialImageUri: String? = null,
    private val episodeTitle: String? = null
) : Screen {

    override val key: String = "CommentsScreen_${mediaType}_${mediaId}_${java.util.UUID.randomUUID()}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getViewModel<CommsUniViewModel>()

        CommsUniCommentsContent(
            viewModel = viewModel,
            navigator = navigator,
            tvdbId = tvdbId,
            mediaId = mediaId,
            mediaType = mediaType,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            accentColorValue = accentColorValue,
            mediaTitle = mediaTitle,
            mediaImage = mediaImage,
            backdropUrl = backdropUrl,
            releaseYear = releaseYear,
            focusInputOnLaunch = focusInputOnLaunch,
            targetCommentId = targetCommentId,
            imdbId = imdbId,
            initialImageUri = initialImageUri,
            episodeTitle = episodeTitle
        )
    }
}
