package com.cinetrack.data.model

import com.google.firebase.Timestamp

data class SocialNotification(
    val id: String = "",
    val type: String = "", // "reply" or "like"
    val mediaId: String = "",
    val mediaType: String = "",
    val mediaTitle: String = "",
    val mediaImage: String? = null,
    val commentId: String = "",
    val senderName: String = "",
    val senderUserId: String = "",
    val createdAt: Timestamp? = null,
    @get:com.google.firebase.firestore.PropertyName("isRead")
    @set:com.google.firebase.firestore.PropertyName("isRead")
    var isRead: Boolean = false
)
