package com.cinetrack.ui.viewmodel

import com.cinetrack.BuildConfig
import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import com.cinetrack.data.repository.BackupRepository
import com.cinetrack.data.repository.FeedbackRepository
import com.cinetrack.data.repository.SettingsRepository
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
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
import com.cinetrack.data.model.UserPreferences
import com.cinetrack.util.IconManager
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Dispatchers
import java.io.File
import com.cinetrack.ui.components.common.CircularRevealOverlay

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val preferenceRepository: PreferenceRepository,
    private val feedbackRepository: FeedbackRepository,
    private val backupRepository: BackupRepository,
    private val movieRepository: MovieRepository,
    private val traktAuthRepository: com.cinetrack.data.repository.TraktAuthRepository,
    private val traktService: dagger.Lazy<com.cinetrack.data.api.TraktService>,
    private val simklAuthRepository: com.cinetrack.data.repository.SimklAuthRepository,
    private val simklService: dagger.Lazy<com.cinetrack.data.api.SimklService>,
    private val auth: FirebaseAuth,
    private val actionFeedbackManager: ActionFeedbackManager,
    private val appUpdateManager: com.cinetrack.util.AppUpdateManager,
    private val blockedAuthorsManager: com.cinetrack.data.repository.BlockedAuthorsManager,
    private val commsUniRepository: com.cinetrack.data.repository.CommsUniRepository,
    private val sharedFolderRepository: dagger.Lazy<com.cinetrack.data.repository.SharedFolderRepository>,
    private val badgeRepository: dagger.Lazy<com.cinetrack.data.repository.BadgeRepository>,
    private val guestAuthManager: com.cinetrack.ui.utils.GuestAuthManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    fun updateCommsUniProfile(displayName: String, avatarUrl: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                commsUniRepository.updateProfile(displayName, avatarUrl)
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to sync profile to CommsUni", e)
            }
            try {
                sharedFolderRepository.get().syncProfileToSharedFolders(displayName, avatarUrl)
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to sync profile to Shared Folders", e)
            }
            try {
                auth.currentUser?.uid?.let { uid ->
                    com.cinetrack.ui.components.folder.FolderMembersAvatarCache.put(uid, avatarUrl.orEmpty())
                    context.getSharedPreferences("commsuni_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putString("user_avatar_$uid", avatarUrl.orEmpty())
                        .putString("user_name_$uid", displayName)
                        .putBoolean("user_avatar_checked_$uid", true)
                        .apply()
                }
            } catch (_: Exception) {}
        }
    }

    val updateInfo = appUpdateManager.updateInfo

    val blockedAuthorsList: StateFlow<List<com.cinetrack.data.repository.BlockedAuthor>> = 
        blockedAuthorsManager.blockedAuthorsList

    fun unblockAuthor(authorId: String) {
        viewModelScope.launch {
            blockedAuthorsManager.unblockAuthor(authorId)
            emitToast(UiText.StringResource(R.string.comment_unblock_user_success))
        }
    }

    var editProfilePencilBounds: Rect? = null

    private val _showEditProfileMenu = MutableSharedFlow<Rect?>()
    val showEditProfileMenu = _showEditProfileMenu.asSharedFlow()

    val showGuestAuthDialog = guestAuthManager.events

    private val _showDashboardSettingsMenu = MutableSharedFlow<Unit>()
    val showDashboardSettingsMenu = _showDashboardSettingsMenu.asSharedFlow()

    fun triggerEditProfileMenu(triggerBounds: Rect? = null) {
        viewModelScope.launch {
            _showEditProfileMenu.emit(triggerBounds ?: editProfilePencilBounds)
        }
    }
    
    fun triggerGuestAuthDialog(
        @androidx.annotation.StringRes titleRes: Int? = null,
        @androidx.annotation.StringRes messageRes: Int? = null
    ) {
        guestAuthManager.triggerGuestAuthDialog(titleRes, messageRes)
    }
    
    fun triggerDashboardSettingsMenu() {
        viewModelScope.launch {
            _showDashboardSettingsMenu.emit(Unit)
        }
    }

    private val _isSettingsLoaded = MutableStateFlow(false)
    val isSettingsLoaded: StateFlow<Boolean> = _isSettingsLoaded.asStateFlow()

    init {
        checkForUpdates(false)
        viewModelScope.launch {
            try {
                movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            } catch (e: Exception) { }
        }
        viewModelScope.launch {
            try {
                kotlinx.coroutines.flow.combine(
                    settingsRepository.accentColor,
                    settingsRepository.dynamicAppIconEnabled,
                    settingsRepository.seasonalAppIconEnabled
                ) { _, _, _ -> true }.first()
                _isSettingsLoaded.value = true
            } catch (_: Exception) { }
        }
    }

    fun checkForUpdates(force: Boolean = false) {
        viewModelScope.launch {
            appUpdateManager.checkForUpdates(force)
        }
    }

    val dismissedUpdateVersion = appUpdateManager.dismissedVersionThisSession

    val ignoredUpdateVersion = settingsRepository.ignoredUpdateVersion.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    fun dismissUpdate(version: String) {
        appUpdateManager.dismissUpdateForSession(version)
    }

    fun ignoreUpdatePermanently(version: String) {
        viewModelScope.launch {
            settingsRepository.updateIgnoredUpdateVersion(version)
            appUpdateManager.dismissUpdateForSession(version)
        }
    }

    fun isUpdateDismissed(version: String): Boolean {
        return appUpdateManager.isDismissedForSession(version)
    }

    val lastSeenAppVersion = settingsRepository.lastSeenAppVersion.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BuildConfig.VERSION_NAME
    )

    fun markCurrentAppVersionSeen() {
        viewModelScope.launch {
            settingsRepository.updateLastSeenAppVersion(BuildConfig.VERSION_NAME)
        }
    }

    val hasSeenOnboarding = settingsRepository.hasSeenOnboarding.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true // default true while loading
    )

    fun markOnboardingSeen() {
        viewModelScope.launch {
            settingsRepository.setOnboardingSeen(true)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingSeen(false)
        }
    }

    // Fired when the user taps "Replay Tutorial" from inside the settings overlay.
    // The overlay collects this event, animates out first, then resets onboarding
    // so the tutorial dialog appears on top instead of underneath.
    private val _replayTutorialEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val replayTutorialEvent = _replayTutorialEvent.asSharedFlow()

    fun requestReplayTutorial() {
        _replayTutorialEvent.tryEmit(Unit)
    }

    val hideSavedFromDiscovery = settingsRepository.hideSavedFromDiscovery.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun toggleHideSavedFromDiscovery(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleHideSavedFromDiscovery(enabled)
        }
    }

    val moodHideSaved = settingsRepository.moodHideSaved.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun toggleMoodHideSaved(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleMoodHideSaved(enabled)
        }
    }

    val promptWatchDateOnDetail = settingsRepository.promptWatchDateOnDetail.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun togglePromptWatchDateOnDetail(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.togglePromptWatchDateOnDetail(enabled)
        }
    }

    val autoOpenVibeOnWatch = settingsRepository.autoOpenVibeOnWatch.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun toggleAutoOpenVibeOnWatch(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleAutoOpenVibeOnWatch(enabled)
        }
    }

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

    val unmatchedMovies: StateFlow<List<Movie>> = movieRepository.getLocalMoviesFlow()
        .map { movies -> movies.filter { it.posterPath == null && it.overview == null } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isAnyDialogOpen = MutableStateFlow(false)
    val isAnyDialogOpen = _isAnyDialogOpen.asStateFlow()

    private val _isFeedbackLoading = MutableStateFlow(false)
    val isFeedbackLoading = _isFeedbackLoading.asStateFlow()

    private val externalSyncDelegate by lazy {
        SettingsExternalSyncDelegate(
            traktAuthRepository = traktAuthRepository,
            traktService = traktService,
            simklAuthRepository = simklAuthRepository,
            simklService = simklService,
            badgeRepository = badgeRepository,
            actionFeedbackManager = actionFeedbackManager,
            context = context
        )
    }

    private val backupDelegate by lazy {
        SettingsBackupDelegate(
            backupRepository = backupRepository,
            movieRepository = movieRepository,
            actionFeedbackManager = actionFeedbackManager,
            context = context
        )
    }

    val isTraktLoggedIn: StateFlow<Boolean> get() = externalSyncDelegate.isTraktLoggedIn

    /** Emits true when the session was forcibly cleared after a token-refresh failure. */
    val traktNeedsReconnect: StateFlow<Boolean> get() = externalSyncDelegate.traktNeedsReconnect

    val isSimklLoggedIn: StateFlow<Boolean> by lazy { externalSyncDelegate.createSimklLoggedInFlow(viewModelScope) }

    fun disconnectTrakt() = externalSyncDelegate.disconnectTrakt(viewModelScope)

    fun disconnectSimkl() = externalSyncDelegate.disconnectSimkl(viewModelScope)

    /** Generates and persists the OAuth CSRF state before opening the Trakt browser flow. */
    fun savePendingOAuthState(state: String) = externalSyncDelegate.savePendingOAuthState(state)

    val syncWorkInfo by lazy { externalSyncDelegate.createSyncWorkInfoFlow(viewModelScope) }

    val externalImportWorkInfo = androidx.work.WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow("ExternalImportWork")
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun syncTraktNow() = externalSyncDelegate.syncTraktNow(viewModelScope)


    fun setAnyDialogOpen(isOpen: Boolean) {
        _isAnyDialogOpen.value = isOpen
    }

    private val _closeDialogsEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closeDialogsEvent = _closeDialogsEvent.asSharedFlow()

    fun triggerCloseDialogs() {
        _closeDialogsEvent.tryEmit(Unit)
    }

    val accentColor: StateFlow<String> = settingsRepository.accentColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Teal")

    val showFolderBookmarks: StateFlow<Boolean> = settingsRepository.showFolderBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showBadges: StateFlow<Boolean> = settingsRepository.showBadges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val disabledBadges: StateFlow<Set<String>> = settingsRepository.disabledBadges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val notificationsReleases: StateFlow<Boolean> = settingsRepository.notificationsReleases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val allowMobileDataUpdates: StateFlow<Boolean> = settingsRepository.allowMobileDataUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationsSocial: StateFlow<Boolean> = settingsRepository.notificationsSocial
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrationEnabled: StateFlow<Boolean> = settingsRepository.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val episodesLayout: StateFlow<String> = settingsRepository.episodesLayout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "STANDARD")

    val advancedVisualEffectsEnabled: StateFlow<Boolean> = settingsRepository.advancedVisualEffectsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicAppIconEnabled: StateFlow<Boolean> = settingsRepository.dynamicAppIconEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val seasonalAppIconEnabled: StateFlow<Boolean> = settingsRepository.seasonalAppIconEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showLayoutToggle: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showLayoutToggle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val enableBottomBarSwipe: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.enableBottomBarSwipe }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showSplitReleasesHome: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showSplitReleasesHome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showSplitDroppedHome: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showSplitDroppedHome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showAppEntryAnimation: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showAppEntryAnimation }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val useMovieLogo: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.useMovieLogo }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appTheme: StateFlow<String> = preferenceRepository.userPreferencesFlow
        .map { it.appTheme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "AMOLED")

    val defaultStartTab: StateFlow<String> = preferenceRepository.userPreferencesFlow
        .map { it.defaultStartTab }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "feed")

    val defaultStartMedia: StateFlow<String> = preferenceRepository.userPreferencesFlow
        .map { it.defaultStartMedia }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "movie")

    val contentLanguage: StateFlow<String> = preferenceRepository.userPreferencesFlow
        .map { it.contentLanguage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val imageQuality: StateFlow<com.cinetrack.util.ImageQuality> = settingsRepository.imageQuality
        .map { com.cinetrack.util.ImageQuality.valueOf(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.cinetrack.util.ImageQuality.HIGH)

    val titleTextSizeMultiplier: StateFlow<Float> = settingsRepository.titleTextSizeMultiplier
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val episodeInfoStyle: StateFlow<String> = settingsRepository.episodeInfoStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "REMAINING")

    val progressBarTarget: StateFlow<String> = settingsRepository.progressBarTarget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CIRCULAR_SEASON")

    val linearProgressBar: StateFlow<String> = settingsRepository.linearProgressBar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "TOTAL")

    val circularProgressBar: StateFlow<String> = settingsRepository.circularProgressBar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SEASON")

    val showMyFolders: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showMyFolders }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showYourFlow: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showYourFlow }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showGeneralStats: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showGeneralStats }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showTrophies: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showTrophies }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showDiary: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showDiary }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dashboardCardOrder: StateFlow<List<String>> = preferenceRepository.userPreferencesFlow
        .map { it.dashboardCardOrder }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("stats", "folders", "trophies", "flow", "diary"))

    val showHomeContinueWatching: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showHomeContinueWatching }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showHomeBoxOffice: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showHomeBoxOffice }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showHomeWatchlist: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showHomeWatchlist }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showHomeBecauseYouWatched: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.showHomeBecauseYouWatched }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val homeSectionOrder: StateFlow<List<String>> = preferenceRepository.userPreferencesFlow
        .map { it.homeSectionOrder }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.cinetrack.data.model.HomeFeedSectionConstants.DEFAULT_ORDER)

    fun updateAccentColor(color: String, revealOrigin: Offset? = null) {
        viewModelScope.launch {
            if (revealOrigin != null && revealOrigin != Offset.Zero) {
                // Publish the pending reveal BEFORE persisting so the overlay
                // can capture the screenshot of the old theme.
                _pendingReveal.value = Pair(color, revealOrigin)
            } else {
                settingsRepository.updateAccentColor(color)
                IconManager.updateAppIcon(context, color, dynamicAppIconEnabled.value, seasonalAppIconEnabled.value)
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_accent_updated))
                // Remote sync in background
                launch {
                    try {
                        movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
                    } catch (_: Exception) { }
                }
            }
        }
    }

    fun applyPendingTheme() {
        viewModelScope.launch {
            _pendingReveal.value?.let { (color, _) ->
                settingsRepository.updateAccentColor(color)
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_accent_updated))
                // Remote sync in background
                launch {
                    try {
                        movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
                    } catch (_: Exception) { }
                }
            }
        }
    }

    fun applyPendingIcon(colorName: String) {
        viewModelScope.launch {
            IconManager.updateAppIcon(context, colorName, dynamicAppIconEnabled.value, seasonalAppIconEnabled.value)
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
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_visible else R.string.status_hidden
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_layout_btn, context.getString(statusRes)))
        }
    }

    fun toggleEnableBottomBarSwipe(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateEnableBottomBarSwipe(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_enabled_f else R.string.status_disabled_f
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_bottom_bar_swipe, context.getString(statusRes)))
        }
    }

    fun updateEpisodesLayout(layout: String) {
        viewModelScope.launch {
            settingsRepository.updateEpisodesLayout(layout)
        }
    }

    fun toggleSplitReleasesHome(enabled: Boolean) {
        viewModelScope.launch {
            val currentPrefs = preferenceRepository.userPreferencesFlow.first()
            preferenceRepository.updateShowSplitReleasesHome(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_enabled_f else R.string.status_disabled_f
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_releases_split, context.getString(statusRes)))
        }
    }

    fun toggleSplitDroppedHome(enabled: Boolean) {
        viewModelScope.launch {
            val currentPrefs = preferenceRepository.userPreferencesFlow.first()
            preferenceRepository.updateShowSplitDroppedHome(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_enabled_f else R.string.status_disabled_f
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_dropped_split, context.getString(statusRes)))
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

    fun toggleShowMyFolders(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowMyFolders(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_visible else R.string.status_hidden
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_my_folders, context.getString(statusRes)))
        }
    }

    fun toggleShowYourFlow(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowYourFlow(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_visible else R.string.status_hidden
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_your_flow, context.getString(statusRes)))
        }
    }

    fun toggleShowGeneralStats(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowGeneralStats(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_visible else R.string.status_hidden
            actionFeedbackManager.emit(UiText.DynamicString("General Stats ${context.getString(statusRes)}")) // Assuming no specific string res yet
        }
    }

    fun toggleShowTrophies(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowTrophies(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_visible else R.string.status_hidden
            actionFeedbackManager.emit(UiText.DynamicString("Trophy Room ${context.getString(statusRes)}"))
        }
    }

    fun toggleShowDiary(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowDiary(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_visible else R.string.status_hidden
            actionFeedbackManager.emit(UiText.DynamicString("Diary ${context.getString(statusRes)}"))
        }
    }

    fun updateDashboardCardOrder(order: List<String>) {
        viewModelScope.launch {
            preferenceRepository.updateDashboardCardOrder(order)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
        }
    }

    fun toggleShowHomeContinueWatching(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowHomeContinueWatching(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
        }
    }

    fun toggleShowHomeBoxOffice(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowHomeBoxOffice(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
        }
    }

    fun toggleShowHomeWatchlist(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowHomeWatchlist(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
        }
    }

    fun toggleShowHomeBecauseYouWatched(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateShowHomeBecauseYouWatched(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
        }
    }

    fun resetHomeFeedSectionsToDefault() {
        viewModelScope.launch {
            preferenceRepository.updateHomeSectionOrder(com.cinetrack.data.model.HomeFeedSectionConstants.DEFAULT_ORDER)
            preferenceRepository.updateShowHomeContinueWatching(true)
            preferenceRepository.updateShowHomeBoxOffice(true)
            preferenceRepository.updateShowHomeWatchlist(true)
            preferenceRepository.updateShowHomeBecauseYouWatched(true)
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_home_feed_reset))
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
        }
    }

    fun updateHomeSectionOrder(order: List<String>) {
        viewModelScope.launch {
            preferenceRepository.updateHomeSectionOrder(order)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
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

    fun toggleNotificationsReleases(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleNotificationsReleases(enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_enabled_plural_f else R.string.status_disabled_plural_f
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_notifications, context.getString(statusRes)))
        }
    }

    fun toggleAllowMobileDataUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleAllowMobileDataUpdates(enabled)
            com.cinetrack.FlickTroveApplication.scheduleWorkers(context, enabled)
            val statusRes = if (enabled) R.string.status_enabled_plural_m else R.string.status_disabled_plural_m
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_mobile_data_updates, context.getString(statusRes)))
        }
    }

    fun toggleNotificationsSocial(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleNotificationsSocial(enabled)
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
            IconManager.updateAppIcon(context, accentColor.value, enabled, seasonalAppIconEnabled.value)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_enabled_f else R.string.status_disabled_f
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_dynamic_icon, context.getString(statusRes)))
        }
    }

    fun toggleSeasonalAppIcon(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleSeasonalAppIcon(enabled)
            IconManager.updateAppIcon(context, accentColor.value, dynamicAppIconEnabled.value, enabled)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val statusRes = if (enabled) R.string.status_enabled_f else R.string.status_disabled_f
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_seasonal_icon, context.getString(statusRes)))
        }
    }

    fun exchangeTraktCode(code: String, returnedState: String? = null) =
        externalSyncDelegate.exchangeTraktCode(code, returnedState, viewModelScope)

    fun savePendingSimklOAuth(codeVerifier: String, state: String) =
        externalSyncDelegate.savePendingSimklOAuth(codeVerifier, state, viewModelScope)

    fun exchangeSimklCode(code: String, returnedState: String? = null) =
        externalSyncDelegate.exchangeSimklCode(code, returnedState, viewModelScope)

    val simklSyncWorkInfo by lazy { externalSyncDelegate.createSimklSyncWorkInfoFlow(viewModelScope) }

    fun syncSimklNow() = externalSyncDelegate.syncSimklNow(viewModelScope)


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
            preferenceRepository.updateImageQuality(quality.name)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
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
            preferenceRepository.updateTitleTextSizeMultiplier(multiplier)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            val desc = when(multiplier) {
                0.8f -> UiText.StringResource(R.string.settings_size_small)
                1.0f -> UiText.StringResource(R.string.settings_size_medium)
                1.2f -> UiText.StringResource(R.string.settings_size_large)
                else -> UiText.StringResource(R.string.settings_custom)
            }
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_text_size, desc))
        }
    }

    fun updateEpisodeInfoStyle(style: String) {
        viewModelScope.launch {
            settingsRepository.updateEpisodeInfoStyle(style)
        }
    }

    fun updateProgressBarTarget(target: String) {
        viewModelScope.launch {
            settingsRepository.updateProgressBarTarget(target)
        }
    }

    fun updateLinearProgressBar(target: String) {
        viewModelScope.launch {
            settingsRepository.updateLinearProgressBar(target)
        }
    }

    fun updateCircularProgressBar(target: String) {
        viewModelScope.launch {
            settingsRepository.updateCircularProgressBar(target)
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
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
        }
    }

    fun setDefaultStartMedia(media: String) {
        viewModelScope.launch {
            preferenceRepository.updateDefaultStartMedia(media)
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
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
            launch(Dispatchers.IO) {
                movieRepository.ensureRegionalWatchProvidersLoaded(
                    com.cinetrack.util.RegionResolver.resolveRegion(language)
                )
            }
            movieRepository.savePreferencesRemote(preferenceRepository.userPreferencesFlow.first())
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_language, language))
            onSuccess()
        }
    }

    @OptIn(coil3.annotation.ExperimentalCoilApi::class)
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
            actionFeedbackManager.emit(UiText.StringResource(R.string.settings_msg_sync_details_started))
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
                val feedback = com.cinetrack.data.model.Feedback(
                    userId = userId,
                    userEmail = email.ifBlank { auth.currentUser?.email?.let { "$it (non inserita)" } ?: "Non inserita" },
                    title = title,
                    description = description,
                    rating = rating,
                    appVersion = com.cinetrack.BuildConfig.VERSION_NAME,
                    deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                    androidVersion = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
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

    suspend fun getBackupData(): String? = backupDelegate.getBackupData()

    fun restoreData(json: String) = backupDelegate.restoreData(json, viewModelScope)

    fun restoreFile(filePath: String) = backupDelegate.restoreFile(filePath, viewModelScope)

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                auth.currentUser?.delete()?.await()
                actionFeedbackManager.emit(UiText.StringResource(R.string.settings_account_deleted_success))
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.DynamicString(e.message ?: "Failed to delete account"))
            }
        }
    }

    fun wipeLocalData() = backupDelegate.wipeLocalData(viewModelScope)

    fun wipeTotalData() = backupDelegate.wipeTotalData(viewModelScope)

    fun countDuplicateRewatches(onResult: (Int) -> Unit) =
        backupDelegate.countDuplicateRewatches(viewModelScope, onResult)

    fun cleanupRewatches(onComplete: (Int) -> Unit) =
        backupDelegate.cleanupRewatches(viewModelScope, onComplete)

    fun migrateExternalData(fileContent: String) =
        backupDelegate.migrateExternalData(fileContent, viewModelScope)

    fun migrateExternalFile(filePath: String) =
        backupDelegate.migrateExternalFile(filePath, viewModelScope)

    fun emitToast(uiText: UiText) {
        actionFeedbackManager.emit(uiText)
    }

    fun deleteUnmatchedItem(movie: Movie) =
        backupDelegate.deleteUnmatchedItem(movie, viewModelScope)

}
