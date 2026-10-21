package com.cinetrack.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cinetrack.data.local.entities.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

data class RawTimelineRecord(
    val historyId: Long,
    val movieId: Long,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val watchedAt: String,
    val isRewatch: Boolean,
    val personalRating: Double?,
    val personalNote: String?,
    val runtimeMinutes: Int,
    val historicalEpisodes: String?,
    val currentEpisodes: Map<String, List<Int>>?,
    val releaseDate: String?,
    val firstAirDate: String?
)

@Dao
interface WatchHistoryDao {
    @Query("""
        SELECT h.id AS historyId, 
               f.id AS movieId, 
               f.media_type AS mediaType, 
               COALESCE(f.title, f.name, '') AS title, 
               f.poster_path AS posterPath, 
               h.watchedAt, 
               h.isRewatch, 
               f.personal_rating AS personalRating, 
               f.personal_note AS personalNote,
               COALESCE(f.runtime, 0) AS runtimeMinutes,
               h.historicalEpisodes, 
               f.watched_episodes AS currentEpisodes,
               f.release_date AS releaseDate,
               f.first_air_date AS firstAirDate
        FROM watch_history h
        INNER JOIN favorites f ON h.movieId = f.id AND h.mediaType = f.media_type
        WHERE f.sync_status != 'pending_delete' AND f.sync_status != 'deleted' AND h.syncStatus != 'deleted'
        ORDER BY h.watchedAt DESC
    """)
    fun getGlobalTimelineFlow(): Flow<List<RawTimelineRecord>>

    @Query("SELECT * FROM watch_history WHERE movieId = :movieId AND syncStatus != 'deleted' ORDER BY watchedAt DESC")
    suspend fun getWatchHistoryForMovie(movieId: Long): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE movieId = :movieId AND mediaType = :mediaType AND syncStatus != 'deleted' ORDER BY watchedAt DESC")
    suspend fun getWatchHistoryForMovie(movieId: Long, mediaType: String): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE movieId = :movieId AND syncStatus != 'deleted' ORDER BY watchedAt DESC")
    fun getWatchHistoryForMovieFlow(movieId: Long): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE movieId = :movieId AND mediaType = :mediaType AND syncStatus != 'deleted' ORDER BY watchedAt DESC")
    fun getWatchHistoryForMovieFlow(movieId: Long, mediaType: String): Flow<List<WatchHistoryEntity>>

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

    @Query("UPDATE watch_history SET syncStatus = 'deleted' WHERE movieId = :movieId AND mediaType = :mediaType")
    suspend fun deleteByMovieId(movieId: Long, mediaType: String)
    
    @Query("UPDATE watch_history SET watchedAt = :newWatchedAt, syncStatus = 'pending' WHERE id = :id")
    suspend fun updateWatchDate(id: Long, newWatchedAt: String)

    @Query("DELETE FROM watch_history WHERE movieId = :movieId")
    suspend fun purgeHistoryForMovie(movieId: Long)

    @Query("DELETE FROM watch_history WHERE movieId = :movieId AND mediaType = :mediaType")
    suspend fun purgeHistoryForMovie(movieId: Long, mediaType: String)
    
    @Query("UPDATE watch_history SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String)
    
    @Query("DELETE FROM watch_history WHERE syncStatus = 'deleted'")
    suspend fun purgeDeleted()
}

