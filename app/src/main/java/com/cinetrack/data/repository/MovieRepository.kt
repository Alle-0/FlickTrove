package com.cinetrack.data.repository

import com.cinetrack.data.model.Movie
import com.cinetrack.data.api.OmdbService
import com.cinetrack.data.api.TraktService
import com.cinetrack.data.api.Person
import com.cinetrack.data.api.PersonSearchResult
import com.cinetrack.data.model.Season
import com.cinetrack.data.api.TMDBService
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

@Singleton
class MovieRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val folderRepository: FolderRepository,
    private val cacheDao: CacheDao,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val tmdbService: TMDBService,
    private val omdbService: OmdbService,
    private val traktService: TraktService,
    private val firebaseRemoteDataSource: FirebaseRemoteDataSource,
    private val preferenceRepository: PreferenceRepository,
    @Named("tmdb_api_key") private val apiKey: String,
    @Named("omdb_api_key") private val omdbApiKey: String,
    @Named("trakt_api_key") private val traktApiKey: String,
    private val boxOfficeRepository: javax.inject.Provider<BoxOfficeRepository>,
    private val widgetNotifier: com.cinetrack.domain.WidgetNotifier,
    @ApplicationContext private val context: Context
) : MovieLookupService {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private val memoryDetailsCache = ConcurrentHashMap<String, Any>()
    private val memoryColorCache = ConcurrentHashMap<String, String>() // id -> hexColor

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

    suspend fun getMovie(id: Long, mediaType: String): Movie? = favoriteDao.getById(id, mediaType)

    /** Returns the movie even if it's pending_delete — used by sync workers to prevent resurrection */
    suspend fun getMovieIncludingDeleted(id: Long, mediaType: String): Movie? = favoriteDao.getByIdIncludingDeleted(id, mediaType)

    /** Physically removes a pending_delete row after the remote API confirms the deletion */
    suspend fun hardDeleteMovie(id: Long, mediaType: String) = favoriteDao.deleteById(id, mediaType)

    suspend fun getShowsForUpdate(limit: Int = 150): List<Movie> = favoriteDao.getShowsForUpdate(limit)

    suspend fun getUpcomingMoviesForUpdate(limit: Int = 150): List<Movie> = favoriteDao.getUpcomingMoviesForUpdate(limit)

    suspend fun saveMovie(movie: Movie, syncToTrakt: Boolean = true) {
        val oldMovie = favoriteDao.getById(movie.id, movie.mediaType)

        // 1. Update Room immediately
        val updatedMovie = movie.copy(
            syncStatus = "synced",
            clientUpdatedAt = if (syncToTrakt) System.currentTimeMillis() else (movie.clientUpdatedAt.takeIf { it > 0 } ?: oldMovie?.clientUpdatedAt ?: System.currentTimeMillis())
        )
        updatedMovie.emotionalVibes = movie.emotionalVibes
        updatedMovie.favoriteActorId = movie.favoriteActorId
        updatedMovie.favoriteActorName = movie.favoriteActorName
        updatedMovie.favoriteActorProfilePath = movie.favoriteActorProfilePath
        updatedMovie.favoriteActorTmdbPath = movie.favoriteActorTmdbPath
        updatedMovie.favoriteActorCharacter = movie.favoriteActorCharacter
        favoriteDao.insert(updatedMovie)
        
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
                        // Se è un rewatch, watchedAt NON è null (viene aggiornato con la data del rewatch).
                        // Se l'utente ha fatto WatchState.NONE, watchedAt diventa null.
                        // Non rimuoviamo dal server se è un rewatch, altrimenti cancelliamo anni di cronologia!
                        if (movie.watchedAt == null) {
                            enqueue(TraktInstantWriteWorker.ACTION_REMOVE_WATCHED)
                        }
                    }
                } else {
                    // I film funzionano in modo standard (acceso/spento)
                    val action = if (movie.watched) TraktInstantWriteWorker.ACTION_MARK_WATCHED
                                 else {
                                     // Per i film, stesso discorso: se watched = false ma watchedAt != null è un rewatch?
                                     // In realtà sui film logRewatch non mette watched = false. Lo fa solo WatchState.NONE.
                                     TraktInstantWriteWorker.ACTION_REMOVE_WATCHED
                                 }
                    enqueue(action)
                }
            }

            // rewatch for movies
            val oldWatchedAt = oldMovie?.watchedAt
            if (oldWatched == movie.watched && movie.watched && oldWatchedAt != movie.watchedAt && movie.watchedAt != null) {
                if (movie.mediaType == "movie") {
                    // For movies, if watchedAt changed but watched state is still true, it's a rewatch.
                    // Enqueuing ACTION_MARK_WATCHED will send a new play to Trakt/SIMKL.
                    enqueue(TraktInstantWriteWorker.ACTION_MARK_WATCHED)
                }
            }

            // dropped changed
            val oldDropped = oldMovie?.dropped ?: false
            if (oldDropped != movie.dropped) {
                val action = if (movie.dropped) TraktInstantWriteWorker.ACTION_MARK_DROPPED
                             else               TraktInstantWriteWorker.ACTION_REMOVE_DROPPED
                enqueue(action)
            }

            // personalRating changed
            if (oldRating != movie.personalRating) {
                val rating = movie.personalRating
                if (rating != null && rating > 0.0) {
                    val traktRating = rating.toInt().coerceIn(1, 10)
                    enqueue(TraktInstantWriteWorker.ACTION_ADD_RATING) {
                        putInt(TraktInstantWriteWorker.KEY_RATING, traktRating)
                    }
                } else if (oldRating != null && (rating == null || rating == 0.0)) {
                    enqueue(TraktInstantWriteWorker.ACTION_REMOVE_RATING)
                }
            }

            // favorite changed -> sync watchlist/favorite
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
                androidx.work.WorkManager.getInstance(context)
                    .enqueueUniqueWork("TRAKT_INSTANT_PUSH", androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE, workRequests)
            }
            
            // Enqueue SIMKL Instant Write
            val simklRequests = workRequests.map { traktReq ->
                androidx.work.OneTimeWorkRequestBuilder<SimklInstantWriteWorker>()
                    .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(traktReq.workSpec.input)
                    .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            }
            
            if (simklRequests.isNotEmpty()) {
                androidx.work.WorkManager.getInstance(context)
                    .enqueueUniqueWork("SIMKL_INSTANT_PUSH", androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE, simklRequests)
            }
        }
        
        // 2. Fire-and-forget to Firebase (SDK handles persistence/retry)
        repositoryScope.launch {
            try {
                firebaseRemoteDataSource.setMovie(movie)
                
                val oldVibes = oldMovie?.emotionalVibes?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
                val newVibes = movie.emotionalVibes?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
                
                val addedVibes = newVibes - oldVibes
                val removedVibes = oldVibes - newVibes
                
                val newMvp = movie.favoriteActorId
                val oldMvp = oldMovie?.favoriteActorId

                val oldRating = oldMovie?.personalRating
                val newRating = movie.personalRating
                
                val newStatus = if (movie.watched) "watched" else "unwatched"
                val oldStatus = if (oldMovie?.watched == true) "watched" else "unwatched"

                var calculatedViewsDelta = 0L
                if (movie.mediaType == "tv") {
                    val oldEps = oldMovie?.watchedEpisodes?.values?.sumOf { it.size } ?: 0
                    val newEps = movie.watchedEpisodes?.values?.sumOf { it.size } ?: 0
                    calculatedViewsDelta = (newEps - oldEps).toLong()
                } else {
                    if (newStatus == "watched" && oldStatus != "watched") calculatedViewsDelta = 1L
                    if (oldStatus == "watched" && newStatus != "watched") calculatedViewsDelta = -1L
                }

                if (addedVibes.isNotEmpty() || removedVibes.isNotEmpty() || newMvp != oldMvp || newRating != oldRating || calculatedViewsDelta != 0L) {
                    firebaseRemoteDataSource.updateGlobalMovieStats(
                        compositeId = movie.compositeId,
                        addedVibes = addedVibes.toList(),
                        removedVibes = removedVibes.toList(),
                        newMvp = newMvp,
                        oldMvp = oldMvp,
                        newRating = newRating,
                        oldRating = oldRating,
                        newStatus = newStatus,
                        oldStatus = oldStatus,
                        viewsDelta = calculatedViewsDelta
                    )
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
        watchHistoryRepository.purgeHistoryForMovie(targetMovie.id)
        widgetNotifier.notifyWidgetUpdated()

        // --- PUSH RIMOZIONE A TRAKT ---
        val builder = androidx.work.Data.Builder()
            .putString(TraktInstantWriteWorker.KEY_ACTION,     TraktInstantWriteWorker.ACTION_REMOVE_WATCHED)
            .putString(TraktInstantWriteWorker.KEY_MEDIA_TYPE, targetMovie.mediaType)
            .putLong(  TraktInstantWriteWorker.KEY_TMDB_ID,    targetMovie.id)
        if (targetMovie.imdbId != null) {
            builder.putString(TraktInstantWriteWorker.KEY_IMDB_ID, targetMovie.imdbId)
        }

        val workRequests = mutableListOf(
            OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(builder.build())
                .build()
        )

        // Fix smart-cast salvando in una variabile locale immutabile
        val currentRating = targetMovie.personalRating
        if (currentRating != null && currentRating > 0.0) {
            // Fix: uso di .putAll() al posto di fromData()
            val ratingBuilder = androidx.work.Data.Builder().putAll(builder.build())
                .putString(TraktInstantWriteWorker.KEY_ACTION, TraktInstantWriteWorker.ACTION_REMOVE_RATING)
            workRequests += OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                .setInputData(ratingBuilder.build())
                .build()
        }
        
        if (targetMovie.favorite) {
            // Fix: uso di .putAll() al posto di fromData()
            val watchlistBuilder = androidx.work.Data.Builder().putAll(builder.build())
                .putString(TraktInstantWriteWorker.KEY_ACTION, TraktInstantWriteWorker.ACTION_REMOVE_WATCHLIST)
            workRequests += OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
                .setInputData(watchlistBuilder.build())
                .build()
        }

        // --- PUSH RIMOZIONE A SIMKL ---
        val simklRequests = workRequests.map { traktReq ->
            OneTimeWorkRequestBuilder<SimklInstantWriteWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(traktReq.workSpec.input)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        }

        WorkManager.getInstance(context).enqueue(workRequests + simklRequests)
        // ------------------------------
        
        repositoryScope.launch {
            try {
                firebaseRemoteDataSource.deleteMovie(targetMovie.id, targetMovie.mediaType)

                // Rimuove il voto, le emotional vibes, l'attore MVP e le views dai global stats di Firestore
                val ratingToRemove = targetMovie.personalRating
                val vibesToRemove = targetMovie.emotionalVibes?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
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
                        addedVibes = emptyList(),
                        removedVibes = vibesToRemove,
                        newMvp = null,
                        oldMvp = mvpToRemove,
                        newRating = null,
                        oldRating = ratingToRemove,
                        newStatus = "unwatched",
                        oldStatus = if (targetMovie.watched) "watched" else "unwatched",
                        viewsDelta = calculatedViewsDelta
                    )
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
        watchHistoryRepository.purgeHistoryForMovie(movie.id)

        // --- PUSH RIMOZIONE A TRAKT ---
        val builder = androidx.work.Data.Builder()
            .putString(TraktInstantWriteWorker.KEY_ACTION,     TraktInstantWriteWorker.ACTION_REMOVE_WATCHED)
            .putString(TraktInstantWriteWorker.KEY_MEDIA_TYPE, movie.mediaType)
            .putLong(  TraktInstantWriteWorker.KEY_TMDB_ID,    movie.id)
        if (movie.imdbId != null) {
            builder.putString(TraktInstantWriteWorker.KEY_IMDB_ID, movie.imdbId)
        }

        val traktRequest = OneTimeWorkRequestBuilder<TraktInstantWriteWorker>()
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(builder.build())
            .build()
            
        val simklRequest = OneTimeWorkRequestBuilder<SimklInstantWriteWorker>()
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(builder.build())
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(listOf(traktRequest, simklRequest))

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

    // --- Remote Sync (The "Bunker" recovery) ---
    suspend fun pushPendingChanges() = kotlinx.coroutines.withContext(Dispatchers.IO) {
        // Push pending Movies
        val pendingMovies = favoriteDao.getPendingSync()
        val toDelete = pendingMovies.filter { it.syncStatus == "pending_delete" }
        val toSync = pendingMovies.filter { it.syncStatus == "pending" }.map { it.copy(syncStatus = "synced") }

        for (movie in toDelete) {
            try {
                firebaseRemoteDataSource.deleteMovie(movie.id, movie.mediaType)
                favoriteDao.deleteById(movie.id, movie.mediaType)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Failed to push pending delete movie ${movie.id}", e)
            }
        }

        if (toSync.isNotEmpty()) {
            try {
                firebaseRemoteDataSource.setMoviesBulk(toSync)
                for (movie in toSync) {
                    favoriteDao.updateSyncStatus(movie.id, movie.mediaType, "synced")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Failed to push pending movies bulk", e)
            }
        }
        
        // Push pending Folders
        folderRepository.pushPendingFolders()
        
        // Push pending Watch History
        watchHistoryRepository.pushPendingWatchHistory()
        
    }

    suspend fun syncWithFirebase(
        force: Boolean = false,
        onProgress: (suspend (SyncProgress) -> Unit)? = null
    ) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        suspend fun emit(message: UiText, progress: Float?) {
            onProgress?.invoke(SyncProgress(message, progress))
        }

        // Rate limiting cooldown guard (5 minutes)
        val lastSync = preferenceRepository.userPreferencesFlow.first().lastSyncTimestamp
        if (!force && System.currentTimeMillis() - lastSync < 300_000L) {
            android.util.Log.d("MovieRepository", "Firebase Sync skipped - last sync was less than 5 minutes ago.")
            emit(UiText.StringResource(R.string.sync_msg_cached), 1f)
            return@withContext
        }

        try {
            // 0. Push any pending local changes first (bounded by timeout so network stalls never freeze the sync)
            android.util.Log.d("MovieRepository", "Starting Firebase Sync - Pushing pending changes...")
            emit(UiText.StringResource(R.string.sync_msg_pushing_changes), 0f)
            kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                pushPendingChanges()
            }

            // 1. Pull & Reconcile Favorites
            android.util.Log.d("MovieRepository", "Starting Firebase Sync - Fetching Favorites...")
            emit(UiText.StringResource(R.string.sync_msg_fetching_favorites), null)
            val remoteFavorites = firebaseRemoteDataSource.fetchAllFavorites()
            android.util.Log.d("MovieRepository", "Fetched ${remoteFavorites.size} favorites from Firebase")
            
            emit(UiText.StringResource(R.string.sync_msg_syncing_favorites), 0.35f)
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
                    val currentCount = (index + 1) * chunk.size
                    emit(UiText.StringResource(R.string.sync_msg_saving_favorites, currentCount, moviesToInsert.size), progress)
                }
            }
            
            for (movieToDelete in moviesToDelete) {
                favoriteDao.deleteById(movieToDelete.id, movieToDelete.mediaType)
            }
            android.util.Log.d("MovieRepository", "Successfully synchronized favorites with conflict resolution")

            // 2. Pull & Reconcile Folders
            emit(UiText.StringResource(R.string.sync_msg_fetching_folders), null)
            val remoteFolders = firebaseRemoteDataSource.fetchAllFolders()
            
            emit(UiText.StringResource(R.string.sync_msg_syncing_folders), 0.7f)
            folderRepository.syncFoldersFromRemote(remoteFolders) { currentCount, totalCount, portion ->
                val progress = 0.7f + (0.9f - 0.7f) * portion
                emit(UiText.StringResource(R.string.sync_msg_saving_folders, currentCount, totalCount), progress)
            }

            // 3. Pull & Reconcile Watch History
            val remoteHistory = firebaseRemoteDataSource.fetchAllWatchHistory()
            watchHistoryRepository.syncWatchHistoryFromRemote(remoteHistory)

            // 4. Pull Preferences
            emit(UiText.StringResource(R.string.sync_msg_syncing_preferences), 0.92f)
            syncPreferencesWithFirebase()
            preferenceRepository.updateLastSyncTimestamp(System.currentTimeMillis())
            
            emit(UiText.StringResource(R.string.sync_msg_completed), 1f)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e("MovieRepository", "Error during Firebase synchronization, aborting sync to prevent data loss.", e)
            emit(UiText.StringResource(R.string.sync_msg_error), 1f)
        }
    }

    fun enqueueSyncWorker() {
        try {
            val syncConstraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            val syncRequest = OneTimeWorkRequestBuilder<com.cinetrack.data.sync.SyncWorker>()
                .setConstraints(syncConstraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                com.cinetrack.data.sync.SyncWorker.WORK_NAME,
                androidx.work.ExistingWorkPolicy.KEEP,
                syncRequest
            )
            android.util.Log.d("MovieRepository", "SyncWorker enqueued successfully via WorkManager")
        } catch (e: Exception) {
            android.util.Log.e("MovieRepository", "Failed to enqueue SyncWorker", e)
        }
    }

    suspend fun clearAllData() {
        favoriteDao.clearAll()
        folderRepository.clearAll()
        preferenceRepository.clearAll()
    }

    suspend fun syncPreferencesWithFirebase() {
        val remotePrefs = firebaseRemoteDataSource.fetchUserPreferences() ?: return
        
        try {
            val currentPrefs = preferenceRepository.userPreferencesFlow
                // Fix: Emesso UserPreferences() corretto al posto di SyncProgress
                .catch { emit(com.cinetrack.data.model.UserPreferences()) }
                .firstOrNull() ?: com.cinetrack.data.model.UserPreferences()
                
            // Helper to parse SortConfig from Firebase Map
            fun parseSortConfig(map: Any?, default: com.cinetrack.data.model.SortConfig): com.cinetrack.data.model.SortConfig {
                if (map !is Map<*, *>) return default
                return com.cinetrack.data.model.SortConfig(
                    sortType = map["sortType"] as? String ?: default.sortType,
                    sortDirection = map["sortDirection"] as? String ?: default.sortDirection,
                    selectedGenres = (map["selectedGenres"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: default.selectedGenres,
                    selectedProviders = (map["selectedProviders"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: default.selectedProviders,
                    selectedDecades = (map["selectedDecades"] as? List<*>)?.filterIsInstance<String>() ?: default.selectedDecades
                )
            }

            // Helper to parse DiscoveryFilters from Firebase Map
            fun parseDiscoveryFilters(map: Any?, default: com.cinetrack.data.model.DiscoveryFilters): com.cinetrack.data.model.DiscoveryFilters {
                if (map !is Map<*, *>) return default
                return com.cinetrack.data.model.DiscoveryFilters(
                    selectedGenres = (map["selectedGenres"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: default.selectedGenres,
                    selectedProviders = (map["selectedProviders"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: default.selectedProviders,
                    selectedDecades = (map["selectedDecades"] as? List<*>)?.filterIsInstance<String>() ?: default.selectedDecades,
                    sortBy = map["sortBy"] as? String ?: default.sortBy
                )
            }

            val updatedPrefs = currentPrefs.copy(
                gridColumns = (remotePrefs["gridColumns"] as? Number)?.toInt() ?: currentPrefs.gridColumns,
                showLayoutToggle = remotePrefs["showLayoutToggle"] as? Boolean ?: currentPrefs.showLayoutToggle,
                isSearchSuggestionsExpanded = remotePrefs["isSearchSuggestionsExpanded"] as? Boolean ?: currentPrefs.isSearchSuggestionsExpanded,
                notificationsReleases = remotePrefs["notificationsReleases"] as? Boolean ?: currentPrefs.notificationsReleases,
                notificationsSocial = remotePrefs["notificationsSocial"] as? Boolean ?: currentPrefs.notificationsSocial,
                showFolderBookmarks = remotePrefs["showFolderBookmarks"] as? Boolean ?: currentPrefs.showFolderBookmarks,
                showBadges = remotePrefs["showBadges"] as? Boolean ?: currentPrefs.showBadges,
                disabledBadges = (remotePrefs["disabledBadges"] as? List<*>)?.filterIsInstance<String>()?.toSet() ?: currentPrefs.disabledBadges,
                vibrationEnabled = remotePrefs["vibrationEnabled"] as? Boolean ?: currentPrefs.vibrationEnabled,
                accentColor = remotePrefs["accentColor"] as? String ?: currentPrefs.accentColor,
                appTheme = remotePrefs["appTheme"] as? String ?: currentPrefs.appTheme,
                contentLanguage = remotePrefs["contentLanguage"] as? String ?: currentPrefs.contentLanguage,
                advancedVisualEffectsEnabled = remotePrefs["advancedVisualEffectsEnabled"] as? Boolean ?: currentPrefs.advancedVisualEffectsEnabled,
                dynamicAppIconEnabled = remotePrefs["dynamicAppIconEnabled"] as? Boolean ?: currentPrefs.dynamicAppIconEnabled,
                showSplitReleasesHome = remotePrefs["showSplitReleasesHome"] as? Boolean ?: currentPrefs.showSplitReleasesHome,
                showSplitDroppedHome = remotePrefs["showSplitDroppedHome"] as? Boolean ?: currentPrefs.showSplitDroppedHome,
                showAppEntryAnimation = remotePrefs["showAppEntryAnimation"] as? Boolean ?: currentPrefs.showAppEntryAnimation,
                useMovieLogo = remotePrefs["useMovieLogo"] as? Boolean ?: currentPrefs.useMovieLogo,
                defaultStartTab = remotePrefs["defaultStartTab"] as? String ?: currentPrefs.defaultStartTab,
                defaultStartMedia = remotePrefs["defaultStartMedia"] as? String ?: currentPrefs.defaultStartMedia,
                showMyFolders = remotePrefs["showMyFolders"] as? Boolean ?: currentPrefs.showMyFolders,
                showYourFlow = remotePrefs["showYourFlow"] as? Boolean ?: currentPrefs.showYourFlow,
                titleTextSizeMultiplier = (remotePrefs["titleTextSizeMultiplier"] as? Number)?.toFloat() ?: currentPrefs.titleTextSizeMultiplier,
                imageQuality = remotePrefs["imageQuality"] as? String ?: currentPrefs.imageQuality,
                homeSort = parseSortConfig(remotePrefs["homeSort"], currentPrefs.homeSort),
                vistiSort = parseSortConfig(remotePrefs["vistiSort"], currentPrefs.vistiSort),
                foldersSort = parseSortConfig(remotePrefs["foldersSort"], currentPrefs.foldersSort),
                discoveryFilters = parseDiscoveryFilters(remotePrefs["discoveryFilters"], currentPrefs.discoveryFilters),
                showHomeContinueWatching = remotePrefs["showHomeContinueWatching"] as? Boolean ?: currentPrefs.showHomeContinueWatching,
                showHomeBoxOffice = remotePrefs["showHomeBoxOffice"] as? Boolean ?: currentPrefs.showHomeBoxOffice,
                showHomeWatchlist = remotePrefs["showHomeWatchlist"] as? Boolean ?: currentPrefs.showHomeWatchlist,
                showHomeBecauseYouWatched = remotePrefs["showHomeBecauseYouWatched"] as? Boolean ?: currentPrefs.showHomeBecauseYouWatched,
                homeSectionOrder = (remotePrefs["homeSectionOrder"] as? List<*>)?.filterIsInstance<String>()?.let {
                    com.cinetrack.data.model.HomeFeedSectionConstants.sanitizeOrder(it)
                } ?: currentPrefs.homeSectionOrder,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            
            preferenceRepository.updateAll(updatedPrefs)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e("MovieRepository", "Error syncing preferences: ${e.message}")
        }
    }

    suspend fun savePreferencesRemote(prefs: com.cinetrack.data.model.UserPreferences) {
        repositoryScope.launch {
            try {
                // Convert to Map for Firestore
                val prefsMap = mapOf(
                    "gridColumns" to prefs.gridColumns,
                    "showLayoutToggle" to prefs.showLayoutToggle,
                    "isSearchSuggestionsExpanded" to prefs.isSearchSuggestionsExpanded,
                    "notificationsReleases" to prefs.notificationsReleases,
                    "notificationsSocial" to prefs.notificationsSocial,
                    "showFolderBookmarks" to prefs.showFolderBookmarks,
                    "showBadges" to prefs.showBadges,
                    "disabledBadges" to prefs.disabledBadges.toList(),
                    "vibrationEnabled" to prefs.vibrationEnabled,
                    "accentColor" to prefs.accentColor,
                    "appTheme" to prefs.appTheme,
                    "contentLanguage" to prefs.contentLanguage,
                    "advancedVisualEffectsEnabled" to prefs.advancedVisualEffectsEnabled,
                    "dynamicAppIconEnabled" to prefs.dynamicAppIconEnabled,
                    "showSplitReleasesHome" to prefs.showSplitReleasesHome,
                    "showSplitDroppedHome" to prefs.showSplitDroppedHome,
                    "showAppEntryAnimation" to prefs.showAppEntryAnimation,
                    "useMovieLogo" to prefs.useMovieLogo,
                    "defaultStartTab" to prefs.defaultStartTab,
                    "defaultStartMedia" to prefs.defaultStartMedia,
                    "showMyFolders" to prefs.showMyFolders,
                    "showYourFlow" to prefs.showYourFlow,
                    "titleTextSizeMultiplier" to prefs.titleTextSizeMultiplier.toDouble(),
                    "imageQuality" to prefs.imageQuality,
                    "homeSort" to mapOf(
                        "sortType" to prefs.homeSort.sortType,
                        "sortDirection" to prefs.homeSort.sortDirection,
                        "selectedGenres" to prefs.homeSort.selectedGenres,
                        "selectedProviders" to prefs.homeSort.selectedProviders,
                        "selectedDecades" to prefs.homeSort.selectedDecades
                    ),
                    "vistiSort" to mapOf(
                        "sortType" to prefs.vistiSort.sortType,
                        "sortDirection" to prefs.vistiSort.sortDirection,
                        "selectedGenres" to prefs.vistiSort.selectedGenres,
                        "selectedProviders" to prefs.vistiSort.selectedProviders,
                        "selectedDecades" to prefs.vistiSort.selectedDecades
                    ),
                    "foldersSort" to mapOf(
                        "sortType" to prefs.foldersSort.sortType,
                        "sortDirection" to prefs.foldersSort.sortDirection,
                        "selectedGenres" to prefs.foldersSort.selectedGenres,
                        "selectedProviders" to prefs.foldersSort.selectedProviders,
                        "selectedDecades" to prefs.foldersSort.selectedDecades
                    ),
                    "discoveryFilters" to mapOf(
                        "selectedGenres" to prefs.discoveryFilters.selectedGenres,
                        "selectedProviders" to prefs.discoveryFilters.selectedProviders,
                        "selectedDecades" to prefs.discoveryFilters.selectedDecades,
                        "sortBy" to prefs.discoveryFilters.sortBy
                    ),
                    "showHomeContinueWatching" to prefs.showHomeContinueWatching,
                    "showHomeBoxOffice" to prefs.showHomeBoxOffice,
                    "showHomeWatchlist" to prefs.showHomeWatchlist,
                    "showHomeBecauseYouWatched" to prefs.showHomeBecauseYouWatched,
                    "homeSectionOrder" to prefs.homeSectionOrder
                )
                firebaseRemoteDataSource.setUserPreferences(prefsMap)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("MovieRepository", "Error saving preferences remote", e)
            }
        }
    }

    // --- Search History (Delegated to SearchHistoryRepository) ---
    fun getRecentSearches(): Flow<List<String>> = searchHistoryRepository.getRecentSearches()

    suspend fun saveSearchQuery(query: String) {
        searchHistoryRepository.saveSearchQuery(query)
    }

    suspend fun deleteSearchQuery(query: String) {
        searchHistoryRepository.deleteSearchQuery(query)
    }

    suspend fun clearRecentSearches() {
        searchHistoryRepository.clearRecentSearches()
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
        val cacheKey = "logo:$mediaType:$id"
        (memoryDetailsCache[cacheKey] as? String)?.let { return it }

        // Check if full detail is already cached in memory or Room
        val cachedDetail = (memoryDetailsCache["$mediaType:$id"] as? com.cinetrack.data.api.MovieDetailResponse)
            ?: try {
                val jsonStr = cacheDao.getDetail(id, mediaType)
                jsonStr?.let { json.decodeFromString<com.cinetrack.data.api.MovieDetailResponse>(it) }
            } catch (e: Exception) { null }

        val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
        val currentLang = if (rawLanguage == "system") java.util.Locale.getDefault().language else rawLanguage

        if (cachedDetail?.images?.logos != null) {
            val logos = cachedDetail.images.logos
            val best = logos.firstOrNull { it.iso6391 == currentLang } ?: logos.firstOrNull { it.iso6391 == "en" } ?: logos.firstOrNull()
            val path = best?.filePath
            if (path != null) {
                memoryDetailsCache[cacheKey] = path
                return path
            }
        }

        return try {
            val response = if (isTv) tmdbService.getTVBasicDetails(id) else tmdbService.getMovieBasicDetails(id)
            val logos = response.images?.logos
            val best = logos?.firstOrNull { it.iso6391 == currentLang } ?: logos?.firstOrNull { it.iso6391 == "en" } ?: logos?.firstOrNull()
            val path = best?.filePath
            if (path != null) {
                memoryDetailsCache[cacheKey] = path
            }
            path
        } catch (e: Exception) {
            null
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
        return if (rawLanguage == "system") {
            val systemCountry = java.util.Locale.getDefault().country
            if (systemCountry.isNotBlank() && systemCountry.length == 2) {
                systemCountry.uppercase()
            } else {
                when (java.util.Locale.getDefault().language.lowercase()) {
                    "it" -> "IT"
                    "es" -> "ES"
                    "fr" -> "FR"
                    "de" -> "DE"
                    "pt" -> "PT"
                    "ru" -> "RU"
                    "hi" -> "IN"
                    "ja" -> "JP"
                    "ko" -> "KR"
                    "zh" -> "CN"
                    "id", "in" -> "ID"
                    "tr" -> "TR"
                    else -> "US"
                }
            }
        } else {
            when (rawLanguage.lowercase().substringBefore("-")) {
                "it" -> "IT"
                "es" -> "ES"
                "fr" -> "FR"
                "de" -> "DE"
                "pt" -> "PT"
                "ru" -> "RU"
                "hi" -> "IN"
                "ja" -> "JP"
                "ko" -> "KR"
                "zh" -> "CN"
                "id", "in" -> "ID"
                "tr" -> "TR"
                else -> "US"
            }
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

    suspend fun getTop10FlickTroveBoth(): Pair<List<Movie>, List<Movie>> {
        val (movieIds, tvIds) = firebaseRemoteDataSource.fetchTop10MonthlyBoth()
        return kotlinx.coroutines.coroutineScope {
            val moviesDeferred = async {
                movieIds.mapNotNull { compositeId ->
                    val idStr = compositeId.removePrefix("movie_")
                    val id = idStr.toLongOrNull() ?: return@mapNotNull null
                    try {
                        val detail = getMovieDetail(id, isTv = false)
                        Movie(
                            id = detail.id.toLong(),
                            title = detail.title ?: "",
                            posterPath = detail.posterPath,
                            backdropPath = detail.backdropPath,
                            releaseDate = detail.releaseDate ?: "",
                            overview = detail.overview ?: "",
                            voteAverage = detail.voteAverage ?: 0.0,
                            voteCount = detail.voteCount ?: 0,
                            genreIds = detail.genres?.map { it.id } ?: emptyList(),
                            mediaType = "movie",
                            runtime = detail.runtime
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            val tvDeferred = async {
                tvIds.mapNotNull { compositeId ->
                    val idStr = compositeId.removePrefix("tv_")
                    val id = idStr.toLongOrNull() ?: return@mapNotNull null
                    try {
                        val detail = getMovieDetail(id, isTv = true)
                        Movie(
                            id = detail.id.toLong(),
                            title = detail.name ?: detail.title ?: "",
                            posterPath = detail.posterPath,
                            backdropPath = detail.backdropPath,
                            releaseDate = detail.firstAirDate ?: detail.releaseDate ?: "",
                            overview = detail.overview ?: "",
                            voteAverage = detail.voteAverage ?: 0.0,
                            voteCount = detail.voteCount ?: 0,
                            genreIds = detail.genres?.map { it.id } ?: emptyList(),
                            mediaType = "tv",
                            runtime = detail.runtime
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            Pair(moviesDeferred.await(), tvDeferred.await())
        }
    }

    suspend fun getTop10FlickTrove(isTv: Boolean): List<Movie> {
        val (movies, tv) = getTop10FlickTroveBoth()
        return if (isTv) tv else movies
    }

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

    // --- Global Stats ---
    fun getGlobalMovieStatsFlow(movieId: Long, mediaType: String): Flow<com.cinetrack.data.model.GlobalMovieStats?> {
        val compositeId = "${mediaType}_${movieId}"
        return callbackFlow {
            val listener = firebaseRemoteDataSource.getGlobalMovieStats(compositeId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val stats = snapshot.toObject(com.cinetrack.data.model.GlobalMovieStats::class.java)
                        trySend(stats)
                    } else {
                        trySend(com.cinetrack.data.model.GlobalMovieStats())
                    }
                }
            awaitClose { listener.remove() }
        }
    }
    // --- Watch History (Delegated to WatchHistoryRepository) ---
    suspend fun getWatchHistoryForMovie(movieId: Long) = watchHistoryRepository.getWatchHistoryForMovie(movieId)
    fun getWatchHistoryForMovieFlow(movieId: Long) = watchHistoryRepository.getWatchHistoryForMovieFlow(movieId)
    fun getAllWatchHistoryFlow() = watchHistoryRepository.getAllWatchHistoryFlow()
    suspend fun getAllWatchHistory() = watchHistoryRepository.getAllWatchHistory()
    suspend fun insertWatchHistory(history: com.cinetrack.data.local.entities.WatchHistoryEntity) = watchHistoryRepository.insertWatchHistory(history)
    suspend fun updateWatchHistory(history: com.cinetrack.data.local.entities.WatchHistoryEntity) = watchHistoryRepository.updateWatchHistory(history)
    suspend fun deleteWatchHistory(history: com.cinetrack.data.local.entities.WatchHistoryEntity) = watchHistoryRepository.deleteWatchHistory(history)
    suspend fun deleteWatchHistoryByMovieId(movieId: Long) = watchHistoryRepository.deleteWatchHistoryByMovieId(movieId)

    // --- Box Office ---
    suspend fun getWeekendBoxOffice(forceRefresh: Boolean = false): List<com.cinetrack.data.model.BoxOfficeMovie> =
        boxOfficeRepository.get().getWeekendBoxOffice(forceRefresh)

    suspend fun getAllTimeBoxOffice(year: Int? = null, page: Int = 1, forceRefresh: Boolean = false): List<com.cinetrack.data.model.BoxOfficeMovie> =
        boxOfficeRepository.get().getAllTimeBoxOffice(year, page, forceRefresh)
}

data class TraktRatingInfo(val rating: Double?, val votes: Int)
