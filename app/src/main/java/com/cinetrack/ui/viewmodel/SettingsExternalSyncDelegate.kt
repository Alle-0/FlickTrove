package com.cinetrack.ui.viewmodel

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.cinetrack.R
import com.cinetrack.data.api.SimklService
import com.cinetrack.data.api.SimklTokenRequestV2
import com.cinetrack.data.api.TraktService
import com.cinetrack.data.api.TraktTokenRequest
import com.cinetrack.data.repository.BadgeRepository
import com.cinetrack.data.repository.SimklAuthRepository
import com.cinetrack.data.repository.TraktAuthRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.UiText
import com.cinetrack.util.Keys
import com.cinetrack.worker.SimklSyncWorker
import com.cinetrack.worker.TraktSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegate responsible for external cloud sync services (Trakt & SIMKL),
 * handling OAuth PKCE/CSRF state, token exchange, and manual sync dispatching.
 */
@Singleton
class SettingsExternalSyncDelegate @Inject constructor(
    private val traktAuthRepository: TraktAuthRepository,
    private val traktService: dagger.Lazy<TraktService>,
    private val simklAuthRepository: SimklAuthRepository,
    private val simklService: dagger.Lazy<SimklService>,
    private val badgeRepository: dagger.Lazy<BadgeRepository>,
    private val actionFeedbackManager: ActionFeedbackManager,
    @ApplicationContext private val context: Context
) {

    val isTraktLoggedIn: StateFlow<Boolean> = traktAuthRepository.isLoggedIn

    /** Emits true when the session was forcibly cleared after a token-refresh failure. */
    val traktNeedsReconnect: StateFlow<Boolean> = traktAuthRepository.needsReconnect

    fun createSimklLoggedInFlow(scope: CoroutineScope): StateFlow<Boolean> =
        simklAuthRepository.isLoggedInFlow.stateIn(scope, SharingStarted.Lazily, false)

    fun createSyncWorkInfoFlow(scope: CoroutineScope): StateFlow<WorkInfo?> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow("TraktManualSync")
            .map { it.firstOrNull() }
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

    fun createSimklSyncWorkInfoFlow(scope: CoroutineScope): StateFlow<WorkInfo?> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow("SimklManualSync")
            .map { it.firstOrNull() }
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

    fun disconnectTrakt(scope: CoroutineScope) {
        traktAuthRepository.clearAuth()
        scope.launch {
            actionFeedbackManager.emit(UiText.DynamicString(context.getString(R.string.trakt_disconnected)))
        }
    }

    fun disconnectSimkl(scope: CoroutineScope) {
        scope.launch {
            simklAuthRepository.clearAuth()
            actionFeedbackManager.emit(UiText.DynamicString(context.getString(R.string.simkl_disconnected)))
        }
    }

    /** Generates and persists the OAuth CSRF state before opening the Trakt browser flow. */
    fun savePendingOAuthState(state: String) {
        traktAuthRepository.savePendingOAuthState(state)
    }

    fun syncTraktNow(scope: CoroutineScope) {
        scope.launch {
            val inputData = workDataOf("force" to true)
            val workRequest = OneTimeWorkRequestBuilder<TraktSyncWorker>()
                .setInputData(inputData)
                .build() // No constraints for forced manual sync
            WorkManager.getInstance(context).enqueueUniqueWork(
                "TraktManualSync",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            actionFeedbackManager.emit(UiText.DynamicString(context.getString(R.string.trakt_manual_sync_started)))
        }
    }

    /**
     * Completes the OAuth code exchange after the browser redirects back to flicktrove://auth.
     */
    fun exchangeTraktCode(code: String, returnedState: String? = null, scope: CoroutineScope) {
        scope.launch {
            // Validate CSRF state if the callback included one
            if (returnedState != null) {
                val expectedState = traktAuthRepository.getPendingOAuthState()
                traktAuthRepository.clearPendingOAuthState()
                if (expectedState == null || expectedState != returnedState) {
                    android.util.Log.e("SettingsExternalSyncDelegate", "Trakt OAuth state mismatch — possible CSRF attack")
                    actionFeedbackManager.emit(UiText.StringResource(R.string.trakt_connect_error))
                    return@launch
                }
            } else {
                // No state in callback — clear any pending state regardless
                traktAuthRepository.clearPendingOAuthState()
            }

            try {
                val response = traktService.get().getAccessToken(
                    TraktTokenRequest(
                        code = code,
                        client_id = Keys.getTraktKey(),
                        client_secret = Keys.getTraktSecret(),
                        redirect_uri = "flicktrove://auth"
                    )
                )
                traktAuthRepository.saveTokens(
                    accessToken = response.access_token,
                    refreshToken = response.refresh_token,
                    expiresInSeconds = response.expires_in
                )
                // Kick off an immediate full sync so the user's history is populated
                // right away without having to wait for the next scheduled run.
                syncTraktNow(scope)
                badgeRepository.get().evaluateBadgesAsync()
                actionFeedbackManager.emit(UiText.StringResource(R.string.trakt_connected))
            } catch (e: Exception) {
                android.util.Log.e("SettingsExternalSyncDelegate", "Trakt token exchange failed", e)
                actionFeedbackManager.emit(UiText.StringResource(R.string.trakt_connect_error))
            }
        }
    }

    fun savePendingSimklOAuth(codeVerifier: String, state: String, scope: CoroutineScope) {
        scope.launch {
            simklAuthRepository.savePendingOAuth(codeVerifier, state)
        }
    }

    fun exchangeSimklCode(code: String, returnedState: String? = null, scope: CoroutineScope) {
        scope.launch {
            val expectedState = simklAuthRepository.getPendingOAuthState()
            val codeVerifier = simklAuthRepository.getPendingCodeVerifier()
            simklAuthRepository.clearPendingOAuth()

            if (expectedState != null && expectedState != returnedState) {
                android.util.Log.e("SettingsExternalSyncDelegate", "Simkl OAuth state mismatch — possible CSRF attack")
                actionFeedbackManager.emit(UiText.StringResource(R.string.simkl_connect_error))
                return@launch
            }

            if (codeVerifier.isNullOrEmpty()) {
                android.util.Log.e("SettingsExternalSyncDelegate", "Simkl PKCE code_verifier missing")
                actionFeedbackManager.emit(UiText.StringResource(R.string.simkl_connect_error))
                return@launch
            }

            try {
                val response = simklService.get().getAccessTokenV2(
                    SimklTokenRequestV2(
                        grant_type = "authorization_code",
                        client_id = Keys.getSimklKey(),
                        code = code,
                        redirect_uri = "flicktrove://simkl_login",
                        code_verifier = codeVerifier
                    )
                )
                simklAuthRepository.saveTokens(
                    accessToken = response.access_token,
                    refreshToken = response.refresh_token,
                    expiresInSeconds = response.expires_in
                )

                // Fetch account profile & user ID immediately
                try {
                    val settings = simklService.get().getUserSettings()
                    val uid = settings.account?.id
                    if (uid != null) {
                        simklAuthRepository.saveUserAccount(uid, settings.account.type)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SettingsExternalSyncDelegate", "Failed to fetch user settings after Simkl login", e)
                }

                syncSimklNow(scope)
                badgeRepository.get().evaluateBadgesAsync()
                actionFeedbackManager.emit(UiText.StringResource(R.string.simkl_connected))
            } catch (e: Exception) {
                android.util.Log.e("SettingsExternalSyncDelegate", "Simkl token exchange failed", e)
                actionFeedbackManager.emit(UiText.StringResource(R.string.simkl_connect_error))
            }
        }
    }

    fun syncSimklNow(scope: CoroutineScope) {
        scope.launch {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SimklSyncWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf("force" to true))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "SimklManualSync",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            actionFeedbackManager.emit(UiText.StringResource(R.string.trakt_manual_sync_started))
        }
    }
}
