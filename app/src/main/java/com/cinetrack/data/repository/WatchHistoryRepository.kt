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
        val localHistory = localHistoryList.associateBy { "${it.movieId}_${it.watchedAt}" }
        val remoteHistoryMap = remoteHistory.associateBy { "${it.movieId}_${it.watchedAt}" }

        val historyToInsert = mutableListOf<WatchHistoryEntity>()
        val historyToDelete = mutableListOf<WatchHistoryEntity>()

        for (remoteEntry in remoteHistory) {
            val local = localHistory["${remoteEntry.movieId}_${remoteEntry.watchedAt}"]
            val remoteEntity = remoteEntry.copy(syncStatus = "synced")

            if (local == null) {
                val isPendingDelete = watchHistoryDao.getPendingSync().any {
                    it.movieId == remoteEntry.movieId && it.watchedAt == remoteEntry.watchedAt && it.syncStatus == "deleted"
                }
                if (!isPendingDelete) {
                    historyToInsert.add(remoteEntity)
                }
            } else {
                if (local.syncStatus == "synced") {
                    historyToInsert.add(remoteEntity)
                }
            }
        }

        for ((key, localEntry) in localHistory) {
            if (!remoteHistoryMap.containsKey(key)) {
                if (localEntry.syncStatus == "synced") {
                    historyToDelete.add(localEntry)
                }
            }
        }

        if (historyToInsert.isNotEmpty()) {
            val chunks = historyToInsert.chunked(50)
            chunks.forEach { chunk ->
                watchHistoryDao.insertAll(chunk)
            }
        }

        for (entryToDelete in historyToDelete) {
            watchHistoryDao.delete(entryToDelete)
        }
        android.util.Log.d("WatchHistoryRepository", "Successfully synchronized watch history")
    }
}
