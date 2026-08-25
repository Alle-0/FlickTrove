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
    @Query("SELECT * FROM watch_history WHERE movieId = :movieId ORDER BY watchedAt DESC")
    suspend fun getWatchHistoryForMovie(movieId: Long): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE movieId = :movieId ORDER BY watchedAt DESC")
    fun getWatchHistoryForMovieFlow(movieId: Long): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC")
    suspend fun getAllWatchHistory(): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC")
    fun getAllWatchHistoryFlow(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watchHistory: WatchHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(watchHistories: List<WatchHistoryEntity>)

    @Update
    suspend fun update(watchHistory: WatchHistoryEntity)

    @Delete
    suspend fun delete(watchHistory: WatchHistoryEntity)
    
    @Query("DELETE FROM watch_history WHERE movieId = :movieId")
    suspend fun deleteByMovieId(movieId: Long)
}
