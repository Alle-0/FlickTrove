package com.cinetrack.data.api

import com.cinetrack.data.repository.TraktAuthRepository
import com.cinetrack.utils.Keys
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TraktAuthenticator @Inject constructor(
    private val authRepository: TraktAuthRepository,
    private val traktServiceProvider: dagger.Lazy<TraktService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // If the request already failed with 401 twice, give up to avoid infinite loops
        if (response.priorResponse?.code == 401) {
            authRepository.clearAuth()
            return null
        }

        val refreshToken = authRepository.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            return null
        }

        return synchronized(this) {
            // Check if another thread already refreshed the token
            val newAccessToken = authRepository.getAccessToken()
            val oldAccessToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            
            if (newAccessToken != oldAccessToken && !newAccessToken.isNullOrEmpty()) {
                return@synchronized response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            }

            try {
                // Perform synchronous refresh
                val refreshResponse = runBlocking {
                    traktServiceProvider.get().refreshToken(
                        TraktRefreshTokenRequest(
                            refresh_token = refreshToken,
                            client_id = Keys.getTraktKey(),
                            client_secret = Keys.getTraktSecret(),
                            redirect_uri = "flicktrove://auth"
                        )
                    )
                }

                authRepository.saveTokens(
                    accessToken = refreshResponse.access_token,
                    refreshToken = refreshResponse.refresh_token,
                    expiresInSeconds = refreshResponse.expires_in
                )

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${refreshResponse.access_token}")
                    .build()
            } catch (e: Exception) {
                authRepository.clearAuth()
                null
            }
        }
    }
}
