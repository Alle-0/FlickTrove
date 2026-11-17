package com.cinetrack.data.repository

import com.cinetrack.data.api.SemanticSearchApi
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.util.TranslationManager
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SemanticSearchRepository @Inject constructor(
    private val api: SemanticSearchApi,
    private val localEngine: LocalMoodSearchEngine,
    private val translationManager: TranslationManager
) {

    suspend fun search(
        query: String,
        language: String = java.util.Locale.getDefault().toLanguageTag()
    ): List<TMDBSearchResult.MovieResult> {
        val sanitizedQuery = query.trim().take(500)
        if (sanitizedQuery.isBlank()) return emptyList()

        val langCode = language.take(2).lowercase()

        // 0. Query Expansion Zero-Neurons: se non è inglese, traduciamo/arricchiamo a costo zero dal device
        val queryEn = if (langCode != "en") {
            try {
                withTimeoutOrNull(1000L) {
                    translationManager.translateOnline(sanitizedQuery, "auto", "en")
                }
            } catch (_: Exception) {
                null
            }
        } else null

        // 1. Tentativo veloce su Cloudflare Vectorize con timeout a 2.5 secondi
        val cloudResults = try {
            withTimeoutOrNull(2500L) {
                val response = api.searchSemantics(
                    query = sanitizedQuery,
                    lang = langCode,
                    queryEn = queryEn
                )
                if (response.isSuccessful) {
                    response.body()?.results?.map { it.toMovieResult() }
                } else null
            }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }

        if (!cloudResults.isNullOrEmpty()) {
            return cloudResults
        }

        // 2. Fallback Invisibile: LocalMoodSearchEngine in RAM (Zero-Crash & Offline-First)
        val localResult = localEngine.searchLocalMood(
            query = sanitizedQuery,
            language = language,
            page = 1,
            isQuotaExhaustedFallback = false
        )
        return localResult.movies
    }
}
