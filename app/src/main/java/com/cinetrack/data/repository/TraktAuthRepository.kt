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

    fun saveTokens(accessToken: String, refreshToken: String, expiresInSeconds: Int) {
        val expirationTimeMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRATION_TIME, expirationTimeMillis)
            .apply()
        _isLoggedIn.value = true
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun isTokenExpired(): Boolean {
        val expirationTime = sharedPreferences.getLong(KEY_EXPIRATION_TIME, 0L)
        // Add a 5 minute buffer to avoid edge cases
        return System.currentTimeMillis() > (expirationTime - 300_000L)
    }

    fun clearAuth() {
        sharedPreferences.edit().clear().apply()
        _isLoggedIn.value = false
    }

    fun getLastActivitiesTime(): String? {
        return sharedPreferences.getString(KEY_LAST_ACTIVITIES, null)
    }

    fun saveLastActivitiesTime(timeIso: String) {
        sharedPreferences.edit().putString(KEY_LAST_ACTIVITIES, timeIso).apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRATION_TIME = "expiration_time"
        private const val KEY_LAST_ACTIVITIES = "last_activities_time"
    }
}
