package com.cinetrack.data.mapper

import com.cinetrack.data.api.CommsUniComment
import com.cinetrack.data.api.SourceCatalogRow
import com.cinetrack.data.model.AppComment
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object CommsUniMapper {

    fun toAppComment(
        comment: CommsUniComment,
        sourcesMap: Map<String, SourceCatalogRow> = emptyMap(),
        currentEntityId: String = "",
        currentEntityType: String = "",
        currentUserId: String? = null,
        myAuthorId: String? = null,
        myActorId: String? = null,
        rawUid: String? = null,
        myCommentIds: Set<String> = emptySet(),
        cachedLocalAvatar: String? = null,
        cachedLocalName: String? = null
    ): AppComment {
        val date = try {
            java.util.Date.from(java.time.Instant.parse(comment.createdAt))
        } catch (e: Exception) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                format.parse(comment.createdAt)
            } catch (e2: Exception) {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    format.timeZone = TimeZone.getTimeZone("UTC")
                    format.parse(comment.createdAt)
                } catch (e3: Exception) {
                    null
                }
            }
        }

        val allMediaUrls = mutableListOf<String>()
        val primaryMediaUrl = comment.media?.url ?: comment.media?.apiUrl ?: comment.imageUrl
        primaryMediaUrl?.takeIf { it.isNotBlank() }?.let { allMediaUrls.add(it) }
        comment.attachments.forEach { att ->
            val attUrl = att.url ?: att.apiUrl
            attUrl?.takeIf { it.isNotBlank() && !allMediaUrls.contains(it) }?.let { allMediaUrls.add(it) }
        }

        val resolvedSlug = comment.origin.slug.ifBlank { comment.source }
        val sourceRow = sourcesMap[resolvedSlug] ?: sourcesMap[comment.origin.slug] ?: sourcesMap[comment.source]
        val resolvedName = comment.origin.displayName.ifBlank {
            sourceRow?.displayName ?: if (resolvedSlug.contains("tvtime", ignoreCase = true)) "TV Time Refugees" else resolvedSlug
        }

        val isMyComment = (comment.id in myCommentIds) ||
            (!comment.userId.isNullOrBlank() && (
                (myAuthorId != null && comment.userId == myAuthorId) ||
                (myActorId != null && comment.userId == myActorId) ||
                (rawUid != null && comment.userId == rawUid) ||
                (currentUserId != null && comment.userId == currentUserId)
            ))

        val resolvedAvatar = if (isMyComment && !cachedLocalAvatar.isNullOrBlank()) {
            cachedLocalAvatar.trim()
        } else if (!comment.userAvatar.isNullOrBlank()) {
            comment.userAvatar.trim()
        } else {
            ""
        }

        val resolvedUserId = if (isMyComment) (currentUserId ?: comment.userId ?: "") else (comment.userId ?: "")

        return AppComment(
            id = comment.id,
            mediaId = currentEntityId,
            mediaType = currentEntityType,
            userId = resolvedUserId,
            userDisplayName = if (isMyComment && comment.userName.isNullOrBlank() && !cachedLocalName.isNullOrBlank()) cachedLocalName else (comment.userName ?: ""),
            userAvatarUrl = resolvedAvatar,
            text = comment.text ?: "",
            createdAt = date?.let { Timestamp(it) },
            likesCount = comment.likeCount,
            likedBy = if (comment.viewerLiked == true && currentUserId != null) listOf(currentUserId) else emptyList(),
            parentId = comment.parentCommentId ?: comment.inReplyToCommentId,
            parentUserId = comment.inReplyToCommentId,
            rootCommentId = comment.rootCommentId ?: (if (comment.parentCommentId == null) comment.id else null),
            repliesCount = comment.replyCount,
            depth = comment.depth,
            isDeleted = comment.deleted ?: false,
            isSpoiler = comment.isSpoiler,
            originSlug = resolvedSlug,
            originName = resolvedName,
            originColor = sourceRow?.accentColor,
            originIcon = sourceRow?.iconUrl,
            archivedLikes = comment.likes.archived,
            nativeLikes = comment.likes.native,
            attachedMedia = allMediaUrls,
            rating = comment.rating,
            language = comment.language
        )
    }
}
