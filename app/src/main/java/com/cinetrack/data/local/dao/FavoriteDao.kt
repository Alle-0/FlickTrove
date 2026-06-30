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

    @Query("""
        UPDATE favorites 
        SET runtime = :runtime, 
            episode_run_time = :episodeRunTime, 
            genres = :genres, 
            top_cast_data = :topCastData, 
            director_data = :directorData, 
            director_id = :directorId, 
            director_name = :directorName, 
            director_profile_path = :directorProfilePath,
            seasons = :seasons,
            number_of_seasons = :numberOfSeasons,
            number_of_episodes = :numberOfEpisodes
        WHERE id = :id AND media_type = :mediaType
    """)
    suspend fun updateMetadata(
        id: Long, 
        mediaType: String,
        runtime: Int?,
        episodeRunTime: List<Int>?,
        genres: List<com.cinetrack.data.Genre>?,
        topCastData: List<com.cinetrack.data.models.PersonData>?,
        directorData: List<com.cinetrack.data.models.PersonData>?,
        directorId: Long?,
        directorName: String?,
        directorProfilePath: String?,
        seasons: List<com.cinetrack.data.models.Season>?,
        numberOfSeasons: Int?,
        numberOfEpisodes: Int?
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun importIgnore(movies: List<Movie>)

    @Query("SELECT * FROM favorites WHERE sync_status != 'pending_delete'")
    suspend fun getAll(): List<Movie>

    @Query("SELECT * FROM favorites WHERE sync_status != 'pending_delete'")
    fun getAllFlow(): Flow<List<Movie>>

    @Query("SELECT * FROM favorites WHERE id = :id AND media_type = :mediaType AND sync_status != 'pending_delete' LIMIT 1")
    suspend fun getById(id: Long, mediaType: String): Movie?

    @Query("SELECT * FROM favorites WHERE id = :id AND media_type = :mediaType AND sync_status != 'pending_delete' LIMIT 1")
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

    @Query("SELECT * FROM favorites WHERE (media_type || '_' || id IN (:compositeIds)) AND sync_status != 'pending_delete'")
    fun getByCompositeIds(compositeIds: List<String>): Flow<List<Movie>>

    @Query("DELETE FROM favorites")
    suspend fun clearAll()
}
