package com.cinetrack.data.repository

import com.cinetrack.data.model.Movie
import com.cinetrack.data.api.OmdbService
import com.cinetrack.data.api.TraktService
import com.cinetrack.data.api.Person
import com.cinetrack.data.api.PersonSearchResult
import com.cinetrack.data.model.Season
import com.cinetrack.data.api.TMDBService
import com.cinetrack.data.api.ImagesResponse
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.model.ExtraRatings
import com.cinetrack.data.local.dao.CacheDao
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.remote.FirebaseRemoteDataSource
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.local.entities.MovieDetailCacheEntity
import com.cinetrack.data.local.entities.ColorCacheEntity
import com.cinetrack.data.local.entities.HomeFeedCacheEntity
import com.cinetrack.data.model.CachedFeedData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import com.cinetrack.data.model.Folder
import com.cinetrack.data.sync.SyncProgress
import com.cinetrack.ui.utils.UiText
import com.cinetrack.R
import com.cinetrack.domain.UpdateEpisodesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
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
import kotlinx.coroutines.channels.awaitClose
import com.cinetrack.worker.TraktInstantWriteWorker
import com.cinetrack.worker.SimklInstantWriteWorker
import com.cinetrack.data.repository.importers.MovieLookupService
import com.cinetrack.util.DateTimeUtils
import com.cinetrack.util.getEpisodeWatchDate
import com.cinetrack.util.setEpisodeWatchDate
import com.cinetrack.util.removeEpisodeWatchDate

@Singleton
class MovieRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val folderRepository: FolderRepository,
    private val cacheDao: CacheDao,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val tmdbService: TMDBService,
    private val omdbService: OmdbService,
    private val traktService: TraktService,
    private val firebaseRemoteDataSource: FirebaseRemoteDataSource,
    private val preferenceRepository: PreferenceRepository,
    private val firebaseSyncCoordinator: com.cinetrack.data.sync.FirebaseSyncCoordinator,
    @Named("tmdb_api_key") private val apiKey: String,
    @Named("omdb_api_key") private val omdbApiKey: String,
    @Named("trakt_api_key") private val traktApiKey: String,
    private val boxOfficeRepository: javax.inject.Provider<BoxOfficeRepository>,
    private val widgetNotifier: com.cinetrack.domain.WidgetNotifier,
    @ApplicationContext private val context: Context,
    private val badgeDao: com.cinetrack.data.local.dao.BadgeDao
) : MovieLookupService {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private val memoryDetailsCache = ConcurrentHashMap<String, Any>()
    private val memoryColorCache = ConcurrentHashMap<String, String>() // id -> hexColor

    private val externalSyncDispatcher by lazy { ExternalSyncDispatcher(context) }
    private val tvEpisodeWatchStateDelegate by lazy { TvEpisodeWatchStateDelegate(watchHistoryRepository) }
    private val trendingStatsSyncDelegate by lazy { TrendingStatsSyncDelegate(firebaseRemoteDataSource) }

    // Cache LRU per Global Movie Stats (elimina addSnapshotListener continuo a favore di on-demand .get())
    private data class CachedGlobalStats(
        val stats: com.cinetrack.data.model.GlobalMovieStats?,
        val timestamp: Long
    )
    private val globalStatsLruCache = android.util.LruCache<String, CachedGlobalStats>(100)
    private val GLOBAL_STATS_TTL_MS = 30 * 60 * 1000L // 30 minuti

    suspend fun getCachedColor(id: String): String? {
        memoryColorCache[id]?.let { return it }
        return try {
            val entity = cacheDao.getColor(id)
            entity?.colorHex?.also { memoryColorCache[id] = it }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveCachedColor(id: String, hexColor: String) {
        memoryColorCache[id] = hexColor
        try {
            cacheDao.saveColor(ColorCacheEntity(id = id, colorHex = hexColor, ambientHex = hexColor, updatedAt = System.currentTimeMillis().toString()))
        } catch (e: Exception) {}
    }

    suspend fun wipeLocalData() {
        favoriteDao.clearAll()
        folderRepository.clearAll()
        widgetNotifier.notifyWidgetUpdated()
    }

    suspend fun wipeTotalData() {
        // Wipe cloud data
        firebaseRemoteDataSource.wipeAllCloudData()
        
        // Wipe local data
        wipeLocalData()
    }

    // --- Local Operations (Bunker: Room First) ---
    suspend fun getLocalMovies(): List<Movie> = favoriteDao.getAll()
    
    suspend fun getLocalMoviesIncludingDeleted(): List<Movie> = favoriteDao.getAllIncludingDeleted()
    
    fun getLocalMoviesFlow(): Flow<List<Movie>> = favoriteDao.getAllFlow()

    fun getFlowMoviesFlow(): Flow<List<Movie>> = favoriteDao.getFlowMoviesFlow()

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

    suspend fun getMovie(id: Long, mediaType: String): Movie? {
        val normalizedType = mediaType.ifBlank { "movie" }
        return favoriteDao.getById(id, normalizedType)
            ?: favoriteDao.getById(id, if (normalizedType == "movie") "tv" else "movie")
    }

    /** Returns the movie even if it's pending_delete — used by sync workers to prevent resurrection */
    suspend fun getMovieIncludingDeleted(id: Long, mediaType: String): Movie? {
        val normalizedType = mediaType.ifBlank { "movie" }
        return favoriteDao.getByIdIncludingDeleted(id, normalizedType)
            ?: favoriteDao.getByIdIncludingDeleted(id, if (normalizedType == "movie") "tv" else "movie")
    }

    /** Physically removes a pending_delete row after the remote API confirms the deletion */
    suspend fun hardDeleteMovie(id: Long, mediaType: String) = favoriteDao.deleteById(id, mediaType)

    suspend fun getShowsForUpdate(limit: Int = 150): List<Movie> = favoriteDao.getShowsForUpdate(limit)

    suspend fun getUpcomingMoviesForUpdate(limit: Int = 150): List<Movie> = favoriteDao.getUpcomingMoviesForUpdate(limit)

    suspend fun saveMovie(movie: Movie, syncToTrakt: Boolean = true) {
        val targetMediaType = if (movie.mediaType.isBlank()) "movie" else movie.mediaType
        val movieToSave = if (movie.mediaType.isBlank()) movie.copy(mediaType = targetMediaType) else movie
        val oldMovie = favoriteDao.getById(movieToSave.id, movieToSave.mediaType)
            ?: favoriteDao.getById(movieToSave.id, if (movieToSave.mediaType == "movie") "tv" else "movie")

        // Safeguard user's personalNote: only clear if caller explicitly provided empty string/blank,
        // and keep old note if caller did not touch personalNote (null).
        val resolvedPersonalNote = if (movieToSave.personalNote != null) {
            movieToSave.personalNote?.trim()?.ifBlank { null }
        } else {
            oldMovie?.personalNote
        }

        // 1. Update Room immediately
        val updatedMovie = movieToSave.copy(
            personalNote = resolvedPersonalNote,
            syncStatus = "synced",
            clientUpdatedAt = if (syncToTrakt) System.currentTimeMillis() else (movieToSave.clientUpdatedAt.takeIf { it > 0 } ?: oldMovie?.clientUpdatedAt ?: System.currentTimeMillis())
        )
        updatedMovie.emotionalVibes = movieToSave.emotionalVibes
        updatedMovie.favoriteActorId = movieToSave.favoriteActorId
        updatedMovie.favoriteActorName = movieToSave.favoriteActorName
        updatedMovie.favoriteActorProfilePath = movieToSave.favoriteActorProfilePath
        updatedMovie.favoriteActorTmdbPath = movieToSave.favoriteActorTmdbPath
        updatedMovie.favoriteActorCharacter = movieToSave.favoriteActorCharacter
        favoriteDao.insert(updatedMovie)
        
        // Notify Widget of changes
        widgetNotifier.notifyWidgetUpdated()

        val oldWatched = oldMovie?.watched ?: false
        // Sincronizzazione locale della cronologia visioni (Watch History / Diario)
        // Se un media passa da visto a NON visto (es. Undo da toast o cambio stato),
        // rimuoviamo le sessioni orfane da watch_history preservando la Single Source of Truth
        if (oldWatched && !movie.watched) {
            if (movie.mediaType == "tv") {
                if (movie.watchedEpisodes.isNullOrEmpty() && movie.watchedAt == null) {
                    watchHistoryRepository.deleteWatchHistoryByMovieId(movieToSave.id, movieToSave.mediaType)
                    watchHistoryRepository.pushPendingWatchHistory()
                }
            } else {
                watchHistoryRepository.deleteWatchHistoryByMovieId(movieToSave.id, movieToSave.mediaType)
                watchHistoryRepository.pushPendingWatchHistory()
            }
        }

        // Enqueue Instant Write to Trakt & SIMKL via WorkManager
        if (syncToTrakt) {
            externalSyncDispatcher.dispatchSaveSync(oldMovie, movie)
        }
        
        // 2. Fire-and-forget to Firebase (SDK handles persistence/retry)
        repositoryScope.launch {
            try {
                firebaseRemoteDataSource.setMovie(movie)
                trendingStatsSyncDelegate.syncGlobalStatsDeltas(oldMovie, movie) { compositeId ->
                    synchronized(globalStatsLruCache) {
                        globalStatsLruCache.remove(compositeId)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Firebase sync failed for ${movie.id}", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    favoriteDao.updateSyncStatus(movie.id, movie.mediaType, "pending")
                }
            }
        }
    }

    private val fetchingDetailsIds = java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val fetchSemaphore = java.util.concurrent.Semaphore(3)

    fun fetchMissingDetailsAsync(movie: Movie) {
        val key = "${movie.mediaType}_${movie.id}"
        val eps = movie.watchedEpisodes
        val needsDetails = movie.runtime == null || movie.runtime == 0 || movie.topCastData.isNullOrEmpty() || movie.productionCompanies.isNullOrEmpty()
        val needsEpisodes = movie.mediaType == "tv" && movie.watched && (eps.isNullOrEmpty() || eps.values.sumOf { it.size } == 0)
        if ((needsDetails || needsEpisodes) && fetchingDetailsIds.add(key)) {
            val updateEpisodesUseCase = com.cinetrack.domain.UpdateEpisodesUseCase()
            repositoryScope.launch {
                try {
                    fetchSemaphore.acquire()
                    try {
                        val isTv = movie.mediaType == "tv"
                        val response = fetchMovieDetails(movie.id, isTv)
                        val freshMovie = com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(response, if (isTv) "tv" else "movie")
                        
                        if (isTv) {
                            val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                            var updatedSeasons = freshMovie.seasons
                            val targetSeasonNum = response.nextEpisodeToAir?.seasonNumber
                                ?: response.seasons?.firstOrNull { !it.airDate.isNullOrBlank() && it.airDate >= today }?.seasonNumber

                            if (targetSeasonNum != null && targetSeasonNum > 0) {
                                try {
                                    val detailedSeason = fetchSeasonDetails(movie.id, targetSeasonNum)
                                    updatedSeasons = updatedSeasons?.map { if (it.seasonNumber == targetSeasonNum) detailedSeason else it }
                                    freshMovie.seasons = updatedSeasons
                                } catch (e: Exception) {
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                }
                            }
                        }
                        
                        var currentLocal = favoriteDao.getById(movie.id, movie.mediaType)
                        if (currentLocal != null) {
                            currentLocal = currentLocal.copy(
                                runtime = freshMovie.runtime ?: currentLocal.runtime,
                                episodeRunTime = freshMovie.episodeRunTime ?: currentLocal.episodeRunTime,
                                genres = freshMovie.genres ?: currentLocal.genres,
                                topCastData = freshMovie.topCastData ?: currentLocal.topCastData,
                                directorData = freshMovie.directorData ?: currentLocal.directorData,
                                directorId = freshMovie.directorId ?: currentLocal.directorId,
                                directorName = freshMovie.directorName ?: currentLocal.directorName,
                                directorProfilePath = freshMovie.directorProfilePath ?: currentLocal.directorProfilePath,
                                seasons = freshMovie.seasons ?: currentLocal.seasons,
                                numberOfSeasons = freshMovie.numberOfSeasons ?: currentLocal.numberOfSeasons,
                                numberOfEpisodes = freshMovie.numberOfEpisodes ?: currentLocal.numberOfEpisodes,
                                posterPath = freshMovie.posterPath ?: currentLocal.posterPath,
                                backdropPath = freshMovie.backdropPath ?: currentLocal.backdropPath,
                                overview = freshMovie.overview ?: currentLocal.overview,
                                firstAirDate = freshMovie.firstAirDate ?: currentLocal.firstAirDate,
                                lastAirDate = freshMovie.lastAirDate ?: currentLocal.lastAirDate,
                                nextEpisodeAirDate = freshMovie.nextEpisodeAirDate ?: currentLocal.nextEpisodeAirDate,
                                nextEpisodeString = freshMovie.nextEpisodeString ?: currentLocal.nextEpisodeString,
                                releaseDate = freshMovie.releaseDate ?: currentLocal.releaseDate,
                                releaseYear = freshMovie.releaseYear ?: currentLocal.releaseYear,
                                status = freshMovie.status ?: currentLocal.status,
                                voteAverage = freshMovie.voteAverage ?: currentLocal.voteAverage,
                                voteCount = freshMovie.voteCount ?: currentLocal.voteCount,
                                originCountry = freshMovie.originCountry ?: currentLocal.originCountry,
                                revenue = freshMovie.revenue ?: currentLocal.revenue,
                                budget = freshMovie.budget ?: currentLocal.budget,
                                tagline = freshMovie.tagline ?: currentLocal.tagline,
                                imdbId = freshMovie.imdbId ?: currentLocal.imdbId,
                                streamingProviderIds = freshMovie.streamingProviderIds ?: currentLocal.streamingProviderIds,
                                productionCompanies = freshMovie.productionCompanies ?: currentLocal.productionCompanies
                            )
                            favoriteDao.insert(currentLocal)
                        }
                        val currentEps = currentLocal?.watchedEpisodes
                        if (isTv && movie.watched && currentLocal != null && (currentEps.isNullOrEmpty() || currentEps.values.sumOf { it.size } == 0)) {
                            val allWatched = updateEpisodesUseCase.markAllWatched(freshMovie).watchedEpisodes
                            if (!allWatched.isNullOrEmpty()) {
                                currentLocal = currentLocal.copy(watchedEpisodes = allWatched, progress = 1.0)
                                favoriteDao.insert(currentLocal)
                            }
                        }

                        // Fetch updated movie from DB to sync to Firebase
                        if (currentLocal != null) {
                            try {
                                firebaseRemoteDataSource.setMovie(currentLocal)
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    } finally {
                        fetchSemaphore.release()
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    android.util.Log.e("MovieRepository", "Failed to fetch missing details in background for ${movie.id}", e)
                } finally {
                    fetchingDetailsIds.remove(key)
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
                // Use batched setMoviesBulk to prevent Firestore SQLiteDocumentOverlayCache OOM
                firebaseRemoteDataSource.setMoviesBulk(updatedMovies)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Firebase bulk sync failed", e)
            }
        }
    }

    suspend fun deleteMovie(movie: Movie) {
        val existingMovie = favoriteDao.getById(movie.id, movie.mediaType)
            ?: favoriteDao.getByIdIncludingDeleted(movie.id, movie.mediaType)
        val targetMovie = existingMovie?.copy(
            personalRating = existingMovie.personalRating ?: movie.personalRating,
            favorite = existingMovie.favorite || movie.favorite,
            watched = existingMovie.watched || movie.watched,
            watchedEpisodes = if (existingMovie.watchedEpisodes.isNullOrEmpty()) movie.watchedEpisodes else existingMovie.watchedEpisodes,
            emotionalVibes = if (existingMovie.emotionalVibes.isNullOrEmpty()) movie.emotionalVibes else existingMovie.emotionalVibes,
            favoriteActorId = existingMovie.favoriteActorId ?: movie.favoriteActorId,
            imdbId = existingMovie.imdbId ?: movie.imdbId
        ) ?: movie

        // FIX: unico insert atomico con syncStatus già = "pending_delete".
        // Se facessimo insert() + markDeleted() in due step, Room emetterebbe il Flow
        // due volte: la prima con la serie ancora visibile, la seconda senza.
        // Ciò causava la card a rimanere visibile per qualche frame dopo "Eliminar".
        val updatedMovie = targetMovie.copy(
            watchedEpisodes = emptyMap(),
            syncStatus = "pending_delete",
            clientUpdatedAt = System.currentTimeMillis()
        )
        favoriteDao.insert(updatedMovie)
        watchHistoryRepository.purgeHistoryForMovie(targetMovie.id, targetMovie.mediaType)
        widgetNotifier.notifyWidgetUpdated()

        // --- PUSH RIMOZIONE A TRAKT & SIMKL ---
        externalSyncDispatcher.dispatchDeleteSync(targetMovie)
        // --------------------------------------
        
        repositoryScope.launch {
            try {
                firebaseRemoteDataSource.deleteMovie(targetMovie.id, targetMovie.mediaType)

                // Rimuove il voto, le emotional vibes, l'attore MVP e le views dai global stats di Firestore
                val ratingToRemove = targetMovie.personalRating
                val vibesToRemove: List<String> = trendingStatsSyncDelegate.parseVibeCodes(targetMovie.emotionalVibes).toList()
                val mvpToRemove = targetMovie.favoriteActorId
                val calculatedViewsDelta = if (targetMovie.mediaType == "tv") {
                    -(targetMovie.watchedEpisodes?.values?.sumOf { it.size } ?: 0).toLong()
                } else {
                    if (targetMovie.watched) -1L else 0L
                }

                if ((ratingToRemove != null && ratingToRemove > 0.0) ||
                    vibesToRemove.isNotEmpty() ||
                    mvpToRemove != null ||
                    calculatedViewsDelta != 0L
                ) {
                    firebaseRemoteDataSource.updateGlobalMovieStats(
                        compositeId = "${targetMovie.mediaType}_${targetMovie.id}",
                        addedVibes = emptyList<String>(),
                        removedVibes = vibesToRemove,
                        newMvp = null,
                        oldMvp = mvpToRemove,
                        newRating = null,
                        oldRating = ratingToRemove,
                        newStatus = "unwatched",
                        oldStatus = if (targetMovie.watched) "watched" else "unwatched",
                        viewsDelta = calculatedViewsDelta
                    )
                    synchronized(globalStatsLruCache) {
                        globalStatsLruCache.remove("${targetMovie.mediaType}_${targetMovie.id}")
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Error deleting movie or updating global stats: ${e.message}", e)
            }
        }
    }

    suspend fun markAsDeleted(movie: Movie) {
        val updatedMovie = movie.copy(
            watchedEpisodes = emptyMap(),
            syncStatus = "pending_delete",
            clientUpdatedAt = System.currentTimeMillis()
        )
        favoriteDao.insert(updatedMovie)
        watchHistoryRepository.purgeHistoryForMovie(movie.id, movie.mediaType)

        // --- PUSH RIMOZIONE A TRAKT & SIMKL ---
        externalSyncDispatcher.dispatchMarkAsDeletedSync(movie)

        repositoryScope.launch {
            try {
                firebaseRemoteDataSource.deleteMovie(movie.id, movie.mediaType)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    // --- Folder Operations (Delegated to FolderRepository) ---
    fun getFoldersFlow(): Flow<List<FolderEntity>> = folderRepository.getFoldersFlow()

    fun getFolderFlow(folderId: String): Flow<FolderEntity?> = folderRepository.getFolderFlow(folderId)

    fun getMoviesByCompositeIds(compositeIds: List<String>): Flow<List<Movie>> = favoriteDao.getByCompositeIds(compositeIds)

    suspend fun saveFolder(folderEntity: FolderEntity) {
        folderRepository.saveFolder(folderEntity)
    }

    suspend fun deleteFolder(folderId: String) {
        folderRepository.deleteFolder(folderId)
    }

    // --- Remote Sync (Delegated to FirebaseSyncCoordinator) ---
    suspend fun pushPendingChanges() = firebaseSyncCoordinator.pushPendingChanges()

    suspend fun syncWithFirebase(
        force: Boolean = false,
        onProgress: (suspend (SyncProgress) -> Unit)? = null
    ) = firebaseSyncCoordinator.syncWithFirebase(force, onProgress)

    fun enqueueSyncWorker() = firebaseSyncCoordinator.enqueueSyncWorker()

    suspend fun syncPreferencesWithFirebase() = firebaseSyncCoordinator.syncPreferencesWithFirebase()

    suspend fun savePreferencesRemote(prefs: com.cinetrack.data.model.UserPreferences) =
        firebaseSyncCoordinator.savePreferencesRemote(prefs)

    suspend fun clearAllData() {
        favoriteDao.clearAll()
        folderRepository.clearAll()
        preferenceRepository.clearAll()
        badgeDao.clearUserBadges()
        badgeDao.clearPersonFrequencies()
        context.getSharedPreferences("flicktrove_badge_prefs", Context.MODE_PRIVATE).edit().clear().apply()
    }


    // --- Remote TMDb Operations (Stale-While-Revalidate & Caching) ---
    fun getMovieDetailsFlow(id: Long, isTv: Boolean): Flow<com.cinetrack.data.api.MovieDetailResponse> = flow {
        val mediaType = if (isTv) "tv" else "movie"
        val cacheKey = "$mediaType:$id"

        // 1. Instant 0ms memory check
        val inMemory = memoryDetailsCache[cacheKey] as? com.cinetrack.data.api.MovieDetailResponse
        if (inMemory != null) {
            emit(inMemory)
        } else {
            // 2. Fast ~3ms Room disk check
            try {
                val cachedJson = cacheDao.getDetail(id, mediaType)
                if (cachedJson != null) {
                    val parsed = json.decodeFromString<com.cinetrack.data.api.MovieDetailResponse>(cachedJson)
                    memoryDetailsCache[cacheKey] = parsed
                    emit(parsed)
                }
            } catch (e: Exception) {
                // Ignore disk parsing errors
            }
        }

        // 3. Background network revalidation
        try {
            val freshResponse = if (isTv) tmdbService.getTVDetails(id) else tmdbService.getMovieDetails(id)
            memoryDetailsCache[cacheKey] = freshResponse
            emit(freshResponse)
            try {
                cacheDao.saveDetailWithLRU(
                    MovieDetailCacheEntity(
                        id = id,
                        mediaType = mediaType,
                        data = json.encodeToString(freshResponse),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                // Ignore room errors
            }
        } catch (e: Exception) {
            if (memoryDetailsCache[cacheKey] == null) {
                throw e
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun fetchMovieDetails(id: Long, isTv: Boolean, forceRefresh: Boolean = false): com.cinetrack.data.api.MovieDetailResponse {
        val mediaType = if (isTv) "tv" else "movie"
        val cacheKey = "$mediaType:$id"
        if (!forceRefresh) {
            (memoryDetailsCache[cacheKey] as? com.cinetrack.data.api.MovieDetailResponse)?.let { return it }
            try {
                val cachedJson = cacheDao.getDetail(id, mediaType)
                if (cachedJson != null) {
                    val cachedResponse = json.decodeFromString<com.cinetrack.data.api.MovieDetailResponse>(cachedJson)
                    memoryDetailsCache[cacheKey] = cachedResponse
                    return cachedResponse
                }
            } catch (e: Exception) {
                // Ignore cache parse error, fallback to network
            }
        }
        val response = if (isTv) tmdbService.getTVDetails(id) else tmdbService.getMovieDetails(id)
        memoryDetailsCache[cacheKey] = response
        try {
            cacheDao.saveDetailWithLRU(
                MovieDetailCacheEntity(
                    id = id,
                    mediaType = mediaType,
                    data = json.encodeToString(response),
                    updatedAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            // Ignore
        }
        return response
    }

    suspend fun getMediaImagesAllLanguages(id: Long, isTv: Boolean): ImagesResponse? = withContext(Dispatchers.IO) {
        val cacheKey = "images_all:${if (isTv) "tv" else "movie"}:$id"
        (memoryDetailsCache[cacheKey] as? ImagesResponse)?.let { return@withContext it }
        try {
            val response = if (isTv) tmdbService.getTVImagesAllLanguages(id) else tmdbService.getMovieImagesAllLanguages(id)
            memoryDetailsCache[cacheKey] = response
            response
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error fetching images for all languages for $id")
            null
        }
    }

    suspend fun getCachedHomeFeed(): CachedFeedData? {
        return try {
            val jsonStr = cacheDao.getHomeFeed("home_feed") ?: return null
            json.decodeFromString<CachedFeedData>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCachedHomeFeedEntity(): HomeFeedCacheEntity? {
        return try {
            cacheDao.getHomeFeedEntity("home_feed")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveCachedHomeFeed(feed: CachedFeedData) {
        try {
            val jsonStr = json.encodeToString(feed)
            cacheDao.saveHomeFeed(
                HomeFeedCacheEntity(
                    id = "home_feed",
                    data = jsonStr,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            // Ignore cache write errors
        }
    }

    suspend fun getMovieLogo(id: Long, isTv: Boolean): String? {
        val mediaType = if (isTv) "tv" else "movie"
        val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
        val currentLang = if (rawLanguage == "system") java.util.Locale.getDefault().language else rawLanguage.substringBefore("-")
        val cacheKey = "logo:$mediaType:$id:$currentLang"
        (memoryDetailsCache[cacheKey] as? String)?.let { return it }

        // Check if full detail is already cached in memory or Room
        val cachedDetail = (memoryDetailsCache["$mediaType:$id"] as? com.cinetrack.data.api.MovieDetailResponse)
            ?: try {
                val jsonStr = cacheDao.getDetail(id, mediaType)
                jsonStr?.let { json.decodeFromString<com.cinetrack.data.api.MovieDetailResponse>(it) }
            } catch (e: Exception) { null }

        val cachedLogos = cachedDetail?.images?.logos
        val cachedFallback = cachedLogos?.let { logos ->
            (logos.firstOrNull { it.iso6391 == currentLang } ?: logos.firstOrNull { it.iso6391 == "en" } ?: logos.firstOrNull())?.filePath
        }
        if (cachedLogos != null && (currentLang == "en" || cachedLogos.any { it.iso6391 == currentLang })) {
            if (cachedFallback != null) {
                memoryDetailsCache[cacheKey] = cachedFallback
                return cachedFallback
            }
        }

        return try {
            val response = if (isTv) tmdbService.getTVBasicDetails(id) else tmdbService.getMovieBasicDetails(id)
            val logos = response.images?.logos
            val best = logos?.firstOrNull { it.iso6391 == currentLang } ?: logos?.firstOrNull { it.iso6391 == "en" } ?: logos?.firstOrNull()
            val path = best?.filePath ?: cachedFallback
            if (path != null) {
                memoryDetailsCache[cacheKey] = path
            }
            path
        } catch (e: Exception) {
            cachedFallback
        }
    }



    suspend fun searchMovies(query: String, page: Int = 1): List<Movie> = tmdbService.searchMovie(query, page = page).results
    suspend fun searchMovieWithYear(query: String, year: String?): Movie? {
        val results = tmdbService.searchMovie(query, year = year).results
        return results.firstOrNull()
    }
    
    override suspend fun searchMediaWithYear(query: String, year: String?, isTv: Boolean): Movie? {
        val cleanQuery = query.trim()
        val cleanYear = year?.trim()?.takeIf { it.isNotBlank() }
        if (isTv) {
            val tvResults = tmdbService.searchTV(cleanQuery, firstAirDateYear = cleanYear).results
            val match = tvResults.firstOrNull() ?: if (cleanYear != null) tmdbService.searchTV(cleanQuery).results.firstOrNull() else null
            return match?.let {
                try {
                    val details = fetchMovieDetails(it.id, true)
                    com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(details, "tv")
                } catch (e: Exception) {
                    it.copy(mediaType = "tv")
                }
            }
        } else {
            val movieResults = tmdbService.searchMovie(cleanQuery, year = cleanYear).results
            val match = movieResults.firstOrNull() ?: if (cleanYear != null) tmdbService.searchMovie(cleanQuery).results.firstOrNull() else null
            return match?.copy(mediaType = "movie")
        }
    }

    suspend fun searchMediaList(query: String, isTv: Boolean = false): List<Movie> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        try {
            if (isTv) {
                // Prendiamo solo i primi 5 risultati per non sovraccaricare le API
                val tvResults = tmdbService.searchTV(cleanQuery).results.take(5)
                
                // Usiamo async (ora importato correttamente) per scaricare in parallelo
                tvResults.map { basicResult ->
                    async {
                        try {
                            val details = fetchMovieDetails(basicResult.id, true)
                            com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(details, "tv")
                        } catch (e: Exception) {
                            basicResult.copy(mediaType = "tv")
                        }
                    }
                }.awaitAll()
            } else {
                // Per i film basta il risultato base
                val movieResults = tmdbService.searchMovie(cleanQuery).results.take(5)
                movieResults.map { it.copy(mediaType = "movie") }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun findByImdbId(imdbId: String): Movie? {
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

    override suspend fun findByTvdbId(tvdbId: String): Movie? {
        val response = tmdbService.findByExternalId(tvdbId, "tvdb_id")
        val tvRes = response.tvResults?.firstOrNull()
        if (tvRes != null) {
            try {
                val details = fetchMovieDetails(tvRes.id, true)
                return com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(details, "tv")
            } catch (e: Exception) {
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
        }
        return null
    }

    override suspend fun getMediaDetails(id: Long, isTv: Boolean): Movie? {
        return try {
            val response = fetchMovieDetails(id, isTv)
            com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(response, if (isTv) "tv" else "movie")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun healMissingPosters() = withContext(Dispatchers.IO) {
        val missing = favoriteDao.getMoviesWithMissingPosters()
        if (missing.isEmpty()) return@withContext
        for (item in missing) {
            try {
                val isTv = item.mediaType == "tv"
                var details = if (item.id > 0) getMediaDetails(item.id, isTv) else null
                if (details == null || details.posterPath.isNullOrBlank()) {
                    val titleToSearch = item.title ?: item.name
                    if (!titleToSearch.isNullOrBlank()) {
                        details = searchMediaWithYear(titleToSearch, item.releaseYear, isTv)
                    }
                }
                if (details != null && !details.posterPath.isNullOrBlank()) {
                    val updated = item.copy(
                        id = if (item.id <= 0) details.id else item.id,
                        title = if (item.title.isNullOrBlank()) details.title else item.title,
                        name = if (item.name.isNullOrBlank()) details.name else item.name,
                        posterPath = details.posterPath,
                        backdropPath = details.backdropPath ?: item.backdropPath,
                        overview = if (item.overview.isNullOrBlank()) details.overview else item.overview,
                        releaseDate = if (item.releaseDate.isNullOrBlank()) details.releaseDate else item.releaseDate,
                        firstAirDate = if (item.firstAirDate.isNullOrBlank()) details.firstAirDate else item.firstAirDate,
                        voteAverage = if (item.voteAverage == null || item.voteAverage == 0.0) details.voteAverage else item.voteAverage,
                        genreIds = if (item.genreIds.isNullOrEmpty()) details.genreIds else item.genreIds
                    )
                    favoriteDao.insert(updated)
                }
            } catch (_: Exception) {
                // Ignore individual item failures
            }
        }
    }
    
    suspend fun searchTV(query: String, page: Int = 1): List<Movie> = tmdbService.searchTV(query, page = page).results
    suspend fun searchMulti(query: String, page: Int = 1): List<TMDBSearchResult> = tmdbService.searchMulti(query, page = page).results
    fun getPersonDetailsFlow(id: Long): Flow<Person> = flow {
        val cacheKey = "person:$id"
        val inMemory = memoryDetailsCache[cacheKey] as? Person
        if (inMemory != null) {
            emit(inMemory)
        } else {
            try {
                val cachedJson = cacheDao.getDetail(id, "person")
                if (cachedJson != null) {
                    val parsed = json.decodeFromString<Person>(cachedJson)
                    memoryDetailsCache[cacheKey] = parsed
                    emit(parsed)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        try {
            val fresh = tmdbService.getPersonDetails(id)
            memoryDetailsCache[cacheKey] = fresh
            emit(fresh)
            try {
                cacheDao.saveDetailWithLRU(
                    MovieDetailCacheEntity(id = id, mediaType = "person", data = json.encodeToString(fresh), updatedAt = System.currentTimeMillis())
                )
            } catch (e: Exception) {}
        } catch (e: Exception) {
            if (memoryDetailsCache[cacheKey] == null) throw e
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getPersonDetails(id: Long): Person {
        val response = tmdbService.getPersonDetails(id)
        memoryDetailsCache["person:$id"] = response
        try {
            cacheDao.saveDetailWithLRU(MovieDetailCacheEntity(id = id, mediaType = "person", data = json.encodeToString(response), updatedAt = System.currentTimeMillis()))
        } catch (e: Exception) {}
        return response
    }

    suspend fun getCachedPersonProfilePath(id: Long): String? {
        val cacheKey = "person:$id"
        val inMemory = memoryDetailsCache[cacheKey] as? Person
        if (inMemory?.profilePath != null) return inMemory.profilePath
        try {
            val cachedJson = cacheDao.getDetail(id, "person")
            if (cachedJson != null) {
                val parsed = json.decodeFromString<Person>(cachedJson)
                memoryDetailsCache[cacheKey] = parsed
                return parsed.profilePath
            }
        } catch (e: Exception) { }
        return null
    }

    suspend fun fetchSeasonDetails(id: Long, seasonNumber: Int): Season = tmdbService.getSeasonDetails(id, seasonNumber)

    fun getCollectionDetailsFlow(id: Long): Flow<com.cinetrack.data.api.CollectionResponse> = flow {
        val cacheKey = "collection:$id"
        val inMemory = memoryDetailsCache[cacheKey] as? com.cinetrack.data.api.CollectionResponse
        if (inMemory != null) {
            emit(inMemory)
        } else {
            try {
                val cachedJson = cacheDao.getDetail(id, "collection")
                if (cachedJson != null) {
                    val parsed = json.decodeFromString<com.cinetrack.data.api.CollectionResponse>(cachedJson)
                    memoryDetailsCache[cacheKey] = parsed
                    emit(parsed)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        try {
            val fresh = tmdbService.getCollectionDetails(id)
            memoryDetailsCache[cacheKey] = fresh
            emit(fresh)
            try {
                cacheDao.saveDetailWithLRU(
                    MovieDetailCacheEntity(id = id, mediaType = "collection", data = json.encodeToString(fresh), updatedAt = System.currentTimeMillis())
                )
            } catch (e: Exception) {}
        } catch (e: Exception) {
            if (memoryDetailsCache[cacheKey] == null) throw e
        }
    }.flowOn(Dispatchers.IO)

    suspend fun fetchCollectionDetails(id: Long): com.cinetrack.data.api.CollectionResponse {
        val response = tmdbService.getCollectionDetails(id)
        memoryDetailsCache["collection:$id"] = response
        try {
            cacheDao.saveDetailWithLRU(MovieDetailCacheEntity(id = id, mediaType = "collection", data = json.encodeToString(response), updatedAt = System.currentTimeMillis()))
        } catch (e: Exception) {}
        return response
    }
    suspend fun getCollectionDetails(id: Long): com.cinetrack.data.api.CollectionResponse = fetchCollectionDetails(id)
    suspend fun getMovieDetail(id: Long, isTv: Boolean = false): com.cinetrack.data.api.MovieDetailResponse = fetchMovieDetails(id, isTv)
    suspend fun searchCollection(query: String, page: Int = 1): List<TMDBSearchResult.CollectionResult> = tmdbService.searchCollection(query, page = page).results
    suspend fun searchPeople(query: String, page: Int = 1): List<PersonSearchResult> = tmdbService.searchPeople(query, page = page).results
    suspend fun getMoviesByGenre(genreId: Long, page: Int = 1): List<Movie> = tmdbService.getMoviesByGenre(genreId, page = page).results
    suspend fun getTVShowsByGenre(genreId: Long, page: Int = 1): List<Movie> = tmdbService.getTVShowsByGenre(genreId, page = page).results
    suspend fun getPopularMovies(page: Int = 1): List<Movie> = tmdbService.getPopularMovies(page = page).results
    suspend fun getMovieRecommendations(id: Long, page: Int = 1): List<Movie> = tmdbService.getMovieRecommendations(id, page = page).results
    suspend fun getTVRecommendations(id: Long, page: Int = 1): List<Movie> = tmdbService.getTVRecommendations(id, page = page).results

    suspend fun getRegionFromPrefs(): String {
        val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
        return com.cinetrack.util.RegionResolver.resolveRegion(rawLanguage)
    }

    suspend fun ensureRegionalWatchProvidersLoaded(region: String? = null) {
        val targetRegion = (region ?: getRegionFromPrefs()).uppercase(java.util.Locale.ROOT)
        if (com.cinetrack.ui.utils.ProviderConstants.hasCachedRegion(targetRegion)) return
        try {
            coroutineScope {
                val movieDeferred = async {
                    runCatching { tmdbService.getWatchProvidersMovie(watchRegion = targetRegion).results }
                        .getOrDefault(emptyList())
                }
                val tvDeferred = async {
                    runCatching { tmdbService.getWatchProvidersTv(watchRegion = targetRegion).results }
                        .getOrDefault(emptyList())
                }
                val combined = movieDeferred.await() + tvDeferred.await()
                val providers = combined
                    .filter { !it.logoPath.isNullOrBlank() && it.providerName.isNotBlank() }
                    .groupBy { it.providerId }
                    .mapNotNull { (_, items) ->
                        val best = items.minByOrNull { item ->
                            item.displayPriorities?.get(targetRegion) ?: item.displayPriority ?: Int.MAX_VALUE
                        } ?: return@mapNotNull null
                        val priority = best.displayPriorities?.get(targetRegion) ?: best.displayPriority ?: Int.MAX_VALUE
                        priority to com.cinetrack.data.api.Provider(
                            providerId = best.providerId,
                            providerName = best.providerName,
                            logoPath = best.logoPath
                        )
                    }
                    .sortedBy { it.first }
                    .take(30)
                    .map { it.second }

                if (providers.isNotEmpty()) {
                    com.cinetrack.ui.utils.ProviderConstants.cacheRegionProviders(targetRegion, providers)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    suspend fun getNowPlayingMovies(page: Int = 1): List<Movie> = tmdbService.getNowPlayingMovies(page = page, region = getRegionFromPrefs()).results

    suspend fun getUpcomingMovies(page: Int = 1): List<Movie> = getRegionalUpcomingMoviesResponse(page).results

    suspend fun getRegionalUpcomingMoviesResponse(page: Int = 1): com.cinetrack.data.api.SearchResponse {
        val today = java.time.LocalDate.now()
        val nextMonth = today.plusWeeks(6)
        val options = mapOf(
            "primary_release_date.gte" to today.toString(),
            "primary_release_date.lte" to nextMonth.toString(),
            "region" to getRegionFromPrefs(),
            "with_release_type" to "2|3",
            "sort_by" to "popularity.desc"
        )
        return tmdbService.discoverMovies(page = page, options = options)
    }

    suspend fun getUpcomingMoviesResponse(page: Int = 1, region: String? = null): com.cinetrack.data.api.SearchResponse {
        val today = java.time.LocalDate.now()
        val nextMonth = today.plusWeeks(6)
        val options = mutableMapOf(
            "primary_release_date.gte" to today.toString(),
            "primary_release_date.lte" to nextMonth.toString(),
            "with_release_type" to "2|3",
            "sort_by" to "popularity.desc"
        )
        region?.let { options["region"] = it }
        return tmdbService.discoverMovies(page = page, options = options)
    }

    suspend fun getGlobalUpcomingMoviesResponse(page: Int = 1, sortBy: String = "popularity.desc"): com.cinetrack.data.api.SearchResponse {
        val today = java.time.LocalDate.now()
        val nextMonth = today.plusWeeks(6)
        val options = mapOf(
            "primary_release_date.gte" to today.toString(),
            "primary_release_date.lte" to nextMonth.toString(),
            "sort_by" to sortBy
        )
        return tmdbService.discoverMovies(page = page, options = options)
    }

    suspend fun getPopularTV(page: Int = 1): List<Movie> = tmdbService.getPopularTV(page = page).results
    suspend fun getAiringTodayTV(page: Int = 1): List<Movie> = tmdbService.getAiringTodayTV(page = page).results

    suspend fun getOnTheAirTV(page: Int = 1): List<Movie> = tmdbService.getOnTheAirTV(page = page).results

    suspend fun getUpcomingTV(page: Int = 1, sortBy: String = "popularity.desc"): List<Movie> {
        val today = java.time.LocalDate.now()
        val nextMonth = today.plusWeeks(6)
        return tmdbService.discoverTV(
            page = page,
            options = mapOf(
                "first_air_date.gte" to today.toString(),
                "first_air_date.lte" to nextMonth.toString(),
                "watch_region" to getRegionFromPrefs(),
                "sort_by" to sortBy
            )
        ).results.map { it.copy(mediaType = "tv") }
    }

    suspend fun getTrendingAll(page: Int = 1): List<Movie> = tmdbService.getTrendingAll(page = page).results
    suspend fun getTrendingMovies(page: Int = 1): List<Movie> = tmdbService.getTrendingMovies(page = page).results
    suspend fun getTrendingTV(page: Int = 1): List<Movie> = tmdbService.getTrendingTV(page = page).results
    suspend fun getTrendingPeople(page: Int = 1): List<PersonSearchResult> = tmdbService.getTrendingPeople(page = page).results
    suspend fun getPopularPeople(page: Int = 1): List<PersonSearchResult> = tmdbService.getPopularPeople(page = page).results

    suspend fun getTop10FlickTroveBoth(): Pair<List<Movie>, List<Movie>> =
        trendingStatsSyncDelegate.getTop10FlickTroveBoth { id, isTv -> getMovieDetail(id, isTv) }

    suspend fun getTop10FlickTrove(isTv: Boolean): List<Movie> =
        trendingStatsSyncDelegate.getTop10FlickTrove(isTv) { id, isTv -> getMovieDetail(id, isTv) }

    suspend fun discoverMoviesWithParams(page: Int = 1, options: Map<String, String>): List<Movie> {
        val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
        val resolvedLanguage = if (rawLanguage == "system") {
            java.util.Locale.getDefault().language
        } else {
            rawLanguage
        }
        val region = getRegionFromPrefs()

        val finalOptions = options.toMutableMap()
        finalOptions.putIfAbsent("language", resolvedLanguage)
        finalOptions.putIfAbsent("region", region)
        finalOptions.putIfAbsent("watch_region", region)

        val cleanedOptions = finalOptions.filterValues { it.isNotBlank() }
        return tmdbService.discoverMovies(page = page, options = cleanedOptions).results.map { it.copy(mediaType = "movie") }
    }

    suspend fun discoverTVWithParams(page: Int = 1, options: Map<String, String>): List<Movie> {
        val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
        val resolvedLanguage = if (rawLanguage == "system") {
            java.util.Locale.getDefault().language
        } else {
            rawLanguage
        }
        val region = getRegionFromPrefs()

        val finalOptions = options.toMutableMap()
        finalOptions.putIfAbsent("language", resolvedLanguage)
        finalOptions.putIfAbsent("watch_region", region)

        val cleanedOptions = finalOptions.filterValues { it.isNotBlank() }
        return tmdbService.discoverTV(page = page, options = cleanedOptions).results.map { it.copy(mediaType = "tv") }
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

    suspend fun fetchComments(id: String, tmdbId: Long, isTv: Boolean): List<com.cinetrack.data.api.TraktComment> {
        val type = if (isTv) "shows" else "movies"
        val traktComments = try {
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

        val tmdbReviews = try {
            val reviewsRes = if (isTv) tmdbService.getTVReviews(tmdbId) else tmdbService.getMovieReviews(tmdbId)
            reviewsRes.results.map { review ->
                com.cinetrack.data.api.TraktComment(
                    id = review.id.hashCode().toLong(),
                    created_at = review.createdAt,
                    comment = review.content,
                    review = true,
                    likes = review.authorDetails?.rating?.let { (it * 10).toInt() } ?: 0,
                    user = com.cinetrack.data.api.TraktUser(
                        username = review.authorDetails?.username ?: review.author,
                        name = review.authorDetails?.name?.takeIf { !it.isBlank() } ?: review.author
                    )
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }

        return (traktComments + tmdbReviews).distinctBy { it.comment?.take(40) }
    }

    // --- Global Stats (On-demand .get() con LRU Cache da 100 elementi e TTL di 30 min) ---
    fun getGlobalMovieStatsFlow(movieId: Long, mediaType: String): Flow<com.cinetrack.data.model.GlobalMovieStats?> = flow {
        val compositeId = "${mediaType}_${movieId}"
        val now = System.currentTimeMillis()

        // 1. Controlla la cache in memoria (0ms, 0 letture Firestore)
        val cached = synchronized(globalStatsLruCache) { globalStatsLruCache.get(compositeId) }
        if (cached != null && (now - cached.timestamp) < GLOBAL_STATS_TTL_MS) {
            emit(cached.stats)
            return@flow
        }

        // 2. Lettura singola on-demand via .get()
        val fetched: com.cinetrack.data.model.GlobalMovieStats? = try {
            val snapshot = kotlinx.coroutines.withTimeoutOrNull(5_000L) {
                firebaseRemoteDataSource.getGlobalMovieStats(compositeId).get().await()
            }
            if (snapshot != null && snapshot.exists()) {
                snapshot.toObject(com.cinetrack.data.model.GlobalMovieStats::class.java)
            } else {
                com.cinetrack.data.model.GlobalMovieStats()
            }
        } catch (e: Exception) {
            android.util.Log.w("MovieRepository", "Error fetching global stats for $compositeId: ${e.message}")
            cached?.stats ?: com.cinetrack.data.model.GlobalMovieStats()
        }

        synchronized(globalStatsLruCache) {
            globalStatsLruCache.put(compositeId, CachedGlobalStats(fetched, System.currentTimeMillis()))
        }
        emit(fetched)
    }

    suspend fun autoReconcileGlobalRatingIfMissing(
        movieId: Long,
        mediaType: String,
        globalStats: com.cinetrack.data.model.GlobalMovieStats?
    ) {
        val compositeId = "${mediaType}_${movieId}"
        val localMovie = favoriteDao.getById(movieId, mediaType) ?: return
        val rating = localMovie.personalRating
        if (rating == null || rating <= 0.0) return

        // Auto-reconcile ONLY if global stats currently has 0 votes (guaranteed zero double-count risk)
        if (globalStats == null || globalStats.ratingCount == 0L) {
            try {
                firebaseRemoteDataSource.updateGlobalMovieStats(
                    compositeId = compositeId,
                    addedVibes = emptyList(),
                    removedVibes = emptyList(),
                    newMvp = null,
                    oldMvp = null,
                    newRating = rating,
                    oldRating = null,
                    newStatus = if (localMovie.watched) "watched" else null,
                    oldStatus = null
                )
                synchronized(globalStatsLruCache) {
                    globalStatsLruCache.remove(compositeId)
                }
                android.util.Log.d("MovieRepository", "Auto-reconciled global rating ($rating) for $compositeId")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Auto-reconcile global rating failed for $compositeId", e)
            }
        }
    }
    // --- Watch History (Delegated to WatchHistoryRepository) ---
    fun getGlobalTimelineFlow() = watchHistoryRepository.getGlobalTimelineFlow()
    suspend fun updateWatchDate(id: Long, newWatchedAt: String) = watchHistoryRepository.updateWatchDate(id, newWatchedAt)
    suspend fun getWatchHistoryForMovie(movieId: Long, mediaType: String? = null) = watchHistoryRepository.getWatchHistoryForMovie(movieId, mediaType)
    fun getWatchHistoryForMovieFlow(movieId: Long, mediaType: String? = null) = watchHistoryRepository.getWatchHistoryForMovieFlow(movieId, mediaType)
    fun getAllWatchHistoryFlow() = watchHistoryRepository.getAllWatchHistoryFlow()
    suspend fun getAllWatchHistory() = watchHistoryRepository.getAllWatchHistory()
    suspend fun insertWatchHistory(history: com.cinetrack.data.local.entities.WatchHistoryEntity) = watchHistoryRepository.insertWatchHistory(history)
    suspend fun updateWatchHistory(history: com.cinetrack.data.local.entities.WatchHistoryEntity) = watchHistoryRepository.updateWatchHistory(history)
    suspend fun deleteWatchHistory(history: com.cinetrack.data.local.entities.WatchHistoryEntity) = watchHistoryRepository.deleteWatchHistory(history)
    suspend fun deleteWatchHistoryByMovieId(movieId: Long, mediaType: String? = null) = watchHistoryRepository.deleteWatchHistoryByMovieId(movieId, mediaType)
    suspend fun purgeHistoryForMovie(movieId: Long, mediaType: String? = null) = watchHistoryRepository.purgeHistoryForMovie(movieId, mediaType)
    suspend fun countDuplicateRewatches(): Int = watchHistoryRepository.countDuplicateRewatches()
    suspend fun cleanupAllRewatches(): Int = watchHistoryRepository.cleanupAllRewatches()

    suspend fun recordEpisodeWatchState(
        movie: Movie,
        changedEpisodes: List<Pair<Int, Int>>? = null,
        isAdding: Boolean = true,
        customDate: java.time.Instant? = null,
        isProgressAdvancing: Boolean = true
    ) {
        tvEpisodeWatchStateDelegate.recordEpisodeWatchState(
            movie = movie,
            changedEpisodes = changedEpisodes,
            isAdding = isAdding,
            customDate = customDate,
            isProgressAdvancing = isProgressAdvancing
        ) { updatedMovie ->
            saveMovie(updatedMovie)
        }
    }

    // --- Box Office ---
    suspend fun getWeekendBoxOffice(forceRefresh: Boolean = false): List<com.cinetrack.data.model.BoxOfficeMovie> =
        boxOfficeRepository.get().getWeekendBoxOffice(forceRefresh)

    suspend fun getAllTimeBoxOffice(year: Int? = null, page: Int = 1, forceRefresh: Boolean = false): List<com.cinetrack.data.model.BoxOfficeMovie> =
        boxOfficeRepository.get().getAllTimeBoxOffice(year, page, forceRefresh)
}

data class TraktRatingInfo(val rating: Double?, val votes: Int)
