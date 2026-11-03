package com.cinetrack.ui.components.settings

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.dialog.BlockedUsersModal
import com.cinetrack.ui.components.dialog.UnmatchedItemsModal
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.viewmodel.AuthState
import com.cinetrack.ui.viewmodel.AuthViewModel
import com.cinetrack.data.repository.BlockedAuthor
import com.cinetrack.ui.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Host component managing all modal dialogs, confirm sheets, and overlays in SettingsScreen.
 * Consolidates dialog rendering into a single decoupled layer.
 */
@Composable
fun SettingsModalsHost(
    // Visibility flags
    showDeleteDialog: Boolean,
    showReauthDialog: Boolean,
    reauthErrorMessage: String?,
    showCacheConfirm: Boolean,
    showDeepSyncConfirm: Boolean,
    showLogoutConfirm: Boolean,
    showWipeSelectionDialog: Boolean,
    showWipeLocalDataConfirm: Boolean,
    showWipeTotalDataConfirm: Boolean,
    showCleanupRewatchesDialog: Boolean,
    duplicateRewatchesCount: Int,
    isCleaningUpRewatches: Boolean,
    showColorDialog: Boolean,
    showSeasonalIconsDialog: Boolean,
    seasonalAppIconEnabled: Boolean,
    showLanguageDialog: Boolean,
    contentLanguage: String,
    showStartScreenDialog: Boolean,
    defaultStartTab: String,
    showStartMediaDialog: Boolean,
    defaultStartMedia: String,
    showFeedbackDialog: Boolean,
    isFeedbackLoading: Boolean,
    showBackupDialog: Boolean,
    isBackupLoading: Boolean,
    showExternalMigrationDialog: Boolean,
    showUnmatchedItemsModal: Boolean,
    unmatchedMovies: List<Movie>,
    showBlockedUsersModal: Boolean,
    blockedUsers: List<BlockedAuthor>,
    showDashboardSettings: Boolean,
    showHomeSectionsDialog: Boolean,
    showCardAppearanceDialog: Boolean,
    showBadgesInfoDialog: Boolean,
    disabledBadges: Set<String>,
    vibrationEnabled: Boolean,
    accentColorName: String,
    currentAccentColor: Color,
    // Haze states
    activeHazeState: HazeState,
    cardHazeState: HazeState,
    // Dependencies & callbacks
    context: Context,
    scope: CoroutineScope,
    viewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    exportLauncher: ActivityResultLauncher<String>,
    importLauncher: ActivityResultLauncher<Array<String>>,
    externalMigrationLauncher: ActivityResultLauncher<Array<String>>,
    onDismissDeleteDialog: () -> Unit,
    onDismissReauthDialog: () -> Unit,
    onReauthErrorUpdate: (String?) -> Unit,
    onDismissCacheConfirm: () -> Unit,
    onClearCacheConfirmed: () -> Unit,
    onDismissDeepSyncConfirm: () -> Unit,
    onDismissLogoutConfirm: () -> Unit,
    onDismissWipeSelectionDialog: () -> Unit,
    onSelectWipeLocal: () -> Unit,
    onSelectWipeTotal: () -> Unit,
    onDismissWipeLocalConfirm: () -> Unit,
    onDismissWipeTotalConfirm: () -> Unit,
    onDismissCleanupRewatchesDialog: () -> Unit,
    onStartCleaningUpRewatches: () -> Unit,
    onFinishCleaningUpRewatches: () -> Unit,
    onDismissColorDialog: () -> Unit,
    onDismissSeasonalIconsDialog: () -> Unit,
    onDismissLanguageDialog: () -> Unit,
    onDismissStartScreenDialog: () -> Unit,
    onDismissStartMediaDialog: () -> Unit,
    onDismissFeedbackDialog: () -> Unit,
    onDismissBackupDialog: () -> Unit,
    onDismissExternalMigrationDialog: () -> Unit,
    onDismissUnmatchedItemsModal: () -> Unit,
    onDismissBlockedUsersModal: () -> Unit,
    onDismissDashboardSettings: () -> Unit,
    onDismissHomeSectionsDialog: () -> Unit,
    onDismissCardAppearanceDialog: () -> Unit,
    onOpenBadgesInfoDialog: () -> Unit,
    onDismissBadgesInfoDialog: () -> Unit
) {
    DeleteAccountDialog(
        visible = showDeleteDialog,
        activeHazeState = activeHazeState,
        onDismiss = onDismissDeleteDialog,
        onConfirm = {
            onDismissDeleteDialog()
            viewModel.deleteAccount {}
        }
    )

    ReauthDeleteAccountDialog(
        visible = showReauthDialog,
        activeHazeState = activeHazeState,
        errorMessage = reauthErrorMessage,
        onDismiss = {
            onDismissReauthDialog()
            viewModel.resetProcessState()
        },
        onConfirm = { password ->
            onReauthErrorUpdate(null)
            viewModel.deleteAccountWithReauth(password) { success ->
                if (!success) {
                    val state = viewModel.authState.value
                    if (state is AuthState.Error) {
                        val message = (state.message as? com.cinetrack.ui.utils.UiText.StringResource)
                            ?.let { context.getString(it.resId) }
                            ?: (state.message as? com.cinetrack.ui.utils.UiText.DynamicString)?.value
                            ?: "Incorrect password"
                        onReauthErrorUpdate(message)
                    }
                }
            }
        }
    )

    ClearCacheConfirmDialog(
        visible = showCacheConfirm,
        activeHazeState = activeHazeState,
        onDismiss = onDismissCacheConfirm,
        onConfirm = {
            settingsViewModel.clearImageCache()
            onClearCacheConfirmed()
            onDismissCacheConfirm()
        }
    )

    DeepSyncConfirmDialog(
        visible = showDeepSyncConfirm,
        activeHazeState = activeHazeState,
        onDismiss = onDismissDeepSyncConfirm,
        onConfirm = {
            settingsViewModel.syncLibraryDetails()
            onDismissDeepSyncConfirm()
        }
    )

    LogoutConfirmDialog(
        visible = showLogoutConfirm,
        activeHazeState = activeHazeState,
        onDismiss = onDismissLogoutConfirm,
        onConfirm = {
            onDismissLogoutConfirm()
            viewModel.logout()
        }
    )
    
    WipeDataSelectionDialog(
        visible = showWipeSelectionDialog,
        activeHazeState = activeHazeState,
        onDismiss = onDismissWipeSelectionDialog,
        onSelectLocal = onSelectWipeLocal,
        onSelectTotal = onSelectWipeTotal
    )

    WipeDataConfirmDialog(
        visible = showWipeLocalDataConfirm,
        title = stringResource(id = R.string.settings_dialog_wipe_local_data_title),
        description = stringResource(id = R.string.settings_dialog_wipe_local_data_desc),
        buttonText = stringResource(id = R.string.settings_yes_wipe_local),
        activeHazeState = activeHazeState,
        onDismiss = onDismissWipeLocalConfirm,
        onConfirm = {
            onDismissWipeLocalConfirm()
            settingsViewModel.wipeLocalData()
        }
    )

    WipeDataConfirmDialog(
        visible = showWipeTotalDataConfirm,
        title = stringResource(id = R.string.settings_dialog_wipe_total_data_title),
        description = stringResource(id = R.string.settings_dialog_wipe_total_data_desc),
        buttonText = stringResource(id = R.string.settings_yes_wipe_total),
        activeHazeState = activeHazeState,
        onDismiss = onDismissWipeTotalConfirm,
        onConfirm = {
            onDismissWipeTotalConfirm()
            settingsViewModel.wipeTotalData()
        }
    )

    CleanupRewatchesConfirmDialog(
        visible = showCleanupRewatchesDialog,
        duplicateCount = duplicateRewatchesCount,
        isLoading = isCleaningUpRewatches,
        activeHazeState = activeHazeState,
        onDismiss = onDismissCleanupRewatchesDialog,
        onConfirm = {
            onStartCleaningUpRewatches()
            settingsViewModel.cleanupRewatches { _ ->
                onFinishCleaningUpRewatches()
                onDismissCleanupRewatchesDialog()
            }
        }
    )

    SettingsColorSelectionDialog(
        visible = showColorDialog,
        activeHazeState = activeHazeState,
        current = accentColorName,
        onDismiss = onDismissColorDialog,
        onSelect = { colorName, origin ->
            onDismissColorDialog()
            scope.launch {
                delay(250)
                settingsViewModel.updateAccentColor(colorName, origin)
            }
        }
    )

    SeasonalIconsPreviewDialog(
        visible = showSeasonalIconsDialog,
        activeHazeState = activeHazeState,
        seasonalAppIconEnabled = seasonalAppIconEnabled,
        onDismiss = onDismissSeasonalIconsDialog
    )

    SettingsLanguageSelectionDialog(
        visible = showLanguageDialog,
        activeHazeState = activeHazeState,
        current = contentLanguage,
        accentColor = currentAccentColor,
        vibrationEnabled = vibrationEnabled,
        onDismiss = onDismissLanguageDialog,
        onSelect = { value ->
            if (contentLanguage != value) {
                settingsViewModel.updateContentLanguage(value) {
                    var actContext = context
                    while (actContext is android.content.ContextWrapper && actContext !is Activity) {
                        actContext = actContext.baseContext
                    }
                    (actContext as? Activity)?.recreate()
                }
            }
            onDismissLanguageDialog()
        }
    )

    SettingsStartScreenSelectionDialog(
        visible = showStartScreenDialog,
        activeHazeState = activeHazeState,
        current = defaultStartTab,
        accentColor = currentAccentColor,
        vibrationEnabled = vibrationEnabled,
        onDismiss = onDismissStartScreenDialog,
        onSelect = { value ->
            if (defaultStartTab != value) {
                settingsViewModel.setDefaultStartTab(value)
            }
            onDismissStartScreenDialog()
        }
    )

    SettingsStartMediaSelectionDialog(
        visible = showStartMediaDialog,
        activeHazeState = activeHazeState,
        current = defaultStartMedia,
        accentColor = currentAccentColor,
        vibrationEnabled = vibrationEnabled,
        onDismiss = onDismissStartMediaDialog,
        onSelect = { value ->
            if (defaultStartMedia != value) {
                settingsViewModel.setDefaultStartMedia(value)
            }
            onDismissStartMediaDialog()
        }
    )

    SettingsFeedbackDialog(
        visible = showFeedbackDialog,
        activeHazeState = activeHazeState,
        initialEmail = "",
        isLoading = isFeedbackLoading,
        onDismiss = { if (!isFeedbackLoading) onDismissFeedbackDialog() },
        onSubmit = { t, d, r, e ->
            settingsViewModel.sendFeedback(t, d, r, e) {
                onDismissFeedbackDialog()
            }
        }
    )

    SettingsBackupDialog(
        visible = showBackupDialog,
        activeHazeState = activeHazeState,
        isBackupLoading = isBackupLoading,
        onDismiss = { if (!isBackupLoading) onDismissBackupDialog() },
        onExport = { 
            onDismissBackupDialog()
            exportLauncher.launch("FlickTrove_Backup_${System.currentTimeMillis()}.json")
        },
        onImport = { 
            onDismissBackupDialog()
            importLauncher.launch(arrayOf("application/json", "application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*"))
        }
    )

    SettingsExternalMigrationDialog(
        visible = showExternalMigrationDialog,
        activeHazeState = activeHazeState,
        onDismiss = onDismissExternalMigrationDialog,
        onImport = {
            onDismissExternalMigrationDialog()
            externalMigrationLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*"))
        }
    )

    UnmatchedItemsModal(
        isVisible = showUnmatchedItemsModal,
        onClose = onDismissUnmatchedItemsModal,
        movies = unmatchedMovies,
        onRemoveItem = { item ->
            settingsViewModel.deleteUnmatchedItem(item)
        }
    )

    BlockedUsersModal(
        isVisible = showBlockedUsersModal,
        onClose = onDismissBlockedUsersModal,
        blockedUsers = blockedUsers,
        onUnblock = { authorId ->
            settingsViewModel.unblockAuthor(authorId)
        },
        hazeState = activeHazeState,
        accentColor = currentAccentColor
    )

    SettingsLoadingOverlay(
        visible = isBackupLoading,
        activeHazeState = activeHazeState
    )
    
    SettingsDashboardSettingsDialog(
        visible = showDashboardSettings,
        activeHazeState = activeHazeState,
        settingsViewModel = settingsViewModel,
        onDismiss = onDismissDashboardSettings
    )

    SettingsHomeSectionsOrderDialog(
        visible = showHomeSectionsDialog,
        activeHazeState = activeHazeState,
        settingsViewModel = settingsViewModel,
        onDismiss = onDismissHomeSectionsDialog
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .haze(
                state = cardHazeState,
                style = HazeStyles.PremiumDark
            )
    ) {
        CardAppearanceDialog(
            visible = showCardAppearanceDialog,
            activeHazeState = activeHazeState,
            settingsViewModel = settingsViewModel,
            onShowBadgesInfo = onOpenBadgesInfoDialog,
            onDismiss = onDismissCardAppearanceDialog
        )
    }

    BadgesInfoDialog(
        visible = showBadgesInfoDialog,
        activeHazeState = if (showCardAppearanceDialog) cardHazeState else activeHazeState,
        disabledBadges = disabledBadges,
        onToggleBadge = { text, enabled -> settingsViewModel.toggleBadgeEnabled(text, enabled) },
        onDismiss = onDismissBadgesInfoDialog
    )
}
