package com.cinetrack.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

@Singleton
class SimklAuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val accessTokenKey = stringPreferencesKey("simkl_access_token")
    private val refreshTokenKey = stringPreferencesKey("simkl_refresh_token")
    private val expiresAtKey = androidx.datastore.preferences.core.longPreferencesKey("simkl_token_expires_at")
    private val pendingCodeVerifierKey = stringPreferencesKey("simkl_pending_code_verifier")
    private val pendingOAuthStateKey = stringPreferencesKey("simkl_pending_oauth_state")
    private val userIdKey = androidx.datastore.preferences.core.longPreferencesKey("simkl_user_id")
    private val userTypeKey = stringPreferencesKey("simkl_user_type")
    private val lastSyncTimeKey = stringPreferencesKey("simkl_last_sync_time")
    private val isFirstSyncCompletedKey = androidx.datastore.preferences.core.booleanPreferencesKey("simkl_first_sync_completed")

    val accessTokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[accessTokenKey]
    }

    val isLoggedInFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[accessTokenKey] != null
    }

    fun getAccessToken(): String? {
        return runBlocking {
            dataStore.data.first()[accessTokenKey]
        }
    }

    fun getRefreshToken(): String? {
        return runBlocking {
            dataStore.data.first()[refreshTokenKey]
        }
    }

    suspend fun saveToken(accessToken: String) {
        dataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
        }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long?) {
        dataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
            if (refreshToken != null) {
                preferences[refreshTokenKey] = refreshToken
            }
            if (expiresInSeconds != null) {
                preferences[expiresAtKey] = System.currentTimeMillis() + (expiresInSeconds * 1000)
            }
        }
    }

    suspend fun savePendingOAuth(codeVerifier: String, state: String) {
        dataStore.edit { preferences ->
            preferences[pendingCodeVerifierKey] = codeVerifier
            preferences[pendingOAuthStateKey] = state
        }
    }

    fun getPendingCodeVerifier(): String? {
        return runBlocking {
            dataStore.data.first()[pendingCodeVerifierKey]
        }
    }

    fun getPendingOAuthState(): String? {
        return runBlocking {
            dataStore.data.first()[pendingOAuthStateKey]
        }
    }

    suspend fun clearPendingOAuth() {
        dataStore.edit { preferences ->
            preferences.remove(pendingCodeVerifierKey)
            preferences.remove(pendingOAuthStateKey)
        }
    }

    suspend fun saveUserAccount(userId: Long, userType: String?) {
        dataStore.edit { preferences ->
            preferences[userIdKey] = userId
            if (userType != null) {
                preferences[userTypeKey] = userType
            }
        }
    }

    fun getUserId(): Long? {
        return runBlocking {
            dataStore.data.first()[userIdKey]
        }
    }

    fun getUserType(): String? {
        return runBlocking {
            dataStore.data.first()[userTypeKey]
        }
    }

    suspend fun clearAuth() {
        dataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
            preferences.remove(refreshTokenKey)
            preferences.remove(expiresAtKey)
            preferences.remove(pendingCodeVerifierKey)
            preferences.remove(pendingOAuthStateKey)
            preferences.remove(userIdKey)
            preferences.remove(userTypeKey)
            preferences.remove(lastSyncTimeKey)
            preferences.remove(isFirstSyncCompletedKey)
        }
    }

    fun clearAuthOnTokenFailure() {
        runBlocking {
            clearAuth()
        }
    }

    fun getLastSyncTime(): String? {
        return runBlocking {
            dataStore.data.first()[lastSyncTimeKey]
        }
    }

    suspend fun saveLastSyncTime(time: String) {
        dataStore.edit { preferences ->
            preferences[lastSyncTimeKey] = time
        }
    }

    fun isFirstSyncCompleted(): Boolean {
        return runBlocking {
            dataStore.data.first()[isFirstSyncCompletedKey] ?: false
        }
    }

    suspend fun setFirstSyncCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[isFirstSyncCompletedKey] = completed
        }
    }
}
