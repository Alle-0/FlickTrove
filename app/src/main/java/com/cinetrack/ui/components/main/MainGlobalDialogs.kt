package com.cinetrack.ui.components.main

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.cinetrack.BuildConfig
import com.cinetrack.R
import com.cinetrack.ui.components.dialog.GithubUpdateDialog
import com.cinetrack.ui.components.dialog.OnboardingDialog
import com.cinetrack.ui.components.dialog.WhatsNewDialog
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.components.shared.FolderEditDialog
import com.cinetrack.ui.components.shared.FolderEditMode
import com.cinetrack.ui.screens.FolderDetailTab
import com.cinetrack.ui.screens.FoldersTab
import com.cinetrack.ui.screens.MovieDetailScreen
import com.cinetrack.ui.screens.SurpriseMeOverlay
import com.cinetrack.ui.screens.UpdatesScreen
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.FoldersViewModel
import com.cinetrack.ui.viewmodel.SettingsViewModel
import com.cinetrack.ui.viewmodel.SurpriseMeViewModel
import com.cinetrack.ui.viewmodel.UpdatesViewModel
import com.cinetrack.util.AppUpdateInfo
import dev.chrisbanes.haze.HazeState

@Composable
fun MainGlobalDialogs(
    screen: Screen,
    currentTab: Tab,
    tabNavigator: TabNavigator,
    rootNavigator: Navigator,
    activity: ComponentActivity?,
    globalHazeState: HazeState,
    settingsViewModel: SettingsViewModel,
    updatesViewModel: UpdatesViewModel,
    showExitConfirmation: Boolean,
    onExitConfirmationChange: (Boolean) -> Unit,
    showFolderEditDialog: Boolean,
    onFolderEditDialogChange: (Boolean) -> Unit,
    folderEditMode: FolderEditMode,
    showFolderDeleteConfirm: Boolean,
    onFolderDeleteConfirmChange: (Boolean) -> Unit,
    updatesOverlayOffset: Offset?,
    onUpdatesOverlayClose: () -> Unit,
    onOverlayClosing: () -> Unit,
    showSurpriseMeOverlay: Boolean,
    onSurpriseMeClose: () -> Unit,
    updateInfo: AppUpdateInfo?,
    dismissedUpdateVersion: String?,
    ignoredUpdateVersion: String?,
    lastSeenAppVersion: String?,
    hasSeenOnboarding: Boolean
) {
    val context = LocalContext.current

    // Exit Confirmation
    if (showExitConfirmation) {
        FlickTroveModal(
            isVisible = true,
            onDismissRequest = { onExitConfirmationChange(false) },
            hazeState = globalHazeState
        ) {
            Text(
                text = stringResource(R.string.main_exit_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.main_exit_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .bounceClick { onExitConfirmationChange(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.main_exit_cancel), color = Color.White)
                }
                Box(
                    modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .bounceClick { onExitConfirmationChange(false); activity?.finish() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.main_exit_confirm), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showFolderEditDialog && currentTab is FolderDetailTab) {
        val foldersViewModel: FoldersViewModel = with(screen) { getViewModel<FoldersViewModel>() }
        val folderId = currentTab.folderId
        val folderFlow = foldersViewModel.folders.collectAsStateWithLifecycle()
        val folder = folderFlow.value.find { it.id == folderId }
        if (folder != null) {
            FolderEditDialog(
                initialName = folder.name,
                initialColor = folder.color ?: "#FFFFFF",
                editMode = folderEditMode,
                hazeState = globalHazeState,
                onDismiss = { onFolderEditDialogChange(false) },
                onSave = { newName, newColor ->
                    foldersViewModel.updateFolder(folder.copy(name = newName, color = newColor))
                    onFolderEditDialogChange(false)
                    val tab = currentTab as FolderDetailTab
                    tabNavigator.current = FolderDetailTab(
                        folderId = tab.folderId,
                        folderName = if (folderEditMode == FolderEditMode.NAME) newName else tab.folderName,
                        folderColor = if (folderEditMode == FolderEditMode.COLOR) newColor else tab.folderColor
                    )
                }
            )
        }
    }

    if (showFolderDeleteConfirm && currentTab is FolderDetailTab) {
        val foldersViewModel: FoldersViewModel = with(screen) { getViewModel<FoldersViewModel>() }
        val folderId = currentTab.folderId
        val folderFlow = foldersViewModel.folders.collectAsStateWithLifecycle()
        val folder = folderFlow.value.find { it.id == folderId }
        
        if (folder != null) {
            FlickTroveModal(
                isVisible = true,
                onDismissRequest = { onFolderDeleteConfirmChange(false) },
                hazeState = globalHazeState
            ) {
                Text(
                    text = stringResource(R.string.folder_delete_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(R.string.folder_delete_confirm_prefix_2) + "\"${folder.name}\"" + stringResource(R.string.folder_delete_confirm_suffix),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .bounceClick { onFolderDeleteConfirmChange(false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.folder_delete_cancel), color = Color.White)
                    }
                    Box(
                        modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFFF3B30))
                            .bounceClick { 
                                onFolderDeleteConfirmChange(false)
                                foldersViewModel.deleteFolder(folderId)
                                tabNavigator.current = FoldersTab
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.folder_delete_short), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (updatesOverlayOffset != null) {
        UpdatesScreen(
            viewModel = updatesViewModel,
            paddingValues = PaddingValues(bottom = 80.dp),
            startX = updatesOverlayOffset.x,
            startY = updatesOverlayOffset.y,
            onBack = onUpdatesOverlayClose,
            onClosing = onOverlayClosing,
            onMovieClick = { movie ->
                rootNavigator.push(MovieDetailScreen(movie.id, movie.mediaType))
            },
            modifier = Modifier.zIndex(80000f)
        )
    }

    if (showSurpriseMeOverlay) {
        val surpriseMeViewModel: SurpriseMeViewModel = with(screen) { getViewModel<SurpriseMeViewModel>() }
        Box(modifier = Modifier.zIndex(90000f)) {
            SurpriseMeOverlay(
                viewModel = surpriseMeViewModel,
                globalHazeState = globalHazeState,
                onMovieFound = { movie ->
                    onSurpriseMeClose()
                    if (movie != null) {
                        rootNavigator.push(MovieDetailScreen(movie.id, movie.mediaType))
                    } else {
                        Toast.makeText(context, context.getString(R.string.main_surprise_me_not_found), Toast.LENGTH_SHORT).show()
                    }
                },
                onClose = onSurpriseMeClose
            )
        }
    }

    if (updateInfo != null && updateInfo.isUpdateAvailable && dismissedUpdateVersion != updateInfo.latestVersion && ignoredUpdateVersion != updateInfo.latestVersion) {
        GithubUpdateDialog(
            updateInfo = updateInfo,
            hazeState = globalHazeState,
            onDismiss = {
                settingsViewModel.dismissUpdate(updateInfo.latestVersion)
            },
            onIgnoreForever = {
                settingsViewModel.ignoreUpdatePermanently(updateInfo.latestVersion)
            }
        )
    }

    if (hasSeenOnboarding && lastSeenAppVersion != BuildConfig.VERSION_NAME) {
        WhatsNewDialog(
            versionName = BuildConfig.VERSION_NAME,
            accentColor = MaterialTheme.colorScheme.primary,
            releaseNotes = updateInfo?.releaseNotes?.takeIf { it.isNotBlank() },
            hazeState = globalHazeState,
            onDismiss = {
                settingsViewModel.markCurrentAppVersionSeen()
            }
        )
    }

    if (!hasSeenOnboarding) {
        OnboardingDialog(
            accentColor = MaterialTheme.colorScheme.primary,
            hazeState = globalHazeState,
            onDismiss = {
                settingsViewModel.markOnboardingSeen()
            }
        )
    }
}
