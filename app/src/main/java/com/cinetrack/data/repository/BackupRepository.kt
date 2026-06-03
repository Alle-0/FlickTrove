package com.cinetrack.data.repository

import com.cinetrack.data.Movie
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.local.dao.FolderDao
import com.cinetrack.data.models.BackupData
import com.cinetrack.data.models.TraktExportItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.InputStream
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

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun importDataStream(inputStream: InputStream) = withContext(Dispatchers.IO) {
        val backup = json.decodeFromStream<BackupData>(inputStream)
        
        // Restore Favorites (Smart Merge)
        if (backup.favorites.isNotEmpty()) {
            val localFavoritesMap = favoriteDao.getAll().associateBy { "${it.mediaType}_${it.id}" }
            
            val mergedFavorites = backup.favorites.map { importedMovie ->
                val localMovie = localFavoritesMap["${importedMovie.mediaType}_${importedMovie.id}"]
                if (localMovie != null) {
                    // Smart Merge: Keep local personalNote, personalRating, and watched data if present locally
                    importedMovie.copy(
                        personalNote = localMovie.personalNote ?: importedMovie.personalNote,
                        personalRating = localMovie.personalRating ?: importedMovie.personalRating,
                        watchedAt = localMovie.watchedAt ?: importedMovie.watchedAt,
                        watched = localMovie.watched || importedMovie.watched,
                        clientUpdatedAt = maxOf(localMovie.clientUpdatedAt, importedMovie.clientUpdatedAt),
                        syncStatus = "pending"
                    )
                } else {
                    importedMovie.copy(syncStatus = "pending")
                }
            }
            favoriteDao.insertAll(mergedFavorites)
        }
        
        // Restore Folders
        if (backup.folders.isNotEmpty()) {
            val pendingFolders = backup.folders.map { folder ->
                folder.copy(syncStatus = "pending")
            }
            folderDao.insertAll(pendingFolders)
        }
        
        // Restore Preferences
        backup.preferences?.let {
            preferenceRepository.updateAll(it)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun migrateTraktStream(inputStream: InputStream): Int = withContext(Dispatchers.IO) {
        val items = json.decodeFromStream<List<TraktExportItem>>(inputStream)
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

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.clear()
            } else {
                current.append(c)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    suspend fun migrateCsvStream(inputStream: InputStream): Int = withContext(Dispatchers.IO) {
        var count = 0
        inputStream.bufferedReader().useLines { linesSequence ->
            val lines = linesSequence.filter { it.isNotBlank() }.iterator()
            if (!lines.hasNext()) return@withContext 0
            val headerLine = lines.next()
            val headers = parseCsvLine(headerLine)
            
            val isLetterboxd = headers.contains("Letterboxd URI")
            val isImdb = headers.contains("Const")
            
            if (!isLetterboxd && !isImdb) return@withContext 0
            
            val nameIdx = headers.indexOf("Name")
            val yearIdx = headers.indexOf("Year")
            val constIdx = headers.indexOf("Const")
            val titleTypeIdx = headers.indexOf("Title Type")
            val titleIdx = headers.indexOf("Title")

            val dataLines = lines.asSequence()
            
            // Use async to fetch external IDs concurrently in chunks to respect API rate limits
            val chunks = dataLines.chunked(20)
            for (chunk in chunks) {
                val deferreds = chunk.mapNotNull { line ->
                    val columns = parseCsvLine(line)
                    if (isLetterboxd) {
                        if (nameIdx == -1 || columns.size <= nameIdx) return@mapNotNull null
                        val name = columns[nameIdx]
                        val year = if (yearIdx != -1 && columns.size > yearIdx) columns[yearIdx] else null
                        
                        async {
                            try {
                                val tmdbMovie = movieRepository.searchMovieWithYear(name, year)
                                tmdbMovie?.let {
                                    Movie(
                                        id = it.id,
                                        mediaType = it.mediaType ?: "movie",
                                        title = it.title,
                                        name = it.name,
                                        watched = true,
                                        favorite = false,
                                        watchedAt = java.time.Instant.now().toString(),
                                        syncStatus = "pending",
                                        clientUpdatedAt = System.currentTimeMillis()
                                    )
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("Migrazione", "Fallito per $name", e)
                                null
                            }
                        }
                    } else if (isImdb) {
                        if (constIdx == -1 || columns.size <= constIdx) return@mapNotNull null
                        val const = columns[constIdx]
                        val titleType = if (titleTypeIdx != -1 && columns.size > titleTypeIdx) columns[titleTypeIdx] else "movie"
                        
                        async {
                            try {
                                val tmdbMovie = movieRepository.findByImdbId(const)
                                tmdbMovie?.let {
                                    val mediaType = if (titleType.contains("tv", ignoreCase = true) || it.mediaType == "tv") "tv" else "movie"
                                    Movie(
                                        id = it.id,
                                        mediaType = mediaType,
                                        title = it.title,
                                        name = it.name,
                                        watched = true,
                                        favorite = false,
                                        watchedAt = java.time.Instant.now().toString(),
                                        syncStatus = "pending",
                                        clientUpdatedAt = System.currentTimeMillis()
                                    )
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("Migrazione", "Fallito per $const", e)
                                null
                            }
                        }
                    } else {
                        null
                    }
                }
                
                val results = deferreds.awaitAll().filterNotNull()
                if (results.isNotEmpty()) {
                    favoriteDao.importIgnore(results)
                    count += results.size
                }
                // Delay to prevent hitting API rate limits aggressively
                kotlinx.coroutines.delay(100)
            }
        }
        count
    }
}
