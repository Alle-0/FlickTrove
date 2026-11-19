package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.model.MoodHistoryEntry
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MoodHistoryRepository
import com.cinetrack.data.repository.MoodSearchRepository
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.ui.utils.UiText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegate responsible for Mood AI search dispatching, local cache resolution,
 * quota management, history interactions, and expanded section preferences.
 */
@Singleton
class SearchMoodDelegate @Inject constructor(
    private val moodSearchRepository: MoodSearchRepository,
    private val moodHistoryRepository: MoodHistoryRepository,
    private val preferenceRepository: PreferenceRepository
) {
    fun resetQuotaForDev(
        scope: CoroutineScope,
        uiState: MutableStateFlow<SearchUiState>
    ) {
        scope.launch {
            preferenceRepository.setMoodSearchesRemaining(3)
            uiState.update { it.copy(moodRemainingQuota = 3) }
        }
    }

    fun submitMoodSearch(
        queryOverride: String?,
        uiState: MutableStateFlow<SearchUiState>,
        onExecute: (String) -> Unit
    ) {
        val q = (queryOverride ?: uiState.value.query).trim()
        if (q.isNotEmpty()) {
            onExecute(q)
        }
    }

    fun setMoodAiMode(
        useAi: Boolean,
        uiState: MutableStateFlow<SearchUiState>,
        onApplyCachedMovies: (List<TMDBSearchResult.MovieResult>, isMoodAiMode: Boolean, vibeTitle: String?, vibeDesc: String?) -> Unit,
        onClearResults: (isMoodAiMode: Boolean) -> Unit,
        onEmitMessage: (UiText) -> Unit
    ) {
        val isGuest = FirebaseAuth.getInstance().currentUser?.isAnonymous != false
        if (isGuest) return
        if (uiState.value.isMoodAiMode == useAi) return
        val remaining = uiState.value.moodRemainingQuota
        val currentQuery = uiState.value.query.trim()

        // 1. Se la query corrente ha già risultati salvati in cache per la modalità richiesta, caricali a costo zero
        if (uiState.value.category == "mood" && currentQuery.isNotEmpty()) {
            val cached = moodHistoryRepository.getCachedEntry(currentQuery, isAi = useAi)
            if (cached != null) {
                val cachedMovies = if (useAi) cached.resolveAiResults() else cached.resolveAlgoResults()
                if (cachedMovies.isNotEmpty()) {
                    onApplyCachedMovies(
                        cachedMovies,
                        useAi,
                        if (useAi) cached.resolveAiVibeTitle() else cached.resolveAlgoVibeTitle(),
                        if (useAi) cached.resolveAiVibeDescription() else cached.resolveAlgoVibeDescription()
                    )
                    return
                }
            }
        }

        // 2. Se non ci sono risultati salvati in cache e si tenta di passare all'IA senza quota
        if (useAi && remaining <= 0) {
            onEmitMessage(UiText.StringResource(R.string.search_mood_exhausted_notice))
            return
        }

        // 3. Se non ci sono risultati salvati in cache per la nuova modalità, azzera i risultati della modalità precedente
        // per evitare che rimangano visibili i risultati di IRIS sotto AI o viceversa
        if (uiState.value.category == "mood") {
            onClearResults(useAi)
        } else {
            uiState.update { it.copy(isMoodAiMode = useAi) }
        }
    }

    fun executeMoodSearch(
        query: String,
        bypassCache: Boolean = false,
        scope: CoroutineScope,
        uiState: MutableStateFlow<SearchUiState>,
        onPreSearch: () -> Unit,
        onSuccess: (List<TMDBSearchResult.MovieResult>, isQuotaFallback: Boolean, isNetworkFallback: Boolean, vibeTitle: String?, vibeDesc: String?) -> Unit,
        onFailure: (String) -> Unit
    ): Job? {
        val isGuest = FirebaseAuth.getInstance().currentUser?.isAnonymous != false
        if (isGuest) return null
        val sanitized = query.trim().take(1000)
        if (sanitized.isBlank()) return null

        onPreSearch()
        uiState.update { 
            it.copy(
                query = sanitized,
                category = "mood",
                isLoading = true, 
                errorMessage = null,
                moodErrorMessage = null,
                isMoodSearchSubmitted = true
            ) 
        }

        return scope.launch(Dispatchers.Default) {
            val contentLang = uiState.value.preferences.contentLanguage
            val targetLanguage = if (contentLang == "system") {
                Locale.getDefault().toLanguageTag()
            } else {
                contentLang
            }
            val useAi = uiState.value.isMoodAiMode && uiState.value.moodRemainingQuota > 0
            val result = moodSearchRepository.searchMoviesByMood(
                sanitized,
                language = targetLanguage,
                forceLocal = !useAi,
                bypassCache = bypassCache
            )
            withContext(Dispatchers.Main) {
                result.onSuccess { data ->
                    onSuccess(
                        data.movies,
                        data.isQuotaFallback,
                        data.isNetworkFallback,
                        data.vibeTitle,
                        data.vibeDescription
                    )
                }.onFailure { ex ->
                    onFailure(ex.message ?: "Errore durante la ricerca Mood")
                }
            }
        }
    }

    fun selectMoodHistory(
        entry: MoodHistoryEntry,
        uiState: MutableStateFlow<SearchUiState>,
        onApplyResult: (List<TMDBSearchResult.MovieResult>, isAi: Boolean, vibeTitle: String?, vibeDesc: String?) -> Unit,
        onFallbackSearch: (String) -> Unit
    ) {
        val currentMode = uiState.value.isMoodAiMode
        val resolvedMode = when {
            currentMode && entry.hasAi() -> true
            !currentMode && entry.hasAlgo() -> false
            entry.hasAi() -> true
            else -> false
        }
        if (!resolvedMode && !entry.hasAlgo()) {
            uiState.update {
                it.copy(
                    query = entry.query.take(1000),
                    category = "mood",
                    isMoodAiMode = false
                )
            }
            onFallbackSearch(entry.query.take(1000))
            return
        }
        val targetMovies = if (resolvedMode) entry.resolveAiResults() else entry.resolveAlgoResults()
        onApplyResult(
            targetMovies,
            resolvedMode,
            if (resolvedMode) entry.resolveAiVibeTitle() else entry.resolveAlgoVibeTitle(),
            if (resolvedMode) entry.resolveAiVibeDescription() else entry.resolveAlgoVibeDescription()
        )
    }

    fun deleteMoodHistory(query: String) {
        moodHistoryRepository.deleteEntry(query)
    }

    fun clearMoodHistory() {
        moodHistoryRepository.clearHistory()
    }

    fun toggleMoodRecentExpanded(scope: CoroutineScope, uiState: MutableStateFlow<SearchUiState>) {
        val current = uiState.value.preferences.isMoodRecentExpanded
        scope.launch {
            try {
                preferenceRepository.updateMoodRecentExpanded(!current)
            } catch (e: Exception) {
                android.util.Log.e("SearchMoodDelegate", "Error updating mood recent expanded preference", e)
            }
        }
    }

    fun toggleMoodInspirationsExpanded(scope: CoroutineScope, uiState: MutableStateFlow<SearchUiState>) {
        val current = uiState.value.preferences.isMoodInspirationsExpanded
        scope.launch {
            try {
                preferenceRepository.updateMoodInspirationsExpanded(!current)
            } catch (e: Exception) {
                android.util.Log.e("SearchMoodDelegate", "Error updating mood inspirations expanded preference", e)
            }
        }
    }
}
