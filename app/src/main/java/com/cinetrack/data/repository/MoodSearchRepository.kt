package com.cinetrack.data.repository

import android.content.Context
import com.cinetrack.R
import com.cinetrack.data.api.SupabaseMoodSearchApi
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.model.MoodHistoryEntry
import com.cinetrack.data.model.MoodSearchRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
    val movies: List<TMDBSearchResult.MovieResult>,
    val isQuotaFallback: Boolean = false,
    val isNetworkFallback: Boolean = false
)

class MoodQuotaExhaustedException(
    message: String = "Daily Mood search quota exhausted. Resets at midnight!"
) : Exception(message)

class MoodRateLimitException(
    message: String = "Servers are temporarily busy. Please try again shortly."
) : Exception(message)

@Singleton
class MoodSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: SupabaseMoodSearchApi,
    private val localEngine: LocalMoodSearchEngine,
    private val semanticSearchRepository: SemanticSearchRepository,
    private val preferenceRepository: PreferenceRepository,
    private val moodHistoryRepository: MoodHistoryRepository,
    private val badgeRepository: dagger.Lazy<BadgeRepository>
) {

    val remainingQuotaFlow: Flow<Int> = preferenceRepository.moodSearchesRemainingFlow

    suspend fun getRemainingQuota(): Int = preferenceRepository.getMoodSearchesRemaining()

    suspend fun refreshRemoteQuota(): Int {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user == null || user.isAnonymous) return 0
        return try {
            val response = api.getMoodQuota()
            if (response.isSuccessful) {
                val rem = response.body()?.remaining ?: 3
                preferenceRepository.setMoodSearchesRemaining(rem)
                rem
            } else if (response.code() == 429) {
                preferenceRepository.setMoodSearchesRemaining(0)
                0
            } else {
                preferenceRepository.getMoodSearchesRemaining()
            }
        } catch (_: Exception) {
            preferenceRepository.getMoodSearchesRemaining()
        }
    }

    fun isLocalEligible(language: String): Boolean = true

    suspend fun searchMoviesByMood(
        query: String,
        language: String = java.util.Locale.getDefault().toLanguageTag(),
        page: Int = 1,
        forceLocal: Boolean = false,
        bypassCache: Boolean = false
    ): Result<MoodSearchResultData> = runCatching {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user == null || user.isAnonymous) {
            throw Exception(context.getString(R.string.mood_guest_dialog_message))
        }
        val trimmedQuery = query.trim().take(1000)
        val remaining = preferenceRepository.getMoodSearchesRemaining()
        val wantAi = !forceLocal && remaining > 0

        // 0. Cache Hit Check: Ricorda le ricerche fatte per almeno 24 ore (Zero network, zero consumo quote)
        // Se non è bypassCache ed è recente (< 24h), riusa la cache per la modalità selezionata (AI o ALGO)
        if (page == 1) {
            val cached = moodHistoryRepository.getCachedEntry(trimmedQuery, isAi = wantAi)
            if (cached != null) {
                val cachedMovies = if (wantAi) cached.resolveAiResults() else cached.resolveAlgoResults()
                if (cachedMovies.isNotEmpty()) {
                    val ageMillis = System.currentTimeMillis() - cached.timestamp
                    val isUnder24Hours = ageMillis < 24 * 60 * 60 * 1000L
                    val shouldReturnCache = !bypassCache && isUnder24Hours
                    if (shouldReturnCache) {
                        badgeRepository.get().recordMoodMoviesDiscovered(cachedMovies.map { it.id })
                        return@runCatching MoodSearchResultData(
                            vibeTitle = if (wantAi) cached.resolveAiVibeTitle() else cached.resolveAlgoVibeTitle(),
                            vibeDescription = if (wantAi) cached.resolveAiVibeDescription() else cached.resolveAlgoVibeDescription(),
                            totalCount = cachedMovies.size,
                            totalPages = 1,
                            movies = cachedMovies
                        )
                    }
                }
            }
        }

        // 1. Modalità ALGO (esplicita o per esaurimento quota AI):
        // Pipeline: Cloudflare Vectorize (online) -> Fallback su Local Engine (offline)
        if (forceLocal || remaining <= 0) {
            val wasQuotaFallback = !forceLocal && remaining <= 0
            return@runCatching performAlgoSearch(
                trimmedQuery = trimmedQuery,
                language = language,
                page = page,
                isQuotaFallback = wasQuotaFallback
            )
        }

        // 2. Modalità AI PURA: Invocazione diretta a Supabase Edge Function (Gemini LLM)
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
                preferenceRepository.setMoodSearchesRemaining(0)
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
                badgeRepository.get().recordMoodMoviesDiscovered(mappedMovies.map { it.id })
                body.remainingQuota?.let { rem ->
                    preferenceRepository.setMoodSearchesRemaining(rem)
                } ?: run {
                    preferenceRepository.consumeMoodSearchQuota()
                }
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
        } catch (_: Exception) {
            // 3. SMART HYBRID FALLBACK: se l'AI Cloud fallisce (429 rate limit, timeout, offline, 500, 502),
            // degrada trasparentemente sull'algoritmo (Cloudflare Vectorize -> Local Engine)
            performAlgoSearch(
                trimmedQuery = trimmedQuery,
                language = language,
                page = page,
                isQuotaFallback = false,
                isNetworkFallback = true
            )
        }
    }

    /**
     * Pipeline di Fallback e Algoritmo (ALGO / Cloudflare Vectorize):
     * 1. Livello 1: Cloudflare Vectorize (algoritmo semantico vettoriale bge-m3, zero consumo quote).
     * 2. Livello 2: Se Cloudflare è offline, in errore o vuoto, scivola sul Local Engine (BM25 + TMDB offline sul dispositivo).
     */
    private suspend fun performAlgoSearch(
        trimmedQuery: String,
        language: String,
        page: Int,
        isQuotaFallback: Boolean,
        isNetworkFallback: Boolean = false
    ): MoodSearchResultData {
        // A. Tentativo 1: Algoritmo Vettoriale Cloudflare Vectorize (Zero Quota, Online)
        if (page == 1) {
            try {
                val semanticMovies = semanticSearchRepository.search(query = trimmedQuery, language = language)
                if (semanticMovies.isNotEmpty()) {
                    badgeRepository.get().recordMoodMoviesDiscovered(semanticMovies.map { it.id })
                    val matchedArchetype = localEngine.detectArchetype(trimmedQuery, language)
                    val isItalian = language.lowercase().startsWith("it")
                    val vibeTitle = matchedArchetype?.let { if (isItalian) it.nameIt else it.nameEn }
                        ?: (if (isQuotaFallback || isNetworkFallback) "Ricerca Semantica Vettoriale" else "Algoritmo Vettoriale")
                    val vibeDesc = matchedArchetype?.let { if (isItalian) it.rationaleTemplateIt else it.rationaleTemplateEn }
                        ?: "Risultati calcolati nello spazio geometrico bge-m3 su Cloudflare Vectorize."
                    moodHistoryRepository.saveEntry(
                        MoodHistoryEntry(
                            query = trimmedQuery,
                            vibeTitle = vibeTitle,
                            vibeDescription = vibeDesc,
                            isAi = isQuotaFallback,
                            movies = semanticMovies
                        )
                    )

                    // Fire-and-forget: registra la ricerca IRIS (Cloudflare Vectorize) su Supabase in background
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val anonKey = com.cinetrack.util.Keys.getSupabaseAnonKey()
                            if (anonKey.isNotBlank()) {
                                val sample = semanticMovies.take(5).map { m ->
                                    com.cinetrack.data.model.SearchLogMovieSampleDto(
                                        title = m.title.orEmpty(),
                                        year = m.releaseDate?.take(4)
                                    )
                                }
                                api.recordSearchLog(
                                    apiKey = anonKey,
                                    authorization = "Bearer $anonKey",
                                    payload = com.cinetrack.data.model.SearchLogDto(
                                        query = trimmedQuery,
                                        engine = "IRIS",
                                        tier = "PRO",
                                        language = language,
                                        resultsCount = semanticMovies.size,
                                        resultsSample = sample,
                                        vibeTitle = vibeTitle
                                    )
                                )
                            }
                        } catch (_: Exception) {
                            // Silente: il fallimento del logging analitico non deve mai impattare l'utente
                        }
                    }

                    return MoodSearchResultData(
                        vibeTitle = vibeTitle,
                        vibeDescription = vibeDesc,
                        totalCount = semanticMovies.size,
                        totalPages = 1,
                        movies = semanticMovies,
                        isQuotaFallback = isQuotaFallback,
                        isNetworkFallback = isNetworkFallback
                    )
                }
            } catch (_: Exception) {
                // Fallthrough silente verso l'algoritmo locale offline
            }
        }

        // B. Tentativo 2: Algoritmo Locale Offline (LocalMoodSearchEngine, BM25 + TMDB sul device)
        val localResult = localEngine.searchLocalMood(
            query = trimmedQuery,
            language = language,
            page = page,
            isQuotaExhaustedFallback = isQuotaFallback
        )
        if (localResult.movies.isNotEmpty()) {
            badgeRepository.get().recordMoodMoviesDiscovered(localResult.movies.map { it.id })
            moodHistoryRepository.saveEntry(
                MoodHistoryEntry(
                    query = trimmedQuery,
                    vibeTitle = localResult.vibeTitle,
                    vibeDescription = localResult.vibeDescription,
                    isAi = isQuotaFallback,
                    movies = localResult.movies
                )
            )

            // Fire-and-forget: registra la ricerca IRIS MID (motore locale) su Supabase in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val anonKey = com.cinetrack.util.Keys.getSupabaseAnonKey()
                    if (anonKey.isNotBlank()) {
                        val sample = localResult.movies.take(5).map { m ->
                            com.cinetrack.data.model.SearchLogMovieSampleDto(
                                title = m.title.orEmpty(),
                                year = m.releaseDate?.take(4)
                            )
                        }
                        api.recordSearchLog(
                            apiKey = anonKey,
                            authorization = "Bearer $anonKey",
                            payload = com.cinetrack.data.model.SearchLogDto(
                                query = trimmedQuery,
                                engine = "IRIS",
                                tier = "MID",
                                language = language,
                                resultsCount = localResult.movies.size,
                                resultsSample = sample,
                                vibeTitle = localResult.vibeTitle.ifBlank { "Algoritmo Locale" }
                            )
                        )
                    }
                } catch (_: Exception) {
                    // Silente: il fallimento del logging analitico non deve mai impattare l'utente
                }
            }
        }
        return localResult.copy(
            isQuotaFallback = isQuotaFallback,
            isNetworkFallback = isNetworkFallback
        )
    }
}
