package com.cinetrack.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.cinetrack.data.model.DiscoveryFilters
import com.cinetrack.data.model.SortConfig
import com.cinetrack.data.model.UserPreferences
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
        val FOLDERS_SORT = stringPreferencesKey("folders_sort")
        val DISCOVERY_FILTERS = stringPreferencesKey("discovery_filters")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val SHOW_LAYOUT_TOGGLE = booleanPreferencesKey("show_layout_toggle")
        val IS_SEARCH_SUGGESTIONS_EXPANDED = booleanPreferencesKey("is_search_suggestions_expanded")
        val NOTIFICATIONS_RELEASES = booleanPreferencesKey("notifications_releases")
        val NOTIFICATIONS_SOCIAL = booleanPreferencesKey("notifications_social")
        val SHOW_FOLDER_BOOKMARKS = booleanPreferencesKey("show_folder_bookmarks")
        val SHOW_BADGES = booleanPreferencesKey("show_badges")
        val DISABLED_BADGES = stringSetPreferencesKey("disabled_badges")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val ADVANCED_VISUAL_EFFECTS_ENABLED = booleanPreferencesKey("advanced_visual_effects_enabled")
        val DYNAMIC_APP_ICON_ENABLED = booleanPreferencesKey("dynamic_app_icon_enabled")
        val APP_THEME = stringPreferencesKey("app_theme")
        val CONTENT_LANGUAGE = stringPreferencesKey("content_language")
        val SHOW_SPLIT_RELEASES_HOME = booleanPreferencesKey("show_split_releases_home")
        val SHOW_SPLIT_DROPPED_HOME = booleanPreferencesKey("show_split_dropped_home")
        val SHOW_APP_ENTRY_ANIMATION = booleanPreferencesKey("show_app_entry_animation")
        val USE_MOVIE_LOGO = booleanPreferencesKey("use_movie_logo")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val DEFAULT_START_TAB = stringPreferencesKey("default_start_tab")
        val TVDB_JWT_TOKEN = stringPreferencesKey("tvdb_jwt_token")
        val SHOW_MY_FOLDERS = booleanPreferencesKey("show_my_folders")
        val SHOW_YOUR_FLOW = booleanPreferencesKey("show_your_flow")
        val TITLE_TEXT_SIZE_MULTIPLIER = floatPreferencesKey("title_text_size_multiplier")
        val IMAGE_QUALITY = stringPreferencesKey("image_quality")
        val SHOW_GENERAL_STATS = booleanPreferencesKey("show_general_stats")
        val DASHBOARD_CARD_ORDER = stringPreferencesKey("dashboard_card_order")
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
            val foldersSortJson = preferences[PreferencesKeys.FOLDERS_SORT]
            val discoveryFiltersJson = preferences[PreferencesKeys.DISCOVERY_FILTERS]

            UserPreferences(
                homeSort = homeSortJson?.let { json.decodeFromString<SortConfig>(it) } ?: SortConfig(),
                vistiSort = vistiSortJson?.let { json.decodeFromString<SortConfig>(it) } ?: SortConfig(sortType = "watched_at"),
                foldersSort = foldersSortJson?.let { json.decodeFromString<SortConfig>(it) } ?: SortConfig(sortType = "date", sortDirection = "desc"),
                discoveryFilters = discoveryFiltersJson?.let { json.decodeFromString<DiscoveryFilters>(it) } ?: DiscoveryFilters(),
                gridColumns = preferences[PreferencesKeys.GRID_COLUMNS] ?: 3,
                showLayoutToggle = preferences[PreferencesKeys.SHOW_LAYOUT_TOGGLE] ?: false,
                isSearchSuggestionsExpanded = preferences[PreferencesKeys.IS_SEARCH_SUGGESTIONS_EXPANDED] ?: true,
                notificationsReleases = preferences[PreferencesKeys.NOTIFICATIONS_RELEASES] ?: true,
                notificationsSocial = preferences[PreferencesKeys.NOTIFICATIONS_SOCIAL] ?: true,
                showFolderBookmarks = preferences[PreferencesKeys.SHOW_FOLDER_BOOKMARKS] ?: true,
                showBadges = preferences[PreferencesKeys.SHOW_BADGES] ?: true,
                disabledBadges = preferences[PreferencesKeys.DISABLED_BADGES] ?: emptySet(),
                vibrationEnabled = preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true,
                accentColor = preferences[PreferencesKeys.ACCENT_COLOR] ?: "Teal",
                appTheme = preferences[PreferencesKeys.APP_THEME] ?: "System",
                contentLanguage = preferences[PreferencesKeys.CONTENT_LANGUAGE] ?: "system",
                advancedVisualEffectsEnabled = preferences[PreferencesKeys.ADVANCED_VISUAL_EFFECTS_ENABLED] ?: true,
                dynamicAppIconEnabled = preferences[PreferencesKeys.DYNAMIC_APP_ICON_ENABLED] ?: false,
                showSplitReleasesHome = preferences[PreferencesKeys.SHOW_SPLIT_RELEASES_HOME] ?: true,
                showSplitDroppedHome = preferences[PreferencesKeys.SHOW_SPLIT_DROPPED_HOME] ?: false,
                showAppEntryAnimation = preferences[PreferencesKeys.SHOW_APP_ENTRY_ANIMATION] ?: true,
                useMovieLogo = preferences[PreferencesKeys.USE_MOVIE_LOGO] ?: true,
                lastSyncTimestamp = preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L,
                defaultStartTab = preferences[PreferencesKeys.DEFAULT_START_TAB] ?: "feed",
                tvdbJwtToken = preferences[PreferencesKeys.TVDB_JWT_TOKEN] ?: "",
                showMyFolders = preferences[PreferencesKeys.SHOW_MY_FOLDERS] ?: true,
                showYourFlow = preferences[PreferencesKeys.SHOW_YOUR_FLOW] ?: true,
                titleTextSizeMultiplier = preferences[PreferencesKeys.TITLE_TEXT_SIZE_MULTIPLIER] ?: 1.0f,
                imageQuality = preferences[PreferencesKeys.IMAGE_QUALITY] ?: "MEDIUM",
                showGeneralStats = preferences[PreferencesKeys.SHOW_GENERAL_STATS] ?: true,
                dashboardCardOrder = (preferences[PreferencesKeys.DASHBOARD_CARD_ORDER] ?: "stats,folders,flow").split(",")
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

    suspend fun updateFoldersSort(config: SortConfig) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOLDERS_SORT] = json.encodeToString(config)
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

    suspend fun updateIsSearchSuggestionsExpanded(expanded: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SEARCH_SUGGESTIONS_EXPANDED] = expanded
        }
    }

    suspend fun toggleNotificationsReleases(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_RELEASES] = enabled
        }
    }

    suspend fun toggleNotificationsSocial(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_SOCIAL] = enabled
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

    suspend fun updateShowSplitReleasesHome(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_SPLIT_RELEASES_HOME] = show
        }
    }

    suspend fun updateShowSplitDroppedHome(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_SPLIT_DROPPED_HOME] = show
        }
    }

    suspend fun updateShowAppEntryAnimation(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_APP_ENTRY_ANIMATION] = show
        }
    }

    suspend fun updateUseMovieLogo(use: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_MOVIE_LOGO] = use
        }
    }

    suspend fun updateDefaultStartTab(tab: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_START_TAB] = tab
        }
    }

    suspend fun updateAll(prefs: UserPreferences) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HOME_SORT] = json.encodeToString(prefs.homeSort)
            preferences[PreferencesKeys.VISTI_SORT] = json.encodeToString(prefs.vistiSort)
            preferences[PreferencesKeys.FOLDERS_SORT] = json.encodeToString(prefs.foldersSort)
            preferences[PreferencesKeys.DISCOVERY_FILTERS] = json.encodeToString(prefs.discoveryFilters)
            preferences[PreferencesKeys.GRID_COLUMNS] = prefs.gridColumns
            preferences[PreferencesKeys.SHOW_LAYOUT_TOGGLE] = prefs.showLayoutToggle
            preferences[PreferencesKeys.IS_SEARCH_SUGGESTIONS_EXPANDED] = prefs.isSearchSuggestionsExpanded
            preferences[PreferencesKeys.NOTIFICATIONS_RELEASES] = prefs.notificationsReleases
            preferences[PreferencesKeys.NOTIFICATIONS_SOCIAL] = prefs.notificationsSocial
            preferences[PreferencesKeys.SHOW_FOLDER_BOOKMARKS] = prefs.showFolderBookmarks
            preferences[PreferencesKeys.SHOW_BADGES] = prefs.showBadges
            preferences[PreferencesKeys.DISABLED_BADGES] = prefs.disabledBadges
            preferences[PreferencesKeys.VIBRATION_ENABLED] = prefs.vibrationEnabled
            preferences[PreferencesKeys.ACCENT_COLOR] = prefs.accentColor
            preferences[PreferencesKeys.APP_THEME] = prefs.appTheme
            preferences[PreferencesKeys.CONTENT_LANGUAGE] = prefs.contentLanguage
            preferences[PreferencesKeys.ADVANCED_VISUAL_EFFECTS_ENABLED] = prefs.advancedVisualEffectsEnabled
            preferences[PreferencesKeys.DYNAMIC_APP_ICON_ENABLED] = prefs.dynamicAppIconEnabled
            preferences[PreferencesKeys.SHOW_SPLIT_RELEASES_HOME] = prefs.showSplitReleasesHome
            preferences[PreferencesKeys.SHOW_SPLIT_DROPPED_HOME] = prefs.showSplitDroppedHome
            preferences[PreferencesKeys.SHOW_APP_ENTRY_ANIMATION] = prefs.showAppEntryAnimation
            preferences[PreferencesKeys.USE_MOVIE_LOGO] = prefs.useMovieLogo
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = prefs.lastSyncTimestamp
            preferences[PreferencesKeys.DEFAULT_START_TAB] = prefs.defaultStartTab
            preferences[PreferencesKeys.TVDB_JWT_TOKEN] = prefs.tvdbJwtToken
            preferences[PreferencesKeys.SHOW_MY_FOLDERS] = prefs.showMyFolders
            preferences[PreferencesKeys.SHOW_YOUR_FLOW] = prefs.showYourFlow
            preferences[PreferencesKeys.TITLE_TEXT_SIZE_MULTIPLIER] = prefs.titleTextSizeMultiplier
            preferences[PreferencesKeys.IMAGE_QUALITY] = prefs.imageQuality
        }
    }

    suspend fun updateTitleTextSizeMultiplier(multiplier: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TITLE_TEXT_SIZE_MULTIPLIER] = multiplier
        }
    }

    suspend fun updateImageQuality(quality: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IMAGE_QUALITY] = quality
        }
    }

    suspend fun updateShowGeneralStats(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_GENERAL_STATS] = show
        }
    }

    suspend fun updateDashboardCardOrder(order: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DASHBOARD_CARD_ORDER] = order.joinToString(",")
        }
    }

    suspend fun updateTvdbJwtToken(token: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TVDB_JWT_TOKEN] = token
        }
    }

    suspend fun updateShowMyFolders(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_MY_FOLDERS] = show
        }
    }

    suspend fun updateShowYourFlow(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_YOUR_FLOW] = show
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
