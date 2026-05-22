package com.cinetrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cinetrack.data.local.entities.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<FolderEntity>)

    @Query("SELECT * FROM folders WHERE sync_status != 'pending_delete'")
    suspend fun getAll(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE sync_status != 'pending_delete'")
    fun getAllFlow(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id AND sync_status != 'pending_delete' LIMIT 1")
    fun getByIdFlow(id: String): Flow<FolderEntity?>

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE folders SET sync_status = 'pending_delete', client_updated_at = :timestamp WHERE id = :id")
    suspend fun markDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM folders WHERE sync_status != 'pending_delete' AND name LIKE '%' || :query || '%' LIMIT 10")
    suspend fun search(query: String): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE sync_status != 'synced'")
    suspend fun getPendingSync(): List<FolderEntity>

    @Query("UPDATE folders SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM folders")
    suspend fun clearAll()
}
