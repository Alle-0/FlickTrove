package com.cinetrack.data.repository

import com.cinetrack.data.Movie
import com.cinetrack.data.api.OmdbService
import com.cinetrack.data.api.TraktService
import com.cinetrack.data.api.Person
import com.cinetrack.data.api.PersonSearchResult
import com.cinetrack.data.models.Season
import com.cinetrack.data.api.TMDBService
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.models.ExtraRatings
import com.cinetrack.data.local.dao.CacheDao
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.local.dao.FolderDao
import com.cinetrack.data.local.dao.SearchHistoryDao
import com.cinetrack.data.remote.FirebaseRemoteDataSource
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.local.entities.SearchHistoryEntity
import com.cinetrack.data.models.Folder
import com.cinetrack.data.sync.SyncProgress
import com.cinetrack.domain.UpdateEpisodesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import java.util.concurrent.TimeUnit
import com.cinetrack.worker.TraktInstantWriteWorker

@Singleton
class MovieRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val folderDao: FolderDao,
    private val cacheDao: CacheDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val tmdbService: TMDBService,
    private val omdbService: OmdbService,
    private val traktService: TraktService,
    private val firebaseRemoteDataSource: FirebaseRemoteDataSource,
    private val preferenceRepository: PreferenceRepository,
    @Named("tmdb_api_key") private val apiKey: String,
    @Named("omdb_api_key") private val omdbApiKey: String,
    @Named("trakt_api_key") private val traktApiKey: String,
    private val widgetNotifier: com.cinetrack.domain.WidgetNotifier,
    @ApplicationContext private val context: Context
) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- Local Operations (Bunker: Room First) ---
    suspend fun getLocalMovies(): List<Movie> = favoriteDao.getAll()
    
    fun getLocalMoviesFlow(): Flow<List<Movie>> = favoriteDao.getAllFlow()

    suspend fun searchLocalMovies(query: String, mediaType: String = ""): List<Movie> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()

        val escaped = "%${normalized.replace("'", "''")}%"
        val results = favoriteDao.searchLocalMovies(escaped, mediaType)

        if (results.isNotEmpty()) return results

        return favoriteDao.getAll()
            .filter { mediaType.isEmpty() || it.mediaType == mediaType }
            .map { movie -> movie to com.cinetrack.ui.utils.FuzzySearch.score(normalized, movie.title ?: movie.name ?: "") }
            .filter { it.second >= 0.45 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun getMovieFlow(id: Long, mediaType: String): Flow<Movie?> = favoriteDao.getByIdFlow(id, mediaType)

    suspend fun getMovie(id: Long, mediaType: String): Movie? = favoriteDao.getById(id, mediaType)

    suspend fun getShowsForUpdate(limit: Int = 150): List<Movie> = favoriteDao.getShowsForUpdate(limit)

    suspend fun getUpcomingMoviesForUpdate(limit: Int = 150): List<Movie> = favoriteDao.getUpcomingMoviesForUpdate(limit)

    suspend fun saveMovie(movie: Movie, syncToTrakt: Boolean = true) {
        val oldMovie = favoriteDao.getById(movie.id, movie.mediaType)

        // 1. Update Room immediately
        favoriteDao.insert(movie.copy(syncStatus = "synced", clientUpdatedAt = System.currentTimeMillis()))
        
        // Notify Widget of changes
        widgetNotifier.notifyWidgetUpdated()

        // Enqueue Instant Write to Trakt via WorkManager
        if (syncToTrakt) {
            val workRequests = mutableListOf<androidx.work.OneTimeWorkRequest>()

            fun enqueue(action: String, extras: androidx.work.Data.Builder.() -> Unit = {}) {
                val builder = androidx.work.Data.Builder()
                    .putString(TraktInstantWriteWorker.KEY_ACTION,     action)
                    .putString(TraktInstantWriteWorker.KEY_MEDIA_TYPE, movie.mediaType)
                    .putLong(  TraktInstantWriteWorker.KEY_TMDB_ID,    movie.id)
                if (movie.imdbId != null) {
                    builder.putString(TraktInstantWriteWorker.KEY_IMDB_ID, movie.imdbId)
                }
                builder.apply(extras)
                workRequests += OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                    .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(builder.build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                    .build()
            }

            // Estraiamo i vecchi valori in sicurezza (se oldMovie è null, usiamo i default)
            val oldWatched = oldMovie?.watched ?: false
            val oldRating  = oldMovie?.personalRating
            val oldFav     = oldMovie?.favorite ?: false

            // watched changed (o nuovo inserimento già visto)
            if (oldWatched != movie.watched) {
                if (movie.mediaType == "tv") {
                    // FIX: Per le serie TV, non "nuclearizzare" lo show se scende al 99%.
                    // Rimuovi l'intero show SOLO se tutti gli episodi sono stati tolti.
                    if (movie.watched) {
                        enqueue(TraktInstantWriteWorker.ACTION_MARK_WATCHED)
                    } else if (movie.watchedEpisodes.isNullOrEmpty()) {
                        enqueue(TraktInstantWriteWorker.ACTION_REMOVE_WATCHED)
                    }
                } else {
                    // I film funzionano in modo standard (acceso/spento)
                    val action = if (movie.watched) TraktInstantWriteWorker.ACTION_MARK_WATCHED
                                 else               TraktInstantWriteWorker.ACTION_REMOVE_WATCHED
                    enqueue(action)
                }
            }

            // personalRating changed
            if (oldRating != movie.personalRating) {
                val rating = movie.personalRating
                if (rating != null && rating > 0.0) {
                    val traktRating = rating.toInt().coerceIn(1, 10)
                    enqueue(TraktInstantWriteWorker.ACTION_ADD_RATING) {
                        putInt(TraktInstantWriteWorker.KEY_RATING, traktRating)
                    }
                } else {
                    enqueue(TraktInstantWriteWorker.ACTION_REMOVE_RATING)
                }
            }

            // favorite changed (watchlist)
            if (oldFav != movie.favorite) {
                val action = if (movie.favorite) TraktInstantWriteWorker.ACTION_ADD_WATCHLIST
                             else                TraktInstantWriteWorker.ACTION_REMOVE_WATCHLIST
                enqueue(action)
            }

            // watchedEpisodes changed (TV only)
            if (movie.mediaType == "tv" && oldMovie?.watchedEpisodes != movie.watchedEpisodes) {
                val oldEps    = oldMovie?.watchedEpisodes ?: emptyMap()
                val newEps    = movie.watchedEpisodes    ?: emptyMap()

                // Episodes added
                val addedEps  = newEps.mapValues { (season, eps) ->
                    eps - (oldEps[season]?.toSet() ?: emptySet())
                }.filter { it.value.isNotEmpty() }

                // Episodes removed
                val removedEps = oldEps.mapValues { (season, eps) ->
                    eps - (newEps[season]?.toSet() ?: emptySet())
                }.filter { it.value.isNotEmpty() }

                if (addedEps.isNotEmpty()) {
                    val encoded = TraktInstantWriteWorker.encodeEpisodes(addedEps)
                    enqueue(TraktInstantWriteWorker.ACTION_MARK_EPISODES_WATCHED) {
                        putString(TraktInstantWriteWorker.KEY_SEASON_EPISODES, encoded)
                    }
                }
                if (removedEps.isNotEmpty()) {
                    val encoded = TraktInstantWriteWorker.encodeEpisodes(removedEps)
                    enqueue(TraktInstantWriteWorker.ACTION_REMOVE_EPISODES_WATCHED) {
                        putString(TraktInstantWriteWorker.KEY_SEASON_EPISODES, encoded)
                    }
                }
            }

            if (workRequests.isNotEmpty()) {
                WorkManager.getInstance(context).enqueue(workRequests)
            }
        }
        
        // 2. Fire-and-forget to Firebase (SDK handles persistence/retry)
        repositoryScope.launch {
            try {
                firebaseRemoteDataSource.setMovie(movie)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Firebase sync failed for ${movie.id}", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    favoriteDao.updateSyncStatus(movie.id, movie.mediaType, "pending")
                }
            }
        }
    }

    fun fetchMissingDetailsAsync(movie: Movie) {
        if (movie.runtime == null || movie.runtime == 0 || movie.topCastData.isNullOrEmpty()) {
            repositoryScope.launch {
                try {
                    val isTv = movie.mediaType == "tv"
                    val response = fetchMovieDetails(movie.id, isTv)
                    val freshMovie = com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(response, if (isTv) "tv" else "movie")
                    
                    favoriteDao.updateMetadata(
                        id = movie.id,
                        mediaType = movie.mediaType,
                        runtime = freshMovie.runtime,
                        episodeRunTime = freshMovie.episodeRunTime,
                        genres = freshMovie.genres,
                        topCastData = freshMovie.topCastData,
                        directorData = freshMovie.directorData,
                        directorId = freshMovie.directorId,
                        directorName = freshMovie.directorName,
                        directorProfilePath = freshMovie.directorProfilePath,
                        seasons = freshMovie.seasons,
                        numberOfSeasons = freshMovie.numberOfSeasons,
                        numberOfEpisodes = freshMovie.numberOfEpisodes
                    )
                    
                    // Fetch updated movie from DB to sync to Firebase
                    val localMovie = favoriteDao.getById(movie.id, movie.mediaType)
                    if (localMovie != null) {
                        try {
                            firebaseRemoteDataSource.setMovie(localMovie)
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    android.util.Log.e("MovieRepository", "Failed to fetch missing details in background for ${movie.id}", e)
                }
            }
        }
    }

    suspend fun saveMoviesBulk(movies: List<Movie>) {
        if (movies.isEmpty()) return

        val updatedMovies = movies.map { 
            it.copy(syncStatus = "synced", clientUpdatedAt = System.currentTimeMillis()) 
        }
        
        favoriteDao.insertAll(updatedMovies) 
        widgetNotifier.notifyWidgetUpdated()
    
        repositoryScope.launch {
            try {
                // Firebase bulk update isn't strictly defined, so we can run them concurrently
                updatedMovies.forEach { movie ->
                    firebaseRemoteDataSource.setMovie(movie)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Firebase bulk sync failed", e)
            }
        }
    }

    suspend fun deleteMovie(movie: Movie) {
        favoriteDao.markDeleted(movie.id, movie.mediaType)
        widgetNotifier.notifyWidgetUpdated()

        // --- PUSH RIMOZIONE A TRAKT ---
        val builder = androidx.work.Data.Builder()
            .putString(TraktInstantWriteWorker.KEY_ACTION,     TraktInstantWriteWorker.ACTION_REMOVE_WATCHED)
            .putString(TraktInstantWriteWorker.KEY_MEDIA_TYPE, movie.mediaType)
            .putLong(  TraktInstantWriteWorker.KEY_TMDB_ID,    movie.id)
        if (movie.imdbId != null) {
            builder.putString(TraktInstantWriteWorker.KEY_IMDB_ID, movie.imdbId)
        }

        val workRequests = mutableListOf(
            OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(builder.build())
                .build()
        )

        // Fix smart-cast salvando in una variabile locale immutabile
        val currentRating = movie.personalRating
        if (currentRating != null && currentRating > 0.0) {
            // Fix: uso di .putAll() al posto di fromData()
            val ratingBuilder = androidx.work.Data.Builder().putAll(builder.build())
                .putString(TraktInstantWriteWorker.KEY_ACTION, TraktInstantWriteWorker.ACTION_REMOVE_RATING)
            workRequests += OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                .setInputData(ratingBuilder.build())
                .build()
        }
        
        if (movie.favorite) {
            // Fix: uso di .putAll() al posto di fromData()
            val watchlistBuilder = androidx.work.Data.Builder().putAll(builder.build())
                .putString(TraktInstantWriteWorker.KEY_ACTION, TraktInstantWriteWorker.ACTION_REMOVE_WATCHLIST)
            workRequests += OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                .setInputData(watchlistBuilder.build())
                .build()
        }

        WorkManager.getInstance(context).enqueue(workRequests)
        // ------------------------------
        
        repositoryScope.launch {
            try {
                firebaseRemoteDataSource.deleteMovie(movie.id, movie.mediaType)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    suspend fun markAsDeleted(movie: Movie) {
        favoriteDao.markDeleted(movie.id, movie.mediaType)

        // --- PUSH RIMOZIONE A TRAKT ---
        val builder = androidx.work.Data.Builder()
            .putString(TraktInstantWriteWorker.KEY_ACTION,     TraktInstantWriteWorker.ACTION_REMOVE_WATCHED)
            .putString(TraktInstantWriteWorker.KEY_MEDIA_TYPE, movie.mediaType)
            .putLong(  TraktInstantWriteWorker.KEY_TMDB_ID,    movie.id)
        if (movie.imdbId != null) {
            builder.putString(TraktInstantWriteWorker.KEY_IMDB_ID, movie.imdbId)
        }

        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(builder.build())
                .build()
        )

        repositoryScope.launch {
            try {
                firebaseRemoteDataSource.deleteMovie(movie.id, movie.mediaType)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    // --- Folder Operations ---
    fun getFoldersFlow(): Flow<List<FolderEntity>> = folderDao.getAllFlow()

    fun getFolderFlow(folderId: String): Flow<FolderEntity?> = folderDao.getByIdFlow(folderId)

    fun getMoviesByCompositeIds(compositeIds: List<String>): Flow<List<Movie>> = favoriteDao.getByCompositeIds(compositeIds)

    suspend fun saveFolder(folderEntity: FolderEntity) {
        // Estraiamo la vecchia cartella per fare il calcolo delle differenze (diff)
        val oldFolder = folderDao.getByIdFlow(folderEntity.id).firstOrNull()
        
        val now = System.currentTimeMillis()
        val updatedEntity = folderEntity.copy(syncStatus = "synced", clientUpdatedAt = now)
        folderDao.insert(updatedEntity)
        
        // --- INIZIO INTEGRAZIONE TRAKT (Instant Write) ---
        if (folderEntity.id.startsWith("trakt_")) {
            val traktListId = folderEntity.id.removePrefix("trakt_").toLongOrNull()
            if (traktListId != null) {
                val workRequests = mutableListOf<androidx.work.OneTimeWorkRequest>()
                
                // 1. Controllo cambio Nome o Descrizione
                if (oldFolder != null && (oldFolder.name != folderEntity.name || oldFolder.description != folderEntity.description)) {
                    val updateBuilder = androidx.work.Data.Builder()
                        .putString(TraktInstantWriteWorker.KEY_ACTION, "ACTION_UPDATE_LIST")
                        .putLong("LIST_ID", traktListId)
                        .putString("LIST_NAME", folderEntity.name)
                        .putString("LIST_DESC", folderEntity.description ?: "")
                    
                    workRequests.add(OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                        .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setInputData(updateBuilder.build()).build())
                }
                
                // 2. Controllo Film/Serie aggiunti o rimossi
                val oldItems = oldFolder?.itemIds?.toSet() ?: emptySet()
                val newItems = folderEntity.itemIds.toSet()
                
                val added = newItems - oldItems
                val removed = oldItems - newItems
                
                if (added.isNotEmpty()) {
                    val addBuilder = androidx.work.Data.Builder()
                        .putString(TraktInstantWriteWorker.KEY_ACTION, "ACTION_ADD_LIST_ITEMS")
                        .putLong("LIST_ID", traktListId)
                        .putStringArray("ITEMS_ADDED", added.toTypedArray())
                    workRequests.add(OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                        .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setInputData(addBuilder.build()).build())
                }
                
                if (removed.isNotEmpty()) {
                    val removeBuilder = androidx.work.Data.Builder()
                        .putString(TraktInstantWriteWorker.KEY_ACTION, "ACTION_REMOVE_LIST_ITEMS")
                        .putLong("LIST_ID", traktListId)
                        .putStringArray("ITEMS_REMOVED", removed.toTypedArray())
                    workRequests.add(OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                        .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setInputData(removeBuilder.build()).build())
                }
                
                if (workRequests.isNotEmpty()) {
                    WorkManager.getInstance(context).enqueue(workRequests)
                }
            }
        } else if (oldFolder == null) {
            // FIX: È una cartella appena creata localmente! La pushiamo su Trakt.
            val createBuilder = androidx.work.Data.Builder()
                .putString(TraktInstantWriteWorker.KEY_ACTION, "ACTION_CREATE_LIST")
                .putString("LOCAL_FOLDER_ID", folderEntity.id)
                .putString("LIST_NAME", folderEntity.name)
                .putString("LIST_DESC", folderEntity.description ?: "")

            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                    .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(createBuilder.build()).build()
            )
        }
        // --- FINE INTEGRAZIONE TRAKT ---

        repositoryScope.launch {
            try {
                val folder = com.cinetrack.data.models.Folder(
                    id = updatedEntity.id,
                    name = updatedEntity.name,
                    icon = updatedEntity.icon,
                    color = updatedEntity.color,
                    description = updatedEntity.description,
                    itemIds = updatedEntity.itemIds,
                    createdAt = updatedEntity.createdAt,
                    updatedAt = updatedEntity.updatedAt,
                    clientUpdatedAt = now
                )
                firebaseRemoteDataSource.setFolder(folder)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Firebase folder sync failed for ${updatedEntity.id}", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    folderDao.updateSyncStatus(updatedEntity.id, "pending")
                }
            }
        }
    }

    suspend fun deleteFolder(folderId: String) {
        folderDao.markDeleted(folderId)
        
        // --- INIZIO INTEGRAZIONE TRAKT ---
        if (folderId.startsWith("trakt_")) {
            val traktListId = folderId.removePrefix("trakt_").toLongOrNull()
            if (traktListId != null) {
                val deleteBuilder = androidx.work.Data.Builder()
                    .putString(TraktInstantWriteWorker.KEY_ACTION, "ACTION_DELETE_LIST")
                    .putLong("LIST_ID", traktListId)
                
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                        .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setInputData(deleteBuilder.build()).build()
                )
            }
        }
        // --- FINE INTEGRAZIONE TRAKT ---

        repositoryScope.launch {
            try {
                firebaseRemoteDataSource.deleteFolder(folderId)
                folderDao.deleteById(folderId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    // --- Remote Sync (The "Bunker" recovery) ---
    suspend fun pushPendingChanges() = kotlinx.coroutines.withContext(Dispatchers.IO) {
        // Push pending Movies
        val pendingMovies = favoriteDao.getPendingSync()
        for (movie in pendingMovies) {
            try {
                if (movie.syncStatus == "pending_delete") {
                    firebaseRemoteDataSource.deleteMovie(movie.id, movie.mediaType)
                    favoriteDao.deleteById(movie.id, movie.mediaType)
                } else if (movie.syncStatus == "pending") {
                    val syncedMovie = movie.copy(syncStatus = "synced")
                    firebaseRemoteDataSource.setMovie(syncedMovie)
                    favoriteDao.updateSyncStatus(movie.id, movie.mediaType, "synced")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Failed to push pending movie ${movie.id}", e)
            }
        }
        
        // Push pending Folders
        val pendingFolders = folderDao.getPendingSync()
        for (folder in pendingFolders) {
            try {
                if (folder.syncStatus == "pending_delete") {
                    firebaseRemoteDataSource.deleteFolder(folder.id)
                    folderDao.deleteById(folder.id)
                } else if (folder.syncStatus == "pending") {
                    val folderDto = com.cinetrack.data.models.Folder(
                        id = folder.id,
                        name = folder.name,
                        icon = folder.icon,
                        color = folder.color,
                        description = folder.description,
                        itemIds = folder.itemIds,
                        createdAt = folder.createdAt,
                        updatedAt = folder.updatedAt,
                        clientUpdatedAt = folder.clientUpdatedAt
                    )
                    firebaseRemoteDataSource.setFolder(folderDto)
                    folderDao.updateSyncStatus(folder.id, "synced")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Failed to push pending folder ${folder.id}", e)
            }
        }
    }

    suspend fun syncWithFirebase(
        force: Boolean = false,
        onProgress: (suspend (SyncProgress) -> Unit)? = null
    ) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        suspend fun emit(message: String, progress: Float?) {
            onProgress?.invoke(SyncProgress(message, progress))
        }

        // Rate limiting cooldown guard (5 minutes)
        val lastSync = preferenceRepository.userPreferencesFlow.first().lastSyncTimestamp
        if (!force && System.currentTimeMillis() - lastSync < 300_000L) {
            android.util.Log.d("MovieRepository", "Firebase Sync skipped - last sync was less than 5 minutes ago.")
            emit("Sincronizzazione completata (cache)", 1f)
            return@withContext
        }

        // 0. Push any pending local changes first
        android.util.Log.d("MovieRepository", "Starting Firebase Sync - Pushing pending changes...")
        emit("Carico modifiche in sospeso...", 0f)
        pushPendingChanges()

        try {
            // 1. Pull & Reconcile Favorites
            android.util.Log.d("MovieRepository", "Starting Firebase Sync - Fetching Favorites...")
            emit("Scarico preferiti...", null)
            val remoteFavorites = firebaseRemoteDataSource.fetchAllFavorites()
            android.util.Log.d("MovieRepository", "Fetched ${remoteFavorites.size} favorites from Firebase")
            
            emit("Sincronizzo preferiti...", 0.35f)
            val localFavoritesList = favoriteDao.getAll()
            val localFavorites = localFavoritesList.associateBy { "${it.mediaType}_${it.id}" }
            val remoteFavoritesMap = remoteFavorites.associateBy { "${it.mediaType}_${it.id}" }
            
            val moviesToInsert = mutableListOf<Movie>()
            val moviesToDelete = mutableListOf<Movie>()
            
            for (remoteMovie in remoteFavorites) {
                val key = "${remoteMovie.mediaType}_${remoteMovie.id}"
                val local = localFavorites[key]
                if (local == null) {
                    moviesToInsert.add(remoteMovie.copy(syncStatus = "synced"))
                } else {
                    if (remoteMovie.clientUpdatedAt >= local.clientUpdatedAt) {
                        moviesToInsert.add(remoteMovie.copy(syncStatus = "synced"))
                    } else {
                        if (local.syncStatus == "synced") {
                            favoriteDao.updateSyncStatus(local.id, local.mediaType, "pending")
                        }
                    }
                }
            }
            
            for ((key, localMovie) in localFavorites) {
                if (!remoteFavoritesMap.containsKey(key)) {
                    if (localMovie.syncStatus == "synced") {
                        moviesToDelete.add(localMovie)
                    }
                }
            }
            
            if (moviesToInsert.isNotEmpty()) {
                val chunks = moviesToInsert.chunked(50)
                chunks.forEachIndexed { index, chunk ->
                    favoriteDao.insertAll(chunk)
                    val portion = (index + 1).toFloat() / chunks.size.toFloat()
                    val progress = 0.35f + (0.55f - 0.35f) * portion
                    emit("Salvo preferiti... (${(index + 1) * chunk.size}/${moviesToInsert.size})", progress)
                }
            }
            
            for (movieToDelete in moviesToDelete) {
                favoriteDao.deleteById(movieToDelete.id, movieToDelete.mediaType)
            }
            android.util.Log.d("MovieRepository", "Successfully synchronized favorites with conflict resolution")

            // 2. Pull & Reconcile Folders
            emit("Scarico cartelle...", null)
            val remoteFolders = firebaseRemoteDataSource.fetchAllFolders()
            
            emit("Sincronizzo cartelle...", 0.7f)
            val localFoldersList = folderDao.getAll()
            val localFolders = localFoldersList.associateBy { it.id }
            val remoteFoldersMap = remoteFolders.associateBy { it.id }
            
            val foldersToInsert = mutableListOf<FolderEntity>()
            val foldersToDelete = mutableListOf<FolderEntity>()
            
            for (remoteFolder in remoteFolders) {
                val local = localFolders[remoteFolder.id]
                val remoteEntity = FolderEntity(
                    id = remoteFolder.id,
                    name = remoteFolder.name,
                    icon = remoteFolder.icon,
                    color = remoteFolder.color,
                    description = remoteFolder.description,
                    itemIds = remoteFolder.itemIds,
                    createdAt = remoteFolder.createdAt ?: "",
                    updatedAt = remoteFolder.updatedAt ?: "",
                    syncStatus = "synced",
                    clientUpdatedAt = remoteFolder.clientUpdatedAt
                )
                
                if (local == null) {
                    foldersToInsert.add(remoteEntity)
                } else {
                    if (remoteFolder.clientUpdatedAt >= local.clientUpdatedAt) {
                        foldersToInsert.add(remoteEntity)
                    } else {
                        if (local.syncStatus == "synced") {
                            folderDao.updateSyncStatus(local.id, "pending")
                        }
                    }
                }
            }
            
            for ((id, localFolder) in localFolders) {
                if (!remoteFoldersMap.containsKey(id)) {
                    if (localFolder.syncStatus == "synced") {
                        foldersToDelete.add(localFolder)
                    }
                }
            }
            
            if (foldersToInsert.isNotEmpty()) {
                val chunks = foldersToInsert.chunked(50)
                chunks.forEachIndexed { index, chunk ->
                    folderDao.insertAll(chunk)
                    val portion = (index + 1).toFloat() / chunks.size.toFloat()
                    val progress = 0.7f + (0.9f - 0.7f) * portion
                    emit("Salvo cartelle... (${(index + 1) * chunk.size}/${foldersToInsert.size})", progress)
                }
            }
            
            for (folderToDelete in foldersToDelete) {
                folderDao.deleteById(folderToDelete.id)
            }
            android.util.Log.d("MovieRepository", "Successfully synchronized folders with conflict resolution")

            // 3. Pull Preferences
            emit("Sincronizzo preferenze...", 0.92f)
            syncPreferencesWithFirebase()
            emit("Sync completata", 1f)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e("MovieRepository", "Error during Firebase synchronization, aborting sync to prevent data loss.", e)
            emit("Errore di sincronizzazione", 1f)
        }
    }

    suspend fun clearAllData() {
        favoriteDao.clearAll()
        folderDao.clearAll()
        preferenceRepository.clearAll()
    }

    suspend fun syncPreferencesWithFirebase() {
        val remotePrefs = firebaseRemoteDataSource.fetchUserPreferences() ?: return
        
        try {
            val currentPrefs = preferenceRepository.userPreferencesFlow
                // Fix: Emesso UserPreferences() corretto al posto di SyncProgress
                .catch { emit(com.cinetrack.data.models.UserPreferences()) }
                .firstOrNull() ?: com.cinetrack.data.models.UserPreferences()
                
            // Helper to parse SortConfig from Firebase Map
            fun parseSortConfig(map: Any?, default: com.cinetrack.data.models.SortConfig): com.cinetrack.data.models.SortConfig {
                if (map !is Map<*, *>) return default
                return com.cinetrack.data.models.SortConfig(
                    sortType = map["sortType"] as? String ?: default.sortType,
                    sortDirection = map["sortDirection"] as? String ?: default.sortDirection,
                    selectedGenres = (map["selectedGenres"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: default.selectedGenres,
                    selectedProviders = (map["selectedProviders"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: default.selectedProviders,
                    selectedDecades = (map["selectedDecades"] as? List<*>)?.filterIsInstance<String>() ?: default.selectedDecades
                )
            }

            // Helper to parse DiscoveryFilters from Firebase Map
            fun parseDiscoveryFilters(map: Any?, default: com.cinetrack.data.models.DiscoveryFilters): com.cinetrack.data.models.DiscoveryFilters {
                if (map !is Map<*, *>) return default
                return com.cinetrack.data.models.DiscoveryFilters(
                    selectedGenres = (map["selectedGenres"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: default.selectedGenres,
                    selectedProviders = (map["selectedProviders"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: default.selectedProviders,
                    selectedDecades = (map["selectedDecades"] as? List<*>)?.filterIsInstance<String>() ?: default.selectedDecades,
                    sortBy = map["sortBy"] as? String ?: default.sortBy
                )
            }

            val updatedPrefs = currentPrefs.copy(
                gridColumns = (remotePrefs["gridColumns"] as? Number)?.toInt() ?: currentPrefs.gridColumns,
                notificationsEnabled = remotePrefs["notificationsEnabled"] as? Boolean ?: currentPrefs.notificationsEnabled,
                showFolderBookmarks = remotePrefs["showFolderBookmarks"] as? Boolean ?: currentPrefs.showFolderBookmarks,
                showBadges = remotePrefs["showBadges"] as? Boolean ?: currentPrefs.showBadges,
                disabledBadges = (remotePrefs["disabledBadges"] as? List<*>)?.filterIsInstance<String>()?.toSet() ?: currentPrefs.disabledBadges,
                vibrationEnabled = remotePrefs["vibrationEnabled"] as? Boolean ?: currentPrefs.vibrationEnabled,
                accentColor = remotePrefs["accentColor"] as? String ?: currentPrefs.accentColor,
                advancedVisualEffectsEnabled = remotePrefs["advancedVisualEffectsEnabled"] as? Boolean ?: currentPrefs.advancedVisualEffectsEnabled,
                dynamicAppIconEnabled = remotePrefs["dynamicAppIconEnabled"] as? Boolean ?: currentPrefs.dynamicAppIconEnabled,
                homeSort = parseSortConfig(remotePrefs["homeSort"], currentPrefs.homeSort),
                vistiSort = parseSortConfig(remotePrefs["vistiSort"], currentPrefs.vistiSort),
                discoveryFilters = parseDiscoveryFilters(remotePrefs["discoveryFilters"], currentPrefs.discoveryFilters),
                lastSyncTimestamp = System.currentTimeMillis()
            )
            
            preferenceRepository.updateAll(updatedPrefs)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e("MovieRepository", "Error syncing preferences: ${e.message}")
        }
    }

    suspend fun savePreferencesRemote(prefs: com.cinetrack.data.models.UserPreferences) {
        repositoryScope.launch {
            try {
                // Convert to Map for Firestore
                val prefsMap = mapOf(
                    "gridColumns" to prefs.gridColumns,
                    "notificationsEnabled" to prefs.notificationsEnabled,
                    "showFolderBookmarks" to prefs.showFolderBookmarks,
                    "showBadges" to prefs.showBadges,
                    "disabledBadges" to prefs.disabledBadges.toList(),
                    "vibrationEnabled" to prefs.vibrationEnabled,
                    "accentColor" to prefs.accentColor,
                    "advancedVisualEffectsEnabled" to prefs.advancedVisualEffectsEnabled,
                    "dynamicAppIconEnabled" to prefs.dynamicAppIconEnabled,
                    "homeSort" to prefs.homeSort,
                    "vistiSort" to prefs.vistiSort,
                    "discoveryFilters" to prefs.discoveryFilters
                )
                firebaseRemoteDataSource.setUserPreferences(prefsMap)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Error saving preferences remote", e)
            }
        }
    }

    // --- Search History ---
    fun getRecentSearches(): Flow<List<String>> = searchHistoryDao.getRecentSearches().map { entities ->
        entities.map { it.query }
    }

    suspend fun saveSearchQuery(query: String) {
        if (query.isBlank()) return
        searchHistoryDao.insertSearch(SearchHistoryEntity(query.trim(), System.currentTimeMillis()))
    }

    suspend fun deleteSearchQuery(query: String) {
        searchHistoryDao.deleteSearch(query)
    }

    suspend fun clearRecentSearches() {
        searchHistoryDao.clearHistory()
    }


    // --- Remote TMDb Operations ---
    suspend fun fetchMovieDetails(id: Long, isTv: Boolean): com.cinetrack.data.api.MovieDetailResponse {
        return if (isTv) tmdbService.getTVDetails(id) else tmdbService.getMovieDetails(id)
    }

    suspend fun searchMovies(query: String, page: Int = 1): List<Movie> = tmdbService.searchMovie(query, page = page).results
    suspend fun searchMovieWithYear(query: String, year: String?): Movie? {
        val results = tmdbService.searchMovie(query, year = year).results
        return results.firstOrNull()
    }
    
    suspend fun findByImdbId(imdbId: String): Movie? {
        val response = tmdbService.findByExternalId(imdbId, "imdb_id")
        val movieRes = response.movieResults?.firstOrNull()
        if (movieRes != null) {
            return Movie(
                id = movieRes.id,
                mediaType = "movie",
                title = movieRes.title,
                posterPath = movieRes.posterPath,
                backdropPath = movieRes.backdropPath,
                voteAverage = movieRes.voteAverage,
                releaseDate = movieRes.releaseDate,
                genreIds = movieRes.genreIds,
                overview = movieRes.overview
            )
        }
        val tvRes = response.tvResults?.firstOrNull()
        if (tvRes != null) {
            return Movie(
                id = tvRes.id,
                mediaType = "tv",
                name = tvRes.name,
                posterPath = tvRes.posterPath,
                backdropPath = tvRes.backdropPath,
                voteAverage = tvRes.voteAverage,
                firstAirDate = tvRes.firstAirDate,
                genreIds = tvRes.genreIds,
                overview = tvRes.overview
            )
        }
        return null
    }
    
    suspend fun searchTV(query: String, page: Int = 1): List<Movie> = tmdbService.searchTV(query, page = page).results
    suspend fun searchMulti(query: String, page: Int = 1): List<TMDBSearchResult> = tmdbService.searchMulti(query, page = page).results
    suspend fun getPersonDetails(id: Long): Person = tmdbService.getPersonDetails(id)
    suspend fun fetchSeasonDetails(id: Long, seasonNumber: Int): Season = tmdbService.getSeasonDetails(id, seasonNumber)
    suspend fun fetchCollectionDetails(id: Long): com.cinetrack.data.api.CollectionResponse = tmdbService.getCollectionDetails(id)
    suspend fun searchPeople(query: String, page: Int = 1): List<PersonSearchResult> = tmdbService.searchPeople(query, page = page).results
    suspend fun getMoviesByGenre(genreId: Long, page: Int = 1): List<Movie> = tmdbService.getMoviesByGenre(genreId, page = page).results
    suspend fun getTVShowsByGenre(genreId: Long, page: Int = 1): List<Movie> = tmdbService.getTVShowsByGenre(genreId, page = page).results
    suspend fun getPopularMovies(page: Int = 1): List<Movie> = tmdbService.getPopularMovies(page = page).results
    suspend fun getMovieRecommendations(id: Long, page: Int = 1): List<Movie> = tmdbService.getMovieRecommendations(id, page = page).results
    suspend fun getTVRecommendations(id: Long, page: Int = 1): List<Movie> = tmdbService.getTVRecommendations(id, page = page).results

    suspend fun getNowPlayingMovies(page: Int = 1): List<Movie> = tmdbService.getNowPlayingMovies(page = page, region = "IT").results

    suspend fun getUpcomingMovies(page: Int = 1): List<Movie> {
        val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
        val resolvedLanguage = if (rawLanguage == "system") {
            java.util.Locale.getDefault().language
        } else {
            rawLanguage
        }
        val region = if (resolvedLanguage == "it") "IT" else "US"

        val today = java.time.LocalDate.now()
        return tmdbService.discoverMovies(
            page = page,
            options = mapOf(
                "release_date.gte" to today.toString(),
                "with_release_type" to "2|3",
                "region" to region,
                "sort_by" to "popularity.desc"
            )
        ).results
    }

    suspend fun getPopularTV(page: Int = 1): List<Movie> = tmdbService.getPopularTV(page = page).results
    suspend fun getAiringTodayTV(page: Int = 1): List<Movie> = tmdbService.getAiringTodayTV(page = page).results

    suspend fun getOnTheAirTV(page: Int = 1): List<Movie> {
        val today = java.time.LocalDate.now()
        return tmdbService.discoverTV(
            page = page,
            options = mapOf(
                "first_air_date.gte" to today.toString(),
                "watch_region" to "IT",
                "sort_by" to "popularity.desc"
            )
        ).results
    }

    suspend fun getTrendingAll(page: Int = 1): List<Movie> = tmdbService.getTrendingAll(page = page).results
    suspend fun getTrendingMovies(page: Int = 1): List<Movie> = tmdbService.getTrendingMovies(page = page).results
    suspend fun getTrendingTV(page: Int = 1): List<Movie> = tmdbService.getTrendingTV(page = page).results
    suspend fun getTrendingPeople(page: Int = 1): List<PersonSearchResult> = tmdbService.getTrendingPeople(page = page).results
    suspend fun getPopularPeople(page: Int = 1): List<PersonSearchResult> = tmdbService.getPopularPeople(page = page).results

    suspend fun discoverMoviesWithParams(page: Int = 1, options: Map<String, String>): List<Movie> {
        val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
        val resolvedLanguage = if (rawLanguage == "system") {
            java.util.Locale.getDefault().language
        } else {
            rawLanguage
        }
        val region = if (resolvedLanguage == "it") "IT" else "US"

        val finalOptions = options.toMutableMap()
        finalOptions.putIfAbsent("language", resolvedLanguage)
        finalOptions.putIfAbsent("region", region)
        finalOptions.putIfAbsent("watch_region", region)

        return tmdbService.discoverMovies(page = page, options = finalOptions).results.map { it.copy(mediaType = "movie") }
    }

    suspend fun discoverTVWithParams(page: Int = 1, options: Map<String, String>): List<Movie> {
        val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
        val resolvedLanguage = if (rawLanguage == "system") {
            java.util.Locale.getDefault().language
        } else {
            rawLanguage
        }
        val region = if (resolvedLanguage == "it") "IT" else "US"

        val finalOptions = options.toMutableMap()
        finalOptions.putIfAbsent("language", resolvedLanguage)
        finalOptions.putIfAbsent("watch_region", region)

        return tmdbService.discoverTV(page = page, options = finalOptions).results.map { it.copy(mediaType = "tv") }
    }


    suspend fun fetchOmdbRatings(imdbId: String): ExtraRatings {
        return try {
            val response = omdbService.getRatings(imdbId, omdbApiKey)
            val rt = response.ratings?.find { it.source == "Rotten Tomatoes" || it.source == "RottenTomatoes" }?.value
            val mc = response.ratings?.find { it.source == "Metacritic" || it.source == "Metascore" }?.value
            ExtraRatings(
                imdbRating = response.imdbRating,
                imdbVotes = response.imdbVotes,
                rottenTomatoes = rt,
                metacritic = mc,
                awards = response.Awards
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ExtraRatings()
        }
    }

    suspend fun fetchTraktRating(id: String, isTv: Boolean): TraktRatingInfo? {
        val type = if (isTv) "shows" else "movies"
        return try {
            val response = traktService.getRatings(type, id, traktApiKey)
            TraktRatingInfo(
                rating = response.rating,
                votes = response.votes
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    suspend fun fetchComments(id: String, isTv: Boolean): List<com.cinetrack.data.api.TraktComment> {
        val type = if (isTv) "shows" else "movies"
        return try {
            traktService.getComments(
                type = type,
                id = id,
                sort = "likes",
                apiKey = traktApiKey
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }
}

data class TraktRatingInfo(val rating: Double?, val votes: Int)