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
    val rating: Double? = null,
    val language: String? = null,
    val isOnCommsUni: Boolean = false
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

fun normalizeCommentText(text: String): String {
    return text.replace("\r\n", "\n")
        .replace("\r", "\n")
        .lines()
        .joinToString("\n") { it.trim() }
        .trim()
}

fun isSameCommentText(t1: String, t2: String): Boolean {
    val n1 = normalizeCommentText(t1)
    val n2 = normalizeCommentText(t2)
    if (n1.isBlank() && n2.isBlank()) return true
    if (n1 == n2) return true

    // Rimuoviamo qualsiasi whitespace per gestire differenze di a capo o formattazione
    val s1 = n1.filterNot { it.isWhitespace() }
    val s2 = n2.filterNot { it.isWhitespace() }
    return s1.isNotEmpty() && s1 == s2
}

fun isSameAuthor(c1: AppComment, c2: AppComment): Boolean {
    val u1 = c1.userDisplayName.trim()
    val u2 = c2.userDisplayName.trim()
    val id1 = c1.userId.trim()
    val id2 = c2.userId.trim()

    // Se entrambi hanno userId noto e coincidono
    if (id1.isNotBlank() && id2.isNotBlank() && id1.equals(id2, ignoreCase = true)) return true

    // Se entrambi hanno un displayName valido e coincidono
    if (u1.isNotBlank() && u2.isNotBlank() && u1.equals(u2, ignoreCase = true)) return true

    // Se uno dei due non ha displayName, ma non hanno userId discordanti
    if ((u1.isBlank() || u2.isBlank()) && (id1.isBlank() || id2.isBlank() || id1.equals(id2, ignoreCase = true))) {
        return true
    }

    return false
}

fun isDuplicateComment(c1: AppComment, c2: AppComment): Boolean {
    if (c1.id.isNotBlank() && c2.id.isNotBlank() && c1.id == c2.id) return true
    return isSameAuthor(c1, c2) && isSameCommentText(c1.text, c2.text) && c1.parentId == c2.parentId
}

fun mergeComments(primary: AppComment, secondary: AppComment): AppComment {
    val avatar = primary.userAvatarUrl.trim().takeIf { it.isNotBlank() }
        ?: secondary.userAvatarUrl.trim().takeIf { it.isNotBlank() }
        ?: ""
    val name = primary.userDisplayName.trim().takeIf { it.isNotBlank() }
        ?: secondary.userDisplayName.trim().takeIf { it.isNotBlank() }
        ?: ""
    val likes = maxOf(primary.likesCount, secondary.likesCount)
    val likedBy = (primary.likedBy + secondary.likedBy).distinct()
    val originSlug = primary.originSlug.takeIf { it.isNotBlank() } ?: secondary.originSlug
    val originName = primary.originName.takeIf { it.isNotBlank() } ?: secondary.originName
    val originColor = primary.originColor ?: secondary.originColor
    val originIcon = primary.originIcon ?: secondary.originIcon
    val createdAt = primary.createdAt ?: secondary.createdAt
    val rating = primary.rating ?: secondary.rating
    val attached = (primary.attachedMedia + secondary.attachedMedia).distinct()
    val isOnCommsUni = primary.isOnCommsUni || secondary.isOnCommsUni

    return primary.copy(
        userAvatarUrl = avatar,
        userDisplayName = name,
        likesCount = likes,
        likedBy = likedBy,
        originSlug = if (originSlug.isBlank()) "flicktrove" else originSlug,
        originName = if (originName.isBlank()) "FlickTrove" else originName,
        originColor = originColor ?: "#2dd4bf",
        originIcon = originIcon,
        createdAt = createdAt,
        rating = rating,
        attachedMedia = attached,
        isOnCommsUni = isOnCommsUni
    )
}

fun List<AppComment>.deduplicateComments(): List<AppComment> {
    val result = mutableListOf<AppComment>()
    for (comment in this) {
        val existingIndex = result.indexOfFirst { isDuplicateComment(it, comment) }
        if (existingIndex == -1) {
            result.add(comment)
        } else {
            result[existingIndex] = mergeComments(result[existingIndex], comment)
        }
    }
    return result
}

