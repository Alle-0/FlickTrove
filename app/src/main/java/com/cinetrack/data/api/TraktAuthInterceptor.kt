package com.cinetrack.data.api

import com.cinetrack.data.repository.TraktAuthRepository
import com.cinetrack.utils.Keys
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class TraktAuthInterceptor @Inject constructor(
    private val authRepository: TraktAuthRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        // Always add the Client ID (api key) and version
        requestBuilder.addHeader("trakt-api-key", Keys.getTraktKey())
        requestBuilder.addHeader("trakt-api-version", "2")

        // If we have an access token, add it as Bearer
        val accessToken = authRepository.getAccessToken()
        if (!accessToken.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }

        return chain.proceed(requestBuilder.build())
    }
}
