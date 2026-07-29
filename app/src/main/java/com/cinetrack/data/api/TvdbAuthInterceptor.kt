package com.cinetrack.data.api

import com.cinetrack.data.repository.PreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class TvdbAuthInterceptor @Inject constructor(
    private val preferenceRepository: PreferenceRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip adding the token for the login endpoint
        if (originalRequest.url.encodedPath.contains("login")) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking { preferenceRepository.userPreferencesFlow.first().tvdbJwtToken }

        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}
