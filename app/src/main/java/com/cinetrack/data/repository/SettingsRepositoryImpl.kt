package com.cinetrack.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val SHOW_FOLDER_BOOKMARKS = booleanPreferencesKey("show_folder_bookmarks")
        val SHOW_BADGES = booleanPreferencesKey("show_badges")
        val DISABLED_BADGES = stringSetPreferencesKey("disabled_badges")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val DYNAMIC_APP_ICON_ENABLED = booleanPreferencesKey("dynamic_app_icon_enabled")
        val ADVANCED_VISUAL_EFFECTS_ENABLED = booleanPreferencesKey("advanced_visual_effects_enabled")
        val LAST_FEEDBACK_TIMESTAMP = longPreferencesKey("last_feedback_timestamp")
        val TITLE_TEXT_SIZE_MULTIPLIER = floatPreferencesKey("title_text_size_multiplier")
        val IMAGE_QUALITY = stringPreferencesKey("image_quality")
        val LAST_SEEN_APP_VERSION = stringPreferencesKey("last_seen_app_version")
        val IGNORED_UPDATE_VERSION = stringPreferencesKey("ignored_update_version")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    }

    override val hasSeenOnboarding: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_SEEN_ONBOARDING] ?: false
    }

    override val lastSeenAppVersion: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_SEEN_APP_VERSION] ?: ""
    }

    override val ignoredUpdateVersion: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IGNORED_UPDATE_VERSION] ?: ""
    }

    override val accentColor: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACCENT_COLOR] ?: "Teal" // Default value
    }

    override val showFolderBookmarks: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_FOLDER_BOOKMARKS] ?: true // Default value
    }

    override val showBadges: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_BADGES] ?: true // Default value
    }

    override val disabledBadges: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DISABLED_BADGES] ?: emptySet()
    }

    override val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true // Default value
    }

    override val vibrationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true // Default value
    }

    override val dynamicAppIconEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DYNAMIC_APP_ICON_ENABLED] ?: false // Default value is false to prevent unexpected restarts
    }

    override val advancedVisualEffectsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ADVANCED_VISUAL_EFFECTS_ENABLED] ?: true // Default value
    }

    override val lastFeedbackTimestamp: Flow<Long> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_FEEDBACK_TIMESTAMP] ?: 0L
    }

    override val titleTextSizeMultiplier: Flow<Float> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TITLE_TEXT_SIZE_MULTIPLIER] ?: 1.0f
    }

    override val imageQuality: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IMAGE_QUALITY] ?: "MEDIUM"
    }

    override suspend fun updateAccentColor(color: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] = color
        }
    }

    override suspend fun toggleFolderBookmarks(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_FOLDER_BOOKMARKS] = enabled
        }
    }

    override suspend fun toggleBadges(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_BADGES] = enabled
        }
    }

    override suspend fun updateDisabledBadges(badges: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISABLED_BADGES] = badges
        }
    }

    override suspend fun toggleNotifications(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override suspend fun toggleVibration(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATION_ENABLED] = enabled
        }
    }

    override suspend fun toggleDynamicAppIcon(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_APP_ICON_ENABLED] = enabled
        }
    }

    override suspend fun toggleAdvancedVisualEffects(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ADVANCED_VISUAL_EFFECTS_ENABLED] = enabled
        }
    }

    override suspend fun updateLastFeedbackTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_FEEDBACK_TIMESTAMP] = timestamp
        }
    }

    override suspend fun updateTitleTextSizeMultiplier(multiplier: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TITLE_TEXT_SIZE_MULTIPLIER] = multiplier
        }
    }

    override suspend fun updateImageQuality(quality: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IMAGE_QUALITY] = quality
        }
    }

    override suspend fun updateLastSeenAppVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SEEN_APP_VERSION] = version
        }
    }

    override suspend fun updateIgnoredUpdateVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IGNORED_UPDATE_VERSION] = version
        }
    }

    override suspend fun setOnboardingSeen(seen: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_ONBOARDING] = seen
        }
    }
}
