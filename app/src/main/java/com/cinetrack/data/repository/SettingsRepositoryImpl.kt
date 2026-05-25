package com.cinetrack.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val ADVANCED_VISUAL_EFFECTS_ENABLED = booleanPreferencesKey("advanced_visual_effects_enabled")
        val LAST_FEEDBACK_TIMESTAMP = longPreferencesKey("last_feedback_timestamp")
    }

    override val accentColor: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACCENT_COLOR] ?: "Teal" // Default value
    }

    override val showFolderBookmarks: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_FOLDER_BOOKMARKS] ?: true // Default value
    }

    override val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true // Default value
    }

    override val vibrationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true // Default value
    }

    override val advancedVisualEffectsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ADVANCED_VISUAL_EFFECTS_ENABLED] ?: true // Default value
    }

    override val lastFeedbackTimestamp: Flow<Long> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_FEEDBACK_TIMESTAMP] ?: 0L
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
}
