package com.cinetrack.data.local.dao

import androidx.room.*
import com.cinetrack.data.model.Movie
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movie: Movie)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<Movie>)



    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun importIgnore(movies: List<Movie>)

    @Query("SELECT * FROM favorites WHERE sync_status NOT IN ('pending_delete', 'deleted')")
    suspend fun getAll(): List<Movie>

    @Query("SELECT * FROM favorites")
    suspend fun getAllIncludingDeleted(): List<Movie>

    @Query("SELECT * FROM favorites WHERE sync_status NOT IN ('pending_delete', 'deleted')")
    fun getAllFlow(): Flow<List<Movie>>

    @Query("SELECT * FROM favorites WHERE sync_status NOT IN ('pending_delete', 'deleted') AND ((emotional_vibes IS NOT NULL AND emotional_vibes != '' AND emotional_vibes != '[]') OR favorite_actor_id IS NOT NULL)")
    fun getFlowMoviesFlow(): Flow<List<Movie>>

    @Query(
        "SELECT * FROM favorites " +
        "WHERE sync_status NOT IN ('pending_delete', 'deleted') " +
        "AND (:mediaType = '' OR media_type = :mediaType) " +
        "AND (title LIKE :query OR name LIKE :query OR overview LIKE :query)"
    )
    suspend fun searchLocalMovies(query: String, mediaType: String = ""): List<Movie>

    @Query("SELECT * FROM favorites WHERE id = :id AND media_type = :mediaType AND sync_status NOT IN ('pending_delete', 'deleted') LIMIT 1")
    suspend fun getById(id: Long, mediaType: String): Movie?

    @Query("SELECT * FROM favorites WHERE id = :id AND media_type = :mediaType AND sync_status NOT IN ('pending_delete', 'deleted') LIMIT 1")
    fun getByIdFlow(id: Long, mediaType: String): Flow<Movie?>

    /** Used by sync workers to detect soft-deleted items and prevent resurrection */
    @Query("SELECT * FROM favorites WHERE id = :id AND media_type = :mediaType LIMIT 1")
    suspend fun getByIdIncludingDeleted(id: Long, mediaType: String): Movie?

    @Query("DELETE FROM favorites WHERE id = :id AND media_type = :mediaType")
    suspend fun deleteById(id: Long, mediaType: String)

    @Query("UPDATE favorites SET sync_status = 'pending_delete', client_updated_at = :timestamp WHERE id = :id AND media_type = :mediaType")
    suspend fun markDeleted(id: Long, mediaType: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE favorites SET sync_status = 'deleted', watched = 0, favorite = 0, reminder = 0, dropped = 0, personal_rating = NULL, personal_note = NULL, client_updated_at = :timestamp WHERE id = :id AND media_type = :mediaType")
    suspend fun markAsTombstone(id: Long, mediaType: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM favorites WHERE sync_status NOT IN ('synced', 'deleted')")
    suspend fun getPendingSync(): List<Movie>

    @Query("SELECT * FROM favorites WHERE media_type = 'tv' AND (status IS NULL OR status NOT IN ('Ended', 'Canceled')) AND sync_status NOT IN ('pending_delete', 'deleted') ORDER BY client_updated_at ASC LIMIT :limit")
    suspend fun getShowsForUpdate(limit: Int = 150): List<Movie>

    @Query("SELECT * FROM favorites WHERE media_type = 'movie' AND reminder = 1 AND watched = 0 AND sync_status NOT IN ('pending_delete', 'deleted') ORDER BY client_updated_at ASC LIMIT :limit")
    suspend fun getUpcomingMoviesForUpdate(limit: Int = 150): List<Movie>

    @Query("UPDATE favorites SET sync_status = :status WHERE id = :id AND media_type = :mediaType")
    suspend fun updateSyncStatus(id: Long, mediaType: String, status: String)

    @Query("SELECT * FROM favorites WHERE (media_type || '_' || id IN (:compositeIds)) AND sync_status NOT IN ('pending_delete', 'deleted')")
    fun getByCompositeIds(compositeIds: List<String>): Flow<List<Movie>>

    @Query("SELECT * FROM favorites WHERE (poster_path IS NULL OR poster_path = '') AND sync_status NOT IN ('pending_delete', 'deleted')")
    suspend fun getMoviesWithMissingPosters(): List<Movie>

    @Query("DELETE FROM favorites")
    suspend fun clearAll()
}
