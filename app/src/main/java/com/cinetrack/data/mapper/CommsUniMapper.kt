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
        val date = com.cinetrack.util.DateTimeUtils.parseIsoDate(comment.createdAt)

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

        val currentAuthUid = currentUserId ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val isMyComment = (comment.id in myCommentIds) ||
            (!comment.userId.isNullOrBlank() && (
                (myAuthorId != null && comment.userId == myAuthorId) ||
                (myActorId != null && comment.userId == myActorId) ||
                (rawUid != null && comment.userId == rawUid) ||
                (currentAuthUid != null && comment.userId == currentAuthUid)
            ))

        val effectiveLocalName = cachedLocalName?.trim()?.takeIf { it.isNotBlank() }
            ?: if (isMyComment) com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() } else null

        val effectiveLocalAvatar = cachedLocalAvatar?.trim()?.takeIf { it.isNotBlank() && !it.startsWith("data:image") }
            ?: if (isMyComment) com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()?.trim()?.takeIf { it.isNotBlank() && !it.startsWith("data:image") } else null

        val resolvedAvatar = if (isMyComment) {
            effectiveLocalAvatar ?: ""
        } else if (!comment.userAvatar.isNullOrBlank()) {
            comment.userAvatar.trim()
        } else {
            ""
        }

        val resolvedUserId = if (isMyComment) (currentAuthUid ?: comment.userId ?: "") else (comment.userId ?: "")

        val resolvedDisplayName = if (isMyComment && !effectiveLocalName.isNullOrBlank()) {
            effectiveLocalName
        } else {
            comment.userName ?: ""
        }

        return AppComment(
            id = comment.id,
            mediaId = currentEntityId,
            mediaType = currentEntityType,
            userId = resolvedUserId,
            userDisplayName = resolvedDisplayName,
            userAvatarUrl = resolvedAvatar,
            text = comment.text ?: "",
            createdAt = date?.let { Timestamp(it) },
            likesCount = maxOf(comment.likeCount, comment.likes.total, comment.likes.native + comment.likes.archived),
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
            language = comment.language,
            isOnCommsUni = true
        )
    }
}
