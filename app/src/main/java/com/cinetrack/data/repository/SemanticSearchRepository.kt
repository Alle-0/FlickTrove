package com.cinetrack.data.repository

import com.cinetrack.data.api.SemanticSearchApi
import com.cinetrack.data.api.TMDBSearchResult
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SemanticSearchRepository @Inject constructor(
    private val api: SemanticSearchApi,
    private val localEngine: LocalMoodSearchEngine
) {

    suspend fun search(
        query: String,
        language: String = java.util.Locale.getDefault().toLanguageTag()
    ): List<TMDBSearchResult.MovieResult> {
        val sanitizedQuery = query.trim().take(500)
        if (sanitizedQuery.isBlank()) return emptyList()

        // 1. Tentativo veloce su Cloudflare Vectorize con timeout a 2.5 secondi
        val langCode = language.take(2).lowercase()
        val cloudResults = try {
            withTimeoutOrNull(2500L) {
                val response = api.searchSemantics(query = sanitizedQuery, lang = langCode)
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
