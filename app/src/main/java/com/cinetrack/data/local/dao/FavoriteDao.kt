package com.cinetrack.data.local.dao

import androidx.room.*
import com.cinetrack.data.Movie
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movie: Movie)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<Movie>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun importIgnore(movies: List<Movie>)

    @Query("SELECT * FROM favorites WHERE sync_status != 'pending_delete'")
    suspend fun getAll(): List<Movie>

    @Query("SELECT * FROM favorites WHERE sync_status != 'pending_delete'")
    fun getAllFlow(): Flow<List<Movie>>

    @Query("SELECT * FROM favorites WHERE id = :id AND media_type = :mediaType LIMIT 1")
    suspend fun getById(id: Long, mediaType: String): Movie?

    @Query("SELECT * FROM favorites WHERE id = :id AND media_type = :mediaType LIMIT 1")
    fun getByIdFlow(id: Long, mediaType: String): Flow<Movie?>

    @Query("DELETE FROM favorites WHERE id = :id AND media_type = :mediaType")
    suspend fun deleteById(id: Long, mediaType: String)

    @Query("UPDATE favorites SET sync_status = 'pending_delete', client_updated_at = :timestamp WHERE id = :id AND media_type = :mediaType")
    suspend fun markDeleted(id: Long, mediaType: String, timestamp: Long = System.currentTimeMillis())



    @Query("SELECT * FROM favorites WHERE sync_status != 'synced'")
    suspend fun getPendingSync(): List<Movie>

    @Query("SELECT * FROM favorites WHERE media_type = 'tv' AND (status IS NULL OR status NOT IN ('Ended', 'Canceled')) ORDER BY client_updated_at ASC LIMIT :limit")
    suspend fun getShowsForUpdate(limit: Int = 150): List<Movie>

    @Query("SELECT * FROM favorites WHERE media_type = 'movie' AND reminder = 1 AND watched = 0 ORDER BY client_updated_at ASC LIMIT :limit")
    suspend fun getUpcomingMoviesForUpdate(limit: Int = 150): List<Movie>

    @Query("UPDATE favorites SET sync_status = :status WHERE id = :id AND media_type = :mediaType")
    suspend fun updateSyncStatus(id: Long, mediaType: String, status: String)

    @Query("SELECT * FROM favorites WHERE media_type || '_' || id IN (:compositeIds)")
    fun getByCompositeIds(compositeIds: List<String>): Flow<List<Movie>>

    @Query("DELETE FROM favorites")
    suspend fun clearAll()
}
