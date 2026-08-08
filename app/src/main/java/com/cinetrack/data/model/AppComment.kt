package com.cinetrack.data.model

import com.google.firebase.Timestamp

data class AppComment(
    val id: String = "",
    val mediaId: String = "",
    val mediaType: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val userAvatarUrl: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null,
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val parentId: String? = null,
    val parentUserId: String? = null,
    val repliesCount: Int = 0,
    val depth: Int = 0,
    val isSpoiler: Boolean = false
)
