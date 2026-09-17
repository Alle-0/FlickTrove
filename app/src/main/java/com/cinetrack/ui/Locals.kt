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
val LocalDeepLinkIntent = staticCompositionLocalOf<MutableState<Intent?>> { error("No intent") }
// Callback to open the filter modal from inside a tab; passes optional trigger Rect
val LocalFilterRequest = compositionLocalOf<((Rect?) -> Unit)?> { null }
val LocalSearchOverlay = compositionLocalOf<((offset: androidx.compose.ui.geometry.Offset?, genreId: Long?, genreName: String?, keywordId: Long?, keywordName: String?) -> Unit)?> { null }
val LocalSurpriseMeRequest = compositionLocalOf<((Rect?) -> Unit)?> { null }

data class FilterModalConfig(
    val triggerBounds: Rect? = null,
    val isVisti: Boolean = false,
    val isFolder: Boolean = false,
    val category: String = "movie",
    val sortConfig: com.cinetrack.data.model.SortConfig,
    val onSortConfigChanged: (com.cinetrack.data.model.SortConfig) -> Unit
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
