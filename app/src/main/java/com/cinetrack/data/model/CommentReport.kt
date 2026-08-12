package com.cinetrack.data.model

import com.google.firebase.Timestamp

data class CommentReport(
    val id: String = "",
    val mediaId: String = "",
    val commentId: String = "",
    val reportedByUid: String = "",
    val reason: String = "",
    val commentText: String = "",
    val commentAuthorId: String = "",
    val commentAuthorName: String = "",
    val timestamp: Timestamp? = null,
    val status: String = "pending"
)
