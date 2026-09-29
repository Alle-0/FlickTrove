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
        comment.media?.let { allMediaUrls.add(it.url) }
        comment.attachments.forEach { allMediaUrls.add(it.url) }

        val sourceRow = sourcesMap[comment.origin.slug]

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
            originSlug = comment.origin.slug,
            originName = comment.origin.displayName,
            originColor = sourceRow?.accentColor,
            originIcon = sourceRow?.iconUrl,
            archivedLikes = comment.likes.archived,
            nativeLikes = comment.likes.native,
            attachedMedia = allMediaUrls
        )
    }
}
