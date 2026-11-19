package com.cinetrack.ui.components.comments

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.cinetrack.data.api.CommsUniConversationStats
import com.cinetrack.data.api.SourceCatalogRow
import com.cinetrack.data.model.AppComment
import com.cinetrack.data.model.CommentSortOption
import com.cinetrack.data.model.CommentSortOrder
import com.cinetrack.data.model.Episode
import com.cinetrack.data.model.Season
import com.cinetrack.ui.components.detail.DetailTranslationPromptModal
import dev.chrisbanes.haze.HazeState

/**
 * Host component managing all overlay modals and dialogs for the CommsUni discussion screen.
 */
@Composable
fun CommsUniCommentsModalsHost(
    // GIF Picker
    showGifPicker: Boolean,
    gifButtonBounds: Rect?,
    onDismissGifPicker: () -> Unit,
    onGifSelected: (String) -> Unit,
    // Filter / Sort
    showSortMenu: Boolean,
    conversationStats: CommsUniConversationStats?,
    comments: List<AppComment>,
    availableSources: List<SourceCatalogRow>,
    sortButtonBounds: Rect?,
    sortOption: CommentSortOption,
    sortOrder: CommentSortOrder,
    sourceFilters: Set<String>,
    languageFilters: Set<String>,
    onSortChanged: (CommentSortOption, CommentSortOrder, Set<String>, Set<String>) -> Unit,
    onDismissSortMenu: () -> Unit,
    // CommsUni Info
    showCommsUniInfo: Boolean,
    commsUniButtonBounds: Rect?,
    onDismissCommsUniInfo: () -> Unit,
    // Writing Tips
    showWritingTips: Boolean,
    writingTipsButtonBounds: Rect?,
    onDismissWritingTips: () -> Unit,
    // Translation Prompt
    showTranslationPrompt: Pair<String, String>?,
    onDismissTranslationPrompt: () -> Unit,
    onTranslatePromptConfirm: (commentId: String, text: String, requireWifi: Boolean) -> Unit,
    // Report Modal
    commentToReport: AppComment?,
    onDismissReport: () -> Unit,
    onReportConfirm: (category: String, detail: String?) -> Unit,
    onReportBlockUser: (AppComment) -> Unit,
    // Block Modal
    userToBlock: AppComment?,
    onDismissBlock: () -> Unit,
    onBlockConfirm: () -> Unit,
    // Delete Modal
    commentToDelete: AppComment?,
    onDismissDelete: () -> Unit,
    onDeleteConfirm: () -> Unit,
    // External Link Warning Modal
    pendingExternalUrl: String?,
    pendingExternalHost: String,
    onDismissExternalLink: () -> Unit,
    onConfirmExternalLink: (String) -> Unit,
    // Episode Discussion Picker
    showEpisodePickerSheet: Boolean,
    showSeasons: List<Season>,
    seasonEpisodes: Map<Int, List<Episode>>,
    isLoadingSeasonEpisodes: Boolean,
    currentSeasonNumber: Int?,
    currentEpisodeNumber: Int?,
    currentBackdropUrl: String?,
    episodesButtonBounds: Rect?,
    onLoadSeasonEpisodes: (Int) -> Unit,
    onSelectEpisode: (Int, Episode) -> Unit,
    onSelectMainDiscussion: () -> Unit,
    onDismissEpisodePicker: () -> Unit,
    // Theme & State
    accentColor: Color,
    globalHazeState: HazeState
) {
    // GIF Picker Modal Overlay
    GifPickerModal(
        isVisible = showGifPicker,
        triggerBounds = gifButtonBounds,
        onDismiss = onDismissGifPicker,
        hazeState = globalHazeState,
        accentColor = accentColor,
        onGifSelected = onGifSelected
    )

    // Sort & Filter Dialog Overlay
    CommsUniFilterModal(
        isVisible = showSortMenu,
        conversationStats = conversationStats,
        comments = comments,
        availableSources = availableSources,
        sortButtonBounds = sortButtonBounds,
        sortOption = sortOption,
        sortOrder = sortOrder,
        sourceFilters = sourceFilters,
        languageFilters = languageFilters,
        hazeState = globalHazeState,
        onSortChanged = onSortChanged,
        onDismissRequest = onDismissSortMenu
    )

    // CommsUni Project Info Modal
    CommsUniInfoModal(
        isVisible = showCommsUniInfo,
        onDismiss = onDismissCommsUniInfo,
        triggerBounds = commsUniButtonBounds,
        hazeState = globalHazeState
    )

    // Writing Tips Modal
    WritingTipsModal(
        isVisible = showWritingTips,
        onDismiss = onDismissWritingTips,
        triggerBounds = writingTipsButtonBounds,
        accentColor = accentColor,
        hazeState = globalHazeState
    )

    // Translation Prompt Dialog
    DetailTranslationPromptModal(
        showTranslationPrompt = showTranslationPrompt,
        onDismiss = onDismissTranslationPrompt,
        onTranslate = onTranslatePromptConfirm,
        hazeState = globalHazeState,
        accentColor = accentColor
    )

    // Report Dialog Overlay
    CommentReportModal(
        comment = commentToReport,
        onDismiss = onDismissReport,
        onReport = onReportConfirm,
        onBlockUser = onReportBlockUser,
        hazeState = globalHazeState
    )

    // Block User Dialog Overlay
    CommentBlockModal(
        comment = userToBlock,
        onDismiss = onDismissBlock,
        onConfirm = onBlockConfirm,
        hazeState = globalHazeState
    )

    // Delete Dialog Overlay
    CommentDeleteModal(
        comment = commentToDelete,
        onDismiss = onDismissDelete,
        onConfirm = onDeleteConfirm,
        hazeState = globalHazeState
    )

    // External Link Warning Dialog Overlay
    ExternalLinkWarningModal(
        url = pendingExternalUrl,
        host = pendingExternalHost,
        onDismiss = onDismissExternalLink,
        onConfirm = onConfirmExternalLink,
        hazeState = globalHazeState
    )

    // Episode Discussion Picker Modal
    EpisodeDiscussionPickerModal(
        isVisible = showEpisodePickerSheet,
        showSeasons = showSeasons,
        seasonEpisodes = seasonEpisodes,
        isLoadingEpisodes = isLoadingSeasonEpisodes,
        currentSeason = currentSeasonNumber,
        currentEpisode = currentEpisodeNumber,
        fallbackBackdropPath = currentBackdropUrl,
        onLoadSeasonEpisodes = onLoadSeasonEpisodes,
        onSelectEpisode = onSelectEpisode,
        onSelectMainDiscussion = onSelectMainDiscussion,
        onDismiss = onDismissEpisodePicker,
        accentColor = accentColor,
        hazeState = globalHazeState,
        triggerBounds = episodesButtonBounds
    )
}
