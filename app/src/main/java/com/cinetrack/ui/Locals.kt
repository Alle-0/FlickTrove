package com.cinetrack.ui

import android.content.Intent
import androidx.compose.runtime.MutableState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp

val LocalAppPadding = compositionLocalOf { PaddingValues(0.dp) }
val LocalHazeState = compositionLocalOf<dev.chrisbanes.haze.HazeState?> { null }
val LocalGlobalHazeState = compositionLocalOf<dev.chrisbanes.haze.HazeState?> { null }
val LocalDeepLinkIntent = staticCompositionLocalOf<MutableState<Intent?>> { error("No intent") }
// Callback to open the filter modal from inside a tab; passes optional trigger Rect
val LocalFilterRequest = compositionLocalOf<((Rect?) -> Unit)?> { null }
val LocalSurpriseMeRequest = compositionLocalOf<((Rect?) -> Unit)?> { null }

fun interface SearchOverlayTrigger {
    fun open(
        offset: androidx.compose.ui.geometry.Offset?,
        genreId: Long?,
        genreName: String?,
        keywordId: Long?,
        keywordName: String?,
        category: String?
    )

    operator fun invoke(
        offset: androidx.compose.ui.geometry.Offset? = null,
        genreId: Long? = null,
        genreName: String? = null,
        keywordId: Long? = null,
        keywordName: String? = null,
        category: String? = null
    ) {
        open(offset, genreId, genreName, keywordId, keywordName, category)
    }
}

val LocalSearchOverlay = compositionLocalOf<SearchOverlayTrigger?> { null }

data class FilterModalConfig(
    val triggerBounds: Rect? = null,
    val isVisti: Boolean = false,
    val isFolder: Boolean = false,
    val category: String = "movie",
    val sortConfig: com.cinetrack.data.model.SortConfig,
    val onSortConfigChanged: (com.cinetrack.data.model.SortConfig) -> Unit,
    val availableCountries: List<String> = emptyList()
)

val LocalActiveFilterConfig = compositionLocalOf<MutableState<FilterModalConfig?>> { androidx.compose.runtime.mutableStateOf(null) }
val LocalFolderReorderRequest = compositionLocalOf<MutableState<(() -> Unit)?>> { androidx.compose.runtime.mutableStateOf(null) }

data class FolderReorderModalConfig(
    val visible: Boolean,
    val movies: kotlinx.collections.immutable.ImmutableList<com.cinetrack.data.model.Movie>,
    val folderName: String,
    val folderColor: String? = null,
    val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    val onSave: () -> Unit,
    val onDismiss: () -> Unit
)

val LocalFolderReorderConfig = compositionLocalOf<MutableState<FolderReorderModalConfig?>> { androidx.compose.runtime.mutableStateOf(null) }

data class TrophyFilterModalConfig(
    val config: com.cinetrack.ui.components.badge.TrophyFilterConfig,
    val onApply: (com.cinetrack.ui.components.badge.TrophyFilterConfig) -> Unit
)

val LocalActiveTrophyFilterConfig = compositionLocalOf<MutableState<TrophyFilterModalConfig?>> { androidx.compose.runtime.mutableStateOf(null) }

data class TrophyDetailModalConfig(
    val item: com.cinetrack.ui.components.badge.TrophyRoomItemUi,
    val onClose: () -> Unit,
    val onVerifySupporter: (suspend (String) -> com.cinetrack.data.repository.BadgeRepository.SupporterVerificationResult)? = null,
    val onMovieClick: ((Long, String) -> Unit)? = null,
    val onResolveUnlockingMovies: (suspend (String) -> List<com.cinetrack.data.model.Movie>)? = null,
    val onResolveUnlockingMovie: (suspend (String) -> com.cinetrack.data.model.Movie?)? = null
)

val LocalActiveTrophyDetailConfig = compositionLocalOf<MutableState<TrophyDetailModalConfig?>?> { null }

data class ScrollToTopState(
    val isVisible: Boolean = false,
    val scrollProgress: Float = if (isVisible) 1f else 0f,
    val onScrollToTop: () -> Unit = {}
)

val LocalScrollToTopState = compositionLocalOf<MutableState<ScrollToTopState>> { androidx.compose.runtime.mutableStateOf(ScrollToTopState()) }

@androidx.compose.runtime.Stable
class ModalHostState {
    private val modalState = androidx.compose.runtime.mutableStateOf<(@androidx.compose.runtime.Composable () -> Unit)?>(null)

    val currentModal: (@androidx.compose.runtime.Composable () -> Unit)?
        get() = modalState.value

    val isModalOpen: Boolean
        get() = modalState.value != null

    fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        modalState.value = content
    }

    fun dismiss() {
        modalState.value = null
    }
}

val LocalModalHost = androidx.compose.runtime.staticCompositionLocalOf { ModalHostState() }


