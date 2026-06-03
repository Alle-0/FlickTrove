package com.cinetrack.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.cinetrack.data.models.DiscoveryFilters
import com.cinetrack.data.models.SortConfig
import com.cinetrack.data.models.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object PreferencesKeys {
        val HOME_SORT = stringPreferencesKey("home_sort")
        val VISTI_SORT = stringPreferencesKey("visti_sort")
        val DISCOVERY_FILTERS = stringPreferencesKey("discovery_filters")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val SHOW_LAYOUT_TOGGLE = booleanPreferencesKey("show_layout_toggle")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SHOW_FOLDER_BOOKMARKS = booleanPreferencesKey("show_folder_bookmarks")
        val SHOW_BADGES = booleanPreferencesKey("show_badges")
        val DISABLED_BADGES = stringSetPreferencesKey("disabled_badges")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val ADVANCED_VISUAL_EFFECTS_ENABLED = booleanPreferencesKey("advanced_visual_effects_enabled")
        val DYNAMIC_APP_ICON_ENABLED = booleanPreferencesKey("dynamic_app_icon_enabled")
        val APP_THEME = stringPreferencesKey("app_theme")
        val CONTENT_LANGUAGE = stringPreferencesKey("content_language")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val homeSortJson = preferences[PreferencesKeys.HOME_SORT]
            val vistiSortJson = preferences[PreferencesKeys.VISTI_SORT]
            val discoveryFiltersJson = preferences[PreferencesKeys.DISCOVERY_FILTERS]

            UserPreferences(
                homeSort = homeSortJson?.let { json.decodeFromString<SortConfig>(it) } ?: SortConfig(),
                vistiSort = vistiSortJson?.let { json.decodeFromString<SortConfig>(it) } ?: SortConfig(sortType = "watched_at"),
                discoveryFilters = discoveryFiltersJson?.let { json.decodeFromString<DiscoveryFilters>(it) } ?: DiscoveryFilters(),
                gridColumns = preferences[PreferencesKeys.GRID_COLUMNS] ?: 3,
                showLayoutToggle = preferences[PreferencesKeys.SHOW_LAYOUT_TOGGLE] ?: false,
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                showFolderBookmarks = preferences[PreferencesKeys.SHOW_FOLDER_BOOKMARKS] ?: true,
                showBadges = preferences[PreferencesKeys.SHOW_BADGES] ?: true,
                disabledBadges = preferences[PreferencesKeys.DISABLED_BADGES] ?: emptySet(),
                vibrationEnabled = preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true,
                accentColor = preferences[PreferencesKeys.ACCENT_COLOR] ?: "Teal",
                appTheme = preferences[PreferencesKeys.APP_THEME] ?: "System",
                contentLanguage = preferences[PreferencesKeys.CONTENT_LANGUAGE] ?: "it-IT",
                advancedVisualEffectsEnabled = preferences[PreferencesKeys.ADVANCED_VISUAL_EFFECTS_ENABLED] ?: true,
                dynamicAppIconEnabled = preferences[PreferencesKeys.DYNAMIC_APP_ICON_ENABLED] ?: false,
                lastSyncTimestamp = preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L
            )
        }

    suspend fun updateHomeSort(config: SortConfig) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HOME_SORT] = json.encodeToString(config)
        }
    }

    suspend fun updateVistiSort(config: SortConfig) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VISTI_SORT] = json.encodeToString(config)
        }
    }

    suspend fun updateDiscoveryFilters(filters: DiscoveryFilters) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISCOVERY_FILTERS] = json.encodeToString(filters)
        }
    }

    suspend fun updateGridColumns(columns: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRID_COLUMNS] = columns
        }
    }

    suspend fun updateShowLayoutToggle(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_LAYOUT_TOGGLE] = show
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateShowFolderBookmarks(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_FOLDER_BOOKMARKS] = show
        }
    }

    suspend fun updateVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun updateAppTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME] = theme
        }
    }

    suspend fun updateContentLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTENT_LANGUAGE] = language
        }
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun updateAll(prefs: UserPreferences) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HOME_SORT] = json.encodeToString(prefs.homeSort)
            preferences[PreferencesKeys.VISTI_SORT] = json.encodeToString(prefs.vistiSort)
            preferences[PreferencesKeys.DISCOVERY_FILTERS] = json.encodeToString(prefs.discoveryFilters)
            preferences[PreferencesKeys.GRID_COLUMNS] = prefs.gridColumns
            preferences[PreferencesKeys.SHOW_LAYOUT_TOGGLE] = prefs.showLayoutToggle
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = prefs.notificationsEnabled
            preferences[PreferencesKeys.SHOW_FOLDER_BOOKMARKS] = prefs.showFolderBookmarks
            preferences[PreferencesKeys.SHOW_BADGES] = prefs.showBadges
            preferences[PreferencesKeys.DISABLED_BADGES] = prefs.disabledBadges
            preferences[PreferencesKeys.VIBRATION_ENABLED] = prefs.vibrationEnabled
            preferences[PreferencesKeys.ACCENT_COLOR] = prefs.accentColor
            preferences[PreferencesKeys.APP_THEME] = prefs.appTheme
            preferences[PreferencesKeys.CONTENT_LANGUAGE] = prefs.contentLanguage
            preferences[PreferencesKeys.ADVANCED_VISUAL_EFFECTS_ENABLED] = prefs.advancedVisualEffectsEnabled
            preferences[PreferencesKeys.DYNAMIC_APP_ICON_ENABLED] = prefs.dynamicAppIconEnabled
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = prefs.lastSyncTimestamp
        }
    }

    suspend fun updateDisabledBadges(badges: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISABLED_BADGES] = badges
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
