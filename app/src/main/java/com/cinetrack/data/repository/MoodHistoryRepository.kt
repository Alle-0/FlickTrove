package com.cinetrack.data.repository

import android.content.Context
import com.cinetrack.data.model.MoodHistoryEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoodHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("mood_history_prefs", Context.MODE_PRIVATE)

    private val _historyFlow = MutableStateFlow<List<MoodHistoryEntry>>(emptyList())
    val historyFlow: StateFlow<List<MoodHistoryEntry>> = _historyFlow.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        try {
            val raw = prefs.getString(KEY_MOOD_HISTORY, null)
            if (!raw.isNullOrBlank()) {
                val list = json.decodeFromString<List<MoodHistoryEntry>>(raw)
                val mergedList = mutableListOf<MoodHistoryEntry>()
                for (item in list.sortedByDescending { it.timestamp }) {
                    val existingIdx = mergedList.indexOfFirst { it.query.equals(item.query.trim(), ignoreCase = true) }
                    if (existingIdx == -1) {
                        val base = if (item.isAi) {
                            item.copy(
                                aiVibeTitle = item.aiVibeTitle ?: item.vibeTitle,
                                aiVibeDescription = item.aiVibeDescription ?: item.vibeDescription,
                                aiMovies = item.aiMovies ?: item.movies
                            )
                        } else {
                            item.copy(
                                algoVibeTitle = item.algoVibeTitle ?: item.vibeTitle,
                                algoVibeDescription = item.algoVibeDescription ?: item.vibeDescription,
                                algoMovies = item.algoMovies ?: item.movies
                            )
                        }
                        mergedList.add(base)
                    } else {
                        val existing = mergedList[existingIdx]
                        val combined = if (item.isAi) {
                            existing.copy(
                                aiVibeTitle = existing.aiVibeTitle ?: item.vibeTitle,
                                aiVibeDescription = existing.aiVibeDescription ?: item.vibeDescription,
                                aiMovies = existing.aiMovies ?: item.movies
                            )
                        } else {
                            existing.copy(
                                algoVibeTitle = existing.algoVibeTitle ?: item.vibeTitle,
                                algoVibeDescription = existing.algoVibeDescription ?: item.vibeDescription,
                                algoMovies = existing.algoMovies ?: item.movies
                            )
                        }
                        mergedList[existingIdx] = combined
                    }
                }
                _historyFlow.value = mergedList.take(MAX_HISTORY_ENTRIES)
            }
        } catch (_: Exception) {
            _historyFlow.value = emptyList()
        }
    }

    fun saveEntry(entry: MoodHistoryEntry) {
        val trimmed = synchronized(this) {
            val current = _historyFlow.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.query.equals(entry.query.trim(), ignoreCase = true) }
            val existing = if (existingIndex != -1) current.removeAt(existingIndex) else null

            val merged = if (existing != null) {
                if (entry.isAi) {
                    existing.copy(
                        timestamp = entry.timestamp,
                        vibeTitle = entry.vibeTitle,
                        vibeDescription = entry.vibeDescription,
                        isAi = true,
                        movies = entry.movies,
                        aiVibeTitle = entry.vibeTitle,
                        aiVibeDescription = entry.vibeDescription,
                        aiMovies = entry.movies,
                        algoVibeTitle = existing.algoVibeTitle ?: if (!existing.isAi) existing.vibeTitle else null,
                        algoVibeDescription = existing.algoVibeDescription ?: if (!existing.isAi) existing.vibeDescription else null,
                        algoMovies = existing.algoMovies ?: if (!existing.isAi) existing.movies else null
                    )
                } else {
                    existing.copy(
                        timestamp = entry.timestamp,
                        vibeTitle = entry.vibeTitle,
                        vibeDescription = entry.vibeDescription,
                        isAi = false,
                        movies = entry.movies,
                        algoVibeTitle = entry.vibeTitle,
                        algoVibeDescription = entry.vibeDescription,
                        algoMovies = entry.movies,
                        aiVibeTitle = existing.aiVibeTitle ?: if (existing.isAi) existing.vibeTitle else null,
                        aiVibeDescription = existing.aiVibeDescription ?: if (existing.isAi) existing.vibeDescription else null,
                        aiMovies = existing.aiMovies ?: if (existing.isAi) existing.movies else null
                    )
                }
            } else {
                if (entry.isAi) {
                    entry.copy(
                        aiVibeTitle = entry.vibeTitle,
                        aiVibeDescription = entry.vibeDescription,
                        aiMovies = entry.movies
                    )
                } else {
                    entry.copy(
                        algoVibeTitle = entry.vibeTitle,
                        algoVibeDescription = entry.vibeDescription,
                        algoMovies = entry.movies
                    )
                }
            }

            current.removeAll { it.query.equals(entry.query.trim(), ignoreCase = true) }
            current.add(0, merged)
            val list = current.take(MAX_HISTORY_ENTRIES)
            _historyFlow.value = list
            list
        }
        scope.launch {
            persist(trimmed)
        }
    }

    fun deleteEntry(query: String) {
        val updated = synchronized(this) {
            val list = _historyFlow.value.filterNot { it.query.equals(query.trim(), ignoreCase = true) }
            _historyFlow.value = list
            list
        }
        scope.launch {
            persist(updated)
        }
    }

    fun clearHistory() {
        synchronized(this) {
            _historyFlow.value = emptyList()
        }
        scope.launch {
            persist(emptyList())
        }
    }

    fun getCachedEntry(query: String, isAi: Boolean? = null): MoodHistoryEntry? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        return _historyFlow.value.firstOrNull { entry ->
            if (!entry.query.equals(trimmed, ignoreCase = true)) return@firstOrNull false
            if (isAi == null) return@firstOrNull true
            if (isAi) entry.hasAi() else entry.hasAlgo()
        }
    }

    private fun persist(list: List<MoodHistoryEntry>) {
        try {
            val raw = json.encodeToString(list)
            prefs.edit().putString(KEY_MOOD_HISTORY, raw).apply()
        } catch (_: Exception) {
            // Silently ignore disk write issues
        }
    }

    companion object {
        private const val KEY_MOOD_HISTORY = "mood_history_json"
        private const val MAX_HISTORY_ENTRIES = 15
    }
}
