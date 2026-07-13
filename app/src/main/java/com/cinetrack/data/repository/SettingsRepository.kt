package com.cinetrack.data.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val accentColor: Flow<String>
    val showFolderBookmarks: Flow<Boolean>
    val showBadges: Flow<Boolean>
    val disabledBadges: Flow<Set<String>>
    val notificationsEnabled: Flow<Boolean>
    val vibrationEnabled: Flow<Boolean>
    val dynamicAppIconEnabled: Flow<Boolean>
    val advancedVisualEffectsEnabled: Flow<Boolean>
    val lastFeedbackTimestamp: Flow<Long>
    val titleTextSizeMultiplier: Flow<Float>
    val imageQuality: Flow<String>
    val lastSeenAppVersion: Flow<String>
    val ignoredUpdateVersion: Flow<String>
    val hasSeenOnboarding: Flow<Boolean>

    suspend fun updateAccentColor(color: String)
    suspend fun toggleFolderBookmarks(enabled: Boolean)
    suspend fun toggleBadges(enabled: Boolean)
    suspend fun toggleNotifications(enabled: Boolean)
    suspend fun toggleVibration(enabled: Boolean)
    suspend fun toggleDynamicAppIcon(enabled: Boolean)
    suspend fun updateDisabledBadges(badges: Set<String>)
    suspend fun toggleAdvancedVisualEffects(enabled: Boolean)
    suspend fun updateLastFeedbackTimestamp(timestamp: Long)
    suspend fun updateTitleTextSizeMultiplier(multiplier: Float)
    suspend fun updateImageQuality(quality: String)
    suspend fun updateLastSeenAppVersion(version: String)
    suspend fun updateIgnoredUpdateVersion(version: String)
    suspend fun setOnboardingSeen(seen: Boolean)
}
