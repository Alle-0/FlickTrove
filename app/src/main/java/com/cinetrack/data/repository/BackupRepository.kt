package com.cinetrack.data.repository

import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.local.dao.FolderDao
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.model.BackupData
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.importers.TraktJsonImporter
import com.cinetrack.data.repository.importers.TraktZipImporter
import com.cinetrack.data.repository.importers.TvTimeGdprImporter
import com.cinetrack.data.repository.importers.UniversalCsvImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val folderDao: FolderDao,
    private val preferenceRepository: PreferenceRepository,
    private val traktJsonImporter: TraktJsonImporter,
    private val traktZipImporter: TraktZipImporter,
    private val universalCsvImporter: UniversalCsvImporter,
    private val tvTimeGdprImporter: TvTimeGdprImporter
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
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
        fileName: String? = null
    ): Int = traktJsonImporter.migrateTraktStream(
        inputStream = inputStream,
        keepLatestWatchDate = keepLatestWatchDate,
        onProgress = onProgress,
        fileName = fileName
    ) { items, keepLatest, cb ->
        processAndSaveImportedItems(items, keepLatest, cb)
    }

    private suspend fun migrateTvTimeGdprZip(
        zipEntries: Map<String, ByteArray>,
        keepLatestWatchDate: Boolean,
        onProgress: suspend (Int, Int) -> Unit
    ): Int = tvTimeGdprImporter.migrateTvTimeGdprZip(
        zipEntries = zipEntries,
        keepLatestWatchDate = keepLatestWatchDate,
        onProgress = onProgress
    ) { items, keepLatest, cb ->
        processAndSaveImportedItems(items, keepLatest, cb)
    }

    suspend fun migrateZipStream(
        inputStream: InputStream,
        keepLatestWatchDate: Boolean = true,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val zipEntries = mutableMapOf<String, ByteArray>()
        java.util.zip.ZipInputStream(inputStream).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name.lowercase().substringAfterLast("/")
                    if (!name.contains("__macosx") && !name.startsWith(".")) {
                        if (name.endsWith(".json") || name.endsWith(".csv") || name.endsWith(".txt") || !name.contains(".")) {
                            try {
                                val bytes = zipStream.readBytes()
                                if (bytes.isNotEmpty()) {
                                    zipEntries[name] = bytes
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("BackupRepository", "Error reading zip entry: ${entry.name}", e)
                            }
                        }
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }

        val isTvTimeGdpr = zipEntries.keys.any { 
            it == "followed_tv_show.csv" || it == "tracking-prod-records-v2.csv" || it == "tracking-prod-records.csv" || it == "user_show_special_status.csv" || it == "user_tv_show_data.csv" || it == "seen_episodes.csv" || it == "lists-prod-lists.csv" || it == "comments-prod-comments.csv"
        }

        if (isTvTimeGdpr) {
            return@withContext migrateTvTimeGdprZip(zipEntries, keepLatestWatchDate, onProgress)
        }

        // Dedicated Trakt export handler: processes each file with the correct semantics
        if (traktZipImporter.isTraktExport(zipEntries.keys)) {
            return@withContext traktZipImporter.import(zipEntries) { items ->
                processAndSaveImportedItems(items, keepLatestWatchDate, onProgress)
            }
        }

        var totalCount = 0
        for ((name, entryBytes) in zipEntries) {
            try {
                val contentStart = entryBytes.decodeToString().take(100).trimStart()
                val isJson = contentStart.startsWith("[") || contentStart.startsWith("{")
                val count = if (isJson) {
                    java.io.ByteArrayInputStream(entryBytes).use { 
                        migrateTraktStream(it, keepLatestWatchDate, onProgress, fileName = name) 
                    }
                } else {
                    java.io.ByteArrayInputStream(entryBytes).use { 
                        migrateCsvStream(it, keepLatestWatchDate, onProgress, fileName = name) 
                    }
                }
                totalCount += count
            } catch (e: Exception) {
                android.util.Log.e("BackupRepository", "Error parsing zip entry: $name", e)
            }
        }
        totalCount
    }

    suspend fun migrateCsvStream(
        inputStream: InputStream,
        keepLatestWatchDate: Boolean = true,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
        fileName: String? = null
    ): Int = universalCsvImporter.migrateCsvStream(
        inputStream = inputStream,
        keepLatestWatchDate = keepLatestWatchDate,
        onProgress = onProgress,
        fileName = fileName
    ) { items, keepLatest, cb ->
        processAndSaveImportedItems(items, keepLatest, cb)
    }

    internal suspend fun processAndSaveImportedItems(
        itemsWithFolders: List<Pair<Movie, String?>>,
        keepLatestWatchDate: Boolean = true,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val mergedIncoming = mutableMapOf<String, Movie>()
        val itemFolderMap = mutableMapOf<String, MutableSet<String>>()

        // 1. Unisci i duplicati all'interno dell'importazione stessa
        for ((m, folder) in itemsWithFolders) {
            val key = "${m.mediaType ?: "movie"}_${m.id}"
            if (folder != null && folder.isNotBlank()) {
                itemFolderMap.getOrPut(folder.trim()) { mutableSetOf() }.add(key)
            }

            val existing = mergedIncoming[key]
            if (existing == null) {
                mergedIncoming[key] = m
            } else {
                val combinedEps = mutableMapOf<String, MutableSet<Int>>()
                existing.watchedEpisodes?.forEach { (s, eps) -> combinedEps.getOrPut(s) { mutableSetOf() }.addAll(eps) }
                m.watchedEpisodes?.forEach { (s, eps) -> combinedEps.getOrPut(s) { mutableSetOf() }.addAll(eps) }
                val newEpsMap = if (combinedEps.isNotEmpty()) combinedEps.mapValues { it.value.sorted() } else existing.watchedEpisodes

                val exWatched = existing.watchedAt
                val mWatched = m.watchedAt
                val bestWatchedAt = when {
                    exWatched != null && mWatched != null -> if (keepLatestWatchDate) {
                        if (exWatched > mWatched) exWatched else mWatched
                    } else {
                        if (exWatched < mWatched) exWatched else mWatched
                    }
                    else -> exWatched ?: mWatched
                }

                mergedIncoming[key] = existing.copy(
                    posterPath = existing.posterPath ?: m.posterPath,
                    backdropPath = existing.backdropPath ?: m.backdropPath,
                    voteAverage = if (existing.voteAverage == 0.0 && m.voteAverage != 0.0) m.voteAverage else existing.voteAverage,
                    overview = existing.overview ?: m.overview,
                    releaseDate = existing.releaseDate ?: m.releaseDate,
                    firstAirDate = existing.firstAirDate ?: m.firstAirDate,
                    genreIds = existing.genreIds ?: m.genreIds,
                    watched = existing.watched || m.watched,
                    favorite = existing.favorite || m.favorite,
                    dropped = existing.dropped || m.dropped,
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

                val locWatched = local.watchedAt
                val incWatched = incoming.watchedAt
                val bestWatchedAt = when {
                    locWatched != null && incWatched != null -> if (keepLatestWatchDate) {
                        if (locWatched > incWatched) locWatched else incWatched
                    } else {
                        if (locWatched < incWatched) locWatched else incWatched
                    }
                    else -> locWatched ?: incWatched
                }

                val updated = local.copy(
                    posterPath = local.posterPath ?: incoming.posterPath,
                    backdropPath = local.backdropPath ?: incoming.backdropPath,
                    voteAverage = if (local.voteAverage == 0.0 && incoming.voteAverage != 0.0) incoming.voteAverage else local.voteAverage,
                    overview = local.overview ?: incoming.overview,
                    releaseDate = local.releaseDate ?: incoming.releaseDate,
                    firstAirDate = local.firstAirDate ?: incoming.firstAirDate,
                    genreIds = local.genreIds ?: incoming.genreIds,
                    watched = local.watched || incoming.watched,
                    favorite = local.favorite || incoming.favorite,
                    dropped = local.dropped || incoming.dropped,
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
