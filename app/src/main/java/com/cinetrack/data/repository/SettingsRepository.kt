package com.cinetrack.data.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val accentColor: Flow<String>
    val showFolderBookmarks: Flow<Boolean>
    val notificationsEnabled: Flow<Boolean>
    val vibrationEnabled: Flow<Boolean>
    val lastFeedbackTimestamp: Flow<Long>

    suspend fun updateAccentColor(color: String)
    suspend fun toggleFolderBookmarks(enabled: Boolean)
    suspend fun toggleNotifications(enabled: Boolean)
    suspend fun toggleVibration(enabled: Boolean)
    suspend fun updateLastFeedbackTimestamp(timestamp: Long)
}
