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

    suspend fun saveToken(accessToken: String) {
        dataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
        }
    }

    suspend fun clearAuth() {
        dataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
            preferences.remove(lastSyncTimeKey)
            preferences.remove(isFirstSyncCompletedKey)
        }
    }

    private val lastSyncTimeKey = stringPreferencesKey("simkl_last_sync_time")
    private val isFirstSyncCompletedKey = androidx.datastore.preferences.core.booleanPreferencesKey("simkl_first_sync_completed")

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
