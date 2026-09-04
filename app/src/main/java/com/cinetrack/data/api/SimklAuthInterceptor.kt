package com.cinetrack.data.api

import com.cinetrack.data.repository.SimklAuthRepository
import com.cinetrack.util.Keys
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class SimklAuthInterceptor @Inject constructor(
    private val authRepository: SimklAuthRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        
        // Add query parameters required by SIMKL
        val url = original.url.newBuilder()
            .addQueryParameter("client_id", Keys.getSimklKey())
            .addQueryParameter("app-name", "FlickTrove")
            .addQueryParameter("app-version", com.cinetrack.BuildConfig.VERSION_NAME)
            .build()
            
        val requestBuilder = original.newBuilder().url(url)

        requestBuilder.header("simkl-api-key", Keys.getSimklKey())
        requestBuilder.header("User-Agent", "FlickTrove/${com.cinetrack.BuildConfig.VERSION_NAME}")

        val accessToken = authRepository.getAccessToken()
        if (!accessToken.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        return chain.proceed(requestBuilder.build())
    }
}
