package com.cinetrack.ui.components.comments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import com.cinetrack.data.api.CommsUniConversationStats
import com.cinetrack.data.api.SourceCatalogRow
import com.cinetrack.data.model.AppComment
import com.cinetrack.data.model.CommentSortOption
import com.cinetrack.data.model.CommentSortOrder
import com.cinetrack.data.model.SortConfig
import com.cinetrack.ui.components.dialog.HomeFilterModal
import dev.chrisbanes.haze.HazeState

@Composable
fun CommsUniFilterModal(
    isVisible: Boolean,
    conversationStats: CommsUniConversationStats?,
    comments: List<AppComment>,
    availableSources: List<SourceCatalogRow>,
    sortButtonBounds: Rect?,
    sortOption: CommentSortOption,
    sortOrder: CommentSortOrder,
    sourceFilters: Set<String>,
    languageFilters: Set<String>,
    hazeState: HazeState?,
    onSortChanged: (sortOption: CommentSortOption, sortOrder: CommentSortOrder, sourceFilters: Set<String>, languageFilters: Set<String>) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sourceCountsMap = remember(conversationStats, comments) {
        val map = mutableMapOf<String, Int>()
        conversationStats?.sourceCounts?.forEach { sc ->
            val key = sc.source.lowercase().trim()
            if (key.isNotBlank()) {
                map[key] = (map[key] ?: 0) + sc.count
            }
        }
        val flickTroveCount = comments.count { it.originSlug.equals("flicktrove", ignoreCase = true) }
        if (flickTroveCount > (map["flicktrove"] ?: 0)) {
            map["flicktrove"] = flickTroveCount
        }
        if (map.isEmpty() && comments.isNotEmpty()) {
            comments.forEach { c ->
                val slug = c.originSlug.ifBlank { "flicktrove" }.lowercase().trim()
                map[slug] = (map[slug] ?: 0) + 1
            }
        }
        map
    }

    val languageCountsMap = remember(conversationStats, comments) {
        val map = mutableMapOf<String, Int>()
        conversationStats?.languageCounts?.forEach { lc ->
            val key = lc.language.lowercase().trim()
            if (key.isNotBlank()) {
                map[key] = (map[key] ?: 0) + lc.count
            }
        }
        if (map.isEmpty() && comments.isNotEmpty()) {
            comments.forEach { c ->
                val lang = c.language?.lowercase()?.trim()
                if (!lang.isNullOrBlank()) {
                    map[lang] = (map[lang] ?: 0) + 1
                }
            }
        }
        map
    }

    val commentLanguages = remember(conversationStats, comments) {
        val fromStats = conversationStats?.languageCounts?.mapNotNull { it.language.lowercase().trim() } ?: emptyList()
        val fromComments = comments.mapNotNull { it.language?.lowercase()?.trim() }
        (fromStats + fromComments).filter { it.isNotBlank() }.distinct()
    }

    HomeFilterModal(
        isVisible = isVisible,
        isCommentsFilter = true,
        availableSources = availableSources,
        availableLanguages = commentLanguages,
        sourceCounts = sourceCountsMap,
        languageCounts = languageCountsMap,
        triggerBounds = sortButtonBounds,
        sortConfig = SortConfig(
            sortType = if (sortOption == CommentSortOption.DATE) "date" else "likes",
            sortDirection = if (sortOrder == CommentSortOrder.DESC) "desc" else "asc",
            selectedSources = sourceFilters.toList(),
            selectedLanguages = languageFilters.toList()
        ),
        hazeState = hazeState,
        onSortConfigChanged = { newConfig ->
            val newSortOption = if (newConfig.sortType == "date") CommentSortOption.DATE else CommentSortOption.LIKES
            val newSortOrder = if (newConfig.sortDirection == "desc") CommentSortOrder.DESC else CommentSortOrder.ASC
            val newSourceFilters = (newConfig.selectedSources + listOfNotNull(newConfig.selectedSource)).toSet()
            val newLanguageFilters = (newConfig.selectedLanguages + listOfNotNull(newConfig.selectedLanguage)).toSet()
            onSortChanged(newSortOption, newSortOrder, newSourceFilters, newLanguageFilters)
        },
        onDismissRequest = onDismissRequest
    )
}
