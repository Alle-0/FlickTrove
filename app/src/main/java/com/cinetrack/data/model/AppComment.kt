package com.cinetrack.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class AppComment(
    val id: String = "",
    val mediaId: String = "",
    val mediaType: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val userAvatarUrl: String = "",
    val text: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val likesCount: Int = 0,
    val likedBy: List<String> = ArrayList(),
    val parentId: String? = null,
    val parentUserId: String? = null,
    val rootCommentId: String? = null,
    val repliesCount: Int = 0,
    val depth: Int = 0,
    val isDeleted: Boolean = false,
    @get:PropertyName("isSpoiler")
    @set:PropertyName("isSpoiler")
    var isSpoiler: Boolean = false,
    
    // --- Campi per CommsUni ---
    val originSlug: String = "",
    val originName: String = "",
    val originColor: String? = null,
    val originIcon: String? = null,
    val archivedLikes: Int = 0,
    val nativeLikes: Int = 0,
    val attachedMedia: List<String> = emptyList(),
    val rating: Double? = null
) {
    val isEffectivelyDeleted: Boolean
        get() = isDeleted ||
                userId.isBlank() ||
                (text.isBlank() && userDisplayName.isBlank()) ||
                (text.trim().startsWith("[") && text.trim().endsWith("]") && (userDisplayName.isBlank() || (userDisplayName.trim().startsWith("[") && userDisplayName.trim().endsWith("]"))))
}

enum class CommentSortOption { DATE, LIKES }
enum class CommentSortOrder { ASC, DESC }

fun List<AppComment>.filterDeletedWithoutReplies(): List<AppComment> {
    val childrenMap = this.groupBy { it.parentId?.takeIf { p -> p.isNotBlank() } }

    fun hasActiveDescendants(commentId: String, visited: MutableSet<String> = mutableSetOf()): Boolean {
        if (!visited.add(commentId)) return false
        val children = childrenMap[commentId] ?: return false
        for (child in children) {
            if (!child.isEffectivelyDeleted) return true
            if (hasActiveDescendants(child.id, visited)) return true
        }
        return false
    }

    return this.filter { comment ->
        if (!comment.isEffectivelyDeleted) {
            true
        } else {
            comment.repliesCount > 0 || hasActiveDescendants(comment.id)
        }
    }
}
