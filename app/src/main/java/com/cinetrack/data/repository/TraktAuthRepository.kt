package com.cinetrack.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraktAuthRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "trakt_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _isLoggedIn = MutableStateFlow(getAccessToken() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /**
     * Emits true when auth was cleared due to a token failure (e.g. invalid_grant after
     * Trakt's March-2025 token lifetime change), so the UI can show a targeted
     * "session expired — please reconnect" warning instead of silently disconnecting.
     * Resets to false on successful login or manual disconnect.
     */
    private val _needsReconnect = MutableStateFlow(false)
    val needsReconnect: StateFlow<Boolean> = _needsReconnect.asStateFlow()

    fun saveTokens(accessToken: String, refreshToken: String, expiresInSeconds: Int) {
        val expirationTimeMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRATION_TIME, expirationTimeMillis)
            .apply()
        _isLoggedIn.value = true
        _needsReconnect.value = false // Successful auth clears any pending reconnect warning
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun isTokenExpired(): Boolean {
        val expirationTime = sharedPreferences.getLong(KEY_EXPIRATION_TIME, 0L)
        // Proactively refresh 1 hour before expiry — critical for the 24-hour token lifetime
        // introduced by Trakt in March 2025 (previously tokens lasted 90 days)
        return System.currentTimeMillis() > (expirationTime - 3_600_000L)
    }

    /** Called on explicit user-initiated disconnect. Does NOT set needsReconnect. */
    fun clearAuth() {
        sharedPreferences.edit().clear().apply()
        _isLoggedIn.value = false
        _needsReconnect.value = false
    }

    /**
     * Called by [TraktAuthenticator] when the refresh token is rejected by Trakt
     * (e.g. invalid_grant). Sets [needsReconnect] so the UI can surface a
     * "session expired" warning with a direct reconnect CTA.
     */
    fun clearAuthOnTokenFailure() {
        sharedPreferences.edit().clear().apply()
        _isLoggedIn.value = false
        _needsReconnect.value = true
    }

    /** Dismisses the reconnect warning without touching stored credentials. */
    fun clearNeedsReconnectFlag() {
        _needsReconnect.value = false
    }

    // --- OAuth CSRF state ---

    /** Saves a random state UUID before launching the Trakt OAuth browser flow. */
    fun savePendingOAuthState(state: String) {
        sharedPreferences.edit().putString(KEY_OAUTH_STATE, state).apply()
    }

    fun getPendingOAuthState(): String? {
        return sharedPreferences.getString(KEY_OAUTH_STATE, null)
    }

    fun clearPendingOAuthState() {
        sharedPreferences.edit().remove(KEY_OAUTH_STATE).apply()
    }

    // --- Last-activities timestamp ---

    fun getLastActivitiesTime(): String? {
        return sharedPreferences.getString(KEY_LAST_ACTIVITIES, null)
    }

    fun saveLastActivitiesTime(timeIso: String) {
        sharedPreferences.edit().putString(KEY_LAST_ACTIVITIES, timeIso).apply()
    }

    fun getLastRatingsTime(): String? {
        return sharedPreferences.getString(KEY_LAST_RATINGS, null)
    }

    fun saveLastRatingsTime(timeIso: String) {
        sharedPreferences.edit().putString(KEY_LAST_RATINGS, timeIso).apply()
    }

    fun getLastWatchlistTime(): String? {
        return sharedPreferences.getString(KEY_LAST_WATCHLIST, null)
    }

    fun saveLastWatchlistTime(timeIso: String) {
        sharedPreferences.edit().putString(KEY_LAST_WATCHLIST, timeIso).apply()
    }

    fun isFirstSyncCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_SYNC_COMPLETED, false)
    }

    fun markFirstSyncCompleted() {
        sharedPreferences.edit().putBoolean(KEY_FIRST_SYNC_COMPLETED, true).apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRATION_TIME = "expiration_time"
        private const val KEY_LAST_ACTIVITIES = "last_activities_time"
        private const val KEY_LAST_RATINGS = "last_ratings_time"
        private const val KEY_LAST_WATCHLIST = "last_watchlist_time"
        private const val KEY_OAUTH_STATE = "oauth_pending_state"
        private const val KEY_FIRST_SYNC_COMPLETED = "first_sync_completed"
    }
}
