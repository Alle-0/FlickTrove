package com.cinetrack.data.api

import com.cinetrack.BuildConfig
import com.cinetrack.data.repository.PreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import javax.inject.Inject

class TvdbAuthenticator @Inject constructor(
    private val preferenceRepository: PreferenceRepository
) : Authenticator {

    // Lazy initialization to avoid circular dependency if we used TvdbApi directly
    private val authApi: TvdbApi by lazy {
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl("https://api4.thetvdb.com/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TvdbApi::class.java)
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // If the request itself was a login request and it failed with 401, give up
        if (response.request.url.encodedPath.contains("login")) {
            return null
        }

        synchronized(this) {
            // Get current token
            val currentToken = runBlocking { preferenceRepository.userPreferencesFlow.first().tvdbJwtToken }

            // If the token used in the failed request is NOT the current token, 
            // another thread might have already refreshed it.
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (requestToken != null && requestToken != currentToken && currentToken.isNotEmpty()) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // Refresh the token
            return try {
                val loginResponse = runBlocking {
                    authApi.login(TvdbLoginRequest(apikey = BuildConfig.TVDB_API_KEY))
                }
                
                val newToken = loginResponse.data?.token
                if (!newToken.isNullOrEmpty()) {
                    runBlocking {
                        preferenceRepository.updateTvdbJwtToken(newToken)
                    }
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
