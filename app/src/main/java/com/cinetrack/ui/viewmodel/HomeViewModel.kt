package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.SortConfig
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.domain.CycleMovieStatusUseCase
import com.cinetrack.domain.usecase.GetHomeFeedUseCase
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import com.cinetrack.data.model.NewsItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import androidx.compose.runtime.mutableStateMapOf
import com.cinetrack.domain.UpdateEpisodesUseCase

data class HomeUiState(
    val movies: ImmutableList<Movie> = persistentListOf(),
    val releasedMovies: ImmutableList<Movie> = persistentListOf(),
    val unreleasedMovies: ImmutableList<Movie> = persistentListOf(),
    val activeTvShows: ImmutableList<Movie> = persistentListOf(),
    val droppedTvShows: ImmutableList<Movie> = persistentListOf(),
    val movieCount: Int = 0,
    val tvCount: Int = 0,
    val isLoading: Boolean = true,
    val isFeedLoading: Boolean = true,
    val hasFeedError: Boolean = false,
    val searchQuery: String = "",
    val activeTab: String = "movie",
    val sortConfig: SortConfig = SortConfig(),
    val notificationCount: Int = 0,
    val movieFolderColors: ImmutableMap<String, ImmutableList<String>> = persistentMapOf(),
    val folders: ImmutableList<com.cinetrack.data.local.entities.FolderEntity> = persistentListOf(),
    val recommendedMovies: ImmutableList<Movie> = persistentListOf(),
    val popularMovies: ImmutableList<Movie> = persistentListOf(),
    val nowPlayingMovies: ImmutableList<Movie> = persistentListOf(),
    val top10Movies: ImmutableList<Movie> = persistentListOf(),
    val upcomingMovies: ImmutableList<Movie> = persistentListOf(),
    val recommendedTv: ImmutableList<Movie> = persistentListOf(),
    val popularTv: ImmutableList<Movie> = persistentListOf(),
    val nowStreamingTv: ImmutableList<Movie> = persistentListOf(),
    val top10Tv: ImmutableList<Movie> = persistentListOf(),
    val upcomingTv: ImmutableList<Movie> = persistentListOf(),
    val trendingMovies: ImmutableList<Movie> = persistentListOf(),
    val trendingTv: ImmutableList<Movie> = persistentListOf(),
    val magazineNews: ImmutableList<NewsItem> = persistentListOf(),
    val continueWatchingTv: ImmutableList<Movie> = persistentListOf(),
    val becauseYouWatchedMovie: Pair<Movie, ImmutableList<Movie>>? = null,
    val becauseYouWatchedTv: Pair<Movie, ImmutableList<Movie>>? = null,
    val preferences: com.cinetrack.data.model.UserPreferences = com.cinetrack.data.model.UserPreferences(),
    val allLocalMovies: ImmutableList<Movie> = persistentListOf()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val getHomeFeedUseCase: GetHomeFeedUseCase,
    private val getHomeUiStateUseCase: com.cinetrack.domain.GetHomeUiStateUseCase,
    private val repository: MovieRepository,
    private val preferenceRepository: PreferenceRepository,
    private val settingsRepository: com.cinetrack.data.repository.SettingsRepository,
    private val actionFeedbackManager: ActionFeedbackManager,
    private val updateEpisodesUseCase: UpdateEpisodesUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeTab = MutableStateFlow("movie")
    
    val movieGridState = LazyGridState()
    val tvGridState = LazyGridState()
    val feedListState = androidx.compose.foundation.lazy.LazyListState()
    var feedPagerIndex: Int? = null
    val animatedMovieIds = mutableSetOf<String>()
    val updatingShowIds = mutableStateMapOf<Long, Boolean>()
    
    fun emitMessage(message: UiText) {
        actionFeedbackManager.emit(message)
    }

    private val _feedState = MutableStateFlow(FeedState())

    init {
        fetchFeed()
    }

    private fun fetchFeed() {
        viewModelScope.launch {
            // 1. Legge prima la cache Room per un rendering istantaneo (T=0ms)
            val cachedData = repository.getCachedHomeFeed()
            if (cachedData != null) {
                _feedState.value = cachedData.toFeedState()
            } else if (!_feedState.value.isLoaded) {
                _feedState.value = FeedState(isLoaded = false, hasError = false)
            }

            // 2. Esegue il refresh di rete in background
            try {
                val freshFeed = getHomeFeedUseCase()
                _feedState.value = freshFeed
                // Salva nella cache Room per i successivi accessi
                repository.saveCachedHomeFeed(com.cinetrack.data.model.CachedFeedData.fromFeedState(freshFeed))
            } catch (e: Exception) {
                // Se non c'è cache pregressa, mostra lo stato di errore
                if (!_feedState.value.isLoaded) {
                    _feedState.value = FeedState(hasError = true)
                    actionFeedbackManager.emit(UiText.DynamicString("Network error: Could not load feed"))
                }
            }
        }
    }

    suspend fun resolveMovieLogo(movie: Movie): String? {
        if (!movie.logoPath.isNullOrEmpty()) return movie.logoPath
        return try {
            val isTv = movie.mediaType == "tv"
            val logo = repository.getMovieLogo(movie.id, isTv)
            if (logo != null) {
                movie.logoPath = logo
                val currentFeed = _feedState.value
                val updatedMovies = currentFeed.trendingMovies.map {
                    if (it.id == movie.id) it.apply { this.logoPath = logo } else it
                }.toImmutableList()
                val updatedTv = currentFeed.trendingTv.map {
                    if (it.id == movie.id) it.apply { this.logoPath = logo } else it
                }.toImmutableList()
                _feedState.value = currentFeed.copy(
                    trendingMovies = updatedMovies,
                    trendingTv = updatedTv
                )
            }
            logo
        } catch (e: Exception) {
            null
        }
    }


    fun retryFeed() {
        fetchFeed()
    }



    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val uiState: StateFlow<HomeUiState> = kotlinx.coroutines.flow.combine(
        getHomeUiStateUseCase(
            moviesFlow = repository.getLocalMoviesFlow(),
            foldersFlow = repository.getFoldersFlow(),
            preferencesFlow = preferenceRepository.userPreferencesFlow,
            searchQueryFlow = _searchQuery.debounce(300).distinctUntilChanged(),
            activeTabFlow = _activeTab
        ),
        _feedState,
        settingsRepository.hideSavedFromDiscovery
    ) { baseState, feedState, hideSaved ->
        val localCompositeIds = if (hideSaved) {
            baseState.allLocalMovies.map { "${it.mediaType}_${it.id}" }.toSet()
        } else {
            emptySet()
        }

        fun filterList(list: ImmutableList<Movie>): ImmutableList<Movie> {
            if (!hideSaved) return list
            return list.filter { !localCompositeIds.contains("${it.mediaType}_${it.id}") }.toImmutableList()
        }

        val allLocalCompositeIds = baseState.allLocalMovies.map { "${it.mediaType}_${it.id}" }.toSet()
        fun forceFilterRecommendations(list: ImmutableList<Movie>): ImmutableList<Movie> {
            return list.filter { !allLocalCompositeIds.contains("${it.mediaType}_${it.id}") }.toImmutableList()
        }

        baseState.copy(
            recommendedMovies = forceFilterRecommendations(feedState.recommendedMovies),
            popularMovies = filterList(feedState.popularMovies),
            nowPlayingMovies = filterList(feedState.nowPlayingMovies),
            top10Movies = feedState.top10Movies,
            upcomingMovies = filterList(feedState.upcomingMovies),
            recommendedTv = forceFilterRecommendations(feedState.recommendedTv),
            popularTv = filterList(feedState.popularTv),
            nowStreamingTv = filterList(feedState.nowStreamingTv),
            top10Tv = feedState.top10Tv,
            upcomingTv = filterList(feedState.upcomingTv),
            trendingMovies = feedState.trendingMovies,
            trendingTv = feedState.trendingTv,
            magazineNews = feedState.magazineNews,
            continueWatchingTv = feedState.continueWatchingTv.mapNotNull { tv ->
                val localTv = baseState.allLocalMovies.find { it.id == tv.id && it.mediaType == "tv" }
                val effectiveTv = localTv ?: tv
                if (!effectiveTv.watched && !effectiveTv.dropped) {
                    val next = effectiveTv.calculateNextEpisode()
                    if (next != null && !next.isUpToDateWithAirDate) effectiveTv else null
                } else null
            }.toImmutableList(),
            becauseYouWatchedMovie = feedState.becauseYouWatchedMovie,
            becauseYouWatchedTv = feedState.becauseYouWatchedTv,
            isLoading = baseState.isLoading,
            isFeedLoading = baseState.isLoading || (!feedState.isLoaded && !feedState.hasError),
            hasFeedError = feedState.hasError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = HomeUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTabChanged(tab: String) {
        _activeTab.value = tab
    }

    fun updateSortConfig(config: SortConfig) {
        viewModelScope.launch {
            try {
                preferenceRepository.updateHomeSort(config)
                repository.savePreferencesRemote(uiState.value.preferences.copy(homeSort = config))
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_saving))
            }
        }
    }

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch {
            try {
                val updated = uiState.value.preferences.copy(gridColumns = columns)
                preferenceRepository.updateGridColumns(columns)
                repository.savePreferencesRemote(updated)
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_saving))
            }
        }
    }

    fun toggleWatched(movie: Movie) {
        val title = movie.title ?: movie.name ?: ""
        viewModelScope.launch {
            try {
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
                    try {
                        repository.saveMovie(previousState)
                    } catch (e: Exception) {
                        // ignore nested error
                    }
                }
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating))
            }
        }
    }

    fun markNextEpisodeWatched(movie: Movie) {
        if (updatingShowIds[movie.id] == true) return

        viewModelScope.launch {
            updatingShowIds[movie.id] = true
            try {
                val localMovie = repository.getMovie(movie.id, movie.mediaType) ?: movie
                val nextInfo = localMovie.calculateNextEpisode()
                if (nextInfo == null || nextInfo.isUpToDateWithAirDate) {
                    return@launch
                }

                val currentWatched = localMovie.watchedEpisodes?.toMutableMap() ?: mutableMapOf()
                val seasonKey = nextInfo.seasonNumber.toString()
                val seasonEps = currentWatched[seasonKey]?.toMutableList() ?: mutableListOf()

                if (!seasonEps.contains(nextInfo.episodeNumber)) {
                    seasonEps.add(nextInfo.episodeNumber)
                }
                currentWatched[seasonKey] = seasonEps

                val updatedMovie = updateEpisodesUseCase(localMovie, nextInfo.seasonNumber, seasonEps).copy(dropped = false)
                val finalMovie = if (nextInfo.isLastEpisodeOfSeries) {
                    updatedMovie.copy(
                        watched = true,
                        favorite = false,
                        reminder = false
                    )
                } else {
                    updatedMovie
                }

                repository.saveMovie(finalMovie)

                if (nextInfo.isLastEpisodeOfSeries) {
                    actionFeedbackManager.emit(
                        UiText.StringResource(R.string.msg_series_completed, movie.name ?: movie.title ?: "")
                    )
                } else {
                    actionFeedbackManager.emit(
                        UiText.StringResource(R.string.msg_episode_watched, nextInfo.episodeCode)
                    )
                }
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating))
            } finally {
                updatingShowIds.remove(movie.id)
            }
        }
    }

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch {
            try {
                repository.deleteMovie(movie)
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_item_removed, movie.title ?: movie.name ?: "")) {
                    try {
                        repository.saveMovie(movie)
                    } catch (e: Exception) {
                        // ignore nested error
                    }
                }
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_removing))
            }
        }
    }

    fun updateRating(movie: Movie, rating: Double) {
        viewModelScope.launch {
            try {
                val local = repository.getMovie(movie.id, movie.mediaType)
                val current = local ?: movie
                repository.saveMovie(current.copy(personalRating = rating, votedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating))
            }
        }
    }

    fun updateNote(movie: Movie, note: String) {
        viewModelScope.launch {
            try {
                val local = repository.getMovie(movie.id, movie.mediaType)
                val current = local ?: movie
                repository.saveMovie(current.copy(personalNote = note))
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating))
            }
        }
    }

    fun toggleItemInFolder(folder: com.cinetrack.data.local.entities.FolderEntity, movie: Movie) {
        viewModelScope.launch {
            try {
                val compositeId = "${movie.mediaType}_${movie.id}"
                val newItemIds = if (folder.itemIds.contains(compositeId)) {
                    folder.itemIds - compositeId
                } else {
                    folder.itemIds + compositeId
                }
                repository.saveFolder(folder.copy(itemIds = newItemIds, updatedAt = java.time.Instant.now().toString()))
                val local = repository.getMovie(movie.id, movie.mediaType)
                if (local == null) {
                    repository.saveMovie(movie)
                }
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating_folder))
            }
        }
    }

    private fun sortMovies(movies: List<Movie>, sort: SortConfig): List<Movie> {
        val isDesc = sort.sortDirection == "desc"
        return when (sort.sortType) {
            "release_date" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { it.releaseDate ?: it.firstAirDate ?: "" }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { it.releaseDate ?: it.firstAirDate ?: "" }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                }
            }
            "title" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { it.title ?: it.name ?: "" }.thenBy { it.id })
                }
            }
            "added_at", "created_at" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { it.clientUpdatedAt }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { it.clientUpdatedAt }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                }
            }
            "vote_average" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { it.voteAverage ?: 0.0 }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { it.voteAverage ?: 0.0 }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                }
            }
            "personal_rating" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { it.personalRating ?: 0.0 }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { it.personalRating ?: 0.0 }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                }
            }
            "runtime" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { getMovieDuration(it) }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { getMovieDuration(it) }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                }
            }
            "remaining_episodes" -> {
                if (isDesc) {
                    movies.sortedWith(
                        compareByDescending<Movie> { getRemainingEpisodes(it) }
                            .thenBy { it.title ?: it.name ?: "" }
                            .thenBy { it.id }
                    )
                } else {
                    movies.sortedWith(
                        compareBy<Movie> {
                            val rem = getRemainingEpisodes(it)
                            if (rem == 0) Int.MAX_VALUE else rem
                        }.thenBy { it.title ?: it.name ?: "" }
                            .thenBy { it.id }
                    )
                }
            }
            else -> movies
        }
    }

    private fun getRemainingEpisodes(movie: Movie): Int {
        if (movie.mediaType != "tv") return 0
        val info = movie.calculateNextEpisode()
        if (info != null) {
            if (info.isUpToDateWithAirDate) return 0
            return if (info.remainingTotal > 0) info.remainingTotal else info.remainingInSeason
        }
        val watched = movie.watchedEpisodes?.filterKeys { it != "0" }?.values?.sumOf { it.size } ?: 0
        val total = movie.numberOfEpisodes ?: 0
        return maxOf(0, total - watched)
    }

    private fun getMovieDuration(movie: Movie): Int {
        return if (movie.mediaType == "tv") {
            val avgRuntime = movie.episodeRunTime?.average()?.toInt() ?: 0
            val totalEpisodes = movie.numberOfEpisodes ?: 0
            avgRuntime * totalEpisodes
        } else {
            movie.runtime ?: 0
        }
    }
}
