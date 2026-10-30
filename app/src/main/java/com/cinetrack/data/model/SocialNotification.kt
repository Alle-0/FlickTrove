package com.cinetrack.data.model

import com.google.firebase.Timestamp

data class SocialNotification(
    val id: String = "",
    val type: String = "", // "reply", "like", or "mention"
    val mediaId: String = "",
    val mediaType: String = "",
    val mediaTitle: String = "",
    val mediaImage: String? = null,
    val commentId: String = "",
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val senderName: String = "",
    val senderUserId: String = "",
    val snippet: String? = null,
    val createdAt: Timestamp? = null,
    @get:com.google.firebase.firestore.PropertyName("isRead")
    @set:com.google.firebase.firestore.PropertyName("isRead")
    var isRead: Boolean = false
)

data class GroupedSocialNotification(
    val id: String = "",
    val type: String = "", // "reply", "like", or "mention"
    val mediaId: String = "",
    val mediaType: String = "",
    val mediaTitle: String = "",
    val mediaImage: String? = null,
    val commentId: String = "",
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val senders: List<String> = emptyList(),
    val count: Int = 1,
    val latestTimestamp: Timestamp? = null,
    val snippet: String? = null,
    val isRead: Boolean = false,
    val originalNotificationIds: List<String> = emptyList()
)
