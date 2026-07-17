package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.domain.CycleMovieStatusUseCase
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.lazy.grid.LazyGridState
import com.cinetrack.util.toComposeColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

data class RecommendationsUiState(
    val mediaType: String = "movie",
    val recommendedMovies: ImmutableList<Movie> = persistentListOf(),
    val isLoading: Boolean = true,
    val isNextPageLoading: Boolean = false,
    val isEndReached: Boolean = false,
    val favorites: ImmutableList<Movie> = persistentListOf(),
    val movieFolderColors: ImmutableMap<String, ImmutableList<String>> = persistentMapOf(),
    val folders: ImmutableList<com.cinetrack.data.local.entities.FolderEntity> = persistentListOf(),
    val preferences: com.cinetrack.data.model.UserPreferences = com.cinetrack.data.model.UserPreferences()
)

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val repository: MovieRepository,
    private val preferenceRepository: PreferenceRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) : ViewModel() {

    private val _mediaType = MutableStateFlow("movie")
    private val _isLoading = MutableStateFlow(true)
    private val _isNextPageLoading = MutableStateFlow(false)
    private val _isEndReached = MutableStateFlow(false)
    private val _recommendedMovies = MutableStateFlow<List<Movie>>(emptyList())
    
    val lazyGridState = LazyGridState()
    val animatedMovieIds = mutableSetOf<String>()
    
    private var currentPage = 1
    private var currentTopSourceIds: List<Long> = emptyList()


    val uiState: StateFlow<RecommendationsUiState> = combine(
        combine(
            _mediaType,
            _isLoading,
            _isNextPageLoading,
            _isEndReached,
            _recommendedMovies
        ) { mediaType, isLoading, isNextLoading, isEnd, recommended ->
            Triple(mediaType, Triple(isLoading, isNextLoading, isEnd), recommended)
        },
        combine(
            repository.getLocalMoviesFlow(),
            repository.getFoldersFlow(),
            preferenceRepository.userPreferencesFlow
        ) { localMovies, folders, prefs ->
            Triple(localMovies, folders, prefs)
        }
    ) { groupA, groupB ->
        val mediaType = groupA.first
        val (isLoading, isNextLoading, isEnd) = groupA.second
        val recommended = groupA.third
        
        val localMovies = groupB.first
        val folders = groupB.second
        val prefs = groupB.third
        
        val favorites = localMovies
        
        val movieFolderColors = mutableMapOf<String, MutableList<String>>()
        folders.forEach { folder ->
            val color = folder.color ?: "#FFFFFF"
            folder.itemIds.forEach { itemId ->
                movieFolderColors.getOrPut(itemId) { mutableListOf() }.add(color)
            }
        }
        val immutableMovieFolderColors = movieFolderColors.mapValues { it.value.toImmutableList() }.toImmutableMap()

        RecommendationsUiState(
            mediaType = mediaType,
            isLoading = isLoading,
            isNextPageLoading = isNextLoading,
            isEndReached = isEnd,
            recommendedMovies = recommended.toImmutableList(),
            favorites = favorites.toImmutableList(),
            movieFolderColors = immutableMovieFolderColors,
            folders = folders.toImmutableList(),
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = RecommendationsUiState()
    )

    init {
        // Fetch recommendations when mediaType changes
        viewModelScope.launch {
            _mediaType.collect { type ->
                currentPage = 1
                _isEndReached.value = false
                val allLocalMovies = repository.getLocalMoviesFlow().first()
                fetchRecommendations(type, allLocalMovies, page = 1)
            }
        }
    }

    fun onMediaTypeChanged(type: String) {
        _mediaType.value = type
    }

    fun onRefresh() {
        val currentState = uiState.value
        viewModelScope.launch {
            currentPage = 1
            _isEndReached.value = false
            fetchRecommendations(currentState.mediaType, currentState.favorites, page = 1)
        }
    }

    fun loadNextPage() {
        if (_isNextPageLoading.value || _isEndReached.value || _isLoading.value) return
        
        val currentState = uiState.value
        viewModelScope.launch {
            _isNextPageLoading.value = true
            currentPage++
            fetchRecommendations(currentState.mediaType, currentState.favorites, page = currentPage, isAppend = true)
        }
    }

    fun toggleFavorite(movie: Movie, onUndo: (suspend () -> Unit)? = null) {
        val title = movie.title ?: movie.name ?: ""
        viewModelScope.launch {
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            val previousState = current.copy()

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
                onUndo?.invoke()
            }
        }
    }

    fun emitMessage(message: UiText) {
        actionFeedbackManager.emit(message)
    }

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch {
            repository.deleteMovie(movie)
            actionFeedbackManager.emit(UiText.StringResource(R.string.msg_item_removed, movie.title ?: movie.name ?: "")) {
                repository.saveMovie(movie)
            }
        }
    }

    fun updateRating(movie: Movie, rating: Double) {
        viewModelScope.launch {
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            repository.saveMovie(current.copy(personalRating = rating))
        }
    }

    fun updateNote(movie: Movie, note: String) {
        viewModelScope.launch {
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            repository.saveMovie(current.copy(personalNote = note))
        }
    }

    fun getMovieFolderColors(movie: Movie): List<androidx.compose.ui.graphics.Color> {
        val compositeId = "${movie.mediaType}_${movie.id}"
        val colorHexes = uiState.value.movieFolderColors[compositeId] ?: emptyList()
        return colorHexes.map { it.toComposeColor() }
    }

    fun isItemInFolder(movie: Movie, folderId: String): Boolean {
        val folder = uiState.value.folders.find { it.id == folderId }
        val compositeId = "${movie.mediaType}_${movie.id}"
        return folder?.itemIds?.contains(compositeId) == true
    }

    fun toggleItemInFolder(folder: com.cinetrack.data.local.entities.FolderEntity, movie: Movie) {
        viewModelScope.launch {
            val compositeId = "${movie.mediaType}_${movie.id}"
            val newItemIds = if (folder.itemIds.contains(compositeId)) {
                folder.itemIds - compositeId
            } else {
                folder.itemIds + compositeId
            }
            repository.saveFolder(folder.copy(itemIds = newItemIds, updatedAt = java.time.Instant.now().toString()))
        }
    }

    private suspend fun fetchRecommendations(type: String, favorites: List<Movie>, page: Int, isAppend: Boolean = false) {
        if (!isAppend) _isLoading.value = true
        try {
            if (favorites.isEmpty()) {
                _recommendedMovies.value = emptyList()
                _isEndReached.value = true
                return
            }

            if (page == 1) {
                val matchingFavs = if (type == "movie") {
                    favorites.filter { it.mediaType != "tv" }
                } else {
                    favorites.filter { it.mediaType == "tv" }
                }

                // Base recommendations on movies the user actually liked.
                val goodCandidates = matchingFavs.filter { movie ->
                    (movie.personalRating ?: 0.0) >= 7.0 ||
                    (movie.watchedAt != null && (movie.voteAverage ?: 0.0) >= 7.0)
                }

                // If not enough good candidates, fallback to anything they watched or added
                val pool = if (goodCandidates.size >= 3) goodCandidates else matchingFavs

                // Sort the pool to find the absolute best seeds
                val topPool = pool.sortedWith(
                    compareByDescending<Movie> { it.personalRating ?: 0.0 }
                        .thenByDescending { it.watchedAt ?: "" }
                        .thenByDescending { it.voteAverage ?: 0.0 }
                ).take(20)

                // To ensure variety, pick 1 seed from the absolute Top 20, and 2 random seeds from the rest of the good pool
                val seed1 = topPool.randomOrNull()
                val remainingPool = pool.filter { it.id != seed1?.id }
                val otherSeeds = remainingPool.shuffled().take(2)

                val topItems = listOfNotNull(seed1) + otherSeeds
                
                currentTopSourceIds = topItems.map { it.id }
            }

            if (currentTopSourceIds.isEmpty()) {
                _recommendedMovies.value = emptyList()
                _isEndReached.value = true
                return
            }

            val results = mutableListOf<Movie>()
            // Use compositeId (mediaType_id) to avoid false matches between movies and TV shows with same TMDB numeric id
            val localCompositeIds = favorites.map { "${it.mediaType}_${it.id}" }.toSet()
            val existingIds = _recommendedMovies.value.map { it.id }.toSet()

            currentTopSourceIds.forEach { sourceId ->
                val data = if (type == "movie") {
                    repository.getMovieRecommendations(sourceId, page = page)
                } else {
                    repository.getTVRecommendations(sourceId, page = page)
                }
                results.addAll(
                    data.filter { movie ->
                        val compositeId = "${type}_${movie.id}"
                        !localCompositeIds.contains(compositeId) && !existingIds.contains(movie.id)
                    }.map { it.copy(mediaType = type) }
                )
            }

            if (results.isEmpty()) {
                if (page > 1) _isEndReached.value = true
                else _recommendedMovies.value = emptyList()
            } else {
                val newMovies = results.distinctBy { it.id }.shuffled()
                if (isAppend) {
                    _recommendedMovies.value = (_recommendedMovies.value + newMovies).distinctBy { it.id }
                } else {
                    _recommendedMovies.value = newMovies.take(30)
                }
            }
            
            // Limit to 5 pages for recommendations to avoid excessive fetching
            if (page >= 5) _isEndReached.value = true

        } catch (e: Exception) {
            if (!isAppend) _recommendedMovies.value = emptyList()
        } finally {
            _isLoading.value = false
            _isNextPageLoading.value = false
        }
    }

    private fun mapMovieGenreToTV(genreId: Long): Long {
        return when (genreId) {
            28L -> 10759L // Action -> Action & Adventure
            12L -> 10759L // Adventure -> Action & Adventure
            878L -> 10765L // Sci-Fi -> Sci-Fi & Fantasy
            14L -> 10765L // Fantasy -> Sci-Fi & Fantasy
            10752L -> 10768L // War -> War & Politics
            else -> genreId
        }
    }

    private fun mapTVGenreToMovie(genreId: Long): Long {
        return when (genreId) {
            10759L -> 28L // Action & Adventure -> Action
            10765L -> 878L // Sci-Fi & Fantasy -> Sci-Fi
            10768L -> 10752L // War & Politics -> War
            else -> genreId
        }
    }

    fun updatePreferences(prefs: com.cinetrack.data.model.UserPreferences) {
        viewModelScope.launch {
            preferenceRepository.updateAll(prefs)
        }
    }
}
