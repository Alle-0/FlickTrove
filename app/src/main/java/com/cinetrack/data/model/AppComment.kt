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
    val repliesCount: Int = 0,
    val depth: Int = 0,
    val isDeleted: Boolean = false,
    @get:PropertyName("isSpoiler")
    @set:PropertyName("isSpoiler")
    var isSpoiler: Boolean = false
)

enum class CommentSortOption { DATE, LIKES }
enum class CommentSortOrder { ASC, DESC }
