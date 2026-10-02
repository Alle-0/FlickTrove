package com.cinetrack.data.repository

import com.cinetrack.data.local.dao.WatchHistoryDao
import com.cinetrack.data.local.entities.WatchHistoryEntity
import com.cinetrack.data.remote.FirebaseRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryRepository @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao,
    private val firebaseRemoteDataSource: FirebaseRemoteDataSource
) {
    suspend fun getWatchHistoryForMovie(movieId: Long): List<WatchHistoryEntity> =
        watchHistoryDao.getWatchHistoryForMovie(movieId)

    fun getWatchHistoryForMovieFlow(movieId: Long): Flow<List<WatchHistoryEntity>> =
        watchHistoryDao.getWatchHistoryForMovieFlow(movieId)

    fun getAllWatchHistoryFlow(): Flow<List<WatchHistoryEntity>> =
        watchHistoryDao.getAllWatchHistoryFlow()

    suspend fun getAllWatchHistory(): List<WatchHistoryEntity> =
        watchHistoryDao.getAllWatchHistory()

    suspend fun insertWatchHistory(history: WatchHistoryEntity) =
        watchHistoryDao.insert(history)

    suspend fun updateWatchHistory(history: WatchHistoryEntity) =
        watchHistoryDao.update(history.copy(syncStatus = "pending"))

    suspend fun deleteWatchHistory(history: WatchHistoryEntity) =
        watchHistoryDao.markDeleted(history.id)

    suspend fun deleteWatchHistoryByMovieId(movieId: Long) =
        watchHistoryDao.deleteByMovieId(movieId)

    suspend fun purgeHistoryForMovie(movieId: Long) {
        watchHistoryDao.deleteByMovieId(movieId)
        watchHistoryDao.purgeHistoryForMovie(movieId)
    }

    suspend fun pushPendingWatchHistory() {
        val pendingHistory = watchHistoryDao.getPendingSync()
        val historyToDelete = pendingHistory.filter { it.syncStatus == "deleted" }
        val historyToSync = pendingHistory.filter { it.syncStatus == "pending" }.map { it.copy(syncStatus = "synced") }

        for (history in historyToDelete) {
            try {
                firebaseRemoteDataSource.deleteWatchHistory(history.movieId, history.watchedAt)
                watchHistoryDao.delete(history)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("WatchHistoryRepository", "Failed to push pending delete watch history ${history.id}", e)
            }
        }

        if (historyToSync.isNotEmpty()) {
            try {
                firebaseRemoteDataSource.batchSetWatchHistory(historyToSync)
                for (history in historyToSync) {
                    watchHistoryDao.updateSyncStatus(history.id, "synced")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("WatchHistoryRepository", "Failed to push pending watch history bulk", e)
            }
        }
    }

    suspend fun syncWatchHistoryFromRemote(remoteHistory: List<WatchHistoryEntity>) {
        val localHistoryList = watchHistoryDao.getAllWatchHistory()
        // Key: "movieId_watchedAt" — unique identity for a watch event
        val localHistory = localHistoryList.associateBy { "${it.movieId}_${it.watchedAt}" }
        val remoteHistoryMap = remoteHistory.associateBy { "${it.movieId}_${it.watchedAt}" }

        val historyToInsert = mutableListOf<WatchHistoryEntity>()
        val historyToDelete = mutableListOf<WatchHistoryEntity>()

        // Cache pending-delete set once to avoid N+1 queries inside the loop
        val pendingDeleteKeys = watchHistoryDao.getPendingSync()
            .filter { it.syncStatus == "deleted" }
            .map { "${it.movieId}_${it.watchedAt}" }
            .toHashSet()

        for (remoteEntry in remoteHistory) {
            val key = "${remoteEntry.movieId}_${remoteEntry.watchedAt}"
            val local = localHistory[key]

            when {
                // Entry doesn't exist locally → insert it, unless the user deleted it locally
                local == null -> {
                    if (key !in pendingDeleteKeys) {
                        historyToInsert.add(remoteEntry.copy(syncStatus = "synced"))
                    }
                }
                // Entry exists locally and is already in sync → nothing to do (skip to avoid phantom REPLACE)
                local.syncStatus == "synced" -> { /* already up-to-date, do nothing */ }
                // Entry exists locally with "pending" or other status → user modified it locally; respect local data
                else -> { /* local wins, do nothing */ }
            }
        }

        // Entries that exist locally (as "synced") but are absent from remote → remote deleted them
        for ((key, localEntry) in localHistory) {
            if (!remoteHistoryMap.containsKey(key) && localEntry.syncStatus == "synced") {
                historyToDelete.add(localEntry)
            }
        }

        if (historyToInsert.isNotEmpty()) {
            historyToInsert.chunked(50).forEach { chunk ->
                watchHistoryDao.insertAll(chunk)
            }
        }

        for (entryToDelete in historyToDelete) {
            watchHistoryDao.delete(entryToDelete)
        }
        android.util.Log.d("WatchHistoryRepository", "syncWatchHistoryFromRemote: inserted=${historyToInsert.size}, deleted=${historyToDelete.size}")
    }
}
