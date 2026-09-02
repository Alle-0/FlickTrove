package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.SortConfig
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.domain.CycleMovieStatusUseCase
import com.cinetrack.domain.CalculateMatchScoreUseCase
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
import com.cinetrack.data.repository.NewsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

data class HomeUiState(
    val movies: ImmutableList<Movie> = persistentListOf(),
    val releasedMovies: ImmutableList<Movie> = persistentListOf(),
    val unreleasedMovies: ImmutableList<Movie> = persistentListOf(),
    val activeTvShows: ImmutableList<Movie> = persistentListOf(),
    val droppedTvShows: ImmutableList<Movie> = persistentListOf(),
    val movieCount: Int = 0,
    val tvCount: Int = 0,
    val isLoading: Boolean = true,
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
    val preferences: com.cinetrack.data.model.UserPreferences = com.cinetrack.data.model.UserPreferences(),
    val allLocalMovies: ImmutableList<Movie> = persistentListOf()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val calculateMatchScoreUseCase: CalculateMatchScoreUseCase,
    private val getHomeUiStateUseCase: com.cinetrack.domain.GetHomeUiStateUseCase,
    private val repository: MovieRepository,
    private val newsRepository: NewsRepository,
    private val preferenceRepository: PreferenceRepository,
    private val settingsRepository: com.cinetrack.data.repository.SettingsRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeTab = MutableStateFlow("movie")
    
    val movieGridState = LazyGridState()
    val tvGridState = LazyGridState()
    val animatedMovieIds = mutableSetOf<String>()
    
    fun emitMessage(message: UiText) {
        actionFeedbackManager.emit(message)
    }

    private val _feedState = MutableStateFlow(FeedState())

    init {
        fetchFeed()
    }

    private fun fetchFeed() {
        viewModelScope.launch {
            try {
                // Carica i film salvati dall'utente per generare le raccomandazioni
                val localMovies = repository.getLocalMoviesFlow().first()

                // Genera raccomandazioni personalizzate usando l'algoritmo (identico a RecommendationsViewModel)
                val recMovies = buildRecommendations(type = "movie", localMovies = localMovies)
                val recTv = buildRecommendations(type = "tv", localMovies = localMovies)

                // Carica tutte le altre sezioni in parallelo
                val popMoviesDeferred = async { repository.getPopularMovies().take(10).map { it.copy(mediaType = "movie") }.toImmutableList() }
                val nowMoviesDeferred = async { repository.getNowPlayingMovies().take(10).map { it.copy(mediaType = "movie") }.toImmutableList() }
                val topMoviesDeferred = async { repository.getTop10FlickTrove(isTv = false).take(10).map { it.copy(mediaType = "movie") }.toImmutableList() }
                val upcMoviesDeferred = async { repository.getUpcomingMovies().take(10).map { it.copy(mediaType = "movie") }.toImmutableList() }
                val popTvDeferred = async { repository.getPopularTV().take(10).map { it.copy(mediaType = "tv") }.toImmutableList() }
                val nowTvDeferred = async { repository.getOnTheAirTV().take(10).map { it.copy(mediaType = "tv") }.toImmutableList() }
                val topTvDeferred = async { repository.getTop10FlickTrove(isTv = true).take(10).map { it.copy(mediaType = "tv") }.toImmutableList() }
                val upcTvDeferred = async { repository.getUpcomingTV().take(10).map { it.copy(mediaType = "tv") }.toImmutableList() }
                val trendingMoviesDeferred = async {
                    val basicTrending = repository.getTrendingMovies().take(10)
                    kotlinx.coroutines.coroutineScope {
                        basicTrending.map { movie ->
                            async {
                                try {
                                    val detail = repository.getMovieDetail(movie.id, false)
                                    val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
                                    val currentLang = if (rawLanguage == "system") java.util.Locale.getDefault().language else rawLanguage
                                    val logos = detail.images?.logos
                                    val bestLogo = logos?.firstOrNull { it.iso6391 == currentLang } ?: logos?.firstOrNull { it.iso6391 == "en" } ?: logos?.firstOrNull()
                                    movie.copy(
                                        mediaType = "movie",
                                        genreIds = detail.genres?.map { it.id } ?: movie.genreIds
                                    ).apply {
                                        this.logoPath = bestLogo?.filePath
                                    }
                                } catch (e: Exception) {
                                    movie.copy(mediaType = "movie")
                                }
                            }
                        }.map { it.await() }.toImmutableList()
                    }
                }
                val trendingTvDeferred = async {
                    val basicTrending = repository.getTrendingTV().take(10)
                    kotlinx.coroutines.coroutineScope {
                        basicTrending.map { tv ->
                            async {
                                try {
                                    val detail = repository.getMovieDetail(tv.id, true)
                                    val rawLanguage = preferenceRepository.userPreferencesFlow.first().contentLanguage
                                    val currentLang = if (rawLanguage == "system") java.util.Locale.getDefault().language else rawLanguage
                                    val logos = detail.images?.logos
                                    val bestLogo = logos?.firstOrNull { it.iso6391 == currentLang } ?: logos?.firstOrNull { it.iso6391 == "en" } ?: logos?.firstOrNull()
                                    tv.copy(
                                        mediaType = "tv",
                                        genreIds = detail.genres?.map { it.id } ?: tv.genreIds
                                    ).apply {
                                        this.logoPath = bestLogo?.filePath
                                    }
                                } catch (e: Exception) {
                                    tv.copy(mediaType = "tv")
                                }
                            }
                        }.map { it.await() }.toImmutableList()
                    }
                }
                val newsDeferred = async { newsRepository.getNews().take(5).toImmutableList() }

                _feedState.value = FeedState(
                    recommendedMovies = recMovies,
                    popularMovies = popMoviesDeferred.await(),
                    nowPlayingMovies = nowMoviesDeferred.await(),
                    top10Movies = topMoviesDeferred.await(),
                    upcomingMovies = upcMoviesDeferred.await(),
                    recommendedTv = recTv,
                    popularTv = popTvDeferred.await(),
                    nowStreamingTv = nowTvDeferred.await(),
                    top10Tv = topTvDeferred.await(),
                    upcomingTv = upcTvDeferred.await(),
                    trendingMovies = trendingMoviesDeferred.await(),
                    trendingTv = trendingTvDeferred.await(),
                    magazineNews = newsDeferred.await()
                )
            } catch (e: Exception) {
                // handle error silently or emit message
            }
        }
    }

    /**
     * Algoritmo di raccomandazione: usa i film/serie salvati dall'utente come seed,
     * chiama l'API TMDB per titoli simili, e filtra con il CalculateMatchScore.
     * Se l'utente non ha nessun film salvato, restituisce lista vuota (la sezione non viene mostrata).
     */
    private suspend fun buildRecommendations(type: String, localMovies: List<Movie>): ImmutableList<Movie> {
        val matching = if (type == "movie") {
            localMovies.filter { it.mediaType != "tv" }
        } else {
            localMovies.filter { it.mediaType == "tv" }
        }
        if (matching.isEmpty()) return persistentListOf()

        // Seleziona i seed migliori (stessa logica di RecommendationsViewModel)
        val goodCandidates = matching.filter { movie ->
            (movie.personalRating ?: 0.0) >= 7.0 ||
            (movie.watchedAt != null && (movie.voteAverage ?: 0.0) >= 7.0)
        }
        val pool = if (goodCandidates.size >= 3) goodCandidates else matching
        val seeds = pool
            .sortedWith(
                compareByDescending<Movie> { it.personalRating ?: 0.0 }
                    .thenByDescending { it.watchedAt ?: "" }
                    .thenByDescending { it.voteAverage ?: 0.0 }
            )
            .take(20)
            .shuffled()
            .take(3)

        // Chiama API in parallelo per i 3 seed
        val localCompositeIds = localMovies.map { "${it.mediaType}_${it.id}" }.toSet()
        val rawData = coroutineScope {
            seeds.map { seed ->
                async {
                    runCatching {
                        if (type == "movie") repository.getMovieRecommendations(seed.id)
                        else repository.getTVRecommendations(seed.id)
                    }.getOrDefault(emptyList())
                }
            }.awaitAll().flatten()
        }

        // Filtra i già salvati e applica il match score (soglia 65%)
        var results = rawData
            .distinctBy { it.id }
            .filter { movie ->
                val compositeId = "${type}_${movie.id}"
                !localCompositeIds.contains(compositeId)
            }
            .map { it.copy(mediaType = type) }
            .mapNotNull { movie ->
                val score = calculateMatchScoreUseCase(movie, matching)
                if (score == null || score >= 65) {
                    movie.apply { matchScore = score }
                } else null
            }

        // Salvavita: se il filtro ha azzerato tutto, mostra i 10 migliori senza filtro
        if (results.isEmpty() && rawData.isNotEmpty()) {
            results = rawData
                .distinctBy { it.id }
                .filter { movie -> !localCompositeIds.contains("${type}_${movie.id}") }
                .map { it.copy(mediaType = type) }
                .map { movie -> 
                    val score = calculateMatchScoreUseCase(movie, matching)
                    movie.apply { matchScore = score }
                }
                .sortedByDescending { it.matchScore ?: 0 }
                .take(10)
        }

        return results.shuffled().take(10).toImmutableList()
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

        baseState.copy(
            recommendedMovies = filterList(feedState.recommendedMovies),
            popularMovies = filterList(feedState.popularMovies),
            nowPlayingMovies = filterList(feedState.nowPlayingMovies),
            top10Movies = feedState.top10Movies,
            upcomingMovies = filterList(feedState.upcomingMovies),
            recommendedTv = filterList(feedState.recommendedTv),
            popularTv = filterList(feedState.popularTv),
            nowStreamingTv = filterList(feedState.nowStreamingTv),
            top10Tv = feedState.top10Tv,
            upcomingTv = filterList(feedState.upcomingTv),
            trendingMovies = feedState.trendingMovies,
            trendingTv = feedState.trendingTv,
            magazineNews = feedState.magazineNews,
            isLoading = baseState.isLoading || feedState.popularMovies.isEmpty()
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
            else -> movies
        }
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

data class FeedState(
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
    val magazineNews: ImmutableList<NewsItem> = persistentListOf()
)
