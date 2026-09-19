package com.cinetrack.data.repository

import com.cinetrack.data.local.dao.SearchHistoryDao
import com.cinetrack.data.local.entities.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepository @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) {
    fun getRecentSearches(): Flow<List<String>> = searchHistoryDao.getRecentSearches().map { entities ->
        entities.map { it.query }
    }

    suspend fun saveSearchQuery(query: String) {
        if (query.isBlank()) return
        searchHistoryDao.insertSearch(SearchHistoryEntity(query.trim(), System.currentTimeMillis()))
    }

    suspend fun deleteSearchQuery(query: String) {
        searchHistoryDao.deleteSearch(query)
    }

    suspend fun clearRecentSearches() {
        searchHistoryDao.clearHistory()
    }
}
