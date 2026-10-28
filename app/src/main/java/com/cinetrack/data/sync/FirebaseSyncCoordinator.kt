package com.cinetrack.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.cinetrack.R
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.model.DiscoveryFilters
import com.cinetrack.data.model.HomeFeedSectionConstants
import com.cinetrack.data.model.SortConfig
import com.cinetrack.data.model.UserPreferences
import com.cinetrack.data.remote.FirebaseRemoteDataSource
import com.cinetrack.data.repository.FolderRepository
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.data.repository.WatchHistoryRepository
import com.cinetrack.ui.utils.UiText
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates bidirectional Delta Synchronization between local Room databases
 * and Cloud Firestore (Zero-Waste Spark tier compliance).
 *
 * Handles:
 * - Local changes push (`pending` and `pending_delete` with tombstones)
 * - Remote changes pull (delta fetch based on client timestamps)
 * - Preferences sync with schema sanitization
 * - Background WorkManager scheduling
 */
@Singleton
class FirebaseSyncCoordinator @Inject constructor(
    private val firebaseRemoteDataSource: FirebaseRemoteDataSource,
    private val favoriteDao: FavoriteDao,
    private val folderRepository: FolderRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val preferenceRepository: PreferenceRepository,
    @ApplicationContext private val context: Context
) {
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastFolderSyncTimestamp: Long = 0L
    private val FOLDER_SYNC_THROTTLE_MS = 20 * 60 * 1000L // 20 minuti

    suspend fun pushPendingChanges() = withContext(Dispatchers.IO) {
        // Push pending Movies
        val pendingMovies = favoriteDao.getPendingSync()
        val toDelete = pendingMovies.filter { it.syncStatus == "pending_delete" }
        val now = System.currentTimeMillis()
        val toSync = pendingMovies.filter { it.syncStatus == "pending" }.map { 
            it.copy(syncStatus = "synced", clientUpdatedAt = now) 
        }

        for (movie in toDelete) {
            try {
                firebaseRemoteDataSource.deleteMovie(movie.id, movie.mediaType)
                // Preserva lo scudo tombstone in Room azzerando flag, rating e note
                val tombstoneMovie = movie.copy(
                    syncStatus = "deleted",
                    watched = false,
                    favorite = false,
                    reminder = false,
                    dropped = false,
                    personalRating = null,
                    personalNote = null,
                    watchedEpisodes = emptyMap(),
                    clientUpdatedAt = now
                )
                favoriteDao.insert(tombstoneMovie)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("FirebaseSyncCoordinator", "Failed to push pending delete movie ${movie.id}", e)
            }
        }

        if (toSync.isNotEmpty()) {
            try {
                firebaseRemoteDataSource.setMoviesBulk(toSync)
                for (movie in toSync) {
                    favoriteDao.insert(movie)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("FirebaseSyncCoordinator", "Failed to push pending movies bulk", e)
            }
        }
        
        // Push pending Folders
        folderRepository.pushPendingFolders()
        
        // Push pending Watch History
        watchHistoryRepository.pushPendingWatchHistory()
    }

    suspend fun syncWithFirebase(
        force: Boolean = false,
        onProgress: (suspend (SyncProgress) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        suspend fun emit(message: UiText, progress: Float?) {
            onProgress?.invoke(SyncProgress(message, progress))
        }

        // Cooldown anti-concorrenza (15 secondi per evitare doppie chiamate da ViewModel simultanei)
        val userPrefs = preferenceRepository.userPreferencesFlow.first()
        val lastSync = userPrefs.lastSyncTimestamp
        val lastHistorySync = userPrefs.lastWatchHistorySyncTimestamp
        if (!force && System.currentTimeMillis() - lastSync < 15_000L) {
            android.util.Log.d("FirebaseSyncCoordinator", "Firebase Sync skipped - last sync was less than 15 seconds ago.")
            emit(UiText.StringResource(R.string.sync_msg_cached), 1f)
            return@withContext
        }

        try {
            // 0. Push any pending local changes first (bounded by timeout so network stalls never freeze the sync)
            android.util.Log.d("FirebaseSyncCoordinator", "Starting Firebase Sync - Pushing pending changes...")
            emit(UiText.StringResource(R.string.sync_msg_pushing_changes), 0f)
            withTimeoutOrNull(15_000L) {
                pushPendingChanges()
            }

            // 1. Pull & Reconcile Favorites (Delta Sync con Tombstones)
            android.util.Log.d("FirebaseSyncCoordinator", "Starting Firebase Delta Sync (since: $lastSync)...")
            emit(UiText.StringResource(R.string.sync_msg_fetching_favorites), null)
            val deltaMovies = firebaseRemoteDataSource.fetchFavoritesDelta(lastSync)
            android.util.Log.d("FirebaseSyncCoordinator", "Fetched ${deltaMovies.size} delta favorites from Firebase")
            
            if (deltaMovies.isNotEmpty()) {
                emit(UiText.StringResource(R.string.sync_msg_syncing_favorites), 0.35f)
                val (tombstones, modified) = deltaMovies.partition { it.syncStatus == "deleted" }
                
                // 1. Preservazione dello scudo locale dei tombstone (invisibili alla UI, visibili a getMovieIncludingDeleted)
                for (tombstone in tombstones) {
                    val tombstoneMovie = tombstone.copy(
                        syncStatus = "deleted",
                        watched = false,
                        favorite = false,
                        reminder = false,
                        dropped = false,
                        personalRating = null,
                        personalNote = null,
                        watchedEpisodes = emptyMap()
                    )
                    favoriteDao.insert(tombstoneMovie)
                }
                
                // 2. Inserimento / Aggiornamento dei record modificati
                if (modified.isNotEmpty()) {
                    val chunks = modified.chunked(50)
                    chunks.forEachIndexed { index, chunk ->
                        favoriteDao.insertAll(chunk.map { it.copy(syncStatus = "synced") })
                        val portion = (index + 1).toFloat() / chunks.size.toFloat()
                        val progress = 0.35f + (0.55f - 0.35f) * portion
                        val currentCount = (index + 1) * chunk.size
                        emit(UiText.StringResource(R.string.sync_msg_saving_favorites, currentCount, modified.size), progress)
                    }
                }
            }
            android.util.Log.d("FirebaseSyncCoordinator", "Successfully synchronized favorites via Delta Sync")

            val now = System.currentTimeMillis()

            // 2. Pull & Reconcile Folders (con throttle di 20 minuti per azzerare letture server ad ogni foreground)
            val shouldSyncFolders = force || (now - lastFolderSyncTimestamp > FOLDER_SYNC_THROTTLE_MS)
            if (shouldSyncFolders) {
                emit(UiText.StringResource(R.string.sync_msg_fetching_folders), null)
                val remoteFolders = firebaseRemoteDataSource.fetchAllFolders()
                
                emit(UiText.StringResource(R.string.sync_msg_syncing_folders), 0.7f)
                folderRepository.syncFoldersFromRemote(remoteFolders) { currentCount, totalCount, portion ->
                    val progress = 0.7f + (0.9f - 0.7f) * portion
                    emit(UiText.StringResource(R.string.sync_msg_saving_folders, currentCount, totalCount), progress)
                }
                lastFolderSyncTimestamp = now
            }

            // 3. Pull & Reconcile Watch History (Delta Sync con Tombstones)
            android.util.Log.d("FirebaseSyncCoordinator", "Starting Firebase Watch History Delta Sync (since: $lastHistorySync)...")
            val remoteHistory = firebaseRemoteDataSource.fetchWatchHistoryDelta(lastHistorySync)
            android.util.Log.d("FirebaseSyncCoordinator", "Fetched ${remoteHistory.size} delta watch history from Firebase")
            watchHistoryRepository.syncWatchHistoryFromRemote(remoteHistory)
            preferenceRepository.updateLastWatchHistorySyncTimestamp(now)

            // 4. Pull Preferences
            emit(UiText.StringResource(R.string.sync_msg_syncing_preferences), 0.92f)
            syncPreferencesWithFirebase()
            preferenceRepository.updateLastSyncTimestamp(now)
            
            emit(UiText.StringResource(R.string.sync_msg_completed), 1f)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e("FirebaseSyncCoordinator", "Error during Firebase synchronization, aborting sync to prevent data loss.", e)
            emit(UiText.StringResource(R.string.sync_msg_error), 1f)
        }
    }

    fun enqueueSyncWorker() {
        try {
            val syncConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(syncConstraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                SyncWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
            android.util.Log.d("FirebaseSyncCoordinator", "SyncWorker enqueued successfully via WorkManager")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSyncCoordinator", "Failed to enqueue SyncWorker", e)
        }
    }

    suspend fun syncPreferencesWithFirebase() {
        val remotePrefs = firebaseRemoteDataSource.fetchUserPreferences() ?: return
        
        try {
            val currentPrefs = preferenceRepository.userPreferencesFlow
                .catch { emit(UserPreferences()) }
                .firstOrNull() ?: UserPreferences()

            val updatedPrefs = currentPrefs.copy(
                gridColumns = (remotePrefs["gridColumns"] as? Number)?.toInt() ?: currentPrefs.gridColumns,
                showLayoutToggle = remotePrefs["showLayoutToggle"] as? Boolean ?: currentPrefs.showLayoutToggle,
                isSearchSuggestionsExpanded = remotePrefs["isSearchSuggestionsExpanded"] as? Boolean ?: currentPrefs.isSearchSuggestionsExpanded,
                notificationsReleases = remotePrefs["notificationsReleases"] as? Boolean ?: currentPrefs.notificationsReleases,
                notificationsSocial = remotePrefs["notificationsSocial"] as? Boolean ?: currentPrefs.notificationsSocial,
                showFolderBookmarks = remotePrefs["showFolderBookmarks"] as? Boolean ?: currentPrefs.showFolderBookmarks,
                showBadges = remotePrefs["showBadges"] as? Boolean ?: currentPrefs.showBadges,
                disabledBadges = (remotePrefs["disabledBadges"] as? List<*>)?.filterIsInstance<String>()?.toSet() ?: currentPrefs.disabledBadges,
                vibrationEnabled = remotePrefs["vibrationEnabled"] as? Boolean ?: currentPrefs.vibrationEnabled,
                accentColor = remotePrefs["accentColor"] as? String ?: currentPrefs.accentColor,
                appTheme = remotePrefs["appTheme"] as? String ?: currentPrefs.appTheme,
                contentLanguage = remotePrefs["contentLanguage"] as? String ?: currentPrefs.contentLanguage,
                advancedVisualEffectsEnabled = remotePrefs["advancedVisualEffectsEnabled"] as? Boolean ?: currentPrefs.advancedVisualEffectsEnabled,
                dynamicAppIconEnabled = remotePrefs["dynamicAppIconEnabled"] as? Boolean ?: currentPrefs.dynamicAppIconEnabled,
                seasonalAppIconEnabled = remotePrefs["seasonalAppIconEnabled"] as? Boolean ?: currentPrefs.seasonalAppIconEnabled,
                showSplitReleasesHome = remotePrefs["showSplitReleasesHome"] as? Boolean ?: currentPrefs.showSplitReleasesHome,
                showSplitDroppedHome = remotePrefs["showSplitDroppedHome"] as? Boolean ?: currentPrefs.showSplitDroppedHome,
                showAppEntryAnimation = remotePrefs["showAppEntryAnimation"] as? Boolean ?: currentPrefs.showAppEntryAnimation,
                useMovieLogo = remotePrefs["useMovieLogo"] as? Boolean ?: currentPrefs.useMovieLogo,
                defaultStartTab = remotePrefs["defaultStartTab"] as? String ?: currentPrefs.defaultStartTab,
                defaultStartMedia = remotePrefs["defaultStartMedia"] as? String ?: currentPrefs.defaultStartMedia,
                showMyFolders = remotePrefs["showMyFolders"] as? Boolean ?: currentPrefs.showMyFolders,
                showYourFlow = remotePrefs["showYourFlow"] as? Boolean ?: currentPrefs.showYourFlow,
                titleTextSizeMultiplier = (remotePrefs["titleTextSizeMultiplier"] as? Number)?.toFloat() ?: currentPrefs.titleTextSizeMultiplier,
                imageQuality = remotePrefs["imageQuality"] as? String ?: currentPrefs.imageQuality,
                homeSort = parseSortConfig(remotePrefs["homeSort"], currentPrefs.homeSort),
                vistiSort = parseSortConfig(remotePrefs["vistiSort"], currentPrefs.vistiSort),
                foldersSort = parseSortConfig(remotePrefs["foldersSort"], currentPrefs.foldersSort),
                discoveryFilters = parseDiscoveryFilters(remotePrefs["discoveryFilters"], currentPrefs.discoveryFilters),
                showHomeContinueWatching = remotePrefs["showHomeContinueWatching"] as? Boolean ?: currentPrefs.showHomeContinueWatching,
                showHomeBoxOffice = remotePrefs["showHomeBoxOffice"] as? Boolean ?: currentPrefs.showHomeBoxOffice,
                showHomeWatchlist = remotePrefs["showHomeWatchlist"] as? Boolean ?: currentPrefs.showHomeWatchlist,
                showHomeBecauseYouWatched = remotePrefs["showHomeBecauseYouWatched"] as? Boolean ?: currentPrefs.showHomeBecauseYouWatched,
                homeSectionOrder = (remotePrefs["homeSectionOrder"] as? List<*>)?.filterIsInstance<String>()?.let {
                    HomeFeedSectionConstants.sanitizeOrder(it)
                } ?: currentPrefs.homeSectionOrder,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            
            preferenceRepository.updateAll(updatedPrefs)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e("FirebaseSyncCoordinator", "Error syncing preferences: ${e.message}")
        }
    }

    suspend fun savePreferencesRemote(prefs: UserPreferences) {
        syncScope.launch {
            try {
                val prefsMap = mapOf(
                    "gridColumns" to prefs.gridColumns,
                    "showLayoutToggle" to prefs.showLayoutToggle,
                    "isSearchSuggestionsExpanded" to prefs.isSearchSuggestionsExpanded,
                    "notificationsReleases" to prefs.notificationsReleases,
                    "notificationsSocial" to prefs.notificationsSocial,
                    "showFolderBookmarks" to prefs.showFolderBookmarks,
                    "showBadges" to prefs.showBadges,
                    "disabledBadges" to prefs.disabledBadges.toList(),
                    "vibrationEnabled" to prefs.vibrationEnabled,
                    "accentColor" to prefs.accentColor,
                    "appTheme" to prefs.appTheme,
                    "contentLanguage" to prefs.contentLanguage,
                    "advancedVisualEffectsEnabled" to prefs.advancedVisualEffectsEnabled,
                    "dynamicAppIconEnabled" to prefs.dynamicAppIconEnabled,
                    "seasonalAppIconEnabled" to prefs.seasonalAppIconEnabled,
                    "showSplitReleasesHome" to prefs.showSplitReleasesHome,
                    "showSplitDroppedHome" to prefs.showSplitDroppedHome,
                    "showAppEntryAnimation" to prefs.showAppEntryAnimation,
                    "useMovieLogo" to prefs.useMovieLogo,
                    "defaultStartTab" to prefs.defaultStartTab,
                    "defaultStartMedia" to prefs.defaultStartMedia,
                    "showMyFolders" to prefs.showMyFolders,
                    "showYourFlow" to prefs.showYourFlow,
                    "showGeneralStats" to prefs.showGeneralStats,
                    "showTrophies" to prefs.showTrophies,
                    "showDiary" to prefs.showDiary,
                    "dashboardCardOrder" to prefs.dashboardCardOrder,
                    "titleTextSizeMultiplier" to prefs.titleTextSizeMultiplier.toDouble(),
                    "imageQuality" to prefs.imageQuality,
                    "homeSort" to mapOf(
                        "sortType" to prefs.homeSort.sortType,
                        "sortDirection" to prefs.homeSort.sortDirection,
                        "selectedGenres" to prefs.homeSort.selectedGenres,
                        "selectedProviders" to prefs.homeSort.selectedProviders,
                        "selectedDecades" to prefs.homeSort.selectedDecades
                    ),
                    "vistiSort" to mapOf(
                        "sortType" to prefs.vistiSort.sortType,
                        "sortDirection" to prefs.vistiSort.sortDirection,
                        "selectedGenres" to prefs.vistiSort.selectedGenres,
                        "selectedProviders" to prefs.vistiSort.selectedProviders,
                        "selectedDecades" to prefs.vistiSort.selectedDecades
                    ),
                    "foldersSort" to mapOf(
                        "sortType" to prefs.foldersSort.sortType,
                        "sortDirection" to prefs.foldersSort.sortDirection,
                        "selectedGenres" to prefs.foldersSort.selectedGenres,
                        "selectedProviders" to prefs.foldersSort.selectedProviders,
                        "selectedDecades" to prefs.foldersSort.selectedDecades
                    ),
                    "discoveryFilters" to mapOf(
                        "selectedGenres" to prefs.discoveryFilters.selectedGenres,
                        "selectedProviders" to prefs.discoveryFilters.selectedProviders,
                        "selectedDecades" to prefs.discoveryFilters.selectedDecades,
                        "sortBy" to prefs.discoveryFilters.sortBy
                    ),
                    "showHomeContinueWatching" to prefs.showHomeContinueWatching,
                    "showHomeBoxOffice" to prefs.showHomeBoxOffice,
                    "showHomeWatchlist" to prefs.showHomeWatchlist,
                    "showHomeBecauseYouWatched" to prefs.showHomeBecauseYouWatched,
                    "homeSectionOrder" to prefs.homeSectionOrder
                )
                firebaseRemoteDataSource.setUserPreferences(prefsMap)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("FirebaseSyncCoordinator", "Error saving preferences remote", e)
            }
        }
    }

    private fun parseSortConfig(map: Any?, default: SortConfig): SortConfig {
        if (map !is Map<*, *>) return default
        return SortConfig(
            sortType = map["sortType"] as? String ?: default.sortType,
            sortDirection = map["sortDirection"] as? String ?: default.sortDirection,
            selectedGenres = (map["selectedGenres"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: default.selectedGenres,
            selectedProviders = (map["selectedProviders"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: default.selectedProviders,
            selectedDecades = (map["selectedDecades"] as? List<*>)?.filterIsInstance<String>() ?: default.selectedDecades
        )
    }

    private fun parseDiscoveryFilters(map: Any?, default: DiscoveryFilters): DiscoveryFilters {
        if (map !is Map<*, *>) return default
        return DiscoveryFilters(
            selectedGenres = (map["selectedGenres"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: default.selectedGenres,
            selectedProviders = (map["selectedProviders"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: default.selectedProviders,
            selectedDecades = (map["selectedDecades"] as? List<*>)?.filterIsInstance<String>() ?: default.selectedDecades,
            sortBy = map["sortBy"] as? String ?: default.sortBy
        )
    }
}

