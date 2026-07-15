package com.cinetrack.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Feedback(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val title: String = "",
    val description: String = "",
    val rating: Int = 0,
    val timestamp: Timestamp = Timestamp.now(),
    val appVersion: String = "",
    val deviceModel: String = "",
    val androidVersion: String = ""
)
