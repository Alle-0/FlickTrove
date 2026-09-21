package com.cinetrack.data.api

import com.cinetrack.data.repository.SimklAuthRepository
import com.cinetrack.util.Keys
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Named

class SimklAuthenticator @Inject constructor(
    private val authRepository: SimklAuthRepository,
    @Named("simkl_refresh_service") private val refreshServiceProvider: dagger.Lazy<SimklService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // If already failed with 401 twice, clear auth and don't retry
        if (response.priorResponse?.code == 401) {
            authRepository.clearAuthOnTokenFailure()
            return null
        }

        val refreshToken = authRepository.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            return null
        }

        return synchronized(this) {
            val currentAccessToken = authRepository.getAccessToken()
            val requestAccessToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            if (currentAccessToken != requestAccessToken && !currentAccessToken.isNullOrEmpty()) {
                return@synchronized response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            try {
                val refreshResponse = runBlocking {
                    val tokens = refreshServiceProvider.get().refreshToken(
                        SimklTokenRequestV2(
                            grant_type = "refresh_token",
                            client_id = Keys.getSimklKey(),
                            refresh_token = refreshToken
                        )
                    )
                    authRepository.saveTokens(
                        accessToken = tokens.access_token,
                        refreshToken = tokens.refresh_token,
                        expiresInSeconds = tokens.expires_in
                    )
                    tokens
                }

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${refreshResponse.access_token}")
                    .build()

            } catch (e: HttpException) {
                if (e.code() == 400 || e.code() == 401) {
                    authRepository.clearAuthOnTokenFailure()
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }
}
