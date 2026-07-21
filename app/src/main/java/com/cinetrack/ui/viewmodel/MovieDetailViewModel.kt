package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.Movie
import com.cinetrack.data.api.MovieDetailResponse
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.domain.CycleMovieStatusUseCase
import com.cinetrack.ui.components.detail.*
import com.cinetrack.ui.theme.FlickTroveTheme
import com.cinetrack.data.api.CrewMember
import com.cinetrack.data.api.Video
import com.cinetrack.domain.UpdateEpisodesUseCase
import com.cinetrack.ui.viewmodel.WatchState
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.ErrorMapper
import com.cinetrack.util.TranslationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import com.cinetrack.util.toComposeColorOrNull
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
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
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val repository: MovieRepository,
    private val updateEpisodesUseCase: UpdateEpisodesUseCase,
    private val actionFeedbackManager: ActionFeedbackManager,
    private val translationManager: TranslationManager,
    private val detailUiStateMapper: DetailUiStateMapper,
    private val preferenceRepository: com.cinetrack.data.repository.PreferenceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var movieId: Long = 0L
        private set
    var mediaType: String = "movie"
        private set

    fun initMovie(id: Long, type: String) {
        if (movieId == 0L && id != 0L) {
            movieId = id
            mediaType = type
            // Load cached accent color instantly if available
            viewModelScope.launch {
                val cachedHex = repository.getCachedColor("$type:$id")
                if (cachedHex != null && _extractedColor.value == null) {
                    _extractedColor.value = cachedHex.toComposeColorOrNull()
                }
            }
            // Trigger fetch
            viewModelScope.launch {
                fetchFromTMDB(movieId, mediaType == "tv")
            }
        }
    }

    private val _metadata = MutableStateFlow<MovieDetailResponse?>(null)
    private val _externalRatings = MutableStateFlow(ExternalRatings())
    private val _loadingSeason = MutableStateFlow(false)
    private val _seasonDetails = MutableStateFlow<Map<Int, com.cinetrack.data.model.Season>>(emptyMap())
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

    fun emitMessage(message: UiText) {
        actionFeedbackManager.emit(message)
    }

    val useMovieLogo: StateFlow<Boolean> = preferenceRepository.userPreferencesFlow
        .map { it.useMovieLogo }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val uiState: StateFlow<DetailUiState> = buildUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DetailUiState.Loading
        )

    private fun buildUiState(): Flow<DetailUiState> {
        val flow1 = combine(
            _metadata,
            _externalRatings,
            _loadingSeason
        ) { metadata, external, loadingS ->
            Triple(metadata, external, loadingS)
        }

        val flow2 = combine(
            _seasonDetails,
            _collectionMovies,
            _error
        ) { seasonD, collectionM, errorMsg ->
            Triple(seasonD, collectionM, errorMsg)
        }

        val metadataFlow = combine(
            flow1,
            flow2,
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
                detailUiStateMapper.mapToState(
                    movieId = movieId,
                    mediaType = mediaType,
                    metadata = meta.metadata,
                    externalRatings = meta.external,
                    loadingSeason = meta.loadingS,
                    seasonDetails = meta.seasonD,
                    collectionMovies = meta.collectionM,
                    errorMsg = meta.errorMsg,
                    traktComments = meta.traktComments,
                    localMovies = localMovies,
                    folders = folders
                )
            }
        }
    }

    private suspend fun fetchFromTMDB(id: Long, isTv: Boolean) {
        val startTime = System.currentTimeMillis()
        try {
            repository.getMovieDetailsFlow(id, isTv).collect { response ->
                // Preload image & accent color while keeping skeleton displayed on first opening
                val targetPath = response.backdropPath ?: response.posterPath
                if (targetPath != null && (_metadata.value == null || _extractedColor.value == null)) {
                    val imageType = if (response.backdropPath != null) ImageType.BACKDROP else ImageType.POSTER
                    val imageUrl = buildTmdbImageUrl(targetPath, imageType, ImageQuality.HIGH)
                    if (imageUrl != null) {
                        preloadAccentColor(imageUrl)
                    }
                }

                // Ensure skeleton is displayed for a minimum smooth duration on initial opening
                if (_metadata.value == null) {
                    val timeTaken = System.currentTimeMillis() - startTime
                    if (timeTaken < 400L) {
                        kotlinx.coroutines.delay(400L - timeTaken)
                    }
                }

                // Emit metadata cleanly after initial image and color are ready
                _metadata.value = response

                // Start caching and secondary background syncs AFTER initial display
                viewModelScope.launch(Dispatchers.IO) {
                    _externalRatings.update { it.copy(certification = com.cinetrack.data.mapper.MovieMapper.extractCertification(response, if (isTv) "tv" else "movie")) }
                    val imdbId = response.externalIds?.imdbId
                    fetchExternalRatings(imdbId, id)

                    // Update local DB if movie already exists (to sync missing release dates etc.)
                    val localMovie = repository.getMovie(id, if (isTv) "tv" else "movie")
                    if (localMovie != null) {
                        val freshMovie = com.cinetrack.data.mapper.MovieMapper.mapResponseToMovie(response, if (isTv) "tv" else "movie")
                        val updatedMovie = localMovie.copy(
                            genres = freshMovie.genres ?: localMovie.genres,
                            runtime = freshMovie.runtime ?: localMovie.runtime,
                            episodeRunTime = freshMovie.episodeRunTime ?: localMovie.episodeRunTime,
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
                            imdbId = freshMovie.imdbId ?: localMovie.imdbId,
                            topCastData = freshMovie.topCastData ?: localMovie.topCastData,
                            directorData = freshMovie.directorData ?: localMovie.directorData,
                            directorId = freshMovie.directorId ?: localMovie.directorId,
                            directorName = freshMovie.directorName ?: localMovie.directorName,
                            directorProfilePath = freshMovie.directorProfilePath ?: localMovie.directorProfilePath
                        )
                        repository.saveMovie(updatedMovie)
                    }

                    // Fetch Collection if present
                    response.belongsToCollection?.id?.let { collectionId ->
                        try {
                            repository.getCollectionDetailsFlow(collectionId).collect { collectionResponse ->
                                _collectionMovies.value = collectionResponse.parts
                                    .map { it.copy(mediaType = "movie") }
                                    .sortedBy { it.releaseDate ?: "9999-12-31" }
                            }
                        } catch (e: Exception) {
                            // Silent fail for collection
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val localMovie = repository.getMovie(id, if (isTv) "tv" else "movie")
            if (localMovie != null) {
                val targetPath = localMovie.customBackdropPath ?: localMovie.backdropPath ?: localMovie.posterPath
                if (targetPath != null && (_metadata.value == null || _extractedColor.value == null)) {
                    val imageType = if (localMovie.customBackdropPath != null || localMovie.backdropPath != null) ImageType.BACKDROP else ImageType.POSTER
                    val imageUrl = buildTmdbImageUrl(targetPath, imageType, ImageQuality.HIGH)
                    if (imageUrl != null) {
                        preloadAccentColor(imageUrl)
                    }
                }
                _metadata.value = com.cinetrack.data.mapper.MovieMapper.mapMovieToResponse(localMovie)
            } else {
                _error.value = e.stackTraceToString()
            }
        }
    }

    private fun fetchExternalRatings(imdbId: String?, tmdbId: Long) {
        viewModelScope.launch {
            kotlinx.coroutines.supervisorScope {
                val omdbDeferred = imdbId?.let { async { repository.fetchOmdbRatings(it) } }
                val traktId = imdbId ?: tmdbId.toString()
                val traktDeferred = async { repository.fetchTraktRating(traktId, mediaType == "tv") }
                val commentsDeferred = async { repository.fetchComments(traktId, tmdbId, mediaType == "tv") }

                val omdbResult = try { omdbDeferred?.await() } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; null }
                val traktResult = try { traktDeferred.await() } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; null }
                val commentsResult = try { commentsDeferred.await() } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emptyList() }

                _traktComments.value = commentsResult

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
            is DetailEvent.UpdateCustomCover -> updateCustomCover(event.newPath)
        }
    }

    private fun updateCustomCover(newPath: String?) {
        viewModelScope.launch {
            val local = repository.getMovie(movieId, mediaType)
                ?: (uiState.value as? DetailUiState.Success)?.movieEntry
                ?: return@launch
            val updated = local.copy(customBackdropPath = newPath)
            repository.saveMovie(updated)
            // Re-extract the dominant color from the new cover or fallback to poster/backdrop
            val targetPath = newPath ?: updated.posterPath ?: updated.backdropPath
            val imageType = if (newPath != null || updated.posterPath == null) ImageType.BACKDROP else ImageType.POSTER
            val imageUrl = buildTmdbImageUrl(targetPath, imageType, ImageQuality.HIGH)
            if (imageUrl != null) fetchAccentColor(imageUrl, updated, forceReload = true)
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
            actionFeedbackManager.emit(UiText.StringResource(R.string.msg_item_deleted, title)) {
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
            actionFeedbackManager.emit(UiText.StringResource(R.string.msg_folder_created_with_item, name))
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
            actionFeedbackManager.emit(UiText.StringResource(R.string.msg_item_deleted, title)) {
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
            val actionMsgRes = when {
                updated == null -> R.string.msg_action_removed
                updated.watched -> R.string.msg_action_watched
                updated.favorite -> R.string.msg_action_favorite
                updated.reminder -> R.string.msg_action_reminder
                else -> R.string.msg_action_updated
            }
            actionFeedbackManager.emit(UiText.StringResource(actionMsgRes, title)) {
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
            val actionMsgRes = when {
                updated == null -> R.string.msg_action_removed
                updated.watched -> R.string.msg_action_watched
                updated.favorite -> R.string.msg_action_favorite
                updated.reminder -> R.string.msg_action_reminder
                else -> R.string.msg_action_updated
            }
            actionFeedbackManager.emit(UiText.StringResource(actionMsgRes, title)) {
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
            val actionMsgRes = when {
                updated == null -> R.string.msg_action_removed
                updated.watched -> R.string.msg_action_watched
                updated.favorite -> R.string.msg_action_favorite
                updated.reminder -> R.string.msg_action_reminder
                else -> R.string.msg_action_updated
            }
            actionFeedbackManager.emit(UiText.StringResource(actionMsgRes, title)) {
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

            val actionMsgRes = when (watchState) {
                WatchState.NONE -> R.string.msg_action_removed
                WatchState.BOOKMARKED -> if (previousMovie.isReleased) R.string.msg_action_favorite else R.string.msg_action_reminder
                WatchState.WATCHED -> R.string.msg_action_watched
            }
            
            actionFeedbackManager.emit(UiText.StringResource(actionMsgRes, title)) {
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
                    emitMessage(UiText.StringResource(R.string.msg_error_translating))
                }
            } else {
                _translationStates.update { it + (commentId to TranslationState(isTranslating = false, error = "Errore nel download del modello lingua")) }
                emitMessage(UiText.StringResource(R.string.msg_error_lang_model))
            }
        }
    }

    private suspend fun preloadAccentColor(imageUrl: String) {
        kotlinx.coroutines.withTimeoutOrNull(1200L) {
            try {
                val loader = Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (_extractedColor.value == null && result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap()
                    if (bitmap.width > 0 && bitmap.height > 0) {
                        val rawColor = ColorUtils.extractAverageColor(bitmap)
                        if (rawColor != Color.Unspecified) {
                            val ambientColor = ColorUtils.darkenForAmbient(rawColor)
                            val finalColor = ColorUtils.ensureMinimumLuminance(ambientColor, 0.25f)
                            _extractedColor.value = finalColor
                            
                            val r = (finalColor.red * 255).toInt().coerceIn(0, 255)
                            val g = (finalColor.green * 255).toInt().coerceIn(0, 255)
                            val b = (finalColor.blue * 255).toInt().coerceIn(0, 255)
                            val hexString = String.format("#%02X%02X%02X", r, g, b)
                            viewModelScope.launch(Dispatchers.IO) {
                                repository.saveCachedColor("$mediaType:$movieId", hexString)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore timeout or extraction error during preload
            }
        }
    }

    fun fetchAccentColor(imageUrl: String, movie: Movie, forceReload: Boolean = false) {
        if (forceReload || movie.accentColor == null || _extractedColor.value == null) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    val loader = Coil.imageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .allowHardware(false)
                        .build()
                    
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = result.drawable.toBitmap()
                        if (bitmap.width > 0 && bitmap.height > 0) {
                            val rawColor = ColorUtils.extractAverageColor(bitmap)
                            if (rawColor != Color.Unspecified) {
                                val ambientColor = ColorUtils.darkenForAmbient(rawColor)
                                val finalColor = ColorUtils.ensureMinimumLuminance(ambientColor, 0.25f)
                                _extractedColor.value = finalColor
                                
                                val r = (finalColor.red * 255).toInt().coerceIn(0, 255)
                                val g = (finalColor.green * 255).toInt().coerceIn(0, 255)
                                val b = (finalColor.blue * 255).toInt().coerceIn(0, 255)
                                val hexString = String.format("#%02X%02X%02X", r, g, b)
                                repository.saveCachedColor("${movie.mediaType}:${movie.id}", hexString)
                                val local = repository.getMovie(movie.id, movie.mediaType)
                                if (local != null && (forceReload || local.accentColor == null || local.accentColor != hexString)) {
                                    repository.saveMovie(local.copy(accentColor = hexString))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore extraction errors and fallback to default
                }
            }
        }
    }

}

private data class MetadataState(
    val metadata: MovieDetailResponse?,
    val external: ExternalRatings,
    val loadingS: Boolean,
    val seasonD: Map<Int, com.cinetrack.data.model.Season>,
    val collectionM: List<Movie>,
    val errorMsg: String?,
    val traktComments: List<com.cinetrack.data.api.TraktComment>
)
