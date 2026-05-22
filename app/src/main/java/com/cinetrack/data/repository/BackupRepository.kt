package com.cinetrack.data.repository

import com.cinetrack.data.Movie
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.local.dao.FolderDao
import com.cinetrack.data.models.BackupData
import com.cinetrack.data.models.TraktExportItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val folderDao: FolderDao,
    private val preferenceRepository: PreferenceRepository,
    private val movieRepository: MovieRepository
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
        prettyPrint = true
    }

    suspend fun exportData(): String = withContext(Dispatchers.IO) {
        val backup = BackupData(
            favorites = favoriteDao.getAll(),
            folders = folderDao.getAll(),
            preferences = preferenceRepository.userPreferencesFlow.first()
        )
        json.encodeToString(backup)
    }

    suspend fun importData(jsonData: String) = withContext(Dispatchers.IO) {
        val backup = json.decodeFromString<BackupData>(jsonData)
        
        // Restore Favorites
        if (backup.favorites.isNotEmpty()) {
            favoriteDao.insertAll(backup.favorites)
        }
        
        // Restore Folders
        if (backup.folders.isNotEmpty()) {
            folderDao.insertAll(backup.folders)
        }
        
        // Restore Preferences
        backup.preferences?.let {
            preferenceRepository.updateAll(it)
        }
    }

    suspend fun migrateTrakt(jsonData: String): Int = withContext(Dispatchers.IO) {
        val items = json.decodeFromString<List<TraktExportItem>>(jsonData)
        var count = 0
        
        val moviesToInsert = items.mapNotNull { item ->
            val tmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb ?: return@mapNotNull null
            val mediaType = if (item.movie != null) "movie" else "tv"
            
            // Create a skeleton movie
            Movie(
                id = tmdbId,
                mediaType = mediaType,
                title = item.movie?.title ?: item.show?.title,
                name = item.show?.title,
                watched = true, // Usually history/watchlist means they are interested/watched
                favorite = false,
                watchedAt = item.watchedAt,
                syncStatus = "pending",
                clientUpdatedAt = System.currentTimeMillis()
            )
        }
        
        if (moviesToInsert.isNotEmpty()) {
            favoriteDao.importIgnore(moviesToInsert)
            count = moviesToInsert.size
            
            // Optional: Trigger background enrichment for these movies
            // We can do this in the ViewModel or a WorkManager
        }
        
        count
    }
}
