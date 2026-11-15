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
                _historyFlow.value = list.sortedByDescending { it.timestamp }
            }
        } catch (_: Exception) {
            _historyFlow.value = emptyList()
        }
    }

    fun saveEntry(entry: MoodHistoryEntry) {
        scope.launch {
            val current = _historyFlow.value.toMutableList()
            current.removeAll { it.query.equals(entry.query.trim(), ignoreCase = true) && it.isAi == entry.isAi }
            current.add(0, entry)
            val trimmed = current.take(MAX_HISTORY_ENTRIES)
            _historyFlow.value = trimmed
            persist(trimmed)
        }
    }

    fun deleteEntry(query: String) {
        scope.launch {
            val updated = _historyFlow.value.filterNot { it.query.equals(query.trim(), ignoreCase = true) }
            _historyFlow.value = updated
            persist(updated)
        }
    }

    fun clearHistory() {
        scope.launch {
            _historyFlow.value = emptyList()
            persist(emptyList())
        }
    }

    fun getCachedEntry(query: String, isAi: Boolean? = null): MoodHistoryEntry? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        return _historyFlow.value.firstOrNull {
            it.query.equals(trimmed, ignoreCase = true) && (isAi == null || it.isAi == isAi)
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
