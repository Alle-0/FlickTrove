package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.cinetrack.data.repository.BackupRepository
import com.cinetrack.data.repository.FeedbackRepository
import com.cinetrack.data.repository.SettingsRepository
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.data.models.UserPreferences
import com.cinetrack.util.IconManager
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val preferenceRepository: PreferenceRepository,
    private val feedbackRepository: FeedbackRepository,
    private val backupRepository: BackupRepository,
    private val movieRepository: MovieRepository,
    private val traktAuthRepository: com.cinetrack.data.repository.TraktAuthRepository,
    private val auth: FirebaseAuth,
    private val actionFeedbackManager: ActionFeedbackManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isBackupLoading = MutableStateFlow(false)
    val isBackupLoading = _isBackupLoading.asStateFlow()

    /**
     * Holds the pending circular-reveal data: (newColorString, touchOriginInPx).
     * Set right before changing the accent color, cleared once the reveal animation finishes.
     */
    private val _pendingReveal = MutableStateFlow<Pair<String, Offset>?>(null)
    val pendingReveal: StateFlow<Pair<String, Offset>?> = _pendingReveal.asStateFlow()

    /** Call this once the CircularRevealOverlay animation finishes. */
    fun clearPendingReveal() {
        _pendingReveal.value = null
    }

    private val _isAnyDialogOpen = MutableStateFlow(false)
    val isAnyDialogOpen = _isAnyDialogOpen.asStateFlow()

    private val _isFeedbackLoading = MutableStateFlow(false)
    val isFeedbackLoading = _isFeedbackLoading.asStateFlow()

    val isTraktLoggedIn: StateFlow<Boolean> = traktAuthRepository.isLoggedIn

    fun disconnectTrakt() {
        traktAuthRepository.clearAuth()
        viewModelScope.launch {
            actionFeedbackManager.emit(UiText.DynamicString(context.getString(R.string.trakt_disconnected)))
        }
    }

    val syncWorkInfo = androidx.work.WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow("TraktManualSync")
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun syncTraktNow() {
        if (!isTraktLoggedIn.value) return
        viewModelScope.launch {
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.cinetrack.worker.TraktSyncWorker>()
                .build() // No constraints for forced manual sync
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                "TraktManualSync",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )
            actionFeedbackManager.emit(UiText.DynamicString(context.getString(R.string.trakt_manual_sync_started)))
        }
    }

    fun setAnyDialogOpen(isOpen: Boolean) {
        _isAnyDialogOpen.value = isOpen
    }

    private val _closeDialogsEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closeDialogsEvent = _closeDialogsEvent.asSharedFlow()

    fun triggerCloseDialogs() {
        _closeDialogsEvent.tryEmit(Unit)
    }

    val accentColor: StateFlow<String> = settingsRepository.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Teal")

    val showFolderBookmarks: StateFlow<Boolean> = settingsRepository.showFolderBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showBadges: StateFlow<Boolean> = settingsRepository.showBadges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val disabledBadges: StateFlow<Set<String>> = settingsRepository.disabledBadges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrationEnabled: StateFlow<Boolean> = settingsRepository.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val advancedVisualEffectsEnabled: StateFlow<Boolean> = settingsRepository.advancedVisualEffectsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicAppIconEnabled: StateFlow<Boolean> = settingsRepository.dynamicAppIconEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showLayoutToggle: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showLayoutToggle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showSplitReleasesHome: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showSplitReleasesHome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showAppEntryAnimation: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showAppEntryAnimation }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val useMovieLogo: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.useMovieLogo }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appTheme: StateFlow<String> = preferenceRepository.userPreferencesFlow
        .map { it.appTheme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")

    val defaultStartTab: StateFlow<String> = preferenceRepository.userPreferencesFlow
        .map { it.defaultStartTab }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "home")

    val contentLanguage: StateFlow<String> = preferenceRepository.userPreferencesFlow
        .map { it.contentLanguage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val imageQuality: StateFlow<com.cinetrack.util.ImageQuality> = settingsRepository.imageQuality
        .map { com.cinetrack.util.ImageQuality.valueOf(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.cinetrack.util.ImageQuality.HIGH)

    val titleTextSizeMultiplier: StateFlow<Float> = settingsRepository.titleTextSizeMultiplier
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    fun updateAccentColor(color: String, revealOrigin: Offset? = null) {
        viewModelScope.launch {
            if (revealOrigin != null) {
                // Publish the pending reveal BEFORE persisting so the overlay
                // can capture the screenshot of the old theme.
                _pendingReveal.value = Pair(color, revealOrigin)
            } else {
                settingsRepository.updateAccentColor(color)
                IconManager.updateAppIcon(context, color, dynamicAppIconEnabled.value)
                movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_accent_updated))
            }
        }
    }

    fun applyPendingTheme() {
        viewModelScope.launch {
            _pendingReveal.value?.let { (color, _) ->
                settingsRepository.updateAccentColor(color)
                movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_accent_updated))
            }
        }
    }

    fun applyPendingIcon(colorName: String) {
        viewModelScope.launch {
            IconManager.updateAppIcon(context, colorName, dynamicAppIconEnabled.value)
        }
    }

    fun toggleFolderBookmarks(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleFolderBookmarks(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_visible_plural else R.string.status_hidden_plural
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_folder_bookmarks, context.getString(statusRes)))
        }
    }

    fun toggleBadges(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleBadges(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_visible_plural else R.string.status_hidden_plural
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_badge, context.getString(statusRes)))
        }
    }

    fun toggleLayoutToggle(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowLayoutToggle(enabled)
            val statusRes = if (enabled) R.string.status_visible else R.string.status_hidden
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_layout_btn, context.getString(statusRes)))
        }
    }

    fun toggleSplitReleasesHome(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowSplitReleasesHome(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_enabled_f else R.string.status_disabled_f
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_releases_split, context.getString(statusRes)))
        }
    }

    fun toggleAppEntryAnimation(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowAppEntryAnimation(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_enabled_f else R.string.status_disabled_f
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_logo_anim, context.getString(statusRes)))
        }
    }

    fun toggleBadgeEnabled(badge: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = disabledBadges.value.toMutableSet()
            if (enabled) {
                current.remove(badge)
            } else {
                current.add(badge)
            }
            settingsRepository.updateDisabledBadges(current)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleNotifications(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_enabled_plural_f else R.string.status_disabled_plural_f
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_notifications, context.getString(statusRes)))
        }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleVibration(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_active_f else R.string.status_inactive_f
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_vibration, context.getString(statusRes)))
        }
    }

    fun toggleDynamicAppIcon(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleDynamicAppIcon(enabled)
            IconManager.updateAppIcon(context, accentColor.value, enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_enabled_f else R.string.status_disabled_f
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_dynamic_icon, context.getString(statusRes)))
        }
    }

    fun toggleAdvancedVisualEffects(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleAdvancedVisualEffects(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_enabled_plural_m else R.string.status_disabled_plural_m
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_visual_fx, context.getString(statusRes)))
        }
    }

    fun updateImageQuality(quality: com.cinetrack.util.ImageQuality) {
        viewModelScope.launch {
            settingsRepository.updateImageQuality(quality.name)
            val desc = when(quality) {
                com.cinetrack.util.ImageQuality.LOW -> com.cinetrack.ui.utils.UiText.StringResource(R.string.quality_low_desc)
                com.cinetrack.util.ImageQuality.MEDIUM -> com.cinetrack.ui.utils.UiText.StringResource(R.string.quality_medium_desc)
                com.cinetrack.util.ImageQuality.HIGH -> com.cinetrack.ui.utils.UiText.StringResource(R.string.quality_high_desc)
            }
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_img_quality, desc))
        }
    }

    fun updateTitleTextSizeMultiplier(multiplier: Float) {
        viewModelScope.launch {
            settingsRepository.updateTitleTextSizeMultiplier(multiplier)
            val desc = when(multiplier) {
                0.8f -> "Piccolo"
                1.0f -> "Predefinito"
                1.2f -> "Grande"
                else -> "Personalizzato"
            }
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_text_size, desc))
        }
    }

    fun toggleUseMovieLogo(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateUseMovieLogo(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
        }
    }

    fun setDefaultStartTab(tab: String) {
        viewModelScope.launch {
            preferenceRepository.updateDefaultStartTab(tab)
        }
    }

    fun updateAppTheme(theme: String) {
        viewModelScope.launch {
            preferenceRepository.updateAppTheme(theme)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_theme, theme))
        }
    }

    fun updateContentLanguage(language: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            preferenceRepository.updateContentLanguage(language)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_language, language))
            onSuccess()
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearImageCache() {
        viewModelScope.launch {
            context.imageLoader.diskCache?.clear()
            context.imageLoader.memoryCache?.clear()
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_cache_cleared))
        }
    }

    val libraryDetailsSyncWorkInfo = androidx.work.WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow("LibraryDetailsSyncWorker")
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun syncLibraryDetails() {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.cinetrack.worker.LibraryDetailsSyncWorker>().build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "LibraryDetailsSyncWorker",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
        viewModelScope.launch {
            actionFeedbackManager.emit(UiText.DynamicString("Sincronizzazione dettagli avviata in background"))
        }
    }

    fun sendFeedback(title: String, description: String, rating: Int, email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val lastTime = settingsRepository.lastFeedbackTimestamp.first()
            val currentTime = System.currentTimeMillis()
            val twelveHoursInMillis = 12 * 60 * 60 * 1000L
            
            if (currentTime - lastTime < twelveHoursInMillis) {
                val hoursRemaining = ((twelveHoursInMillis - (currentTime - lastTime)) / (1000 * 60 * 60)).toInt()
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_feedback_wait, hoursRemaining))
                return@launch
            }

            _isFeedbackLoading.value = true
            try {
                val userId = auth.currentUser?.uid ?: "anonymous"
                val feedback = com.cinetrack.data.models.Feedback(
                    userId = userId,
                    userEmail = email.ifBlank { auth.currentUser?.email ?: "" },
                    title = title,
                    description = description,
                    rating = rating,
                    appVersion = "3.1.3" // Aligning with UI version
                )
                val result = feedbackRepository.sendFeedback(feedback)
                if (result.isSuccess) {
                    settingsRepository.updateLastFeedbackTimestamp(currentTime)
                    actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_feedback_thanks))
                    onSuccess()
                } else {
                    actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_feedback_error))
                }
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_connection_error))
            } finally {
                _isFeedbackLoading.value = false
            }
        }
    }

    suspend fun getBackupData(): String? {
        _isBackupLoading.value = true
        return try {
            backupRepository.exportData()
        } catch (e: Exception) {
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_export_error))
            null
        } finally {
            _isBackupLoading.value = false
        }
    }

    fun restoreData(json: String) {
        viewModelScope.launch {
            _isBackupLoading.value = true
            try {
                backupRepository.importDataStream(json.byteInputStream())
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_restore_success))
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_restore_error))
            } finally {
                _isBackupLoading.value = false
            }
        }
    }

    fun migrateExternalData(fileContent: String) {
        viewModelScope.launch {
            _isBackupLoading.value = true
            try {
                val isJson = fileContent.trimStart().startsWith("[") || fileContent.trimStart().startsWith("{")
                val count = if (isJson) {
                    backupRepository.migrateTraktStream(fileContent.byteInputStream())
                } else {
                    backupRepository.migrateCsvStream(fileContent.byteInputStream())
                }
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_import_success, count))
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_import_error))
            } finally {
                _isBackupLoading.value = false
            }
        }
    }
}
