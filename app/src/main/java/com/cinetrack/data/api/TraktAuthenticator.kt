package com.cinetrack.data.api

import com.cinetrack.data.repository.TraktAuthRepository
import com.cinetrack.utils.Keys
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Named

class TraktAuthenticator @Inject constructor(
    private val authRepository: TraktAuthRepository,
    /**
     * Uses the plain (non-authenticated) OkHttpClient so the refresh request itself
     * does NOT go through this authenticator — breaking the infinite loop / deadlock
     * that would occur if the refresh token is rejected with 401.
     */
    @Named("trakt_refresh_service") private val refreshServiceProvider: dagger.Lazy<TraktService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // If the request already failed with 401 twice, don't retry
        if (response.priorResponse?.code == 401) {
            authRepository.clearAuthOnTokenFailure()
            return null
        }

        val refreshToken = authRepository.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            return null
        }

        return synchronized(this) {
            // Check if another thread already refreshed the token while we were waiting
            val currentAccessToken = authRepository.getAccessToken()
            val requestAccessToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            if (currentAccessToken != requestAccessToken && !currentAccessToken.isNullOrEmpty()) {
                return@synchronized response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            try {
                val refreshResponse = runBlocking {
                    // Uses the clean, non-authenticated service — no deadlock risk
                    refreshServiceProvider.get().refreshToken(
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

            } catch (e: HttpException) {
                // 400/401 from Trakt → invalid_grant or token revoked.
                // Signal the UI to show a "please reconnect" warning.
                if (e.code() == 400 || e.code() == 401) {
                    authRepository.clearAuthOnTokenFailure()
                }
                null
            } catch (e: Exception) {
                // Network error, server down, etc. — don't clear auth, just fail this
                // request so the app can retry on the next network call.
                android.util.Log.w("TraktAuthenticator", "Token refresh failed (network): ${e.message}")
                null
            }
        }
    }
}

