package com.cinetrack.data.repository

import android.content.Context
import com.cinetrack.R
import com.cinetrack.data.api.SupabaseMoodSearchApi
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.model.MoodHistoryEntry
import com.cinetrack.data.model.MoodSearchRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

data class MoodSearchResultData(
    val vibeTitle: String,
    val vibeDescription: String,
    val totalCount: Int,
    val totalPages: Int,
    val movies: List<TMDBSearchResult.MovieResult>
)

class MoodQuotaExhaustedException(
    message: String = "Hai raggiunto il limite di 5 ricerche Mood per oggi. Si ricaricheranno a mezzanotte!"
) : Exception(message)

class MoodRateLimitException(
    message: String = "I server sono momentaneamente affollati. Riprova tra un minuto."
) : Exception(message)

@Singleton
class MoodSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: SupabaseMoodSearchApi,
    private val localEngine: LocalMoodSearchEngine,
    private val preferenceRepository: PreferenceRepository,
    private val moodHistoryRepository: MoodHistoryRepository
) {

    val remainingQuotaFlow: Flow<Int> = preferenceRepository.moodSearchesRemainingFlow

    suspend fun getRemainingQuota(): Int = preferenceRepository.getMoodSearchesRemaining()

    fun isLocalEligible(language: String): Boolean = localEngine.isSupportedLanguage(language)

    suspend fun searchMoviesByMood(
        query: String,
        language: String = java.util.Locale.getDefault().toLanguageTag(),
        page: Int = 1,
        forceLocal: Boolean = false,
        bypassCache: Boolean = false
    ): Result<MoodSearchResultData> = runCatching {
        val trimmedQuery = query.trim()
        val localEligible = isLocalEligible(language)
        val remaining = preferenceRepository.getMoodSearchesRemaining()
        val isRequestedAi = !forceLocal && remaining > 0

        // 0. Cache Hit Check: If this exact query was already searched and cached FOR THIS ENGINE, return it directly!
        // (Zero network, zero AI token consumption, 100% offline-ready)
        if (page == 1 && !bypassCache) {
            val cached = moodHistoryRepository.getCachedEntry(trimmedQuery, isAi = isRequestedAi)
            if (cached != null && cached.movies.isNotEmpty()) {
                return@runCatching MoodSearchResultData(
                    vibeTitle = cached.vibeTitle,
                    vibeDescription = cached.vibeDescription,
                    totalCount = cached.movies.size,
                    totalPages = 1,
                    movies = cached.movies
                )
            }
        }

        // 1. If user explicitly chose Local Engine or if AI quota is exhausted (for IT/EN)
        if (forceLocal || remaining <= 0) {
            if (localEligible) {
                val localResult = localEngine.searchLocalMood(
                    query = trimmedQuery,
                    language = language,
                    page = page,
                    isQuotaExhaustedFallback = !forceLocal && remaining <= 0
                )
                if (localResult.movies.isNotEmpty()) {
                    moodHistoryRepository.saveEntry(
                        MoodHistoryEntry(
                            query = trimmedQuery,
                            vibeTitle = localResult.vibeTitle,
                            vibeDescription = localResult.vibeDescription,
                            isAi = false,
                            movies = localResult.movies
                        )
                    )
                }
                return@runCatching localResult
            } else {
                throw MoodQuotaExhaustedException()
            }
        }

        // 2. Network invocation to Supabase Edge Function (Gemini 3.8 Flash) with Smart Fallback on error
        try {
            val response = try {
                api.searchMoviesByMood(
                    MoodSearchRequest(query = trimmedQuery, language = language, page = page)
                )
            } catch (e: SocketTimeoutException) {
                throw Exception(context.getString(R.string.search_error_message))
            } catch (e: UnknownHostException) {
                throw Exception(context.getString(R.string.search_error_message))
            } catch (e: IOException) {
                throw Exception(context.getString(R.string.search_error_message))
            }

            if (response.code() == 429) {
                throw MoodRateLimitException()
            }

            if (!response.isSuccessful) {
                val errBody = response.errorBody()?.string()
                val parsedMsg = errBody?.let {
                    try {
                        val json = org.json.JSONObject(it)
                        if (json.has("message")) json.getString("message") else null
                    } catch (_: Exception) {
                        null
                    }
                }
                throw Exception(parsedMsg ?: context.getString(R.string.search_error_message))
            }

            val body = response.body() ?: throw IllegalStateException("Risposta vuota dal server")
            if (!body.success) {
                if (body.code == "RATE_LIMIT_EXCEEDED") {
                    throw MoodRateLimitException(body.message ?: "Server momentaneamente occupati.")
                }
                throw Exception(body.message ?: "Ricerca non riuscita.")
            }

            val mappedMovies = body.results.map { it.toTMDBSearchResult() }

            val finalVibeTitle = body.vibeTitle?.trim()?.ifBlank { "Atmosfera Cinematografica" } ?: "Atmosfera Cinematografica"
            val finalVibeDesc = body.vibeDescription?.trim()?.ifBlank { "" } ?: ""

            // Quota consumption & Caching: ONLY consume quota and cache if the search succeeded and produced results
            if (mappedMovies.isNotEmpty()) {
                preferenceRepository.consumeMoodSearchQuota()
                moodHistoryRepository.saveEntry(
                    MoodHistoryEntry(
                        query = trimmedQuery,
                        vibeTitle = finalVibeTitle,
                        vibeDescription = finalVibeDesc,
                        isAi = true,
                        movies = mappedMovies
                    )
                )
            }

            MoodSearchResultData(
                vibeTitle = finalVibeTitle,
                vibeDescription = finalVibeDesc,
                totalCount = body.count,
                totalPages = body.totalPages,
                movies = mappedMovies
            )
        } catch (cloudException: Exception) {
            // 3. SMART HYBRID FALLBACK: If Cloud AI failed (401, timeout, offline, 500) and language is IT/EN,
            // fallback immediately to the local algorithm so the user never encounters a broken experience!
            if (localEligible) {
                val fallbackResult = localEngine.searchLocalMood(
                    query = trimmedQuery,
                    language = language,
                    page = page,
                    isQuotaExhaustedFallback = false
                )
                if (fallbackResult.movies.isNotEmpty()) {
                    moodHistoryRepository.saveEntry(
                        MoodHistoryEntry(
                            query = trimmedQuery,
                            vibeTitle = fallbackResult.vibeTitle,
                            vibeDescription = fallbackResult.vibeDescription,
                            isAi = false,
                            movies = fallbackResult.movies
                        )
                    )
                }
                fallbackResult
            } else {
                throw cloudException
            }
        }
    }
}
