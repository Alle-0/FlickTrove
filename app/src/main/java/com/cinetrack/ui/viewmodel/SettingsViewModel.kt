package com.cinetrack.ui.viewmodel

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.cinetrack.data.repository.BackupRepository
import com.cinetrack.data.repository.FeedbackRepository
import com.cinetrack.data.repository.SettingsRepository
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
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val feedbackRepository: FeedbackRepository,
    private val backupRepository: BackupRepository,
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

    fun setAnyDialogOpen(isOpen: Boolean) {
        _isAnyDialogOpen.value = isOpen
    }

    val accentColor: StateFlow<String> = settingsRepository.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Teal")

    val showFolderBookmarks: StateFlow<Boolean> = settingsRepository.showFolderBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrationEnabled: StateFlow<Boolean> = settingsRepository.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun updateAccentColor(color: String, revealOrigin: Offset? = null) {
        viewModelScope.launch {
            if (revealOrigin != null) {
                // Publish the pending reveal BEFORE persisting so the overlay
                // can capture the screenshot of the old theme.
                _pendingReveal.value = Pair(color, revealOrigin)
            } else {
                settingsRepository.updateAccentColor(color)
                actionFeedbackManager.emit("Colore accento aggiornato")
            }
        }
    }

    fun applyPendingTheme() {
        viewModelScope.launch {
            _pendingReveal.value?.let { (color, _) ->
                settingsRepository.updateAccentColor(color)
                actionFeedbackManager.emit("Colore accento aggiornato")
            }
        }
    }

    fun toggleFolderBookmarks(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleFolderBookmarks(enabled)
            val status = if (enabled) "visibili" else "nascosti"
            actionFeedbackManager.emit("Segnalibri cartelle $status")
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleNotifications(enabled)
            val status = if (enabled) "attivate" else "disattivate"
            actionFeedbackManager.emit("Notifiche $status")
        }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleVibration(enabled)
            val status = if (enabled) "attiva" else "disattiva"
            actionFeedbackManager.emit("Vibrazione $status")
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearImageCache() {
        viewModelScope.launch {
            context.imageLoader.diskCache?.clear()
            context.imageLoader.memoryCache?.clear()
            actionFeedbackManager.emit("Cache immagini svuotata")
        }
    }

    fun sendFeedback(title: String, description: String, rating: Int, email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val lastTime = settingsRepository.lastFeedbackTimestamp.first()
            val currentTime = System.currentTimeMillis()
            val twelveHoursInMillis = 12 * 60 * 60 * 1000L
            
            if (currentTime - lastTime < twelveHoursInMillis) {
                val hoursRemaining = ((twelveHoursInMillis - (currentTime - lastTime)) / (1000 * 60 * 60)).toInt()
                actionFeedbackManager.emit("Puoi inviare un altro feedback tra circa $hoursRemaining ore")
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
                    actionFeedbackManager.emit("Grazie per il tuo feedback!")
                    onSuccess()
                } else {
                    actionFeedbackManager.emit("Errore nell'invio del feedback. Riprova più tardi.")
                }
            } catch (e: Exception) {
                actionFeedbackManager.emit("Errore di connessione durante l'invio")
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
            actionFeedbackManager.emit("Errore durante l'esportazione")
            null
        } finally {
            _isBackupLoading.value = false
        }
    }

    fun restoreData(json: String) {
        viewModelScope.launch {
            _isBackupLoading.value = true
            try {
                backupRepository.importData(json)
                actionFeedbackManager.emit("Dati ripristinati con successo")
            } catch (e: Exception) {
                actionFeedbackManager.emit("Errore durante il ripristino: file non valido")
            } finally {
                _isBackupLoading.value = false
            }
        }
    }

    fun migrateFromTrakt(json: String) {
        viewModelScope.launch {
            _isBackupLoading.value = true
            try {
                val count = backupRepository.migrateTrakt(json)
                actionFeedbackManager.emit("Importati $count elementi da Trakt")
            } catch (e: Exception) {
                actionFeedbackManager.emit("Errore durante la migrazione: verifica il formato")
            } finally {
                _isBackupLoading.value = false
            }
        }
    }
}
