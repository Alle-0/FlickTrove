package com.cinetrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cinetrack.data.local.entities.ColorCacheEntity
import com.cinetrack.data.local.entities.MovieDetailCacheEntity

@Dao
interface CacheDao {
    // --- Color Cache ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveColor(color: ColorCacheEntity)

    @Query("SELECT * FROM color_cache WHERE id = :id LIMIT 1")
    suspend fun getColor(id: String): ColorCacheEntity?

    // --- Movie/TV Detail Cache ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(detail: MovieDetailCacheEntity)

    @Query("DELETE FROM movie_details_cache WHERE id NOT IN (SELECT id FROM movie_details_cache ORDER BY updated_at DESC LIMIT 150)")
    suspend fun cleanupDetails()

    @Transaction
    suspend fun saveDetailWithLRU(detail: MovieDetailCacheEntity) {
        insertDetail(detail)
        cleanupDetails()
    }

    @Query("SELECT data FROM movie_details_cache WHERE id = :id AND media_type = :mediaType LIMIT 1")
    suspend fun getDetail(id: Long, mediaType: String): String?
}
