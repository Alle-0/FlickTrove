package com.cinetrack.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cinetrack.data.local.entities.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE movieId = :movieId AND syncStatus != 'deleted' ORDER BY watchedAt DESC")
    suspend fun getWatchHistoryForMovie(movieId: Long): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE movieId = :movieId AND syncStatus != 'deleted' ORDER BY watchedAt DESC")
    fun getWatchHistoryForMovieFlow(movieId: Long): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE syncStatus != 'deleted' ORDER BY watchedAt DESC")
    suspend fun getAllWatchHistory(): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE syncStatus != 'deleted' ORDER BY watchedAt DESC")
    fun getAllWatchHistoryFlow(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE syncStatus = 'pending' OR syncStatus = 'deleted'")
    suspend fun getPendingSync(): List<WatchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watchHistory: WatchHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(watchHistories: List<WatchHistoryEntity>)

    @Update
    suspend fun update(watchHistory: WatchHistoryEntity)

    @Delete
    suspend fun delete(watchHistory: WatchHistoryEntity)
    
    @Query("UPDATE watch_history SET syncStatus = 'deleted' WHERE id = :id")
    suspend fun markDeleted(id: Long)
    
    @Query("UPDATE watch_history SET syncStatus = 'deleted' WHERE movieId = :movieId")
    suspend fun deleteByMovieId(movieId: Long)
    
    @Query("DELETE FROM watch_history WHERE movieId = :movieId")
    suspend fun purgeHistoryForMovie(movieId: Long)
    
    @Query("UPDATE watch_history SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String)
    
    @Query("DELETE FROM watch_history WHERE syncStatus = 'deleted'")
    suspend fun purgeDeleted()
}
