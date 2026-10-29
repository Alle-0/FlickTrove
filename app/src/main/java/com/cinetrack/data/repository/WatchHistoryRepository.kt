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
    fun getGlobalTimelineFlow(): Flow<List<com.cinetrack.data.local.dao.RawTimelineRecord>> =
        watchHistoryDao.getGlobalTimelineFlow()

    suspend fun updateWatchDate(id: Long, newWatchedAt: String) =
        watchHistoryDao.updateWatchDate(id, newWatchedAt)

    suspend fun updateEpisodeDatesAndWatchDate(id: Long, episodeDates: String?, newWatchedAt: String) =
        watchHistoryDao.updateEpisodeDatesAndWatchDate(id, episodeDates, newWatchedAt)

    suspend fun getWatchHistoryForMovie(movieId: Long, mediaType: String? = null): List<WatchHistoryEntity> =
        if (mediaType.isNullOrBlank()) watchHistoryDao.getWatchHistoryForMovie(movieId)
        else watchHistoryDao.getWatchHistoryForMovie(movieId, mediaType)

    fun getWatchHistoryForMovieFlow(movieId: Long, mediaType: String? = null): Flow<List<WatchHistoryEntity>> =
        if (mediaType.isNullOrBlank()) watchHistoryDao.getWatchHistoryForMovieFlow(movieId)
        else watchHistoryDao.getWatchHistoryForMovieFlow(movieId, mediaType)

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

    suspend fun deleteWatchHistoryByMovieId(movieId: Long, mediaType: String? = null) {
        if (mediaType.isNullOrBlank()) {
            watchHistoryDao.deleteByMovieId(movieId)
        } else {
            watchHistoryDao.deleteByMovieId(movieId, mediaType)
        }
    }

    suspend fun purgeHistoryForMovie(movieId: Long, mediaType: String? = null) {
        if (mediaType.isNullOrBlank()) {
            watchHistoryDao.deleteByMovieId(movieId)
            watchHistoryDao.purgeHistoryForMovie(movieId)
        } else {
            watchHistoryDao.deleteByMovieId(movieId, mediaType)
            watchHistoryDao.purgeHistoryForMovie(movieId, mediaType)
        }
    }

    suspend fun countDuplicateRewatches(): Int {
        val allHistory = watchHistoryDao.getAllWatchHistory()
        var duplicateCount = 0
        val byMovie = allHistory.groupBy { "${it.mediaType}_${it.movieId}" }
        for ((_, entries) in byMovie) {
            if (entries.size > 1) {
                duplicateCount += (entries.size - 1)
            } else if (entries.size == 1 && entries.first().isRewatch) {
                duplicateCount += 1
            }
        }
        return duplicateCount
    }

    suspend fun cleanupAllRewatches(): Int {
        val allHistory = watchHistoryDao.getAllWatchHistory()
        val toDelete = mutableListOf<WatchHistoryEntity>()
        val byMovie = allHistory.groupBy { "${it.mediaType}_${it.movieId}" }
        for ((_, entries) in byMovie) {
            if (entries.size > 1) {
                // Keep the oldest watch (first watch event)
                val sorted = entries.sortedBy { it.watchedAt }
                val duplicates = sorted.drop(1)
                toDelete.addAll(duplicates)
                // Ensure the retained first watch has isRewatch = false
                val first = sorted.first()
                if (first.isRewatch) {
                    watchHistoryDao.update(first.copy(isRewatch = false, syncStatus = "pending"))
                }
            } else if (entries.size == 1 && entries.first().isRewatch) {
                // If there's only 1 entry for this movie but it was flagged as rewatch, normalize it
                watchHistoryDao.update(entries.first().copy(isRewatch = false, syncStatus = "pending"))
            }
        }

        // Also catch any entry explicitly marked isRewatch = true that wasn't caught above
        for (entry in allHistory) {
            if (entry.isRewatch && toDelete.none { it.id == entry.id }) {
                toDelete.add(entry)
            }
        }

        if (toDelete.isEmpty()) return 0

        for (entry in toDelete) {
            watchHistoryDao.markDeleted(entry.id)
        }

        // Push pending deletions to Firebase (in bulk) and purge from Room
        pushPendingWatchHistory()

        return toDelete.size
    }

    suspend fun pushPendingWatchHistory() {
        val pendingHistory = watchHistoryDao.getPendingSync()
        val historyToDelete = pendingHistory.filter { it.syncStatus == "deleted" }
        val historyToSync = pendingHistory.filter { it.syncStatus == "pending" }.map { it.copy(syncStatus = "synced") }

        if (historyToDelete.isNotEmpty()) {
            try {
                firebaseRemoteDataSource.batchSoftDeleteWatchHistory(historyToDelete)
                for (history in historyToDelete) {
                    watchHistoryDao.delete(history)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("WatchHistoryRepository", "Failed to push pending delete watch history bulk", e)
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
        if (remoteHistory.isEmpty()) return

        val localHistoryList = watchHistoryDao.getAllWatchHistory()
        // Key: "mediaType_movieId_watchedAt" — composite unique identity for a watch event
        val localHistory = localHistoryList.associateBy { "${it.mediaType}_${it.movieId}_${it.watchedAt}" }

        val historyToInsert = mutableListOf<WatchHistoryEntity>()
        val historyToDeleteLocally = mutableListOf<WatchHistoryEntity>()

        // Cache pending-delete set once to avoid N+1 queries inside the loop
        val pendingDeleteKeys = watchHistoryDao.getPendingSync()
            .filter { it.syncStatus == "deleted" }
            .map { "${it.mediaType}_${it.movieId}_${it.watchedAt}" }
            .toHashSet()

        for (remoteEntry in remoteHistory) {
            val key = "${remoteEntry.mediaType}_${remoteEntry.movieId}_${remoteEntry.watchedAt}"
            val local = localHistory[key]

            if (remoteEntry.syncStatus == "deleted") {
                // Remote tombstone: rimosso da un altro dispositivo
                if (local != null) {
                    historyToDeleteLocally.add(local)
                }
            } else {
                when {
                    // Entry doesn't exist locally → insert it, unless the user deleted it locally
                    local == null -> {
                        if (key !in pendingDeleteKeys) {
                            historyToInsert.add(remoteEntry.copy(syncStatus = "synced"))
                        }
                    }
                    // Entry exists locally and is already in sync → update if remote metadata changed
                    local.syncStatus == "synced" -> {
                        if (local.historicalEpisodes != remoteEntry.historicalEpisodes ||
                            local.episodeDates != remoteEntry.episodeDates ||
                            local.isRewatch != remoteEntry.isRewatch) {
                            historyToInsert.add(remoteEntry.copy(id = local.id, syncStatus = "synced"))
                        }
                    }
                    // Entry exists locally with "pending" or other status → user modified it locally; respect local data
                    else -> { /* local wins, do nothing */ }
                }
            }
        }

        if (historyToInsert.isNotEmpty()) {
            historyToInsert.chunked(50).forEach { chunk ->
                watchHistoryDao.insertAll(chunk)
            }
        }

        for (entryToDelete in historyToDeleteLocally) {
            watchHistoryDao.delete(entryToDelete)
        }
        android.util.Log.d("WatchHistoryRepository", "syncWatchHistoryFromRemote: inserted=${historyToInsert.size}, deleted=${historyToDeleteLocally.size}")
    }
}
