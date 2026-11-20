package com.cinetrack.ui.components.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.Navigator
import com.cinetrack.R
import com.cinetrack.data.api.Video
import com.cinetrack.data.model.GlobalMovieStats
import com.cinetrack.ui.components.comments.ExternalLinkWarningModal
import com.cinetrack.ui.components.episodes.EpisodesBottomSheet
import com.cinetrack.ui.screens.CommsUniCommentsScreen
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.UiText
import com.cinetrack.ui.viewmodel.DetailEvent
import com.cinetrack.ui.viewmodel.DetailUiState
import com.cinetrack.ui.viewmodel.MovieDetailViewModel
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.toComposeColor
import dev.chrisbanes.haze.HazeState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Host component managing the registration, state and lifecycle of all bottom sheets,
 * dialogs, and modals for MovieDetailScreen.
 */
@Composable
fun MovieDetailModalsHost(
    cachedSuccess: DetailUiState.Success?,
    viewModel: MovieDetailViewModel,
    navigator: Navigator,
    localHazeState: HazeState,
    rootHazeState: HazeState,
    globalAccentColor: Color,
    globalStats: GlobalMovieStats?,
    currentImageQuality: ImageQuality,
    // Bottom Sheets & Dialog states
    showEpisodesSheet: Boolean,
    onDismissEpisodesSheet: () -> Unit,
    showWatchHistorySheet: Boolean,
    onDismissWatchHistorySheet: () -> Unit,
    showWatchDatePrompt: Boolean,
    onDismissWatchDatePrompt: () -> Unit,
    onDateSelected: (Instant?) -> Unit,
    showCheckInModal: Boolean,
    onDismissCheckInModal: () -> Unit,
    pendingExternalUrl: String?,
    pendingExternalHost: String,
    onDismissExternalLink: () -> Unit,
    showRatingInfoDialog: Boolean,
    onDismissRatingInfoDialog: () -> Unit,
    showTranslationPrompt: Pair<Long, String>?,
    showCoverSelectionSheet: Boolean,
    onDismissCoverSelectionSheet: () -> Unit,
    showTrailerModal: Boolean,
    trailerTriggerBounds: Rect?,
    onDismissTrailerModal: () -> Unit
) {
    val context = LocalContext.current
    val activeMovie = cachedSuccess?.movieEntry
    val vividAccentColor = remember(activeMovie?.accentColor, globalAccentColor) {
        ColorUtils.ensureVividAccent(
            activeMovie?.accentColor.toComposeColor(globalAccentColor)
        )
    }

    // 1. Episodes Bottom Sheet
    if (showEpisodesSheet && cachedSuccess != null) {
        val activeMovie = cachedSuccess.movieEntry
        EpisodesBottomSheet(
            movie = activeMovie,
            viewModel = viewModel,
            hazeState = localHazeState,
            onDismiss = onDismissEpisodesSheet,
            onEpisodeCommentsClick = { seasonNumber, episode ->
                val epStill = episode.stillPath?.let {
                    buildTmdbImageUrl(it, ImageType.BACKDROP, ImageQuality.HIGH)
                }
                val seasonPoster = cachedSuccess.seasonDetails[seasonNumber]?.posterPath?.let {
                    buildTmdbImageUrl(it, ImageType.POSTER, ImageQuality.HIGH)
                }
                val image = epStill ?: seasonPoster ?: buildTmdbImageUrl(
                    cachedSuccess.details.posterPath ?: cachedSuccess.details.backdropPath,
                    ImageType.POSTER,
                    ImageQuality.HIGH
                )
                val backdrop = epStill ?: buildTmdbImageUrl(
                    cachedSuccess.movieEntry.customBackdropPath ?: cachedSuccess.movieEntry.backdropPath
                    ?: cachedSuccess.details.backdropPath ?: cachedSuccess.details.posterPath,
                    ImageType.BACKDROP,
                    ImageQuality.HIGH
                )
                val tvdbId = viewModel.tvdbId ?: cachedSuccess.details.externalIds?.tvdbId
                val imdbId = cachedSuccess.details.externalIds?.imdbId
                val releaseYear = episode.airDate?.take(4)
                    ?: (cachedSuccess.details.releaseDate ?: cachedSuccess.details.firstAirDate)?.take(4)
                val seriesTitle = cachedSuccess.details.name ?: cachedSuccess.details.title ?: ""

                navigator.push(
                    CommsUniCommentsScreen(
                        tvdbId = tvdbId,
                        mediaId = viewModel.movieId.toString(),
                        mediaType = viewModel.mediaType,
                        seasonNumber = seasonNumber,
                        episodeNumber = episode.episodeNumber,
                        accentColorValue = globalAccentColor.value.toLong(),
                        mediaTitle = seriesTitle,
                        mediaImage = image,
                        backdropUrl = backdrop,
                        releaseYear = releaseYear,
                        focusInputOnLaunch = false,
                        imdbId = imdbId,
                        episodeTitle = episode.name
                    )
                )
            }
        )
    }

    // 2. Watch History Bottom Sheet
    if (showWatchHistorySheet && cachedSuccess != null) {
        val activeMovie = cachedSuccess.movieEntry
        WatchHistoryBottomSheet(
            movie = activeMovie,
            history = cachedSuccess.watchHistory,
            accentColor = vividAccentColor,
            onDismiss = onDismissWatchHistorySheet,
            onAddRewatch = { viewModel.logRewatch() },
            onUpdateDate = { entity, newDate -> viewModel.updateWatchHistoryDate(entity, newDate) },
            onDelete = { entity -> viewModel.deleteWatchHistory(entity) },
            onUpdateEpisodeDate = { entity, seasonNumber, episodeNumber, newDateIso ->
                viewModel.updateEpisodeWatchDate(entity, seasonNumber, episodeNumber, newDateIso)
            },
            onUpdateSeasonDate = { entity, seasonNumber, episodeNumbers, newDateIso ->
                viewModel.updateSeasonEpisodesWatchDate(entity, seasonNumber, episodeNumbers, newDateIso)
            },
            hazeState = localHazeState
        )
    }

    // 3. Watch Date Prompt Modal
    if (showWatchDatePrompt && activeMovie != null) {
        WatchDatePromptModal(
            hazeState = localHazeState,
            accentColor = vividAccentColor,
            releaseDate = activeMovie.releaseDate?.let { dateStr ->
                try {
                    if (dateStr.length >= 10) {
                        LocalDate.parse(dateStr.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            },
            onDismiss = onDismissWatchDatePrompt,
            onDateSelected = onDateSelected
        )
    }

    // 4. Check-in Wizard Modal
    if (showCheckInModal && cachedSuccess != null) {
        CheckInModal(
            visible = showCheckInModal,
            movie = cachedSuccess.movieEntry,
            globalStats = globalStats,
            cast = cachedSuccess.cast,
            characterImages = cachedSuccess.characterImages,
            accentColor = vividAccentColor,
            hazeState = localHazeState,
            onSave = { rating, vibes, mvp, charUrl ->
                viewModel.onEvent(DetailEvent.SaveCheckIn(rating, vibes, mvp, charUrl))
            },
            onDismiss = onDismissCheckInModal
        )
    }

    // 5. External Link Warning Modal
    ExternalLinkWarningModal(
        url = pendingExternalUrl,
        host = pendingExternalHost,
        onDismiss = onDismissExternalLink,
        onConfirm = { linkUrl ->
            onDismissExternalLink()
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                viewModel.emitMessage(UiText.StringResource(R.string.external_link_error_no_browser))
            }
        },
        hazeState = localHazeState
    )

    // 6. Rating & Age Certification Dialog
    val currentRatings = cachedSuccess?.externalRatings
    val isTvShow = cachedSuccess?.movieEntry?.mediaType == "tv"
    DetailRatingInfoDialog(
        visible = showRatingInfoDialog,
        onDismiss = onDismissRatingInfoDialog,
        hazeState = localHazeState,
        countryCode = currentRatings?.certificationCountry ?: "US",
        isTv = isTvShow,
        onViewGuideClick = null
    )

    // 7. Comment Translation Prompt Modal
    DetailTranslationPromptModal(
        showTranslationPrompt = showTranslationPrompt,
        onDismiss = { viewModel.dismissTranslationPrompt() },
        onTranslate = { commentId, text, requireWifi ->
            viewModel.translateComment(commentId, text, requireWifi)
        },
        hazeState = localHazeState,
        accentColor = globalAccentColor
    )

    // 8. Custom Cover Selection Modal
    DetailCoverSelectionModal(
        isVisible = showCoverSelectionSheet && cachedSuccess != null,
        onDismiss = onDismissCoverSelectionSheet,
        movieEntry = cachedSuccess?.movieEntry,
        backdrops = cachedSuccess?.details?.images?.backdrops ?: emptyList(),
        currentImageQuality = currentImageQuality,
        accentColor = globalAccentColor,
        hazeState = localHazeState,
        onSelectCover = { filePath ->
            viewModel.onEvent(DetailEvent.UpdateCustomCover(filePath))
        }
    )

    // 9. Trailer Modal
    if (cachedSuccess != null) {
        val modalVideos = remember(cachedSuccess.videos, cachedSuccess.trailers) {
            val vids = cachedSuccess.videos
            if (vids.isNotEmpty()) {
                vids.filter { it.site == "YouTube" || it.site == "Vimeo" }
            } else {
                cachedSuccess.trailers.map {
                    Video(key = it, site = "YouTube", type = "Trailer", name = "Trailer")
                }
            }
        }
        if (modalVideos.isNotEmpty()) {
            DetailTrailerModal(
                isVisible = showTrailerModal,
                videos = modalVideos,
                accentColor = globalAccentColor,
                hazeState = localHazeState,
                triggerBounds = trailerTriggerBounds,
                onDismiss = onDismissTrailerModal
            )
        }
    }
}
