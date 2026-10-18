package com.cinetrack.ui.components.comments

import com.cinetrack.data.model.AppComment
import com.cinetrack.data.model.CommentSortOption
import com.cinetrack.data.model.CommentSortOrder
import com.cinetrack.data.model.filterDeletedWithoutReplies

fun buildFlatCommentTree(
    comments: List<AppComment>,
    expandedComments: Set<String>,
    sortOption: CommentSortOption,
    sortOrder: CommentSortOrder,
    sourceFilters: Set<String> = emptySet(),
    languageFilters: Set<String> = emptySet()
): List<AppComment> {
    val nonDeleted = comments.filterDeletedWithoutReplies()
    val hasSourceFilter = sourceFilters.isNotEmpty()
    val hasLanguageFilter = languageFilters.isNotEmpty()

    val matchingIds = if (hasSourceFilter || hasLanguageFilter) {
        val ids = mutableSetOf<String>()
        nonDeleted.forEach { c ->
            val matchesSource = !hasSourceFilter || sourceFilters.any { c.originSlug.equals(it, ignoreCase = true) }
            val matchesLanguage = !hasLanguageFilter || languageFilters.any { langCode ->
                c.language?.equals(langCode, ignoreCase = true) == true
            }
            if (matchesSource && matchesLanguage) {
                ids.add(c.id)
                var p = c.parentId
                while (!p.isNullOrBlank()) {
                    ids.add(p)
                    p = comments.find { it.id == p }?.parentId
                }
            }
        }
        ids
    } else null

    val filtered = if (matchingIds != null) nonDeleted.filter { it.id in matchingIds } else nonDeleted
    val tree = mutableListOf<AppComment>()
    val map = filtered.groupBy { it.parentId?.takeIf { p -> p.isNotBlank() } }

    fun addChildren(parentId: String?) {
        val children = map[parentId]?.let { list ->
            when (sortOption) {
                CommentSortOption.DATE -> {
                    if (sortOrder == CommentSortOrder.ASC) list.sortedBy { it.createdAt?.seconds ?: 0L }
                    else list.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                CommentSortOption.LIKES -> {
                    if (sortOrder == CommentSortOrder.ASC) list.sortedWith(compareBy({ it.likesCount }, { it.createdAt?.seconds ?: 0L }))
                    else list.sortedWith(compareByDescending<AppComment> { it.likesCount }.thenByDescending { it.createdAt?.seconds ?: 0L })
                }
            }
        } ?: return

        for (child in children) {
            tree.add(child)
            if (expandedComments.contains(child.id)) {
                addChildren(child.id)
            }
        }
    }

    addChildren(null)
    return tree
}
