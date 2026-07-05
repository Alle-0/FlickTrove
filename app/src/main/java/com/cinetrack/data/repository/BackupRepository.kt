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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        val content = inputStream.bufferedReader().use { it.readText() }
        if (content.isBlank()) return@withContext 0
        var count = 0
        
        // 1. Try standard Trakt Export format
        try {
            val items = json.decodeFromString<List<TraktExportItem>>(content)
            val moviesToInsert = items.mapNotNull { item ->
                val tmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb ?: return@mapNotNull null
                val mediaType = if (item.movie != null) "movie" else "tv"
                Movie(
                    id = tmdbId,
                    mediaType = mediaType,
                    title = item.movie?.title ?: item.show?.title,
                    name = item.show?.title,
                    watched = true,
                    favorite = false,
                    watchedAt = item.watchedAt ?: java.time.Instant.now().toString(),
                    syncStatus = "pending",
                    clientUpdatedAt = System.currentTimeMillis()
                )
            }
            if (moviesToInsert.isNotEmpty()) {
                favoriteDao.importIgnore(moviesToInsert)
                return@withContext moviesToInsert.size
            }
        } catch (e: Exception) {
            // Fallthrough to Universal Smart JSON Array parser
        }

        // 2. Universal Smart JSON Parser (for TVTime, Serializd, MyDramaList, custom exports, etc.)
        try {
            val element = json.parseToJsonElement(content)
            val array: List<JsonObject> = when (element) {
                is JsonArray -> element.mapNotNull { it as? JsonObject }
                is JsonObject -> {
                    val targetField = element.entries.firstOrNull { it.value is JsonArray }?.value as? JsonArray
                    targetField?.mapNotNull { it as? JsonObject } ?: listOf(element)
                }
                else -> emptyList()
            }

            val chunks = array.chunked(20)
            for (chunk in chunks) {
                val deferreds = chunk.mapNotNull { obj ->
                    val tmdbId = obj["tmdb_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: obj["tmdbId"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: obj["tmdb"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: obj["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                    val imdbId = obj["imdb_id"]?.jsonPrimitive?.contentOrNull
                        ?: obj["imdbId"]?.jsonPrimitive?.contentOrNull
                        ?: obj["imdb"]?.jsonPrimitive?.contentOrNull
                        ?: obj["const"]?.jsonPrimitive?.contentOrNull
                    val title = obj["title"]?.jsonPrimitive?.contentOrNull
                        ?: obj["name"]?.jsonPrimitive?.contentOrNull
                        ?: obj["show_name"]?.jsonPrimitive?.contentOrNull
                        ?: obj["movie_name"]?.jsonPrimitive?.contentOrNull
                    val year = obj["year"]?.jsonPrimitive?.contentOrNull
                        ?: obj["release_year"]?.jsonPrimitive?.contentOrNull
                    val typeStr = obj["type"]?.jsonPrimitive?.contentOrNull
                        ?: obj["media_type"]?.jsonPrimitive?.contentOrNull
                        ?: "movie"

                    async {
                        try {
                            if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt")) {
                                val tmdbMovie = movieRepository.findByImdbId(imdbId)
                                tmdbMovie?.let {
                                    val mediaType = if (typeStr.contains("tv", ignoreCase = true) || it.mediaType == "tv") "tv" else "movie"
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
                            } else if (tmdbId != null && tmdbId > 0) {
                                val mediaType = if (typeStr.contains("tv", ignoreCase = true) || typeStr.contains("show", ignoreCase = true)) "tv" else "movie"
                                Movie(
                                    id = tmdbId,
                                    mediaType = mediaType,
                                    title = title ?: "Unknown ($tmdbId)",
                                    name = if (mediaType == "tv") title else null,
                                    watched = true,
                                    favorite = false,
                                    watchedAt = java.time.Instant.now().toString(),
                                    syncStatus = "pending",
                                    clientUpdatedAt = System.currentTimeMillis()
                                )
                            } else if (!title.isNullOrBlank()) {
                                val tmdbMovie = movieRepository.searchMovieWithYear(title, year)
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
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                val results = deferreds.awaitAll().filterNotNull()
                if (results.isNotEmpty()) {
                    favoriteDao.importIgnore(results)
                    count += results.size
                }
                kotlinx.coroutines.delay(300)
            }
        } catch (e: Exception) {
            android.util.Log.e("Migrazione", "Fallito Universal JSON", e)
        }
        
        count
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    // Escaped quote
                    current.append('"')
                    i++ // Skip the second quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.clear()
            } else {
                current.append(c)
            }
            i++
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
            val lowerHeaders = headers.map { it.trim().lowercase() }
            
            val imdbIdx = lowerHeaders.indexOfFirst { it in listOf("const", "imdb id", "imdb_id", "imdbid", "imdb", "id", "title_id", "imdb_uri", "tconst") }
            val tmdbIdx = lowerHeaders.indexOfFirst { it in listOf("tmdb id", "tmdb_id", "tmdbid", "tmdb", "movie_id", "show_id") }
            val titleIdx = lowerHeaders.indexOfFirst { it in listOf("title", "name", "movie title", "show name", "film", "series", "show_name", "movie_name", "item_name", "movie", "show", "original title", "show title", "episode title", "original_title", "letterboxd uri") }
            val yearIdx = lowerHeaders.indexOfFirst { it in listOf("year", "release year", "release_year", "date_released", "date released", "release date", "date", "watched date", "created date") }
            val typeIdx = lowerHeaders.indexOfFirst { it in listOf("title type", "type", "media type", "media_type", "item_type", "contenttype") }
            
            val hasKnownHeaders = imdbIdx != -1 || tmdbIdx != -1 || titleIdx != -1
            if (!hasKnownHeaders && headers.none { it.startsWith("tt") }) {
                return@withContext 0
            }

            val dataLines = lines.asSequence()
            val chunks = dataLines.chunked(20)
            for (chunk in chunks) {
                val deferreds = chunk.mapNotNull { line ->
                    val columns = parseCsvLine(line)
                    if (columns.isEmpty()) return@mapNotNull null
                    
                    val imdbVal = if (imdbIdx != -1 && columns.size > imdbIdx) columns[imdbIdx] 
                                  else columns.firstOrNull { it.trim().startsWith("tt") }
                    val tmdbVal = if (tmdbIdx != -1 && columns.size > tmdbIdx) columns[tmdbIdx]?.toLongOrNull() else null
                    val titleVal = if (titleIdx != -1 && columns.size > titleIdx) columns[titleIdx] else null
                    val yearVal = if (yearIdx != -1 && columns.size > yearIdx) columns[yearIdx] else null
                    val typeVal = if (typeIdx != -1 && columns.size > typeIdx) columns[typeIdx] else "movie"
                    
                    async {
                        try {
                            if (!imdbVal.isNullOrBlank() && imdbVal.trim().startsWith("tt")) {
                                val tmdbMovie = movieRepository.findByImdbId(imdbVal.trim())
                                tmdbMovie?.let {
                                    val mediaType = if (typeVal.contains("tv", ignoreCase = true) || typeVal.contains("series", ignoreCase = true) || it.mediaType == "tv") "tv" else "movie"
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
                            } else if (tmdbVal != null && tmdbVal > 0) {
                                val mediaType = if (typeVal.contains("tv", ignoreCase = true) || typeVal.contains("series", ignoreCase = true) || typeVal.contains("show", ignoreCase = true)) "tv" else "movie"
                                Movie(
                                    id = tmdbVal,
                                    mediaType = mediaType,
                                    title = titleVal ?: "Unknown ($tmdbVal)",
                                    name = if (mediaType == "tv") titleVal else null,
                                    watched = true,
                                    favorite = false,
                                    watchedAt = java.time.Instant.now().toString(),
                                    syncStatus = "pending",
                                    clientUpdatedAt = System.currentTimeMillis()
                                )
                            } else if (!titleVal.isNullOrBlank()) {
                                val tmdbMovie = movieRepository.searchMovieWithYear(titleVal.trim(), yearVal?.trim())
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
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                
                val results = deferreds.awaitAll().filterNotNull()
                if (results.isNotEmpty()) {
                    favoriteDao.importIgnore(results)
                    count += results.size
                }
                kotlinx.coroutines.delay(350)
            }
        }
        count
    }
}
