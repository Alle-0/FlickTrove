package com.cinetrack.ui.components.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.components.glass.GlassmorphicModal
import dev.chrisbanes.haze.HazeState

@Composable
fun SettingsColorSelectionDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String, Offset) -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        ColorSelectionDialog(
            current = current,
            onDismiss = onDismiss,
            onSelect = onSelect
        )
    }
}

@Composable
fun SettingsFeedbackDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    initialEmail: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Int, String) -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        FeedbackDialog(
            initialEmail = initialEmail,
            isLoading = isLoading,
            onDismiss = onDismiss,
            onSubmit = onSubmit
        )
    }
}

@Composable
fun SettingsBackupDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    isBackupLoading: Boolean,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        BackupDialog(
            isBackupLoading = isBackupLoading,
            onDismiss = onDismiss,
            onExport = onExport,
            onImport = onImport
        )
    }
}

@Composable
fun SettingsExternalMigrationDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        ExternalMigrationDialog(
            onDismiss = onDismiss,
            onImport = onImport
        )
    }
}

@Composable
fun SettingsLanguageSelectionDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    current: String,
    accentColor: Color,
    vibrationEnabled: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        LanguageSelectionDialog(
            current = current,
            accentColor = accentColor,
            vibrationEnabled = vibrationEnabled,
            onDismiss = onDismiss,
            onSelect = onSelect
        )
    }
}

@Composable
fun SettingsStartScreenSelectionDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    current: String,
    accentColor: Color,
    vibrationEnabled: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        StartScreenSelectionDialog(
            current = current,
            accentColor = accentColor,
            vibrationEnabled = vibrationEnabled,
            onDismiss = onDismiss,
            onSelect = onSelect
        )
    }
}

@Composable
fun SettingsStartMediaSelectionDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    current: String,
    accentColor: Color,
    vibrationEnabled: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        onDismissRequest = onDismiss
    ) {
        StartMediaSelectionDialog(
            current = current,
            accentColor = accentColor,
            vibrationEnabled = vibrationEnabled,
            onDismiss = onDismiss,
            onSelect = onSelect
        )
    }
}

@Composable
fun SettingsDashboardSettingsDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    settingsViewModel: com.cinetrack.ui.viewmodel.SettingsViewModel,
    onDismiss: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismiss
    ) {
        DashboardSettingsDialog(
            visible = visible,
            activeHazeState = activeHazeState,
            settingsViewModel = settingsViewModel,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun SettingsHomeSectionsOrderDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    settingsViewModel: com.cinetrack.ui.viewmodel.SettingsViewModel,
    onDismiss: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismiss
    ) {
        HomeSectionsOrderDialog(
            visible = visible,
            activeHazeState = activeHazeState,
            settingsViewModel = settingsViewModel,
            onDismiss = onDismiss
        )
    }
}


@Composable
fun SettingsLoadingOverlay(
    visible: Boolean,
    activeHazeState: HazeState
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = true,
        dismissOnClickOutside = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

