package com.cinetrack.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.api.MovieDetailResponse
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.domain.CycleMovieStatusUseCase
import com.cinetrack.ui.components.detail.*
import com.cinetrack.ui.theme.FlickTrove_KotlinTheme
import com.cinetrack.data.api.CrewMember
import com.cinetrack.data.api.Video
import com.cinetrack.domain.UpdateEpisodesUseCase
import com.cinetrack.ui.viewmodel.WatchState
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.ErrorMapper
import com.cinetrack.utils.TranslationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.cinetrack.ui.utils.ColorUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import com.cinetrack.data.local.entities.FolderEntity
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import java.util.UUID
import java.time.Instant
import com.cinetrack.ui.navigation.DetailRoute

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val repository: MovieRepository,
    private val updateEpisodesUseCase: UpdateEpisodesUseCase,
    private val actionFeedbackManager: ActionFeedbackManager,
    private val translationManager: TranslationManager,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var movieId: Long = 0L
        private set
    var mediaType: String = "movie"
        private set

    fun initMovie(id: Long, type: String) {
        if (movieId == 0L && id != 0L) {
            movieId = id
            mediaType = type
            // Trigger fetch
            viewModelScope.launch {
                fetchFromTMDB(movieId, mediaType == "tv")
            }
        }
    }

    private val _metadata = MutableStateFlow<MovieDetailResponse?>(null)
    private val _externalRatings = MutableStateFlow(ExternalRatings())
    private val _loadingSeason = MutableStateFlow(false)
    private val _seasonDetails = MutableStateFlow<Map<Int, com.cinetrack.data.models.Season>>(emptyMap())
    private val _collectionMovies = MutableStateFlow<List<Movie>>(emptyList())
    private val _traktComments = MutableStateFlow<List<com.cinetrack.data.api.TraktComment>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)
    
    data class TranslationState(
        val isTranslating: Boolean = false,
        val translatedText: String? = null,
        val error: String? = null
    )
    private val _translationStates = MutableStateFlow<Map<Long, TranslationState>>(emptyMap())
    val translationStates: StateFlow<Map<Long, TranslationState>> = _translationStates.asStateFlow()

    private val _showTranslationPrompt = MutableStateFlow<Pair<Long, String>?>(null)
    val showTranslationPrompt: StateFlow<Pair<Long, String>?> = _showTranslationPrompt.asStateFlow()

    private val _extractedColor = MutableStateFlow<Color?>(null)
    val extractedColor: StateFlow<Color?> = _extractedColor.asStateFlow()

    fun emitMessage(message: String) {
        actionFeedbackManager.emit(message)
    }

    val uiState: StateFlow<DetailUiState> = buildUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DetailUiState.Loading
        )

    private fun buildUiState(): Flow<DetailUiState> {
        val metadataFlow = combine(
            combine(
                _metadata,
                _externalRatings,
                _loadingSeason
            ) { metadata, external, loadingS ->
                Triple(metadata, external, loadingS)
            },
            combine(
                _seasonDetails,
                _collectionMovies,
                _error
            ) { seasonD, collectionM, errorMsg ->
                Triple(seasonD, collectionM, errorMsg)
            },
            _traktComments
        ) { groupA, groupB, traktComms ->
            val (metadata, external, loadingS) = groupA
            val (seasonD, collectionM, errorMsg) = groupB

            MetadataState(metadata, external, loadingS, seasonD, collectionM, errorMsg, traktComms)
        }

        return combine(
            metadataFlow,
            repository.getLocalMoviesFlow(),
            repository.getFoldersFlow()
        ) { meta, localMovies, folders ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val movie = localMovies.find { it.id == movieId && it.mediaType == mediaType }
                val metadata = meta.metadata
                val external = meta.external
                val loadingS = meta.loadingS
                val seasonD = meta.seasonD
                val collectionM = meta.collectionM
                val errorMsg = meta.errorMsg
                val traktComms = meta.traktComments

                when {
                    errorMsg != null && metadata == null -> DetailUiState.Error(errorMsg)
                    metadata == null -> DetailUiState.Loading
                    else -> {
                        // Update the movie entry with fresh metadata fields
                        val freshMovie = mapResponseToMovie(metadata, mediaType)
                        val effectiveMovie = movie?.copy(
                            genres = freshMovie.genres,
                            runtime = freshMovie.runtime,
                            tagline = freshMovie.tagline,
                            overview = freshMovie.overview,
                            title = freshMovie.title,
                            name = freshMovie.name,
                            posterPath = freshMovie.posterPath,
                            backdropPath = freshMovie.backdropPath,
                            voteAverage = freshMovie.voteAverage,
                            voteCount = freshMovie.voteCount,
                            numberOfSeasons = freshMovie.numberOfSeasons,
                            numberOfEpisodes = freshMovie.numberOfEpisodes,
                            revenue = freshMovie.revenue,
                            budget = freshMovie.budget,
                            seasons = freshMovie.seasons,
                            releaseDate = freshMovie.releaseDate,
                            firstAirDate = freshMovie.firstAirDate,
                            lastAirDate = freshMovie.lastAirDate,
                            nextEpisodeAirDate = freshMovie.nextEpisodeAirDate,
                            nextEpisodeString = freshMovie.nextEpisodeString,
                            releaseYear = freshMovie.releaseYear,
                            status = freshMovie.status,
                            imdbId = freshMovie.imdbId
                        ) ?: freshMovie

                        val totalEpisodes = metadata.numberOfEpisodes ?: 0
                        val watchedEpisodesCount = effectiveMovie.watchedEpisodes?.values?.sumOf { it.size } ?: 0
                        val progress = if (mediaType == "tv" && totalEpisodes > 0) watchedEpisodesCount.toFloat() / totalEpisodes else 0f
                    
                        // Pre-calculate map for O(1) lookups
                        val localMoviesMap = localMovies.associateBy { "${it.mediaType}_${it.id}" }

                        // Helper to hydrate movie with local DB status
                        fun Movie.hydrate(): Movie {
                            val local = localMoviesMap["${this.mediaType}_${this.id}"]
                            return if (local != null) {
                                this.copy(
                                    favorite = local.favorite,
                                    watched = local.watched,
                                    reminder = local.reminder,
                                    progress = local.progress,
                                    watchedEpisodes = local.watchedEpisodes,
                                    watchedAt = local.watchedAt,
                                    personalRating = local.personalRating,
                                    personalNote = local.personalNote
                                )
                            } else this
                        }

                        val finalMovie = effectiveMovie

                        DetailUiState.Success(
                            movieEntry = finalMovie,
                            details = metadata,
                            isFavorite = finalMovie.favorite,
                            isWatched = finalMovie.watched,
                            watchState = when {
                                finalMovie.watched -> WatchState.WATCHED
                                finalMovie.favorite || finalMovie.reminder -> WatchState.BOOKMARKED
                                else -> WatchState.NONE
                            },
                            watchedProgress = progress,
                            directors = (metadata.credits?.crew?.filter { c: CrewMember -> c.job == "Director" } ?: emptyList()).toImmutableList(),
                            cast = (metadata.credits?.cast?.take(15)?.distinctBy { it.id } ?: emptyList()).toImmutableList(),
                            streamingProviders = (metadata.watchProviders?.results?.get("IT")?.flatrate?.distinctBy { it.providerId } ?: emptyList()).toImmutableList(),
                            buyRentProviders = ((metadata.watchProviders?.results?.get("IT")?.buy ?: emptyList()) + 
                                               (metadata.watchProviders?.results?.get("IT")?.rent ?: emptyList())).distinctBy { it.providerId }.toImmutableList(),
                            trailers = (metadata.videos?.results?.let { videos -> 
                                videos.filter { v -> v.site == "YouTube" && v.type == "Trailer" }.map { v -> v.key }.distinct()
                            } ?: emptyList()).toImmutableList(),
                            recommendations = (metadata.recommendations?.results?.map { 
                                it.hydrate() 
                            }?.distinctBy { it.id } ?: emptyList()).toImmutableList(),
                            collectionMovies = collectionM.map { it.hydrate() }.toImmutableList(),
                            externalRatings = external,
                            loadingSeason = loadingS,
                            seasonDetails = seasonD.toImmutableMap(),
                            folders = folders.sortedByDescending { it.createdAt }.toImmutableList(),
                            watchProviderLink = metadata.watchProviders?.results?.get("IT")?.link,
                            traktComments = traktComms.toImmutableList()
                        )
                    }
                }
            }
        }
    }

    private suspend fun fetchFromTMDB(id: Long, isTv: Boolean) {
        val startTime = System.currentTimeMillis()
        try {
            val response = repository.fetchMovieDetails(id, isTv)
            
            // Trigger extra ratings fetch if we have IMDB ID
            _externalRatings.update { it.copy(certification = extractCertification(response, isTv)) }
            val imdbId = response.externalIds?.imdbId
            fetchExternalRatings(imdbId, id)

            // Update local DB if movie already exists (to sync missing release dates etc.)
            viewModelScope.launch {
                val localMovie = repository.getMovie(id, if (isTv) "tv" else "movie")
                if (localMovie != null) {
                    val freshMovie = mapResponseToMovie(response, if (isTv) "tv" else "movie")
                    val updatedMovie = localMovie.copy(
                        genres = freshMovie.genres ?: localMovie.genres,
                        runtime = freshMovie.runtime ?: localMovie.runtime,
                        tagline = freshMovie.tagline ?: localMovie.tagline,
                        overview = freshMovie.overview ?: localMovie.overview,
                        title = freshMovie.title ?: localMovie.title,
                        name = freshMovie.name ?: localMovie.name,
                        posterPath = freshMovie.posterPath ?: localMovie.posterPath,
                        backdropPath = freshMovie.backdropPath ?: localMovie.backdropPath,
                        voteAverage = freshMovie.voteAverage ?: localMovie.voteAverage,
                        voteCount = freshMovie.voteCount ?: localMovie.voteCount,
                        numberOfSeasons = freshMovie.numberOfSeasons ?: localMovie.numberOfSeasons,
                        numberOfEpisodes = freshMovie.numberOfEpisodes ?: localMovie.numberOfEpisodes,
                        revenue = freshMovie.revenue ?: localMovie.revenue,
                        budget = freshMovie.budget ?: localMovie.budget,
                        seasons = freshMovie.seasons ?: localMovie.seasons,
                        releaseDate = freshMovie.releaseDate ?: localMovie.releaseDate,
                        firstAirDate = freshMovie.firstAirDate ?: localMovie.firstAirDate,
                        lastAirDate = freshMovie.lastAirDate ?: localMovie.lastAirDate,
                        nextEpisodeAirDate = freshMovie.nextEpisodeAirDate ?: localMovie.nextEpisodeAirDate,
                        nextEpisodeString = freshMovie.nextEpisodeString ?: localMovie.nextEpisodeString,
                        releaseYear = freshMovie.releaseYear ?: localMovie.releaseYear,
                        status = freshMovie.status ?: localMovie.status,
                        imdbId = freshMovie.imdbId ?: localMovie.imdbId
                    )
                    repository.saveMovie(updatedMovie)
                }
            }

            // Fetch Collection if present
            response.belongsToCollection?.id?.let { collectionId ->
                viewModelScope.launch {
                    try {
                        val collectionResponse = repository.fetchCollectionDetails(collectionId)
                        _collectionMovies.value = collectionResponse.parts
                            .map { it.copy(mediaType = "movie") }
                            .sortedBy { it.releaseDate ?: "9999-12-31" }
                    } catch (e: Exception) {
                        // Silent fail for collection
                    }
                }
            }

            _metadata.value = response
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _error.value = ErrorMapper.map(e.message)
        } finally {
            val timeTaken = System.currentTimeMillis() - startTime
            if (timeTaken < 600L) {
                kotlinx.coroutines.delay(600L - timeTaken)
            }
        }
    }

    private fun fetchExternalRatings(imdbId: String?, tmdbId: Long) {
        viewModelScope.launch {
            kotlinx.coroutines.supervisorScope {
                val omdbDeferred = imdbId?.let { async { repository.fetchOmdbRatings(it) } }
                val traktId = imdbId ?: tmdbId.toString()
                val traktDeferred = async { repository.fetchTraktRating(traktId, mediaType == "tv") }
                val commentsDeferred = async { repository.fetchComments(traktId, mediaType == "tv") }

                val omdbResult = try { omdbDeferred?.await() } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; null }
                val traktResult = try { traktDeferred.await() } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; null }
                val commentsResult = try { commentsDeferred.await() } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emptyList() }

                _traktComments.value = commentsResult ?: emptyList()

                _externalRatings.update {  
                    it.copy(
                        imdb = omdbResult?.imdbRating,
                        imdbVotes = omdbResult?.imdbVotes,
                        rottenTomatoes = omdbResult?.rottenTomatoes,
                        metacritic = omdbResult?.metacritic,
                        trakt = traktResult?.rating,
                        traktVotes = traktResult?.votes?.let { v -> 
                            java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(v)
                        },
                        awards = omdbResult?.awards
                    )
                }
            }
        }
    }
    private fun mapResponseToMovie(response: MovieDetailResponse, type: String): Movie {
        val effectiveRuntime = if (type == "tv") {
            response.episodeRunTime?.firstOrNull() ?: response.runtime
        } else {
            response.runtime
        }
        
        return Movie(
            id = response.id,
            mediaType = type,
            imdbId = response.externalIds?.imdbId,
            title = response.title,
            name = response.name,
            posterPath = response.posterPath,
            backdropPath = response.backdropPath,
            voteAverage = response.voteAverage,
            voteCount = response.voteCount,
            overview = response.overview,
            releaseDate = response.releaseDate,
            firstAirDate = response.firstAirDate,
            runtime = effectiveRuntime,
            revenue = response.revenue,
            budget = response.budget,
            tagline = response.tagline,
            genres = response.genres,
            genreIds = response.genres?.map { it.id },
            numberOfSeasons = response.numberOfSeasons,
            numberOfEpisodes = response.numberOfEpisodes,
            streamingProviderIds = response.watchProviders?.results?.get("IT")?.flatrate?.map { it.providerId },
            seasons = response.seasons,
            nextEpisodeAirDate = response.nextEpisodeToAir?.airDate,
            nextEpisodeString = response.nextEpisodeToAir?.let { "S${it.seasonNumber.toString().padStart(2, '0')}E${it.episodeNumber.toString().padStart(2, '0')}" }
        )
    }


    fun onEvent(event: DetailEvent) {
        when (event) {
            DetailEvent.ToggleFavorite -> toggleFavorite()
            DetailEvent.ToggleWatched -> toggleWatched()
            is DetailEvent.SetWatchState -> setWatchState(event.state)
            DetailEvent.Refresh -> viewModelScope.launch { fetchFromTMDB(movieId, mediaType == "tv") }
            is DetailEvent.Rate -> updateRating(event.rating)
            is DetailEvent.UpdateNote -> updateNote(event.note)
            is DetailEvent.RateMovie -> updateMovieRating(event.movie, event.rating)
            is DetailEvent.UpdateMovieNote -> updateMovieNote(event.movie, event.note)
            is DetailEvent.DeleteMovieItem -> deleteMovieItem(event.movie)
            is DetailEvent.ToggleMovieFolderMembership -> toggleMovieFolderMembership(event.movie, event.folder)
            is DetailEvent.LoadSeasonDetails -> loadSeasonDetails(event.seasonNumber)
            is DetailEvent.ToggleEpisode -> toggleEpisode(event.seasonNumber, event.episodeNumber)
            is DetailEvent.ToggleSeason -> toggleSeason(event.seasonNumber, event.episodes)
            is DetailEvent.CycleStatus -> cycleStatus(event.movie)
            is DetailEvent.SyncWatchedEpisodes -> syncWatchedEpisodes(event.episodes)
            DetailEvent.DeleteMovie -> deleteMovie()
            is DetailEvent.ToggleFolderMembership -> toggleFolderMembership(event.folder)
            is DetailEvent.CreateFolder -> createFolder(event.name, event.color)
        }
    }

    private fun updateMovieRating(movie: Movie, rating: Double?) {
        viewModelScope.launch {
            val effectiveRating = if (rating == 0.0) null else rating
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            val updated = current.copy(personalRating = effectiveRating)
            repository.saveMovie(updated)
        }
    }

    private fun updateMovieNote(movie: Movie, note: String) {
        viewModelScope.launch {
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            val updated = current.copy(personalNote = note)
            repository.saveMovie(updated)
        }
    }

    private fun deleteMovieItem(movie: Movie) {
        val title = movie.displayName
        viewModelScope.launch {
            repository.deleteMovie(movie)
            actionFeedbackManager.emit("\"$title\" eliminato") {
                repository.saveMovie(movie)
            }
        }
    }

    private fun toggleMovieFolderMembership(movie: Movie, folder: FolderEntity) {
        val itemId = "${movie.mediaType}_${movie.id}"
        val newItemIds = if (folder.itemIds.contains(itemId)) {
            folder.itemIds - itemId
        } else {
            folder.itemIds + itemId
        }
        viewModelScope.launch {
            repository.saveFolder(folder.copy(itemIds = newItemIds, updatedAt = Instant.now().toString()))
        }
    }

    private fun toggleFolderMembership(folder: FolderEntity) {
        val itemId = "${mediaType}_${movieId}"
        val newItemIds = if (folder.itemIds.contains(itemId)) {
            folder.itemIds - itemId
        } else {
            folder.itemIds + itemId
        }
        viewModelScope.launch {
            repository.saveFolder(folder.copy(itemIds = newItemIds, updatedAt = Instant.now().toString()))
        }
    }

    private fun createFolder(name: String, color: String) {
        val itemId = "${mediaType}_${movieId}"
        val newFolder = FolderEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            icon = "folder",
            color = color,
            itemIds = listOf(itemId),
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )
        viewModelScope.launch {
            repository.saveFolder(newFolder)
            actionFeedbackManager.emit("Cartella \"$name\" creata con questo elemento")
        }
    }

    private fun syncWatchedEpisodes(episodes: Map<String, List<Int>>) {
        val state = uiState.value as? DetailUiState.Success ?: return
        val movie = state.movieEntry
        val castedMap = episodes.mapKeys { it.key.toIntOrNull() ?: 0 }
        
        viewModelScope.launch {
            val updated = updateEpisodesUseCase.batchUpdate(movie, castedMap)
            repository.saveMovie(updated)
        }
    }

    private fun updateRating(rating: Double?) {
        val state = uiState.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            // If rating is 0.0 or null, set to null (remove rating)
            val effectiveRating = if (rating == 0.0) null else rating
            val updated = state.movieEntry.copy(personalRating = effectiveRating)
            repository.saveMovie(updated)
        }
    }

    private fun updateNote(note: String) {
        val state = uiState.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            val updated = state.movieEntry.copy(personalNote = note)
            repository.saveMovie(updated)
        }
    }

    private fun deleteMovie() {
        val state = uiState.value as? DetailUiState.Success ?: return
        val movie = state.movieEntry
        val title = movie.displayName
        viewModelScope.launch {
            repository.deleteMovie(movie)
            actionFeedbackManager.emit("\"$title\" eliminato") {
                repository.saveMovie(movie)
            }
        }
    }

    private fun toggleFavorite() {
        val state = uiState.value as? DetailUiState.Success ?: return
        val title = state.movieEntry.displayName
        val previousState = state.movieEntry.copy()

        // IDEMPOTENCY CHECK: If already watched, do nothing
        if (state.movieEntry.watched) {
            return
        }

        viewModelScope.launch {
            cycleMovieStatusUseCase(state.movieEntry)
            val updated = repository.getMovie(state.movieEntry.id, state.movieEntry.mediaType)
            val actionLabel = when {
                updated == null -> "rimosso"
                updated.watched -> "segnato come visto"
                updated.favorite -> "aggiunto a Da Vedere"
                updated.reminder -> "promemoria impostato"
                else -> "aggiornato"
            }
            actionFeedbackManager.emit("\"$title\" $actionLabel") {
                repository.saveMovie(previousState)
            }
        }
    }

    private fun toggleWatched() {
        val state = uiState.value as? DetailUiState.Success ?: return
        val title = state.movieEntry.displayName
        val previousState = state.movieEntry.copy()

        // IDEMPOTENCY CHECK: If already watched, do nothing
        if (state.movieEntry.watched) {
            return
        }

        viewModelScope.launch {
            cycleMovieStatusUseCase(state.movieEntry)
            val updated = repository.getMovie(state.movieEntry.id, state.movieEntry.mediaType)
            val actionLabel = when {
                updated == null -> "rimosso"
                updated.watched -> "segnato come visto"
                updated.favorite -> "aggiunto a Da Vedere"
                updated.reminder -> "promemoria impostato"
                else -> "aggiornato"
            }
            actionFeedbackManager.emit("\"$title\" $actionLabel") {
                repository.saveMovie(previousState)
            }
        }
    }

    private fun cycleStatus(movie: Movie) {
        val title = movie.title ?: movie.name ?: ""
        val previousState = movie.copy()
        viewModelScope.launch {
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            
            // IDEMPOTENCY CHECK: If already watched, do nothing
            if (current.watched) {
                return@launch
            }

            cycleMovieStatusUseCase(current)
            val updated = repository.getMovie(movie.id, movie.mediaType)
            val actionLabel = when {
                updated == null -> "rimosso"
                updated.watched -> "segnato come visto"
                updated.favorite -> "aggiunto a Da Vedere"
                updated.reminder -> "promemoria impostato"
                else -> "aggiornato"
            }
            actionFeedbackManager.emit("\"$title\" $actionLabel") {
                repository.saveMovie(previousState)
            }
        }
    }

    private fun setWatchState(watchState: WatchState) {
        val state = uiState.value as? DetailUiState.Success ?: return
        val previousMovie = state.movieEntry
        val title = previousMovie.title ?: previousMovie.name ?: ""
        viewModelScope.launch {
            val updated = when (watchState) {
                WatchState.NONE -> previousMovie.copy(favorite = false, watched = false, reminder = false, watchedAt = null)
                WatchState.BOOKMARKED -> {
                    if (previousMovie.isReleased) {
                        previousMovie.copy(favorite = true, watched = false, reminder = false, watchedAt = null)
                    } else {
                        previousMovie.copy(favorite = false, watched = false, reminder = true, watchedAt = null)
                    }
                }
                WatchState.WATCHED -> {
                    if (mediaType == "tv") {
                        updateEpisodesUseCase.markAllWatched(previousMovie)
                    } else {
                        previousMovie.copy(favorite = false, watched = true, reminder = false, watchedAt = java.time.Instant.now().toString())
                    }
                }
            }
            repository.saveMovie(updated)

            val message = when (watchState) {
                WatchState.NONE -> "\"$title\" rimosso dalla lista"
                WatchState.BOOKMARKED -> "\"$title\" aggiunto a Da Vedere"
                WatchState.WATCHED -> "\"$title\" segnato come Visto"
            }
            actionFeedbackManager.emit(message) {
                repository.saveMovie(previousMovie)
            }
        }
    }

    fun loadSeasonDetails(seasonNumber: Int) {
        val id = movieId
        val state = uiState.value as? DetailUiState.Success ?: return
        if (state.seasonDetails.containsKey(seasonNumber)) return

        viewModelScope.launch {
            _loadingSeason.value = true
            try {
                val season = repository.fetchSeasonDetails(id, seasonNumber)
                _seasonDetails.update { it + (seasonNumber to season) }
                _loadingSeason.value = false
            } catch (e: Exception) {
                _loadingSeason.value = false
            }
        }
    }

    fun toggleEpisode(seasonNumber: Int, episodeNumber: Int) {
        val state = uiState.value as? DetailUiState.Success ?: return
        val movie = state.movieEntry
        val currentWatched = movie.watchedEpisodes?.get(seasonNumber.toString())?.toMutableList() ?: mutableListOf()
        
        if (currentWatched.contains(episodeNumber)) {
            currentWatched.remove(episodeNumber)
        } else {
            currentWatched.add(episodeNumber)
        }

        viewModelScope.launch {
            val updated = updateEpisodesUseCase(movie, seasonNumber, currentWatched)
            repository.saveMovie(updated)
        }
    }

    fun toggleSeason(seasonNumber: Int, allEpisodeNumbers: List<Int>) {
        val state = uiState.value as? DetailUiState.Success ?: return
        val movie = state.movieEntry
        val currentWatched = movie.watchedEpisodes?.get(seasonNumber.toString()) ?: emptyList()
        
        val newEpisodes = if (currentWatched.size >= allEpisodeNumbers.size) {
            emptyList<Int>()
        } else {
            allEpisodeNumbers
        }

        viewModelScope.launch {
            val updated = updateEpisodesUseCase(movie, seasonNumber, newEpisodes)
            repository.saveMovie(updated)
        }
    }

    private fun extractCertification(details: MovieDetailResponse, isTv: Boolean): String? {
        return if (isTv) {
            details.contentRatings?.results?.find { it.iso31661 == "US" }?.rating
                ?: details.contentRatings?.results?.firstOrNull()?.rating
        } else {
            details.releaseDates?.results?.find { it.iso31661 == "US" }?.releaseDates?.firstOrNull { it.certification.isNotEmpty() }?.certification
                ?: details.releaseDates?.results?.firstOrNull()?.releaseDates?.firstOrNull { it.certification.isNotEmpty() }?.certification
        }
    }

    fun requestTranslation(commentId: Long, text: String) {
        viewModelScope.launch {
            if (translationManager.isModelDownloaded()) {
                // If model is already downloaded, translate immediately without prompt
                translateComment(commentId, text, requireWifi = false)
            } else {
                // Ask user for permission to download
                _showTranslationPrompt.value = Pair(commentId, text)
            }
        }
    }

    fun dismissTranslationPrompt() {
        _showTranslationPrompt.value = null
    }

    fun translateComment(commentId: Long, text: String, requireWifi: Boolean) {
        viewModelScope.launch {
            _showTranslationPrompt.value = null
            _translationStates.update { it + (commentId to TranslationState(isTranslating = true)) }
            val success = translationManager.downloadModel(requireWifi)
            if (success) {
                val translated = translationManager.translate(text)
                if (translated != null) {
                    _translationStates.update { it + (commentId to TranslationState(isTranslating = false, translatedText = translated)) }
                } else {
                    _translationStates.update { it + (commentId to TranslationState(isTranslating = false, error = "Errore durante la traduzione")) }
                }
            } else {
                _translationStates.update { it + (commentId to TranslationState(isTranslating = false, error = "Errore nel download del modello lingua")) }
                emitMessage("Errore nel download del modello lingua italiana.")
            }
        }
    }

    fun fetchAccentColor(imageUrl: String, movie: Movie) {
        if (movie.accentColor == null && (movie.posterPath != null || movie.backdropPath != null)) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                val loader = Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable && drawable.bitmap.width > 0 && drawable.bitmap.height > 0) {
                        val bitmap = drawable.bitmap
                        val averageColor = ColorUtils.extractAverageColor(bitmap)
                        val brightColor = ColorUtils.ensureMinimumLuminance(averageColor, 0.5f)
                        val finalColor = ColorUtils.saturateColor(brightColor, 2.0f)
                        _extractedColor.value = finalColor
                    }
                }
            }
        }
    }
}

private data class MetadataState(
    val metadata: MovieDetailResponse?,
    val external: ExternalRatings,
    val loadingS: Boolean,
    val seasonD: Map<Int, com.cinetrack.data.models.Season>,
    val collectionM: List<Movie>,
    val errorMsg: String?,
    val traktComments: List<com.cinetrack.data.api.TraktComment>
)
