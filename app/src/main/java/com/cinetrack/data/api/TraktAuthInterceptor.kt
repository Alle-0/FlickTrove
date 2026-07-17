package com.cinetrack.data.api

import com.cinetrack.data.repository.TraktAuthRepository
import com.cinetrack.util.Keys
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class TraktAuthInterceptor @Inject constructor(
    private val authRepository: TraktAuthRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        // Always set the Client ID (api key) and version (use header instead of addHeader to prevent duplicates)
        requestBuilder.header("trakt-api-key", Keys.getTraktKey())
        requestBuilder.header("trakt-api-version", "2")

        // If we have an access token, add it as Bearer
        val accessToken = authRepository.getAccessToken()
        if (!accessToken.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        return chain.proceed(requestBuilder.build())
    }
}
