package com.cinetrack.data.api

import kotlinx.serialization.Serializable

@Serializable
data class CommsUniConversationStatsResponse(
    val data: CommsUniConversationStats
)

@Serializable
data class CommsUniConversationStats(
    val entityType: String = "",
    val entityId: String = "",
    val comments: CommsUniSplitCountStats = CommsUniSplitCountStats(),
    val replies: CommsUniSplitCountStats = CommsUniSplitCountStats(),
    val commentsComplete: Boolean = true,
    val repliesComplete: Boolean = true,
    val languageCounts: List<CommsUniLanguageCount> = emptyList(),
    val sourceCounts: List<CommsUniSourceCount> = emptyList()
)

@Serializable
data class CommsUniSplitCountStats(
    val archived: Int = 0,
    val native: Int = 0,
    val total: Int = 0,
    val allTime: CommsUniSplitCount? = null
)

@Serializable
data class CommsUniSplitCount(
    val archived: Int = 0,
    val native: Int = 0,
    val total: Int = 0
)

@Serializable
data class CommsUniLanguageCount(
    val language: String = "",
    val base: String? = null,
    val count: Int = 0
)

@Serializable
data class CommsUniSourceCount(
    val source: String = "",
    val count: Int = 0
)

@Serializable
data class CommsUniCommentsResponse(
    val data: CommsUniCommentsData
)

@Serializable
data class CommsUniCommentsData(
    val comments: List<CommsUniComment> = emptyList(),
    val replies: List<CommsUniComment> = emptyList(),
    val sort: String = "most_recent",
    val nextCursor: String? = null,
    val complete: Boolean = true,
    val count: Int = 0,
    val total: Int = 0,
    val allTimeTotal: Int? = null,
    val languageCounts: List<CommsUniLanguageCount> = emptyList(),
    val sourceCounts: List<CommsUniSourceCount> = emptyList()
) {
    val allItems: List<CommsUniComment>
        get() = if (replies.isNotEmpty()) replies else comments
}

@Serializable
data class CommsUniSingleCommentResponse(
    val data: CommsUniSingleCommentData
)

@Serializable
data class CommsUniSingleCommentData(
    val comment: CommsUniComment
)

@Serializable
data class CommsUniComment(
    val id: String = "",
    val entityId: String = "",
    val source: String = "",
    val origin: CommsUniOrigin = CommsUniOrigin(),
    val text: String? = null,
    val createdAt: String = "",
    val likeCount: Int = 0,
    val likes: CommsUniSplitCount = CommsUniSplitCount(0, 0, 0),
    val isSpoiler: Boolean = false,
    val spoilerCount: Int = 0,
    val spoilers: CommsUniSplitCount = CommsUniSplitCount(0, 0, 0),
    val viewerLiked: Boolean? = null,
    val viewerMarkedSpoiler: Boolean? = null,
    val replyCount: Int = 0,
    val replyCountAllTime: Int = 0,
    val parentCommentId: String? = null,
    val inReplyToCommentId: String? = null,
    val rootCommentId: String? = null,
    val depth: Int = 0,
    val userId: String? = null,
    val userName: String? = null,
    val userColor: String? = null,
    val userAvatar: String? = null,
    val language: String? = null,
    val rating: Double? = null,
    val mentions: List<CommsUniMention> = emptyList(),
    val hasImage: Boolean = false,
    val media: CommsUniMedia? = null,
    val attachments: List<CommsUniMedia> = emptyList(),
    val imageUrl: String? = null,
    val replies: List<CommsUniComment> = emptyList(),
    val fetchedReplyCount: Int = 0,
    val repliesComplete: Boolean = true,
    val replyStatus: String? = null,
    val deleted: Boolean? = null,
    val deletedAt: String? = null
)

@Serializable
data class CommsUniOrigin(
    val kind: String = "",
    val slug: String = "",
    val displayName: String = ""
)

@Serializable
data class CommsUniMention(
    val ordinal: Int = 0,
    val userId: String? = null,
    val userName: String? = null,
    val userColor: String? = null,
    val start: Int? = null,
    val end: Int? = null
)

@Serializable
data class CommsUniMedia(
    val kind: String = "",
    val url: String? = null,
    val apiUrl: String? = null,
    val contentType: String? = null,
    val provider: String? = null,
    val bytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String? = null
)

@Serializable
data class CommsUniEntityPayload(
    val title: String? = null,
    val showTitle: String? = null
)

@Serializable
data class CommsUniWriteCommentRequest(
    val text: String,
    val language: String? = null,
    val isSpoiler: Boolean = false,
    val rating: Double? = null,
    val entity: CommsUniEntityPayload? = null,
    val createdAt: String? = null
)

enum class CommsUniReportReason(val value: String) {
    SPOILER("spoiler"),
    ABUSE("abuse"),
    SPAM("spam"),
    SEXUAL("sexual"),
    ILLEGAL("illegal"),
    OTHER("other"),
    MINE_HIDE("mine_hide"),
    MINE_CLAIM("mine_claim");

    companion object {
        fun fromValue(value: String): CommsUniReportReason =
            values().find { it.value.equals(value, ignoreCase = true) }
                ?: when (value.uppercase()) {
                    "INAPPROPRIATE_CONTENT" -> ABUSE
                    "INAPPROPRIATE_USER" -> ABUSE
                    else -> OTHER
                }
    }
}

@Serializable
data class CommsUniReportRequest(
    val reason: String,
    val detail: String? = null
)

@Serializable
data class CommsUniUpdateProfileRequest(
    val displayName: String,
    val avatarUrl: String? = null
)

@Serializable
data class CommsUniProfileResponse(
    val data: CommsUniProfileData
)

@Serializable
data class CommsUniProfileData(
    val authorId: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

