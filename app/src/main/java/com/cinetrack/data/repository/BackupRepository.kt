package com.cinetrack.data.repository

import com.cinetrack.data.model.Movie
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.local.dao.FolderDao
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.model.BackupData
import com.cinetrack.data.model.TraktExportItem
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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
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
    suspend fun importDataStream(
        inputStream: InputStream,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val backup = json.decodeFromStream<BackupData>(inputStream)
        
        // Restore Favorites (Smart Merge)
        if (backup.favorites.isNotEmpty()) {
            val total = backup.favorites.size
            val localFavoritesMap = favoriteDao.getAll().associateBy { "${it.mediaType}_${it.id}" }
            
            val mergedFavorites = backup.favorites.mapIndexed { index, importedMovie ->
                if ((index + 1) % 5 == 0 || (index + 1) == total) {
                    onProgress(index + 1, total)
                }
                val localMovie = localFavoritesMap["${importedMovie.mediaType}_${importedMovie.id}"]
                if (localMovie != null) {
                    val combinedEps = mutableMapOf<String, MutableSet<Int>>()
                    localMovie.watchedEpisodes?.forEach { (s, eps) -> combinedEps.getOrPut(s) { mutableSetOf() }.addAll(eps) }
                    importedMovie.watchedEpisodes?.forEach { (s, eps) -> combinedEps.getOrPut(s) { mutableSetOf() }.addAll(eps) }
                    val newEpsMap = if (combinedEps.isNotEmpty()) combinedEps.mapValues { it.value.sorted() } else null

                    // Smart Merge: Keep local personalNote, personalRating, and watched data if present locally
                    importedMovie.copy(
                        personalNote = (localMovie.personalNote ?: importedMovie.personalNote)?.take(5000),
                        personalRating = localMovie.personalRating ?: importedMovie.personalRating,
                        watchedAt = localMovie.watchedAt ?: importedMovie.watchedAt,
                        watched = localMovie.watched || importedMovie.watched,
                        watchedEpisodes = newEpsMap,
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
    suspend fun importZipBackupStream(
        inputStream: InputStream,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        java.util.zip.ZipInputStream(inputStream).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.lowercase().endsWith(".json")) {
                    val entryBytes = zipStream.readBytes()
                    java.io.ByteArrayInputStream(entryBytes).use { 
                        importDataStream(it, onProgress)
                    }
                    break
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }
    }

    suspend fun migrateTraktStream(
        inputStream: InputStream,
        keepLatestWatchDate: Boolean = true,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
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
                    title = item.movie?.title ?: item.show?.title ?: "Unknown ($tmdbId)",
                    name = if (mediaType == "tv") (item.show?.title ?: "Unknown ($tmdbId)") else null,
                    watched = item.watched == true || item.watchedAt != null,
                    favorite = false,
                    personalRating = item.rating?.toDouble(),
                    personalNote = item.notes?.take(5000),
                    watchedAt = parseAndNormalizeWatchedDate(item.watchedAt),
                    syncStatus = "pending",
                    clientUpdatedAt = System.currentTimeMillis()
                )
            }
            if (moviesToInsert.isNotEmpty()) {
                processAndSaveImportedItems(moviesToInsert.map { Pair(it, null) }, keepLatestWatchDate, onProgress)
                return@withContext moviesToInsert.size
            }
        } catch (e: Exception) {
            // Fallthrough to Universal Smart JSON Array parser
        }

        // 2. Universal Smart JSON Parser (for Cinemaniac, TVTime, Serializd, MyDramaList, custom exports, etc.)
        try {
            val element = json.parseToJsonElement(content)
            
            // Build rating map if user ratings are stored in a separate array (e.g. Cinemaniac "ratings": [{"id": ..., "r": ...}])
            val ratingsMap = mutableMapOf<Long, Double>()
            if (element is JsonObject && element["ratings"] is JsonArray) {
                (element["ratings"] as JsonArray).forEach { rObj ->
                    val o = rObj as? JsonObject
                    val id = o?.get("id")?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                    val r = o?.get("r")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                        ?: o?.get("rating")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                    if (id != null && r != null) {
                        ratingsMap[id] = r
                    }
                }
            }

            val array: List<JsonObject> = when (element) {
                is JsonArray -> element.mapNotNull { it as? JsonObject }
                is JsonObject -> {
                    val allObjects = mutableListOf<JsonObject>()
                    element.entries.forEach { entry ->
                        // Skip non-media arrays like ratings, settings, categories
                        if (entry.key != "ratings" && entry.key != "settings" && entry.key != "categories") {
                            val valArray = entry.value as? JsonArray
                            if (valArray != null) {
                                val isCustomList = entry.key !in listOf("movies", "shows", "items", "media", "series", "list", "data", "results", "episodes", "seen_episodes", "followed_shows", "watched_episodes", "watched_movies", "watched", "watchlist", "diary", "history", "shows_list", "movies_list", "ratings", "reviews")
                                valArray.forEach { el ->
                                    val obj = el as? JsonObject
                                    if (obj != null) {
                                        val objWithFolder = if (isCustomList && !obj.containsKey("folder") && !obj.containsKey("list_name")) {
                                            JsonObject(obj + ("folder" to JsonPrimitive(entry.key)))
                                        } else obj
                                        allObjects.add(objWithFolder)
                                    }
                                }
                            }
                        }
                    }
                    if (allObjects.isNotEmpty()) allObjects else listOf(element)
                }
                else -> emptyList()
            }

            val chunks = array.chunked(20)
            for (chunk in chunks) {
                val deferreds = chunk.mapNotNull { obj ->
                    val itemObj = obj["movie"]?.jsonObject
                        ?: obj["show"]?.jsonObject
                        ?: obj["item"]?.jsonObject
                        ?: obj["series"]?.jsonObject
                        ?: obj
                    val idsObj = itemObj["ids"]?.jsonObject ?: obj["ids"]?.jsonObject ?: itemObj

                    val tmdbId = idsObj["tmdb_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: idsObj["tmdbId"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: idsObj["id_movie"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: idsObj["movie_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: idsObj["show_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: idsObj["tmdb"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: idsObj["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                    val imdbId = idsObj["imdb_id"]?.jsonPrimitive?.contentOrNull
                        ?: idsObj["imdbId"]?.jsonPrimitive?.contentOrNull
                        ?: idsObj["imdb"]?.jsonPrimitive?.contentOrNull
                        ?: idsObj["const"]?.jsonPrimitive?.contentOrNull
                    val title = itemObj["title"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["name"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["show_name"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["movie_name"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["show_title"]?.jsonPrimitive?.contentOrNull
                    val year = itemObj["year"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["release_year"]?.jsonPrimitive?.contentOrNull
                    val typeStr = itemObj["type"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["media_type"]?.jsonPrimitive?.contentOrNull
                        ?: obj["type"]?.jsonPrimitive?.contentOrNull
                        ?: obj["media_type"]?.jsonPrimitive?.contentOrNull
                        ?: "movie"

                    val userRating = if (tmdbId != null && ratingsMap.containsKey(tmdbId)) {
                        ratingsMap[tmdbId]
                    } else {
                        obj["personal_rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: obj["user_rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: obj["my_rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: itemObj["personal_rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: itemObj["user_rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: itemObj["my_rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: if (ratingsMap.isEmpty()) (obj["rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: itemObj["rating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()) else null
                    }
                    val userNote = (obj["personal_note"]?.jsonPrimitive?.contentOrNull
                        ?: obj["note"]?.jsonPrimitive?.contentOrNull
                        ?: obj["comment"]?.jsonPrimitive?.contentOrNull
                        ?: obj["review"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["personal_note"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["note"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["comment"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["review"]?.jsonPrimitive?.contentOrNull)?.take(5000)
                    // Determine if this item is watched.
                    // We check explicit watched/seen/status fields first.
                    // If none of those fields are present in the JSON at all, we assume
                    // the file is a pure watch-history export (every entry = watched).
                    val hasWatchedField = listOf("watched", "seen", "status", "watch_status").any { key ->
                        obj.containsKey(key) || itemObj.containsKey(key)
                    }
                    val explicitSeen = obj["watched"]?.jsonPrimitive?.booleanOrNull == true
                        || obj["seen"]?.jsonPrimitive?.contentOrNull == "1"
                        || obj["seen"]?.jsonPrimitive?.intOrNull == 1
                        || obj["seen"]?.jsonPrimitive?.booleanOrNull == true
                        || obj["status"]?.jsonPrimitive?.contentOrNull?.lowercase()
                            ?.let { it == "watched" || it == "completed" || it == "seen" } == true
                        || obj["watch_status"]?.jsonPrimitive?.contentOrNull?.lowercase()
                            ?.let { it == "watched" || it == "completed" } == true
                        || itemObj["watched"]?.jsonPrimitive?.booleanOrNull == true
                        || itemObj["seen"]?.jsonPrimitive?.contentOrNull == "1"
                        || itemObj["seen"]?.jsonPrimitive?.intOrNull == 1
                        || itemObj["seen"]?.jsonPrimitive?.booleanOrNull == true
                        || itemObj["status"]?.jsonPrimitive?.contentOrNull?.lowercase()
                            ?.let { it == "watched" || it == "completed" || it == "seen" } == true
                    val isSeen = if (hasWatchedField) explicitSeen else true
                    val isFav = obj["favorite"]?.jsonPrimitive?.booleanOrNull == true
                        || obj["fav"]?.jsonPrimitive?.booleanOrNull == true
                        || obj["starred"]?.jsonPrimitive?.booleanOrNull == true
                        || itemObj["favorite"]?.jsonPrimitive?.booleanOrNull == true
                        || itemObj["fav"]?.jsonPrimitive?.booleanOrNull == true
                        || itemObj["starred"]?.jsonPrimitive?.booleanOrNull == true

                    val folderName = obj["folder"]?.jsonPrimitive?.contentOrNull
                        ?: obj["list_name"]?.jsonPrimitive?.contentOrNull
                        ?: obj["list"]?.jsonPrimitive?.contentOrNull
                        ?: obj["tag"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["folder"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["list_name"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["list"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["tag"]?.jsonPrimitive?.contentOrNull

                    val watchedEpsMap = mutableMapOf<String, MutableSet<Int>>()
                    val directEpsObj = obj["watched_episodes"]?.jsonObject ?: obj["watchedEpisodes"]?.jsonObject ?: itemObj["watched_episodes"]?.jsonObject ?: itemObj["watchedEpisodes"]?.jsonObject
                    directEpsObj?.entries?.forEach { (sNum, epsVal) ->
                        val epNums = epsVal.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull?.toIntOrNull() ?: (it as? JsonObject)?.get("number")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: (it as? JsonObject)?.get("episode")?.jsonPrimitive?.contentOrNull?.toIntOrNull() }
                        if (!epNums.isNullOrEmpty()) watchedEpsMap.getOrPut(sNum) { mutableSetOf() }.addAll(epNums)
                    }
                    val seasonsArr = obj["seasons"]?.jsonArray ?: itemObj["seasons"]?.jsonArray
                    seasonsArr?.forEach { sEl ->
                        val sObj = sEl as? JsonObject
                        val sNum = sObj?.get("number")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: sObj?.get("season")?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                        if (sNum != null && sNum > 0) {
                            val epsArr = sObj?.get("episodes")?.jsonArray
                            val epNums = epsArr?.mapNotNull { epEl ->
                                val epObj = epEl as? JsonObject
                                val isEpWatched = epObj?.get("watched")?.jsonPrimitive?.booleanOrNull ?: epObj?.get("seen")?.jsonPrimitive?.booleanOrNull ?: true
                                if (isEpWatched) {
                                    epObj?.get("number")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: epObj?.get("episode")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: (epEl as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.toIntOrNull()
                                } else null
                            }
                            if (!epNums.isNullOrEmpty()) watchedEpsMap.getOrPut(sNum.toString()) { mutableSetOf() }.addAll(epNums)
                        }
                    }
                    val epSeasonNum = obj["season_number"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: obj["season"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: itemObj["season_number"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: itemObj["season"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    val epNum = obj["episode_number"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: obj["episode"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: itemObj["episode_number"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: itemObj["episode"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    if (epSeasonNum != null && epSeasonNum > 0 && epNum != null && epNum > 0) {
                        watchedEpsMap.getOrPut(epSeasonNum.toString()) { mutableSetOf() }.add(epNum)
                    }
                    val finalWatchedEps = if (watchedEpsMap.isNotEmpty()) watchedEpsMap.mapValues { it.value.sorted() } else null

                    val rawWatchedDate = obj["watched_at"]?.jsonPrimitive?.contentOrNull
                        ?: obj["watchedAt"]?.jsonPrimitive?.contentOrNull
                        ?: obj["watched_date"]?.jsonPrimitive?.contentOrNull
                        ?: obj["date"]?.jsonPrimitive?.contentOrNull
                        ?: obj["log_date"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["watched_at"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["watchedAt"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["watched_date"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["date"]?.jsonPrimitive?.contentOrNull
                        ?: itemObj["log_date"]?.jsonPrimitive?.contentOrNull
                    val parsedWatchedDate = parseAndNormalizeWatchedDate(rawWatchedDate)

                    async {
                        try {
                            if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt")) {
                                val tmdbMovie = movieRepository.findByImdbId(imdbId)
                                tmdbMovie?.let {
                                    val mediaType = if (finalWatchedEps != null || typeStr.contains("tv", ignoreCase = true) || it.mediaType == "tv") "tv" else "movie"
                                    val m = Movie(
                                        id = it.id,
                                        mediaType = mediaType,
                                        title = it.title,
                                        name = it.name,
                                        watched = isSeen,
                                        favorite = isFav,
                                        personalRating = userRating,
                                        personalNote = userNote,
                                        watchedEpisodes = finalWatchedEps,
                                        watchedAt = if (isSeen) (parsedWatchedDate ?: java.time.Instant.now().toString()) else null,
                                        syncStatus = "pending",
                                        clientUpdatedAt = System.currentTimeMillis()
                                    )
                                    Pair(m, folderName)
                                }
                            } else if (tmdbId != null && tmdbId > 0) {
                                val mediaType = if (finalWatchedEps != null || typeStr.contains("tv", ignoreCase = true) || typeStr.contains("show", ignoreCase = true)) "tv" else "movie"
                                val m = Movie(
                                    id = tmdbId,
                                    mediaType = mediaType,
                                    title = title ?: "Unknown ($tmdbId)",
                                    name = if (mediaType == "tv") title else null,
                                    watched = isSeen,
                                    favorite = isFav,
                                    personalRating = userRating,
                                    personalNote = userNote,
                                    watchedEpisodes = finalWatchedEps,
                                    watchedAt = if (isSeen) (parsedWatchedDate ?: java.time.Instant.now().toString()) else null,
                                    syncStatus = "pending",
                                    clientUpdatedAt = System.currentTimeMillis()
                                )
                                Pair(m, folderName)
                            } else if (!title.isNullOrBlank()) {
                                val isTvMedia = finalWatchedEps != null || typeStr.contains("tv", ignoreCase = true) || typeStr.contains("show", ignoreCase = true) || typeStr.contains("series", ignoreCase = true)
                                val tmdbMovie = movieRepository.searchMediaWithYear(title, year, isTv = isTvMedia)
                                tmdbMovie?.let {
                                    val mediaType = if (finalWatchedEps != null || it.mediaType == "tv") "tv" else (it.mediaType ?: "movie")
                                    val m = Movie(
                                        id = it.id,
                                        mediaType = mediaType,
                                        title = it.title,
                                        name = it.name,
                                        watched = isSeen,
                                        favorite = isFav,
                                        personalRating = userRating,
                                        personalNote = userNote,
                                        watchedEpisodes = finalWatchedEps,
                                        watchedAt = if (isSeen) (parsedWatchedDate ?: java.time.Instant.now().toString()) else null,
                                        syncStatus = "pending",
                                        clientUpdatedAt = System.currentTimeMillis()
                                    )
                                    Pair(m, folderName)
                                }
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                val results = deferreds.awaitAll().filterNotNull()
                if (results.isNotEmpty()) {
                    processAndSaveImportedItems(results, keepLatestWatchDate, onProgress)
                    count += results.size
                }
                kotlinx.coroutines.delay(300)
            }
        } catch (e: Exception) {
            android.util.Log.e("Migrazione", "Fallito Universal JSON", e)
        }
        
        count
    }

    suspend fun migrateZipStream(
        inputStream: InputStream,
        keepLatestWatchDate: Boolean = true,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        var totalCount = 0
        java.util.zip.ZipInputStream(inputStream).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name.lowercase()
                    if (!name.contains("__macosx") && !name.startsWith(".") && !name.substringAfterLast("/").startsWith(".")) {
                        if (name.endsWith(".json") || name.endsWith(".csv") || name.endsWith(".txt") || !name.contains(".")) {
                            try {
                                val entryBytes = zipStream.readBytes()
                                if (entryBytes.isNotEmpty()) {
                                    val contentStart = entryBytes.decodeToString().take(100).trimStart()
                                    val isJson = contentStart.startsWith("[") || contentStart.startsWith("{")
                                    val count = if (isJson) {
                                        java.io.ByteArrayInputStream(entryBytes).use { 
                                            migrateTraktStream(it, keepLatestWatchDate, onProgress) 
                                        }
                                    } else {
                                        java.io.ByteArrayInputStream(entryBytes).use { 
                                            migrateCsvStream(it, keepLatestWatchDate, onProgress) 
                                        }
                                    }
                                    totalCount += count
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("BackupRepository", "Error parsing zip entry: ${entry.name}", e)
                            }
                        }
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }
        totalCount
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

    private fun parseAndNormalizeWatchedDate(rawDate: String?): String? {
        if (rawDate.isNullOrBlank()) return null
        val trimmed = rawDate.trim()
        try {
            java.time.Instant.parse(trimmed)
            return trimmed
        } catch (_: Exception) {}

        try {
            val cleanDate = trimmed.replace("/", "-").take(10)
            val localDate = java.time.LocalDate.parse(cleanDate)
            return localDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toString()
        } catch (_: Exception) {}

        try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val localDateTime = java.time.LocalDateTime.parse(trimmed, formatter)
            return localDateTime.toInstant(java.time.ZoneOffset.UTC).toString()
        } catch (_: Exception) {}

        return null
    }

    suspend fun migrateCsvStream(
        inputStream: InputStream,
        keepLatestWatchDate: Boolean = true,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        inputStream.bufferedReader().useLines { linesSequence ->
            val lines = linesSequence.filter { it.isNotBlank() }.iterator()
            if (!lines.hasNext()) return@withContext 0
            val headerLine = lines.next()
            val headers = parseCsvLine(headerLine)
            val lowerHeaders = headers.map { it.trim().lowercase() }

            // Common Headers
            val imdbIdx = lowerHeaders.indexOfFirst { it in listOf("const", "imdb id", "imdb_id", "imdbid", "imdb", "id", "title_id", "imdb_uri", "tconst") }
            // `letterboxd uri` excluded: it's a URL, handled separately below to extract slug as title fallback
            val titleIdx = lowerHeaders.indexOfFirst { it in listOf("title", "name", "movie title", "show name", "film", "series", "show_name", "movie_name", "item_name", "movie", "show", "original title", "show title", "episode title", "original_title", "show_title") }
            val letterboxdUriIdx = lowerHeaders.indexOfFirst { it == "letterboxd uri" }
            val yearIdx = lowerHeaders.indexOfFirst { it in listOf("year", "release year", "release_year", "date_released", "date released", "release date") }
                .takeIf { it != -1 }
                ?: lowerHeaders.indexOfFirst { (it == "date" || it == "created date") && !lowerHeaders.contains("watched date") && !lowerHeaders.contains("watched_date") && !lowerHeaders.contains("watched_at") }
            val watchedDateIdx = lowerHeaders.indexOfFirst { it in listOf("watched date", "watched_date", "watched_at", "date watched", "date_watched", "log date", "diary date", "date rated", "date_rated") }
                .takeIf { it != -1 }
                ?: lowerHeaders.indexOfFirst { (it == "date" || it == "created date" || it == "created_at") && !lowerHeaders.contains("year") && !lowerHeaders.contains("release year") && !lowerHeaders.contains("release_year") }
                .takeIf { it != -1 }
                ?: lowerHeaders.indexOfFirst { it == "date" }
            val typeIdx = lowerHeaders.indexOfFirst { it in listOf("title type", "type", "media_type", "item_type", "contenttype") }
            val ratingIdx = lowerHeaders.indexOfFirst { it in listOf("rating", "your rating", "my rating", "score", "user rating", "rating10", "rating5", "user_rating", "personal_rating") }
            val noteIdx = lowerHeaders.indexOfFirst { it in listOf("review", "comment", "note", "personal_note", "your review", "my review", "notes") }
            val watchedIdx = lowerHeaders.indexOfFirst { it in listOf("watched", "seen", "status", "watched status", "watched_status") }
            val seasonIdx = lowerHeaders.indexOfFirst { it in listOf("season", "season number", "season_number", "s", "season_num") }
            val episodeIdx = lowerHeaders.indexOfFirst { it in listOf("episode", "episode number", "episode_number", "e", "episode_num", "ep") }
            val folderIdx = lowerHeaders.indexOfFirst { it in listOf("folder", "list", "list name", "list_name", "tag", "playlist", "collection") }
            
            // Yamtrack-specific Headers
            val yamtrackMediaIdIdx = lowerHeaders.indexOfFirst { it == "media_id" }
            val yamtrackSourceIdx  = lowerHeaders.indexOfFirst { it == "source" }

            // Universal TMDB ID - prioritize Yamtrack's `media_id` if present
            val tmdbIdx = if (yamtrackMediaIdIdx != -1) yamtrackMediaIdIdx 
                          else lowerHeaders.indexOfFirst { it in listOf("tmdb id", "tmdb_id", "tmdbid", "tmdb", "movie_id", "show_id", "id_movie", "movieid") }
            
            val isYamtrack = yamtrackMediaIdIdx != -1
            
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
                    
                    // Yamtrack source check
                    if (isYamtrack) {
                        val source = if (yamtrackSourceIdx != -1 && columns.size > yamtrackSourceIdx) columns[yamtrackSourceIdx].trim().lowercase() else ""
                        if (source.isNotBlank() && source != "tmdb") return@mapNotNull null
                    }
                    
                    val imdbVal = if (imdbIdx != -1 && columns.size > imdbIdx) columns[imdbIdx] 
                                  else columns.firstOrNull { it.trim().startsWith("tt") }
                    val tmdbVal = if (tmdbIdx != -1 && columns.size > tmdbIdx) columns[tmdbIdx]?.toLongOrNull() else null
                    val titleVal = if (titleIdx != -1 && columns.size > titleIdx) columns[titleIdx] else null
                    val yearVal = if (yearIdx != -1 && columns.size > yearIdx) columns[yearIdx] else null
                    val rawWatchedDateVal = if (watchedDateIdx != -1 && columns.size > watchedDateIdx) columns[watchedDateIdx] else null
                    val parsedWatchedDate = parseAndNormalizeWatchedDate(rawWatchedDateVal)
                    val ratingVal = if (ratingIdx != -1 && columns.size > ratingIdx) columns[ratingIdx]?.toDoubleOrNull() else null
                    val noteVal = if (noteIdx != -1 && columns.size > noteIdx) columns[noteIdx]?.take(5000) else null
                    
                    var typeVal = if (typeIdx != -1 && columns.size > typeIdx) columns[typeIdx] else "movie"
                    if (isYamtrack) {
                        typeVal = when (typeVal.lowercase()) {
                            "movie" -> "movie"
                            "tv", "season", "episode" -> "tv"
                            else -> "movie" // Fallback, though invalid types are skipped
                        }
                    }

                    var watchedVal = if (watchedIdx != -1 && columns.size > watchedIdx) {
                        val w = columns[watchedIdx]?.trim()?.lowercase()
                        w == "1" || w == "true" || w == "yes" || w == "watched" || w == "completed"
                    } else true

                    if (isYamtrack && watchedIdx != -1 && columns.size > watchedIdx) {
                        watchedVal = when (columns[watchedIdx]) {
                            "Completed" -> true
                            "Planned", "Watching" -> false
                            "Dropped", "Paused" -> return@mapNotNull null // Skip
                            else -> false
                        }
                    }
                    
                    val seasonVal = if (seasonIdx != -1 && columns.size > seasonIdx) columns[seasonIdx]?.toIntOrNull() else null
                    val episodeVal = if (episodeIdx != -1 && columns.size > episodeIdx) columns[episodeIdx]?.toIntOrNull() else null
                    val epsMap = if (seasonVal != null && seasonVal > 0 && episodeVal != null && episodeVal > 0) {
                        mapOf(seasonVal.toString() to listOf(episodeVal))
                    } else null
                    val folderVal = if (folderIdx != -1 && columns.size > folderIdx) columns[folderIdx]?.takeIf { it.isNotBlank() } else null
                    
                    async {
                        try {
                            if (!imdbVal.isNullOrBlank() && imdbVal.trim().startsWith("tt")) {
                                val tmdbMovie = movieRepository.findByImdbId(imdbVal.trim())
                                tmdbMovie?.let {
                                    val mediaType = if (epsMap != null || typeVal.contains("tv", ignoreCase = true) || typeVal.contains("series", ignoreCase = true) || it.mediaType == "tv") "tv" else "movie"
                                    val m = Movie(
                                        id = it.id,
                                        mediaType = mediaType,
                                        title = it.title,
                                        name = it.name,
                                        watched = watchedVal,
                                        favorite = false,
                                        personalRating = ratingVal,
                                        personalNote = noteVal,
                                        watchedEpisodes = epsMap,
                                        watchedAt = if (watchedVal) (parsedWatchedDate ?: java.time.Instant.now().toString()) else null,
                                        syncStatus = "pending",
                                        clientUpdatedAt = System.currentTimeMillis()
                                    )
                                    Pair(m, folderVal)
                                }
                            } else if (tmdbVal != null && tmdbVal > 0) {
                                val mediaType = if (epsMap != null || typeVal.contains("tv", ignoreCase = true) || typeVal.contains("series", ignoreCase = true) || typeVal.contains("show", ignoreCase = true)) "tv" else "movie"
                                val m = Movie(
                                    id = tmdbVal,
                                    mediaType = mediaType,
                                    title = titleVal ?: "Unknown ($tmdbVal)",
                                    name = if (mediaType == "tv") titleVal else null,
                                    watched = watchedVal,
                                    favorite = false,
                                    personalRating = ratingVal,
                                    personalNote = noteVal,
                                    watchedEpisodes = epsMap,
                                    watchedAt = if (watchedVal) (parsedWatchedDate ?: java.time.Instant.now().toString()) else null,
                                    syncStatus = "pending",
                                    clientUpdatedAt = System.currentTimeMillis()
                                )
                                Pair(m, folderVal)
                            } else if (!titleVal.isNullOrBlank()) {
                                val isTvMedia = epsMap != null || typeVal.contains("tv", ignoreCase = true) || typeVal.contains("series", ignoreCase = true) || typeVal.contains("show", ignoreCase = true) || typeVal.contains("episode", ignoreCase = true)
                                val tmdbMovie = movieRepository.searchMediaWithYear(titleVal.trim(), yearVal?.trim(), isTv = isTvMedia)
                                tmdbMovie?.let {
                                    val mediaType = if (epsMap != null || it.mediaType == "tv") "tv" else (it.mediaType ?: "movie")
                                    val m = Movie(
                                        id = it.id,
                                        mediaType = mediaType,
                                        title = it.title,
                                        name = it.name,
                                        watched = watchedVal,
                                        favorite = false,
                                        personalRating = ratingVal,
                                        personalNote = noteVal,
                                        watchedEpisodes = epsMap,
                                        watchedAt = if (watchedVal) (parsedWatchedDate ?: java.time.Instant.now().toString()) else null,
                                        syncStatus = "pending",
                                        clientUpdatedAt = System.currentTimeMillis()
                                    )
                                    Pair(m, folderVal)
                                }
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                
                val results = deferreds.awaitAll().filterNotNull()
                if (results.isNotEmpty()) {
                    processAndSaveImportedItems(results, keepLatestWatchDate, onProgress)
                    count += results.size
                }
                kotlinx.coroutines.delay(350)
            }
        }
        count
    }

    private suspend fun processAndSaveImportedItems(
        itemsWithFolders: List<Pair<Movie, String?>>,
        keepLatestWatchDate: Boolean = true,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ) {
        if (itemsWithFolders.isEmpty()) return
        
        // 1. Raggruppa per (id, mediaType) unificando puntate, voti e note tra righe multiple
        val mergedIncoming = mutableMapOf<Pair<Long, String>, Movie>()
        val itemFolderMap = mutableMapOf<String, MutableSet<String>>() // folderName -> set of compositeIds

        for ((m, folderName) in itemsWithFolders) {
            val key = Pair(m.id, m.mediaType ?: "movie")
            val compositeId = "${key.second}_${key.first}"
            if (!folderName.isNullOrBlank()) {
                itemFolderMap.getOrPut(folderName.trim()) { mutableSetOf() }.add(compositeId)
            }

            val existing = mergedIncoming[key]
            if (existing == null) {
                mergedIncoming[key] = m
            } else {
                val combinedEps = mutableMapOf<String, MutableSet<Int>>()
                existing.watchedEpisodes?.forEach { (s, eps) -> combinedEps.getOrPut(s) { mutableSetOf() }.addAll(eps) }
                m.watchedEpisodes?.forEach { (s, eps) -> combinedEps.getOrPut(s) { mutableSetOf() }.addAll(eps) }
                val newEpsMap = if (combinedEps.isNotEmpty()) combinedEps.mapValues { it.value.sorted() } else null

                val bestWatchedAt = when {
                    existing.watchedAt != null && m.watchedAt != null -> if (keepLatestWatchDate) {
                        if (existing.watchedAt!! > m.watchedAt!!) existing.watchedAt else m.watchedAt
                    } else {
                        if (existing.watchedAt!! < m.watchedAt!!) existing.watchedAt else m.watchedAt
                    }
                    else -> existing.watchedAt ?: m.watchedAt
                }

                mergedIncoming[key] = existing.copy(
                    watched = existing.watched || m.watched,
                    favorite = existing.favorite || m.favorite,
                    personalRating = existing.personalRating ?: m.personalRating,
                    personalNote = (if (!existing.personalNote.isNullOrBlank()) existing.personalNote else m.personalNote)?.take(5000),
                    watchedEpisodes = newEpsMap,
                    watchedAt = bestWatchedAt
                )
            }
        }

        // 2. Unisci con il database locale e salva in favoriteDao
        val totalToSave = mergedIncoming.size
        var currentSaved = 0
        for (incoming in mergedIncoming.values) {
            currentSaved++
            if (currentSaved % 5 == 0 || currentSaved == totalToSave) {
                onProgress(currentSaved, totalToSave)
            }
            val local = favoriteDao.getById(incoming.id, incoming.mediaType ?: "movie")
            if (local == null) {
                favoriteDao.insert(incoming)
            } else {
                val combinedEps = mutableMapOf<String, MutableSet<Int>>()
                local.watchedEpisodes?.forEach { (s, eps) -> combinedEps.getOrPut(s) { mutableSetOf() }.addAll(eps) }
                incoming.watchedEpisodes?.forEach { (s, eps) -> combinedEps.getOrPut(s) { mutableSetOf() }.addAll(eps) }
                val newEpsMap = if (combinedEps.isNotEmpty()) combinedEps.mapValues { it.value.sorted() } else local.watchedEpisodes

                val bestWatchedAt = when {
                    local.watchedAt != null && incoming.watchedAt != null -> if (keepLatestWatchDate) {
                        if (local.watchedAt!! > incoming.watchedAt!!) local.watchedAt else incoming.watchedAt
                    } else {
                        if (local.watchedAt!! < incoming.watchedAt!!) local.watchedAt else incoming.watchedAt
                    }
                    else -> local.watchedAt ?: incoming.watchedAt
                }

                val updated = local.copy(
                    watched = local.watched || incoming.watched,
                    favorite = local.favorite || incoming.favorite,
                    personalRating = local.personalRating ?: incoming.personalRating,
                    personalNote = (if (!local.personalNote.isNullOrBlank()) local.personalNote else incoming.personalNote)?.take(5000),
                    watchedEpisodes = newEpsMap,
                    watchedAt = bestWatchedAt,
                    clientUpdatedAt = System.currentTimeMillis(),
                    syncStatus = if (local.syncStatus == "synced") "pending_update" else local.syncStatus
                )
                favoriteDao.insert(updated)
            }
        }

        // 3. Creazione / Aggiornamento Cartelle (Folders / Liste personalizzate)
        if (itemFolderMap.isNotEmpty()) {
            val existingFolders = folderDao.getAll().associateBy { it.name.trim().lowercase() }
            for ((folderName, newIds) in itemFolderMap) {
                val existingFolder = existingFolders[folderName.lowercase()]
                if (existingFolder != null) {
                    val mergedIds = (existingFolder.itemIds + newIds).distinct()
                    if (mergedIds.size != existingFolder.itemIds.size) {
                        folderDao.insert(existingFolder.copy(
                            itemIds = mergedIds,
                            updatedAt = java.time.Instant.now().toString(),
                            syncStatus = "pending"
                        ))
                    }
                } else {
                    val newFolder = FolderEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        name = folderName,
                        icon = "folder",
                        color = "#6200EE",
                        description = "Imported list: $folderName",
                        itemIds = newIds.toList(),
                        createdAt = java.time.Instant.now().toString(),
                        updatedAt = java.time.Instant.now().toString(),
                        syncStatus = "pending"
                    )
                    folderDao.insert(newFolder)
                }
            }
        }
    }
}
