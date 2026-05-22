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
    private val updateEpisodesUseCase: UpdateEpisodesUseCase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- Local Operations (Bunker: Room First) ---
    suspend fun getLocalMovies(): List<Movie> = favoriteDao.getAll()
    
    fun getLocalMoviesFlow(): Flow<List<Movie>> = favoriteDao.getAllFlow()

    fun getMovieFlow(id: Long, mediaType: String): Flow<Movie?> = favoriteDao.getByIdFlow(id, mediaType)

    suspend fun getMovie(id: Long, mediaType: String): Movie? = favoriteDao.getById(id, mediaType)

    suspend fun saveMovie(movie: Movie) {
        // 1. Update Room immediately
        favoriteDao.insert(movie.copy(syncStatus = "synced", clientUpdatedAt = System.currentTimeMillis()))
        
        // Notify Widget of changes
        com.cinetrack.widget.WidgetUpdater.update(context)
        
        // 2. Fire-and-forget to Firebase (SDK handles persistence/retry)
        repositoryScope.launch {
            try {
                firebaseRemoteDataSource.setMovie(movie)
            } catch (e: Exception) {
                android.util.Log.e("MovieRepository", "Firebase sync failed for ${movie.id}", e)
                favoriteDao.updateSyncStatus(movie.id, movie.mediaType, "pending")
            }
        }
    }

    suspend fun cycleMovieStatus(movie: Movie) {
        android.util.Log.d("MovieRepository", "cycleMovieStatus START: id=${movie.id}, title=${movie.title ?: movie.name}, watched=${movie.watched}, fav=${movie.favorite}, rem=${movie.reminder}, released=${movie.isReleased}, mediaType=${movie.mediaType}")
        
        val updated = when {
            // Case 1: State: Watched (Check) -> Idempotent (Stay Watched)
            movie.watched -> {
                android.util.Log.d("MovieRepository", "cycleMovieStatus: Branch [Watched -> Stay Watched] (Action ignored)")
                movie
            }
            
            // Case 2: State: To See (Eye/Bell) -> Next step
            movie.favorite || movie.reminder -> {
                if (movie.isReleased) {
                    android.util.Log.d("MovieRepository", "cycleMovieStatus: Branch [To See -> Watched] (isReleased=true)")
                    // Released: Move to Watched (Check)
                    if (movie.mediaType == "tv") {
                        updateEpisodesUseCase.markAllWatched(movie).copy(
                            favorite = false,
                            reminder = false, // Explicitly clear both
                            clientUpdatedAt = System.currentTimeMillis()
                        )
                    } else {
                        movie.copy(
                            favorite = false,
                            reminder = false,
                            watched = true,
                            watchedAt = java.time.Instant.now().toString(),
                            clientUpdatedAt = System.currentTimeMillis()
                        )
                    }
                } else {
                    android.util.Log.d("MovieRepository", "cycleMovieStatus: Branch [To See -> Untracked] (isReleased=false, toggling OFF reminder)")
                    // Unreleased: Toggle OFF reminder
                    movie.copy(
                        favorite = false,
                        reminder = false,
                        watched = false,
                        clientUpdatedAt = System.currentTimeMillis()
                    )
                }
            }
            
            // Case 3: State: Untracked (+) -> Move to To See (Eye/Bell)
            else -> {
                if (movie.isReleased) {
                    android.util.Log.d("MovieRepository", "cycleMovieStatus: Branch [Untracked -> To See (Eye)] (isReleased=true)")
                    movie.copy(
                        favorite = true,
                        reminder = false,
                        watched = false,
                        clientUpdatedAt = System.currentTimeMillis()
                    )
                } else {
                    android.util.Log.d("MovieRepository", "cycleMovieStatus: Branch [Untracked -> To See (Bell)] (isReleased=false)")
                    movie.copy(
                        favorite = false,
                        reminder = true,
                        watched = false,
                        clientUpdatedAt = System.currentTimeMillis()
                    )
                }
            }
        }
        
        android.util.Log.d("MovieRepository", "cycleMovieStatus: [${updated.title}] isReleased=${updated.isReleased} (date=${updated.releaseDate})")
        android.util.Log.d("MovieRepository", "cycleMovieStatus END: id=${updated.id}, watched=${updated.watched}, fav=${updated.favorite}, rem=${updated.reminder}")
        
        if (updated != movie) {
            android.util.Log.d("MovieRepository", "cycleMovieStatus: Saving updated movie")
            saveMovie(updated)
        } else {
            android.util.Log.d("MovieRepository", "cycleMovieStatus: No changes to save")
        }
    }

    suspend fun deleteMovie(movie: Movie) {
        favoriteDao.deleteById(movie.id, movie.mediaType)
        
        // Notify Widget of changes
        com.cinetrack.widget.WidgetUpdater.update(context)
        
        repositoryScope.launch {
            firebaseRemoteDataSource.deleteMovie(movie.id, movie.mediaType)
        }
    }

    suspend fun markAsDeleted(movie: Movie) {
        favoriteDao.markDeleted(movie.id, movie.mediaType)
        repositoryScope.launch {
            firebaseRemoteDataSource.deleteMovie(movie.id, movie.mediaType)
        }
    }

    // --- Folder Operations ---
    fun getFoldersFlow(): Flow<List<FolderEntity>> = folderDao.getAllFlow()

    fun getFolderFlow(folderId: String): Flow<FolderEntity?> = folderDao.getByIdFlow(folderId)

    fun getMoviesByCompositeIds(compositeIds: List<String>): Flow<List<Movie>> = favoriteDao.getByCompositeIds(compositeIds)

    suspend fun saveFolder(folderEntity: FolderEntity) {
        folderDao.insert(folderEntity.copy(syncStatus = "synced", clientUpdatedAt = System.currentTimeMillis()))
        repositoryScope.launch {
            val folder = Folder(
                id = folderEntity.id,
                name = folderEntity.name,
                icon = folderEntity.icon,
                color = folderEntity.color,
                description = folderEntity.description,
                itemIds = folderEntity.itemIds,
                createdAt = folderEntity.createdAt,
                updatedAt = folderEntity.updatedAt
            )
            firebaseRemoteDataSource.setFolder(folder)
        }
    }

    suspend fun deleteFolder(folderId: String) {
        folderDao.markDeleted(folderId)
        repositoryScope.launch {
            try {
                firebaseRemoteDataSource.deleteFolder(folderId)
                folderDao.deleteById(folderId)
            } catch (e: Exception) {
                // Keep as pending_delete for sync retry
            }
        }
    }

    // --- Remote Sync (The "Bunker" recovery) ---
    suspend fun pushPendingChanges() {
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
                        updatedAt = folder.updatedAt
                    )
                    firebaseRemoteDataSource.setFolder(folderDto)
                    folderDao.updateSyncStatus(folder.id, "synced")
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieRepository", "Failed to push pending folder ${folder.id}", e)
            }
        }
    }

    suspend fun syncWithFirebase(
        forcedUid: String? = null,
        onProgress: (suspend (SyncProgress) -> Unit)? = null
    ) {
        suspend fun emit(message: String, progress: Float?) {
            onProgress?.invoke(SyncProgress(message, progress))
        }

        // 0. Push any pending local changes first
        android.util.Log.d("MovieRepository", "Starting Firebase Sync - Pushing pending changes...")
        emit("Carico modifiche in sospeso...", 0f)
        pushPendingChanges()

        // 1. Pull Favorites
        android.util.Log.d("MovieRepository", "Starting Firebase Sync - Fetching Favorites (ForcedUID: $forcedUid)...")
        emit("Scarico preferiti...", null)
        val remoteFavorites = firebaseRemoteDataSource.fetchAllFavorites(forcedUid)
        android.util.Log.d("MovieRepository", "Fetched ${remoteFavorites.size} favorites from Firebase")
        if (remoteFavorites.isNotEmpty()) {
            emit("Salvo preferiti...", 0.35f)
            val moviesToInsert = remoteFavorites.map { it.copy(syncStatus = "synced") }
            val chunks = moviesToInsert.chunked(50)
            var inserted = 0
            chunks.forEachIndexed { index, chunk ->
                favoriteDao.insertAll(chunk)
                inserted += chunk.size
                val portion = (index + 1).toFloat() / chunks.size.toFloat()
                val progress = 0.35f + (0.55f - 0.35f) * portion
                emit("Salvo preferiti... ($inserted/${moviesToInsert.size})", progress)
            }
            android.util.Log.d("MovieRepository", "Successfully inserted remote favorites into local DB")
        } else {
            emit("Preferiti aggiornati", 0.55f)
        }

        // 2. Pull Folders
        emit("Scarico cartelle...", null)
        val remoteFolders = firebaseRemoteDataSource.fetchAllFolders(forcedUid)
        if (remoteFolders.isNotEmpty()) {
            emit("Salvo cartelle...", 0.75f)
            val foldersToInsert = remoteFolders.map { f ->
                FolderEntity(
                    id = f.id,
                    name = f.name,
                    icon = f.icon,
                    color = f.color,
                    description = f.description,
                    itemIds = f.itemIds,
                    createdAt = f.createdAt ?: "",
                    updatedAt = f.updatedAt ?: "",
                    syncStatus = "synced"
                )
            }
            val chunks = foldersToInsert.chunked(50)
            var inserted = 0
            chunks.forEachIndexed { index, chunk ->
                folderDao.insertAll(chunk)
                inserted += chunk.size
                val portion = (index + 1).toFloat() / chunks.size.toFloat()
                val progress = 0.75f + (0.9f - 0.75f) * portion
                emit("Salvo cartelle... ($inserted/${foldersToInsert.size})", progress)
            }
        } else {
            emit("Cartelle aggiornate", 0.9f)
        }

        // 3. Pull Preferences
        emit("Sincronizzo preferenze...", 0.92f)
        syncPreferencesWithFirebase(forcedUid)
        emit("Sync completata", 1f)
    }

    suspend fun clearAllData() {
        favoriteDao.clearAll()
        folderDao.clearAll()
    }

    suspend fun syncPreferencesWithFirebase(forcedUid: String? = null) {
        val remotePrefs = firebaseRemoteDataSource.fetchUserPreferences(forcedUid) ?: return
        
        try {
            val currentPrefs = preferenceRepository.userPreferencesFlow.first()
            
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
                homeSort = parseSortConfig(remotePrefs["homeSort"], currentPrefs.homeSort),
                vistiSort = parseSortConfig(remotePrefs["vistiSort"], currentPrefs.vistiSort),
                discoveryFilters = parseDiscoveryFilters(remotePrefs["discoveryFilters"], currentPrefs.discoveryFilters),
                lastSyncTimestamp = System.currentTimeMillis()
            )
            
            preferenceRepository.updateAll(updatedPrefs)
        } catch (e: Exception) {
            android.util.Log.e("MovieRepository", "Error syncing preferences: ${e.message}")
        }
    }

    suspend fun savePreferencesRemote(prefs: com.cinetrack.data.models.UserPreferences) {
        repositoryScope.launch {
            // Convert to Map for Firestore
            val prefsMap = mapOf(
                "gridColumns" to prefs.gridColumns,
                "notificationsEnabled" to prefs.notificationsEnabled,
                "showFolderBookmarks" to prefs.showFolderBookmarks,
                "homeSort" to prefs.homeSort,
                "vistiSort" to prefs.vistiSort,
                "discoveryFilters" to prefs.discoveryFilters
            )
            firebaseRemoteDataSource.setUserPreferences(prefsMap)
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
        return if (isTv) tmdbService.getTVDetails(id, apiKey) else tmdbService.getMovieDetails(id, apiKey)
    }

    suspend fun searchMovies(query: String, page: Int = 1): List<Movie> = tmdbService.searchMovie(query, apiKey, page = page).results
    suspend fun searchTV(query: String, page: Int = 1): List<Movie> = tmdbService.searchTV(query, apiKey, page = page).results
    suspend fun searchMulti(query: String, page: Int = 1): List<TMDBSearchResult> = tmdbService.searchMulti(query, apiKey, page = page).results
    suspend fun getPersonDetails(id: Long): Person = tmdbService.getPersonDetails(id, apiKey)
    suspend fun fetchSeasonDetails(id: Long, seasonNumber: Int): Season = tmdbService.getSeasonDetails(id, seasonNumber, apiKey)
    suspend fun fetchCollectionDetails(id: Long): com.cinetrack.data.api.CollectionResponse = tmdbService.getCollectionDetails(id, apiKey)
    suspend fun searchPeople(query: String, page: Int = 1): List<PersonSearchResult> = tmdbService.searchPeople(query, apiKey, page = page).results
    suspend fun getMoviesByGenre(genreId: Long, page: Int = 1): List<Movie> = tmdbService.getMoviesByGenre(genreId, apiKey, page = page).results
    suspend fun getTVShowsByGenre(genreId: Long, page: Int = 1): List<Movie> = tmdbService.getTVShowsByGenre(genreId, apiKey, page = page).results
    suspend fun getPopularMovies(page: Int = 1): List<Movie> = tmdbService.getPopularMovies(apiKey, page = page).results

    suspend fun getNowPlayingMovies(page: Int = 1): List<Movie> = tmdbService.getNowPlayingMovies(apiKey, page = page, region = "IT").results

    suspend fun getUpcomingMovies(page: Int = 1): List<Movie> {
        val today = java.time.LocalDate.now()
        return tmdbService.discoverMovies(
            apiKey = apiKey,
            page = page,
            options = mapOf(
                "primary_release_date.gte" to today.toString(),
                "with_release_type" to "2|3",
                "region" to "IT",
                "sort_by" to "popularity.desc"
            )
        ).results
    }

    suspend fun getPopularTV(page: Int = 1): List<Movie> = tmdbService.getPopularTV(apiKey, page = page).results
    suspend fun getAiringTodayTV(page: Int = 1): List<Movie> = tmdbService.getAiringTodayTV(apiKey, page = page).results

    suspend fun getOnTheAirTV(page: Int = 1): List<Movie> {
        val today = java.time.LocalDate.now()
        return tmdbService.discoverTV(
            apiKey = apiKey,
            page = page,
            options = mapOf(
                "first_air_date.gte" to today.toString(),
                "watch_region" to "IT",
                "sort_by" to "popularity.desc"
            )
        ).results
    }

    suspend fun getTrendingAll(page: Int = 1): List<Movie> = tmdbService.getTrendingAll(apiKey, page = page).results
    suspend fun getTrendingMovies(page: Int = 1): List<Movie> = tmdbService.getTrendingMovies(apiKey, page = page).results
    suspend fun getTrendingTV(page: Int = 1): List<Movie> = tmdbService.getTrendingTV(apiKey, page = page).results
    suspend fun getTrendingPeople(page: Int = 1): List<PersonSearchResult> = tmdbService.getTrendingPeople(apiKey, page = page).results
    suspend fun getPopularPeople(page: Int = 1): List<PersonSearchResult> = tmdbService.getPopularPeople(apiKey, page = page).results

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
}

data class TraktRatingInfo(val rating: Double?, val votes: Int)
